package fr.perrier.dungeons.module.worldedit;

import fr.perrier.dungeons.common.module.ModuleBlockDescriptor;
import fr.perrier.dungeons.common.module.ModuleBlockDescriptor.BlockParameter;
import fr.perrier.dungeons.common.module.ModuleContext;
import fr.perrier.dungeons.common.module.NextDungeonModule;

import java.util.List;
import java.util.Map;

/**
 * WorldEdit module for NextDungeon.
 * Registers workflow blocks for WorldEdit operations (set, cut, replace, schematic).
 *
 * <p>Replaces the built-in WorldEdit actions with dynamic module blocks.</p>
 */
public class WorldEditModule implements NextDungeonModule {

    private WorldEditManager manager;

    @Override
    public void onEnable(ModuleContext ctx) {
        // Initialize the manager
        this.manager = new WorldEditManager();

        // Register block descriptors (for Blockly UI)
        registerSetBlock(ctx);
        registerCutBlock(ctx);
        registerReplaceBlock(ctx);
        registerSchematicBlock(ctx);
        registerCopyRegionBlock(ctx);

        // Register action execution handlers
        registerActionHandlers(ctx);
    }

    @Override
    public void onDisable() {
        // No cleanup needed
    }

    @Override
    public String getId() {
        return "worldedit";
    }

