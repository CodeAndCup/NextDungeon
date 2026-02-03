package fr.perrier.dungeons.spigot.workflow.trigger.handler.impl;

import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.PlayerJumpTrigger;
import fr.perrier.dungeons.spigot.workflow.trigger.handler.TriggerEventHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;

/**
 * Handler pour les événements de saut de joueur
 */
public class PlayerJumpTriggerHandler implements TriggerEventHandler<PlayerMoveEvent> {

    private final Set<UUID> jumpingPlayers = new HashSet<>();

    @Override
    public Class<PlayerMoveEvent> getEventType() {
        return PlayerMoveEvent.class;
    }

    @Override
    public List<String> getSupportedTriggerTypes() {
        return Collections.singletonList("player_jump_trigger");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @Override
    public void handleEvent(PlayerMoveEvent event, List<Trigger> triggers) {
        Player player = event.getPlayer();

        // Détecter un saut : vélocité Y positive et joueur était au sol
        if (event.getFrom().getY() < event.getTo().getY() &&
            player.isOnGround() == false &&
            player.getVelocity().getY() > 0) {

            UUID playerId = player.getUniqueId();

            // Éviter les détections multiples pour un seul saut
            if (jumpingPlayers.contains(playerId)) {
                return;
            }

            jumpingPlayers.add(playerId);

            // Retirer après un court délai
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                fr.perrier.dungeons.spigot.Main.getInstance(),
                () -> jumpingPlayers.remove(playerId),
                5L
            );

            for (Trigger trigger : triggers) {
                if (trigger instanceof PlayerJumpTrigger jumpTrigger) {
                    // Créer les données de l'événement
                    Map<String, Object> data = extractEventData(event);

                    // Exécuter le trigger
                    jumpTrigger.execute(player, player.getLocation(), data);
                }
            }
        }
    }

    @Override
    public Map<String, Object> extractEventData(PlayerMoveEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("player_name", event.getPlayer().getName());
        data.put("from_location", event.getFrom());
        data.put("to_location", event.getTo());
        data.put("velocity_y", event.getPlayer().getVelocity().getY());

        return data;
    }

    @Override
    public Player getPlayerFromEvent(PlayerMoveEvent event) {
        return event.getPlayer();
    }
}
