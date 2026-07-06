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
 * This monitor runs asynchronously and executes four lightweight probes on each tick:
 * <ul>
 *   <li><strong>Redis connectivity:</strong> Verifies Redis is accessible via a single
 *       {@code getKeys().count()} call with error handling.</li>
 *   <li><strong>Database connectivity:</strong> Tests database liveness with a dummy floor fetch
 *       using a reserved test floor ID.</li>
 *   <li><strong>Version consistency:</strong> Detects drift between the Redis floorsMap and
 *       floor metadata, ensuring local cache coherency.</li>
 *   <li><strong>Cache integrity:</strong> Performs sampled checksum verification on up to 50 floors
 *       to detect data corruption without blocking on large catalogs.</li>
 * </ul>
 * <p>
 * <strong>Thread Safety:</strong> All checks execute asynchronously on the Bukkit async task pool.
 * Individual probes are designed to be non-blocking and resilient to transient failures.
 * <p>
 * <strong>Startup Sequence:</strong> Monitoring begins 30 seconds after server load completion
 * (to allow all subsystems to stabilize), then repeats every 60 seconds.
 *
 * @see DungeonService
 * @see DatabaseManager
 * @see FloorData
 */
public class CacheHealthMonitor {

    /** Startup delay in ticks (20 ticks = 1 second); 600 ticks = 30 seconds after load. */
    private static final long START_DELAY_TICKS = 600L;

    /** Monitoring interval in ticks; 1200 ticks = 60 seconds. */
    private static final long PERIOD_TICKS = 1200L;

    /** Maximum number of floors to sample in integrity checks to avoid performance degradation. */
    private static final int INTEGRITY_SAMPLE_SIZE = 50;

    /** Reference to the Spigot plugin instance. */
    private final Main plugin;

    /** The scheduled async task handle; null if monitoring is stopped. */
    private BukkitTask task;

