package fr.perrier.dungeons.module.cinematic;

import fr.perrier.dungeons.common.module.ModuleBlockDescriptor;
import fr.perrier.dungeons.common.module.ModuleBlockDescriptor.BlockParameter;
import fr.perrier.dungeons.common.module.ModuleBlockDescriptor.BlockType;
import fr.perrier.dungeons.common.module.ModuleContext;
import fr.perrier.dungeons.common.module.NextDungeonModule;

import java.util.List;

/**
 * Cinematic module for NextDungeon.
 * Registers workflow blocks (triggers, actions, conditions) for
 * data-driven cinematic sequences (camera, NPCs, timeline events).
 *
 * <p>All cinematic data is stored in the database as JSON — no files on disk.</p>
 */
public class CinematicModule implements NextDungeonModule {

    @Override
    public void onEnable(ModuleContext ctx) {
        // Register action blocks (descriptors for Blockly UI)
        registerStartCinematic(ctx);
        registerStopCinematic(ctx);
        registerAddCameraWaypoint(ctx);
        registerMoveNpc(ctx);
        registerTimelineEvent(ctx);

        // Register trigger blocks
        registerCinematicEndTrigger(ctx);

        // Register condition blocks
        registerIsCinematicPlaying(ctx);

        // Register action execution handlers
        registerActionHandlers(ctx);
    }

    @Override
    public void onDisable() {
        // Cleanup if needed
    }

    @Override
    public String getId() {
        return "cinematic";
    }

