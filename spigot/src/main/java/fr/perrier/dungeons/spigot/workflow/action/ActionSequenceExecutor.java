package fr.perrier.dungeons.spigot.workflow.action;

import fr.perrier.dungeons.common.workflow.action.ActionData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.workflow.action.impl.DelayAction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * Exécuteur séquentiel asynchrone pour les actions d'un trigger
 */
public class ActionSequenceExecutor {
    private final List<ActionData> actions;
    private final Player player;
    private final Location location;
    private final Map<String, Object> data;
    private int index = 0;

    public ActionSequenceExecutor(List<ActionData> actions, Player player, Location location, Map<String, Object> data) {
        this.actions = actions;
        this.player = player;
        this.location = location;
        this.data = data;
    }

    /**
     * Démarre l'exécution de la séquence d'actions
     */
    public void executeNext() {
        if (index >= actions.size()) {
            return;
        }
        ActionData actionData = actions.get(index);
        index++;
        try {
            if (actionData instanceof DelayAction delayAction) {
                int ticks = delayAction.getTicks();
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), this::executeNext, Math.max(1, ticks));
            } else if(actionData instanceof Action action) {
                action.execute(player, location, data);
                executeNext();
            } else {
                Main.getLoggerUtil().warning("Action inconnue de type: " + actionData.getClass().getName());
                executeNext();
            }
        } catch (Exception e) {
            Main.getLoggerUtil().severe("Error executing action " + actionData.getClass().getSimpleName() + ": " + e.getMessage());
            // Continue quand même la séquence
            executeNext();
        }
    }
}

