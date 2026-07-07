package fr.perrier.dungeons.spigot.storage;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.database.DatabaseManager;
import fr.perrier.dungeons.spigot.model.ProfileData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBatch;
import org.redisson.api.RBucket;
import org.redisson.api.RFuture;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RScoredSortedSetAsync;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Per-floor leaderboards backed by Redis sorted sets (ZSETs).
 *
 * <p>One ZSET per {@link Metric} per floor — key {@code <topic>:lb:<metric>:<floorId>},
 * score = the metric value, member = player UUID. This turns "what is my rank on this
 * floor" into an {@code O(log N)} {@code ZRANK}/{@code ZREVRANK} plus an {@code O(1)}
 * {@code ZCARD}, instead of an {@code O(players)} scan of the profiles table.</p>
 *
 * <p>Scores are written whenever a run completes (see
 * {@code FloorInstance.complete}); the existing per-player merge already keeps the
 * cumulative/best value, so we simply push that snapshot here.</p>
 */
@RequiredArgsConstructor
public class LeaderboardService {

    private final RedissonClient redissonClient;

    private static final String TOPIC =
            Objects.requireNonNull(Main.getInstance().getConfig().getString("RedisConfiguration.topic"));
    private static final String LB_PREFIX = TOPIC + ":lb:";
    // Sentinel marking that the backfill has run; disappears on a Redis flush so the
    // rebuild re-runs on the next boot.
    private static final String BUILT_FLAG = TOPIC + ":lb:built";

    /**
     * Positions at or below this value are shown as an absolute {@code #position};
     * beyond it the menu falls back to a {@code Top X%} percentile.
     */
    public static final int ABSOLUTE_RANK_LIMIT = 200;

    public enum Metric {
        BEST_TIME("besttime", false),          // completion time (ms) — lower is better
        TOTAL_KILLS("totalkills", true),
        MOST_KILLS_IN_RUN("mostkillsinrun", true),
        TOTAL_RUNS("totalruns", true),
        TOTAL_COMPLETIONS("totalcompletions", true);

        private final String key;
        @Getter
        private final boolean higherIsBetter;

        Metric(String key, boolean higherIsBetter) {
            this.key = key;
            this.higherIsBetter = higherIsBetter;
        }
    }

    /** 1-based position out of {@code total} ranked players. */
    public record Rank(int position, int total) {}

    private RScoredSortedSet<String> zset(Metric metric, String floorId) {
        return redissonClient.getScoredSortedSet(LB_PREFIX + metric.key + ":" + floorId, StringCodec.INSTANCE);
    }

    /**
     * Writes one score. Higher-vs-lower semantics are resolved at read time
     * ({@code rank} vs {@code revRank}), so writing is always a plain {@code ZADD}.
     */
    public void submit(String floorId, UUID playerId, Metric metric, double score) {
        try {
            zset(metric, floorId).add(score, playerId.toString());
        } catch (Exception e) {
            Main.getLoggerUtil().warning("[Leaderboard] submit failed for "
                    + floorId + "/" + metric.key + ": " + e.getMessage());
        }
    }

    /**
     * Pushes every ranked metric for a floor from an already-merged
     * {@link ProfileData.FloorStats} snapshot (cumulative totals / personal bests).
     */
    public void submitAll(UUID playerId, ProfileData.FloorStats stats) {
        if (playerId == null || stats == null) return;
        String floorId = stats.getFloorId();
        // Sentinel completion times (-1 / -2) mean "never finished" — keep them out of
        // the best-time ranking so only real records compete.
        if (stats.getFastestCompletionTime() > 0) {
            submit(floorId, playerId, Metric.BEST_TIME, stats.getFastestCompletionTime());
        }
        submit(floorId, playerId, Metric.TOTAL_KILLS, stats.getTotalEnemiesKilled());
        submit(floorId, playerId, Metric.MOST_KILLS_IN_RUN, stats.getMostEnemiesKilledInRun());
        submit(floorId, playerId, Metric.TOTAL_RUNS, stats.getTotalRuns());
        submit(floorId, playerId, Metric.TOTAL_COMPLETIONS, stats.getTotalCompletions());
    }

