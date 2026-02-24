package fr.perrier.dungeons.module.cinematic.actions.camera;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPosition;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCamera;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import fr.perrier.dungeons.module.cinematic.action.SimpleCinematicAction;
import fr.perrier.dungeons.module.cinematic.model.CameraWaypoint;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Camera movement action replicating Typewriter's {@code CameraCinematicAction} + {@code DisplayCameraAction}.
 * <p>
 * Architecture (mirrors Typewriter exactly):
 * <ul>
 *   <li>{@code CameraCinematicAction} wraps a {@code CameraAction} (Display/Teleport/Bedrock strategy)</li>
 *   <li>{@code DisplayCameraAction} creates a TEXT_DISPLAY entity with {@code positionRotationInterpolationDuration = 10}</li>
 *   <li>Player spectates entity via Camera packet → client-side smooth interpolation</li>
 *   <li>Path points are transformed into frame-based {@code PointSegment}s with Catmull-Rom interpolation</li>
 *   <li>On segment start: setupPath → teleport → spawn entity → spectate</li>
 *   <li>On tick: interpolate position → teleport entity. Player teleported only when needed for chunk loading</li>
 *   <li>On frame skip (&gt;5 frames): switchSeamless → recreate entity at current position</li>
 *   <li>On segment switch (same world): switchSeamless. Different world: switchWithStop</li>
 *   <li>Packet interception: Y+500 offset, fake inventory, block self-interact</li>
 * </ul>
 *
 * @see <a href="https://github.com/gabber235/Typewriter">Typewriter CameraCinematicEntry.kt</a>
 */
public class CameraMoveAction extends SimpleCinematicAction<CameraSegment> {

    // ── Constants matching Typewriter exactly ──

    /** ref: Typewriter CameraCinematicEntry.kt — fake Y offset for anti-self-clipping */
    private static final double FAKE_Y_OFFSET = 500.0;
    /** ref: Typewriter DisplayCameraAction.BASE_INTERPOLATION = 10 */
    private static final int BASE_INTERPOLATION = 10;
    /** ref: Typewriter MAX_DISTANCE_SQUARED = 25 * 25 */
    private static final double MAX_DISTANCE_SQUARED = 625.0;
    /** ref: Typewriter DisplayCameraAction.setupPath — playerDefaultEyeHeight = 1.6 */
    private static final double PLAYER_DEFAULT_EYE_HEIGHT = 1.6;

    // ── State ──

    private List<CameraSegment> segments = new ArrayList<>();
    private transient List<PointSegment> currentPath = List.of();
    private transient PacketListenerAbstract packetListener = null;
    private transient WrapperEntity entity = null;
    private transient boolean isActive = false;

