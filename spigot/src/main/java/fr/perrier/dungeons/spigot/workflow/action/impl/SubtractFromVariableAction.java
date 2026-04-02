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

import java.io.Serial;
import java.util.Map;

/**
 * Action to subtract a value from an existing variable
 */
@Setter
@Getter
@BlocklyInfo(
        name = "subtract_from_variable_action",
        color = "#FF5722",
        displayText = "➖ Retirer à variable",
        tooltip = "Retire une valeur d'une variable existante",
        category = "Actions"
)
public class SubtractFromVariableAction extends Action implements BlocklyAction {
    @Serial
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Nom variable:",
            defaultValue = "ma_variable", order = 1)
    private String variableName;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Valeur à retirer:",
            defaultValue = "1", order = 2)
    private String value;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Portée:",
            options = "player,global", defaultValue = "player", order = 3)
    private String scope;

    public SubtractFromVariableAction() {
        super("Subtract From Variable", "subtract_from_variable_action");
        this.variableName = "ma_variable";
        this.value = "1";
        this.scope = "player";
    }

    public SubtractFromVariableAction(String variableName, String value, String scope) {
        super("Subtract From Variable", "subtract_from_variable_action");
        this.variableName = variableName;
        this.value = value;
        this.scope = scope;
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        if (variableName == null || variableName.trim().isEmpty()) {
            Main.getLoggerUtil().warning("Variable name is empty in SubtractFromVariableAction");
            return false;
        }

        if (value == null) {
            Main.getLoggerUtil().warning("Variable value is null in SubtractFromVariableAction");
            return false;
        }

        String trimmedName = variableName.trim();
        String scopeToUse = scope != null ? scope : "player";

        // Get the current variable value
        Object currentValue = Main.getInstance().getVariableRegistry().getVariable(triggerPlayer, trimmedName, scopeToUse);

        // If variable doesn't exist, initialize it to 0
        if (currentValue == null) {
            currentValue = 0;
        }

        // Parse the value to subtract
        Object valueToSubtract = parseValue(value);

        // Calculate the new value
        Object newValue = subtractValues(currentValue, valueToSubtract);

        // Set the new variable value
        Main.getInstance().getVariableRegistry().setVariable(triggerPlayer, trimmedName, newValue, scopeToUse);

        // Also store in the current data context for immediate use
        data.put(trimmedName, newValue);

        if (Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info("Subtracted " + valueToSubtract + " from variable " + trimmedName + " (scope: " + scopeToUse + "). New value: " + newValue);
        }

        return true;
    }

    /**
     * Subtract values, handling different types
     */
    private Object subtractValues(Object current, Object toSubtract) {
        // Both are numbers
        if (current instanceof Number currentNumber && toSubtract instanceof Number subtractNumber) {
            double currentDouble = currentNumber.doubleValue();
            double subtractDouble = subtractNumber.doubleValue();
            double result = currentDouble - subtractDouble;

            // Return as integer if both were integers and result has no decimal part
            if (current instanceof Integer && toSubtract instanceof Integer && result == Math.floor(result)) {
                return (int) result;
            }
            return result;
        }

        // If not numbers, try to parse as numbers
        try {
            double currentNum = Double.parseDouble(current.toString());
            double subtractNum = Double.parseDouble(toSubtract.toString());
            return currentNum - subtractNum;
        } catch (NumberFormatException e) {
            Main.getLoggerUtil().warning("Cannot subtract non-numeric values in SubtractFromVariableAction");
            return current;
        }
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

}


