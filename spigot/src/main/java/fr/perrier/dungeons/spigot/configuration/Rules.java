package fr.perrier.dungeons.spigot.configuration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Rules {
    private long deathBanDuration;
    private String gamemode;
    private boolean allowFlight;
}
