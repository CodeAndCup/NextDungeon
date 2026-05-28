package fr.perrier.dungeons.module.labyrinth.lifecycle;

import fr.perrier.dungeons.module.labyrinth.generator.RoomPicker;
import fr.perrier.dungeons.module.labyrinth.model.DoorChoice;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthRun;
import fr.perrier.dungeons.common.model.labyrinth.RewardIcon;
import fr.perrier.dungeons.common.model.labyrinth.LabyrinthRoom;
import fr.perrier.dungeons.common.model.labyrinth.RoomType;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Orchestrates the per-room lifecycle of a Memory Labyrinth run :
 * teleport on entry, mob spawning, mob-death tracking, "room cleared"
 * resolution and door-choice preparation.
 *
 * <p>The actual door lock/unlock + icon hologram visuals come in P4 ; this
 * class only flags the room as "cleared" by populating
 * {@link LabyrinthRun#setPendingChoice(DoorChoice)} so the door layer
 * can react.</p>
 */
public class LabyrinthRoomLifecycle {

    private final RoomPicker roomPicker;
    private final MobSpawner mobSpawner;
    private final Logger logger;
    private DoorController doorController;
    private BossEncounterHandler bossEncounterHandler;
    private fr.perrier.dungeons.module.labyrinth.event.LabyrinthTriggerBus triggerBus;
    private fr.perrier.dungeons.module.labyrinth.loot.EndOfRunHandler endOfRunHandler;
    private final WorldEditBridge worldEdit = new WorldEditBridge();

    /** Buffer (in blocks) between two consecutive pasted rooms along +X. */
    private static final int ROOM_PASTE_BUFFER = 50;

    public LabyrinthRoomLifecycle(RoomPicker roomPicker, MobSpawner mobSpawner) {
        this.roomPicker = roomPicker;
        this.mobSpawner = mobSpawner;
        this.logger = Main.getInstance().getLogger();
    }

    /**
     * Wired in by the module after construction (cyclic dependency between
     * the lifecycle and the door controller — neither owns the other).
     */
    public void setDoorController(DoorController doorController) {
        this.doorController = doorController;
    }

    public void setBossEncounterHandler(BossEncounterHandler handler) {
        this.bossEncounterHandler = handler;
    }

    public void setTriggerBus(fr.perrier.dungeons.module.labyrinth.event.LabyrinthTriggerBus bus) {
        this.triggerBus = bus;
    }

    public void setEndOfRunHandler(fr.perrier.dungeons.module.labyrinth.loot.EndOfRunHandler handler) {
        this.endOfRunHandler = handler;
    }

    /**
     * Move the players into {@code template} and prep the runtime state for
     * that room. Spawns mobs for combat / boss rooms.
     *
     * <p>The caller is responsible for incrementing
     * {@link LabyrinthRun#getCurrentRoomIndex()} *before* calling this
     * method (except for the initial lobby entry, which sits at index 0).</p>
     */
    public void enterRoom(LabyrinthRun run, LabyrinthRoom template, FloorInstance instance) {
        if (run == null || template == null) return;
        UUID roomUuid = UUID.randomUUID();
        run.setCurrentRoom(template);
        run.setCurrentRoomUuid(roomUuid);
        run.setPendingChoice(null);
        run.getRouteHistory().add(template);
        run.setCurrentRoomEnteredAtMs(System.currentTimeMillis());

        // Icon : COMBAT use the choice's rolled icon (set by the door layer
        // before this call) ; LOBBY is forced to NONE ; BOSS uses fixedIcon.
        switch (template.getType()) {
            case LOBBY -> run.setCurrentRoomIcon(RewardIcon.NONE);
            case BOSS -> run.setCurrentRoomIcon(template.getFixedIcon() != null
                    ? template.getFixedIcon() : RewardIcon.NONE);
            case COMBAT -> {
                // currentRoomIcon was set by the caller from the chosen DoorChoice side
                if (run.getCurrentRoomIcon() == null) run.setCurrentRoomIcon(RewardIcon.NONE);
            }
        }

        pasteRoomCopy(run, template);
        teleportPlayers(template, instance, run);

        if (triggerBus != null && instance != null) {
            for (UUID id : instance.getPlayers()) {
                org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(id);
                if (p != null && p.isOnline()) {
                    triggerBus.fireRoomEntered(run, instance, p);
                }
            }
        }

        if (template.getType() != RoomType.LOBBY) {
            int spawned = mobSpawner.spawnRoomMobs(run);
            run.getAliveMobsByRoom().put(roomUuid, spawned);
            if (spawned == 0) {
                // No mobs declared in a combat/boss room — auto-clear so the
                // run is not stuck. Logged so admins can fix the template.
                if (template.getType() == RoomType.BOSS) {
                    logger.severe("[MemoryLabyrinth] Boss room " + template.getId()
                            + " spawned 0 mobs at index=" + run.getCurrentRoomIndex()
                            + " — boss mobId likely misconfigured (check mobSpawns + MythicMobs id)."
                            + " The run will end immediately if this is the last boss of a finite floor.");
                } else {
                    logger.warning("[MemoryLabyrinth] Room " + template.getId()
                            + " (type=" + template.getType() + ") spawned 0 mobs — auto-clearing");
                }
                onRoomCleared(run);
            }
        } else if (!run.isLobbyDecisionPending()) {
            // Lobby is mob-less by design (CDC §1.7) — auto-clear here so the
            // exit door anchors get registered with the DoorController and the
            // player can actually walk out. For Infinite runs still awaiting
            // the leader's resume/new decision the flag holds us back ; the
            // resume / discard paths call enterLobby again with the flag
            // cleared, which re-enters this branch.
            onRoomCleared(run);
        }
    }

    /**
     * Decrement the alive-mob count for {@code roomUuid}. If it reaches
     * zero the room is flagged cleared and a {@link DoorChoice} is built.
     */
    public void onMobDeath(LabyrinthRun run, UUID roomUuid) {
        if (run == null || roomUuid == null) return;
        Integer alive = run.getAliveMobsByRoom().get(roomUuid);
        if (alive == null) return;
        int next = alive - 1;
        if (next <= 0) {
            run.getAliveMobsByRoom().remove(roomUuid);
            if (roomUuid.equals(run.getCurrentRoomUuid())) {
                onRoomCleared(run);
            }
        } else {
            run.getAliveMobsByRoom().put(roomUuid, next);
        }
    }

    /**
     * Called once when every mob of the current room is dead. Builds the
     * next {@link DoorChoice} and stores it in the run as
     * {@link LabyrinthRun#setPendingChoice(DoorChoice)}.
     *
     * <p>Boss kill side effects (revive prompt, save checkpoint) are
     * handled by P5 ({@code BossEncounterHandler}). This lifecycle just
     * proposes the next door.</p>
     */
    public void onRoomCleared(LabyrinthRun run) {
        long clearTimeMs = run.getCurrentRoomEnteredAtMs() > 0
                ? System.currentTimeMillis() - run.getCurrentRoomEnteredAtMs() : 0L;
        if (triggerBus != null) triggerBus.fireRoomCleared(run, null, clearTimeMs);

        // Boss-room side effects fire BEFORE the door is presented so the
        // revive prompt and tier bump are visible while the player walks
        // toward the single boss-exit door (CDC §6.3).
        boolean isBoss = run.getCurrentRoom() != null
                && run.getCurrentRoom().getType() == RoomType.BOSS;
        if (isBoss && bossEncounterHandler != null) {
            bossEncounterHandler.handleBossKill(run);
        }

        // Finite floor completion (CDC §1.3) : the last boss kill is the
        // natural end of the run — no next door to open. The classic check in
        // LabyrinthRunManager.advanceToChosen only fires on door traversal,
        // which never happens here (boss room has no further door for finite).
        // Two triggers, either is enough :
        //   1. currentRoomIndex >= maxRooms — the admin set an explicit cap.
        //   2. boss room has no exitDoors — admin intent is "boss is the end"
        //      (otherwise the player would be stuck : no door to traverse).
        if (isBoss && !run.isInfinite()) {
            boolean atOrPastCap = run.getCurrentRoomIndex() >= run.getMaxRooms();
            boolean bossHasNoExit = run.getCurrentRoom().getExitDoors() == null
                    || run.getCurrentRoom().getExitDoors().isEmpty();
            if (atOrPastCap || bossHasNoExit) {
                if (endOfRunHandler != null) {
                    endOfRunHandler.onFiniteCompletion(run);
                } else {
                    logger.warning("[MemoryLabyrinth] Finite floor boss killed at index "
                            + run.getCurrentRoomIndex()
                            + " but no EndOfRunHandler wired — run stuck");
                }
                return;
            }
        }

        DoorChoice next = roomPicker.pickNext(run);
        if (next == null) {
            logger.warning("[MemoryLabyrinth] No next room available for floor=" + run.getFloorId()
                    + " at index=" + run.getCurrentRoomIndex());
            return;
        }
        run.setPendingChoice(next);
        if (doorController != null) doorController.openDoors(run);
        if (triggerBus != null) triggerBus.fireDoorsProposed(run, null);
    }

    private void teleportPlayers(LabyrinthRoom template, FloorInstance instance, LabyrinthRun run) {
        if (instance == null || template.getPlayerSpawn() == null) return;
        // v2 procedural: TP to the offset-adjusted location of the pasted copy,
        // not the template region (which the admin is editing and should not be
        // overrun by players). The offset was set by pasteRoomCopy just before.
        String worldId = run.getInstanceWorldId() != null ? run.getInstanceWorldId() : template.getWorldId();
        World world = Bukkit.getWorld(worldId);
        if (world == null) {
            logger.warning("[MemoryLabyrinth] World not found for TP: " + worldId);
            return;
        }
        LabyrinthRoom.Vec3 spawn = template.getPlayerSpawn();
        LabyrinthRoom.Vec3 srcMin = template.getRegion() != null ? template.getRegion().getMin() : null;
        double dx = srcMin != null ? (spawn.getX() - srcMin.getX()) : 0;
        double dy = srcMin != null ? (spawn.getY() - srcMin.getY()) : 0;
        double dz = srcMin != null ? (spawn.getZ() - srcMin.getZ()) : 0;
        Location target = new Location(world,
                run.getCurrentRoomAnchorX() + dx,
                run.getCurrentRoomAnchorY() + dy,
                run.getCurrentRoomAnchorZ() + dz,
                spawn.getYaw(), spawn.getPitch());
        for (UUID id : instance.getPlayers()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) p.teleport(target);
        }
    }

    /**
     * Copies the room's source region from its template world into the
     * instance world at the next available offset along +X. Updates the
     * run's anchor pointers so subsequent TP / mob / door logic can map
     * template coords → instance coords by adding {@code (anchor - srcMin)}.
     *
     * <p>If worldedit is not loaded or the region is not defined, this is
     * a no-op and players fall back to the legacy TP-to-template behavior
     * (their spawn still uses the offset, which will be (0,0,0) — so they
     * land at the template, as before v2).</p>
     */
    private void pasteRoomCopy(LabyrinthRun run, LabyrinthRoom template) {
        if (run == null || template == null) return;
        LabyrinthRoom.Region region = template.getRegion();
        if (region == null || region.getMin() == null || region.getMax() == null) {
            logger.warning("[MemoryLabyrinth] Room " + template.getId()
                    + " has no region — paste skipped, player will see the template");
            return;
        }
        String srcWorld = template.getWorldId();
        String dstWorld = run.getInstanceWorldId() != null ? run.getInstanceWorldId() : srcWorld;
        if (srcWorld == null || dstWorld == null) {
            logger.warning("[MemoryLabyrinth] Room " + template.getId()
                    + " missing worldId — paste skipped");
            return;
        }

        LabyrinthRoom.Vec3 mn = region.getMin();
        LabyrinthRoom.Vec3 mx = region.getMax();
        int width = (int) Math.abs(mx.getX() - mn.getX()) + 1;
        // Anchor for this room = base + accumulated offset along +X
        int anchorX = run.getBaseAnchorX() + run.getNextRoomOffsetX();
        int anchorY = run.getBaseAnchorY();
        int anchorZ = run.getBaseAnchorZ();

        boolean ok = worldEdit.copyRegion(
                srcWorld,
                (int) Math.min(mn.getX(), mx.getX()),
                (int) Math.min(mn.getY(), mx.getY()),
                (int) Math.min(mn.getZ(), mx.getZ()),
                (int) Math.max(mn.getX(), mx.getX()),
                (int) Math.max(mn.getY(), mx.getY()),
                (int) Math.max(mn.getZ(), mx.getZ()),
                dstWorld, anchorX, anchorY, anchorZ);

        if (!ok) {
            logger.warning("[MemoryLabyrinth] Room " + template.getId()
                    + " paste FAILED — players will land at the template region");
            // Fall back to template coords so the run keeps moving (degraded mode).
            run.setCurrentRoomAnchorX((int) Math.min(mn.getX(), mx.getX()));
            run.setCurrentRoomAnchorY((int) Math.min(mn.getY(), mx.getY()));
            run.setCurrentRoomAnchorZ((int) Math.min(mn.getZ(), mx.getZ()));
            return;
        }

        run.setCurrentRoomAnchorX(anchorX);
        run.setCurrentRoomAnchorY(anchorY);
        run.setCurrentRoomAnchorZ(anchorZ);
        // Advance the cursor for the next room
        run.setNextRoomOffsetX(run.getNextRoomOffsetX() + width + ROOM_PASTE_BUFFER);
        logger.info("[MemoryLabyrinth] Pasted room " + template.getId() + " at "
                + dstWorld + "(" + anchorX + "," + anchorY + "," + anchorZ + ")");
    }
}
