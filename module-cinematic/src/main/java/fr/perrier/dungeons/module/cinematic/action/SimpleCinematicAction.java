package fr.perrier.dungeons.module.cinematic.action;

import fr.perrier.dungeons.module.cinematic.clock.CinematicClock;
import fr.perrier.dungeons.module.cinematic.segment.CinematicSegment;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Abstract helper for cinematic actions with automatic segment lifecycle management.
 * <p>
 * Exact replica of Typewriter's {@code SimpleCinematicAction<S>}:
 * <ul>
 *   <li>Tracks a single {@code previousSegment} (not a Set of active segments)</li>
 *   <li>On each tick: finds the active segment, compares with previous</li>
 *   <li>If segment changed: stop old → start new → tick new</li>
 *   <li>If same segment: just tick</li>
 *   <li>On teardown: stop the current segment if any</li>
 * </ul>
 *
 * @param <S> the segment type managed by this action
 * @see <a href="https://github.com/gabber235/Typewriter">Typewriter SimpleCinematicAction.kt</a>
 */
public abstract class SimpleCinematicAction<S extends CinematicSegment> implements CinematicAction {

    /**
     * The last frame that was ticked.
     * Exposed for subclasses (e.g. frame-skip detection like Typewriter's {@code abs(frame - lastFrame) > 5}).
     */
    protected int lastFrame = 0;

    private S previousSegment = null;

    @Override
    public void onCinematicSetup(Player player, CinematicClock clock) throws Exception {
        // Override if global initialization is needed
    }

    /**
     * Tick logic matching Typewriter's SimpleCinematicAction.tick():
     * <pre>
     * lastFrame = frame
     * segment = segments activeSegmentAt frame
     *
     * if (segment == previousSegment) {
     *     segment?.let { tickSegment(it, frame) }
     *     return
     * }
     *
     * previousSegment?.let { stopSegment(it) }
     * segment?.let {
     *     startSegment(it)
     *     tickSegment(it, frame)
     * }
     * </pre>
     */
    @Override
    public void onCinematicTick(Player player, int frame) throws Exception {
        lastFrame = frame;

        // Find the active segment at this frame (ref: Typewriter segments activeSegmentAt frame)
        S segment = activeSegmentAt(frame);

        if (segment == previousSegment) {
            // Same segment as before — just tick it
            if (segment != null) {
                onSegmentTick(player, segment, frame);
            }
            return;
        }

        // Segment changed — stop old, start new
        if (previousSegment != null) {
            onSegmentStop(player, previousSegment);
        }

        if (segment != null) {
            onSegmentStart(player, segment);
            onSegmentTick(player, segment, frame);
        }

        previousSegment = segment;
    }

    @Override
    public void onCinematicStop(Player player) throws Exception {
        // Stop the current segment if any (ref: Typewriter teardown)
        if (previousSegment != null) {
            onSegmentStop(player, previousSegment);
            previousSegment = null;
        }
    }

    /**
     * Matches Typewriter's {@code segments canFinishAt frame} which checks {@code frame > endFrame}.
     */
    @Override
    public boolean canCinematicFinish(int frame) {
        for (CinematicSegment segment : getCinematicSegments()) {
            if (frame <= segment.getEndFrame()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Called when a segment starts (transitions in).
     */
    protected void onSegmentStart(Player player, S segment) throws Exception {
    }

    /**
     * Called each frame while the segment is active.
     */
    protected void onSegmentTick(Player player, S segment, int frame) throws Exception {
    }

    /**
     * Called when a segment ends (transitions out).
     */
    protected void onSegmentStop(Player player, S segment) throws Exception {
    }

    /**
     * Finds the first segment active at the given frame.
     * Mirrors Typewriter's {@code List<Segment>.activeSegmentAt(frame)}.
     */
    @SuppressWarnings("unchecked")
    private S activeSegmentAt(int frame) {
        for (CinematicSegment segment : getCinematicSegments()) {
            if (segment.isActiveAt(frame)) {
                return (S) segment;
            }
        }
        return null;
    }

    @Override
    public abstract List<S> getCinematicSegments();
}