    /**
     * Constructs a new cache health monitor.
     *
     * @param plugin the Spigot plugin instance that manages scheduler and logging
     */
    public CacheHealthMonitor(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts the periodic health monitoring task.
     * <p>
     * If monitoring is already active, this method is a no-op. The first check
     * runs 30 seconds after invocation, and subsequent checks repeat every 60 seconds.
     * <p>
     * Logs the start event with configured delays to the plugin logger at INFO level.
     */
    public void startMonitoring() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin, this::checkCacheHealth, START_DELAY_TICKS, PERIOD_TICKS);
        plugin.getLogger().info("[CacheHealthMonitor] Started (delay=" + START_DELAY_TICKS
                                + "t, period=" + PERIOD_TICKS + "t)");
    }

    /**
     * Stops the periodic health monitoring task.
     * <p>
     * Cancels the scheduled task and clears the task reference. Subsequent calls
     * have no effect. Logs the stop event at INFO level.
     */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
            plugin.getLogger().info("[CacheHealthMonitor] Stopped");
        }
    }

    /**
     * Executes a complete health check cycle.
     * <p>
     * Invokes all four probe methods in sequence. If any probe throws an exception,
     * it is caught and logged as a WARNING; execution continues with remaining probes.
     * This ensures a single probe failure does not suppress other diagnostics.
     */
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

    /**
     * Probes Redis connectivity.
     * <p>
     * Attempts a lightweight {@code getKeys().count()} call to verify the Redisson client
     * can reach Redis. If the client is unavailable or the probe fails, a SEVERE log entry
     * is recorded with the error message.
     */
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

    /**
     * Probes database connectivity.
     * <p>
     * Attempts a dummy floor fetch using a reserved health-check floor ID
     * ({@code __healthcheck__}). A missing floor returns null without exception,
     * serving as a cheap liveness check. Actual query failures are logged as SEVERE.
     */
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

    /**
     * Checks for version/metadata consistency between Redis floorsMap and floor metadata.
     * <p>
     * Detects local drift by verifying that if the floorsMap is populated,
     * the corresponding metadata map is not empty. This is a simple sanity check
     * to catch divergence between related cache layers. Cross-server version
     * comparisons are handled by the sync subsystem.
     * <p>
     * Failures are logged as WARNING to avoid alert fatigue on transient inconsistencies.
     */
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
     * Performs a sampled integrity check on the Redis floorsMap.
     * <p>
     * Initiates a checksum verification on up to {@value #INTEGRITY_SAMPLE_SIZE} floors
     * selected from the Redis map via iterator (avoiding full materialization).
     * Results are accumulated in an {@link IntegrityReport} and logged with categorization:
     * <ul>
     *   <li><strong>MISSING:</strong> No checksum stored (legacy floors from pre-versioning era).
     *       Benign and auto-healed on next dashboard save.</li>
     *   <li><strong>MISMATCH:</strong> Checksum exists but differs from recomputed value.
     *       May indicate real corruption or a versioning bug. Stored and actual values are
     *       logged for operator diagnosis.</li>
     *   <li><strong>OK:</strong> All sampled floors pass checksum verification.</li>
     * </ul>
     * <p>
     * Sampling prevents performance degradation on catalogs with thousands of floors.
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

    /**
     * Samples the Redis floorsMap and collects integrity data.
     * <p>
     * Iterates the floorsMap via {@link RMap#entrySet()} without materializing
     * the entire map, stopping after {@value #INTEGRITY_SAMPLE_SIZE} entries
     * are examined. Each entry is classified by its checksum status
     * (missing, mismatched, or valid).
     *
     * @param floorsMap the Redis-backed floor data map
     * @return an {@link IntegrityReport} containing counts and lists of floors by status
     */
    private IntegrityReport sampleIntegrity(RMap<String, FloorData> floorsMap) {
        IntegrityReport report = new IntegrityReport();
        Iterator<Map.Entry<String, FloorData>> it = floorsMap.entrySet().iterator();
        while (it.hasNext() && report.checked < INTEGRITY_SAMPLE_SIZE) {
            Map.Entry<String, FloorData> entry = it.next();
            classifyEntry(entry, report);
        }
        return report;
    }

    /**
     * Classifies a single floor entry by its checksum validity.
     * <p>
     * Compares the stored checksum in {@link FloorData#getChecksum()} against
     * a freshly computed value from {@link FloorData#calculateChecksum()}.
     * Results are categorized as missing or mismatched, or valid (no action).
     * <p>
     * Mismatches are logged immediately at SEVERE level with the floor key,
     * version, both checksum values (abbreviated), and trigger count for forensic analysis.
     *
     * @param entry a map entry containing a floor ID and its FloorData
     * @param report the accumulating {@link IntegrityReport} to update
     */
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

    /**
     * Logs the collected integrity report with categorized outcomes.
     * <p>
     * Mismatches are logged as SEVERE (operational concern), missing checksums
     * as WARNING (informational), and a fully clean sample as INFO.
     * This granular logging allows operators to triage cache health issues.
     *
     * @param report the {@link IntegrityReport} containing check results
     */
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
        /*if (report.mismatched.isEmpty() && report.missing.isEmpty() && report.checked > 0) {
            plugin.getLogger().info("[CacheHealthMonitor] Sample OK (" + report.checked + " floor(s) verified)");
        }*/
    }

    /**
     * Abbreviates a hash string for compact logging.
     * <p>
     * Returns the full string if 12 characters or fewer, otherwise returns
     * the first 12 characters followed by an ellipsis.
     *
     * @param full the hash string to abbreviate; may be null
     * @return an abbreviated representation, or "null" if input is null
     */
    private static String shortHash(String full) {
        if (full == null) return "null";
        return full.length() <= 12 ? full : full.substring(0, 12) + "...";
    }

    /**
     * Accumulates integrity check results across a sample of floors.
     * <p>
     * Stores the count of floors examined and separate lists for floors
     * with missing or mismatched checksums. Used to generate summary reports
     * that categorize cache health outcomes.
     */
    private static final class IntegrityReport {
        /** Number of floors examined in the current sample. */
        int checked;

        /** Floors with no stored checksum (legacy floors). */
        final List<String> missing = new ArrayList<>();

        /** Floors with checksum mismatches (potential corruption). */
        final List<String> mismatched = new ArrayList<>();
    }
}
