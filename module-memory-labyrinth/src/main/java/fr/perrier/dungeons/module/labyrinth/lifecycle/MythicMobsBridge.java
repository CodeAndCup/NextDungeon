package fr.perrier.dungeons.module.labyrinth.lifecycle;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Reflection bridge to MythicMobs (CDC Q4 = C — hybrid Mythic/MMO ref).
 *
 * <p>MythicMobs is a soft dependency : we never link against its classes
 * at compile time. The bridge resolves API methods lazily and caches
 * them so the per-spawn cost stays bounded. When MythicMobs is absent,
 * {@link #isAvailable()} returns {@code false} and {@link #spawnMob}
 * returns {@link Optional#empty()} — callers fall back to vanilla
 * {@code EntityType.valueOf}.</p>
 */
public final class MythicMobsBridge {

    private static volatile boolean initialised;
    private static volatile boolean available;
    private static Object mobManager;
    private static Method spawnMobMethod;
    private static Method getActiveMobMethod;

    private MythicMobsBridge() {}

    /**
     * Probe MythicMobs once. Safe to call repeatedly — only the first
     * call does the lookup work.
     */
    public static boolean isAvailable() {
        if (!initialised) initialise();
        return available;
    }

    /**
     * Spawn a Mythic mob by id at the given location. Returns
     * {@link Optional#empty()} when MythicMobs is missing or the mob id
     * is unknown — let the caller fall back to a vanilla spawn.
     */
    public static Optional<Entity> spawnMob(String mobId, Location at) {
        if (mobId == null || at == null) return Optional.empty();
        if (!isAvailable()) return Optional.empty();
        try {
            // ActiveMob api = mobManager.spawnMob(mobId, location, level)
            // Some Mythic versions use (String, Location) — try both signatures.
            Object active = invokeSpawn(mobId, at);
            if (active == null) return Optional.empty();
            Object entity = active.getClass().getMethod("getEntity").invoke(active);
            if (entity == null) return Optional.empty();
            // The wrapped BukkitEntity's getBukkitEntity() returns the Bukkit Entity.
            Object bukkit = entity.getClass().getMethod("getBukkitEntity").invoke(entity);
            if (bukkit instanceof Entity e) return Optional.of(e);
            return Optional.empty();
        } catch (Throwable t) {
            // Cache stays valid — only this spawn failed.
            return Optional.empty();
        }
    }

    private static synchronized void initialise() {
        if (initialised) return;
        initialised = true;
        try {
            if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) {
                available = false;
                return;
            }
            // io.lumine.mythic.bukkit.MythicBukkit.inst().getMobManager()
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Object inst = mythicBukkitClass.getMethod("inst").invoke(null);
            mobManager = inst.getClass().getMethod("getMobManager").invoke(inst);

            for (Method m : mobManager.getClass().getMethods()) {
                if (m.getName().equals("spawnMob")
                        && m.getParameterCount() >= 2
                        && m.getParameterTypes()[0] == String.class
                        && Location.class.isAssignableFrom(m.getParameterTypes()[1])) {
                    spawnMobMethod = m;
                    break;
                }
            }
            // Optional helper to look up an ActiveMob from a Bukkit entity.
            for (Method m : mobManager.getClass().getMethods()) {
                if (m.getName().equals("getActiveMob") && m.getParameterCount() == 1) {
                    getActiveMobMethod = m;
                    break;
                }
            }
            available = spawnMobMethod != null;
        } catch (Throwable t) {
            available = false;
        }
    }

    private static Object invokeSpawn(String mobId, Location at) throws ReflectiveOperationException {
        if (spawnMobMethod == null) return null;
        Class<?>[] params = spawnMobMethod.getParameterTypes();
        return switch (params.length) {
            case 2 -> spawnMobMethod.invoke(mobManager, mobId, at);
            case 3 -> spawnMobMethod.invoke(mobManager, mobId, at, 1.0d);
            default -> null;
        };
    }
}
