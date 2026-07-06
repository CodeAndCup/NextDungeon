package fr.perrier.dungeons.common.model.dungeon.config;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Requirements {
    private long retryCooldown;
    private List<String> requiredFloorsId;
    private List<String> removeCompletion;
    private PartyRequirements partyRequirements;
    private List<String> requiredItems;
    /**
     * Subset of {@link #requiredItems} whose items must <em>not</em> be consumed at launch — i.e.
     * they are checked as "the player must have it" but kept in the inventory. Any required item
     * not listed here keeps the historical behaviour of being consumed. {@code null}/empty means
     * every required item is consumed, so existing floors are unaffected.
     */
    private List<String> nonConsumedItems;
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
               ", removeCompletion=" + removeCompletion +
               ", partyRequirements=" + partyRequirements +
               ", requiredItems=" + requiredItems +
               ", nonConsumedItems=" + nonConsumedItems +
               ", forbiddenItems=" + forbiddenItems +
               ", minLevel=" + minLevel +
               '}';
    }
}
