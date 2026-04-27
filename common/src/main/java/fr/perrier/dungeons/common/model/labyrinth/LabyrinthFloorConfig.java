package fr.perrier.dungeons.common.model.labyrinth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Floor-level (= difficulty) configuration for a {@code LABYRINTH} floor.
 *
 * <p>Lives in {@code FloorData.labyrinthFloorConfig} when the floor's
 * type is {@link fr.perrier.dungeons.common.model.dungeon.FloorType#LABYRINTH}.</p>
 *
 * <ul>
 *   <li>{@link #maxRooms} — finite floors stop at this index ; use
 *       {@link Integer#MAX_VALUE} for the infinite difficulty</li>
 *   <li>{@link #saveEnabled} — typically true only for infinite</li>
 *   <li>{@link #tagFilter} — only rooms carrying this tag are eligible
 *       for this floor's picker (typically the floor name itself,
 *       e.g. {@code "easy"}). Empty = pick from the whole pool.</li>
 *   <li>{@link #hpScalingPerTier} / {@link #dmgScalingPerTier} — per-floor
 *       overrides of the default difficulty scaling. {@code 0.0} keeps
 *       the global default of the {@code DifficultyModifier}.</li>
 *   <li>{@link #lootTable} — inline loot table for this difficulty</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
public class LabyrinthFloorConfig implements Serializable {

    private int maxRooms;
    private boolean saveEnabled;
    private String tagFilter;
    private double hpScalingPerTier;
    private double dmgScalingPerTier;
    private LootTable lootTable;
}
