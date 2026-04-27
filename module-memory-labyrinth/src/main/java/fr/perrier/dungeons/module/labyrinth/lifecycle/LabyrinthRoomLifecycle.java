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

        teleportPlayers(template, instance);

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
                logger.warning("[MemoryLabyrinth] Room " + template.getId()
                        + " (type=" + template.getType() + ") spawned 0 mobs — auto-clearing");
                onRoomCleared(run);
            }
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
        if (run.getCurrentRoom() != null
                && run.getCurrentRoom().getType() == RoomType.BOSS
                && bossEncounterHandler != null) {
            bossEncounterHandler.handleBossKill(run);
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

    private void teleportPlayers(LabyrinthRoom template, FloorInstance instance) {
        if (instance == null || template.getPlayerSpawn() == null) return;
        World world = Bukkit.getWorld(template.getWorldId());
        if (world == null) {
            logger.warning("[MemoryLabyrinth] World not found for TP: " + template.getWorldId());
            return;
        }
        LabyrinthRoom.Vec3 s = template.getPlayerSpawn();
        Location target = new Location(world, s.getX(), s.getY(), s.getZ());
        for (UUID id : instance.getPlayers()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) p.teleport(target);
        }
    }
}
