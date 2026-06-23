package fr.perrier.dungeons.module.labyrinth;

import fr.perrier.dungeons.common.module.ModuleBlockDescriptor;
import fr.perrier.dungeons.common.module.ModuleBlockDescriptor.BlockType;
import fr.perrier.dungeons.common.module.ModuleContext;
import fr.perrier.dungeons.common.module.NextDungeonModule;
import fr.perrier.dungeons.module.labyrinth.event.LabyrinthTriggerBus;
import fr.perrier.dungeons.module.labyrinth.generator.IconRoller;
import fr.perrier.dungeons.module.labyrinth.generator.RoomPicker;
import fr.perrier.dungeons.module.labyrinth.lifecycle.BossEncounterHandler;
import fr.perrier.dungeons.module.labyrinth.lifecycle.DoorController;
import fr.perrier.dungeons.module.labyrinth.lifecycle.LabyrinthInstanceReadyListener;
import fr.perrier.dungeons.module.labyrinth.lifecycle.LabyrinthLeaveListener;
import fr.perrier.dungeons.module.labyrinth.lifecycle.LabyrinthMobDeathListener;
import fr.perrier.dungeons.module.labyrinth.lifecycle.LabyrinthPlayerDeathListener;
import fr.perrier.dungeons.module.labyrinth.lifecycle.LabyrinthPlayerJoinListener;
import fr.perrier.dungeons.module.labyrinth.lifecycle.LabyrinthResumePromptListener;
import fr.perrier.dungeons.module.labyrinth.lifecycle.LabyrinthReviveListener;
import fr.perrier.dungeons.module.labyrinth.lifecycle.MobSpawner;
import fr.perrier.dungeons.module.labyrinth.loot.EndOfRunHandler;
import fr.perrier.dungeons.module.labyrinth.loot.LootCalculator;
import fr.perrier.dungeons.module.labyrinth.manager.LabyrinthRunManager;
import fr.perrier.dungeons.module.labyrinth.manager.LabyrinthSaveManager;
import fr.perrier.dungeons.module.labyrinth.manager.LootTableRegistry;
import fr.perrier.dungeons.module.labyrinth.manager.RoomTemplateRegistry;
import fr.perrier.dungeons.module.labyrinth.ui.TierIndicatorTask;
import fr.perrier.dungeons.spigot.Main;
import lombok.Getter;
import org.bukkit.Bukkit;

/**
 * Memory Labyrinth — procedural dungeon module for NextDungeon.
 *
 * <p>Implements a Hades/Archero-inspired room loop: lobby → combat rooms with
 * branching doors and visible reward icons → boss every 10 rooms → infinite
 * difficulty floor with per-boss checkpoints.</p>
 *
 * <p>Option C — the module drives its own room loop natively. It exposes
 * Blockly hooks (triggers, conditions, values) but no actions: workflows
 * subscribe to events to graft side effects (cinematics, sounds, titles).</p>
 */
public class MemoryLabyrinthModule implements NextDungeonModule {

    @Getter
    private RoomTemplateRegistry roomTemplateRegistry;

    @Getter
    private IconRoller iconRoller;

    @Getter
    private RoomPicker roomPicker;

    @Getter
    private MobSpawner mobSpawner;

    @Getter
    private LabyrinthRunManager runManager;

    @Getter
    private DoorController doorController;

    @Getter
    private BossEncounterHandler bossEncounterHandler;

    @Getter
    private LabyrinthSaveManager saveManager;

    @Getter
    private LootTableRegistry lootTableRegistry;

    @Getter
    private LootCalculator lootCalculator;

    @Getter
    private EndOfRunHandler endOfRunHandler;

    @Getter
    private LabyrinthTriggerBus triggerBus;

    private LabyrinthMobDeathListener mobDeathListener;
    private LabyrinthPlayerDeathListener playerDeathListener;
    private LabyrinthReviveListener reviveListener;
    private LabyrinthResumePromptListener resumePromptListener;
    private LabyrinthLeaveListener leaveListener;
    private LabyrinthInstanceReadyListener instanceReadyListener;
    private LabyrinthPlayerJoinListener playerJoinListener;
    private TierIndicatorTask tierIndicatorTask;

