package fr.perrier.dungeons.module.labyrinth.blessing;

import fr.perrier.dungeons.module.labyrinth.model.LabyrinthRun;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthSave;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Thin bridge to the external SAO-Blessing plugin. Drives the blessing
 * session lifecycle of a Memory Labyrinth run through that plugin's admin
 * console commands ({@code /saoblessing dungeon ...}).
 *
 * <p>The blessing "dungeon id" is the run's <b>party hash</b> — the very
 * same stable key used to persist Infinite saves
 * ({@link LabyrinthSave#computePartyHash}). A party therefore keeps its
 * blessings tied to the same identity as their checkpoint : resuming an
 * Infinite save resumes the matching blessing session.</p>
 *
 * <p>All dispatches run on the Bukkit main thread. The call sites here are
 * already main-thread, but {@link #dispatch(String)} re-schedules defensively
 * if ever called off-thread.</p>
 *
 * <p><b>Late join handling.</b> When an instance becomes ready the players
 * are not necessarily connected to this floor service yet (CloudNet transfers
 * them with a small delay). Per-player commands that must reach a specific
 * player ({@code start}, {@code offer}) are therefore dispatched <em>as soon
 * as that player is online</em> via {@link #dispatchWhenOnline} — retried
 * every second up to {@link #MAX_ATTEMPTS}, and dropped if the player leaves
 * the instance before connecting.</p>
 */
public final class BlessingBridge {

    private BlessingBridge() {}

    /** Delay between two online-checks while waiting for a player to connect. */
    private static final long RETRY_DELAY_TICKS = 20L; // 1 second

    /** Give up waiting for a player after this many attempts (~60s). */
    private static final int MAX_ATTEMPTS = 60;

    private static Logger log() {
        return Main.getInstance().getLogger();
    }

    /**
     * Stable per-party blessing dungeon id — identical to the Infinite
     * save key so blessings track the same party identity as the save.
     */
    public static String dungeonId(LabyrinthRun run) {
        if (run == null) return "";
        return LabyrinthSave.computePartyHash(run.getInitialPlayerUuids());
    }

    /**
     * Opens a blessing session for every player of the run. Called once per
     * run at start (covers both fresh and resumed runs). Each player's
     * {@code start} is dispatched as soon as they are online, so players
     * still being transferred to the floor service are not missed.
     */
    public static void startSession(LabyrinthRun run, FloorInstance instance) {
        if (run == null || instance == null) return;
        String dungeonId = dungeonId(run);
        // Use the frozen initial party as the authoritative recipient set.
        for (UUID id : new HashSet<>(run.getInitialPlayerUuids())) {
            dispatchWhenOnline(id, instance,
                    p -> "saoblessing dungeon start " + p.getName() + " " + dungeonId, 0);
        }
    }

    /**
     * Closes the blessing session for every recipient. Best-effort : only
     * players still online receive the command (a player who already left
     * has, by definition, abandoned).
     *
     * @param success {@code true} → {@code completed}, {@code false} → {@code abandoned}.
     */
    public static void endSession(Collection<UUID> recipients, boolean success) {
        if (recipients == null) return;
        String state = success ? "completed" : "abandoned";
        forEachOnline(recipients, p ->
                dispatch("saoblessing dungeon end " + p.getName() + " " + state));
    }

    /**
     * Pushes a passive blessing offer to every player of the instance,
     * dispatched per-player as soon as they are online. Called at the entry
     * (lobby) room of a fresh run — finite floors and Infinite "new game";
     * never on Infinite resume (which bypasses the lobby).
     */
    public static void offerPassive(FloorInstance instance) {
        if (instance == null) return;
        for (UUID id : new HashSet<>(instance.getPlayers())) {
            dispatchWhenOnline(id, instance,
                    p -> "saoblessing dungeon offer " + p.getName() + " passive", 0);
        }
    }

    /**
     * Dispatch a per-player command as soon as {@code id} is online, retrying
     * once per second. Stops early if the player leaves the instance (so a
     * disconnect during the wait does not keep firing) or after
     * {@link #MAX_ATTEMPTS} attempts.
     */
    private static void dispatchWhenOnline(UUID id, FloorInstance instance,
                                           Function<Player, String> command, int attempt) {
        Player p = Bukkit.getPlayer(id);
        if (p != null && p.isOnline()) {
            dispatch(command.apply(p));
            return;
        }
        if (attempt >= MAX_ATTEMPTS) {
            log().warning("[MemoryLabyrinth] Blessing command skipped — player " + id
                    + " never came online after " + MAX_ATTEMPTS + " attempts");
            return;
        }
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            // The player left the instance (or it was torn down) — stop retrying.
            if (instance != null && !instance.getPlayers().contains(id)) return;
            dispatchWhenOnline(id, instance, command, attempt + 1);
        }, RETRY_DELAY_TICKS);
    }

    private static void forEachOnline(Collection<UUID> ids, Consumer<Player> action) {
        if (ids == null) return;
        for (UUID id : ids) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) action.accept(p);
        }
    }

    private static void dispatch(String command) {
        Runnable task = () -> {
            try {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
            } catch (Exception e) {
                log().warning("[MemoryLabyrinth] Blessing command failed: /" + command
                        + " — " + e.getMessage());
            }
        };
        if (Bukkit.isPrimaryThread()) task.run();
        else Bukkit.getScheduler().runTask(Main.getInstance(), task);
    }
}
