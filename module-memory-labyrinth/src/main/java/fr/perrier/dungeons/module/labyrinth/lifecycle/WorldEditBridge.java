package fr.perrier.dungeons.module.labyrinth.lifecycle;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Direct WorldEdit-plugin bridge for the v2 procedural room paste.
 *
 * <p>Calls the WorldEdit API straight (no reflection, no {@code module-worldedit}
 * hop). The WorldEdit classes are {@code provided} at compile time and resolve at
 * runtime through the NextDungeon plugin classloader — modules parent to it, and
 * it sees the WorldEdit plugin like any other soft dependency. If WorldEdit is not
 * installed the single entry point degrades to a logged warning and returns
 * {@code false}, so the caller falls back to the template region.</p>
 *
 * <p>The paste pre-loads every source and destination chunk before running the
 * WorldEdit batch and retries once on a transient failure : the destination is a
 * sliding +X offset far from the template build, so its chunks are usually
 * ungenerated and a batch that touches a not-yet-ready chunk is the intermittent
 * "paste FAILED" symptom.</p>
 */
public final class WorldEditBridge {

    private static final Logger LOGGER = Logger.getLogger("MemoryLabyrinth.WE");

    /**
     * Hard cap on the block volume of a single paste. The copy iterates the
     * region on the main thread, so anything beyond a few million blocks would
     * stall the server.
     */
    private static final long MAX_REGION_VOLUME = 15_000_000L;

    /** Cached "is WorldEdit available" probe (null = not yet checked). */
    private volatile Boolean available;

    /**
     * Copies a cuboid from a source world to a destination world. Returns
     * {@code false} when WorldEdit is not loaded or the operation fails after a
     * retry (with a logged reason so the admin can diagnose).
     */
    public boolean copyRegion(String srcWorld, int srcMinX, int srcMinY, int srcMinZ,
                              int srcMaxX, int srcMaxY, int srcMaxZ,
                              String dstWorld, int dstX, int dstY, int dstZ) {
        if (!isWorldEditPresent()) {
            LOGGER.warning("[MemoryLabyrinth] WorldEdit plugin not available — procedural paste disabled");
            return false;
        }
        try {
            return doCopy(srcWorld, srcMinX, srcMinY, srcMinZ, srcMaxX, srcMaxY, srcMaxZ,
                    dstWorld, dstX, dstY, dstZ);
        } catch (Throwable t) {
            // Throwable (not just Exception) so a NoClassDefFoundError from a
            // missing/incompatible WorldEdit never bubbles into the run loop.
            LOGGER.log(Level.SEVERE, "[MemoryLabyrinth] copyRegion crashed", t);
            return false;
        }
    }

    private boolean doCopy(String srcWorld, int srcMinX, int srcMinY, int srcMinZ,
                           int srcMaxX, int srcMaxY, int srcMaxZ,
                           String dstWorld, int dstX, int dstY, int dstZ) {
        World srcBukkit = Bukkit.getWorld(srcWorld);
        World dstBukkit = Bukkit.getWorld(dstWorld);
        if (srcBukkit == null) {
            LOGGER.warning("[MemoryLabyrinth] copyRegion: source world not loaded: " + srcWorld);
            return false;
        }
        if (dstBukkit == null) {
            LOGGER.warning("[MemoryLabyrinth] copyRegion: destination world not loaded: " + dstWorld);
            return false;
        }

        BlockVector3 min = BlockVector3.at(
                Math.min(srcMinX, srcMaxX), Math.min(srcMinY, srcMaxY), Math.min(srcMinZ, srcMaxZ));
        BlockVector3 max = BlockVector3.at(
                Math.max(srcMinX, srcMaxX), Math.max(srcMinY, srcMaxY), Math.max(srcMinZ, srcMaxZ));

        long dx = (long) (max.x() - min.x()) + 1;
        long dy = (long) (max.y() - min.y()) + 1;
        long dz = (long) (max.z() - min.z()) + 1;
        long volume = dx * dy * dz;
        if (volume > MAX_REGION_VOLUME) {
            LOGGER.warning("[MemoryLabyrinth] copyRegion refused: volume " + volume
                    + " > cap " + MAX_REGION_VOLUME + " (dims " + dx + "x" + dy + "x" + dz + ")");
            return false;
        }

        BlockVector3 dstAnchor = BlockVector3.at(dstX, dstY, dstZ);
        BlockVector3 dstMax = dstAnchor.add(max.subtract(min));

        // Force-load every chunk the copy will touch BEFORE the batch runs.
        ensureChunksLoaded(srcBukkit, min.x(), min.z(), max.x(), max.z());
        ensureChunksLoaded(dstBukkit, dstAnchor.x(), dstAnchor.z(), dstMax.x(), dstMax.z());

        com.sk89q.worldedit.world.World srcWeWorld = BukkitAdapter.adapt(srcBukkit);
        com.sk89q.worldedit.world.World dstWeWorld = BukkitAdapter.adapt(dstBukkit);
        CuboidRegion region = new CuboidRegion(srcWeWorld, min, max);

        // A single retry absorbs transient failures (a chunk still finishing an
        // async load/generation on the first pass). Re-pasting to the same anchor
        // overwrites, so the retry is idempotent and safe.
        Throwable lastError = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
            // Origin = min so paste-to(dstAnchor) lines up srcMin with dstAnchor.
            clipboard.setOrigin(min);
            try (EditSession readSession = WorldEdit.getInstance().newEditSession(srcWeWorld)) {
                ForwardExtentCopy copy = new ForwardExtentCopy(readSession, region, clipboard, min);
                Operations.complete(copy);
            } catch (Throwable e) {
                lastError = e;
                LOGGER.log(Level.WARNING, "[MemoryLabyrinth] copyRegion read failed (attempt "
                        + attempt + "/2)", e);
                continue;
            }
            try (EditSession writeSession = WorldEdit.getInstance().newEditSession(dstWeWorld)) {
                ClipboardHolder holder = new ClipboardHolder(clipboard);
                Operation paste = holder.createPaste(writeSession).to(dstAnchor).ignoreAirBlocks(false).build();
                Operations.complete(paste);
            } catch (Throwable e) {
                lastError = e;
                LOGGER.log(Level.WARNING, "[MemoryLabyrinth] copyRegion paste failed (attempt "
                        + attempt + "/2)", e);
                continue;
            }
            LOGGER.info("[MemoryLabyrinth] copyRegion " + srcWorld + min + "→" + max
                    + " to " + dstWorld + dstAnchor + " OK" + (attempt > 1 ? " (after retry)" : ""));
            return true;
        }
        LOGGER.log(Level.SEVERE, "[MemoryLabyrinth] copyRegion " + srcWorld + min + "→" + max
                + " to " + dstWorld + dstAnchor + " FAILED after 2 attempts", lastError);
        return false;
    }

    /**
     * Synchronously loads (generating if necessary) every chunk overlapping the
     * X/Z span so the WorldEdit batch never trips over an unloaded chunk. Block
     * coordinates ; Y is irrelevant for chunk addressing. Any order accepted.
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

    private boolean isWorldEditPresent() {
        Boolean cached = available;
        if (cached != null) return cached;
        boolean present = Bukkit.getPluginManager().getPlugin("WorldEdit") != null
                || Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null;
        available = present;
        return present;
    }
}
