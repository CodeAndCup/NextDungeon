package fr.perrier.dungeons.module.labyrinth.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Persisted template describing a labyrinth room.
 *
 * <p>Pre-built rooms live in a static "labyrinth_pool" world ; the picker
 * teleports players between regions at runtime (CDC §2.3, v1 = static rooms).</p>
 *
 * <p>{@code fixedIcon} semantics (CDC §4.1) :
 * <ul>
 *   <li>{@code COMBAT} → {@code null} (icon rolled at door proposal time)</li>
 *   <li>{@code LOBBY}  → {@link RewardIcon#NONE} (forced)</li>
 *   <li>{@code BOSS}   → non-null fixed icon, defined by the admin</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
public class RoomTemplate implements Serializable {

    private String id;
    private RoomType type;
    private String worldId;
    private Region region;
    private Vec3 playerSpawn;
    private List<Door> doors = new ArrayList<>();
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

        public Vec3(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /**
     * A door anchor in the room ; the picker uses it as the exit from this
     * room, the {@code DoorController} positions the icon hologram and the
     * lock/unlock state, and the player traversal triggers the transition.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Door implements Serializable {
        private String id;
        private Vec3 anchor;

        public Door(String id, Vec3 anchor) {
            this.id = id;
            this.anchor = anchor;
        }
    }

    /**
     * A mob spawn directive. {@code mobId} references a mob from the host
     * server's MMOCore / MythicMobs configuration ; the module applies tier
     * scaling at spawn time (see {@link DifficultyModifier}).
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
