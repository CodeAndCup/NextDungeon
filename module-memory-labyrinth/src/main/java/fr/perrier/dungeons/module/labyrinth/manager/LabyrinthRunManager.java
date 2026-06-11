package fr.perrier.dungeons.module.labyrinth.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.perrier.dungeons.common.model.labyrinth.LabyrinthDungeonConfig;
import fr.perrier.dungeons.common.model.labyrinth.LabyrinthFloorConfig;
import fr.perrier.dungeons.common.model.labyrinth.LabyrinthRoom;
import fr.perrier.dungeons.common.model.labyrinth.RewardIcon;
import fr.perrier.dungeons.module.labyrinth.blessing.BlessingBridge;
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
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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
        // v2 procedural paste — rooms are copied into this world at a sliding
        // X-offset from baseAnchorX (5000 default, far from typical builds).
        // Currently same as the template world ; can diverge in v3 if rooms
        // live in a separate template world.
        if (dungeonConfig.getWorldId() != null && !dungeonConfig.getWorldId().isEmpty()) {
            run.setInstanceWorldId(dungeonConfig.getWorldId());
        }
        runs.put(instanceId, run);

        // R5 — TP players to the dungeon spawn point first ; the lobby is
        // entered only once the (Infinite) resume/new decision is made.
        teleportToDungeonSpawn(instance, dungeonConfig);

        // NOTE: the blessing session is intentionally NOT started here. The
        // plugin opens a GUI on `dungeon start`, which would capture the
        // screen and make the Infinite resume/new chat prompt unclickable.
        // It is started once the run actually proceeds — enterLobby() (fresh
        // / new game) or resumeAtNextRoom() (resume), i.e. after the choice.

        if (floorConfig.isSaveEnabled() && saveManager != null) {
            run.setLobbyDecisionPending(true);
            saveManager.findSaveForRun(run).thenAccept(save ->
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                        // Guard: the run may have been endRun()'d while the
                        // DB lookup was in flight (instance cancelled, …).
                        if (runs.get(instanceId) != run) return;
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
                    if (runs.get(instanceId) != run) return;
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
        World world = Bukkit.getWorld(dungeonConfig.getWorldId());
        if (world == null) {
            logger.warning("[MemoryLabyrinth] Dungeon world not found: " + dungeonConfig.getWorldId());
            return;
        }
        LabyrinthRoom.Vec3 s = dungeonConfig.getDungeonSpawn();
        Location target = new Location(world, s.getX(), s.getY(), s.getZ(),
                s.getYaw(), s.getPitch());
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

        // Start the blessing session now that the run is actually beginning
        // (deferred from finalizeStart so the Infinite resume/new prompt stays
        // clickable). enterLobby() runs for fresh runs only — finite floors +
        // Infinite "new game".
        BlessingBridge.startSession(run, instance);

        // Entry blessing : each player receives a passive offer on entering
        // the lobby. Resume bypasses the lobby via resumeAtNextRoom(), so
        // resumed parties keep their existing blessings (no entry offer).
        BlessingBridge.offerPassive(instance);
    }

    /**
     * Inject point — set by the module after construction. The run
     * manager doesn't strictly own the loot registry but it needs to
     * push the per-instance loot table at startRun.
     */
    @Setter
    private LootTableRegistry lootRegistry;

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
        UUID instanceId = run.getInstanceId();
        FloorInstance instance = Main.getInstance().getDungeonService().getInstance(instanceId);
        if (instance == null) {
            sender.sendMessage("§cInstance introuvable — resume impossible.");
            return;
        }
        saveManager.findSaveForRun(run).thenAccept(save -> {
            if (save == null) {
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    if (runs.get(instanceId) != run) return;
                    sender.sendMessage("§cLa save n'est plus disponible.");
                    run.setLobbyDecisionPending(false);
                    enterLobby(run, instance);
                });
                return;
            }
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                if (runs.get(instanceId) != run) return;
                resumeAtNextRoom(run, save, instance, sender);
            });
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

        // Start the blessing session on resume too — fired after the leader's
        // click so the prompt was clickable. No entry offer on resume: the
        // party keeps the blessings tied to this party-hash dungeon id.
        BlessingBridge.startSession(run, instance);

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
     *
     * <p>The run is removed from the live map <em>before</em> per-instance
     * registries are released, so any callback racing against endRun
     * (DB writes, scheduled triggers) sees a {@code null} run via
     * {@link #getRun(UUID)} and bails out instead of touching a freed
     * registry slot.</p>
     */
    public LabyrinthRun endRun(UUID instanceId) {
        if (instanceId == null) return null;
        LabyrinthRun previous = runs.remove(instanceId);
        roomTemplateRegistry.release(instanceId);
        if (lootRegistry != null) lootRegistry.release(instanceId);
        return previous;
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
