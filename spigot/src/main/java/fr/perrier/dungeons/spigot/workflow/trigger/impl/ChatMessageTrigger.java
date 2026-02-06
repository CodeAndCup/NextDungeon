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

import java.util.Map;

/**
 * Trigger qui se déclenche quand un joueur envoie un message dans le chat
 */
@Getter
@Setter
@BlocklyInfo(
        name = "chat_message_trigger",
        color = "#3F51B5",
        displayText = "💬 Quand le joueur envoie un message",
        tooltip = "Déclenche quand un joueur envoie un message contenant un mot-clé",
        category = "Triggers"
)
public class ChatMessageTrigger extends Trigger implements BlocklyTrigger {

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Mot-clé:",
            defaultValue = "", order = 1)
    private String keyword;

    @BlocklyField(type = BlocklyField.FieldType.CHECKBOX, label = "Sensible à la casse:",
            defaultValue = "false", order = 2)
    private boolean caseSensitive;

    @BlocklyField(type = BlocklyField.FieldType.CHECKBOX, label = "Annuler le message:",
            defaultValue = "false", order = 3)
    private boolean cancelMessage;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Type de correspondance:",
            options = "contains,equals,starts_with,ends_with",
            defaultValue = "contains", order = 4)
    private String matchType;

    public ChatMessageTrigger(String name) {
        super(name);
        this.keyword = "";
        this.caseSensitive = false;
        this.cancelMessage = false;
        this.matchType = "contains";
    }

    public ChatMessageTrigger() {
        super("Chat Message Trigger");
        this.keyword = "";
        this.caseSensitive = false;
        this.cancelMessage = false;
        this.matchType = "contains";
    }

    @Override
    public String getType() {
        return "chat_message_trigger";
    }

    @Override
    public boolean execute(Player player, Location location, Map<String, Object> data) {
        if (ServerUtil.isInEditMode()) return false;

        if (!checkConditions(player, data)) {
            return false;
        }
        return executeActions(player, location, data);
    }

    @Override
    public boolean checkConditions(Player player, Map<String, Object> data) {
        return true;
    }

    /**
     * Vérifie si ce trigger correspond au message
     */
    public boolean matchesMessage(String message) {
        if (keyword == null || keyword.isEmpty()) {
            return true; // Tous les messages si pas de mot-clé
        }

        String msg = caseSensitive ? message : message.toLowerCase();
        String key = caseSensitive ? keyword : keyword.toLowerCase();

        return switch (matchType) {
            case "equals" -> msg.equals(key);
            case "starts_with" -> msg.startsWith(key);
            case "ends_with" -> msg.endsWith(key);
            default -> msg.contains(key); // contains par défaut
        };
    }

    public boolean shouldCancelMessage() {
        return cancelMessage;
    }
}
