package fr.perrier.dungeons.module.cinematic.executor;

import fr.perrier.dungeons.module.cinematic.action.CinematicAction;
import fr.perrier.dungeons.module.cinematic.clock.CinematicClock;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Orchestrates a cinematic: coordinates the clock, actions, and player state.
 * <p>
 * Architecture mirrors Typewriter's CameraCinematicAction:
 * <ul>
 *   <li>Player setup: save state → invisibility → hide players → damage event cancellation</li>
 *   <li>Tick: delegate to all actions in parallel</li>
 *   <li>Teardown: unregister events → restore state</li>
 * </ul>
 *
 * @see <a href="https://github.com/gabber235/Typewriter">Typewriter CameraCinematicEntry.kt</a>
 */
public class CinematicExecutor {

    private final List<CinematicAction> actions;
    private final Player player;
    private final CinematicClock clock;
    private PlayerCinematicState stateSnapshot;
    private Consumer<Integer> frameListener;
    private Listener damageListener;
    private boolean isRunning = false;

    public CinematicExecutor(List<CinematicAction> actions, Player player, CinematicClock clock) {
        this.actions = actions;
        this.player = player;
        this.clock = clock;
    }

    /**
     * Starts the cinematic: captures player state, applies cinematic setup, registers events.
     * <p>
     * Mirrors Typewriter CameraCinematicAction.setup() flow:
     * <ol>
     *   <li>Capture player state (location, flight, visibility, effects)</li>
     *   <li>Set invisible + flying</li>
     *   <li>Hide players mutually</li>
     *   <li>Register damage event cancellation listeners</li>
     *   <li>Setup all cinematic actions</li>
     *   <li>Subscribe to clock frame changes</li>
     * </ol>
     */
    public void start() {
        if (isRunning) return;
        isRunning = true;

        // 1. Capture player state (ref: Typewriter originalState = state(...))
        stateSnapshot = new PlayerCinematicState();
        stateSnapshot.captureState(player);

        // 2. Invisibility (ref: Typewriter CameraCinematicEntry.kt:215)
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.INVISIBILITY,
                Integer.MAX_VALUE,
                0,
                false,
                false
        ));

        // 3. Allow flight + flying (ref: Typewriter setup: allowFlight = true; isFlying = true)
        player.setAllowFlight(true);
        player.setFlying(true);

        // 4. Hide players mutually (ref: Typewriter CameraCinematicEntry.kt:216-218)
        Plugin plugin = Bukkit.getPluginManager().getPlugin("NextDungeon");
        if (plugin != null) {
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (!other.getUniqueId().equals(player.getUniqueId())) {
                    other.hidePlayer(plugin, player);
                    player.hidePlayer(plugin, other);
                }
            }
        }

        // 5. Register damage/target event cancellation (ref: Typewriter CameraCinematicEntry.kt:226-239)
        registerDamageListeners(plugin);

        // 6. Setup all cinematic actions
        for (CinematicAction action : actions) {
            try {
                action.onCinematicSetup(player, clock);
            } catch (Exception e) {
                System.err.println("[Cinematic] Setup action error: " + e.getMessage());
            }
        }

        // 7. Subscribe to clock frame changes
        frameListener = this::tickFrame;
        clock.addFrameChangeListener(frameListener);
    }

    /**
     * Ticks all actions in parallel for the given frame.
     */
    private void tickFrame(int frame) {
        if (!isRunning) return;

        try {
            List<CompletableFuture<Void>> futures = actions.stream()
                    .map(action -> CompletableFuture.runAsync(() -> {
                        try {
                            action.onCinematicTick(player, frame);
                        } catch (Exception e) {
                            System.err.println("[Cinematic] Tick action error: " + e.getMessage());
                        }
                    }))
                    .collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            System.err.println("[Cinematic] Tick error: " + e.getMessage());
        }

        if (canFinish(frame)) {
            stop();
        }
    }

    /**
     * Stops the cinematic: stops all actions, unregisters events, restores player state.
     * <p>
     * Mirrors Typewriter CameraCinematicAction.teardown() flow:
     * <ol>
     *   <li>Unsubscribe from clock</li>
     *   <li>Stop all cinematic actions</li>
     *   <li>Unregister damage listeners</li>
     *   <li>Remove invisibility</li>
     *   <li>Restore player state (location, flight, visibility, effects)</li>
     * </ol>
     */
    public void stop() {
        if (!isRunning) return;
        isRunning = false;

        // 1. Unsubscribe from clock
        if (frameListener != null) {
            clock.removeFrameChangeListener(frameListener);
        }

        try {
            // 2. Stop all cinematic actions
            for (CinematicAction action : actions) {
                try {
                    action.onCinematicStop(player);
                } catch (Exception e) {
                    System.err.println("[Cinematic] Stop action error: " + e.getMessage());
                }
            }

            // 3. Unregister damage listeners
            unregisterDamageListeners();

            // 4. Remove invisibility
            player.removePotionEffect(PotionEffectType.INVISIBILITY);

            // 5. Restore player state (ref: Typewriter teardown → restore(originalState))
            if (stateSnapshot != null) {
                stateSnapshot.restoreState(player);
            }
        } catch (Exception e) {
            System.err.println("[Cinematic] Stop error: " + e.getMessage());
        }
    }

    /**
     * Registers Bukkit event listeners to cancel damage and targeting during cinematics.
     * Exact replica of Typewriter's event listeners in CameraCinematicEntry.kt:226-239.
     */
    private void registerDamageListeners(Plugin plugin) {
        if (plugin == null) return;

        damageListener = new Listener() {
            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onDamage(EntityDamageEvent event) {
                if (event.getEntity().getUniqueId().equals(player.getUniqueId())) {
                    event.setCancelled(true);
                }
            }

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onDamageByEntity(EntityDamageByEntityEvent event) {
                if (event.getEntity().getUniqueId().equals(player.getUniqueId())) {
                    event.setCancelled(true);
                }
            }

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onTarget(EntityTargetEvent event) {
                if (player.equals(event.getTarget())) {
                    event.setCancelled(true);
                }
            }

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onTargetLiving(EntityTargetLivingEntityEvent event) {
                if (player.equals(event.getTarget())) {
                    event.setCancelled(true);
                }
            }
        };

        Bukkit.getPluginManager().registerEvents(damageListener, plugin);
    }

    private void unregisterDamageListeners() {
        if (damageListener != null) {
            HandlerList.unregisterAll(damageListener);
            damageListener = null;
        }
    }

    private boolean canFinish(int frame) {
        for (CinematicAction action : actions) {
            if (!action.canCinematicFinish(frame)) {
                return false;
            }
        }
        return true;
    }

    public boolean isRunning() {
        return isRunning;
    }
}
