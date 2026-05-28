package fr.perrier.dungeons.module.labyrinth.lifecycle;

import fr.perrier.dungeons.common.module.NextDungeonModule;
import fr.perrier.dungeons.spigot.Main;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Reflection bridge to the {@code module-worldedit} {@code WorldEditManager}.
 *
 * <p>Both modules live in their own classloader (see {@code ModuleLoader})
 * so a compile-time import of {@code WorldEditManager} would not resolve.
 * This bridge looks up the worldedit module by id at first use, caches
 * the reflected handles, and gracefully degrades to a logged warning
 * when the worldedit module is not available.</p>
 *
 * <p>The single operation exposed is {@code copyRegion} — the v2
 * procedural paste backbone.</p>
 */
public final class WorldEditBridge {

    private static final Logger LOGGER = Logger.getLogger("MemoryLabyrinth.WE");

    private volatile boolean resolved;
    private volatile Object weManager;          // WorldEditManager instance
    private volatile Method copyRegion;         // copyRegion(String, BV3, BV3, String, BV3) → boolean
    private volatile Method blockVector3At;     // BlockVector3.at(int, int, int) → BV3

    /**
     * Copies a cuboid from a source world to a destination world. Returns
     * {@code false} when worldedit is not loaded or any reflection step
     * fails (with a logged warning so the admin can diagnose).
     */
    public boolean copyRegion(String srcWorld, int srcMinX, int srcMinY, int srcMinZ,
                              int srcMaxX, int srcMaxY, int srcMaxZ,
                              String dstWorld, int dstX, int dstY, int dstZ) {
        if (!ensureResolved()) return false;
        try {
            Object srcMin = blockVector3At.invoke(null, srcMinX, srcMinY, srcMinZ);
            Object srcMax = blockVector3At.invoke(null, srcMaxX, srcMaxY, srcMaxZ);
            Object dstAnchor = blockVector3At.invoke(null, dstX, dstY, dstZ);
            Object result = copyRegion.invoke(weManager, srcWorld, srcMin, srcMax, dstWorld, dstAnchor);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            LOGGER.warning("[MemoryLabyrinth] copyRegion reflective call failed: " + e.getMessage());
            return false;
        }
    }

    private synchronized boolean ensureResolved() {
        if (resolved) return weManager != null;
        resolved = true;
        try {
            NextDungeonModule we = Main.getInstance().getModuleLoader()
                    .getLoadedModules().get("worldedit");
            if (we == null) {
                LOGGER.warning("[MemoryLabyrinth] worldedit module not loaded — procedural paste disabled");
                return false;
            }
            Method getManager = we.getClass().getMethod("getManager");
            this.weManager = getManager.invoke(we);
            if (weManager == null) {
                LOGGER.warning("[MemoryLabyrinth] worldedit module returned a null manager");
                return false;
            }
            Class<?> bv3 = Class.forName("com.sk89q.worldedit.math.BlockVector3");
            this.blockVector3At = bv3.getMethod("at", int.class, int.class, int.class);
            this.copyRegion = weManager.getClass().getMethod("copyRegion",
                    String.class, bv3, bv3, String.class, bv3);
            return true;
        } catch (Exception e) {
            LOGGER.warning("[MemoryLabyrinth] cannot resolve WorldEditManager: " + e.getMessage());
            this.weManager = null;
            return false;
        }
    }
}
