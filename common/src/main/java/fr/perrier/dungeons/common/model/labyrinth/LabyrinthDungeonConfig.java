package fr.perrier.dungeons.common.model.labyrinth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Dungeon-level configuration for a {@code LABYRINTH}-typed dungeon.
 *
 * <p>Lives inside the dashboard's {@code DungeonEntry} payload as a
 * sibling of the regular fields. Carries every piece of data shared
 * across the dungeon's difficulties (= floors) :</p>
 *
 * <ul>
 *   <li>{@link #worldId} — name of the static world that contains the
 *       pre-built room geometries</li>
 *   <li>{@link #dungeonSpawn} — single point where players land when
 *       the {@code FloorInstance} becomes ready ; from here they're
 *       prompted (Infinite) or teleported to the lobby</li>
 *   <li>{@link #rooms} — shared pool of {@link LabyrinthRoom} ; each
 *       floor (difficulty) picks its subset via tag filtering</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
public class LabyrinthDungeonConfig implements Serializable {

    private String worldId;
    private LabyrinthRoom.Vec3 dungeonSpawn;
    private List<LabyrinthRoom> rooms = new ArrayList<>();
}
