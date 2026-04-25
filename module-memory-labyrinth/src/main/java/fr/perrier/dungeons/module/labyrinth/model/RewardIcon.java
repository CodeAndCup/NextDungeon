package fr.perrier.dungeons.module.labyrinth.model;

/**
 * Reward icons displayed above doors and accumulated during a run.
 *
 * <p>The icon displayed at door choice indicates the reward bias of the next
 * room ; counts are persisted in the run (and in the Infinite save) and used
 * by the loot calculator at end-of-run.</p>
 *
 * <ul>
 *   <li>{@link #NONE} — neutral (lobby rooms, never visible above a door)</li>
 *   <li>{@link #GOLD} — multiplies final gold reward</li>
 *   <li>{@link #BLESSING} — reserved, not implemented in v1 (see CDC §10)</li>
 * </ul>
 */
public enum RewardIcon {
    NONE,
    GOLD,
    BLESSING
}
