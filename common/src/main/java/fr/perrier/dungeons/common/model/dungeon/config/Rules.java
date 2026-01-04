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
    private int maxInstance;

    @Override
    public String toString() {
        return "Rules{" +
                "maxLives=" + maxLives +
                ", deathBanDuration='" + deathBanDuration + '\'' +
                ", gamemode='" + gamemode + '\'' +
                ", allowFlight=" + allowFlight +
                ", maxInstance=" + maxInstance +
                '}';
    }
}
