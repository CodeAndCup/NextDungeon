package fr.perrier.dungeons.workflow.variable;

import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.webserver.blockly.BlocklyComponent;
import fr.perrier.dungeons.webserver.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.webserver.blockly.annotations.BlocklyInfo;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.io.Serializable;

/**
 * Block to get a variable value (used as value input in other blocks)
 */
@Getter
@Setter
@BlocklyInfo(
        name = "get_variable",
        color = "#FF9800",
        displayText = "📄 Variable",
        tooltip = "Récupère la valeur d'une variable",
        category = "Variables"
)
public class GetVariableBlock implements BlocklyComponent, Serializable {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Nom:",
            defaultValue = "ma_variable", order = 1)
    private String variableName;

    public GetVariableBlock() {
        this.variableName = "ma_variable";
    }

    public GetVariableBlock(String variableName) {
        this.variableName = variableName;
    }

    @Override
    public String getBlockName() {
        return "get_variable";
    }

    @Override
    public String getColor() {
        return "#FF9800";
    }

    @Override
    public String getTooltip() {
        return "Récupère la valeur d'une variable";
    }

    @Override
    public String getDisplayText() {
        return "📄 " + (variableName != null ? variableName : "variable");
    }

    /**
     * Get the variable value for a specific player
     */
    public Object getValue(Player player) {
        if (variableName == null || variableName.trim().isEmpty()) {
            return null;
        }

        return Main.getInstance().getVariableManager().getVariable(player, variableName.trim());
    }

    /**
     * Get the variable value as string
     */
    public String getValueAsString(Player player) {
        Object value = getValue(player);
        return value != null ? value.toString() : "";
    }

    /**
     * Get the variable value as number
     */
    public double getValueAsNumber(Player player) {
        Object value = getValue(player);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    /**
     * Get the variable value as boolean
     */
    public boolean getValueAsBoolean(Player player) {
        Object value = getValue(player);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0;
        }
        return false;
    }
}
