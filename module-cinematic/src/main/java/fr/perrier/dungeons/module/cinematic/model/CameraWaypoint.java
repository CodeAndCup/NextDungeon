package fr.perrier.dungeons.module.cinematic.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * A single camera waypoint on the cinematic timeline.
 * The engine interpolates between consecutive waypoints using
 * the specified interpolation method.
 */
@Getter
@Setter
public class CameraWaypoint implements Serializable {

    /** Tick offset from the start of the cinematic */
    private int tick;

    /** World position */
    private double x;
    private double y;
    private double z;

    /** Camera rotation (degrees) */
    private float yaw;
    private float pitch;

    /** Interpolation mode to reach the NEXT waypoint */
    private InterpolationMode interpolation = InterpolationMode.CATMULL_ROM;

    public enum InterpolationMode {
        LINEAR,
        CATMULL_ROM,
        CUBIC
    }

    public CameraWaypoint() {}

    public CameraWaypoint(int tick, double x, double y, double z, float yaw, float pitch) {
        this.tick = tick;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }
}
