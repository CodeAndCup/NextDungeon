package fr.perrier.dungeons.module.cinematic.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Describes an NPC actor that participates in a cinematic.
 * The NPC follows a path of movement waypoints synchronized to the timeline.
 */
@Getter
@Setter
public class NpcActor implements Serializable {

    /** Unique actor identifier within the cinematic */
    private String actorId;

    /** Display name shown above the NPC */
    private String displayName;

    /** NPC skin texture value (base64) */
    private String skinTexture;

    /** NPC skin signature */
    private String skinSignature;

    /** Movement waypoints for this NPC on the timeline */
    private List<NpcWaypoint> waypoints = new ArrayList<>();

    public NpcActor() {}

    public NpcActor(String actorId, String displayName) {
        this.actorId = actorId;
        this.displayName = displayName;
    }

    /**
     * A single NPC movement waypoint on the timeline.
     */
    @Getter
    @Setter
    public static class NpcWaypoint implements Serializable {

        /** Tick offset from cinematic start */
        private int tick;

        /** World position */
        private double x, y, z;

        /** NPC facing direction */
        private float yaw, pitch;

        /** Animation to play at this waypoint (IDLE, WALK, SWING_ARM, etc.) */
        private String animation;

        public NpcWaypoint() {}

        public NpcWaypoint(int tick, double x, double y, double z, float yaw, float pitch) {
            this.tick = tick;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}
