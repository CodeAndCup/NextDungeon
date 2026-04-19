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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * WorldEdit manager for NextDungeon.
 * Handles all WorldEdit operations (set, cut, replace, schematic).
 */
public class WorldEditManager {
    private static final Logger LOGGER = Logger.getLogger("NextDungeon-WorldEdit");

    public WorldEditManager() {
        LOGGER.info("WorldEditManager initialized");
    }

    /**
     * Fills a cuboid region with a block pattern.
     *
     * @param params parameters including positions and pattern
     * @return true if successful
     */
    public boolean handleSet(Map<String, Object> params) {
        try {
            String pattern = String.valueOf(params.getOrDefault("pattern", "stone"));
            World bukkitWorld = resolveWorld(params);
            if (bukkitWorld == null) {
                LOGGER.warning("Could not resolve world for set operation");
                return false;
            }

            BlockVector3 min = getMin(params);
            BlockVector3 max = getMax(params);
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);
            CuboidRegion region = new CuboidRegion(weWorld, min, max);

            List<WeightedBlock> weightedBlocks = parsePattern(pattern);
            if (weightedBlocks.isEmpty()) {
                LOGGER.warning("[WorldEdit] Invalid pattern: " + pattern);
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
            }
            LOGGER.info("Set operation completed in world: " + bukkitWorld.getName());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in set operation", e);
            return false;
        }
    }

    /**
     * Clears a cuboid region (removes all blocks).
     *
     * @param params parameters including positions
     * @return true if successful
     */
    public boolean handleCut(Map<String, Object> params) {
        try {
            World bukkitWorld = resolveWorld(params);
            if (bukkitWorld == null) {
                LOGGER.warning("Could not resolve world for cut operation");
                return false;
            }

            BlockVector3 min = getMin(params);
            BlockVector3 max = getMax(params);
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);
            CuboidRegion region = new CuboidRegion(weWorld, min, max);

            try (EditSession editSession = com.sk89q.worldedit.WorldEdit.getInstance().newEditSession(weWorld)) {
                for (BlockVector3 pos : region) {
                    BlockState airState = Objects.requireNonNull(BlockTypes.AIR).getDefaultState();
                    editSession.setBlock(pos, airState);
                }
            }
            LOGGER.info("Cut operation completed in world: " + bukkitWorld.getName());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in cut operation", e);
            return false;
        }
    }

    /**
     * Replaces blocks of one type with another pattern in a region.
     *
     * @param params parameters including from/to patterns
     * @return true if successful
     */
    public boolean handleReplace(Map<String, Object> params) {
        try {
            String fromPattern = String.valueOf(params.getOrDefault("fromPattern", "stone"));
            String toPattern = String.valueOf(params.getOrDefault("toPattern", "gravel"));
            World bukkitWorld = resolveWorld(params);
            if (bukkitWorld == null) {
                LOGGER.warning("Could not resolve world for replace operation");
                return false;
            }

            List<com.sk89q.worldedit.world.block.BlockType> fromTypes = parseFromPattern(fromPattern);
            if (fromTypes.isEmpty()) {
                LOGGER.warning("Invalid from pattern: " + fromPattern);
                return false;
            }
            List<WeightedBlock> toBlocks = parsePattern(toPattern);
            if (toBlocks.isEmpty()) {
                LOGGER.warning("Invalid to pattern: " + toPattern);
                return false;
            }

            BlockVector3 min = getMin(params);
            BlockVector3 max = getMax(params);
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);
            CuboidRegion region = new CuboidRegion(weWorld, min, max);

            try (EditSession editSession = com.sk89q.worldedit.WorldEdit.getInstance().newEditSession(weWorld)) {
                Random random = new Random();
                int replacedCount = 0;
                for (BlockVector3 pos : region) {
                    BlockState current = editSession.getBlock(pos);
                    if (fromTypes.contains(current.getBlockType())) {
                        BlockState replacement = pickBlock(toBlocks, random);
                        if (replacement != null) {
                            editSession.setBlock(pos, replacement);
                            replacedCount++;
                        }
                    }
                }
                LOGGER.info("Replace operation completed: " + replacedCount + " blocks replaced");
            }
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in replace operation", e);
            return false;
        }
    }

    /**
     * Pastes a schematic file at the specified location.
     *
     * @param params parameters including filename and coordinates
     * @return true if successful
     */
    public boolean handleSchematic(Map<String, Object> params) {
        try {
            String filename = String.valueOf(params.getOrDefault("filename", ""));
            if (filename.isEmpty()) {
                LOGGER.warning("Schematic filename is empty");
                return false;
            }

            int x = toInt(params.getOrDefault("x", 0));
            int y = toInt(params.getOrDefault("y", 64));
            int z = toInt(params.getOrDefault("z", 0));

            // Sanitize filename to prevent path traversal
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                LOGGER.warning("Invalid schematic filename (path traversal attempt): " + filename);
                return false;
            }

            // Look for schematic file in plugin data folder
            File pluginDir = Bukkit.getPluginManager().getPlugin("NextDungeon") != null
                    ? Bukkit.getPluginManager().getPlugin("NextDungeon").getDataFolder()
                    : new File("plugins/NextDungeon");
            File file = new File(pluginDir, "schematics" + File.separator + filename + ".schematic");
            if (!file.exists()) {
                LOGGER.warning("Schematic file not found: " + file.getAbsolutePath());
                return false;
            }

            World bukkitWorld = resolveWorld(params);
            if (bukkitWorld == null) bukkitWorld = Bukkit.getWorld("world");
            if (bukkitWorld == null) {
                LOGGER.warning("Could not find world for schematic paste");
                return false;
            }

            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);
            BlockVector3 to = BlockVector3.at(x, y, z);

            ClipboardFormat format = ClipboardFormats.findByFile(file);
            if (format == null) {
                LOGGER.warning("Could not detect schematic format for: " + filename);
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
            }
            LOGGER.info("Schematic '" + filename + "' pasted at " + x + "," + y + "," + z);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error pasting schematic", e);
            return false;
        }
    }

    // --- Helper methods ---

    private World resolveWorld(Map<String, Object> params) {
        Object locObj = params.get("location");
        if (locObj instanceof Location loc && loc.getWorld() != null) {
            return loc.getWorld();
        }
        World defaultWorld = Bukkit.getWorld("world");
        if (defaultWorld == null) {
            LOGGER.warning("Default world 'world' not found");
        }
        return defaultWorld;
    }

    private BlockVector3 getMin(Map<String, Object> params) {
        CubicPosition cubicPosition = new CubicPosition(params);
        return BlockVector3.at(Math.min(cubicPosition.p1x, cubicPosition.p2x), Math.min(cubicPosition.p1y, cubicPosition.p2y), Math.min(cubicPosition.p1z, cubicPosition.p2z));
    }

    private BlockVector3 getMax(Map<String, Object> params) {
        CubicPosition cubicPosition = new CubicPosition(params);
        return BlockVector3.at(Math.max(cubicPosition.p1x, cubicPosition.p2x), Math.max(cubicPosition.p1y, cubicPosition.p2y), Math.max(cubicPosition.p1z, cubicPosition.p2z));
    }

    private static class CubicPosition {
        private final double p1x;
        private final double p1y;
        private final double p1z;
        private final double p2x;
        private final double p2y;
        private final double p2z;

        public CubicPosition(Map<String, Object> params) {
            this.p1x = toDouble(params.getOrDefault("pos1_x", 0));
            this.p1y = toDouble(params.getOrDefault("pos1_y", 64));
            this.p1z = toDouble(params.getOrDefault("pos1_z", 0));
            this.p2x = toDouble(params.getOrDefault("pos2_x", 0));
            this.p2y = toDouble(params.getOrDefault("pos2_y", 64));
            this.p2z = toDouble(params.getOrDefault("pos2_z", 0));
        }

        private double toDouble(Object obj) {
            if (obj instanceof Number n) return n.doubleValue();
            try { return Double.parseDouble(String.valueOf(obj)); } catch (Exception e) { return 0; }
        }
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

    private static List<com.sk89q.worldedit.world.block.BlockType> parseFromPattern(String pattern) {
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
        if (blocks.isEmpty()) return null;
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

    /**
     * Represents a block type with a weight for randomized patterns.
     */
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

