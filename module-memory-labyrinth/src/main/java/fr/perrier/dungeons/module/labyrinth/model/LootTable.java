package fr.perrier.dungeons.module.labyrinth.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Loot configuration scoped to a single floor (CDC §4.4, Q5.2).
 *
 * <p>Resolved at end-of-run (CDC §6.5) :</p>
 * <pre>
 *   gold      = baseGold × (1 + goldPerIcon × iconCounts.GOLD) × tierMultiplier
 *   itemRolls = baseItemRolls
 *   for r in 0..itemRolls : pickWeightedFromItems(filter: minTier ≤ currentTier)
 * </pre>
 *
 * <p>One pool per floor — {@code easy} cannot drop legendary items reserved
 * to {@code infinite} (CDC Q5.2).</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class LootTable implements Serializable {

    private String floorId;

    private long baseGold;
    private double goldPerIcon;
    private int baseItemRolls;

    private List<Entry> items = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Entry implements Serializable {
        private String itemId;
        private int weight;
        /** Minimum difficulty tier needed to roll this entry. Use 0 to always allow. */
        private int minTier;

        public Entry(String itemId, int weight, int minTier) {
            this.itemId = itemId;
            this.weight = weight;
            this.minTier = minTier;
        }
    }
}
