package fr.perrier.dungeons.common.model.dungeon.config;

import fr.perrier.dungeons.common.model.player.PlayerStats;
import lombok.Getter;

import java.util.HashMap;
import java.util.UUID;

@Getter
public class FloorInstanceData {
    protected final String floorId;
    protected UUID instanceId;
    protected boolean ready;

    protected final HashMap<UUID, PlayerStats> playerStats = new HashMap<>();
    protected final HashMap<UUID, Integer> playerCurrentLives = new HashMap<>();

    public FloorInstanceData(String floorId) {
        this.floorId = floorId;
        this.ready = false;
    }

    public FloorInstanceData(UUID instanceId, String floorId) {
        this.instanceId = instanceId;
        this.floorId = floorId;
        this.ready = false;
    }

    /**
     * Gets the name of this instance.
     * <p>
     * The name is in the format of {@code <floorId>_<instanceId>}.
     * @return the name of this instance
     */
    public String getInstanceName() {
        return floorId + "_" + instanceId.toString();
    }
}
