package fr.perrier.dungeons.spigot.workflow.action;

import fr.perrier.dungeons.common.workflow.action.ActionData;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.util.Map;

/**
 * Action exécutable par un trigger
 */
@Getter
public abstract class Action extends ActionData {

    protected Action(String name, String type) {
        super(name, type);
    }

    /**
     * Exécute l'action
     * @param player Le joueur qui a déclenché le trigger
     * @param location La location du déclenchement
     * @param data Données additionnelles
     * @return true si l'action s'est exécutée avec succès
     */
    public abstract boolean execute(Player player, Location location, Map<String, Object> data);
}
