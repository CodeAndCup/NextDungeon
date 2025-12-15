package fr.perrier.dungeons.common.model.dungeon.config;

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

        @Override
        public String toString() {
            return "PartyRequirements{" +
                   "minSize=" + minSize +
                   ", maxSize=" + maxSize +
                   '}';
        }
    }

    @Override
    public String toString() {
        return "Requirements{" +
               "retryCooldown=" + retryCooldown +
               ", requiredFloorsId=" + requiredFloorsId +
               ", partyRequirements=" + partyRequirements +
               ", requiredItems=" + requiredItems +
               ", forbiddenItems=" + forbiddenItems +
               ", minLevel=" + minLevel +
               '}';
    }
}
