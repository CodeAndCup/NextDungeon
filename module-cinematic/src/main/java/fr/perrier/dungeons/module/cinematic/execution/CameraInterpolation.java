package fr.perrier.dungeons.module.cinematic.execution;

import fr.perrier.dungeons.module.cinematic.model.CameraWaypoint;

/**
 * Provides interpolation methods for smooth camera movement between waypoints.
 */
public class CameraInterpolation {

    /**
     * Interpolate between two waypoints at the given progress (0.0 to 1.0).
     *
     * @param from     start waypoint
     * @param to       end waypoint
     * @param progress normalized progress [0, 1]
     * @param mode     interpolation mode
     * @return interpolated position/rotation as a double array [x, y, z, yaw, pitch]
     */
    public static double[] interpolate(CameraWaypoint from, CameraWaypoint to,
                                       double progress, CameraWaypoint.InterpolationMode mode) {
        return switch (mode) {
            case LINEAR -> linearInterpolate(from, to, progress);
            case CATMULL_ROM -> catmullRomInterpolate(from, to, progress);
            case CUBIC -> cubicInterpolate(from, to, progress);
        };
    }

    private static double[] linearInterpolate(CameraWaypoint from, CameraWaypoint to, double t) {
        return new double[]{
                lerp(from.getX(), to.getX(), t),
                lerp(from.getY(), to.getY(), t),
                lerp(from.getZ(), to.getZ(), t),
                lerpAngle(from.getYaw(), to.getYaw(), t),
                lerpAngle(from.getPitch(), to.getPitch(), t)
        };
    }

    private static double[] catmullRomInterpolate(CameraWaypoint from, CameraWaypoint to, double t) {
        // Catmull-Rom with tangent estimation using endpoints as control points
        double t2 = t * t;
        double t3 = t2 * t;

        // Hermite basis functions
        double h1 = 2 * t3 - 3 * t2 + 1;
        double h2 = -2 * t3 + 3 * t2;
        double h3 = t3 - 2 * t2 + t;
        double h4 = t3 - t2;

        // Tangent estimation (using finite differences with clamped endpoints)
        double tanScale = 0.5;
        double dxFrom = (to.getX() - from.getX()) * tanScale;
        double dyFrom = (to.getY() - from.getY()) * tanScale;
        double dzFrom = (to.getZ() - from.getZ()) * tanScale;
        double dxTo = dxFrom;
        double dyTo = dyFrom;
        double dzTo = dzFrom;

        return new double[]{
                h1 * from.getX() + h2 * to.getX() + h3 * dxFrom + h4 * dxTo,
                h1 * from.getY() + h2 * to.getY() + h3 * dyFrom + h4 * dyTo,
                h1 * from.getZ() + h2 * to.getZ() + h3 * dzFrom + h4 * dzTo,
                lerpAngle(from.getYaw(), to.getYaw(), t),
                lerpAngle(from.getPitch(), to.getPitch(), t)
        };
    }

    private static double[] cubicInterpolate(CameraWaypoint from, CameraWaypoint to, double t) {
        // Smoothstep cubic interpolation
        double smooth = t * t * (3 - 2 * t);
        return new double[]{
                lerp(from.getX(), to.getX(), smooth),
                lerp(from.getY(), to.getY(), smooth),
                lerp(from.getZ(), to.getZ(), smooth),
                lerpAngle(from.getYaw(), to.getYaw(), smooth),
                lerpAngle(from.getPitch(), to.getPitch(), smooth)
        };
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /**
     * Interpolates between two angles (in degrees), taking the shortest path.
     */
    private static double lerpAngle(double a, double b, double t) {
        double diff = ((b - a) % 360 + 540) % 360 - 180;
        return a + diff * t;
    }
}
