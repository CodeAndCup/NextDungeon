package fr.perrier.dungeons.spigot.workflow.trigger.handler.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.ItemPickupTrigger;
import fr.perrier.dungeons.spigot.workflow.trigger.handler.TriggerEventHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;

import java.util.*;

/**
 * Handler pour les événements de ramassage d'items
 */
public class ItemPickupTriggerHandler implements TriggerEventHandler<EntityPickupItemEvent> {

    @Override
    public Class<EntityPickupItemEvent> getEventType() {
        return EntityPickupItemEvent.class;
    }

    @Override
    public List<String> getSupportedTriggerTypes() {
        return Collections.singletonList("item_pickup_trigger");
    }

    @Override
    public void handleEvent(EntityPickupItemEvent event, List<Trigger> triggers) {
        // Vérifier que c'est un joueur qui ramasse l'item
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        for (Trigger trigger : triggers) {
            if (trigger instanceof ItemPickupTrigger pickupTrigger) {
                // Vérifier si l'item correspond
                if (!pickupTrigger.matchesItem(event.getItem().getItemStack())) {
                    continue;
                }

                // Créer les données de l'événement
                Map<String, Object> data = extractEventData(event);

                // Exécuter le trigger
                if (pickupTrigger.execute(player, player.getLocation(), data)) {
                    // Annuler le ramassage si demandé
                    if (pickupTrigger.shouldCancelPickup()) {
                        event.setCancelled(true);
                        if (Main.isDebug()) {
                            Main.getInstance().getLogger().info("Item pickup cancelled by ItemPickupTrigger: " + pickupTrigger.getName());
                        }
                    }
                }
            }
        }
    }

    @Override
    public Map<String, Object> extractEventData(EntityPickupItemEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("item_type", event.getItem().getItemStack().getType().name());
        data.put("item_amount", event.getItem().getItemStack().getAmount());
        data.put("item_location", event.getItem().getLocation());

        if (event.getEntity() instanceof Player player) {
            data.put("player_name", player.getName());
            data.put("player_location", player.getLocation());
        }

        return data;
    }

    @Override
    public Player getPlayerFromEvent(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            return player;
        }
        return null;
    }
}
