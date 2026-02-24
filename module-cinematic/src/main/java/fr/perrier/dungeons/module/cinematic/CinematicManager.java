package fr.perrier.dungeons.module.cinematic;

import fr.perrier.dungeons.module.cinematic.action.CinematicAction;
import fr.perrier.dungeons.module.cinematic.actions.blind.BlindSegment;
import fr.perrier.dungeons.module.cinematic.actions.blind.CinematicBlindAction;
import fr.perrier.dungeons.module.cinematic.actions.camera.CameraMoveAction;
import fr.perrier.dungeons.module.cinematic.actions.camera.CameraSegment;
import fr.perrier.dungeons.module.cinematic.actions.message.CinematicMessageAction;
import fr.perrier.dungeons.module.cinematic.actions.message.MessageSegment;
import fr.perrier.dungeons.module.cinematic.actions.sound.CinematicSoundAction;
import fr.perrier.dungeons.module.cinematic.actions.sound.SoundSegment;
import fr.perrier.dungeons.module.cinematic.actions.title.CinematicTitleAction;
import fr.perrier.dungeons.module.cinematic.actions.title.TitleSegment;
import fr.perrier.dungeons.module.cinematic.clock.CinematicClock;
import fr.perrier.dungeons.module.cinematic.executor.CinematicExecutor;
import fr.perrier.dungeons.module.cinematic.interpolation.PositionInterpolator;
import fr.perrier.dungeons.module.cinematic.model.CameraWaypoint;
import fr.perrier.dungeons.module.cinematic.model.CinematicData;
import fr.perrier.dungeons.module.cinematic.model.TimelineEvent;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère les données cinématiques (waypoints), l'état de lecture et le timing.
 *
 * <p>Workflow (système waypoints simple) :
 * <ol>
 *   <li>Les actions {@code cinematic_add_camera_waypoint} alimentent un {@link CinematicData}</li>
 *   <li>{@code cinematic_start} pré-calcule le chemin Catmull-Rom, spawn un {@link ItemDisplay},
 *       passe le joueur en SPECTATOR et lui fait spectater l'entité</li>
 *   <li>Un {@link BukkitTask} à 1 tick avance la position de l'entité le long du chemin</li>
 *   <li>À la fin, l'entité est supprimée et le joueur est restauré</li>
 * </ol>
 *
 * <p>Le système segment-based (segment actions) utilise {@link CinematicExecutor} +
 * {@link fr.perrier.dungeons.module.cinematic.actions.camera.CameraMoveAction} en parallèle.</p>
 */
public class CinematicManager {

    /** Cinematic definitions keyed by cinematic ID */
    private final Map<String, CinematicData> cinematics = new ConcurrentHashMap<>();

    /** Active playback sessions keyed by player UUID */
    private final Map<UUID, ActiveSession> activeSessions = new ConcurrentHashMap<>();

    /** Active segment-based cinematic executors keyed by player UUID */
    private final Map<UUID, CinematicExecutor> activeExecutors = new ConcurrentHashMap<>();


    /** Callback to invoke when a cinematic ends (for trigger system) */
    private Runnable onCinematicEndCallback;

    /** Real-time cinematic clock for segment-based actions */
    @Setter
    private CinematicClock cinematicClock;

    private static final Gson GSON = new Gson();

    /**
     * Get or create a CinematicData for the given ID.
     */
    public CinematicData getOrCreate(String cinematicId) {
        return cinematics.computeIfAbsent(cinematicId, id -> {
            CinematicData data = new CinematicData(id, "workflow");
            data.setName(id);
            return data;
        });
    }

    /**
     * Add a camera waypoint to a cinematic.
     */
    public void addCameraWaypoint(String cinematicId, int tick, double x, double y, double z,
                                   float yaw, float pitch, CameraWaypoint.InterpolationMode interpolation) {
        CinematicData data = getOrCreate(cinematicId);
        CameraWaypoint wp = new CameraWaypoint(tick, x, y, z, yaw, pitch);
        wp.setInterpolation(interpolation);
        data.getCameraWaypoints().add(wp);

        // Sort waypoints by tick and update duration
        data.getCameraWaypoints().sort(Comparator.comparingInt(CameraWaypoint::getTick));
        int maxTick = data.getCameraWaypoints().stream()
                .mapToInt(CameraWaypoint::getTick)
                .max().orElse(0);
        if (maxTick > data.getDurationTicks()) {
            data.setDurationTicks(maxTick);
        }
    }

