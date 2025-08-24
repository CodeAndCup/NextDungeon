package fr.perrier.dungeons.trigger.impl;

import fr.perrier.dungeons.trigger.Trigger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

@Getter@Setter
public class LocationTrigger extends Trigger {
    private Location targetLocation;
    private double radius;

    public LocationTrigger(String name) {
        super(name);
        this.radius = 2.0;
    }

    @Override
    public boolean execute(Player player, Location location, Map<String, Object> data) {
        if (checkConditions(player, data)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean checkConditions(Player player, Map<String, Object> data) {
        if (!enabled || targetLocation == null) return false;

        Location playerLoc = player.getLocation();
        return playerLoc.distance(targetLocation) <= radius;
    }

    @Override
    public String getType() {
        return "location";
    }

    @Override
    public Map<String, Object> getBlocklyConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("type", getType());
        config.put("color", "#4ECDC4");
        config.put("icon", "map-marker");

        Map<String, Object> fields = new HashMap<>();
        fields.put("x", Map.of("type", "number", "label", "Coordonnée X"));
        fields.put("y", Map.of("type", "number", "label", "Coordonnée Y"));
        fields.put("z", Map.of("type", "number", "label", "Coordonnée Z"));
        fields.put("radius", Map.of("type", "number", "label", "Rayon", "default", 2.0));
        config.put("fields", fields);

        return config;
    }
}
