package fr.perrier.dungeons.spigot.workflow.action.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.util.Map;

/**
 * Action to set a variable value
 */
@Setter
@Getter
@BlocklyInfo(
        name = "set_variable_action",
        color = "#FF5722",
        displayText = "📝 Définir variable",
        tooltip = "Définit la valeur d'une variable",
        category = "Actions"
)
public class SetVariableAction extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Nom variable:",
            defaultValue = "ma_variable", order = 1)
    private String variableName;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Valeur:",
            defaultValue = "0", order = 2)
    private String value;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Portée:",
            options = "player,global", defaultValue = "player", order = 3)
    private String scope;

    public SetVariableAction() {
        super("Set Variable", "set_variable_action");
        this.variableName = "ma_variable";
        this.value = "0";
        this.scope = "player";
    }

    public SetVariableAction(String variableName, String value, String scope) {
        super("Set Variable", "set_variable_action");
        this.variableName = variableName;
        this.value = value;
        this.scope = scope;
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        if (variableName == null || variableName.trim().isEmpty()) {
            Main.getInstance().getLogger().warning("&eVariable name is empty in SetVariableAction");
            return false;
        }

        if (value == null) {
            Main.getInstance().getLogger().warning("&eVariable value is null in SetVariableAction");
            return false;
        }

        String trimmedName = variableName.trim();
        String scopeToUse = scope != null ? scope : "player";

        // Parse the value (try to convert to appropriate type)
        Object parsedValue = parseValue(value);

        // Set the variable using the variable manager
        Main.getInstance().getVariableRegistry().setVariable(triggerPlayer, trimmedName, parsedValue, scopeToUse);

        // Also store in the current data context for immediate use
        data.put(trimmedName, parsedValue);

        if (Main.isDebug()) {
            Main.getInstance().getLogger().info("Set variable " + trimmedName + " = " + parsedValue + " (scope: " + scopeToUse + ")");
        }

        return true;
    }

    /**
     * Parse value from string to appropriate type
     */
    private Object parseValue(String valueStr) {
        if (valueStr == null || valueStr.trim().isEmpty()) {
            return "";
        }

        String trimmed = valueStr.trim();

        // Try boolean
        if (trimmed.equalsIgnoreCase("true") || trimmed.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(trimmed);
        }

        // Try integer
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            // Not an integer
        }

        // Try double
        try {
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException e) {
            // Not a double
        }

        // Return as string
        return trimmed;
    }

    @Override
    public boolean isChainable() {
        return true;
    }
}