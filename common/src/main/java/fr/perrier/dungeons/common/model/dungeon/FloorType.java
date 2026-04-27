package fr.perrier.dungeons.common.model.dungeon;

/**
 * Discriminator for the kind of floor the host plugin should run.
 *
 * <ul>
 *   <li>{@link #CLASSIC} — the historical NextDungeon flow : worldConfig
 *       + region + steps + triggers, all driven by the host engine.</li>
 *   <li>{@link #LABYRINTH} — the {@code module-memory-labyrinth} module
 *       takes over once the {@code FloorInstance} is ready ; the
 *       floor's {@code labyrinthFloorConfig} is the input and the
 *       parent dungeon's {@code labyrinthDungeonConfig} carries the
 *       shared room pool.</li>
 * </ul>
 */
public enum FloorType {
    CLASSIC,
    LABYRINTH
}
