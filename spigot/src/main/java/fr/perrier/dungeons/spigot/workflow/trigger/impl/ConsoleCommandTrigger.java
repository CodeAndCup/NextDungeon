package fr.perrier.dungeons.spigot.workflow.trigger.impl;

import fr.perrier.dungeons.spigot.utils.ServerUtil;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyTrigger;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Trigger qui se déclenche quand une commande est exécutée depuis la console
 */
@Getter
@Setter
@BlocklyInfo(
        name = "console_command_trigger",
        color = "#607D8B",
        displayText = "🖥 Quand la console exécute une commande",
        tooltip = "Déclenche quand une commande spécifique est exécutée depuis la console du serveur",
        category = "Triggers"
)
public class ConsoleCommandTrigger extends Trigger implements BlocklyTrigger {

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Commande:",
            defaultValue = "", order = 1)
    private String command;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Type de correspondance:",
            options = "equals,starts_with,contains",
            defaultValue = "equals", order = 2)
    private String matchType;

    @BlocklyField(type = BlocklyField.FieldType.CHECKBOX, label = "Sensible à la casse:",
            defaultValue = "false", order = 3)
    private boolean caseSensitive;

    @BlocklyField(type = BlocklyField.FieldType.CHECKBOX, label = "Annuler la commande:",
            defaultValue = "true", order = 4)
    private boolean cancelCommand;

    public ConsoleCommandTrigger(String name) {
        super(name);
        this.command = "";
        this.matchType = "equals";
        this.caseSensitive = false;
        this.cancelCommand = true;
    }

    public ConsoleCommandTrigger() {
        super("Console Command Trigger");
        this.command = "";
        this.matchType = "equals";
        this.caseSensitive = false;
        this.cancelCommand = true;
    }

    @Override
    public String getType() {
        return "console_command_trigger";
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
        return enabled;
    }

    /**
     * Vérifie si ce trigger correspond à la commande exécutée.
     * @param fullCommand la ligne de commande complète saisie en console (sans préfixe '/')
     */
    public boolean matchesCommand(String fullCommand) {
        if (command == null || command.isEmpty()) {
            return false;
        }

        String input = caseSensitive ? fullCommand : fullCommand.toLowerCase();
        String key = caseSensitive ? command : command.toLowerCase();

        return switch (matchType) {
            case "starts_with" -> input.startsWith(key);
            case "contains" -> input.contains(key);
            default -> {
                // equals: matche soit toute la ligne, soit la première partie suivie d'un espace (args)
                if (input.equals(key)) yield true;
                yield input.startsWith(key + " ");
            }
        };
    }

    public boolean shouldCancelCommand() {
        return cancelCommand;
    }
}
