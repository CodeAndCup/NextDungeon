package fr.perrier.dungeons.spigot.workflow.trigger.handler.impl;

import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.BlockClickTrigger;
import fr.perrier.dungeons.spigot.workflow.trigger.handler.TriggerEventHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;

/**
 * Handler pour les événements de clic sur blocs
 */
public class BlockClickTriggerHandler implements TriggerEventHandler<PlayerInteractEvent> {

    @Override
    public Class<PlayerInteractEvent> getEventType() {
        return PlayerInteractEvent.class;
    }

    @Override
    public List<String> getSupportedTriggerTypes() {
        return Collections.singletonList("block_click_trigger");
    }

    @EventHandler(priority = EventPriority.HIGH)
    @Override
    public void handleEvent(PlayerInteractEvent event, List<Trigger> triggers) {
        Player player = event.getPlayer();

        // Ignorer si pas de bloc cliqué
        if (event.getClickedBlock() == null) {
            return;
        }

        // Déterminer le type de clic
        String clickType;
        String detectionType;

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            clickType = "left_click";
            detectionType = "block";
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            clickType = "right_click";
            detectionType = "block";
        } else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_AIR) {
            // Pour les interactions en l'air, on peut utiliser l'item en main
            detectionType = "interaction";
            clickType = (event.getAction() == Action.LEFT_CLICK_AIR) ? "left_click" : "right_click";
        } else {
            return; // Action non gérée
        }

        for (Trigger trigger : triggers) {
            if (trigger instanceof BlockClickTrigger blockClickTrigger) {
                if (blockClickTrigger.matchesBlock(event.getClickedBlock(), clickType, detectionType)) {
                    Map<String, Object> data = extractEventData(event);
                    data.put("click_type", clickType);
                    data.put("detection_type", detectionType);
                    data.put("clicked_block", event.getClickedBlock());

                    if (blockClickTrigger.checkConditions(player, data)) {
                        blockClickTrigger.execute(player, event.getClickedBlock().getLocation(), data);
                    }
                }
            }
        }
    }

    @Override
    public Map<String, Object> extractEventData(PlayerInteractEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("event_type", "player_interact");
        data.put("action", event.getAction().name());
        data.put("player", event.getPlayer());

        if (event.getClickedBlock() != null) {
            data.put("block_location", event.getClickedBlock().getLocation());
            data.put("block_type", event.getClickedBlock().getType().name());
        }

        if (event.getItem() != null) {
            data.put("item_in_hand", event.getItem());
            data.put("item_type", event.getItem().getType().name());
        }

        if (event.getBlockFace() != null) {
            data.put("block_face", event.getBlockFace().name());
        }

        return data;
    }

    @Override
    public Player getPlayerFromEvent(PlayerInteractEvent event) {
        return event.getPlayer();
    }
}

