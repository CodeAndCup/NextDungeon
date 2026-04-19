package fr.perrier.dungeons.spigot.parties.impl;

import fr.perrier.dungeons.common.model.party.DungeonPartyData;
import fr.perrier.dungeons.spigot.Main;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.redisson.api.RMap;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cluster-wide registry of dungeon parties, backed by Redis.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Holds the canonical {@link DungeonPartyData} map so every lobby sees the same parties,
 *       regardless of which server hosts the live {@code IParty} object.</li>
 *   <li>Publishes a heartbeat for every party this server owns so orphaned parties left behind
 *       by a crashed owner are evicted automatically.</li>
 *   <li>Broadcasts UPDATE/REMOVE events over a Redis topic so peer servers can invalidate
 *       their local caches without polling.</li>
 * </ul>
 *
 * Only the owner server (identified by {@link DungeonPartyData#getOwnerServiceId()}) is allowed
 * to mutate a party. Other servers route mutations via Pidgin packets.
 */
public class DungeonPartyRegistry {

    private static final long HEARTBEAT_INTERVAL_TICKS = 20L * 30L; // 30s — refresh own parties
    private static final long CLEANUP_INTERVAL_TICKS = 20L * 60L;   // 60s — evict stale parties
    private static final long STALE_THRESHOLD_MS = 1000L * 60L * 3L; // 3min without heartbeat → evict

    @Getter
    private static DungeonPartyRegistry instance;

    private final RedissonClient redissonClient;
    private final UUID currentServiceId;

    private RMap<UUID, DungeonPartyData> partiesMap;
    private RTopic syncTopic;

    /**
     * Local cache of every known party keyed by leaderId. This keeps reads off the wire for
     * menu redraws; Redis stays authoritative and is re-read when the cache is cold.
     */
    private final Map<UUID, DungeonPartyData> cacheByLeader = new ConcurrentHashMap<>();

    private BukkitTask heartbeatTask;
    private BukkitTask cleanupTask;

    public DungeonPartyRegistry(RedissonClient redissonClient, UUID currentServiceId) {
        this.redissonClient = redissonClient;
        this.currentServiceId = currentServiceId;
        instance = this;
    }

    public void initialize() {
        String topic = Objects.requireNonNull(
                Main.getInstance().getConfig().getString("RedisConfiguration.topic"));
        this.partiesMap = redissonClient.getMap(topic + ":dungeon_parties");
        this.syncTopic = redissonClient.getTopic(topic + ":dungeon_parties_sync");

        subscribeSyncTopic();
        warmCache();
        startHeartbeat();
        startCleanup();

        Main.getLoggerUtil().info("DungeonPartyRegistry initialized (service=" + currentServiceId + ")");
    }

    private void subscribeSyncTopic() {
        syncTopic.addListener(SyncMessage.class, (channel, msg) -> {
            if (msg == null) return;
            switch (msg.action) {
                case UPDATE -> {
                    if (msg.data != null) cacheByLeader.put(msg.data.getLeaderId(), msg.data);
                }
                case REMOVE -> {
                    if (msg.leaderId != null) cacheByLeader.remove(msg.leaderId);
                }
            }
        });
    }

    private void warmCache() {
        try {
            for (DungeonPartyData data : partiesMap.values()) {
                if (data != null && data.getLeaderId() != null) {
                    cacheByLeader.put(data.getLeaderId(), data);
                }
            }
        } catch (Exception e) {
            Main.getLoggerUtil().warning("DungeonPartyRegistry warmCache failed: " + e.getMessage());
        }
    }

    /**
     * Publishes a party to Redis and notifies peers. Must only be called on the owner server.
     */
    public void publish(DungeonPartyData data) {
        if (data == null || data.getLeaderId() == null) return;
        data.setLastHeartbeat(System.currentTimeMillis());
        data.setOwnerServiceId(currentServiceId);

        cacheByLeader.put(data.getLeaderId(), data);
        try {
            partiesMap.fastPut(data.getLeaderId(), data);
            syncTopic.publish(SyncMessage.update(data));
        } catch (Exception e) {
            Main.getLoggerUtil().warning("DungeonPartyRegistry publish failed: " + e.getMessage());
        }
    }

    /**
     * Removes a party from Redis and notifies peers.
     */
    public void remove(UUID leaderId) {
        if (leaderId == null) return;
        cacheByLeader.remove(leaderId);
        try {
            partiesMap.remove(leaderId);
            syncTopic.publish(SyncMessage.remove(leaderId));
        } catch (Exception e) {
            Main.getLoggerUtil().warning("DungeonPartyRegistry remove failed: " + e.getMessage());
        }
    }

    public DungeonPartyData get(UUID leaderId) {
        if (leaderId == null) return null;
        DungeonPartyData cached = cacheByLeader.get(leaderId);
        if (cached != null) return cached;
        try {
            DungeonPartyData fetched = partiesMap.get(leaderId);
            if (fetched != null) cacheByLeader.put(leaderId, fetched);
            return fetched;
        } catch (Exception e) {
            Main.getLoggerUtil().warning("DungeonPartyRegistry get failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns every party known to the cluster. Used by the party finder UI.
     */
    public Collection<DungeonPartyData> getAll() {
        return new ArrayList<>(cacheByLeader.values());
    }

    /**
     * True if this server owns the given party and may mutate it directly.
     */
    public boolean isOwnedLocally(DungeonPartyData data) {
        return data != null && currentServiceId != null
                && currentServiceId.equals(data.getOwnerServiceId());
    }

    private void startHeartbeat() {
        heartbeatTask = Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), () -> {
            long now = System.currentTimeMillis();
            for (DungeonPartyData data : cacheByLeader.values()) {
                if (!isOwnedLocally(data)) continue;
                data.setLastHeartbeat(now);
                try {
                    partiesMap.fastPut(data.getLeaderId(), data);
                } catch (Exception e) {
                    Main.getLoggerUtil().warning("Heartbeat failed for party "
                            + data.getLeaderId() + ": " + e.getMessage());
                }
            }
        }, HEARTBEAT_INTERVAL_TICKS, HEARTBEAT_INTERVAL_TICKS);
    }

    private void startCleanup() {
        cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), () -> {
            long now = System.currentTimeMillis();
            try {
                for (Map.Entry<UUID, DungeonPartyData> entry : partiesMap.entrySet()) {
                    DungeonPartyData data = entry.getValue();
                    if (data == null) continue;
                    if (now - data.getLastHeartbeat() > STALE_THRESHOLD_MS) {
                        Main.getLoggerUtil().info("Evicting stale dungeon party "
                                + entry.getKey() + " (owner=" + data.getOwnerServiceId() + ")");
                        remove(entry.getKey());
                    }
                }
            } catch (Exception e) {
                Main.getLoggerUtil().warning("DungeonPartyRegistry cleanup failed: " + e.getMessage());
            }
        }, CLEANUP_INTERVAL_TICKS, CLEANUP_INTERVAL_TICKS);
    }

    /**
     * Removes every party owned by this server from Redis. Called on clean shutdown so the
     * cluster never serves stale listings when the plugin is reloaded.
     */
    public void shutdown() {
        if (heartbeatTask != null) heartbeatTask.cancel();
        if (cleanupTask != null) cleanupTask.cancel();

        if (partiesMap == null || currentServiceId == null) return;
        try {
            List<UUID> toRemove = new ArrayList<>();
            for (Map.Entry<UUID, DungeonPartyData> entry : partiesMap.entrySet()) {
                DungeonPartyData data = entry.getValue();
                if (data != null && currentServiceId.equals(data.getOwnerServiceId())) {
                    toRemove.add(entry.getKey());
                }
            }
            for (UUID leaderId : toRemove) {
                partiesMap.remove(leaderId);
                syncTopic.publish(SyncMessage.remove(leaderId));
            }
            Main.getLoggerUtil().info("DungeonPartyRegistry cleared "
                    + toRemove.size() + " owned parties on shutdown");
        } catch (Exception e) {
            Main.getLoggerUtil().warning("DungeonPartyRegistry shutdown cleanup failed: " + e.getMessage());
        }
    }

    /**
     * Sync payload published on the Redis topic. Kept as a simple POJO to avoid coupling the
     * payload format to the Redisson codec configuration.
     */
    public static class SyncMessage implements java.io.Serializable {
        public enum Action { UPDATE, REMOVE }

        public Action action;
        public UUID leaderId;
        public DungeonPartyData data;

        public SyncMessage() { }

        public static SyncMessage update(DungeonPartyData data) {
            SyncMessage m = new SyncMessage();
            m.action = Action.UPDATE;
            m.leaderId = data.getLeaderId();
            m.data = data;
            return m;
        }

        public static SyncMessage remove(UUID leaderId) {
            SyncMessage m = new SyncMessage();
            m.action = Action.REMOVE;
            m.leaderId = leaderId;
            return m;
        }
    }
}
