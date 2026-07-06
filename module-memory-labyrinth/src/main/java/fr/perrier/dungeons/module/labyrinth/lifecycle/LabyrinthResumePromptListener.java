package fr.perrier.dungeons.module.labyrinth.lifecycle;

import fr.perrier.dungeons.module.labyrinth.manager.LabyrinthRunManager;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthRun;
import fr.perrier.dungeons.module.labyrinth.ui.LabyrinthMessages;
import static fr.perrier.dungeons.module.labyrinth.ui.LabyrinthMessages.RED;
import static fr.perrier.dungeons.module.labyrinth.ui.LabyrinthMessages.WHITE;
import static fr.perrier.dungeons.module.labyrinth.ui.LabyrinthMessages.DARK;
import fr.perrier.dungeons.module.labyrinth.ui.ResumeOrNewPrompt;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Intercepts {@code /labyrinth_resume <choice>} commands fired by
 * {@link ResumeOrNewPrompt}. Chat-preprocess pattern, same rationale as
 * the revive prompt — avoids hot-registering a Bukkit command from a
 * URLClassLoader-loaded module.
 */
public class LabyrinthResumePromptListener implements Listener {

    public static final String COMMAND = "labyrinth_resume";

    private final LabyrinthRunManager runManager;

    public LabyrinthResumePromptListener(LabyrinthRunManager runManager) {
        this.runManager = runManager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message == null) return;
        String prefix = "/" + COMMAND;
        if (!message.startsWith(prefix)) return;

        event.setCancelled(true);
        Player sender = event.getPlayer();
        String rest = message.substring(prefix.length()).trim();
        if (rest.isEmpty()) {
            LabyrinthMessages.send(sender, RED + "Usage " + DARK + ": " + WHITE + "/" + COMMAND + " "
                    + DARK + "<" + WHITE + "saveId" + DARK + "|" + WHITE + ResumeOrNewPrompt.CHOICE_NEW + DARK + ">");
            return;
        }
        String choice = rest.split("\\s+", 2)[0];

        LabyrinthRun run = runManager.findRunByPlayer(sender.getUniqueId());
        if (run == null) {
            LabyrinthMessages.send(sender, RED + "You are not in a Memory Labyrinth instance" + DARK + ".");
            return;
        }
        if (!run.isLobbyDecisionPending()) {
            LabyrinthMessages.send(sender, RED + "There is no save to resume here" + DARK + ".");
            return;
        }

        if (ResumeOrNewPrompt.CHOICE_NEW.equalsIgnoreCase(choice)) {
            runManager.newRun(sender);
        } else {
            runManager.resumeChosen(sender, choice);
        }
    }
}
