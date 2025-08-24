package fr.perrier.dungeons.trigger.impl;

import fr.perrier.dungeons.trigger.Trigger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Trigger pour la mort de mobs MythicMobs
 */
@Getter@Setter
public class MythicMobKillTrigger extends Trigger {
    private String mobInternalName;
    private int requiredKills;
    private String bossName;

    public MythicMobKillTrigger(String name) {
        super(name);
        this.requiredKills = 1;
    }

    @Override
    public boolean execute(Player player, Location location, Map<String, Object> data) {
        // Logique d'exécution du trigger
        if (checkConditions(player, data)) {
            // Exécuter les actions définies
            return true;
        }
        return false;
    }

    @Override
    public boolean checkConditions(Player player, Map<String, Object> data) {
        if (!enabled) return false;

        String killedMob = (String) data.get("mythicmob_internal_name");
        if (mobInternalName != null && !mobInternalName.equals(killedMob)) {
            return false;
        }

        if (bossName != null && !bossName.equals(data.get("boss_name"))) {
            return false;
        }

        return true;
    }

    @Override
    public String getType() {
        return "mythicmob_kill";
    }

    @Override
    public Map<String, Object> getBlocklyConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("type", getType());
        config.put("color", "#FF6B6B");
        config.put("icon", "skull");

        Map<String, Object> fields = new HashMap<>();
        fields.put("mob_internal_name", Map.of("type", "text", "label", "Nom interne du mob"));
        fields.put("required_kills", Map.of("type", "number", "label", "Nombre de kills requis", "default", 1));
        fields.put("boss_name", Map.of("type", "text", "label", "Nom du boss (optionnel)"));
        config.put("fields", fields);

        return config;
    }
}
