package fr.perrier.dungeons.workflow.trigger.handler;

import fr.perrier.dungeons.workflow.trigger.Trigger;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.Map;

/**
 * Interface générique pour tous les gestionnaires d'événements de triggers
 */
public interface TriggerEventHandler<T extends Event> extends Listener {

    /**
     * Retourne le type d'événement Bukkit que ce handler gère
     */
    Class<T> getEventType();

    /**
     * Retourne les types de triggers que ce handler peut traiter
     */
    List<String> getSupportedTriggerTypes();

    /**
     * Traite un événement et vérifie les triggers associés
     * @param event L'événement Bukkit
     * @param triggers Liste des triggers à vérifier
     */
    void handleEvent(T event, List<Trigger> triggers);

    /**
     * Extrait les données de l'événement pour les passer aux triggers
     * @param event L'événement Bukkit
     * @return Map des données extraites
     */
    Map<String, Object> extractEventData(T event);

    /**
     * Obtient le joueur concerné par l'événement (si applicable)
     * @param event L'événement Bukkit
     * @return Le joueur ou null
     */
    Player getPlayerFromEvent(T event);
}
