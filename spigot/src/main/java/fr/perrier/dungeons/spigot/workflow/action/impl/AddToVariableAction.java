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
 * Action to add a value to an existing variable
 */
@Setter
@Getter
@BlocklyInfo(
        name = "add_to_variable_action",
        color = "#FF5722",
        displayText = "➕ Ajouter à variable",
        tooltip = "Ajoute une valeur à une variable existante",
        category = "Actions"
)
public class AddToVariableAction extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Nom variable:",
            defaultValue = "ma_variable", order = 1)
    private String variableName;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Valeur à ajouter:",
            defaultValue = "1", order = 2)
    private String value;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Portée:",
            options = "player,global", defaultValue = "player", order = 3)
    private String scope;

    public AddToVariableAction() {
        super("Add To Variable", "add_to_variable_action");
        this.variableName = "ma_variable";
        this.value = "1";
        this.scope = "player";
    }

    public AddToVariableAction(String variableName, String value, String scope) {
        super("Add To Variable", "add_to_variable_action");
        this.variableName = variableName;
        this.value = value;
        this.scope = scope;
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        if (variableName == null || variableName.trim().isEmpty()) {
            Main.getLoggerUtil().warning("Variable name is empty in AddToVariableAction");
            return false;
        }

        if (value == null) {
            Main.getLoggerUtil().warning("Variable value is null in AddToVariableAction");
            return false;
        }

        String trimmedName = variableName.trim();
        String scopeToUse = scope != null ? scope : "player";

        // Get the current variable value
        Object currentValue = Main.getInstance().getVariableRegistry().getVariable(triggerPlayer, trimmedName);

        // If variable doesn't exist, initialize it to 0
        if (currentValue == null) {
            currentValue = 0;
        }

        // Parse the value to add
        Object valueToAdd = parseValue(value);

        // Calculate the new value
        Object newValue = addValues(currentValue, valueToAdd);

        // Set the new variable value
        Main.getInstance().getVariableRegistry().setVariable(triggerPlayer, trimmedName, newValue, scopeToUse);

        // Also store in the current data context for immediate use
        data.put(trimmedName, newValue);

        if (Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info("Added " + valueToAdd + " to variable " + trimmedName + " (scope: " + scopeToUse + "). New value: " + newValue);
        }

        return true;
    }

    /**
     * Add two values together, handling different types
     */
    private Object addValues(Object current, Object toAdd) {
        // Both are numbers
        if ((current instanceof Number curentNumber) && (toAdd instanceof Number newNumber)) {
            double currentNum = curentNumber.doubleValue();
            double addNum = newNumber.doubleValue();
            double result = currentNum + addNum;

            // Return as integer if both were integers and result has no decimal part
            if (current instanceof Integer && toAdd instanceof Integer && result == Math.floor(result)) {
                return (int) result;
            }
            return result;
        }

        // String concatenation fallback
        return current.toString() + toAdd.toString();
    }

    /**
     * Parse value from string to appropriate type
     */
    private Object parseValue(String valueStr) {
        if (valueStr == null || valueStr.trim().isEmpty()) {
            return 0;
        }

        String trimmed = valueStr.trim();

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