    @Override
    public String getName() {
        return "Cinematic Module";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    // --- Block Registration ---

    private void registerStartCinematic(ModuleContext ctx) {
        ModuleBlockDescriptor block = new ModuleBlockDescriptor(
                "cinematic_start", BlockType.ACTION,
                "🎬 Start Cinematic",
                "Starts a cinematic sequence from JSON in the database",
                getId()
        );
        block.setColor("#9C27B0");
        block.setCategory("Cinematic");
        block.setParameters(List.of(
                new BlockParameter("cinematicId", "string", "Cinematic ID:",
                        "ID de la cinématique à lancer", "")
        ));
        ctx.getBlockRegistry().registerBlock(block);
    }

    private void registerStopCinematic(ModuleContext ctx) {
        ModuleBlockDescriptor block = new ModuleBlockDescriptor(
                "cinematic_stop", BlockType.ACTION,
                "⏹ Stop Cinematic",
                "Stops the currently playing cinematic for the player",
                getId()
        );
        block.setColor("#9C27B0");
        block.setCategory("Cinematic");
        ctx.getBlockRegistry().registerBlock(block);
    }

    private void registerAddCameraWaypoint(ModuleContext ctx) {
        ModuleBlockDescriptor block = new ModuleBlockDescriptor(
                "cinematic_add_camera_waypoint", BlockType.ACTION,
                "📷 Add Camera Waypoint",
                "Adds a camera waypoint to a cinematic timeline",
                getId()
        );
        block.setColor("#9C27B0");
        block.setCategory("Cinematic");
        block.setParameters(List.of(
                new BlockParameter("cinematicId", "string", "Cinematic ID:", "Target cinematic", ""),
                new BlockParameter("tick", "number", "Tick:", "Timeline tick position", "0"),
                new BlockParameter("x", "number", "X:", "X coordinate", "0"),
                new BlockParameter("y", "number", "Y:", "Y coordinate", "64"),
                new BlockParameter("z", "number", "Z:", "Z coordinate", "0"),
                new BlockParameter("yaw", "number", "Yaw:", "Camera yaw rotation", "0"),
                new BlockParameter("pitch", "number", "Pitch:", "Camera pitch rotation", "0"),
                createInterpolationParam()
        ));
        ctx.getBlockRegistry().registerBlock(block);
    }

    private void registerMoveNpc(ModuleContext ctx) {
        ModuleBlockDescriptor block = new ModuleBlockDescriptor(
                "cinematic_move_npc", BlockType.ACTION,
                "🚶 Move NPC",
                "Moves/synchronizes an NPC to a position on the timeline",
                getId()
        );
        block.setColor("#9C27B0");
        block.setCategory("Cinematic");
        block.setParameters(List.of(
                new BlockParameter("cinematicId", "string", "Cinematic ID:", "Target cinematic", ""),
                new BlockParameter("actorId", "string", "Actor ID:", "NPC actor identifier", ""),
                new BlockParameter("tick", "number", "Tick:", "Timeline tick position", "0"),
                new BlockParameter("x", "number", "X:", "X coordinate", "0"),
                new BlockParameter("y", "number", "Y:", "Y coordinate", "64"),
                new BlockParameter("z", "number", "Z:", "Z coordinate", "0"),
                new BlockParameter("yaw", "number", "Yaw:", "NPC facing yaw", "0"),
                new BlockParameter("pitch", "number", "Pitch:", "NPC facing pitch", "0")
        ));
        ctx.getBlockRegistry().registerBlock(block);
    }

    private void registerTimelineEvent(ModuleContext ctx) {
        ModuleBlockDescriptor block = new ModuleBlockDescriptor(
                "cinematic_timeline_event", BlockType.ACTION,
                "⏱ Timeline Event",
                "Injects a timed event into the cinematic timeline (command, sound, title, etc.)",
                getId()
        );
        block.setColor("#9C27B0");
        block.setCategory("Cinematic");

        BlockParameter eventType = new BlockParameter("eventType", "dropdown", "Event Type:",
                "Type of event to fire", "COMMAND");
        eventType.setOptions("COMMAND,TITLE,SOUND,PARTICLE,SPAWN_NPC,REMOVE_NPC");

        block.setParameters(List.of(
                new BlockParameter("cinematicId", "string", "Cinematic ID:", "Target cinematic", ""),
                new BlockParameter("tick", "number", "Tick:", "Timeline tick position", "0"),
                eventType,
                new BlockParameter("value", "string", "Value:", "Event value (command, sound name, etc.)", "")
        ));
        ctx.getBlockRegistry().registerBlock(block);
    }

    private void registerCinematicEndTrigger(ModuleContext ctx) {
        ModuleBlockDescriptor block = new ModuleBlockDescriptor(
                "cinematic_on_end", BlockType.TRIGGER,
                "🎬 When Cinematic Ends",
                "Triggers when a cinematic sequence finishes for a player",
                getId()
        );
        block.setColor("#9C27B0");
        block.setCategory("Cinematic");
        block.setParameters(List.of(
                new BlockParameter("cinematicId", "string", "Cinematic ID:",
                        "ID of the cinematic (leave empty for any)", "")
        ));
        ctx.getBlockRegistry().registerBlock(block);
    }

    private void registerIsCinematicPlaying(ModuleContext ctx) {
        ModuleBlockDescriptor block = new ModuleBlockDescriptor(
                "cinematic_is_playing", BlockType.CONDITION,
                "🎬 Is Cinematic Playing?",
                "Checks if a cinematic is currently playing for the player",
                getId()
        );
        block.setColor("#9C27B0");
        block.setCategory("Cinematic");
        block.setParameters(List.of(
                new BlockParameter("cinematicId", "string", "Cinematic ID:",
                        "ID of the cinematic (leave empty for any)", "")
        ));
        ctx.getBlockRegistry().registerBlock(block);
    }

    private BlockParameter createInterpolationParam() {
        BlockParameter p = new BlockParameter("interpolation", "dropdown", "Interpolation:",
                "Camera interpolation mode", "CATMULL_ROM");
        p.setOptions("LINEAR,CATMULL_ROM,CUBIC");
        return p;
    }

    // --- Action Handler Registration ---

    private void registerActionHandlers(ModuleContext ctx) {
        ctx.registerActionHandler("cinematic_start", params -> {
            String cinematicId = String.valueOf(params.getOrDefault("cinematicId", ""));
            System.out.println("[Cinematic] Starting cinematic: " + cinematicId);
            // TODO: Implement cinematic playback via CinematicPlayer
            return true;
        });

        ctx.registerActionHandler("cinematic_stop", params -> {
            System.out.println("[Cinematic] Stopping cinematic for player");
            // TODO: Implement cinematic stop via CinematicPlayer
            return true;
        });

        ctx.registerActionHandler("cinematic_add_camera_waypoint", params -> {
            String cinematicId = String.valueOf(params.getOrDefault("cinematicId", ""));
            Number tick = (Number) params.getOrDefault("tick", 0);
            Number x = (Number) params.getOrDefault("x", 0);
            Number y = (Number) params.getOrDefault("y", 64);
            Number z = (Number) params.getOrDefault("z", 0);
            Number yaw = (Number) params.getOrDefault("yaw", 0);
            Number pitch = (Number) params.getOrDefault("pitch", 0);
            String interpolation = String.valueOf(params.getOrDefault("interpolation", "LINEAR"));
            System.out.println("[Cinematic] Adding camera waypoint to '" + cinematicId
                    + "' at tick " + tick + " pos(" + x + "," + y + "," + z
                    + ") yaw=" + yaw + " pitch=" + pitch + " interp=" + interpolation);
            // TODO: Implement waypoint addition to CinematicData
            return true;
        });

        ctx.registerActionHandler("cinematic_move_npc", params -> {
            String cinematicId = String.valueOf(params.getOrDefault("cinematicId", ""));
            String actorId = String.valueOf(params.getOrDefault("actorId", ""));
            Number tick = (Number) params.getOrDefault("tick", 0);
            System.out.println("[Cinematic] Moving NPC '" + actorId + "' in cinematic '" + cinematicId + "' at tick " + tick);
            // TODO: Implement NPC movement via CinematicData
            return true;
        });

        ctx.registerActionHandler("cinematic_timeline_event", params -> {
            String cinematicId = String.valueOf(params.getOrDefault("cinematicId", ""));
            String eventType = String.valueOf(params.getOrDefault("eventType", "COMMAND"));
            String value = String.valueOf(params.getOrDefault("value", ""));
            Number tick = (Number) params.getOrDefault("tick", 0);
            System.out.println("[Cinematic] Timeline event in '" + cinematicId
                    + "' at tick " + tick + ": " + eventType + " = " + value);
            // TODO: Implement timeline event injection
            return true;
        });
    }
}
