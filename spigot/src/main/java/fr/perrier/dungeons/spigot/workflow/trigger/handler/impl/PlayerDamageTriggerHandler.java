package fr.perrier.dungeons.spigot.workflow.trigger.handler.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.PlayerDamageTrigger;
import fr.perrier.dungeons.spigot.workflow.trigger.handler.TriggerEventHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.*;

/**
 * Handler pour les événements de dégâts sur joueur
 */
public class PlayerDamageTriggerHandler implements TriggerEventHandler<EntityDamageEvent> {

    @Override
    public Class<EntityDamageEvent> getEventType() {
        return EntityDamageEvent.class;
    }

    @Override
    public List<String> getSupportedTriggerTypes() {
        return Collections.singletonList("player_damage_trigger");
    }

    @Override
    public void handleEvent(EntityDamageEvent event, List<Trigger> triggers) {
        // Vérifier que c'est un joueur qui prend des dégâts
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        for (Trigger trigger : triggers) {
            if (trigger instanceof PlayerDamageTrigger damageTrigger) {
                // Vérifier si le type de dégâts correspond
                if (!damageTrigger.matchesDamageType(event.getCause())) {
                    continue;
                }

                // Créer les données de l'événement
                Map<String, Object> data = extractEventData(event);

                // Exécuter le trigger
                if (damageTrigger.execute(player, player.getLocation(), data)) {
                    // Annuler les dégâts si demandé
                    if (damageTrigger.shouldCancelDamage()) {
                        event.setCancelled(true);
                        if (Main.getLoggerUtil().isDebugEnabled()) {
                            Main.getLoggerUtil().info("Damage cancelled by PlayerDamageTrigger: " + damageTrigger.getName());
                        }
                    }
                }
            }
        }
    }

    @Override
    public Map<String, Object> extractEventData(EntityDamageEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("damage_cause", event.getCause().name());
        data.put("damage_amount", event.getDamage());
        data.put("final_damage", event.getFinalDamage());

        if (event.getEntity() instanceof Player player) {
            data.put("player_name", player.getName());
            data.put("player_health", player.getHealth());
            data.put("player_location", player.getLocation());
        }

        return data;
    }

    @Override
    public Player getPlayerFromEvent(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            return player;
        }
        return null;
    }
}
