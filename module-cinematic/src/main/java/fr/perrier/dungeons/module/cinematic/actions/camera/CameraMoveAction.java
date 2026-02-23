package fr.perrier.dungeons.module.cinematic.actions.camera;

import fr.perrier.dungeons.module.cinematic.action.SimpleCinematicAction;
import fr.perrier.dungeons.module.cinematic.interpolation.PositionInterpolator;
import fr.perrier.dungeons.module.cinematic.model.CameraWaypoint;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Action de mouvement de caméra fluide utilisant l'interpolation Catmull-Rom.
 * <p>
 * Technique:
 * 1. Calcul du chemin interpolé au démarrage du segment
 * 2. Chaque frame: interpoler la position sur le chemin
 * 3. Tous les 10 frames: téléporter le joueur réel (pour le chargement de chunks)
 */
public class CameraMoveAction extends SimpleCinematicAction<CameraSegment> {

    private List<CameraSegment> segments = new ArrayList<>();
    private final transient Map<CameraSegment, List<CameraWaypoint>> segmentPaths = new HashMap<>();

    @Override
    protected void onSegmentStart(Player player, CameraSegment segment) throws Exception {
        // Calculer chemin interpolé avec Catmull-Rom
        List<CameraWaypoint> path = PositionInterpolator.interpolatePath(
                segment.getPathPoints(),
                20 // 20 points par segment pour lisser
        );
        segmentPaths.put(segment, path);

        // Téléporter joueur au point de départ
        if (!path.isEmpty()) {
            CameraWaypoint start = path.get(0);
            Location startLoc = new Location(player.getWorld(),
                    start.getX(), start.getY(), start.getZ(),
                    start.getYaw(), start.getPitch());
            player.teleport(startLoc);
        }

        // Setup joueur pour le vol
        player.setAllowFlight(true);
        player.setFlying(true);
    }

    @Override
    protected void onSegmentTick(Player player, CameraSegment segment, int frame) throws Exception {
        List<CameraWaypoint> path = segmentPaths.get(segment);
        if (path == null || path.isEmpty()) return;

        double percentage = segment.getPercentageAt(frame);
        CameraWaypoint interpolated = PositionInterpolator.interpolateAt(path, percentage);

        Location loc = new Location(player.getWorld(),
                interpolated.getX(), interpolated.getY(), interpolated.getZ(),
                interpolated.getYaw(), interpolated.getPitch());

        // Téléporter joueur tous les 10 frames pour le chargement de chunks
        if (frame % 10 == 0) {
            player.teleport(loc);
        }
    }

    @Override
    protected void onSegmentStop(Player player, CameraSegment segment) throws Exception {
        segmentPaths.remove(segment);
    }

    @Override
    public List<CameraSegment> getCinematicSegments() {
        return segments;
    }
}
