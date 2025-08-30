package fr.perrier.dungeons.spigot.configuration;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Requirements {
    private long retryCooldown;
    private List<String> requiredFloorsId;
    private PartyRequirements partyRequirements;
    private List<String> requiredItems;
    private List<String> forbiddenItems;
    private int minLevel;

    @Getter
    @Setter
    public static class PartyRequirements {
        private int minSize;
        private int maxSize;
    }
}
