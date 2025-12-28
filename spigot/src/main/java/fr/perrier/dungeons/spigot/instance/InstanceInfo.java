package fr.perrier.dungeons.spigot.instance;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

/**
 * Data Transfer Object (DTO) class containing information about an instance.
 * Independent of the provider used.
 */
@Data
@AllArgsConstructor
public class InstanceInfo {
    /**
     * Unique identifier of the instance
     */
    private final UUID instanceId;

    /**
     * Identifier of the floor
     */
    private final String floorId;

    /**
     * Creation date of the instance
     */
    private final String createdAt;

    /**
     * Indicates if the instance is in edit mode
     */
    private boolean editMode;

    /**
     * Indicates if the instance is ready to receive players
     */
    private boolean ready;

    /**
     * Constructor without optional flags
     */
    public InstanceInfo(UUID instanceId, String floorId, String createdAt) {
        this(instanceId, floorId, createdAt, false, false);
    }
}