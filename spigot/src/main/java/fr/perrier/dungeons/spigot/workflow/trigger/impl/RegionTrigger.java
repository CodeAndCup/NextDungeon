package fr.perrier.dungeons.spigot.workflow.trigger.impl;

import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyTrigger;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.utils.ServerUtil;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Trigger qui se déclenche quand un joueur entre/sort d'une région définie par deux positions
 * Mis à jour pour fonctionner avec le système global de triggers
 */
@Getter
@Setter
@BlocklyInfo(
        name = "region_trigger",
        color = "#4CAF50",
        displayText = "📍 Quand le joueur entre/sort de région",
        tooltip = "Déclenche quand un joueur entre ou sort d'une région définie",
        category = "Triggers"
)
public class RegionTrigger extends Trigger implements BlocklyTrigger {

    @BlocklyField(type = BlocklyField.FieldType.LOCATION_INPUT, label = "Position 1:", defaultValue = "", order = 0)
    private Location pos1;

    @BlocklyField(type = BlocklyField.FieldType.LOCATION_INPUT, label = "Position 2:", defaultValue = "", order = 1)
    private Location pos2;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Monde:", defaultValue = "world", order = 2)
    private String worldName;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Événement:", order = 3, options = "enter,exit,both")
    private String regionEvent = "enter";

    @BlocklyField(type = BlocklyField.FieldType.CHECKBOX, label = "Une seule fois:", order = 4)
    private boolean onlyOnce = false;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Cooldown (sec):", defaultValue = "0", order = 5)
    private int cooldownSeconds = 0;

    private final Map<String, Long> playerTriggerHistory = new HashMap<>();

    public RegionTrigger(String name) {
        super(name);
        this.worldName = "world";
    }

    @Override
    public boolean execute(Player player, Location location, Map<String, Object> data) {
        if(ServerUtil.isInEditMode()) return false;

        if (!checkConditions(player, data)) {
            return false;
        }

        if (cooldownSeconds > 0) {
            String playerId = player.getUniqueId().toString();
            long currentTime = System.currentTimeMillis();
            Long lastTrigger = playerTriggerHistory.get(playerId);

            if (lastTrigger != null && (currentTime - lastTrigger) < (cooldownSeconds * 1000L)) {
                return false;
            }

            playerTriggerHistory.put(playerId, currentTime);
        }

        if (onlyOnce) {
            String playerId = player.getUniqueId().toString() + "_once";
            if (playerTriggerHistory.containsKey(playerId)) {
                return false;
            }

            playerTriggerHistory.put(playerId, System.currentTimeMillis());
        }

        return executeActions(player, location, data);
    }

    @Override
    public boolean checkConditions(Player player, Map<String, Object> data) {
        if (!enabled || player == null) {
            return false;
        }

        String currentRegionEvent = (String) data.get("region_event");
        if (currentRegionEvent != null && !regionEvent.equals("both")) {
            if (!regionEvent.equals(currentRegionEvent)) {
                return false;
            }
        }

        Location playerLoc = player.getLocation();

        return worldName == null || worldName.isEmpty() || Objects.requireNonNull(playerLoc.getWorld()).getName().equals(worldName);
    }

    /**
     * Vérification manuelle si un joueur est dans la région
     * (utilisée par le RegionTriggerHandler)
     */
    public boolean isPlayerInRegion(Player player) {
        if (player == null) return false;

        Location playerLoc = player.getLocation();

        if (worldName != null && !worldName.isEmpty() && !playerLoc.getWorld().getName().equals(worldName)) {
            return false;
        }

        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());

        double px = playerLoc.getX();
        double py = playerLoc.getY();
        double pz = playerLoc.getZ();

        return px >= minX && px <= maxX &&
               py >= minY && py <= maxY &&
               pz >= minZ && pz <= maxZ;
    }

    @Override
    public String getType() {
        return "region_trigger";
    }

    /**
     * Remet à zéro l'historique d'un joueur (utile pour les tests ou reset)
     */
    public void resetPlayerHistory(Player player) {
        String playerId = player.getUniqueId().toString();
        playerTriggerHistory.remove(playerId);
        playerTriggerHistory.remove(playerId + "_once");
    }

    /**
     * Remet à zéro tout l'historique du trigger
     */
    public void resetAllHistory() {
        playerTriggerHistory.clear();
    }

    /**
     * Vérifie si un joueur a déjà déclenché ce trigger (pour "une seule fois")
     */
    public boolean hasPlayerTriggeredOnce(Player player) {
        return playerTriggerHistory.containsKey(player.getUniqueId().toString() + "_once");
    }

    /**
     * Obtient le temps restant du cooldown pour un joueur (en millisecondes)
     */
    public long getRemainingCooldown(Player player) {
        if (cooldownSeconds <= 0) return 0;

        String playerId = player.getUniqueId().toString();
        Long lastTrigger = playerTriggerHistory.get(playerId);

        if (lastTrigger == null) return 0;

        long elapsed = System.currentTimeMillis() - lastTrigger;
        long cooldownMs = cooldownSeconds * 1000L;

        return Math.max(0, cooldownMs - elapsed);
    }

    /**
     * Informations de debug pour ce trigger
     */
    public Map<String, Object> getDebugInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("trigger_id", getTriggerId().toString());
        info.put("name", getName());
        info.put("enabled", isEnabled());
        info.put("region_event", regionEvent);
        info.put("only_once", onlyOnce);
        info.put("cooldown_seconds", cooldownSeconds);
        info.put("world", worldName);
        info.put("region_bounds", String.format("(%,.1f,%,.1f,%,.1f) -> (%,.1f,%,.1f,%,.1f)",
                pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ()));
        info.put("players_in_history", playerTriggerHistory.size());
        info.put("actions_count", getActions().size());
        return info;
    }
}