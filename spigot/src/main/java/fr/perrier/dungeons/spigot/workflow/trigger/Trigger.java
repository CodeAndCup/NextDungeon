package fr.perrier.dungeons.spigot.workflow.trigger;

import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.workflow.action.ActionSequenceExecutor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Classe abstraite pour tous les triggers de donjon
 */
@Getter
public abstract class Trigger implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    protected UUID triggerId;
    @Setter
    protected String name;
    @Setter
    protected boolean enabled;
    @Setter
    protected Map<String, Object> properties;
    @Setter
    protected List<Action> actions;

    public Trigger(String name) {
        this.triggerId = UUID.randomUUID();
        this.name = name;
        this.enabled = true;
        this.actions = new ArrayList<>();
    }

    /**
     * Exécute le trigger
     * @param player Le joueur concerné
     * @param location La location si applicable
     * @param data Données additionnelles
     * @return true si le trigger s'est exécuté avec succès
     */
    public abstract boolean execute(Player player, Location location, Map<String, Object> data);

    /**
     * Vérifie si les conditions du trigger sont remplies
     * @param player Le joueur concerné
     * @param data Données additionnelles
     * @return true si les conditions sont remplies
     */
    public abstract boolean checkConditions(Player player, Map<String, Object> data);

    /**
     * Retourne le type du trigger pour l'éditeur web
     */
    public abstract String getType();

    /**
     * Exécute toutes les actions du trigger
     */
    protected boolean executeActions(Player player, Location location, Map<String, Object> data) {
        if (actions == null || actions.isEmpty()) {
            return true;
        }
        data.put("trigger_name", this.name);
        // Nouvelle exécution asynchrone
        new ActionSequenceExecutor(actions, player, location, data).executeNext();
        return true;
    }
}