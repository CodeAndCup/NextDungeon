package fr.perrier.dungeons.workflow.trigger.impl;

import fr.perrier.cupcodeapi.utils.RegionUtils;
import fr.perrier.dungeons.webserver.blockly.BlocklyTrigger;
import fr.perrier.dungeons.webserver.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.webserver.blockly.annotations.BlocklyInfo;
import fr.perrier.dungeons.workflow.trigger.Trigger;
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

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "X1:", defaultValue = "0", order = 1)
    private double pos1X;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Y1:", defaultValue = "64", order = 2)
    private double pos1Y;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Z1:", defaultValue = "0", order = 3)
    private double pos1Z;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "X2:", defaultValue = "10", order = 4)
    private double pos2X;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Y2:", defaultValue = "74", order = 5)
    private double pos2Y;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Z2:", defaultValue = "10", order = 6)
    private double pos2Z;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Monde:", defaultValue = "world", order = 7)
    private String worldName;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Événement:", order = 8, options = "enter,exit,both")
    private String regionEvent = "enter";

    @BlocklyField(type = BlocklyField.FieldType.CHECKBOX, label = "Une seule fois:", order = 9)
    private boolean onlyOnce = false;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Cooldown (sec):", defaultValue = "0", order = 10)
    private int cooldownSeconds = 0;

    private final Map<String, Long> playerTriggerHistory = new HashMap<>();

    public RegionTrigger(String name) {
        super(name);
        this.worldName = "world";
    }

    @Override
    public boolean execute(Player player, Location location, Map<String, Object> data) {
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

        if (worldName != null && !worldName.isEmpty() && !Objects.requireNonNull(playerLoc.getWorld()).getName().equals(worldName)) {
            return false;
        }

        return RegionUtils.isInside(
                playerLoc,
                new Location(playerLoc.getWorld(), pos1X, pos1Y, pos1Z),
                new Location(playerLoc.getWorld(), pos2X, pos2Y, pos2Z)
        );
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
                pos1X, pos1Y, pos1Z, pos2X, pos2Y, pos2Z));
        info.put("players_in_history", playerTriggerHistory.size());
        info.put("actions_count", getActions().size());
        return info;
    }
}