package fr.perrier.dungeons.module.labyrinth.lifecycle;

import fr.perrier.dungeons.module.labyrinth.manager.LabyrinthRunManager;
import fr.perrier.dungeons.module.labyrinth.model.DoorChoice;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthRun;
import fr.perrier.dungeons.common.model.labyrinth.RewardIcon;
import fr.perrier.dungeons.common.model.labyrinth.LabyrinthRoom;
import fr.perrier.dungeons.module.labyrinth.ui.DoorIconHologram;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Owns the state of "doors open" for every active run and detects when a
 * player walks through a door anchor.
 *
 * <p>v1 — no physical block lock/unlock. Doors are "active" only when a
 * {@link DoorChoice} is set on the run (mobs cleared) ; the controller
 * simply ignores anchor proximity until then. The existing core
 * {@code aliveMobsByRoom} accounting drives whether the room is cleared
 * or not (CDC §1.7).</p>
 *
 * <p>Polling : a single Bukkit task ticks every 5 server ticks (250 ms).
 * For each open run, it walks through the players of the instance and
 * matches their position against the anchors of the current room.</p>
 */
public class DoorController {

    public static final long POLL_PERIOD_TICKS = 5L;
    public static final double TRAVERSAL_RADIUS = 1.5;

    /** Fallback height above the door anchor when no custom iconAnchor is set. */
    private static final double ICON_Y_OFFSET = 2.0;

    private final LabyrinthRunManager runManager;
    private final Logger logger;
    private final Map<UUID, OpenDoors> openByInstance = new ConcurrentHashMap<>();
    private BukkitTask pollTask;

    public DoorController(LabyrinthRunManager runManager) {
        this.runManager = runManager;
        this.logger = Main.getInstance().getLogger();
    }

    /**
     * Start the polling task that checks every active run for door
     * traversals. Idempotent.
     */
    public void start() {
        if (pollTask != null) return;
        pollTask = Bukkit.getScheduler().runTaskTimer(Main.getInstance(),
                this::pollAll, POLL_PERIOD_TICKS, POLL_PERIOD_TICKS);
    }

    public void stop() {
        if (pollTask != null) {
            pollTask.cancel();
            pollTask = null;
        }
        for (OpenDoors o : openByInstance.values()) o.despawnHolograms();
        openByInstance.clear();
    }

    /**
     * Open the doors for a run that has just had its {@link DoorChoice}
     * built by the lifecycle. Spawns the icon holograms above the
     * anchors of the *current* room (the one the player just cleared).
     */
    public void openDoors(LabyrinthRun run) {
        if (run == null || run.getPendingChoice() == null) return;
        UUID instanceId = run.getInstanceId();
        OpenDoors previous = openByInstance.remove(instanceId);
        if (previous != null) previous.despawnHolograms();

        LabyrinthRoom currentRoom = run.getCurrentRoom();
        if (currentRoom == null || currentRoom.getExitDoors() == null || currentRoom.getExitDoors().isEmpty()) {
            logger.warning("[MemoryLabyrinth] Cannot open doors — current room has no door anchors");
            return;
        }

        DoorChoice choice = run.getPendingChoice();
        // v2 procedural — doors live in the pasted copy, not the template.
        String worldId = run.getInstanceWorldId() != null ? run.getInstanceWorldId() : currentRoom.getWorldId();
        World world = Bukkit.getWorld(worldId);
        if (world == null) {
            logger.warning("[MemoryLabyrinth] World not found for door open: " + worldId);
            return;
        }

        OpenDoors open = new OpenDoors();
        open.instanceId = instanceId;
        open.currentRoom = currentRoom;
        open.choice = choice;

        Location leftAnchor = anchorLocation(world, currentRoom, 0, run);
        if (leftAnchor != null) {
            open.leftAnchor = leftAnchor;
            Location leftIcon = iconLocation(world, currentRoom, 0, run);
            open.leftHologram = choice.isBossSingleDoor()
                    ? DoorIconHologram.spawnBoss(leftIcon)
                    : DoorIconHologram.spawn(leftIcon, choice.getIconLeft());
        }
        if (!choice.isBossSingleDoor() && currentRoom.getExitDoors().size() >= 2) {
            Location rightAnchor = anchorLocation(world, currentRoom, 1, run);
            if (rightAnchor != null) {
                open.rightAnchor = rightAnchor;
                Location rightIcon = iconLocation(world, currentRoom, 1, run);
                open.rightHologram = DoorIconHologram.spawn(rightIcon, choice.getIconRight());
            }
        }
        openByInstance.put(instanceId, open);
    }

    /**
     * Close any open doors for {@code instanceId} (despawn holograms +
     * drop polling state). Called when the run advances or ends.
     */
    public void closeDoors(UUID instanceId) {
        if (instanceId == null) return;
        OpenDoors open = openByInstance.remove(instanceId);
        if (open != null) open.despawnHolograms();
    }

