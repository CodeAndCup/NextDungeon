package fr.perrier.dungeons.workflow.trigger.impl;

import fr.perrier.dungeons.webserver.blockly.BlocklyTrigger;
import fr.perrier.dungeons.webserver.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.webserver.blockly.annotations.BlocklyInfo;
import fr.perrier.dungeons.workflow.trigger.Trigger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

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
        category = "Régions"
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

    // Nouvelles options pour le système global
    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Événement:", order = 8, options = "enter,exit,both")
    private String regionEvent = "enter"; // "enter", "exit", "both"

    @BlocklyField(type = BlocklyField.FieldType.CHECKBOX, label = "Une seule fois:", order = 9)
    private boolean onlyOnce = false; // Déclenche seulement une fois par joueur

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Cooldown (sec):", defaultValue = "0", order = 10)
    private int cooldownSeconds = 0; // Cooldown entre les déclenchements

    // Cache pour le système "une seule fois" et les cooldowns
    private final Map<String, Long> playerTriggerHistory = new HashMap<>();

    public RegionTrigger(String name) {
        super(name);
        this.worldName = "world"; // Monde par défaut
    }

    @Override
    public String getBlockName() {
        return "region_trigger";
    }

    @Override
    public String getColor() {
        return "#4CAF50";
    }

    @Override
    public String getTooltip() {
        return "Déclenche quand un joueur entre ou sort d'une région définie";
    }

    @Override
    public String getDisplayText() {
        return "📍 Quand le joueur " + getRegionEventText() + " région";
    }

    private String getRegionEventText() {
        return switch (regionEvent.toLowerCase()) {
            case "enter" -> "entre en";
            case "exit" -> "sort de";
            case "both" -> "entre/sort de";
            default -> "entre en";
        };
    }

    @Override
    public boolean execute(Player player, Location location, Map<String, Object> data) {
        if (!checkConditions(player, data)) {
            return false;
        }

        // Vérifier le cooldown si activé
        if (cooldownSeconds > 0) {
            String playerId = player.getUniqueId().toString();
            long currentTime = System.currentTimeMillis();
            Long lastTrigger = playerTriggerHistory.get(playerId);

            if (lastTrigger != null && (currentTime - lastTrigger) < (cooldownSeconds * 1000L)) {
                return false; // Encore en cooldown
            }

            playerTriggerHistory.put(playerId, currentTime);
        }

        // Vérifier "une seule fois" si activé
        if (onlyOnce) {
            String playerId = player.getUniqueId().toString() + "_once";
            if (playerTriggerHistory.containsKey(playerId)) {
                return false; // Déjà déclenché pour ce joueur
            }

            playerTriggerHistory.put(playerId, System.currentTimeMillis());
        }

        // Exécuter toutes les actions définies
        return executeActions(player, location, data);
    }

    @Override
    public boolean checkConditions(Player player, Map<String, Object> data) {
        if (!enabled || player == null) {
            return false;
        }

        // Vérifier le type d'événement de région si spécifié dans les données
        String currentRegionEvent = (String) data.get("region_event");
        if (currentRegionEvent != null && !regionEvent.equals("both")) {
            if (!regionEvent.equals(currentRegionEvent)) {
                return false; // L'événement ne correspond pas
            }
        }

        Location playerLoc = player.getLocation();

        // Vérifier le monde si spécifié
        if (worldName != null && !worldName.isEmpty() && !playerLoc.getWorld().getName().equals(worldName)) {
            return false;
        }

        // Le RegionTriggerHandler s'occupe déjà de vérifier si le joueur est dans la région
        // Ici on peut ajouter des conditions supplémentaires si nécessaire

        return true; // Les conditions de base sont remplies
    }

    /**
     * Vérification manuelle si un joueur est dans la région
     * (utilisée par le RegionTriggerHandler)
     */
    public boolean isPlayerInRegion(Player player) {
        if (player == null) return false;

        Location playerLoc = player.getLocation();

        // Vérifier le monde si spécifié
        if (worldName != null && !worldName.isEmpty() && !playerLoc.getWorld().getName().equals(worldName)) {
            return false;
        }

        // Calculer les coordonnées min/max
        double minX = Math.min(pos1X, pos2X);
        double maxX = Math.max(pos1X, pos2X);
        double minY = Math.min(pos1Y, pos2Y);
        double maxY = Math.max(pos1Y, pos2Y);
        double minZ = Math.min(pos1Z, pos2Z);
        double maxZ = Math.max(pos1Z, pos2Z);

        // Vérifier si le joueur est dans la région
        double px = playerLoc.getX();
        double py = playerLoc.getY();
        double pz = playerLoc.getZ();

        return px >= minX && px <= maxX &&
                py >= minY && py <= maxY &&
                pz >= minZ && pz <= maxZ;
    }

    @Override
    public String getType() {
        return "region";
    }

    @Override
    public Map<String, Object> getBlocklyConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("type", getType());
        config.put("color", "#4CAF50");
        config.put("icon", "crop-free");
        config.put("category", "location");

        Map<String, Object> fields = new HashMap<>();

        // Coordonnées de la région
        fields.put("pos1_x", Map.of("type", "number", "label", "Position 1 - X", "default", 0));
        fields.put("pos1_y", Map.of("type", "number", "label", "Position 1 - Y", "default", 64));
        fields.put("pos1_z", Map.of("type", "number", "label", "Position 1 - Z", "default", 0));
        fields.put("pos2_x", Map.of("type", "number", "label", "Position 2 - X", "default", 10));
        fields.put("pos2_y", Map.of("type", "number", "label", "Position 2 - Y", "default", 74));
        fields.put("pos2_z", Map.of("type", "number", "label", "Position 2 - Z", "default", 10));
        fields.put("world", Map.of("type", "text", "label", "Monde", "default", "world"));

        // Nouvelles options
        fields.put("region_event", Map.of(
                "type", "dropdown",
                "label", "Événement",
                "default", "enter",
                "options", new String[]{"enter", "exit", "both"}
        ));

        fields.put("only_once", Map.of(
                "type", "checkbox",
                "label", "Une seule fois par joueur",
                "default", false
        ));

        fields.put("cooldown_seconds", Map.of(
                "type", "number",
                "label", "Cooldown (secondes)",
                "default", 0,
                "min", 0
        ));

        config.put("fields", fields);

        return config;
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