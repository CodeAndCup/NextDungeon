package fr.perrier.dungeons.module.labyrinth.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.perrier.dungeons.common.model.labyrinth.LabyrinthDungeonConfig;
import fr.perrier.dungeons.common.model.labyrinth.LabyrinthFloorConfig;
import fr.perrier.dungeons.common.model.labyrinth.LabyrinthRoom;
import fr.perrier.dungeons.common.model.labyrinth.RewardIcon;
import fr.perrier.dungeons.module.labyrinth.generator.RoomPicker;
import fr.perrier.dungeons.module.labyrinth.lifecycle.BossEncounterHandler;
import fr.perrier.dungeons.module.labyrinth.lifecycle.LabyrinthRoomLifecycle;
import fr.perrier.dungeons.module.labyrinth.lifecycle.MobSpawner;
import fr.perrier.dungeons.module.labyrinth.loot.EndOfRunHandler;
import fr.perrier.dungeons.module.labyrinth.model.DoorChoice;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthRun;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthSave;
import fr.perrier.dungeons.module.labyrinth.ui.ResumeOrNewPrompt;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.database.DatabaseManager;
import fr.perrier.dungeons.spigot.model.Floor;
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

    public void setEndOfRunHandler(EndOfRunHandler handler) {
        this.endOfRunHandler = handler;
    }

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    /**
     * Start a fresh labyrinth run for a {@link FloorInstance} that has
     * just become ready. Async because the dungeon-level config (the
     * shared room pool) is fetched from the database.
     *
     * <p>The {@code callback} fires on the Bukkit main thread when the
     * run has been registered and the lobby has been entered (or with
     * {@code null} if the run could not be started — see logs).</p>
     */
    public void startRun(FloorInstance instance, String floorId, java.util.function.Consumer<LabyrinthRun> callback) {
        if (instance == null || floorId == null) {
            if (callback != null) callback.accept(null);
            return;
        }
        UUID instanceId = instance.getInstanceId();
        if (instanceId == null) {
            logger.warning("[MemoryLabyrinth] FloorInstance has no UUID — cannot start run");
            if (callback != null) callback.accept(null);
            return;
        }
        if (runs.containsKey(instanceId)) {
            logger.warning("[MemoryLabyrinth] Run already exists for instance " + instanceId);
            if (callback != null) callback.accept(runs.get(instanceId));
            return;
        }

        Floor floor = Floor.getFloor(floorId);
        if (floor == null || floor.getLabyrinthFloorConfig() == null) {
            logger.warning("[MemoryLabyrinth] No labyrinthFloorConfig for floor=" + floorId);
            if (callback != null) callback.accept(null);
            return;
        }
        LabyrinthFloorConfig floorConfig = floor.getLabyrinthFloorConfig();
        String dungeonId = floor.getDungeonId();
        if (dungeonId == null || dungeonId.isEmpty()) {
            logger.warning("[MemoryLabyrinth] Floor " + floorId + " has no dungeonId");
            if (callback != null) callback.accept(null);
            return;
        }

        DatabaseManager db = Main.getInstance().getDatabaseManager();
        if (db == null) {
            logger.warning("[MemoryLabyrinth] DatabaseManager unavailable — cannot start run");
            if (callback != null) callback.accept(null);
            return;
        }

        db.loadDungeon(dungeonId).thenAccept(json -> Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            LabyrinthDungeonConfig dungeonConfig = parseDungeonConfig(json);
            if (dungeonConfig == null) {
                logger.warning("[MemoryLabyrinth] Dungeon " + dungeonId + " has no labyrinthDungeonConfig");
                if (callback != null) callback.accept(null);
                return;
            }
            LabyrinthRun run = finalizeStart(instance, floorId, instanceId, floorConfig, dungeonConfig);
            if (callback != null) callback.accept(run);
        })).exceptionally(ex -> {
            logger.warning("[MemoryLabyrinth] startRun async failure: " + ex.getMessage());
            Bukkit.getScheduler().runTask(Main.getInstance(),
                    () -> { if (callback != null) callback.accept(null); });
            return null;
        });
    }

    private LabyrinthRun finalizeStart(FloorInstance instance, String floorId, UUID instanceId,
                                       LabyrinthFloorConfig floorConfig, LabyrinthDungeonConfig dungeonConfig) {
        // Populate per-instance pools from the dungeon-level config.
        roomTemplateRegistry.load(instanceId, dungeonConfig);
        if (floorConfig.getLootTable() != null && lootRegistry != null) {
            lootRegistry.load(instanceId, floorConfig.getLootTable());
        }

        LabyrinthRun run = new LabyrinthRun();
        run.setInstanceId(instanceId);
        run.setFloorId(floorId);
        run.setTagFilter(floorConfig.getTagFilter());
        run.setMaxRooms(floorConfig.getMaxRooms() <= 0 ? Integer.MAX_VALUE : floorConfig.getMaxRooms());
        run.setCurrentRoomIndex(0);
        run.setSeed(ThreadLocalRandom.current().nextLong());
        run.setStartedAtMs(System.currentTimeMillis());
        run.getInitialPlayerUuids().addAll(instance.getPlayers());
        runs.put(instanceId, run);

        // R5 — TP players to the dungeon spawn point first ; the lobby is
        // entered only once the (Infinite) resume/new decision is made.
        teleportToDungeonSpawn(instance, dungeonConfig);

        if (floorConfig.isSaveEnabled() && saveManager != null) {
            run.setLobbyDecisionPending(true);
            saveManager.findSaveForRun(run).thenAccept(save ->
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                        if (save == null) {
                            // No save → straight to lobby (fresh run).
                            run.setLobbyDecisionPending(false);
                            enterLobby(run, instance);
                            return;
                        }
                        UUID leader = resolveLeader(instance);
                        if (leader == null) {
                            // No leader resolvable — fall back to fresh run.
                            run.setLobbyDecisionPending(false);
                            enterLobby(run, instance);
                            return;
                        }
                        run.setInfiniteSaveId(save.getId());
                        ResumeOrNewPrompt.sendToLeader(leader, save);
                    })).exceptionally(ex -> {
                logger.warning("[MemoryLabyrinth] save lookup failure: " + ex.getMessage());
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    run.setLobbyDecisionPending(false);
                    enterLobby(run, instance);
                });
                return null;
            });
        } else {
            enterLobby(run, instance);
        }
        return run;
    }

    private void teleportToDungeonSpawn(FloorInstance instance, LabyrinthDungeonConfig dungeonConfig) {
        if (instance == null || dungeonConfig == null || dungeonConfig.getDungeonSpawn() == null) return;
        org.bukkit.World world = Bukkit.getWorld(dungeonConfig.getWorldId());
        if (world == null) {
            logger.warning("[MemoryLabyrinth] Dungeon world not found: " + dungeonConfig.getWorldId());
            return;
        }
        LabyrinthRoom.Vec3 s = dungeonConfig.getDungeonSpawn();
        org.bukkit.Location target = new org.bukkit.Location(world, s.getX(), s.getY(), s.getZ());
        for (UUID id : instance.getPlayers()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) p.teleport(target);
        }
    }

    private void enterLobby(LabyrinthRun run, FloorInstance instance) {
        LabyrinthRoom lobby = roomPicker.pickLobby(
                run.getInstanceId(), run.getFloorId(), run.getTagFilter());
        if (lobby == null) {
            logger.warning("[MemoryLabyrinth] No lobby room for floor=" + run.getFloorId()
                    + " — players will stay at the dungeon spawn");
            return;
        }
        roomLifecycle.enterRoom(run, lobby, instance);
    }

    /**
     * Inject point — set by the module after construction. The run
     * manager doesn't strictly own the loot registry but it needs to
     * push the per-instance loot table at startRun.
     */
    private LootTableRegistry lootRegistry;

    public void setLootRegistry(LootTableRegistry lootRegistry) {
        this.lootRegistry = lootRegistry;
    }

    /**
     * Pull {@code labyrinthDungeonConfig} out of a dashboard
     * {@code DungeonEntry}-shaped JSON. Returns {@code null} when the
     * dungeon is not a labyrinth or the field is missing.
     */
    private LabyrinthDungeonConfig parseDungeonConfig(String dungeonJson) {
        if (dungeonJson == null || dungeonJson.isEmpty()) return null;
        try {
            JsonElement root = GSON.fromJson(dungeonJson, JsonElement.class);
            if (root == null || !root.isJsonObject()) return null;
            JsonObject obj = root.getAsJsonObject();
            JsonElement payload = obj.get("labyrinthDungeonConfig");
            if (payload == null || payload.isJsonNull()) return null;
            return GSON.fromJson(payload, LabyrinthDungeonConfig.class);
        } catch (Exception e) {
            logger.warning("[MemoryLabyrinth] Cannot parse dungeon config: " + e.getMessage());
            return null;
        }
    }

    /**
     * Resume the player(s) onto the room right after the last cleared
     * boss (CDC §6.1 / §6.5 — R5 redesign). The lobby is bypassed
     * entirely on resume : the players go straight to room
     * {@code lastBossClearedRoom + 1}.
     */
    public void applyResume(LabyrinthRun run, Player sender) {
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
                    enterLobby(run, instance);
                });
                return;
            }
            Bukkit.getScheduler().runTask(Main.getInstance(),
                    () -> resumeAtNextRoom(run, save, instance, sender));
        });
    }

    private void resumeAtNextRoom(LabyrinthRun run, LabyrinthSave save, FloorInstance instance, Player sender) {
        // Apply the persisted state — sets currentRoomIndex = lastBossClearedRoom,
        // tier, seed, iconCounts.
        saveManager.applyResume(run, save);
        run.setLobbyDecisionPending(false);

        // Pick the next combat room. pickNext sees currentRoomIndex =
        // lastBossClearedRoom (multiple of 10), so nextIndex = +1 is
        // guaranteed not to be a boss — it returns a combat pair.
        DoorChoice choice = roomPicker.pickNext(run);
        if (choice == null) {
            sender.sendMessage("§cAucune salle disponible pour reprendre — fallback lobby.");
            enterLobby(run, instance);
            return;
        }
        LabyrinthRoom next = choice.getLeft();
        run.setCurrentRoomIndex(save.getLastBossClearedRoom() + 1);
        // No door choice was made on resume — neutral icon for this entry.
        run.setCurrentRoomIcon(RewardIcon.NONE);
        roomLifecycle.enterRoom(run, next, instance);

        broadcastInstance(instance, sender.getName() + " a repris la save (salle "
                + save.getLastBossClearedRoom() + ", palier " + save.getDifficultyTier() + ")");
    }

    /**
     * Discard the bound save and start a fresh run by entering the
     * lobby (R5 redesign — players were waiting at the dungeon spawn).
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
            enterLobby(run, instance);
            broadcastInstance(instance, sender.getName() + " a démarré une nouvelle partie (save effacée)");
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
        roomTemplateRegistry.release(instanceId);
        if (lootRegistry != null) lootRegistry.release(instanceId);
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
     * passes the resolved {@link LabyrinthRoom} (left or right of the
     * pending {@link DoorChoice}). The door layer (P4) wires the actual
     * "player walks through door" detection.
     */
    public void advanceToChosen(LabyrinthRun run, LabyrinthRoom chosen,
                                RewardIcon chosenIcon,
                                FloorInstance instance) {
        if (run == null || chosen == null) return;
        if (bossEncounterHandler != null) bossEncounterHandler.onRoomTraversed(run);

        int nextIndex = run.getCurrentRoomIndex() + 1;
        // Finite floor completion (CDC §1.3 / §6.5) — read maxRooms from
        // the run's cached cap, set at startRun from LabyrinthFloorConfig.
        if (nextIndex > run.getMaxRooms() && endOfRunHandler != null) {
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
