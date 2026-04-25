package fr.perrier.dungeons.module.labyrinth.lifecycle;

import fr.perrier.dungeons.module.labyrinth.loot.EndOfRunHandler;
import fr.perrier.dungeons.module.labyrinth.manager.LabyrinthRunManager;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthRun;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

/**
 * Records labyrinth player deaths into the run's
 * {@link LabyrinthRun#getDeadPlayers()} set, so the post-boss revive
 * prompt has the correct candidate list.
 *
 * <p>Runs at {@link EventPriority#MONITOR} — after the core
 * {@link fr.perrier.dungeons.spigot.listener.dungeons.InstancePlayerDeathListener}
 * has applied its ghost system.</p>
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
        UUID playerId = player.getUniqueId();

        LabyrinthRun run = runManager.findRunByPlayer(playerId);
        if (run == null) return;
        run.getDeadPlayers().add(playerId);

        // Total wipe = every initial player has died with no live revive.
        // Triggers run end (CDC §1.5 / §6.4) and Infinite save invalidation.
        if (run.getDeadPlayers().size() >= run.getInitialPlayerUuids().size()
                && endOfRunHandler != null) {
            endOfRunHandler.onTotalWipe(run);
        }
    }
}
