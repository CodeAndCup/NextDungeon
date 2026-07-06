package fr.perrier.dungeons.spigot.queue;

import fr.perrier.dungeons.common.queue.QueueEntry;
import fr.perrier.dungeons.spigot.Main;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RDeque;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Service that manages the global dungeon queue in Redis.
 * This queue is synchronized across all Spigot servers.
 */
@RequiredArgsConstructor
public class DungeonQueueService {
    private final RedissonClient redissonClient;

    private static final String QUEUE_PREFIX = Objects.requireNonNull(Main.getInstance().getConfig().getString("RedisConfiguration.topic")) + ":queue:";
    private static final String INSTANCE_COUNT_PREFIX = Objects.requireNonNull(Main.getInstance().getConfig().getString("RedisConfiguration.topic")) + ":instance_count:";

    // Cache of active queue floors to avoid expensive Redis pattern matching
    private final Set<String> activeQueueFloorsCache = ConcurrentHashMap.newKeySet();

    // Reverse map: player UUID → set of floor IDs they are queued in (O(1) cleanup on quit)
    private final Map<UUID, Set<String>> playerQueueMembership = new ConcurrentHashMap<>();

    /**
     * Initializes the queue service.
     */
    public void initialize() {
        Main.getLoggerUtil().info("DungeonQueueService initialized successfully");
    }

    /**
     * Gets the queue for a specific floor.
     *
     * @param floorId the floor ID
     * @return the queue deque
     */
    private RDeque<QueueEntry> getQueue(String floorId) {
        return redissonClient.getDeque(QUEUE_PREFIX + floorId);
    }

    /**
     * Gets the instance count map for a specific floor.
     *
     * @param floorId the floor ID
     * @return the instance count map
     */
    private RMap<UUID, String> getInstanceCountMap(String floorId) {
        return redissonClient.getMap(INSTANCE_COUNT_PREFIX + floorId);
    }

    /**
     * Adds a player to the queue for a specific floor.
     *
     * @param entry the queue entry to add
     * @return true if added successfully, false if already in queue
     */
    public boolean addToQueue(QueueEntry entry) {
        RDeque<QueueEntry> queue = getQueue(entry.getFloorId());
        
        // Check if player is already in queue
        if (isPlayerInQueue(entry.getPlayerId(), entry.getFloorId())) {
            return false;
        }

        boolean added = queue.offer(entry);

        if(added) {
            // Update caches
            activeQueueFloorsCache.add(entry.getFloorId());
            playerQueueMembership.computeIfAbsent(entry.getPlayerId(), k -> ConcurrentHashMap.newKeySet()).add(entry.getFloorId());

            if (Main.getLoggerUtil().isDebugEnabled())
                Main.getLoggerUtil().info(String.format(
                        "Added player %s to queue for floor %s (Position: %d/%d)",
                        entry.getPlayerName(),
                        entry.getFloorId(),
                        queue.size(),
                        queue.size()
                ));
        }
        return added;
    }

    /**
     * Removes a player from the queue.
     *
     * @param playerId the player's UUID
     * @param floorId the floor ID
     * @return true if removed successfully
     */
    public boolean removeFromQueue(UUID playerId, String floorId) {
        RDeque<QueueEntry> queue = getQueue(floorId);
        boolean removed = queue.removeIf(entry -> entry.getPlayerId().equals(playerId));

        // Update caches
        if (removed) {
            Set<String> floors = playerQueueMembership.get(playerId);
            if (floors != null) {
                floors.remove(floorId);
                if (floors.isEmpty()) {
                    playerQueueMembership.remove(playerId);
                }
            }
        }
        if (removed && queue.isEmpty()) {
            activeQueueFloorsCache.remove(floorId);
        }

        if (removed && Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info(String.format(
                "Removed player %s from queue for floor %s",
                playerId,
                floorId
            ));
        }
        
        return removed;
    }

    /**
     * Gets the next player from the queue.
     *
     * @param floorId the floor ID
     * @return the next queue entry, or null if queue is empty
     */
    public QueueEntry pollNext(String floorId) {
        RDeque<QueueEntry> queue = getQueue(floorId);
        QueueEntry entry = queue.poll();
        
        // Update cache if queue is now empty
        if (queue.isEmpty()) {
            activeQueueFloorsCache.remove(floorId);
        }

        if (entry != null && Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info(String.format(
                "Polled player %s from queue for floor %s",
                entry.getPlayerName(),
                floorId
            ));
        }
        
        return entry;
    }

