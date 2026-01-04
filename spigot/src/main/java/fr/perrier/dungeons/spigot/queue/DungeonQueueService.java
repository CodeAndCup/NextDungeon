package fr.perrier.dungeons.spigot.queue;

import fr.perrier.dungeons.spigot.Main;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RDeque;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service that manages the global dungeon queue in Redis.
 * This queue is synchronized across all Spigot servers.
 */
@RequiredArgsConstructor
public class DungeonQueueService {
    private final RedissonClient redissonClient;

    private static final String QUEUE_PREFIX = "dungeons:queue:";
    private static final String INSTANCE_COUNT_PREFIX = "dungeons:instance_count:";

    /**
     * Initializes the queue service.
     */
    public void initialize() {
        Main.getInstance().getLogger().info("DungeonQueueService initialized successfully");
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

        queue.offer(entry);
        Main.getInstance().getLogger().info(String.format(
            "Added player %s to queue for floor %s (Position: %d/%d)",
            entry.getPlayerName(),
            entry.getFloorId(),
            queue.size(),
            queue.size()
        ));
        return true;
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
        
        if (removed) {
            Main.getInstance().getLogger().info(String.format(
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
        
        if (entry != null) {
            Main.getInstance().getLogger().info(String.format(
                "Polled player %s from queue for floor %s",
                entry.getPlayerName(),
                floorId
            ));
        }
        
        return entry;
    }

    /**
     * Gets a player's position in the queue.
     * 
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
        
        Main.getInstance().getLogger().info(String.format(
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
        
        Main.getInstance().getLogger().info(String.format(
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
     * Gets all floors that have active queues.
     *
     * @return set of floor IDs
     */
    public Set<String> getActiveQueueFloors() {
        Set<String> floors = new HashSet<>();
        for (String key : redissonClient.getKeys().getKeysByPattern(QUEUE_PREFIX + "*")) {
            String floorId = key.substring(QUEUE_PREFIX.length());
            RDeque<QueueEntry> queue = getQueue(floorId);
            if (!queue.isEmpty()) {
                floors.add(floorId);
            }
        }
        return floors;
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
        
        Main.getInstance().getLogger().info(String.format(
            "Cleared queue for floor %s (%d entries removed)",
            floorId,
            size
        ));
    }
}
