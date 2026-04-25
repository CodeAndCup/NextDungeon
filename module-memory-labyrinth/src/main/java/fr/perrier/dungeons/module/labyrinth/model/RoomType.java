package fr.perrier.dungeons.module.labyrinth.model;

/**
 * Type of a labyrinth room template.
 *
 * <ul>
 *   <li>{@link #LOBBY} — entry room, no mobs, single door, icon forced to NONE.</li>
 *   <li>{@link #COMBAT} — fight room, doors locked until all mobs dead, icon rolled at door proposal.</li>
 *   <li>{@link #BOSS} — boss room every 10th index, single exit, fixed icon, triggers checkpoint in Infinite.</li>
 * </ul>
 */
public enum RoomType {
    LOBBY,
    COMBAT,
    BOSS
}
