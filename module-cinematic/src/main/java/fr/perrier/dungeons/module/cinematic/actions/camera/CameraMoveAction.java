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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Camera movement action replicating Typewriter's {@code DisplayCameraAction} exactly.
 * <p>
 * How it works (ref: Typewriter DisplayCameraAction / CameraCinematicEntry.kt):
 * <ol>
 *   <li>Path points are converted to frame-based {@link PathSegment}s with Catmull-Rom at runtime</li>
 *   <li>A {@code TEXT_DISPLAY} entity is created with {@code positionRotationInterpolationDuration = 10}
 *       so the client smoothly interpolates between positions</li>
 *   <li>The player spectates the entity via {@link WrapperPlayServerCamera}</li>
 *   <li>Each frame: entity is teleported to the interpolated position</li>
 *   <li>Player is teleported only every 10 frames or when &gt; 25 blocks away (chunk loading)</li>
 *   <li>On large frame skips (&gt; 5 frames), the entity is seamlessly recreated</li>
 *   <li>Packet interception: fake Y+500, fake inventory, block self-interact</li>
 * </ol>
 *
 * @see <a href="https://github.com/gabber235/Typewriter">Typewriter</a>
 */
public class CameraMoveAction extends SimpleCinematicAction<CameraSegment> {

    // ref: Typewriter CameraCinematicEntry.kt — fake Y offset for anti-clipping
    private static final double FAKE_Y_OFFSET = 500.0;
    // ref: Typewriter DisplayCameraAction.BASE_INTERPOLATION = 10
    private static final int BASE_INTERPOLATION = 10;
    // ref: Typewriter MAX_DISTANCE_SQUARED = 25 * 25
    private static final double MAX_DISTANCE_SQUARED = 625.0;
    // ref: Typewriter DisplayCameraAction.setupPath — playerDefaultEyeHeight = 1.6
    private static final double EYE_HEIGHT = 1.6;

    private List<CameraSegment> segments = new ArrayList<>();
    private final transient Map<CameraSegment, List<PathSegment>> segmentPaths = new HashMap<>();
    private transient PacketListenerAbstract packetListener = null;
    private transient WrapperEntity cameraEntity = null;
    private transient int lastFrame = -1;

