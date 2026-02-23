package fr.perrier.dungeons.module.cinematic.interpolation;

import fr.perrier.dungeons.module.cinematic.model.CameraWaypoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Interpolation de positions utilisant l'algorithme Catmull-Rom.
 * <p>
 * Formule:
 * P(t) = 0.5 * [2*P1 + (-P0+P2)*t + (2*P0-5*P1+4*P2-P3)*t² + (-P0+3*P1-3*P2+P3)*t³]
 * <p>
 * Propriétés:
 * - C¹ continue (transition lisse, zéro jumps)
 * - Passe par tous les points de contrôle
 * - Appliqué par axe: X, Y, Z, Pitch, Yaw
 */
public class PositionInterpolator {

    private PositionInterpolator() {
    }

    /**
     * Interpole une liste de waypoints et génère un chemin lissé
     * avec Catmull-Rom entre chaque paire de waypoints.
     *
     * @param waypoints les points de contrôle (minimum 2)
     * @param stepsPerSegment nombre de points à générer entre chaque paire
     * @return la liste de waypoints interpolés
     */
    public static List<CameraWaypoint> interpolatePath(List<CameraWaypoint> waypoints, int stepsPerSegment) {
        if (waypoints == null || waypoints.size() < 2) {
            return new ArrayList<>(waypoints != null ? waypoints : List.of());
        }

        List<CameraWaypoint> result = new ArrayList<>();
        int n = waypoints.size();

        for (int i = 0; i < n - 1; i++) {
            // Points P0, P1, P2, P3 pour Catmull-Rom
            CameraWaypoint p0 = waypoints.get(Math.max(0, i - 1));
            CameraWaypoint p1 = waypoints.get(i);
            CameraWaypoint p2 = waypoints.get(Math.min(n - 1, i + 1));
            CameraWaypoint p3 = waypoints.get(Math.min(n - 1, i + 2));

            int steps = Math.max(1, stepsPerSegment);
            for (int step = 0; step < steps; step++) {
                double t = (double) step / steps;
                result.add(catmullRom(p0, p1, p2, p3, t));
            }
        }

        // Ajouter le dernier point
        result.add(waypoints.get(n - 1));
        return result;
    }

    /**
     * Interpole entre les waypoints du chemin à un pourcentage donné
     *
     * @param path le chemin de waypoints
     * @param percentage valeur entre 0.0 et 1.0
     * @return le waypoint interpolé
     */
    public static CameraWaypoint interpolateAt(List<CameraWaypoint> path, double percentage) {
        if (path == null || path.isEmpty()) {
            return new CameraWaypoint();
        }
        if (path.size() == 1 || percentage <= 0.0) {
            return path.get(0);
        }
        if (percentage >= 1.0) {
            return path.get(path.size() - 1);
        }

        double index = percentage * (path.size() - 1);
        int lower = (int) index;
        int upper = Math.min(lower + 1, path.size() - 1);
        double t = index - lower;

        return lerp(path.get(lower), path.get(upper), t);
    }

    /**
     * Interpolation Catmull-Rom entre 4 waypoints
     */
    private static CameraWaypoint catmullRom(CameraWaypoint p0, CameraWaypoint p1,
                                              CameraWaypoint p2, CameraWaypoint p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;

        double x = 0.5 * (2 * p1.getX() + (-p0.getX() + p2.getX()) * t
                + (2 * p0.getX() - 5 * p1.getX() + 4 * p2.getX() - p3.getX()) * t2
                + (-p0.getX() + 3 * p1.getX() - 3 * p2.getX() + p3.getX()) * t3);

        double y = 0.5 * (2 * p1.getY() + (-p0.getY() + p2.getY()) * t
                + (2 * p0.getY() - 5 * p1.getY() + 4 * p2.getY() - p3.getY()) * t2
                + (-p0.getY() + 3 * p1.getY() - 3 * p2.getY() + p3.getY()) * t3);

        double z = 0.5 * (2 * p1.getZ() + (-p0.getZ() + p2.getZ()) * t
                + (2 * p0.getZ() - 5 * p1.getZ() + 4 * p2.getZ() - p3.getZ()) * t2
                + (-p0.getZ() + 3 * p1.getZ() - 3 * p2.getZ() + p3.getZ()) * t3);

        float yaw = (float) (0.5 * (2 * p1.getYaw() + (-p0.getYaw() + p2.getYaw()) * t
                + (2 * p0.getYaw() - 5 * p1.getYaw() + 4 * p2.getYaw() - p3.getYaw()) * t2
                + (-p0.getYaw() + 3 * p1.getYaw() - 3 * p2.getYaw() + p3.getYaw()) * t3));

        float pitch = (float) (0.5 * (2 * p1.getPitch() + (-p0.getPitch() + p2.getPitch()) * t
                + (2 * p0.getPitch() - 5 * p1.getPitch() + 4 * p2.getPitch() - p3.getPitch()) * t2
                + (-p0.getPitch() + 3 * p1.getPitch() - 3 * p2.getPitch() + p3.getPitch()) * t3));

        return new CameraWaypoint(0, x, y, z, yaw, pitch);
    }

    /**
     * Interpolation linéaire entre 2 waypoints
     */
    private static CameraWaypoint lerp(CameraWaypoint a, CameraWaypoint b, double t) {
        double x = a.getX() + (b.getX() - a.getX()) * t;
        double y = a.getY() + (b.getY() - a.getY()) * t;
        double z = a.getZ() + (b.getZ() - a.getZ()) * t;
        float yaw = (float) lerpAngle(a.getYaw(), b.getYaw(), t);
        float pitch = (float) lerpAngle(a.getPitch(), b.getPitch(), t);

        return new CameraWaypoint(0, x, y, z, yaw, pitch);
    }

    /**
     * Interpolates between two angles (in degrees), taking the shortest path.
     */
    private static double lerpAngle(double a, double b, double t) {
        double diff = ((b - a) % 360 + 540) % 360 - 180;
        return a + diff * t;
    }
}