    @Override
    public String getName() {
        return "WorldEdit Module";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    // --- Block Registration ---

    private void registerSetBlock(ModuleContext ctx) {
        ModuleBlockDescriptor block = new ModuleBlockDescriptor(
                "worldedit_set_action", ModuleBlockDescriptor.BlockType.ACTION,
                "🧱 WE Set Région (pos1/pos2)",
                "Remplit une région (pos1 → pos2) avec un ou plusieurs blocs. Supporte les patterns pondérés.",
                getId()
        );
        block.setColor("#8BC34A");
        block.setCategory("WorldEdit");
        block.setParameters(List.of(
                new BlockParameter("pos1_x", "number", "Pos1 X:", "Position 1 X", "0"),
                new BlockParameter("pos1_y", "number", "Pos1 Y:", "Position 1 Y", "64"),
                new BlockParameter("pos1_z", "number", "Pos1 Z:", "Position 1 Z", "0"),
                new BlockParameter("pos2_x", "number", "Pos2 X:", "Position 2 X", "0"),
                new BlockParameter("pos2_y", "number", "Pos2 Y:", "Position 2 Y", "64"),
                new BlockParameter("pos2_z", "number", "Pos2 Z:", "Position 2 Z", "0"),
                new BlockParameter("pattern", "string", "Pattern (ex: 70%stone,30%gravel):", "Block pattern", "stone")
        ));
        ctx.getBlockRegistry().registerBlock(block);
    }

    private void registerCutBlock(ModuleContext ctx) {
        ModuleBlockDescriptor block = new ModuleBlockDescriptor(
                "worldedit_cut_action", ModuleBlockDescriptor.BlockType.ACTION,
                "✂\uFE0F WE Cut Région (pos1/pos2)",
                "Supprime tous les blocs entre pos1 et pos2 (remplace par air).",
                getId()
        );
        block.setColor("#F44336");
        block.setCategory("WorldEdit");
        block.setParameters(List.of(
                new BlockParameter("pos1_x", "number", "Pos1 X:", "Position 1 X", "0"),
                new BlockParameter("pos1_y", "number", "Pos1 Y:", "Position 1 Y", "64"),
                new BlockParameter("pos1_z", "number", "Pos1 Z:", "Position 1 Z", "0"),
                new BlockParameter("pos2_x", "number", "Pos2 X:", "Position 2 X", "0"),
                new BlockParameter("pos2_y", "number", "Pos2 Y:", "Position 2 Y", "64"),
                new BlockParameter("pos2_z", "number", "Pos2 Z:", "Position 2 Z", "0")
        ));
        ctx.getBlockRegistry().registerBlock(block);
    }

    private void registerReplaceBlock(ModuleContext ctx) {
        ModuleBlockDescriptor block = new ModuleBlockDescriptor(
                "worldedit_replace_action", ModuleBlockDescriptor.BlockType.ACTION,
                "🔄 WE Replace Région (pos1/pos2)",
                "Remplace les blocs d'un type par un autre dans la région pos1→pos2.",
                getId()
        );
        block.setColor("#FF9800");
        block.setCategory("WorldEdit");
        block.setParameters(List.of(
                new BlockParameter("pos1_x", "number", "Pos1 X:", "Position 1 X", "0"),
                new BlockParameter("pos1_y", "number", "Pos1 Y:", "Position 1 Y", "64"),
                new BlockParameter("pos1_z", "number", "Pos1 Z:", "Position 1 Z", "0"),
                new BlockParameter("pos2_x", "number", "Pos2 X:", "Position 2 X", "0"),
                new BlockParameter("pos2_y", "number", "Pos2 Y:", "Position 2 Y", "64"),
                new BlockParameter("pos2_z", "number", "Pos2 Z:", "Position 2 Z", "0"),
                new BlockParameter("fromPattern", "string", "Blocs à remplacer (ex: stone,cobblestone):", "Source blocks", "stone"),
                new BlockParameter("toPattern", "string", "Remplacer par (ex: 70%stone,30%gravel):", "Destination pattern", "gravel")
        ));
        ctx.getBlockRegistry().registerBlock(block);
    }

    private void registerSchematicBlock(ModuleContext ctx) {
        ModuleBlockDescriptor block = new ModuleBlockDescriptor(
                "worldedit_schematic_action", ModuleBlockDescriptor.BlockType.ACTION,
                "📋 Placer un schematic WorldEdit",
                "Place un schematic WorldEdit à une position donnée.",
                getId()
        );
        block.setColor("#8BC34A");
        block.setCategory("WorldEdit");
        block.setParameters(List.of(
                new BlockParameter("filename", "string", "Nom du schematic:", "Schematic file name", "schematic.schem"),
                new BlockParameter("x", "number", "X:", "X coordinate", "0"),
                new BlockParameter("y", "number", "Y:", "Y coordinate", "64"),
                new BlockParameter("z", "number", "Z:", "Z coordinate", "0")
        ));
        ctx.getBlockRegistry().registerBlock(block);
    }

    private void registerCopyRegionBlock(ModuleContext ctx) {
        ModuleBlockDescriptor block = new ModuleBlockDescriptor(
                "worldedit_copy_region_action", ModuleBlockDescriptor.BlockType.ACTION,
                "📦 WE Copier région entre mondes",
                "Copie un cuboïde d'un monde source vers une position dans un monde destination. "
                        + "L'angle min de la source atterrit à l'anchor destination.",
                getId()
        );
        block.setColor("#03A9F4");
        block.setCategory("WorldEdit");
        block.setParameters(List.of(
                new BlockParameter("srcWorld", "string", "Monde source:", "Source world name", "world"),
                new BlockParameter("pos1_x", "number", "Src Pos1 X:", "Source position 1 X", "0"),
                new BlockParameter("pos1_y", "number", "Src Pos1 Y:", "Source position 1 Y", "64"),
                new BlockParameter("pos1_z", "number", "Src Pos1 Z:", "Source position 1 Z", "0"),
                new BlockParameter("pos2_x", "number", "Src Pos2 X:", "Source position 2 X", "0"),
                new BlockParameter("pos2_y", "number", "Src Pos2 Y:", "Source position 2 Y", "64"),
                new BlockParameter("pos2_z", "number", "Src Pos2 Z:", "Source position 2 Z", "0"),
                new BlockParameter("dstWorld", "string", "Monde destination:", "Destination world name", "world"),
                new BlockParameter("dst_x", "number", "Dst X:", "Destination anchor X", "200"),
                new BlockParameter("dst_y", "number", "Dst Y:", "Destination anchor Y", "64"),
                new BlockParameter("dst_z", "number", "Dst Z:", "Destination anchor Z", "0")
        ));
        ctx.getBlockRegistry().registerBlock(block);
    }

    // --- Action Handler Registration ---

    private void registerActionHandlers(ModuleContext ctx) {
        ctx.registerActionHandler("worldedit_set_action", manager::handleSet);
        ctx.registerActionHandler("worldedit_cut_action", manager::handleCut);
        ctx.registerActionHandler("worldedit_replace_action", manager::handleReplace);
        ctx.registerActionHandler("worldedit_schematic_action", manager::handleSchematic);
        ctx.registerActionHandler("worldedit_copy_region_action", manager::handleCopyRegion);
    }

    /**
     * Exposes the underlying {@link WorldEditManager} so other modules
     * (e.g. {@code module-memory-labyrinth}) can invoke
     * {@link WorldEditManager#copyRegion} directly without going through
     * a workflow action. Returned reference is only valid while this
     * module is enabled.
     */
    public WorldEditManager getManager() {
        return manager;
    }
}