    /**
     * Internal frame-based path segment (mirrors Typewriter's PointSegment).
     * Each segment represents a path point with a frame range during which it is the "current" point.
     */
    private record PathSegment(int startFrame, int endFrame, double x, double y, double z, float yaw, float pitch) {
        boolean isActiveAt(int frame) {
            return frame >= startFrame && frame < endFrame;
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Segment lifecycle (ref: Typewriter DisplayCameraAction)
    // ────────────────────────────────────────────────────────────────────────

    @Override
    protected void onSegmentStart(Player player, CameraSegment segment) throws Exception {
        // Build frame-based path (ref: Typewriter DisplayCameraAction.setupPath)
        List<PathSegment> path = buildPath(segment);
        segmentPaths.put(segment, path);
        if (path.isEmpty()) return;

        PathSegment start = path.get(0);
        Location startLoc = new Location(player.getWorld(),
                start.x, start.y, start.z, start.yaw, start.pitch);

        // ref: Typewriter DisplayCameraAction.startSegment
        player.teleport(startLoc);
        player.setAllowFlight(true);
        player.setFlying(true);

        // Spawn entity and spectate (ref: Typewriter DisplayCameraAction.startSegment)
        spawnCameraEntity(player, toPacketLocation(start));

        // Packet interception (skip Bedrock/Floodgate players)
        if (!isFloodgatePlayer(player)) {
            setupPacketInterception(player);
        }
    }

    @Override
    protected void onSegmentTick(Player player, CameraSegment segment, int frame) throws Exception {
        List<PathSegment> path = segmentPaths.get(segment);
        if (path == null || path.isEmpty()) return;

        // Use frame relative to segment start (ref: Typewriter baseFrame = frame - segment.startFrame)
        int relativeFrame = frame - segment.getStartFrame();

        // Frame skip detection: recreate entity seamlessly (ref: Typewriter skipToFrame)
        if (cameraEntity != null && Math.abs(frame - lastFrame) > 5 && relativeFrame > 0) {
            switchSeamless(player, path, relativeFrame);
        }
        lastFrame = frame;

        // Runtime Catmull-Rom interpolation (ref: Typewriter List<PointSegment>.interpolate)
        double[] pos = interpolatePath(path, relativeFrame);
        if (pos == null) return;

        com.github.retrooper.packetevents.protocol.world.Location packetLoc =
                new com.github.retrooper.packetevents.protocol.world.Location(
                        pos[0], pos[1], pos[2], (float) pos[3], (float) pos[4]);

        // Entity teleported every frame → client interpolates smoothly
        // ref: Typewriter DisplayCameraAction.tickSegment
        if (cameraEntity != null) {
            cameraEntity.teleport(packetLoc);
        }

        // Player teleported only for chunk loading (ref: Typewriter teleportIfNeeded)
        Location loc = new Location(player.getWorld(), pos[0], pos[1], pos[2], (float) pos[3], (float) pos[4]);
        double distSq = player.getLocation().distanceSquared(loc);
        if (relativeFrame % 10 == 0 || distSq > MAX_DISTANCE_SQUARED) {
            player.teleport(loc);
        }
    }

    @Override
    protected void onSegmentStop(Player player, CameraSegment segment) throws Exception {
        // ref: Typewriter DisplayCameraAction.stop / onSegmentStop
        despawnCameraEntity(player);
        segmentPaths.remove(segment);
        cleanupPacketInterception();
        player.updateInventory();
        lastFrame = -1;
    }

    @Override
    public List<CameraSegment> getCinematicSegments() {
        return segments;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Path building (ref: Typewriter List<PathPoint>.transform)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Converts waypoints to frame-based {@link PathSegment}s, mirroring Typewriter's
     * {@code List<PathPoint>.transform()} function.
     * <p>
     * Key Typewriter behaviors replicated:
     * <ul>
     *   <li>Duration adjusted by {@code BASE_INTERPOLATION} (10 ticks shorter)</li>
     *   <li>Eye height offset (+1.6) added to Y positions</li>
     *   <li>Frames distributed evenly among path point transitions</li>
     * </ul>
     */
    private List<PathSegment> buildPath(CameraSegment segment) {
        List<CameraWaypoint> waypoints = segment.getPathPoints();
        if (waypoints == null || waypoints.isEmpty()) return List.of();

        // ref: Typewriter — segment.duration - BASE_INTERPOLATION
        int segDuration = (segment.getEndFrame() - segment.getStartFrame()) - BASE_INTERPOLATION;
        segDuration = Math.max(segDuration, 1);

        int n = waypoints.size();
        if (n == 1) {
            CameraWaypoint wp = waypoints.get(0);
            return List.of(new PathSegment(0, segDuration,
                    wp.getX(), wp.getY() + EYE_HEIGHT, wp.getZ(), wp.getYaw(), wp.getPitch()));
        }

        // Distribute frames evenly among transitions (ref: Typewriter default when no duration specified)
        List<PathSegment> result = new ArrayList<>();
        int transitionCount = n - 1;
        int framesPerTransition = segDuration / transitionCount;
        int leftover = segDuration % transitionCount;
        int currentFrame = 0;

        for (int i = 0; i < n; i++) {
            CameraWaypoint wp = waypoints.get(i);
            int duration;
            if (i < transitionCount) {
                duration = framesPerTransition + (i < leftover ? 1 : 0);
            } else {
                // Last point has no duration (ref: Typewriter — last segment reached when cinematic ends)
                duration = 0;
            }
            int endFrame = currentFrame + duration;
            result.add(new PathSegment(currentFrame, endFrame,
                    wp.getX(), wp.getY() + EYE_HEIGHT, wp.getZ(), wp.getYaw(), wp.getPitch()));
            currentFrame = endFrame;
        }

        return result;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Runtime Catmull-Rom interpolation (ref: Typewriter Interpolation.kt)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Interpolates position on the path at the given frame using Catmull-Rom.
     * Exact replica of Typewriter's {@code List<PointSegment>.interpolate(frame)} function.
     *
     * @return [x, y, z, yaw, pitch] or null if path is empty
     */
    private static double[] interpolatePath(List<PathSegment> path, int frame) {
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
            PathSegment last = path.get(path.size() - 1);
            return new double[]{last.x, last.y, last.z, last.yaw, last.pitch};
        }

        PathSegment current = path.get(index);
        int totalFrames = current.endFrame - current.startFrame;
        int currentFrame = frame - current.startFrame;
        double percentage = totalFrames > 0 ? (double) currentFrame / totalFrames : 0;

        // Get 4 surrounding points for Catmull-Rom (ref: Typewriter PointSegment interpolation)
        PathSegment prev = index > 0 ? path.get(index - 1) : current;
        PathSegment next = index + 1 < path.size() ? path.get(index + 1) : current;
        PathSegment nextNext = index + 2 < path.size() ? path.get(index + 2) : next;

        double x = catmullRom(prev.x, current.x, next.x, nextNext.x, percentage);
        double y = catmullRom(prev.y, current.y, next.y, nextNext.y, percentage);
        double z = catmullRom(prev.z, current.z, next.z, nextNext.z, percentage);

        // Yaw correction for wrap-around before interpolation (ref: Typewriter correctYaw in Rotatable.kt)
        double prevYaw = prev.yaw;
        double curYaw = correctYaw(prevYaw, current.yaw);
        double nextYaw = correctYaw(curYaw, next.yaw);
        double nextNextYaw = correctYaw(nextYaw, nextNext.yaw);
        float yaw = (float) catmullRom(prevYaw, curYaw, nextYaw, nextNextYaw, percentage);

        float pitch = (float) catmullRom(prev.pitch, current.pitch, next.pitch, nextNext.pitch, percentage);

        return new double[]{x, y, z, yaw, pitch};
    }

    /**
     * Catmull-Rom interpolation between 4 values.
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
     * Correct yaw so that it interpolates via the shortest path around the 360° boundary.
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
    // Camera entity management (ref: Typewriter DisplayCameraAction)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new TEXT_DISPLAY entity with interpolation configured.
     * Exact replica of Typewriter's {@code DisplayCameraAction.createEntity()}.
     */
    private WrapperEntity createCameraEntity() {
        WrapperEntity entity = new WrapperEntity(EntityTypes.TEXT_DISPLAY);
        if (entity.getEntityMeta() instanceof TextDisplayMeta meta) {
            meta.setNotifyAboutChanges(false);
            meta.setPositionRotationInterpolationDuration(BASE_INTERPOLATION);
            meta.setNotifyAboutChanges(true);
        }
        return entity;
    }

    /**
     * Spawns the camera entity and makes the player spectate it.
     * Spawn order matches Typewriter: spawn → addViewer → spectateEntity.
     */
    private void spawnCameraEntity(Player player,
                                   com.github.retrooper.packetevents.protocol.world.Location location) {
        if (cameraEntity != null) {
            despawnCameraEntity(player);
        }
        cameraEntity = createCameraEntity();
        cameraEntity.spawn(location);
        cameraEntity.addViewer(player.getUniqueId());
        spectateEntity(player, cameraEntity);
    }

    /**
     * Seamlessly recreates the camera entity at a new position.
     * Used for frame skip handling and segment switching.
     * Exact replica of Typewriter's {@code DisplayCameraAction.switchSeamless()}.
     * <p>
     * Order: create new → spawn new → addViewer → spectate new → despawn old → remove old.
     */
    private void switchSeamless(Player player, List<PathSegment> path, int frame) {
        double[] pos = interpolatePath(path, frame);
        if (pos == null) return;

        com.github.retrooper.packetevents.protocol.world.Location packetLoc =
                new com.github.retrooper.packetevents.protocol.world.Location(
                        pos[0], pos[1], pos[2], (float) pos[3], (float) pos[4]);

        // Create and spawn new entity before destroying old (ref: Typewriter switchSeamless)
        WrapperEntity newEntity = createCameraEntity();
        newEntity.spawn(packetLoc);
        newEntity.addViewer(player.getUniqueId());
        spectateEntity(player, newEntity);

        // Destroy old entity
        if (cameraEntity != null) {
            cameraEntity.despawn();
            cameraEntity.remove();
        }
        cameraEntity = newEntity;
    }

    /**
     * Stops spectating and despawns the camera entity.
     * Mirrors Typewriter's {@code DisplayCameraAction.stop()} behavior.
     */
    private void despawnCameraEntity(Player player) {
        if (cameraEntity != null) {
            stopSpectatingEntity(player);
            cameraEntity.despawn();
            cameraEntity.remove();
            cameraEntity = null;
        }
    }

    /**
     * Sends a Camera packet to make the player spectate an entity.
     * ref: Typewriter PlayerPackets.kt — {@code Player.spectateEntity(entity)}
     */
    private void spectateEntity(Player player, WrapperEntity entity) {
        try {
            PacketEvents.getAPI().getPlayerManager().sendPacket(
                    player,
                    new WrapperPlayServerCamera(entity.getEntityId()));
        } catch (Exception e) {
            System.err.println("[Cinematic] Failed to spectate entity: " + e.getMessage());
        }
    }

    /**
     * Sends a Camera packet to stop spectating (return to player's own POV).
     * ref: Typewriter PlayerPackets.kt — {@code Player.stopSpectatingEntity()}
     */
    private void stopSpectatingEntity(Player player) {
        try {
            PacketEvents.getAPI().getPlayerManager().sendPacket(
                    player,
                    new WrapperPlayServerCamera(player.getEntityId()));
        } catch (Exception e) {
            System.err.println("[Cinematic] Failed to stop spectating entity: " + e.getMessage());
        }
    }

    private static com.github.retrooper.packetevents.protocol.world.Location toPacketLocation(PathSegment seg) {
        return new com.github.retrooper.packetevents.protocol.world.Location(
                seg.x, seg.y, seg.z, seg.yaw, seg.pitch);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Packet interception (ref: Typewriter CameraCinematicEntry.kt setup())
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Configures packet interception mirroring Typewriter's {@code interceptPackets} block:
     * <ul>
     *   <li>SERVER→CLIENT: Position Y+500 (anti-self-clipping)</li>
     *   <li>SERVER→CLIENT: Fake inventory vide ({@code keepFakeInventory})</li>
     *   <li>CLIENT→SERVER: Correction Y-500</li>
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
                    List<com.github.retrooper.packetevents.protocol.item.ItemStack> fakeItems =
                            new ArrayList<>(java.util.Collections.nCopies(
                                    itemCount,
                                    com.github.retrooper.packetevents.protocol.item.ItemStack.EMPTY
                            ));
                    packet.setItems(fakeItems);
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

                if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
                    event.setCancelled(true);
                    return;
                }
                if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW_BUTTON) {
                    event.setCancelled(true);
                    return;
                }

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
     * Bedrock players don't support Java packet manipulation.
     */
    private boolean isFloodgatePlayer(Player player) {
        try {
            String uuid = player.getUniqueId().toString();
            return uuid.startsWith("00000000-0000-0000-0009-");
        } catch (Exception e) {
            return false;
        }
    }
}