    /**
     * Frame-based path segment mirroring Typewriter's {@code PointSegment}.
     * Each represents a path point with a frame range during which it is the "current" point for interpolation.
     */
    private record PointSegment(int startFrame, int endFrame, double x, double y, double z, float yaw, float pitch) {
        /** ref: Typewriter Segment.isActiveAt(frame) — frame in startFrame..endFrame */
        boolean isActiveAt(int frame) {
            return frame >= startFrame && frame <= endFrame;
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Segment lifecycle (mirrors Typewriter CameraCinematicAction.tick)
    // ────────────────────────────────────────────────────────────────────────

    @Override
    protected void onSegmentStart(Player player, CameraSegment segment) throws Exception {
        // ref: Typewriter CameraCinematicAction.tick — if previousSegment == null && segment != null → player.setup() + action.startSegment
        setupPath(player, segment);

        if (currentPath.isEmpty()) return;

        // Setup packet interception (ref: Typewriter CameraCinematicAction.Player.setup → interceptPackets)
        if (!isFloodgatePlayer(player)) {
            setupPacketInterception(player);
        }

        // ref: Typewriter DisplayCameraAction.startSegment
        PointSegment first = currentPath.get(0);
        Location startLoc = toLocation(player, first);

        player.teleport(startLoc);
        player.setAllowFlight(true);
        player.setFlying(true);

        entity = createEntity();
        entity.spawn(toPacketLocation(first));
        entity.addViewer(player.getUniqueId());
        spectateEntity(player, entity);
        isActive = true;
    }

    @Override
    protected void onSegmentTick(Player player, CameraSegment segment, int frame) throws Exception {
        if (currentPath.isEmpty()) return;

        // ref: Typewriter CameraCinematicAction.tick — baseFrame = frame - segment.startFrame
        int baseFrame = frame - segment.getStartFrame();

        // Frame skip detection → switchSeamless (ref: Typewriter — abs(frame - lastFrame) > 5)
        if (isActive && Math.abs(frame - lastFrame) > 5 && baseFrame > 0) {
            switchSeamless(player, baseFrame);
        }

        // ref: Typewriter DisplayCameraAction.tickSegment
        double[] pos = interpolatePath(currentPath, baseFrame);
        if (pos == null) return;

        com.github.retrooper.packetevents.protocol.world.Location packetLoc =
                new com.github.retrooper.packetevents.protocol.world.Location(
                        pos[0], pos[1], pos[2], (float) pos[3], (float) pos[4]);

        // Entity teleported every frame → client interpolates smoothly via BASE_INTERPOLATION
        if (entity != null) {
            entity.teleport(packetLoc);
        }

        // Player teleported only for chunk loading (ref: Typewriter teleportIfNeeded)
        Location loc = new Location(player.getWorld(), pos[0], pos[1], pos[2], (float) pos[3], (float) pos[4]);
        double distSq = player.getLocation().distanceSquared(loc);
        if (baseFrame % 10 == 0 || distSq > MAX_DISTANCE_SQUARED) {
            player.teleport(loc);
        }
    }

    @Override
    protected void onSegmentStop(Player player, CameraSegment segment) throws Exception {
        // ref: Typewriter DisplayCameraAction.stop
        stop(player);
        cleanupPacketInterception();
        player.updateInventory();
    }

    @Override
    public List<CameraSegment> getCinematicSegments() {
        return segments;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Path setup (mirrors Typewriter List<PathPoint>.transform)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Converts waypoints to frame-based {@link PointSegment}s.
     * Exact replica of Typewriter's {@code List<PathPoint>.transform()} function.
     * <p>
     * Key Typewriter behaviors:
     * <ul>
     *   <li>Total duration = segment.duration - BASE_INTERPOLATION</li>
     *   <li>Eye height offset (+1.6) added to Y</li>
     *   <li>Duration distributed evenly among path points without explicit duration</li>
     *   <li>Last path point always has duration 0</li>
     * </ul>
     */
    private void setupPath(Player player, CameraSegment segment) {
        List<CameraWaypoint> waypoints = segment.getPathPoints();
        if (waypoints == null || waypoints.isEmpty()) {
            currentPath = List.of();
            return;
        }

        // ref: Typewriter — segment.duration - BASE_INTERPOLATION
        int totalDuration = (segment.getEndFrame() - segment.getStartFrame()) - BASE_INTERPOLATION;
        totalDuration = Math.max(totalDuration, 1);

        int n = waypoints.size();

        if (n == 1) {
            CameraWaypoint wp = waypoints.get(0);
            currentPath = List.of(new PointSegment(0, totalDuration,
                    wp.getX(), wp.getY() + PLAYER_DEFAULT_EYE_HEIGHT, wp.getZ(),
                    wp.getYaw(), wp.getPitch()));
            return;
        }

        // All path points distribute duration evenly (ref: Typewriter — when no duration is specified on PathPoint)
        // The last segment should never have a duration — it's reached when the cinematic ends
        int transitions = n - 1;
        int durationPerSegment = totalDuration / transitions;
        int leftOverDuration = totalDuration % transitions;

        List<PointSegment> result = new ArrayList<>();
        int currentFrame = 0;

        for (int i = 0; i < n; i++) {
            CameraWaypoint wp = waypoints.get(i);
            int duration;
            if (i < transitions) {
                // ref: Typewriter — distribute leftover +1 to early segments
                duration = durationPerSegment;
                if (leftOverDuration > 0) {
                    duration++;
                    leftOverDuration--;
                }
            } else {
                // Last point has duration 0 (ref: Typewriter — last segment reached when cinematic ends)
                duration = 0;
            }
            int endFrame = currentFrame + duration;
            result.add(new PointSegment(currentFrame, endFrame,
                    wp.getX(), wp.getY() + PLAYER_DEFAULT_EYE_HEIGHT, wp.getZ(),
                    wp.getYaw(), wp.getPitch()));
            currentFrame = endFrame;
        }

        currentPath = result;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Catmull-Rom interpolation (mirrors Typewriter Interpolation.kt)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Interpolates position on the path at the given frame using Catmull-Rom.
     * Exact replica of Typewriter's {@code List<PointSegment>.interpolate(frame)}.
     *
     * @return [x, y, z, yaw, pitch] or null if path is empty
     */
    private static double[] interpolatePath(List<PointSegment> path, int frame) {
        if (path.isEmpty()) return null;

        // Find active segment (ref: Typewriter indexOfFirst { it isActiveAt frame })
        int index = -1;
        for (int i = 0; i < path.size(); i++) {
            if (path.get(i).isActiveAt(frame)) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            // Past the end — hold at last position (ref: Typewriter return last().position)
            PointSegment last = path.get(path.size() - 1);
            return new double[]{last.x, last.y, last.z, last.yaw, last.pitch};
        }

        PointSegment segment = path.get(index);
        int totalFrames = segment.endFrame - segment.startFrame;
        int currentFrame = frame - segment.startFrame;
        double percentage = totalFrames > 0 ? (double) currentFrame / totalFrames : 0;

        // Get 4 surrounding points for Catmull-Rom (ref: Typewriter CameraCinematicEntry.kt interpolate)
        PointSegment prev = index > 0 ? path.get(index - 1) : segment;
        PointSegment next = index + 1 < path.size() ? path.get(index + 1) : segment;
        PointSegment nextNext = index + 2 < path.size() ? path.get(index + 2) : next;

        // Position interpolation
        double x = catmullRom(prev.x, segment.x, next.x, nextNext.x, percentage);
        double y = catmullRom(prev.y, segment.y, next.y, nextNext.y, percentage);
        double z = catmullRom(prev.z, segment.z, next.z, nextNext.z, percentage);

        // Yaw with correction for 360° wrap-around (ref: Typewriter Interpolation.kt correctYaw chain)
        double prevYaw = prev.yaw;
        double curYaw = correctYaw(prevYaw, segment.yaw);
        double nextYaw = correctYaw(curYaw, next.yaw);
        double nextNextYaw = correctYaw(nextYaw, nextNext.yaw);
        float yaw = (float) catmullRom(prevYaw, curYaw, nextYaw, nextNextYaw, percentage);

        // Pitch (no wrap correction needed)
        float pitch = (float) catmullRom(prev.pitch, segment.pitch, next.pitch, nextNext.pitch, percentage);

        return new double[]{x, y, z, yaw, pitch};
    }

    /**
     * Catmull-Rom interpolation between 4 scalar values.
     * Exact formula from Typewriter's {@code interpolatePoints()} in Interpolation.kt:
     * {@code 0.5 * ((2*P1) + (-P0+P2)*t + (2*P0-5*P1+4*P2-P3)*t² + (-P0+3*P1-3*P2+P3)*t³)}
     */
    private static double catmullRom(double p0, double p1, double p2, double p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return 0.5 * ((2 * p1)
                + (-p0 + p2) * t
                + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2
                + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
    }

    /**
     * Corrects yaw to interpolate via the shortest path around the 360° boundary.
     * Exact replica of Typewriter's {@code correctYaw()} from Rotatable.kt.
     */
    private static double correctYaw(double currentYaw, double nextYaw) {
        double difference = nextYaw - currentYaw;
        if (difference > 180) {
            return nextYaw - 360;
        } else if (difference < -180) {
            return nextYaw + 360;
        }
        return nextYaw;
    }

    // ────────────────────────────────────────────────────────────────────────
    // DisplayCameraAction entity management (mirrors Typewriter DisplayCameraAction)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new TEXT_DISPLAY entity with interpolation duration.
     * ref: Typewriter DisplayCameraAction.createEntity()
     */
    private WrapperEntity createEntity() {
        WrapperEntity e = new WrapperEntity(EntityTypes.TEXT_DISPLAY);
        if (e.getEntityMeta() instanceof TextDisplayMeta meta) {
            meta.setNotifyAboutChanges(false);
            meta.setPositionRotationInterpolationDuration(BASE_INTERPOLATION);
            meta.setNotifyAboutChanges(true);
        }
        return e;
    }

    /**
     * Seamlessly recreates the entity at a new position without visual pop.
     * ref: Typewriter DisplayCameraAction.switchSeamless()
     * <p>
     * Order: create new → spawn → addViewer → spectate → despawn old → remove old.
     */
    private void switchSeamless(Player player, int frame) {
        switchSeamless(player, frame, currentPath.get(0));
    }

    private void switchSeamless(Player player, int frame, PointSegment fallback) {
        double[] pos = interpolatePath(currentPath, frame);
        com.github.retrooper.packetevents.protocol.world.Location packetLoc;
        if (pos != null) {
            packetLoc = new com.github.retrooper.packetevents.protocol.world.Location(
                    pos[0], pos[1], pos[2], (float) pos[3], (float) pos[4]);
        } else {
            packetLoc = toPacketLocation(fallback);
        }

        WrapperEntity newEntity = createEntity();
        newEntity.spawn(packetLoc);
        newEntity.addViewer(player.getUniqueId());
        spectateEntity(player, newEntity);

        if (entity != null) {
            entity.despawn();
            entity.remove();
        }
        entity = newEntity;

        // Teleport player to new position for chunk loading
        if (pos != null) {
            player.teleport(new Location(player.getWorld(), pos[0], pos[1], pos[2], (float) pos[3], (float) pos[4]));
        }
    }

    /**
     * Stops the display camera action.
     * ref: Typewriter DisplayCameraAction.stop()
     */
    private void stop(Player player) {
        if (!isActive) return;
        isActive = false;
        stopSpectatingEntity(player);
        if (entity != null) {
            entity.despawn();
            entity.remove();
            entity = null;
        }
    }

    /**
     * Sends Camera packet to make the player spectate an entity.
     * ref: Typewriter PlayerPackets.kt — Player.spectateEntity(entity)
     */
    private void spectateEntity(Player player, WrapperEntity e) {
        try {
            PacketEvents.getAPI().getPlayerManager().sendPacket(
                    player, new WrapperPlayServerCamera(e.getEntityId()));
        } catch (Exception ex) {
            System.err.println("[Cinematic] Failed to spectate entity: " + ex.getMessage());
        }
    }

    /**
     * Sends Camera packet to stop spectating (return to player's own POV).
     * ref: Typewriter PlayerPackets.kt — Player.stopSpectatingEntity()
     */
    private void stopSpectatingEntity(Player player) {
        try {
            PacketEvents.getAPI().getPlayerManager().sendPacket(
                    player, new WrapperPlayServerCamera(player.getEntityId()));
        } catch (Exception ex) {
            System.err.println("[Cinematic] Failed to stop spectating: " + ex.getMessage());
        }
    }

    // ── Utility ──

    private static com.github.retrooper.packetevents.protocol.world.Location toPacketLocation(PointSegment seg) {
        return new com.github.retrooper.packetevents.protocol.world.Location(
                seg.x, seg.y, seg.z, seg.yaw, seg.pitch);
    }

    private static Location toLocation(Player player, PointSegment seg) {
        return new Location(player.getWorld(), seg.x, seg.y, seg.z, seg.yaw, seg.pitch);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Packet interception (mirrors Typewriter CameraCinematicEntry.kt setup())
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Configures packet interception matching Typewriter's interceptPackets block:
     * <ul>
     *   <li>SERVER→CLIENT: Position Y+500 (anti-self-clipping)</li>
     *   <li>SERVER→CLIENT: Fake inventory empty (keepFakeInventory)</li>
     *   <li>CLIENT→SERVER: Y-500 correction</li>
     *   <li>CLIENT→SERVER: Block inventory clicks + self-interact</li>
     * </ul>
     */
    private void setupPacketInterception(Player player) {
        if (packetListener != null) return;

        UUID playerUUID = player.getUniqueId();
        int playerEntityId = player.getEntityId();

        packetListener = new PacketListenerAbstract() {
            @Override
            public void onPacketSend(PacketSendEvent event) {
                if (!playerUUID.equals(event.getUser().getUUID())) return;

                if (event.getPacketType() == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
                    WrapperPlayServerPlayerPositionAndLook packet =
                            new WrapperPlayServerPlayerPositionAndLook(event);
                    packet.setY(packet.getY() + FAKE_Y_OFFSET);
                }
                else if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
                    WrapperPlayServerWindowItems packet =
                            new WrapperPlayServerWindowItems(event);
                    int itemCount = packet.getItems().size();
                    packet.setItems(new ArrayList<>(Collections.nCopies(
                            itemCount,
                            com.github.retrooper.packetevents.protocol.item.ItemStack.EMPTY)));
                }
                else if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
                    WrapperPlayServerSetSlot packet =
                            new WrapperPlayServerSetSlot(event);
                    packet.setItem(com.github.retrooper.packetevents.protocol.item.ItemStack.EMPTY);
                }
            }

            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (!playerUUID.equals(event.getUser().getUUID())) return;

                // Block inventory clicks (ref: Typewriter keepFakeInventory)
                if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW
                        || event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW_BUTTON) {
                    event.setCancelled(true);
                    return;
                }

                // Y correction SERVER←CLIENT
                if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION) {
                    WrapperPlayClientPlayerPosition packet =
                            new WrapperPlayClientPlayerPosition(event);
                    Vector3d pos = packet.getPosition();
                    packet.setPosition(new Vector3d(pos.x, pos.y - FAKE_Y_OFFSET, pos.z));
                }
                else if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
                    WrapperPlayClientPlayerPositionAndRotation packet =
                            new WrapperPlayClientPlayerPositionAndRotation(event);
                    Vector3d pos = packet.getPosition();
                    packet.setPosition(new Vector3d(pos.x, pos.y - FAKE_Y_OFFSET, pos.z));
                }
                // Block self-interaction
                else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
                    WrapperPlayClientInteractEntity packet =
                            new WrapperPlayClientInteractEntity(event);
                    if (packet.getEntityId() == playerEntityId) {
                        event.setCancelled(true);
                    }
                }
            }
        };

        try {
            PacketEvents.getAPI().getEventManager().registerListener(packetListener);
        } catch (Exception e) {
            System.err.println("[Cinematic] Failed to setup packet interception: " + e.getMessage());
            packetListener = null;
        }
    }

    private void cleanupPacketInterception() {
        if (packetListener != null) {
            try {
                PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
            } catch (Exception e) {
                System.err.println("[Cinematic] Failed to cleanup packet interception: " + e.getMessage());
            }
            packetListener = null;
        }
    }

    /**
     * Detects Bedrock players via Floodgate UUID prefix.
     * ref: Typewriter CameraCinematicEntry.kt — player.isFloodgate check
     */
    private boolean isFloodgatePlayer(Player player) {
        try {
            return player.getUniqueId().toString().startsWith("00000000-0000-0000-0009-");
        } catch (Exception e) {
            return false;
        }
    }
}
