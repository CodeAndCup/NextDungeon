package fr.perrier.dungeons.common.model.dungeon.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Rules {
    private int maxLives;
    private String deathBanDuration;
    private String gamemode;
    private boolean allowFlight;
}