    @Override
    public void onEnable(ModuleContext ctx) {
        // P2 — pool & picker
        this.roomTemplateRegistry = new RoomTemplateRegistry();
        this.iconRoller = new IconRoller();
        this.roomPicker = new RoomPicker(roomTemplateRegistry, iconRoller);

        // P3 — runtime
        this.mobSpawner = new MobSpawner();
        this.runManager = new LabyrinthRunManager(roomTemplateRegistry, roomPicker, mobSpawner);
        this.mobDeathListener = new LabyrinthMobDeathListener(runManager);
        Bukkit.getPluginManager().registerEvents(mobDeathListener, Main.getInstance());

        // P4 — doors & navigation
        this.doorController = new DoorController(runManager);
        runManager.getRoomLifecycle().setDoorController(doorController);
        doorController.start();

        // P5 — boss & revive UI
        this.bossEncounterHandler = new BossEncounterHandler(runManager);
        runManager.setBossEncounterHandler(bossEncounterHandler);
        runManager.getRoomLifecycle().setBossEncounterHandler(bossEncounterHandler);
        this.playerDeathListener = new LabyrinthPlayerDeathListener(runManager);
        this.reviveListener = new LabyrinthReviveListener(bossEncounterHandler);
        Bukkit.getPluginManager().registerEvents(playerDeathListener, Main.getInstance());
        Bukkit.getPluginManager().registerEvents(reviveListener, Main.getInstance());

        // P6 — Infinite save & resume prompt
        this.saveManager = new LabyrinthSaveManager();
        runManager.setSaveManager(saveManager);
        bossEncounterHandler.setSaveManager(saveManager);
        this.resumePromptListener = new LabyrinthResumePromptListener(runManager);
        Bukkit.getPluginManager().registerEvents(resumePromptListener, Main.getInstance());
        this.leaveListener = new LabyrinthLeaveListener(runManager);
        Bukkit.getPluginManager().registerEvents(leaveListener, Main.getInstance());

        // P7 — Difficulty scaling : Mythic spawn bridge probe + tier
        // action-bar feedback. Scaling itself is already applied by
        // MobSpawner.applyTierScaling at spawn time (CDC §6.6).
        if (fr.perrier.dungeons.module.labyrinth.lifecycle.MythicMobsBridge.isAvailable()) {
            Main.getInstance().getLogger().info("[MemoryLabyrinth] MythicMobs detected — Mythic mob ids resolvable");
        }
        this.tierIndicatorTask = new TierIndicatorTask(runManager);
        tierIndicatorTask.start();

        // P8 — Loot tables, calculator, end-of-run orchestration
        this.lootTableRegistry = new LootTableRegistry();
        this.lootCalculator = new LootCalculator(lootTableRegistry);
        this.endOfRunHandler = new EndOfRunHandler(runManager, lootCalculator, saveManager, doorController);
        runManager.setEndOfRunHandler(endOfRunHandler);
        runManager.getRoomLifecycle().setEndOfRunHandler(endOfRunHandler);
        runManager.setLootRegistry(lootTableRegistry);
        playerDeathListener.setEndOfRunHandler(endOfRunHandler);

        // P9 — Blockly hooks : descriptor registration + trigger bus wiring
        this.triggerBus = new LabyrinthTriggerBus(ctx);
        runManager.getRoomLifecycle().setTriggerBus(triggerBus);
        bossEncounterHandler.setTriggerBus(triggerBus);
        endOfRunHandler.setTriggerBus(triggerBus);
        registerBlocklyDescriptors(ctx);

        // R3 — registries are now per-instance, populated at startRun
        // from each FloorData/Dungeon payload. No boot-time DB scan.

        // R4 — entry point hook : listen for FloorInstanceReadyEvent and
        // start a labyrinth run when the floor's type is LABYRINTH.
        this.instanceReadyListener = new LabyrinthInstanceReadyListener(runManager);
        Bukkit.getPluginManager().registerEvents(instanceReadyListener, Main.getInstance());

        // Replay the room teleport when a player connects after the run
        // already started — without this, enterRoom's teleportPlayers is
        // a no-op (player not yet online) and the player lands at the
        // world's default spawn instead of the lobby room.
        this.playerJoinListener = new LabyrinthPlayerJoinListener(runManager);
        Bukkit.getPluginManager().registerEvents(playerJoinListener, Main.getInstance());

        // P9 — register Blockly triggers / conditions / values
        // (no actions exposed: see CDC §5)

        // Manager wiring will be plugged in subsequent phases:
        //   P4: DoorController, DoorIconHologram
        //   P5: BossEncounterHandler, RevivePromptComponent
        //   P6: LabyrinthSaveManager, ResumeOrNewPrompt
        //   P7: DifficultyModifier scaling refinements (Mythic/MMOCore integration)
        //   P8: LootCalculator end-of-run distribution
    }

