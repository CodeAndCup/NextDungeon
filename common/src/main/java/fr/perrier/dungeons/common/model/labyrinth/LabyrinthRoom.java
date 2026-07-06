package fr.perrier.dungeons.common.model.labyrinth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A labyrinth room template — the building block of a procedural run.
 *
 * <p>Lives inside a {@link LabyrinthDungeonConfig}'s shared pool ; the
 * picker filters rooms by tag so each floor (= difficulty) can pick
 * its own subset (CDC §4.1, Q3 = pool unique + tags).</p>
 *
 * <p>{@code fixedIcon} semantics :
 * <ul>
 *   <li>{@code COMBAT} → {@code null} (icon rolled at door proposal time)</li>
 *   <li>{@code LOBBY}  → {@link RewardIcon#NONE} (forced)</li>
 *   <li>{@code BOSS}   → non-null fixed icon, defined by the admin</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
public class LabyrinthRoom implements Serializable {

    private String id;
    private RoomType type;
    private String worldId;
    private Region region;
    private Vec3 playerSpawn;

    /**
     * Optional. When present, the entry point at which players are
     * teleported when arriving from the previous room. Defaults to
     * {@link #playerSpawn} when null.
     */
    private Vec3 entryDoor;

    private List<Door> exitDoors = new ArrayList<>();
    private List<MobSpawn> mobSpawns = new ArrayList<>();
    private List<String> tags = new ArrayList<>();
    private RewardIcon fixedIcon;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Region implements Serializable {
        private Vec3 min;
        private Vec3 max;

        public Region(Vec3 min, Vec3 max) {
            this.min = min;
            this.max = max;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Vec3 implements Serializable {
        private double x;
        private double y;
        private double z;
        /**
         * Optional facing applied on teleport (dungeon spawn / player spawn).
         * Defaults to 0 ; ignored where rotation is meaningless (region corners,
         * mob/door anchors). Old payloads without these fields deserialize to 0.
         */
        private float yaw;
        private float pitch;

        public Vec3(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Vec3(double x, double y, double z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    /**
     * A door anchor in the room ; the picker uses it as the exit from
     * this room, the {@code DoorController} positions the icon
     * hologram and the lock/unlock state, and the player traversal
     * triggers the transition.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Door implements Serializable {
        private String id;
        private Vec3 anchor;

        /**
         * Optional override for where the reward icon hologram is rendered.
         * When {@code null} the icon falls back to {@code anchor} lifted ~2
         * blocks (legacy behavior). When set, the icon is placed at these
         * coords (block-centered on X/Z, exact Y) — independent of the
         * {@code anchor} traversal-detection point. Old payloads without
         * this field deserialize to {@code null}.
         */
        private Vec3 iconAnchor;

        public Door(String id, Vec3 anchor) {
            this.id = id;
            this.anchor = anchor;
        }
    }

    /**
     * A mob spawn directive. {@code mobId} references a mob from the
     * host server's MMOCore / MythicMobs configuration ; the module
     * applies tier scaling at spawn time.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class MobSpawn implements Serializable {
        private String mobId;
        private double x;
        private double y;
        private double z;
        private int count;

        public MobSpawn(String mobId, double x, double y, double z, int count) {
            this.mobId = mobId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.count = count;
        }
    }
}
