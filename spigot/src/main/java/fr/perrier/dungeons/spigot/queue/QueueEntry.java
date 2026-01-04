package fr.perrier.dungeons.spigot.queue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Represents a player's entry in the dungeon queue.
 * This class is serializable to be stored in Redis.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueueEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID playerId;
    private String playerName;
    private String floorId;
    private long timestamp;
    private String serverName;

    /**
     * Creates a new queue entry for a player.
     *
     * @param playerId the UUID of the player
     * @param playerName the name of the player
     * @param floorId the ID of the floor they want to join
     * @param serverName the name of the server they are on
     */
    public QueueEntry(UUID playerId, String playerName, String floorId, String serverName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.floorId = floorId;
        this.timestamp = System.currentTimeMillis();
        this.serverName = serverName;
    }
}