    @Override
    public void onDisable() {
        // Bukkit auto-unregisters listeners on plugin shutdown ; the module
        // host plugin (NextDungeon) is the registrar so the listener dies
        // with it. We still drop our state explicitly here.
        if (tierIndicatorTask != null) {
            tierIndicatorTask.stop();
        }
        if (doorController != null) {
            doorController.stop();
        }
        if (playerJoinListener != null) {
            playerJoinListener.shutdown();
        }
        if (lootTableRegistry != null) {
            lootTableRegistry.clear();
        }
        if (roomTemplateRegistry != null) {
            roomTemplateRegistry.clear();
        }
    }

    private static final String CATEGORY = "Memory Labyrinth";
    private static final String COLOR = "#3F51B5";

    private void registerBlocklyDescriptors(ModuleContext ctx) {
        // Triggers (CDC §5.1)
        register(ctx, LabyrinthTriggerBus.TRIGGER_ROOM_ENTERED, BlockType.TRIGGER,
                "🏛 When room entered",
                "Fires for each player on entry into a labyrinth room");
        register(ctx, LabyrinthTriggerBus.TRIGGER_ROOM_CLEARED, BlockType.TRIGGER,
                "✅ When room cleared",
                "Fires when every mob of the current room is dead");
        register(ctx, LabyrinthTriggerBus.TRIGGER_DOORS_PROPOSED, BlockType.TRIGGER,
                "🚪 When doors proposed",
                "Fires when the picker offers the next door choice");
        register(ctx, LabyrinthTriggerBus.TRIGGER_BOSS_KILLED, BlockType.TRIGGER,
                "👹 When boss killed",
                "Fires after a boss room is cleared (every 10th room)");
        register(ctx, LabyrinthTriggerBus.TRIGGER_RUN_ENDED, BlockType.TRIGGER,
                "🏁 When run ended",
                "Fires once per player at run end with success/loot variables");
        register(ctx, LabyrinthTriggerBus.TRIGGER_CHECKPOINT_SAVED, BlockType.TRIGGER,
                "💾 When checkpoint saved",
                "Fires after an Infinite save is upserted on boss kill");
        register(ctx, LabyrinthTriggerBus.TRIGGER_SAVE_INVALIDATED, BlockType.TRIGGER,
                "🗑 When save invalidated",
                "Fires when an Infinite save is deleted (wipe / voluntary exit)");

        // Conditions (CDC §5.2). Their evaluation logic will land in P11
        // when the host workflow engine grows a module-condition handler ;
        // for now the descriptors only seed the Blockly toolbox.
        register(ctx, "labyrinth_is_boss_room", BlockType.CONDITION,
                "❓ Is boss room?",
                "True when currentRoomIndex > 0 and divisible by 10");
        register(ctx, "labyrinth_is_infinite_floor", BlockType.CONDITION,
                "♾ Is infinite floor?",
                "True when the current run is on the infinite floor");
        register(ctx, "labyrinth_has_resumable_save", BlockType.CONDITION,
                "💾 Has resumable save?",
                "True when an Infinite save exists for the current party composition");

        // Values (CDC §5.3) intentionally not registered — the host
        // BlockType enum currently only exposes TRIGGER / ACTION /
        // CONDITION. The data they would expose is already passed via
        // the trigger data maps (e.g. tier, roomIndex on every trigger).
    }

    private static void register(ModuleContext ctx, String id, BlockType type, String label, String description) {
        ModuleBlockDescriptor block = new ModuleBlockDescriptor(id, type, label, description, "memory_labyrinth");
        block.setColor(COLOR);
        block.setCategory(CATEGORY);
        ctx.getBlockRegistry().registerBlock(block);
    }

    @Override
    public String getId() {
        return "memory_labyrinth";
    }

    @Override
    public String getName() {
        return "Memory Labyrinth Module";
    }

    @Override
    public String getVersion() {
        return "0.1.1";
    }
}
