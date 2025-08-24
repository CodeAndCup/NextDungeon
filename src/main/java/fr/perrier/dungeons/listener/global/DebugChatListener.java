package fr.perrier.dungeons.listener.global;

import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.model.Dungeon;
import fr.perrier.dungeons.model.Floor;
import fr.perrier.dungeons.model.FloorInstance;
import fr.perrier.dungeons.trigger.impl.DebugTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Listener pour les événements de chat debug
 */
public class DebugChatListener implements Listener {

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        FloorInstance instance = Main.getInstance().getRedisStorageService().getCurrentInstance().get();

        // Vérifier si le joueur est dans un donjon
        Dungeon dungeon = Dungeon.getDungeon(instance.getFloorId().split("_")[0]);
        if (dungeon == null) return;

        Floor currentFloor = instance.getFloor();
        if (currentFloor == null) return;

        // Préparer les données pour les triggers
        Map<String, Object> triggerData = new HashMap<>();
        triggerData.put("message", event.getMessage());
        triggerData.put("event_type", "chat");
        triggerData.put("player", player.getName());
        triggerData.put("timestamp", System.currentTimeMillis());

        if(currentFloor.getTriggers().isEmpty()) return;

        // Vérifier tous les triggers debug du floor
        currentFloor.getTriggers().stream()
                .filter(trigger -> trigger instanceof DebugTrigger)
                .map(trigger -> (DebugTrigger) trigger)
                .forEach(debugTrigger -> {
                    if (debugTrigger.checkChatEvent(event)) {
                        // Exécuter le trigger
                        debugTrigger.execute(player, player.getLocation(), triggerData);

                        // Optionnel: annuler le message de chat si c'est un message de debug
                        if (debugTrigger.getTriggerMessage().equals(event.getMessage()) &&
                                player.hasPermission("dungeons.debug")) {
                            event.setCancelled(true);
                            player.sendMessage("§8[§eDebug§8] §7Message intercepté par le trigger");
                        }
                    }
                });
    }
}