    /**
     * Start cinematic playback for a player.
     * Pré-calcule le chemin Catmull-Rom, spawn un ItemDisplay, passe le joueur
     * en SPECTATOR et advance la caméra à chaque tick Bukkit.
     * (ref: didacculo/CinematicManager.playCinematicInternal)
     */
    public boolean startCinematic(String cinematicId, Player player) {
        CinematicData data = cinematics.get(cinematicId);
        if (data == null || data.getCameraWaypoints().isEmpty()) {
            System.out.println("[Cinematic] Aucun waypoint pour '" + cinematicId + "', impossible de démarrer");
            return false;
        }

        // Stop any existing cinematic for this player
        stopCinematic(player);

        Plugin plugin = Bukkit.getPluginManager().getPlugin("NextDungeon");
        if (plugin == null) {
            System.out.println("[Cinematic] Plugin NextDungeon introuvable");
            return false;
        }

        List<CameraWaypoint> waypoints = data.getCameraWaypoints();
        if (waypoints.size() < 1) return false;

        // Nombre total de ticks
        int totalTicks = data.getDurationTicks();
        int transitions = Math.max(1, waypoints.size() - 1);
        int stepsPerTransition = Math.max(1, totalTicks / transitions);

        // Pré-calcul du chemin Catmull-Rom (inclut ajout des waypoints fantômes)
        // On duplique premier et dernier point comme dans didacculo pour que
        // CatmullRom ait toujours 4 points autour de chaque segment
        List<CameraWaypoint> extendedWaypoints = new ArrayList<>();
        extendedWaypoints.add(waypoints.get(0));
        extendedWaypoints.addAll(waypoints);
        extendedWaypoints.add(waypoints.get(waypoints.size() - 1));

        final List<CameraWaypoint> smoothPath =
                PositionInterpolator.interpolatePath(extendedWaypoints, stepsPerTransition);

        if (smoothPath.isEmpty()) return false;

        // Sauvegarder l'état du joueur
        final GameMode originalMode = player.getGameMode();
        final Location originalLocation = player.getLocation().clone();

        // Préparer le premier point
        CameraWaypoint first = smoothPath.get(0);
        Location startLoc = new Location(player.getWorld(),
                first.getX(), first.getY(), first.getZ(),
                first.getYaw(), first.getPitch());

        // Spawner l'entité ItemDisplay (ref: didacculo — ItemDisplay avec teleportDuration=4)
        ItemDisplay camera = (ItemDisplay) player.getWorld().spawnEntity(startLoc, EntityType.ITEM_DISPLAY);
        camera.setItemStack(new ItemStack(Material.AIR));
        camera.setBillboard(Display.Billboard.FIXED);
        camera.setTeleportDuration(4);

        // Passer le joueur en SPECTATOR et lui faire spectater la caméra
        // TP d'abord le joueur sur l'entité (setSpectatorTarget requiert proximité)
        player.teleport(startLoc);
        player.setGameMode(GameMode.SPECTATOR);
        player.setSpectatorTarget(camera);

        final int[] tickRef = {0};

        // BukkitTask à 1 tick (ref: didacculo runTaskTimer 0L, 1L)
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !activeSessions.containsKey(player.getUniqueId())) {
                camera.remove();
                return;
            }

            if (tickRef[0] >= smoothPath.size()) {
                // Fin de la cinématique
                camera.remove();
                activeSessions.remove(player.getUniqueId());
                if (player.isOnline()) {
                    player.setSpectatorTarget(null);
                    player.setGameMode(originalMode);
                    player.teleport(originalLocation);
                }
                System.out.println("[Cinematic] Cinématique '" + cinematicId + "' terminée pour " + player.getName());
                return;
            }

            CameraWaypoint wp = smoothPath.get(tickRef[0]);
            Location target = new Location(player.getWorld(),
                    wp.getX(), wp.getY(), wp.getZ(),
                    wp.getYaw(), wp.getPitch());
            camera.teleport(target);

            // Re-vérifier spectating toutes les 10 ticks (ref: didacculo tick % 10)
            if (tickRef[0] % 10 == 0) {
                if (player.getGameMode() != GameMode.SPECTATOR) {
                    player.setGameMode(GameMode.SPECTATOR);
                }
                if (player.getSpectatorTarget() != camera) {
                    // Re-TP sur l'entité avant de re-spectater
                    player.teleport(target);
                    player.setSpectatorTarget(camera);
                }
            }

