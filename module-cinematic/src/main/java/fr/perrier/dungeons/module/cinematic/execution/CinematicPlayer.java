package fr.perrier.dungeons.module.cinematic.execution;

import fr.perrier.dungeons.module.cinematic.model.CameraWaypoint;
import fr.perrier.dungeons.module.cinematic.model.CinematicData;
import fr.perrier.dungeons.module.cinematic.model.NpcActor;
import fr.perrier.dungeons.module.cinematic.model.TimelineEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Manages the playback of a single cinematic for one viewer.
 * Tracks current tick, performs interpolation, and fires timeline events.
 *
 * <p>This class is platform-agnostic — the actual Bukkit/NPC operations are
 * delegated via callback interfaces so the module can be tested independently.</p>
 */
public class CinematicPlayer {

    /**
     * Callback for applying interpolated camera positions and timeline events
     * to the actual game world (Bukkit API, NPC lib, etc.).
     */
    public interface PlaybackCallback {
        /** Set the viewer's camera position and rotation */
        void setCameraPosition(double x, double y, double z, float yaw, float pitch);

        /** Move an NPC actor to the given position */
        void moveNpc(String actorId, double x, double y, double z, float yaw, float pitch, String animation);

        /** Fire a timeline event */
        void fireEvent(TimelineEvent event);

        /** Called when the cinematic finishes */
        void onComplete();
    }

    private final UUID viewerId;
    private final CinematicData data;
    private final PlaybackCallback callback;

    private int currentTick;
    private boolean playing;
    private boolean cancelled;

    public CinematicPlayer(UUID viewerId, CinematicData data, PlaybackCallback callback) {
        this.viewerId = viewerId;
        this.data = data;
        this.callback = callback;
        this.currentTick = 0;
        this.playing = false;
        this.cancelled = false;
    }

    /**
     * Start playback.
     */
    public void start() {
        this.playing = true;
        this.currentTick = 0;
    }

    /**
     * Advance one tick. Should be called every server tick (50ms) during playback.
     * Performs camera interpolation, NPC movement, and event firing for this tick.
     */
    public void tick() {
        if (!playing || cancelled) return;

        if (currentTick > data.getDurationTicks()) {
            playing = false;
            callback.onComplete();
            return;
        }

        // Camera interpolation
        interpolateCamera();

        // NPC movement
        for (NpcActor actor : data.getNpcActors()) {
            interpolateNpc(actor);
        }

        // Timeline events
        for (TimelineEvent event : data.getEvents()) {
            if (event.getTick() == currentTick) {
                callback.fireEvent(event);
            }
        }

        currentTick++;
    }

    /**
     * Cancel playback early.
     */
    public void cancel() {
        this.cancelled = true;
        this.playing = false;
    }

    public boolean isPlaying() {
        return playing && !cancelled;
    }

    public UUID getViewerId() {
        return viewerId;
    }

    public int getCurrentTick() {
        return currentTick;
    }

    // --- Internal interpolation ---

    private void interpolateCamera() {
        List<CameraWaypoint> waypoints = data.getCameraWaypoints();
        if (waypoints == null || waypoints.isEmpty()) return;

        // Find surrounding waypoints
        CameraWaypoint before = null;
        CameraWaypoint after = null;

        for (int i = 0; i < waypoints.size(); i++) {
            CameraWaypoint wp = waypoints.get(i);
            if (wp.getTick() <= currentTick) {
                before = wp;
            }
            if (wp.getTick() > currentTick && after == null) {
                after = wp;
            }
        }

        if (before == null && after != null) {
            // Before first waypoint — snap to first
            callback.setCameraPosition(after.getX(), after.getY(), after.getZ(), after.getYaw(), after.getPitch());
        } else if (before != null && after == null) {
            // After last waypoint — hold at last
            callback.setCameraPosition(before.getX(), before.getY(), before.getZ(), before.getYaw(), before.getPitch());
        } else if (before != null) {
            // Between two waypoints — interpolate
            int segmentLength = after.getTick() - before.getTick();
            double progress = segmentLength > 0 ? (double)(currentTick - before.getTick()) / segmentLength : 0;

            double[] pos = CameraInterpolation.interpolate(before, after, progress, before.getInterpolation());
            callback.setCameraPosition(pos[0], pos[1], pos[2], (float) pos[3], (float) pos[4]);
        }
    }

    private void interpolateNpc(NpcActor actor) {
        List<NpcActor.NpcWaypoint> waypoints = actor.getWaypoints();
        if (waypoints == null || waypoints.isEmpty()) return;

        NpcActor.NpcWaypoint before = null;
        NpcActor.NpcWaypoint after = null;

        for (NpcActor.NpcWaypoint wp : waypoints) {
            if (wp.getTick() <= currentTick) {
                before = wp;
            }
            if (wp.getTick() > currentTick && after == null) {
                after = wp;
            }
        }

        if (before == null && after != null) {
            callback.moveNpc(actor.getActorId(), after.getX(), after.getY(), after.getZ(),
                    after.getYaw(), after.getPitch(), after.getAnimation());
        } else if (before != null && after == null) {
            callback.moveNpc(actor.getActorId(), before.getX(), before.getY(), before.getZ(),
                    before.getYaw(), before.getPitch(), before.getAnimation());
        } else if (before != null) {
            int segmentLength = after.getTick() - before.getTick();
            double progress = segmentLength > 0 ? (double)(currentTick - before.getTick()) / segmentLength : 0;
            double x = before.getX() + (after.getX() - before.getX()) * progress;
            double y = before.getY() + (after.getY() - before.getY()) * progress;
            double z = before.getZ() + (after.getZ() - before.getZ()) * progress;
            float yaw = (float)(before.getYaw() + (after.getYaw() - before.getYaw()) * progress);
            float pitch = (float)(before.getPitch() + (after.getPitch() - before.getPitch()) * progress);

            callback.moveNpc(actor.getActorId(), x, y, z, yaw, pitch, before.getAnimation());
        }
    }
}
