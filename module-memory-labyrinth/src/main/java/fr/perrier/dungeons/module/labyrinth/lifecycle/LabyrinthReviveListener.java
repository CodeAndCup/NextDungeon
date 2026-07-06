package fr.perrier.dungeons.module.labyrinth.lifecycle;

import fr.perrier.dungeons.module.labyrinth.ui.LabyrinthMessages;
import static fr.perrier.dungeons.module.labyrinth.ui.LabyrinthMessages.RED;
import static fr.perrier.dungeons.module.labyrinth.ui.LabyrinthMessages.WHITE;
import static fr.perrier.dungeons.module.labyrinth.ui.LabyrinthMessages.DARK;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Intercepts {@code /labyrinth_revive <playerName>} chat commands fired by
 * the clickable {@link fr.perrier.dungeons.module.labyrinth.ui.RevivePromptComponent}.
 *
 * <p>Implemented as a chat-preprocess listener instead of a registered
 * Bukkit command to avoid fiddling with the server command map at runtime
 * (the module is hot-loaded via URLClassLoader).</p>
 */
public class LabyrinthReviveListener implements Listener {

    private final BossEncounterHandler bossEncounterHandler;

    public LabyrinthReviveListener(BossEncounterHandler bossEncounterHandler) {
        this.bossEncounterHandler = bossEncounterHandler;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message == null) return;
        String prefix = "/" + BossEncounterHandler.REVIVE_COMMAND;
        if (!message.startsWith(prefix)) return;

        event.setCancelled(true);
        Player sender = event.getPlayer();
        String rest = message.substring(prefix.length()).trim();
        if (rest.isEmpty()) {
            LabyrinthMessages.send(sender, RED + "Usage " + DARK + ": " + WHITE + "/" + BossEncounterHandler.REVIVE_COMMAND
                    + " " + DARK + "<" + WHITE + "player" + DARK + ">");
            return;
        }
        // Split on whitespace and take the first token — guards against
        // accidental extra arguments injected via chat hover/click.
        String targetName = rest.split("\\s+", 2)[0];
        bossEncounterHandler.processReviveClick(sender, targetName);
    }
}