            tickRef[0]++;
        }, 0L, 1L);

        activeSessions.put(player.getUniqueId(),
                new ActiveSession(task, camera, cinematicId, originalMode, originalLocation));
        System.out.println("[Cinematic] Démarrage de '" + cinematicId + "' pour " + player.getName()
                + " (" + waypoints.size() + " waypoints, " + smoothPath.size() + " points, " + totalTicks + " ticks)");
        return true;
    }

    /**
     * Stop the currently playing cinematic for a player.
     */
    public void stopCinematic(Player player) {
        ActiveSession session = activeSessions.remove(player.getUniqueId());
        if (session != null) {
            session.task.cancel();
            if (session.camera != null && !session.camera.isDead()) {
                session.camera.remove();
            }
            if (player.isOnline()) {
                player.setSpectatorTarget(null);
                player.setGameMode(session.originalMode);
                player.teleport(session.originalLocation);
            }
            System.out.println("[Cinematic] Cinématique stoppée pour " + player.getName());
        }
    }

    /**
     * Check if a player has an active cinematic.
     */
    public boolean isPlaying(UUID playerId) {
        return activeSessions.containsKey(playerId);
    }

    /**
     * Check if a player has an active cinematic with the given ID.
     */
    public boolean isPlaying(UUID playerId, String cinematicId) {
        ActiveSession session = activeSessions.get(playerId);
        return session != null
                && (cinematicId == null || cinematicId.isEmpty() || cinematicId.equals(session.cinematicId));
    }

    /**
     * Shutdown: cancel all active cinematics.
     */
    public void shutdown() {
        for (ActiveSession session : activeSessions.values()) {
            session.task.cancel();
            if (session.camera != null && !session.camera.isDead()) {
                session.camera.remove();
            }
        }
        activeSessions.clear();

        // Stop all segment-based executors
        for (CinematicExecutor exec : activeExecutors.values()) {
            exec.stop();
        }
        activeExecutors.clear();

        cinematics.clear();
    }

    // ========== Segment-Based Cinematic Action Execution ==========

    /**
     * Execute a camera move action using the segment-based system.
     */
    public boolean executeCameraMove(Player player, int startFrame, int endFrame, String pathPointsJson) {
        if (cinematicClock == null) return false;

        List<CameraWaypoint> waypoints = parseWaypointsFromJson(pathPointsJson);
        if (waypoints.isEmpty()) return false;

        CameraSegment segment = new CameraSegment(startFrame, endFrame, waypoints);
        CameraMoveAction action = new CameraMoveAction();
        action.getCinematicSegments().add(segment);

        return startSegmentAction(player, action);
    }

    /**
     * Execute a title action using the segment-based system.
     */
    public boolean executeTitle(Player player, int startFrame, int endFrame,
                                String title, String subtitle, int fadeIn, int fadeOut) {
        if (cinematicClock == null) return false;

        TitleSegment segment = new TitleSegment(startFrame, endFrame, title, subtitle, fadeIn, fadeOut);
        CinematicTitleAction action = new CinematicTitleAction();
        action.getCinematicSegments().add(segment);

        return startSegmentAction(player, action);
    }

    /**
     * Execute a sound action using the segment-based system.
     */
    public boolean executeSound(Player player, int startFrame, int endFrame,
                                String soundType, float volume, float pitch) {
        if (cinematicClock == null) return false;

        SoundSegment segment = new SoundSegment(startFrame, endFrame, soundType, volume, pitch);
        CinematicSoundAction action = new CinematicSoundAction();
        action.getCinematicSegments().add(segment);

        return startSegmentAction(player, action);
    }

    /**
     * Execute a message action using the segment-based system.
     */
    public boolean executeMessage(Player player, int startFrame, int endFrame,
                                  String message, String displayType) {
        if (cinematicClock == null) return false;

        MessageSegment segment = new MessageSegment(startFrame, endFrame, message, displayType);
        CinematicMessageAction action = new CinematicMessageAction();
        action.getCinematicSegments().add(segment);

        return startSegmentAction(player, action);
    }

    /**
     * Execute a blind action using the segment-based system.
     */
    public boolean executeBlind(Player player, int startFrame, int endFrame) {
        if (cinematicClock == null) return false;

        BlindSegment segment = new BlindSegment(startFrame, endFrame);
        CinematicBlindAction action = new CinematicBlindAction();
        action.getCinematicSegments().add(segment);

        return startSegmentAction(player, action);
    }

    /**
     * Start a segment-based cinematic action via the CinematicExecutor.
     */
    private boolean startSegmentAction(Player player, CinematicAction action) {
        // Stop any existing segment-based executor for this player
        CinematicExecutor existing = activeExecutors.remove(player.getUniqueId());
        if (existing != null && existing.isRunning()) {
            existing.stop();
        }

        CinematicExecutor exec = new CinematicExecutor(List.of(action), player, cinematicClock);
        activeExecutors.put(player.getUniqueId(), exec);
        exec.start();
        return true;
    }

    /**
     * Parse camera waypoints from a JSON array string.
     */
    private List<CameraWaypoint> parseWaypointsFromJson(String json) {
        List<CameraWaypoint> waypoints = new ArrayList<>();
        try {
            JsonArray arr = GSON.fromJson(json, JsonArray.class);
            if (arr == null) return waypoints;
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                double x = obj.has("x") ? obj.get("x").getAsDouble() : 0;
                double y = obj.has("y") ? obj.get("y").getAsDouble() : 64;
                double z = obj.has("z") ? obj.get("z").getAsDouble() : 0;
                float yaw = obj.has("yaw") ? obj.get("yaw").getAsFloat() : 0;
                float pitch = obj.has("pitch") ? obj.get("pitch").getAsFloat() : 0;
                int tick = obj.has("tick") ? obj.get("tick").getAsInt() : 0;
                waypoints.add(new CameraWaypoint(tick, x, y, z, yaw, pitch));
            }
        } catch (Exception e) {
            System.err.println("[Cinematic] Failed to parse waypoints JSON: " + e.getMessage());
        }
        return waypoints;
    }

    private record ActiveSession(BukkitTask task, ItemDisplay camera, String cinematicId,
                                  GameMode originalMode, Location originalLocation) {}
}
