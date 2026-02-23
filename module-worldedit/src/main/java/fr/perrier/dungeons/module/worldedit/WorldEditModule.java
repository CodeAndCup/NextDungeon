package fr.perrier.dungeons.module.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import fr.perrier.dungeons.common.module.ModuleBlockDescriptor;
import fr.perrier.dungeons.common.module.ModuleBlockDescriptor.BlockParameter;
import fr.perrier.dungeons.common.module.ModuleContext;
import fr.perrier.dungeons.common.module.NextDungeonModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * WorldEdit module for NextDungeon.
 * Registers workflow blocks for WorldEdit operations (set, cut, replace, schematic).
 *
 * <p>Replaces the built-in WorldEdit actions with dynamic module blocks.</p>
 */
public class WorldEditModule implements NextDungeonModule {

    @Override
    public void onEnable(ModuleContext ctx) {
        // Register block descriptors (for Blockly UI)
        registerSetBlock(ctx);
        registerCutBlock(ctx);
        registerReplaceBlock(ctx);
        registerSchematicBlock(ctx);

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

    // --- Action Handler Registration ---

    private void registerActionHandlers(ModuleContext ctx) {
        ctx.registerActionHandler("worldedit_set_action", this::handleSet);
        ctx.registerActionHandler("worldedit_cut_action", this::handleCut);
        ctx.registerActionHandler("worldedit_replace_action", this::handleReplace);
        ctx.registerActionHandler("worldedit_schematic_action", this::handleSchematic);
    }

    // --- Execution Handlers ---

    private boolean handleSet(Map<String, Object> params) {
        String pattern = String.valueOf(params.getOrDefault("pattern", "stone"));
        World bukkitWorld = resolveWorld(params);
        if (bukkitWorld == null) return false;

        BlockVector3 min = getMin(params);
        BlockVector3 max = getMax(params);
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);
        CuboidRegion region = new CuboidRegion(weWorld, min, max);

        List<WeightedBlock> weightedBlocks = parsePattern(pattern);
        if (weightedBlocks.isEmpty()) {
            System.out.println("[WorldEdit] Invalid pattern: " + pattern);
            return false;
        }

        try (EditSession editSession = com.sk89q.worldedit.WorldEdit.getInstance().newEditSession(weWorld)) {
            Random random = new Random();
            for (BlockVector3 pos : region) {
                BlockState block = pickBlock(weightedBlocks, random);
                if (block != null) {
                    editSession.setBlock(pos, block);
                }
            }
        } catch (Exception e) {
            System.out.println("[WorldEdit] Error in set: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
        return true;
    }

    private boolean handleCut(Map<String, Object> params) {
        World bukkitWorld = resolveWorld(params);
        if (bukkitWorld == null) return false;

        BlockVector3 min = getMin(params);
        BlockVector3 max = getMax(params);
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);
        CuboidRegion region = new CuboidRegion(weWorld, min, max);

        try (EditSession editSession = com.sk89q.worldedit.WorldEdit.getInstance().newEditSession(weWorld)) {
            for (BlockVector3 pos : region) {
                editSession.setBlock(pos, Objects.requireNonNull(BlockTypes.AIR).getDefaultState());
            }
        } catch (Exception e) {
            System.out.println("[WorldEdit] Error in cut: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
        return true;
    }

    private boolean handleReplace(Map<String, Object> params) {
        String fromPattern = String.valueOf(params.getOrDefault("fromPattern", "stone"));
        String toPattern = String.valueOf(params.getOrDefault("toPattern", "gravel"));
        World bukkitWorld = resolveWorld(params);
        if (bukkitWorld == null) return false;

        List<com.sk89q.worldedit.world.block.BlockType> fromTypes = parseFromPattern(fromPattern);
        if (fromTypes.isEmpty()) return false;
        List<WeightedBlock> toBlocks = parsePattern(toPattern);
        if (toBlocks.isEmpty()) return false;

        BlockVector3 min = getMin(params);
        BlockVector3 max = getMax(params);
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);
        CuboidRegion region = new CuboidRegion(weWorld, min, max);

        try (EditSession editSession = com.sk89q.worldedit.WorldEdit.getInstance().newEditSession(weWorld)) {
            Random random = new Random();
            for (BlockVector3 pos : region) {
                BlockState current = editSession.getBlock(pos);
                if (fromTypes.contains(current.getBlockType())) {
                    BlockState replacement = pickBlock(toBlocks, random);
                    if (replacement != null) {
                        editSession.setBlock(pos, replacement);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[WorldEdit] Error in replace: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
        return true;
    }

    private boolean handleSchematic(Map<String, Object> params) {
        String filename = String.valueOf(params.getOrDefault("filename", ""));
        if (filename.isEmpty()) return false;

        int x = toInt(params.getOrDefault("x", 0));
        int y = toInt(params.getOrDefault("y", 64));
        int z = toInt(params.getOrDefault("z", 0));

        // Look for schematic file in plugin data folder
        File pluginDir = Bukkit.getPluginManager().getPlugin("NextDungeon") != null
                ? Bukkit.getPluginManager().getPlugin("NextDungeon").getDataFolder()
                : new File("plugins/NextDungeon");
        File file = new File(pluginDir, "schematics" + File.separator + filename + ".schematic");
        if (!file.exists()) {
            System.out.println("[WorldEdit] Schematic file not found: " + file.getAbsolutePath());
            return false;
        }

        World bukkitWorld = resolveWorld(params);
        if (bukkitWorld == null) bukkitWorld = Bukkit.getWorld("world");
        if (bukkitWorld == null) return false;

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);
        BlockVector3 to = BlockVector3.at(x, y, z);

        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            System.out.println("[WorldEdit] Could not detect schematic format for: " + filename);
            return false;
        }

        try (FileInputStream fis = new FileInputStream(file);
             ClipboardReader reader = format.getReader(fis)) {
            Clipboard clipboard = reader.read();
            try (EditSession editSession = com.sk89q.worldedit.WorldEdit.getInstance().newEditSession(weWorld)) {
                ClipboardHolder holder = new ClipboardHolder(clipboard);
                Operation operation = holder.createPaste(editSession).to(to).build();
                Operations.complete(operation);
            }
        } catch (Exception e) {
            System.out.println("[WorldEdit] Error pasting schematic: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
        return true;
    }

    // --- Helpers ---

    private World resolveWorld(Map<String, Object> params) {
        Object locObj = params.get("location");
        if (locObj instanceof Location loc && loc.getWorld() != null) {
            return loc.getWorld();
        }
        return Bukkit.getWorld("world");
    }

    private BlockVector3 getMin(Map<String, Object> params) {
        double p1x = toDouble(params.getOrDefault("pos1_x", 0));
        double p1y = toDouble(params.getOrDefault("pos1_y", 64));
        double p1z = toDouble(params.getOrDefault("pos1_z", 0));
        double p2x = toDouble(params.getOrDefault("pos2_x", 0));
        double p2y = toDouble(params.getOrDefault("pos2_y", 64));
        double p2z = toDouble(params.getOrDefault("pos2_z", 0));
        return BlockVector3.at(Math.min(p1x, p2x), Math.min(p1y, p2y), Math.min(p1z, p2z));
    }

    private BlockVector3 getMax(Map<String, Object> params) {
        double p1x = toDouble(params.getOrDefault("pos1_x", 0));
        double p1y = toDouble(params.getOrDefault("pos1_y", 64));
        double p1z = toDouble(params.getOrDefault("pos1_z", 0));
        double p2x = toDouble(params.getOrDefault("pos2_x", 0));
        double p2y = toDouble(params.getOrDefault("pos2_y", 64));
        double p2z = toDouble(params.getOrDefault("pos2_z", 0));
        return BlockVector3.at(Math.max(p1x, p2x), Math.max(p1y, p2y), Math.max(p1z, p2z));
    }

    static List<WeightedBlock> parsePattern(String pattern) {
        List<WeightedBlock> list = new ArrayList<>();
        double totalWeight = 0;
        for (String part : pattern.split(",")) {
            part = part.trim();
            double weight = 1.0;
            String blockId = part;
            if (part.contains("%")) {
                String[] split = part.split("%", 2);
                try { weight = Double.parseDouble(split[0].trim()); }
                catch (NumberFormatException ignored) {}
                blockId = split.length > 1 ? split[1].trim() : part;
            }
            if (!blockId.contains(":")) blockId = "minecraft:" + blockId.toLowerCase(Locale.ROOT);
            else blockId = blockId.toLowerCase(Locale.ROOT);
            com.sk89q.worldedit.world.block.BlockType bt = BlockTypes.get(blockId);
            if (bt == null) continue;
            totalWeight += weight;
            list.add(new WeightedBlock(bt.getDefaultState(), weight));
        }
        if (totalWeight > 0) {
            for (WeightedBlock wb : list) wb.normalizedWeight = wb.weight / totalWeight;
        }
        return list;
    }

    private List<com.sk89q.worldedit.world.block.BlockType> parseFromPattern(String pattern) {
        List<com.sk89q.worldedit.world.block.BlockType> types = new ArrayList<>();
        for (String part : pattern.split(",")) {
            String blockId = part.trim();
            if (!blockId.contains(":")) blockId = "minecraft:" + blockId.toLowerCase(Locale.ROOT);
            else blockId = blockId.toLowerCase(Locale.ROOT);
            com.sk89q.worldedit.world.block.BlockType bt = BlockTypes.get(blockId);
            if (bt != null) types.add(bt);
        }
        return types;
    }

    private static BlockState pickBlock(List<WeightedBlock> blocks, Random random) {
        if (blocks.size() == 1) return blocks.get(0).blockState;
        double r = random.nextDouble();
        double cumul = 0;
        for (WeightedBlock wb : blocks) {
            cumul += wb.normalizedWeight;
            if (r <= cumul) return wb.blockState;
        }
        return blocks.get(blocks.size() - 1).blockState;
    }

    private static int toInt(Object obj) {
        if (obj instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(obj)); } catch (Exception e) { return 0; }
    }

    private static double toDouble(Object obj) {
        if (obj instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(obj)); } catch (Exception e) { return 0; }
    }

    static class WeightedBlock {
        final BlockState blockState;
        final double weight;
        double normalizedWeight;
        WeightedBlock(BlockState blockState, double weight) {
            this.blockState = blockState;
            this.weight = weight;
            this.normalizedWeight = weight;
        }
    }
}
