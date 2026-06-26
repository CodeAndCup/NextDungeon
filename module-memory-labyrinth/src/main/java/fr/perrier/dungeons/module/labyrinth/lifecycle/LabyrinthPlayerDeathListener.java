package fr.perrier.dungeons.module.labyrinth.lifecycle;

import fr.perrier.dungeons.module.labyrinth.loot.EndOfRunHandler;
import fr.perrier.dungeons.module.labyrinth.manager.LabyrinthRunManager;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthRun;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Records labyrinth player deaths into the run's
 * {@link LabyrinthRun#getDeadPlayers()} set, so the post-boss revive
 * prompt has the correct candidate list, and drives total-wipe detection.
 *
 * <p>Runs at {@link EventPriority#MONITOR} — after the core
 * {@link fr.perrier.dungeons.spigot.listener.dungeons.InstancePlayerDeathListener}
 * has applied its ghost system.</p>
 *
 * <p>A mid-run disconnect is also treated as a death for wipe tracking
 * (see {@link #onPlayerQuit}): an offline player leaves no living body and
 * cannot be revived, so without this a quit (rather than a death) would
 * keep {@code deadPlayers.size()} permanently below
 * {@code initialPlayerUuids.size()} and a subsequent party death would
 * never reach the total-wipe threshold — stranding the remaining players
 * as ghosts while the instance server never shuts down.</p>
 */
public class LabyrinthPlayerDeathListener implements Listener {

    private final LabyrinthRunManager runManager;
    private EndOfRunHandler endOfRunHandler;

    public LabyrinthPlayerDeathListener(LabyrinthRunManager runManager) {
        this.runManager = runManager;
    }

    public void setEndOfRunHandler(EndOfRunHandler handler) {
        this.endOfRunHandler = handler;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player == null) return;

        registerDownAndCheckWipe(player.getUniqueId());
    }

    /**
     * A mid-run disconnect counts as a death for wipe tracking — see the
     * class javadoc for the stranded-ghost / never-shutdown failure mode
     * this prevents. Voluntary exits ({@code /nd memory leave}) end the run
     * through {@code EndOfRunHandler.onVoluntaryExit} instead and are not
     * routed here.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        registerDownAndCheckWipe(player.getUniqueId());
    }

    /**
     * Marks the player as down in their run and ends the run on a total
     * wipe = every initial player has died (or disconnected) with no live
     * revive. Triggers run end (CDC §1.5 / §6.4) and Infinite save
     * invalidation. Idempotent: {@code deadPlayers} is a set, and once the
     * run is ended {@code findRunByPlayer} no longer resolves it.
     */
    private void registerDownAndCheckWipe(UUID playerId) {
        LabyrinthRun run = runManager.findRunByPlayer(playerId);
        if (run == null) return;
        run.getDeadPlayers().add(playerId);

        if (run.getDeadPlayers().size() >= run.getInitialPlayerUuids().size()
                && endOfRunHandler != null) {
            endOfRunHandler.onTotalWipe(run);
        }
    }
}
