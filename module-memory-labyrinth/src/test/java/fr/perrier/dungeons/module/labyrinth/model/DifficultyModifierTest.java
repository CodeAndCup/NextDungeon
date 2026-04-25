package fr.perrier.dungeons.module.labyrinth.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DifficultyModifierTest {

    @Test
    void tier1IsNeutral() {
        DifficultyModifier mod = new DifficultyModifier(1);
        assertThat(mod.hpMultiplier()).isEqualTo(1.0);
        assertThat(mod.damageMultiplier()).isEqualTo(1.0);
    }

    @Test
    void tierIncrementBumpsBothMultipliers() {
        DifficultyModifier mod = new DifficultyModifier();
        mod.incrementTier();
        assertThat(mod.getTier()).isEqualTo(2);
        assertThat(mod.hpMultiplier()).isEqualTo(1.0 + DifficultyModifier.HP_PER_TIER);
        assertThat(mod.damageMultiplier()).isEqualTo(1.0 + DifficultyModifier.DMG_PER_TIER);
    }

    @Test
    void tier3MatchesLinearFormula() {
        DifficultyModifier mod = new DifficultyModifier(3);
        assertThat(mod.hpMultiplier()).isEqualTo(1.0 + 2 * DifficultyModifier.HP_PER_TIER);
        assertThat(mod.damageMultiplier()).isEqualTo(1.0 + 2 * DifficultyModifier.DMG_PER_TIER);
    }

    @Test
    void clampsToMinTierOne() {
        DifficultyModifier mod = new DifficultyModifier(0);
        assertThat(mod.getTier()).isEqualTo(1);
        mod.setTier(-5);
        // Setter does not clamp ; multipliers should still produce sane
        // negative values — behaviour is documented as "do not call with
        // tier < 1" on the model.
        assertThat(mod.getTier()).isEqualTo(-5);
    }
}
