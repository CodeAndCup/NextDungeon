package fr.perrier.dungeons.module.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
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

    /**
     * Hard cap on the number of blocks a single cuboid operation may
     * touch. The set/cut/replace/copyRegion paths iterate the region on
     * the main thread, so anything beyond a few million blocks will hang
     * the server. 8M = 200×200×200, which is well above the largest
     * realistic labyrinth room while still safe to chew through synchronously.
     * If a use case ever needs more, expose this as a config knob.
     */
    private static final long MAX_REGION_VOLUME = 8_000_000L;

    public WorldEditManager() {
        LOGGER.info("WorldEditManager initialized");
    }

    /**
     * @return {@code true} if the region [min,max] exceeds the per-op
     *         volume cap. Logs a warning naming {@code opName} so the
     *         workflow author can find the offending block.
     */
    private static boolean rejectIfTooLarge(BlockVector3 min, BlockVector3 max, String opName) {
        long dx = (long) Math.abs(max.x() - min.x()) + 1;
        long dy = (long) Math.abs(max.y() - min.y()) + 1;
        long dz = (long) Math.abs(max.z() - min.z()) + 1;
        long volume = dx * dy * dz;
        if (volume > MAX_REGION_VOLUME) {
            LOGGER.warning("[WorldEdit] " + opName + " refused: region volume "
                    + volume + " > cap " + MAX_REGION_VOLUME
                    + " (dims " + dx + "x" + dy + "x" + dz + ")");
            return true;
        }
        return false;
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
            if (rejectIfTooLarge(min, max, "set")) return false;
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
            if (rejectIfTooLarge(min, max, "cut")) return false;
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
            if (rejectIfTooLarge(min, max, "replace")) return false;
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

    /**
     * Copies a cuboid region from one world to another using an in-memory
     * clipboard (no schematic file needed).
     *
     * <p>The destination anchor is the position where {@code srcMin} will
     * land in the destination world. Block at {@code srcMin + (dx, dy, dz)}
     * ends up at {@code dstAnchor + (dx, dy, dz)}, preserving relative
     * geometry.</p>
     *
     * @param srcWorldId  the Bukkit world name containing the source region
     * @param srcMin      one corner of the cuboid (will be normalised internally)
     * @param srcMax      the opposite corner of the cuboid
     * @param dstWorldId  the destination Bukkit world name
     * @param dstAnchor   the position where {@code srcMin} should land
     * @return {@code true} on success, {@code false} on any I/O or WorldEdit failure
     */
    public boolean copyRegion(String srcWorldId, BlockVector3 srcMin, BlockVector3 srcMax,
                              String dstWorldId, BlockVector3 dstAnchor) {
        if (srcWorldId == null || dstWorldId == null || srcMin == null || srcMax == null || dstAnchor == null) {
            LOGGER.warning("[WorldEdit] copyRegion called with null argument");
            return false;
        }
        World srcBukkit = Bukkit.getWorld(srcWorldId);
        World dstBukkit = Bukkit.getWorld(dstWorldId);
        if (srcBukkit == null) {
            LOGGER.warning("[WorldEdit] copyRegion: source world not loaded: " + srcWorldId);
            return false;
        }
        if (dstBukkit == null) {
            LOGGER.warning("[WorldEdit] copyRegion: destination world not loaded: " + dstWorldId);
            return false;
        }
        BlockVector3 min = BlockVector3.at(
                Math.min(srcMin.x(), srcMax.x()),
                Math.min(srcMin.y(), srcMax.y()),
                Math.min(srcMin.z(), srcMax.z()));
        BlockVector3 max = BlockVector3.at(
                Math.max(srcMin.x(), srcMax.x()),
                Math.max(srcMin.y(), srcMax.y()),
                Math.max(srcMin.z(), srcMax.z()));
        if (rejectIfTooLarge(min, max, "copyRegion")) return false;

        // Force-load every chunk the copy will touch BEFORE the WorldEdit
        // operations run. The destination is a sliding +X offset far from the
        // template build (baseAnchorX defaults to 5000), so those chunks are
        // usually ungenerated ; if the batched paste touches a chunk that is
        // not yet ready it fails intermittently (the "paste FAILED" symptom).
        // Pre-loading makes generation deterministic and synchronous here.
        BlockVector3 dstMax = dstAnchor.add(max.subtract(min));
        ensureChunksLoaded(srcBukkit, min.x(), min.z(), max.x(), max.z());
        ensureChunksLoaded(dstBukkit, dstAnchor.x(), dstAnchor.z(), dstMax.x(), dstMax.z());

        com.sk89q.worldedit.world.World srcWeWorld = BukkitAdapter.adapt(srcBukkit);
        com.sk89q.worldedit.world.World dstWeWorld = BukkitAdapter.adapt(dstBukkit);
        CuboidRegion region = new CuboidRegion(srcWeWorld, min, max);

        // A single retry absorbs transient failures (a chunk that was still
        // finishing async load/generation on the first pass). Re-pasting to the
        // same anchor is idempotent — it overwrites — so a retry is safe.
        Exception lastError = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
            // Origin = min so paste-to(dstAnchor) lines up srcMin with dstAnchor.
            clipboard.setOrigin(min);
            try (EditSession readSession = com.sk89q.worldedit.WorldEdit.getInstance().newEditSession(srcWeWorld)) {
                ForwardExtentCopy copy = new ForwardExtentCopy(readSession, region, clipboard, min);
                Operations.complete(copy);
            } catch (Exception e) {
                lastError = e;
                LOGGER.log(Level.WARNING, "[WorldEdit] copyRegion read failed (attempt "
                        + attempt + "/2)", e);
                continue;
            }
            try (EditSession writeSession = com.sk89q.worldedit.WorldEdit.getInstance().newEditSession(dstWeWorld)) {
                ClipboardHolder holder = new ClipboardHolder(clipboard);
                Operation paste = holder.createPaste(writeSession).to(dstAnchor).ignoreAirBlocks(false).build();
                Operations.complete(paste);
            } catch (Exception e) {
                lastError = e;
                LOGGER.log(Level.WARNING, "[WorldEdit] copyRegion paste failed (attempt "
                        + attempt + "/2)", e);
                continue;
            }
            LOGGER.info("[WorldEdit] copyRegion " + srcWorldId + min + "→" + max
                    + " to " + dstWorldId + dstAnchor + " OK"
                    + (attempt > 1 ? " (after retry)" : ""));
            return true;
        }
        LOGGER.log(Level.SEVERE, "[WorldEdit] copyRegion " + srcWorldId + min + "→" + max
                + " to " + dstWorldId + dstAnchor + " FAILED after 2 attempts", lastError);
        return false;
    }

    /**
     * Synchronously loads (generating if necessary) every chunk overlapping the
     * X/Z span so a subsequent WorldEdit batch never trips over an unloaded
     * chunk. Coordinates are block coordinates ; Y is irrelevant for chunk
     * addressing. Safe to call with min/max in any order.
     */
    private static void ensureChunksLoaded(World world, int x1, int z1, int x2, int z2) {
        int minCx = Math.min(x1, x2) >> 4;
        int maxCx = Math.max(x1, x2) >> 4;
        int minCz = Math.min(z1, z2) >> 4;
        int maxCz = Math.max(z1, z2) >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (!world.isChunkLoaded(cx, cz)) {
                    world.getChunkAt(cx, cz); // loads + generates synchronously
                }
            }
        }
    }

    /**
     * Map-based wrapper around {@link #copyRegion(String, BlockVector3, BlockVector3, String, BlockVector3)}
     * so the operation can be invoked from a Blockly workflow.
     *
     * <p>Expected params: {@code srcWorld}, {@code pos1_x/y/z}, {@code pos2_x/y/z},
     * {@code dstWorld}, {@code dst_x/y/z}.</p>
     */
    public boolean handleCopyRegion(Map<String, Object> params) {
        String srcWorld = String.valueOf(params.getOrDefault("srcWorld", "world"));
        String dstWorld = String.valueOf(params.getOrDefault("dstWorld", "world"));
        BlockVector3 srcMin = BlockVector3.at(toInt(params.getOrDefault("pos1_x", 0)),
                toInt(params.getOrDefault("pos1_y", 64)), toInt(params.getOrDefault("pos1_z", 0)));
        BlockVector3 srcMax = BlockVector3.at(toInt(params.getOrDefault("pos2_x", 0)),
                toInt(params.getOrDefault("pos2_y", 64)), toInt(params.getOrDefault("pos2_z", 0)));
        BlockVector3 dst = BlockVector3.at(toInt(params.getOrDefault("dst_x", 0)),
                toInt(params.getOrDefault("dst_y", 64)), toInt(params.getOrDefault("dst_z", 0)));
        return copyRegion(srcWorld, srcMin, srcMax, dstWorld, dst);
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

