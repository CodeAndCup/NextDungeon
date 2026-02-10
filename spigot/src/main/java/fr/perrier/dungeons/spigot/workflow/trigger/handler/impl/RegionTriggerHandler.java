package fr.perrier.dungeons.spigot.workflow.trigger.handler.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.RegionTrigger;
import fr.perrier.dungeons.spigot.workflow.trigger.handler.TriggerEventHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;

public class RegionTriggerHandler implements TriggerEventHandler<PlayerMoveEvent> {

    // Cache des joueurs dans les régions pour éviter les spam
    private final Map<UUID, Set<UUID>> playersInRegions = new HashMap<>();

    @Override
    public Class<PlayerMoveEvent> getEventType() {
        return PlayerMoveEvent.class;
    }

    @Override
    public List<String> getSupportedTriggerTypes() {
        return Arrays.asList("region_trigger");
    }

    @Override
    public void handleEvent(PlayerMoveEvent event, List<Trigger> triggers) {
        Player player = event.getPlayer();

        for (Trigger trigger : triggers) {
            if (trigger instanceof RegionTrigger regionTrigger) {
                checkRegionTrigger(player, regionTrigger, event);
            }
        }
    }

    private void checkRegionTrigger(Player player, RegionTrigger regionTrigger, PlayerMoveEvent event) {
        if(Main.isDebug()) {
            Main.getInstance().getLogger().info("Checking region trigger: " + regionTrigger.getTriggerId() + " for player: " + player.getName());
        }

        boolean wasInRegion = isPlayerInRegionCache(player.getUniqueId(), regionTrigger.getTriggerId());
        boolean isInRegion = isPlayerInRegion(player, regionTrigger);

        // Le joueur entre dans la région
        if (!wasInRegion && isInRegion) {
            addPlayerToRegionCache(player.getUniqueId(), regionTrigger.getTriggerId());

            Map<String, Object> data = extractEventData(event);
            data.put("region_event", "enter");

            if (regionTrigger.checkConditions(player, data)) {
                regionTrigger.execute(player, event.getTo(), data);
            }
        }
        // Le joueur sort de la région
        else if (wasInRegion && !isInRegion) {
            removePlayerFromRegionCache(player.getUniqueId(), regionTrigger.getTriggerId());

            Map<String, Object> data = extractEventData(event);
            data.put("region_event", "exit");

            if (regionTrigger.checkConditions(player, data)) {
                regionTrigger.execute(player, event.getFrom(), data);
            }
        }
    }

    private boolean isPlayerInRegion(Player player, RegionTrigger regionTrigger) {
        return regionTrigger.isPlayerInRegion(player);
    }

    @Override
    public Map<String, Object> extractEventData(PlayerMoveEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("event_type", "player_move");
        data.put("from_location", event.getFrom());
        data.put("to_location", event.getTo());
        data.put("player", event.getPlayer());
        return data;
    }

    @Override
    public Player getPlayerFromEvent(PlayerMoveEvent event) {
        return event.getPlayer();
    }

    private boolean isPlayerInRegionCache(UUID playerId, UUID regionId) {
        return playersInRegions.getOrDefault(playerId, Collections.emptySet()).contains(regionId);
    }

    private void addPlayerToRegionCache(UUID playerId, UUID regionId) {
        playersInRegions.computeIfAbsent(playerId, k -> new HashSet<>()).add(regionId);
    }

    private void removePlayerFromRegionCache(UUID playerId, UUID regionId) {
        Set<UUID> regions = playersInRegions.get(playerId);
        if (regions != null) {
            regions.remove(regionId);
            if (regions.isEmpty()) {
                playersInRegions.remove(playerId);
            }
        }
    }
}