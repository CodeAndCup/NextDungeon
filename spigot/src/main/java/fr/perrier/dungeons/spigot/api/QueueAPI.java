package fr.perrier.dungeons.spigot.api;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.queue.QueueEntry;
import lombok.Data;

import java.util.*;

/**
 * API for dashboard and external systems to access queue information.
 * This provides read-only access to queue data.
 */
public class QueueAPI {

    /**
     * Gets the current queue state for all floors.
     *
     * @return map of floor IDs to queue info
     */
    public static Map<String, QueueInfo> getAllQueues() {
        if (Main.getInstance().getDungeonQueueService() == null) {
            return Collections.emptyMap();
        }

        Map<String, QueueInfo> result = new HashMap<>();
        Set<String> activeFloors = Main.getInstance().getDungeonQueueService().getActiveQueueFloors();

        for (String floorId : activeFloors) {
            QueueInfo info = getQueueInfo(floorId);
            if (info != null) {
                result.put(floorId, info);
            }
        }

        return result;
    }

    /**
     * Gets queue information for a specific floor.
     *
     * @param floorId the floor ID
     * @return queue info, or null if not found
     */
    public static QueueInfo getQueueInfo(String floorId) {
        if (Main.getInstance().getDungeonQueueService() == null) {
            return null;
        }

        List<QueueEntry> entries = Main.getInstance().getDungeonQueueService().getQueueEntries(floorId);
        int activeInstances = Main.getInstance().getDungeonQueueService().getActiveInstanceCount(floorId);

        List<PlayerQueueInfo> players = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            QueueEntry entry = entries.get(i);
            players.add(new PlayerQueueInfo(
                entry.getPlayerId().toString(),
                entry.getPlayerName(),
                i + 1,
                entry.getTimestamp(),
                entry.getServerName()
            ));
        }

        return new QueueInfo(
            floorId,
            entries.size(),
            activeInstances,
            players
        );
    }

    /**
     * Gets queue statistics across all floors.
     *
     * @return queue statistics
     */
    public static QueueStats getQueueStats() {
        if (Main.getInstance().getDungeonQueueService() == null) {
            return new QueueStats(0, 0, 0);
        }

        Set<String> activeFloors = Main.getInstance().getDungeonQueueService().getActiveQueueFloors();
        int totalQueued = 0;
        int totalInstances = 0;

        for (String floorId : activeFloors) {
            totalQueued += Main.getInstance().getDungeonQueueService().getQueueSize(floorId);
            totalInstances += Main.getInstance().getDungeonQueueService().getActiveInstanceCount(floorId);
        }

        return new QueueStats(activeFloors.size(), totalQueued, totalInstances);
    }

    /**
     * Data class representing queue information for a floor.
     */
    @Data
    public static class QueueInfo {
        private final String floorId;
        private final int queueSize;
        private final int activeInstances;
        private final List<PlayerQueueInfo> players;
    }

    /**
     * Data class representing a player in the queue.
     */
    @Data
    public static class PlayerQueueInfo {
        private final String playerId;
        private final String playerName;
        private final int position;
        private final long timestamp;
        private final String serverName;
    }

    /**
     * Data class representing overall queue statistics.
     */
    @Data
    public static class QueueStats {
        private final int activeFloors;
        private final int totalQueued;
        private final int totalInstances;
    }
}