    /**
     * Gets a player's position in the queue.
     * <p>
     * Note: This operation has O(n) complexity. For large queues with frequent
     * position lookups, consider using a Redis sorted set for better performance.
     *
     * @param playerId the player's UUID
     * @param floorId the floor ID
     * @return the queue position, or null if not in queue
     */
    public QueuePosition getQueuePosition(UUID playerId, String floorId) {
        RDeque<QueueEntry> queue = getQueue(floorId);
        List<QueueEntry> queueList = new ArrayList<>(queue);
        
        int position = -1;
        for (int i = 0; i < queueList.size(); i++) {
            if (queueList.get(i).getPlayerId().equals(playerId)) {
                position = i + 1; // 1-based position
                break;
            }
        }
        
        if (position == -1) {
            return null;
        }
        
        return new QueuePosition(position, queueList.size(), floorId);
    }

    /**
     * Checks if a player is in the queue for a specific floor.
     *
     * @param playerId the player's UUID
     * @param floorId the floor ID
     * @return true if player is in queue
     */
    public boolean isPlayerInQueue(UUID playerId, String floorId) {
        RDeque<QueueEntry> queue = getQueue(floorId);
        return queue.stream().anyMatch(entry -> entry.getPlayerId().equals(playerId));
    }

    /**
     * Gets the current number of active instances for a floor.
     *
     * @param floorId the floor ID
     * @return the number of active instances
     */
    public int getActiveInstanceCount(String floorId) {
        RMap<UUID, String> instanceMap = getInstanceCountMap(floorId);
        return instanceMap.size();
    }

    /**
     * Registers a new instance for a floor.
     *
     * @param floorId the floor ID
     * @param instanceId the instance UUID
     */
    public void registerInstance(String floorId, UUID instanceId) {
        RMap<UUID, String> instanceMap = getInstanceCountMap(floorId);
        instanceMap.put(instanceId, floorId);

        if (Main.getLoggerUtil().isDebugEnabled())
            Main.getLoggerUtil().info(String.format(
                "Registered instance %s for floor %s (Total: %d)",
                instanceId,
                floorId,
                instanceMap.size()
            ));
    }

    /**
     * Unregisters an instance for a floor.
     *
     * @param floorId the floor ID
     * @param instanceId the instance UUID
     */
    public void unregisterInstance(String floorId, UUID instanceId) {
        RMap<UUID, String> instanceMap = getInstanceCountMap(floorId);
        instanceMap.remove(instanceId);
        if (Main.getLoggerUtil().isDebugEnabled())
            Main.getLoggerUtil().info(String.format(
                "Unregistered instance %s for floor %s (Total: %d)",
                instanceId,
                floorId,
                instanceMap.size()
            ));
    }

    /**
     * Gets the size of the queue for a specific floor.
     *
     * @param floorId the floor ID
     * @return the queue size
     */
    public int getQueueSize(String floorId) {
        RDeque<QueueEntry> queue = getQueue(floorId);
        return queue.size();
    }

    /**
     * Gets all queue entries for a specific floor.
     *
     * @param floorId the floor ID
     * @return list of queue entries
     */
    public List<QueueEntry> getQueueEntries(String floorId) {
        RDeque<QueueEntry> queue = getQueue(floorId);
        return new ArrayList<>(queue);
    }

    /**
     * Gets all floors that a specific player is currently queued in.
     * This uses a local membership map for O(1) lookup instead of scanning all queues.
     *
     * @param playerId the player's UUID
     * @return set of floor IDs the player is queued in (may be empty, never null)
     */
    public Set<String> getPlayerQueueFloors(UUID playerId) {
        Set<String> floors = playerQueueMembership.get(playerId);
        return floors != null ? new HashSet<>(floors) : Collections.emptySet();
    }

    /**
     * Gets all floors that have active queues.
     * This uses a cached set of floors to avoid expensive Redis pattern matching queries.
     *
     * @return set of floor IDs with active queues
     */
    public Set<String> getActiveQueueFloors() {
        return new HashSet<>(activeQueueFloorsCache);
    }

    /**
     * Executes an action for each entry in the queue for a specific floor.
     *
     * @param floorId the floor ID
     * @param action the action to execute
     */
    public void executeForEachInQueue(String floorId, Consumer<QueueEntry> action) {
        RDeque<QueueEntry> queue = getQueue(floorId);
        for (QueueEntry entry : queue) {
            action.accept(entry);
        }
    }

    /**
     * Clears the queue for a specific floor.
     *
     * @param floorId the floor ID
     */
    public void clearQueue(String floorId) {
        RDeque<QueueEntry> queue = getQueue(floorId);
        int size = queue.size();
        queue.clear();
        // Update cache to remove this floor
        activeQueueFloorsCache.remove(floorId);

        if (Main.getLoggerUtil().isDebugEnabled())
            Main.getLoggerUtil().info(String.format(
                "Cleared queue for floor %s (%d entries removed)",
                floorId,
                size
            ));
    }
}
