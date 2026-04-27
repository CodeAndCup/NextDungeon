package fr.perrier.dungeons.module.labyrinth.lifecycle;

import fr.perrier.dungeons.module.labyrinth.manager.LabyrinthRunManager;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthRun;
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
            sender.sendMessage("§cUsage : /" + COMMAND + " " + ResumeOrNewPrompt.CHOICE_RESUME
                    + "|" + ResumeOrNewPrompt.CHOICE_NEW);
            return;
        }
        String choice = rest.split("\\s+", 2)[0].toLowerCase();

        LabyrinthRun run = runManager.findRunByPlayer(sender.getUniqueId());
        if (run == null) {
            sender.sendMessage("§cTu n'es pas dans une instance de Memory Labyrinth.");
            return;
        }
        if (!run.isLobbyDecisionPending()) {
            sender.sendMessage("§cIl n'y a pas de save à reprendre ici.");
            return;
        }

        switch (choice) {
            case ResumeOrNewPrompt.CHOICE_RESUME ->
                    runManager.applyResume(run, sender);
            case ResumeOrNewPrompt.CHOICE_NEW ->
                    runManager.discardSaveAndContinue(run, sender);
            default ->
                    sender.sendMessage("§cChoix invalide : " + choice);
        }
    }
}
