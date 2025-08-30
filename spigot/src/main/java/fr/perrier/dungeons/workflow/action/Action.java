package fr.perrier.dungeons.workflow.action;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.io.Serializable;
import java.util.Map;

/**
 * Action exécutable par un trigger
 */
@Getter
public abstract class Action implements Serializable {
    private static final long serialVersionUID = 1L;

    @Setter
    protected String name;
    protected String type;

    public Action(String name, String type) {
        this.name = name;
        this.type = type;
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
