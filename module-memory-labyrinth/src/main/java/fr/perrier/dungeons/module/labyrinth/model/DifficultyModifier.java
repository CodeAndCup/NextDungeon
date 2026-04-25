package fr.perrier.dungeons.module.labyrinth.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Scaling applied to mobs spawned in a labyrinth run.
 *
 * <p>Tier starts at 1 and increments by one each time a boss is killed in
 * Infinite. Multipliers grow linearly :</p>
 *
 * <pre>
 *   hpMult(tier)  = 1 + 0.30 * (tier - 1)
 *   dmgMult(tier) = 1 + 0.15 * (tier - 1)
 * </pre>
 *
 * <p>Concrete values (proposition initiale, à équilibrer en QA — CDC §6.6) :
 * tier 1 → ×1.0/×1.0, tier 2 → ×1.3/×1.15, tier 3 → ×1.6/×1.30, …</p>
 */
@Getter
@Setter
public class DifficultyModifier {

    public static final double HP_PER_TIER = 0.30;
    public static final double DMG_PER_TIER = 0.15;

    private int tier;

    public DifficultyModifier() {
        this.tier = 1;
    }

    public DifficultyModifier(int tier) {
        this.tier = Math.max(1, tier);
    }

    public double hpMultiplier() {
        return 1.0 + HP_PER_TIER * (tier - 1);
    }

    public double damageMultiplier() {
        return 1.0 + DMG_PER_TIER * (tier - 1);
    }

    public void incrementTier() {
        this.tier++;
    }
}
