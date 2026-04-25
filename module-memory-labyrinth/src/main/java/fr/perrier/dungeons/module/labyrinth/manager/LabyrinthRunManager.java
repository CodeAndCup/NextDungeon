package fr.perrier.dungeons.module.labyrinth.manager;

import fr.perrier.dungeons.module.labyrinth.generator.RoomPicker;
import fr.perrier.dungeons.module.labyrinth.lifecycle.BossEncounterHandler;
import fr.perrier.dungeons.module.labyrinth.lifecycle.DoorController;
import fr.perrier.dungeons.module.labyrinth.lifecycle.LabyrinthRoomLifecycle;
import fr.perrier.dungeons.module.labyrinth.lifecycle.MobSpawner;
import fr.perrier.dungeons.module.labyrinth.loot.EndOfRunHandler;
import fr.perrier.dungeons.module.labyrinth.loot.LabyrinthFloorConfig;
import fr.perrier.dungeons.module.labyrinth.model.DoorChoice;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthRun;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthSave;
import fr.perrier.dungeons.module.labyrinth.model.RewardIcon;
import fr.perrier.dungeons.module.labyrinth.model.RoomTemplate;
import fr.perrier.dungeons.module.labyrinth.ui.ResumeOrNewPrompt;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import fr.perrier.dungeons.spigot.parties.impl.DungeonPartyImpl;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Tracks every active Memory Labyrinth {@link LabyrinthRun} keyed by
 * {@link FloorInstance#getInstanceId()}.
 *
 * <p>Single-server scope : runs live on the server hosting the instance.
 * Cross-server persistence (Infinite saves) is handled separately by the
 * {@code LabyrinthSaveManager} (P6).</p>
 */
public class LabyrinthRunManager {

    private final Map<UUID, LabyrinthRun> runs = new ConcurrentHashMap<>();

    @Getter
    private final RoomTemplateRegistry roomTemplateRegistry;

    @Getter
    private final RoomPicker roomPicker;

    @Getter
    private final MobSpawner mobSpawner;

    @Getter
    private final LabyrinthRoomLifecycle roomLifecycle;

    private BossEncounterHandler bossEncounterHandler;
    private LabyrinthSaveManager saveManager;
    private DoorController doorController;
    private EndOfRunHandler endOfRunHandler;

    private final Logger logger;

    public LabyrinthRunManager(RoomTemplateRegistry registry, RoomPicker picker, MobSpawner spawner) {
        this.roomTemplateRegistry = registry;
        this.roomPicker = picker;
        this.mobSpawner = spawner;
        this.roomLifecycle = new LabyrinthRoomLifecycle(picker, spawner);
        this.logger = Main.getInstance().getLogger();
    }

    public void setBossEncounterHandler(BossEncounterHandler handler) {
        this.bossEncounterHandler = handler;
    }

    public void setSaveManager(LabyrinthSaveManager saveManager) {
        this.saveManager = saveManager;
    }

    public void setDoorController(DoorController doorController) {
        this.doorController = doorController;
    }

    public void setEndOfRunHandler(EndOfRunHandler handler) {
        this.endOfRunHandler = handler;
    }

    /**
     * Start a fresh run for a {@link FloorInstance}. Picks the lobby room,
     * teleports players, and registers the run.
     *
     * @return the freshly built run, or {@code null} if no lobby room is
     *         available for the floor (logged).
     */
    public LabyrinthRun startRun(FloorInstance instance, String floorId) {
        if (instance == null || floorId == null) return null;
        UUID instanceId = instance.getInstanceId();
        if (instanceId == null) {
            logger.warning("[MemoryLabyrinth] FloorInstance has no UUID — cannot start run");
            return null;
        }
        if (runs.containsKey(instanceId)) {
            logger.warning("[MemoryLabyrinth] Run already exists for instance " + instanceId);
            return runs.get(instanceId);
        }

        RoomTemplate lobby = roomPicker.pickLobby(floorId);
        if (lobby == null) {
            logger.warning("[MemoryLabyrinth] startRun aborted: no lobby for floor=" + floorId);
            return null;
        }

        LabyrinthRun run = new LabyrinthRun();
        run.setInstanceId(instanceId);
        run.setFloorId(floorId);
        run.setCurrentRoomIndex(0);
        run.setSeed(ThreadLocalRandom.current().nextLong());
        run.setStartedAtMs(System.currentTimeMillis());
        run.getInitialPlayerUuids().addAll(instance.getPlayers());

        runs.put(instanceId, run);
        roomLifecycle.enterRoom(run, lobby, instance);

        // Infinite — kick off the resume prompt asynchronously. The lobby
        // door has already been opened by the lifecycle ; we soft-lock
        // traversal via lobbyDecisionPending until the leader replies.
        if (run.isInfinite() && saveManager != null) {
            run.setLobbyDecisionPending(true);
            saveManager.findSaveForRun(run).thenAccept(save -> {
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    if (save == null) {
                        run.setLobbyDecisionPending(false);
                        return;
                    }
                    UUID leader = resolveLeader(instance);
                    if (leader == null) {
                        // No leader resolvable — fall back to the standard
                        // « new run » flow rather than dead-lock the lobby.
                        run.setLobbyDecisionPending(false);
                        return;
                    }
                    run.setInfiniteSaveId(save.getId());
                    ResumeOrNewPrompt.sendToLeader(leader, save);
                });
            }).exceptionally(ex -> {
                ex.printStackTrace(System.err);
                run.setLobbyDecisionPending(false);
                return null;
            });
        }
        return run;
    }

    /**
     * Apply the save bound to {@code run} and refresh the lobby door
     * choice with the resumed candidates. Called from the resume listener
     * when the leader picks « Reprendre ».
     */
    public void applyResumeAtLobby(LabyrinthRun run, Player sender) {
        if (run == null || saveManager == null) return;
        FloorInstance instance = Main.getInstance().getDungeonService().getInstance(run.getInstanceId());
        if (instance == null) {
            sender.sendMessage("§cInstance introuvable — resume impossible.");
            return;
        }
        saveManager.findSaveForRun(run).thenAccept(save -> {
            if (save == null) {
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    sender.sendMessage("§cLa save n'est plus disponible.");
                    run.setLobbyDecisionPending(false);
                });
                return;
            }
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                saveManager.applyResume(run, save);
                run.setLobbyDecisionPending(false);
                refreshLobbyDoors(run);
                broadcastInstance(instance, sender.getName() + " a repris la save (salle "
                        + save.getLastBossClearedRoom() + ", palier " + save.getDifficultyTier() + ")");
            });
        });
    }

    /**
     * Discard the existing save bound to the run and continue with a
     * fresh Infinite run. The lobby doors stay open as picked initially.
     */
    public void discardSaveAndContinue(LabyrinthRun run, Player sender) {
        if (run == null) return;
        if (saveManager != null) {
            String partyHash = LabyrinthSave.computePartyHash(run.getInitialPlayerUuids());
            saveManager.deleteByPartyHash(partyHash, run.getFloorId());
            run.setInfiniteSaveId(null);
        }
        run.setLobbyDecisionPending(false);
        FloorInstance instance = Main.getInstance().getDungeonService().getInstance(run.getInstanceId());
        if (instance != null) {
            broadcastInstance(instance, sender.getName() + " a démarré une nouvelle partie (save effacée)");
        }
    }

    private void refreshLobbyDoors(LabyrinthRun run) {
        DoorChoice next = roomPicker.pickNext(run);
        if (next == null) return;
        run.setPendingChoice(next);
        if (doorController != null) {
            doorController.closeDoors(run.getInstanceId());
            doorController.openDoors(run);
        }
    }

    private UUID resolveLeader(FloorInstance instance) {
        if (instance == null) return null;
        if (instance.getPlayers().size() == 1) return instance.getPlayers().iterator().next();
        for (UUID id : instance.getPlayers()) {
            if (DungeonPartyImpl.hasLeadParty(id)) return id;
        }
        // Fallback : any present player. Better than dead-locking the prompt.
        return instance.getPlayers().isEmpty() ? null : instance.getPlayers().iterator().next();
    }

    private void broadcastInstance(FloorInstance instance, String message) {
        for (UUID id : instance.getPlayers()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) p.sendMessage("§7▶ " + message);
        }
    }

    public LabyrinthRun getRun(UUID instanceId) {
        return instanceId == null ? null : runs.get(instanceId);
    }

    /**
     * Drop a run from the manager. Called when the instance terminates
     * (success / fail / shutdown).
     */
    public LabyrinthRun endRun(UUID instanceId) {
        if (instanceId == null) return null;
        return runs.remove(instanceId);
    }

    /**
     * Forwards a mob death notification to the lifecycle. Called by
     * {@code LabyrinthMobDeathListener} after metadata lookup.
     */
    public void onMobDeath(LabyrinthRun run, UUID roomUuid) {
        roomLifecycle.onMobDeath(run, roomUuid);
    }

    /**
     * Move a run to the room chosen at the pending door choice. The caller
     * passes the resolved {@link RoomTemplate} (left or right of the
     * pending {@link DoorChoice}). The door layer (P4) wires the actual
     * "player walks through door" detection.
     */
    public void advanceToChosen(LabyrinthRun run, RoomTemplate chosen,
                                RewardIcon chosenIcon,
                                FloorInstance instance) {
        if (run == null || chosen == null) return;
        if (bossEncounterHandler != null) bossEncounterHandler.onRoomTraversed(run);

        int nextIndex = run.getCurrentRoomIndex() + 1;
        // Finite floor completion (CDC §1.3 / §6.5) : the player has just
        // cleared the last room of an `easy/normal/hard` floor — end the
        // run on success rather than entering a non-existent next room.
        int maxRooms = LabyrinthFloorConfig.getMaxRooms(run.getFloorId());
        if (nextIndex > maxRooms && endOfRunHandler != null) {
            endOfRunHandler.onFiniteCompletion(run);
            return;
        }

        run.setCurrentRoomIndex(nextIndex);
        run.incrementIcon(chosenIcon);
        run.setCurrentRoomIcon(chosenIcon);
        roomLifecycle.enterRoom(run, chosen, instance);
    }

    public int getActiveRunCount() {
        return runs.size();
    }

    /**
     * Read-only view of every active run on this server. Returned
     * collection backs the live map (no copy) — callers must only read.
     */
    public java.util.Collection<LabyrinthRun> getRunsView() {
        return java.util.Collections.unmodifiableCollection(runs.values());
    }

    /**
     * Find the run that the given player is part of (matched against the
     * frozen initial composition — Q2 = A in CDC §11). Returns
     * {@code null} when the player is not in any active labyrinth run on
     * this server.
     */
    public LabyrinthRun findRunByPlayer(UUID playerId) {
        if (playerId == null) return null;
        for (LabyrinthRun run : runs.values()) {
            if (run.getInitialPlayerUuids().contains(playerId)) return run;
        }
        return null;
    }
}
