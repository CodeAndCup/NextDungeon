package fr.perrier.dungeons.common.model.dungeon.config;

import fr.perrier.dungeons.common.model.player.PlayerStats;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
public class FloorInstanceData {
    protected final String floorId;
    protected UUID instanceId;
    @Setter
    protected boolean ready;

    protected final Set<UUID> players;
    protected final Map<UUID, PlayerStats> playerStats;
    protected final Map<UUID, Integer> playerCurrentLives;

    public FloorInstanceData(String floorId, Set<UUID> players) {
        this.floorId = floorId;
        this.ready = false;
        this.players = new HashSet<>(players);
        this.playerStats = new HashMap<>();
        this.playerCurrentLives = new HashMap<>();
    }

    public FloorInstanceData(UUID instanceId, String floorId) {
        this.instanceId = instanceId;
        this.floorId = floorId;
        this.ready = false;
        this.players = new HashSet<>();
        this.playerStats = new HashMap<>();
        this.playerCurrentLives = new HashMap<>();
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

    @Override
    public String toString() {
        return "FloorInstanceData{" +
               "floorId='" + floorId + '\'' +
               ", instanceId=" + instanceId +
               ", ready=" + ready +
               ", players=" + players +
               ", playerStats=" + playerStats +
               ", playerCurrentLives=" + playerCurrentLives +
               '}';
    }
}
