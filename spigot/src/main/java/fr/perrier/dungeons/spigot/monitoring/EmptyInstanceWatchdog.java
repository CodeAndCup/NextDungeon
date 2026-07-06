package fr.perrier.dungeons.spigot.monitoring;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.storage.DungeonService;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/**
 * Shuts a dungeon instance server down once it has been empty (no online
 * players) for a configurable, continuous period — by default 2 minutes.
 * <p>
 * Each dungeon runs on its own cloud service. When everyone leaves, dies out,
 * or never manages to join, the service would otherwise idle forever and keep a
 * stale record in Redis. This watchdog reclaims it: the same terminal action the
 * rest of the codebase uses for an instance — {@link DungeonService#removeInstance(UUID)}
 * followed by {@link Bukkit#shutdown()} (see {@code CancelInstanceSubscriber} and
 * {@code FloorInstance.complete()}). CloudNet then tears the service down.
 * <p>
 * The empty window is continuous: any player online resets the counter, so a
 * brief gap between two groups does not trigger a shutdown. The same window also
 * cleans up an instance that booted but never received its players (e.g. the
 * party disbanded mid-load).
 * <p>
 * The check runs on the main thread because it reads {@link Bukkit#getOnlinePlayers()}
 * and calls {@link Bukkit#shutdown()}, both of which require it. The probe is a
 * single {@code isEmpty()} test once per second, so it is effectively free.
 */
public class EmptyInstanceWatchdog {

    /** Poll interval in ticks (20 ticks = 1 second). */
    private static final long PERIOD_TICKS = 20L;

    /** Config key (seconds) for how long the server may stay empty before shutting down. */
    private static final String TIMEOUT_CONFIG_KEY = "InstanceSettings.emptyShutdownTimeout";

    /** Fallback timeout in seconds when the config key is absent: 2 minutes. */
    private static final int DEFAULT_TIMEOUT_SECONDS = 120;

    /** The plugin instance, used for scheduling and logging. */
    private final Main plugin;

    /** Id of the instance hosted by this server, dropped from Redis on shutdown. */
    private final UUID instanceId;

    /** Number of seconds the server may stay empty before it shuts itself down. */
    private final int timeoutSeconds;

    /** Consecutive seconds the server has had no online players. */
    private int emptySeconds;

    /** The scheduled task handle; null until {@link #start()} runs. */
    private BukkitTask task;

    /** Guards against scheduling the shutdown twice if the task fires again first. */
    private boolean shuttingDown;

    /**
     * @param plugin     the Spigot plugin instance
     * @param instanceId the id of the instance this server hosts
     */
    public EmptyInstanceWatchdog(Main plugin, UUID instanceId) {
        this.plugin = plugin;
        this.instanceId = instanceId;
        this.timeoutSeconds = Math.max(1,
                plugin.getConfig().getInt(TIMEOUT_CONFIG_KEY, DEFAULT_TIMEOUT_SECONDS));
    }

    /**
     * Starts the watchdog. No-op if already started.
     */
    public void start() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, PERIOD_TICKS, PERIOD_TICKS);
        plugin.getLogger().info(String.format(
                "[EmptyInstanceWatchdog] Started — instance %s shuts down after %ds with no players",
                instanceId, timeoutSeconds));
    }

    /**
     * Stops the watchdog and clears its task handle. Subsequent calls have no effect.
     */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /**
     * One probe: reset the empty counter when anyone is online, otherwise count
     * up and trigger shutdown once the configured window has elapsed.
     */
    private void tick() {
        if (shuttingDown) return;

        if (!Bukkit.getOnlinePlayers().isEmpty()) {
            emptySeconds = 0;
            return;
        }

        emptySeconds++;
        if (emptySeconds >= timeoutSeconds) {
            shutdownEmptyInstance();
        }
    }

    /**
     * Drops the instance record from Redis and shuts the server down — the same
     * terminal action used elsewhere for an instance. Guarded so it runs once.
     */
    private void shutdownEmptyInstance() {
        shuttingDown = true;
        stop();
        plugin.getLogger().warning(String.format(
                "[EmptyInstanceWatchdog] Instance %s has been empty for %ds — shutting down",
                instanceId, timeoutSeconds));
        Main.getInstance().getDungeonService().removeInstance(instanceId);
        Bukkit.shutdown();
    }
}
