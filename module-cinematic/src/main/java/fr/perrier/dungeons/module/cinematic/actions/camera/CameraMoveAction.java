package fr.perrier.dungeons.module.cinematic.actions.camera;

import fr.perrier.dungeons.module.cinematic.CinematicManager;
import fr.perrier.dungeons.module.cinematic.CinematicModule;
import fr.perrier.dungeons.module.cinematic.action.SimpleCinematicAction;
import fr.perrier.dungeons.module.cinematic.interpolation.PositionInterpolator;
import fr.perrier.dungeons.module.cinematic.model.CameraWaypoint;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Action de mouvement de caméra cinématique.
 * <p>
 * Fonctionnement (inspiré de didacculo/CinematicManager.java) :
 * <ol>
 *   <li>Le chemin Catmull-Rom est pré-calculé au démarrage du segment</li>
 *   <li>Un {@link ItemDisplay} (AIR) est spawné avec {@code teleportDuration=4}
 *       pour que le client interpole fluidement entre les positions</li>
 *   <li>Le joueur passe en mode SPECTATOR et spectate l'entité via
 *       {@link Player#setSpectatorTarget(org.bukkit.entity.Entity)}</li>
 *   <li>À chaque tick : l'entité est téléportée au prochain point du chemin</li>
 *   <li>Toutes les 10 ticks : on re-vérifie que le joueur spectate toujours l'entité</li>
 *   <li>À la fin : entité supprimée, joueur restauré dans son mode précédent</li>
 * </ol>
 */
public class CameraMoveAction extends SimpleCinematicAction<CameraSegment> {

    /** Hauteur des yeux du joueur ajoutée à Y pour le point de vue caméra */
    private static final double EYE_HEIGHT = 1.6;

    private final List<CameraSegment> segments = new ArrayList<>();

    // État transient du segment actif
    private transient ItemDisplay cameraEntity = null;
    private transient List<CameraWaypoint> smoothPath = null;
    private transient int pathIndex = 0;
    private transient GameMode originalGameMode = null;
    private transient int tickCounter = 0;

    // ── Cycle de vie des segments ────────────────────────────────────────────

    @Override
    protected void onSegmentStart(Player player, CameraSegment segment) {
        List<CameraWaypoint> waypoints = segment.getPathPoints();
        if (waypoints == null || waypoints.isEmpty()) return;

        int segmentDuration = Math.max(1, segment.getEndFrame() - segment.getStartFrame());
        int transitions = Math.max(1, waypoints.size() - 1);
        int stepsPerTransition = Math.max(1, segmentDuration / transitions);

        // Pré-calculer le chemin Catmull-Rom
        smoothPath = PositionInterpolator.interpolatePath(waypoints, stepsPerTransition);
        pathIndex = 0;
        tickCounter = 0;

        if (smoothPath.isEmpty()) return;

        // Sauvegarder le mode de jeu original
        originalGameMode = player.getGameMode();

        // Spawner l'entité ItemDisplay et démarrer le spectating sur le thread principal
        CameraWaypoint first = smoothPath.get(0);
        Location startLoc = new Location(
                player.getWorld(),
                first.getX(), first.getY() + EYE_HEIGHT, first.getZ(),
                first.getYaw(), first.getPitch()
        );

        Plugin plugin = Bukkit.getPluginManager().getPlugin("NextDungeon");
        if (plugin == null) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                // Spawner l'entité caméra (ItemDisplay invisible)
                cameraEntity = Objects.requireNonNull(startLoc.getWorld()).spawn(startLoc, ItemDisplay.class, (entity) -> {
                    entity.setItemStack(new ItemStack(Material.AIR));
                    entity.setTeleportDuration(4);
                    entity.setBillboard(Display.Billboard.FIXED);
                });
                player.setGameMode(GameMode.SPECTATOR);
                player.setSpectatorTarget(null);
                player.teleport(cameraEntity, PlayerTeleportEvent.TeleportCause.SPECTATE);
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,()->{
                    player.setSpectatorTarget(cameraEntity);
                },8);

            } catch (Exception e) {
                System.err.println("[Cinematic] Erreur lors du démarrage de la caméra : " + e.getMessage());
            }
        });
    }

    @Override
    protected void onSegmentTick(Player player, CameraSegment segment, int frame) {
        if (smoothPath == null || cameraEntity == null) return;
        if (pathIndex >= smoothPath.size()) return;

        CameraWaypoint wp = smoothPath.get(pathIndex++);
        Location loc = new Location(
                player.getWorld(),
                wp.getX(), wp.getY() + EYE_HEIGHT, wp.getZ(),
                wp.getYaw(), wp.getPitch()
        );

        Plugin plugin = Bukkit.getPluginManager().getPlugin("NextDungeon");
        if (plugin == null) return;

        final ItemDisplay entity = cameraEntity;
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                entity.teleport(loc);

                // Re-vérification périodique du spectatorTarget (ref: didacculo tick % 10)
                tickCounter++;
                if (tickCounter % 10 == 0) {
                    if (player.isOnline() && player.getSpectatorTarget() != entity) {
                        if (player.getGameMode() != GameMode.SPECTATOR) {
                            player.setGameMode(GameMode.SPECTATOR);
                        }
                        // Re-TP le joueur sur l'entité pour que setSpectatorTarget fonctionne
                        player.teleport(loc);
                        player.setSpectatorTarget(entity);
                    }
                }
            } catch (Exception e) {
                System.err.println("[Cinematic] Erreur lors du tick caméra : " + e.getMessage());
            }
        });
    }

    @Override
    protected void onSegmentStop(Player player, CameraSegment segment) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("NextDungeon");
        final ItemDisplay entity = cameraEntity;
        final GameMode mode = originalGameMode;

        // Nettoyer l'état transient immédiatement
        cameraEntity = null;
        smoothPath = null;
        pathIndex = 0;
        tickCounter = 0;
        originalGameMode = null;

        if (plugin == null) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                // Libérer le spectating
                if (player.isOnline()) {
                    player.setSpectatorTarget(null);
                    player.setGameMode(mode != null ? mode : GameMode.SURVIVAL);
                }
                // Supprimer l'entité caméra
                if (entity != null && !entity.isDead()) {
                    entity.remove();
                }
            } catch (Exception e) {
                System.err.println("[Cinematic] Erreur lors de l'arrêt de la caméra : " + e.getMessage());
            }
        });
    }

    @Override
    public List<CameraSegment> getCinematicSegments() {
        return segments;
    }
}
