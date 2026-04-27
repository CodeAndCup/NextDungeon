package fr.perrier.dungeons.module.labyrinth.loot;

import fr.perrier.dungeons.module.labyrinth.manager.LootTableRegistry;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthRun;
import fr.perrier.dungeons.common.model.labyrinth.LootTable;
import fr.perrier.dungeons.common.model.labyrinth.RewardIcon;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Computes per-player loot at end-of-run from the run's accumulated
 * icon counts and the floor's {@link LootTable}.
 *
 * <p>Formula (CDC §6.5) :</p>
 * <pre>
 *   gold      = baseGold × (1 + goldPerIcon × iconCounts.GOLD) × goldTierBonus(tier)
 *   itemRolls = baseItemRolls
 *   for r in 0..itemRolls : pickWeightedFromItems(filter: minTier ≤ tier)
 * </pre>
 *
 * <p>Each player gets an independent RNG seeded from
 * {@code run.seed ^ player.uuid} so a save/restart re-roll is identical
 * for each player but distinct between players in the same run.</p>
 */
public class LootCalculator {

    /** Linear gold bonus per tier above the first (small Infinite incentive). */
    public static final double GOLD_TIER_BONUS_PER_TIER = 0.10;

    private final LootTableRegistry lootTables;

    public LootCalculator(LootTableRegistry lootTables) {
        this.lootTables = lootTables;
    }

    /**
     * Compute the loot for every player of the run. Returns one
     * {@link LootResult} per UUID in {@code playerIds} (typically the
     * instance's player set). Players whose UUID is not in the run's
     * initial party still get a result, computed with the run's icons.
     */
    public List<LootResult> computeForPlayers(LabyrinthRun run,
                                              List<UUID> playerIds,
                                              boolean success) {
        List<LootResult> out = new ArrayList<>();
        if (run == null || playerIds == null) return out;
        LootTable table = lootTables == null ? null : lootTables.getByInstance(run.getInstanceId());
        for (UUID playerId : playerIds) {
            out.add(computeForPlayer(run, playerId, table, success));
        }
        return out;
    }

    public LootResult computeForPlayer(LabyrinthRun run,
                                       UUID playerId,
                                       LootTable table,
                                       boolean success) {
        LootResult result = new LootResult();
        result.setPlayerId(playerId);
        result.setSuccess(success);
        result.setFloorId(run.getFloorId());
        result.setFinalRoomIndex(run.getCurrentRoomIndex());
        result.setTier(run.getCurrentModifier() != null ? run.getCurrentModifier().getTier() : 1);
        result.setIconCounts(new EnumMap<>(run.getIconCounts()));

        if (table == null) {
            // No loot table configured for this floor : neutral result.
            result.setGoldEarned(0L);
            return result;
        }

        Random rng = new Random(run.getSeed() ^ (playerId == null ? 0L : playerId.getMostSignificantBits()));
        result.setGoldEarned(rollGold(run, table, result.getTier()));
        result.setItemsRolled(rollItems(table, result.getTier(), rng));
        return result;
    }

    private long rollGold(LabyrinthRun run, LootTable table, int tier) {
        int goldIcons = run.getIconCount(RewardIcon.GOLD);
        double iconMult = 1.0 + table.getGoldPerIcon() * goldIcons;
        double tierMult = 1.0 + GOLD_TIER_BONUS_PER_TIER * Math.max(0, tier - 1);
        return Math.round(table.getBaseGold() * iconMult * tierMult);
    }

    private List<String> rollItems(LootTable table, int tier, Random rng) {
        List<String> rolled = new ArrayList<>();
        if (table.getItems() == null || table.getItems().isEmpty()) return rolled;
        int rolls = Math.max(0, table.getBaseItemRolls());
        if (rolls == 0) return rolled;

        // Filter once — minTier gate.
        List<LootTable.Entry> eligible = new ArrayList<>();
        long totalWeight = 0;
        for (LootTable.Entry e : table.getItems()) {
            if (e == null || e.getItemId() == null || e.getItemId().isEmpty()) continue;
            if (e.getMinTier() > tier) continue;
            int w = Math.max(0, e.getWeight());
            if (w == 0) continue;
            eligible.add(e);
            totalWeight += w;
        }
        if (eligible.isEmpty() || totalWeight == 0) return rolled;

        for (int r = 0; r < rolls; r++) {
            long pick = (long) (rng.nextDouble() * totalWeight);
            long cursor = 0;
            for (LootTable.Entry e : eligible) {
                cursor += Math.max(0, e.getWeight());
                if (pick < cursor) {
                    rolled.add(e.getItemId());
                    break;
                }
            }
        }
        return rolled;
    }
}
