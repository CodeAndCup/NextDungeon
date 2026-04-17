package fr.perrier.dungeons.spigot.monitoring;

import fr.perrier.dungeons.common.model.dungeon.FloorData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.database.DatabaseManager;
import fr.perrier.dungeons.spigot.storage.DungeonService;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Periodic health check for the 3-tier cache (local → Redis → database).
 * <p>
 * Runs asynchronously every 60 seconds, 30 seconds after the server finishes
 * loading. Each tick performs four cheap probes:
 * <ul>
 *   <li>Redis connectivity (a single {@code getKeys().count()} with a guard)</li>
 *   <li>Database connectivity (a dummy floor fetch)</li>
 *   <li>Version consistency between Redis floorsMap and the current floor</li>
 *   <li>Cache integrity — checksum verification on a <b>sample</b> of 50 floors
 *       max, never all of them (avoids freezes on large catalogs)</li>
 * </ul>
 */
public class CacheHealthMonitor {

    private static final long START_DELAY_TICKS = 600L;   // 30s after load
    private static final long PERIOD_TICKS = 1200L;       // every 60s
    private static final int INTEGRITY_SAMPLE_SIZE = 50;

    private final Main plugin;
    private BukkitTask task;

    public CacheHealthMonitor(Main plugin) {
        this.plugin = plugin;
    }

    public void startMonitoring() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin, this::checkCacheHealth, START_DELAY_TICKS, PERIOD_TICKS);
        plugin.getLogger().info("[CacheHealthMonitor] Started (delay=" + START_DELAY_TICKS
                + "t, period=" + PERIOD_TICKS + "t)");
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
            plugin.getLogger().info("[CacheHealthMonitor] Stopped");
        }
    }

    private void checkCacheHealth() {
        try {
            checkRedisConnectivity();
            checkDatabaseConnectivity();
            checkVersionConsistency();
            checkCacheIntegrity();
        } catch (Exception e) {
            plugin.getLogger().warning("[CacheHealthMonitor] Tick failed: " + e.getMessage());
        }
    }

    private void checkRedisConnectivity() {
        DungeonService ds = plugin.getDungeonService();
        RedissonClient client = ds != null ? ds.getRedissonClient() : null;
        if (client == null) {
            plugin.getLogger().warning("[CacheHealthMonitor] Redisson client missing");
            return;
        }
        try {
            client.getKeys().count();
        } catch (Exception e) {
            plugin.getLogger().severe("[CacheHealthMonitor] Redis ping FAILED: " + e.getMessage());
        }
    }

    private void checkDatabaseConnectivity() {
        DatabaseManager db = plugin.getDatabaseManager();
        if (db == null) {
            plugin.getLogger().warning("[CacheHealthMonitor] DatabaseManager missing");
            return;
        }
        try {
            // A missing floorId returns null without raising — cheap liveness probe.
            db.getFloor("__healthcheck__").get();
        } catch (Exception e) {
            plugin.getLogger().severe("[CacheHealthMonitor] DB ping FAILED: " + e.getMessage());
        }
    }

    private void checkVersionConsistency() {
        DungeonService ds = plugin.getDungeonService();
        if (ds == null || ds.getFloorsMap() == null) return;
        try {
            // Only check the current floor — cross-server version comparison
            // is the sync topic's job; here we just catch the local drift.
            RMap<String, FloorData> floorsMap = ds.getFloorsMap();
            if (!ds.hasActiveInstance()) return;
            // No direct getter for currentFloor from outside; rely on floorsMap self-consistency.
            // If the map is non-empty but metadata is empty, that is a strong drift signal.
            if (!floorsMap.isEmpty() && ds.getFloorMetadataMap() != null
                    && ds.getFloorMetadataMap().isEmpty()) {
                plugin.getLogger().warning("[CacheHealthMonitor] floorsMap populated but floor_metadata empty");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[CacheHealthMonitor] Version check failed: " + e.getMessage());
        }
    }

    /**
     * Sampled integrity check. We iterate the Redis map via {@link RMap#entrySet()}
     * (never {@code readAllMap()} — that materialises everything at once) and bail
     * out after {@value #INTEGRITY_SAMPLE_SIZE} entries.
     * <p>
     * Three outcomes are logged distinctly so operators can triage:
     * <ul>
     *   <li>{@code MISSING} — no checksum stored. Legacy floor predating the
     *       versioning migration; benign and auto-healed on next write.</li>
     *   <li>{@code MISMATCH} — checksum stored but differs from recomputed value.
     *       Either real corruption or a versioning bug. The expected and actual
     *       values are logged for diagnosis.</li>
     *   <li>OK — nothing to report.</li>
     * </ul>
     */
    private void checkCacheIntegrity() {
        DungeonService ds = plugin.getDungeonService();
        if (ds == null || ds.getFloorsMap() == null) return;
        try {
            IntegrityReport report = sampleIntegrity(ds.getFloorsMap());
            reportIntegrity(report);
        } catch (Exception e) {
            plugin.getLogger().warning("[CacheHealthMonitor] Integrity sampling failed: " + e.getMessage());
        }
    }

    private IntegrityReport sampleIntegrity(RMap<String, FloorData> floorsMap) {
        IntegrityReport report = new IntegrityReport();
        Iterator<Map.Entry<String, FloorData>> it = floorsMap.entrySet().iterator();
        while (it.hasNext() && report.checked < INTEGRITY_SAMPLE_SIZE) {
            Map.Entry<String, FloorData> entry = it.next();
            classifyEntry(entry, report);
        }
        return report;
    }

    private void classifyEntry(Map.Entry<String, FloorData> entry, IntegrityReport report) {
        FloorData fd = entry.getValue();
        if (fd == null) return;
        report.checked++;
        String stored = fd.getChecksum();
        if (stored == null || stored.isEmpty()) {
            report.missing.add(entry.getKey());
            return;
        }
        String recomputed = fd.calculateChecksum();
        if (!stored.equals(recomputed)) {
            report.mismatched.add(entry.getKey());
            int triggerCount = fd.getTriggers() == null ? -1 : fd.getTriggers().size();
            plugin.getLogger().severe("[CacheHealthMonitor] MISMATCH floor=" + entry.getKey()
                    + " v=" + fd.getVersion()
                    + " stored=" + shortHash(stored)
                    + " actual=" + shortHash(recomputed)
                    + " triggers=" + (triggerCount < 0 ? "null" : triggerCount + " item(s)"));
        }
    }

    private void reportIntegrity(IntegrityReport report) {
        if (!report.mismatched.isEmpty()) {
            plugin.getLogger().severe("[CacheHealthMonitor] " + report.mismatched.size()
                    + "/" + report.checked + " sampled floor(s) have bad checksums: " + report.mismatched);
        }
        if (!report.missing.isEmpty()) {
            plugin.getLogger().warning("[CacheHealthMonitor] " + report.missing.size()
                    + "/" + report.checked + " floor(s) have NO checksum (legacy, will self-heal on next dashboard save): "
                    + report.missing);
        }
        if (report.mismatched.isEmpty() && report.missing.isEmpty() && report.checked > 0) {
            plugin.getLogger().info("[CacheHealthMonitor] Sample OK (" + report.checked + " floor(s) verified)");
        }
    }

    private static String shortHash(String full) {
        if (full == null) return "null";
        return full.length() <= 12 ? full : full.substring(0, 12) + "...";
    }

    private static final class IntegrityReport {
        int checked;
        final List<String> missing = new ArrayList<>();
        final List<String> mismatched = new ArrayList<>();
    }
}
