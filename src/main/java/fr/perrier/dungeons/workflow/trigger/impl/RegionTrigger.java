package fr.perrier.dungeons.workflow.trigger.impl;

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
public class RegionTrigger extends Trigger {
    private double pos1X, pos1Y, pos1Z;
    private double pos2X, pos2Y, pos2Z;
    private String worldName;

    public RegionTrigger(String name) {
        super(name);
        this.worldName = "world"; // Monde par défaut
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
