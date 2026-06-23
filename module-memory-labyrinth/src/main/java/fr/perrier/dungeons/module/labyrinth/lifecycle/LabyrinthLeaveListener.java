package fr.perrier.dungeons.module.labyrinth.lifecycle;

import fr.perrier.dungeons.module.labyrinth.manager.LabyrinthRunManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Set;

/**
 * Intercepts {@code /<alias> memory leave} (e.g. {@code /nd memory leave}) and
 * runs the labyrinth dungeon exit. Chat-preprocess pattern (same as the revive
 * / resume prompts) so the module owns the sub-command without registering it
 * on the core {@code /nd} command tree.
 */
public class LabyrinthLeaveListener implements Listener {

    private static final Set<String> ROOTS = Set.of(
            "nd", "dungeon", "dungeons", "nextdungeon", "nextdungeons");

    private final LabyrinthRunManager runManager;

    public LabyrinthLeaveListener(LabyrinthRunManager runManager) {
        this.runManager = runManager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message == null || message.isEmpty() || message.charAt(0) != '/') return;

        String[] parts = message.substring(1).trim().split("\\s+");
        if (parts.length < 3) return;
        if (!ROOTS.contains(parts[0].toLowerCase())) return;
        if (!parts[1].equalsIgnoreCase("memory") || !parts[2].equalsIgnoreCase("leave")) return;

        event.setCancelled(true);
        runManager.requestLeave(event.getPlayer());
    }
}
