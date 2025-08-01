package fr.perrier.dungeons.configuration;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Requirements {
    private long retryCooldown;
    private List<String> requiredDungeons;
    private PartyRequirements partyRequirements;
    private List<String> requiredItems;
    private List<String> forbiddenItems;

    @Getter
    @Setter
    public static class PartyRequirements {
        private int minSize;
        private int maxSize;
    }
}
