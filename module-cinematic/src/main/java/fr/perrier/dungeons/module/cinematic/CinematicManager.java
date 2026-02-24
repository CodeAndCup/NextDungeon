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
import fr.perrier.dungeons.module.cinematic.execution.CinematicPlayer;
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
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages cinematic data (waypoints), playback state, and precise timing.
 *
 * <p>Workflow:
 * <ol>
 *   <li>{@code cinematic_add_camera_waypoint} actions populate waypoints in a {@link CinematicData}</li>
 *   <li>{@code cinematic_start} creates a {@link CinematicPlayer} and schedules updates via ScheduledExecutorService</li>
 *   <li>Each update, the player is teleported to the interpolated camera position</li>
 *   <li>On completion, the task is cancelled and the player's game mode is restored</li>
 * </ol>
 *
 * <p>Uses ScheduledExecutorService for precise timing (microsecond resolution) instead of Bukkit ticks,
 * allowing smooth camera movement with configurable frame rate.</p>
 */
public class CinematicManager {

    // ========== SMOOTHNESS CONFIGURATION ==========
    /** Update frequency in microseconds */
    private static final long UPDATE_INTERVAL_MICROSECONDS = 33333L;

    /** Cinematic definitions keyed by cinematic ID */
    private final Map<String, CinematicData> cinematics = new ConcurrentHashMap<>();

    /** Active playback sessions keyed by player UUID */
    private final Map<UUID, ActiveSession> activeSessions = new ConcurrentHashMap<>();

    /** Active segment-based cinematic executors keyed by player UUID */
    private final Map<UUID, CinematicExecutor> activeExecutors = new ConcurrentHashMap<>();

    /** Executor for precise timing of cinematic updates */
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Cinematic-Player");
        t.setDaemon(true);
        return t;
    });

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
     */
    public boolean startCinematic(String cinematicId, Player player) {
        CinematicData data = cinematics.get(cinematicId);
        if (data == null || data.getCameraWaypoints().isEmpty()) {
            System.out.println("[Cinematic] No waypoints found for cinematic '" + cinematicId + "', cannot start");
            return false;
        }

        // Stop any existing cinematic for this player
        stopCinematic(player);

        Plugin plugin = Bukkit.getPluginManager().getPlugin("NextDungeon");
        if (plugin == null) {
            System.out.println("[Cinematic] NextDungeon plugin not found, cannot schedule cinematic");
            return false;
        }

        // Store original game mode for restoration
        GameMode originalMode = player.getGameMode();
        Location originalLocation = player.getLocation().clone();
        final org.bukkit.World playerWorld = player.getWorld();

        // Set player to spectator mode for free camera movement
        player.setGameMode(GameMode.SPECTATOR);

        CinematicPlayer cinematicPlayer = new CinematicPlayer(player.getUniqueId(), data, new CinematicPlayer.PlaybackCallback() {
            @Override
            public void setCameraPosition(double x, double y, double z, float yaw, float pitch) {
                if (player.isOnline()) {
                    Location loc = new Location(playerWorld, x, y, z, yaw, pitch);
                    player.teleport(loc);
                }
            }

            @Override
            public void moveNpc(String actorId, double x, double y, double z, float yaw, float pitch, String animation) {
                // NPC movement - can be implemented with NPC library integration
            }

            @Override
            public void fireEvent(TimelineEvent event) {
                if (!player.isOnline()) return;
                if (event.getType() == null) return;

                switch (event.getType().toUpperCase()) {
                    case "COMMAND" -> {
                        String cmd = event.getParameters().getOrDefault("value", "");
                        if (!cmd.isEmpty()) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                    cmd.replace("{player}", player.getName()));
                        }
                    }
                    case "TITLE" -> {
                        String title = event.getParameters().getOrDefault("value", "");
                        player.sendTitle(title, "", 10, 40, 10);
                    }
                    case "SOUND" -> {
                        String sound = event.getParameters().getOrDefault("value", "");
                        if (!sound.isEmpty()) {
                            player.playSound(player.getLocation(), sound, 1f, 1f);
                        }
                    }
                }
            }

            @Override
            public void onComplete() {
                // Restore player state
                ActiveSession session = activeSessions.remove(player.getUniqueId());
                if (session != null) {
                    session.future.cancel(false);
                }
                if (player.isOnline()) {
                    player.setGameMode(originalMode);
                    player.teleport(originalLocation);
                }
                System.out.println("[Cinematic] Cinematic '" + cinematicId + "' completed for " + player.getName());
            }
        });

        cinematicPlayer.start();

        // Schedule updates with precise timing using ScheduledExecutorService
        // UPDATE_INTERVAL_MICROSECONDS controls the update frequency
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> {
            // Always run cinematic updates on the Bukkit thread for safety
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) {
                    stopCinematic(player);
                    return;
                }
                cinematicPlayer.tick();
            });
        }, 0, UPDATE_INTERVAL_MICROSECONDS, TimeUnit.MICROSECONDS);

        activeSessions.put(player.getUniqueId(), new ActiveSession(cinematicPlayer, future, cinematicId));
        System.out.println("[Cinematic] Started cinematic '" + cinematicId + "' for " + player.getName()
                + " (" + data.getCameraWaypoints().size() + " waypoints, " + data.getDurationTicks() + " ticks)");
        return true;
    }

    /**
     * Stop the currently playing cinematic for a player.
     */
    public void stopCinematic(Player player) {
        ActiveSession session = activeSessions.remove(player.getUniqueId());
        if (session != null) {
            session.future.cancel(false);
            session.player.cancel();
            System.out.println("[Cinematic] Stopped cinematic for " + player.getName());
        }
    }

    /**
     * Check if a player has an active cinematic.
     */
    public boolean isPlaying(UUID playerId) {
        ActiveSession session = activeSessions.get(playerId);
        return session != null && session.player.isPlaying();
    }

    /**
     * Check if a player has an active cinematic with the given ID.
     */
    public boolean isPlaying(UUID playerId, String cinematicId) {
        ActiveSession session = activeSessions.get(playerId);
        return session != null && session.player.isPlaying()
                && (cinematicId == null || cinematicId.isEmpty() || cinematicId.equals(session.cinematicId));
    }

    /**
     * Shutdown: cancel all active cinematics and stop the executor service.
     */
    public void shutdown() {
        for (ActiveSession session : activeSessions.values()) {
            session.player.cancel();
            session.future.cancel(false);
        }
        activeSessions.clear();

        // Stop all segment-based executors
        for (CinematicExecutor exec : activeExecutors.values()) {
            exec.stop();
        }
        activeExecutors.clear();

        cinematics.clear();
        executor.shutdown();
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

    private record ActiveSession(CinematicPlayer player, ScheduledFuture<?> future, String cinematicId) {}
}
