package fr.perrier.dungeons.spigot.workflow.trigger.handler.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.ChatMessageTrigger;
import fr.perrier.dungeons.spigot.workflow.trigger.handler.TriggerEventHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;

/**
 * Handler pour les événements de message dans le chat
 */
public class ChatMessageTriggerHandler implements TriggerEventHandler<AsyncPlayerChatEvent> {

    @Override
    public Class<AsyncPlayerChatEvent> getEventType() {
        return AsyncPlayerChatEvent.class;
    }

    @Override
    public List<String> getSupportedTriggerTypes() {
        return Collections.singletonList("chat_message_trigger");
    }

    @EventHandler(priority = EventPriority.HIGH)
    @Override
    public void handleEvent(AsyncPlayerChatEvent event, List<Trigger> triggers) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        for (Trigger trigger : triggers) {
            if (trigger instanceof ChatMessageTrigger chatTrigger) {
                // Vérifier si le message correspond
                if (!chatTrigger.matchesMessage(message)) {
                    continue;
                }

                // Créer les données de l'événement
                Map<String, Object> data = extractEventData(event);

                // Exécuter le trigger de manière synchrone
                Main.getInstance().getServer().getScheduler().runTask(Main.getInstance(), () -> {
                    chatTrigger.execute(player, player.getLocation(), data);
                });

                // Annuler le message si demandé
                if (chatTrigger.shouldCancelMessage()) {
                    event.setCancelled(true);
                    if (Main.isDebug()) {
                        Main.getInstance().getLogger().info("Chat message cancelled by ChatMessageTrigger: " + chatTrigger.getName());
                    }
                    break; // Arrêter après le premier match qui annule
                }
            }
        }
    }

    @Override
    public Map<String, Object> extractEventData(AsyncPlayerChatEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", event.getMessage());
        data.put("player_name", event.getPlayer().getName());
        data.put("player_location", event.getPlayer().getLocation());
        data.put("format", event.getFormat());

        return data;
    }

    @Override
    public Player getPlayerFromEvent(AsyncPlayerChatEvent event) {
        return event.getPlayer();
    }
}
