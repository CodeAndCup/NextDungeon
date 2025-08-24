package fr.perrier.dungeons.trigger.impl;

import fr.perrier.dungeons.trigger.Trigger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Trigger de debug qui se déclenche sur un message spécifique
 */
public class DebugTrigger extends Trigger {
    private String triggerMessage;
    private boolean caseSensitive;
    private boolean exactMatch;

    public DebugTrigger(String name) {
        super(name);
        this.triggerMessage = "test";
        this.caseSensitive = false;
        this.exactMatch = true;
    }

    @Override
    public boolean execute(Player player, Location location, Map<String, Object> data) {
        if (checkConditions(player, data)) {
            // Log de debug
            player.sendMessage("§e[DEBUG] §7Trigger activé: §a" + getName());
            player.sendMessage("§e[DEBUG] §7Message détecté: §f" + data.get("message"));
            player.sendMessage("§e[DEBUG] §7Heure: §f" + java.time.LocalDateTime.now().toString());
            return true;
        }
        return false;
    }

    @Override
    public boolean checkConditions(Player player, Map<String, Object> data) {
        if (!enabled || triggerMessage == null || triggerMessage.isEmpty()) {
            return false;
        }

        String message = (String) data.get("message");
        if (message == null) {
            return false;
        }

        // Gestion de la casse
        String checkMessage = caseSensitive ? message : message.toLowerCase();
        String targetMessage = caseSensitive ? triggerMessage : triggerMessage.toLowerCase();

        // Vérification exacte ou contient
        if (exactMatch) {
            return checkMessage.equals(targetMessage);
        } else {
            return checkMessage.contains(targetMessage);
        }
    }

    @Override
    public String getType() {
        return "debug_chat";
    }

    @Override
    public Map<String, Object> getBlocklyConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("type", getType());
        config.put("color", "#9B59B6");
        config.put("icon", "bug");
        config.put("category", "debug");

        Map<String, Object> fields = new HashMap<>();
        fields.put("trigger_message", Map.of(
                "type", "text",
                "label", "Message déclencheur",
                "default", "test",
                "placeholder", "Tapez le message..."
        ));
        fields.put("case_sensitive", Map.of(
                "type", "checkbox",
                "label", "Sensible à la casse",
                "default", false
        ));
        fields.put("exact_match", Map.of(
                "type", "checkbox",
                "label", "Correspondance exacte",
                "default", true
        ));
        config.put("fields", fields);

        return config;
    }

    /**
     * Méthode utilitaire pour vérifier si un événement chat correspond
     */
    public boolean checkChatEvent(AsyncPlayerChatEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", event.getMessage());
        data.put("event_type", "chat");

        return checkConditions(event.getPlayer(), data);
    }

    // Getters et Setters
    public String getTriggerMessage() { return triggerMessage; }
    public void setTriggerMessage(String triggerMessage) { this.triggerMessage = triggerMessage; }
    public boolean isCaseSensitive() { return caseSensitive; }
    public void setCaseSensitive(boolean caseSensitive) { this.caseSensitive = caseSensitive; }
    public boolean isExactMatch() { return exactMatch; }
    public void setExactMatch(boolean exactMatch) { this.exactMatch = exactMatch; }
}
