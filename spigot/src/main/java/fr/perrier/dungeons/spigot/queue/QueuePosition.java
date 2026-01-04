package fr.perrier.dungeons.spigot.queue;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents a player's position in the queue.
 */
@Data
@AllArgsConstructor
public class QueuePosition {
    private int position;
    private int totalInQueue;
    private String floorId;
}
