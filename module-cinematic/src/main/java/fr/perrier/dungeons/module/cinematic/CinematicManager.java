package fr.perrier.dungeons.module.cinematic;

import fr.perrier.dungeons.module.cinematic.execution.CinematicPlayer;
import fr.perrier.dungeons.module.cinematic.model.CameraWaypoint;
import fr.perrier.dungeons.module.cinematic.model.CinematicData;
import fr.perrier.dungeons.module.cinematic.model.TimelineEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages cinematic data (waypoints), playback state, and Bukkit scheduling.
 *
 * <p>Workflow:
 * <ol>
 *   <li>{@code cinematic_add_camera_waypoint} actions populate waypoints in a {@link CinematicData}</li>
 *   <li>{@code cinematic_start} creates a {@link CinematicPlayer} and schedules a repeating Bukkit task</li>
 *   <li>Each tick, the player is teleported to the interpolated camera position</li>
 *   <li>On completion, the task is cancelled and the player's game mode is restored</li>
 * </ol>
 */
public class CinematicManager {

    /** Cinematic definitions keyed by cinematic ID */
    private final Map<String, CinematicData> cinematics = new ConcurrentHashMap<>();

    /** Active playback sessions keyed by player UUID */
    private final Map<UUID, ActiveSession> activeSessions = new ConcurrentHashMap<>();

    /** Callback to invoke when a cinematic ends (for trigger system) */
    private Runnable onCinematicEndCallback;

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
        org.bukkit.World playerWorld = player.getWorld();

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
                    session.task.cancel();
                }
                if (player.isOnline()) {
                    player.setGameMode(originalMode);
                    player.teleport(originalLocation);
                }
                System.out.println("[Cinematic] Cinematic '" + cinematicId + "' completed for " + player.getName());
            }
        });

        cinematicPlayer.start();

        // Schedule a repeating task (every tick = 50ms)
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                stopCinematic(player);
                return;
            }
            cinematicPlayer.tick();
        }, 0L, 1L);

        activeSessions.put(player.getUniqueId(), new ActiveSession(cinematicPlayer, task, cinematicId));
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
            session.task.cancel();
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
     * Shutdown: cancel all active cinematics.
     */
    public void shutdown() {
        for (ActiveSession session : activeSessions.values()) {
            session.player.cancel();
            session.task.cancel();
        }
        activeSessions.clear();
        cinematics.clear();
    }

    private record ActiveSession(CinematicPlayer player, BukkitTask task, String cinematicId) {}
}
