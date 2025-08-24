package fr.perrier.dungeons.workflow.trigger.impl;

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

/**
 * Trigger qui se déclenche quand un joueur entre dans une région définie par deux positions
 */
@Getter
@Setter
@BlocklyInfo(
        name = "region_trigger",
        color = "#4CAF50",
        displayText = "📍 Quand le joueur entre en région",
        tooltip = "Déclenche quand un joueur entre dans une région définie",
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
        return "Déclenche quand un joueur entre dans une région définie";
    }

    @Override
    public String getDisplayText() {
        return "📍 Quand le joueur entre en région";
    }

    @Override
    public boolean execute(Player player, Location location, Map<String, Object> data) {
        if (checkConditions(player, data)) {
            // Exécuter toutes les actions définies
            return executeActions(player, location, data);
        }
        return false;
    }

    @Override
    public boolean checkConditions(Player player, Map<String, Object> data) {
        if (!enabled || player == null) {
            return false;
        }

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
        fields.put("pos1_x", Map.of("type", "number", "label", "Position 1 - X", "default", 0));
        fields.put("pos1_y", Map.of("type", "number", "label", "Position 1 - Y", "default", 64));
        fields.put("pos1_z", Map.of("type", "number", "label", "Position 1 - Z", "default", 0));
        fields.put("pos2_x", Map.of("type", "number", "label", "Position 2 - X", "default", 10));
        fields.put("pos2_y", Map.of("type", "number", "label", "Position 2 - Y", "default", 74));
        fields.put("pos2_z", Map.of("type", "number", "label", "Position 2 - Z", "default", 10));
        fields.put("world", Map.of("type", "text", "label", "Monde", "default", "world"));
        config.put("fields", fields);

        return config;
    }
}
