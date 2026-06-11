package fr.perrier.dungeons.common.utils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Position {
    private double x, y, z;

    /**
     * Optional facing applied on teleport (spawn). Defaults to 0 ; ignored
     * where rotation is meaningless. Old payloads without these fields
     * deserialize to 0. Mirrors {@code LabyrinthRoom.Vec3}.
     */
    private float yaw;
    private float pitch;

    public Position(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Position(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @Override
    public String toString() {
        return "Position{" +
               "x=" + x +
               ", y=" + y +
               ", z=" + z +
               ", yaw=" + yaw +
               ", pitch=" + pitch +
               '}';
    }
}
