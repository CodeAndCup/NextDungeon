package fr.perrier.dungeons.common.model.labyrinth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Loot configuration scoped to a single labyrinth floor (CDC §4.4,
 * Q5.2 = un pool d'items par floor).
 *
 * <p>Resolved at end-of-run :</p>
 * <pre>
 *   gold      = baseGold × (1 + goldPerIcon × iconCounts.GOLD) × tierMultiplier
 *   itemRolls = baseItemRolls
 *   for r in 0..itemRolls : pickWeightedFromItems(filter: minTier ≤ currentTier)
 * </pre>
 *
 * <p>Lives inline in the floor's {@code LabyrinthFloorConfig} now —
 * no more dedicated DB table (pre-redesign R6 cleanup).</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class LootTable implements Serializable {

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
