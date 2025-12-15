package fr.perrier.dungeons.common.model.player;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PlayerStats {
    private final UUID playerId;
    private int enemiesKilled;
    private int deaths;
    private long startTime;

    public PlayerStats(UUID playerId) {
        this.playerId = playerId;
        this.enemiesKilled = 0;
        this.deaths = 0;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Increments the count of enemies killed by the player.
     */
    public void incrementEnemiesKilled() {
        this.enemiesKilled++;
    }

    /**
     * Increments the count of deaths for the player.
     */
    public void incrementDeaths() {
        this.deaths++;
    }

    @Override
    public String toString() {
        return "PlayerStats{" +
               "playerId=" + playerId +
               ", enemiesKilled=" + enemiesKilled +
               ", deaths=" + deaths +
               ", startTime=" + startTime +
               '}';
    }
}