    /**
     * Returns the player's 1-based position and the total ranked count for a metric,
     * or {@code null} if the player is not on that leaderboard (e.g. never completed).
     */
    public Rank getRank(String floorId, UUID playerId, Metric metric) {
        try {
            RScoredSortedSet<String> z = zset(metric, floorId);
            Integer r = metric.higherIsBetter
                    ? z.revRank(playerId.toString())
                    : z.rank(playerId.toString());
            if (r == null) return null;
            return new Rank(r + 1, z.size());
        } catch (Exception e) {
            Main.getLoggerUtil().warning("[Leaderboard] getRank failed for "
                    + floorId + "/" + metric.key + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Batched rank lookup: fetches {@code rank + size} for every (floor × metric) pair in a
     * single Redis pipeline round-trip. Preferred over looping {@link #getRank} when a menu
     * needs many ranks at once (e.g. {@code ProfileMenu} — up to 10 floors × 5 metrics would
     * otherwise be ~100 blocking round-trips on the main thread).
     *
     * @return {@code floorId -> (metric -> Rank)}; a missing metric entry means the player is
     *         not ranked for it. Never null.
     */
    public Map<String, EnumMap<Metric, Rank>> getRanks(Collection<String> floorIds, UUID playerId, Metric... metrics) {
        Map<String, EnumMap<Metric, Rank>> out = new HashMap<>();
        if (floorIds == null || floorIds.isEmpty() || metrics.length == 0) return out;

        String member = playerId.toString();
        Map<String, EnumMap<Metric, RFuture<Integer>>> rankFutures = new HashMap<>();
        Map<String, EnumMap<Metric, RFuture<Integer>>> sizeFutures = new HashMap<>();
        try {
            RBatch batch = redissonClient.createBatch();
            for (String floorId : floorIds) {
                EnumMap<Metric, RFuture<Integer>> rf = new EnumMap<>(Metric.class);
                EnumMap<Metric, RFuture<Integer>> sf = new EnumMap<>(Metric.class);
                for (Metric m : metrics) {
                    RScoredSortedSetAsync<String> z =
                            batch.getScoredSortedSet(LB_PREFIX + m.key + ":" + floorId, StringCodec.INSTANCE);
                    rf.put(m, m.higherIsBetter ? z.revRankAsync(member) : z.rankAsync(member));
                    sf.put(m, z.sizeAsync());
                }
                rankFutures.put(floorId, rf);
                sizeFutures.put(floorId, sf);
            }
            batch.execute(); // single round-trip; all futures resolved after this returns

            for (String floorId : floorIds) {
                EnumMap<Metric, Rank> perFloor = new EnumMap<>(Metric.class);
                for (Metric m : metrics) {
                    Integer r = rankFutures.get(floorId).get(m).toCompletableFuture().getNow(null);
                    Integer size = sizeFutures.get(floorId).get(m).toCompletableFuture().getNow(null);
                    if (r != null && size != null && size > 0) {
                        perFloor.put(m, new Rank(r + 1, size));
                    }
                }
                out.put(floorId, perFloor);
            }
        } catch (Exception e) {
            Main.getLoggerUtil().warning("[Leaderboard] batch getRanks failed: " + e.getMessage());
        }
        return out;
    }

    /**
     * Rebuilds every leaderboard from the profiles table when the ZSETs are missing
     * (first deploy, or after a Redis flush). Guarded by a sentinel bucket so it runs
     * at most once per Redis lifetime.
     *
     * <p>This performs a full {@code O(players)} profile scan and is the one place that
     * cost is paid — once, at boot, off the main thread — instead of on every menu open.</p>
     */
    public void backfillIfEmpty(DatabaseManager dbManager) {
        if (dbManager == null) return;
        RBucket<String> flag = redissonClient.getBucket(BUILT_FLAG, StringCodec.INSTANCE);
        if (flag.isExists()) return;
        try {
            List<ProfileData> profiles = dbManager.getAllProfiles().get();
            int players = 0;
            for (ProfileData profile : profiles) {
                if (profile == null || profile.getPlayerId() == null) continue;
                for (ProfileData.FloorStats stats : profile.getFloorStats()) {
                    submitAll(profile.getPlayerId(), stats);
                }
                players++;
            }
            flag.set("1");
            Main.getLoggerUtil().info("[Leaderboard] Backfill complete — " + players + " profile(s) indexed.");
        } catch (Exception e) {
            Main.getLoggerUtil().warning("[Leaderboard] Backfill failed: " + e.getMessage());
        }
    }
}