    private void pollAll() {
        if (openByInstance.isEmpty()) return;
        for (Map.Entry<UUID, OpenDoors> e : openByInstance.entrySet()) {
            UUID instanceId = e.getKey();
            OpenDoors open = e.getValue();
            LabyrinthRun run = runManager.getRun(instanceId);
            if (run == null || run.getPendingChoice() == null) {
                open.despawnHolograms();
                openByInstance.remove(instanceId);
                continue;
            }
            // Soft-lock during the Infinite lobby resume/new prompt
            // (CDC §6.1) — players cannot leave the lobby until the
            // leader has decided.
            if (run.isLobbyDecisionPending()) continue;
            FloorInstance instance = Main.getInstance().getDungeonService().getInstance(instanceId);
            if (instance == null) continue;

            for (UUID pid : instance.getPlayers()) {
                Player p = Bukkit.getPlayer(pid);
                if (p == null || !p.isOnline()) continue;
                Location loc = p.getLocation();
                if (open.leftAnchor != null && nearby(loc, open.leftAnchor)) {
                    fireTraversal(run, open, true, instance);
                    break;
                }
                if (open.rightAnchor != null && nearby(loc, open.rightAnchor)) {
                    fireTraversal(run, open, false, instance);
                    break;
                }
            }
        }
    }

    private void fireTraversal(LabyrinthRun run, OpenDoors open, boolean leftSide, FloorInstance instance) {
        DoorChoice choice = open.choice;
        LabyrinthRoom chosen = leftSide ? choice.getLeft() : choice.getRight();
        RewardIcon chosenIcon = leftSide ? choice.getIconLeft() : choice.getIconRight();
        // Despawn before advance so the new room's potential door layer is clean.
        open.despawnHolograms();
        openByInstance.remove(run.getInstanceId());
        runManager.advanceToChosen(run, chosen, chosenIcon, instance);
    }

    private boolean nearby(Location playerLoc, Location anchor) {
        if (playerLoc.getWorld() == null || anchor.getWorld() == null) return false;
        if (!playerLoc.getWorld().equals(anchor.getWorld())) return false;
        return playerLoc.distanceSquared(anchor) <= TRAVERSAL_RADIUS * TRAVERSAL_RADIUS;
    }

    private Location anchorLocation(World world, LabyrinthRoom room, int doorIndex, LabyrinthRun run) {
        if (room.getExitDoors().size() <= doorIndex) return null;
        LabyrinthRoom.Door door = room.getExitDoors().get(doorIndex);
        if (door == null || door.getAnchor() == null) return null;
        LabyrinthRoom.Vec3 a = door.getAnchor();
        // v2 procedural — translate template door coords into the pasted copy.
        LabyrinthRoom.Vec3 srcMin = room.getRegion() != null ? room.getRegion().getMin() : null;
        double offX = (run != null && srcMin != null) ? (run.getCurrentRoomAnchorX() - srcMin.getX()) : 0;
        double offY = (run != null && srcMin != null) ? (run.getCurrentRoomAnchorY() - srcMin.getY()) : 0;
        double offZ = (run != null && srcMin != null) ? (run.getCurrentRoomAnchorZ() - srcMin.getZ()) : 0;
        return new Location(world, a.getX() + offX, a.getY() + offY, a.getZ() + offZ);
    }

    /**
     * Where to render the door's reward icon hologram. Uses the door's
     * optional {@code iconAnchor} when set (exact placement, admin-controlled
     * height, block-centered on X/Z) ; otherwise falls back to the door
     * anchor lifted by {@link #ICON_Y_OFFSET} — the legacy behavior. Both are
     * translated into the pasted copy like {@link #anchorLocation}.
     */
    private Location iconLocation(World world, LabyrinthRoom room, int doorIndex, LabyrinthRun run) {
        if (room.getExitDoors().size() <= doorIndex) return null;
        LabyrinthRoom.Door door = room.getExitDoors().get(doorIndex);
        if (door == null) return null;
        // v2 procedural — translate template coords into the pasted copy.
        LabyrinthRoom.Vec3 srcMin = room.getRegion() != null ? room.getRegion().getMin() : null;
        double offX = (run != null && srcMin != null) ? (run.getCurrentRoomAnchorX() - srcMin.getX()) : 0;
        double offY = (run != null && srcMin != null) ? (run.getCurrentRoomAnchorY() - srcMin.getY()) : 0;
        double offZ = (run != null && srcMin != null) ? (run.getCurrentRoomAnchorZ() - srcMin.getZ()) : 0;
        LabyrinthRoom.Vec3 ic = door.getIconAnchor();
        if (ic != null) {
            // Exact placement — admin set the height ; just block-center X/Z.
            return new Location(world, ic.getX() + offX + 0.5, ic.getY() + offY, ic.getZ() + offZ + 0.5);
        }
        LabyrinthRoom.Vec3 a = door.getAnchor();
        if (a == null) return null;
        return new Location(world, a.getX() + offX + 0.5, a.getY() + offY + ICON_Y_OFFSET, a.getZ() + offZ + 0.5);
    }

    private static class OpenDoors {
        UUID instanceId;
        LabyrinthRoom currentRoom;
        DoorChoice choice;
        Location leftAnchor;
        Location rightAnchor;
        DoorIconHologram leftHologram;
        DoorIconHologram rightHologram;

        void despawnHolograms() {
            if (leftHologram != null) leftHologram.despawn();
            if (rightHologram != null) rightHologram.despawn();
            leftHologram = null;
            rightHologram = null;
        }
    }
}
