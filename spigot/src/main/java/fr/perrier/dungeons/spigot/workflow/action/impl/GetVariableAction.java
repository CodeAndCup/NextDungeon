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
 * Action pour récupérer la valeur d'une variable
 */
@Setter
@Getter
@BlocklyInfo(
        name = "get_variable_action",
        color = "#2196F3",
        displayText = "🔍 Récupérer variable",
        tooltip = "Récupère la valeur d'une variable et la stocke dans une nouvelle variable",
        category = "Actions"
)
public class GetVariableAction extends Action implements BlocklyAction {
    @Serial
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Variable source:",
            defaultValue = "ma_variable", order = 1)
    private String sourceVariableName;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Variable destination:",
            defaultValue = "resultat_variable", order = 2)
    private String destinationVariableName;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Portée source:",
            options = "player,global", defaultValue = "player", order = 3)
    private String sourceScope;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Portée destination:",
            options = "player,global", defaultValue = "player", order = 4)
    private String destinationScope;

    public GetVariableAction() {
        super("Get Variable", "get_variable_action");
        this.sourceVariableName = "ma_variable";
        this.destinationVariableName = "resultat_variable";
        this.sourceScope = "player";
        this.destinationScope = "player";
    }

    public GetVariableAction(String sourceVariableName, String destinationVariableName, String sourceScope, String destinationScope) {
        super("Get Variable", "get_variable_action");
        this.sourceVariableName = sourceVariableName;
        this.destinationVariableName = destinationVariableName;
        this.sourceScope = sourceScope;
        this.destinationScope = destinationScope;
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        if (sourceVariableName == null || sourceVariableName.trim().isEmpty()) {
            Main.getLoggerUtil().warning("Source variable name is empty in GetVariableAction");
            return false;
        }

        if (destinationVariableName == null || destinationVariableName.trim().isEmpty()) {
            Main.getLoggerUtil().warning("Destination variable name is empty in GetVariableAction");
            return false;
        }

        String trimmedSourceName = sourceVariableName.trim();
        String trimmedDestinationName = destinationVariableName.trim();
        String sourceScopeToUse = sourceScope != null ? sourceScope : "player";
        String destinationScopeToUse = destinationScope != null ? destinationScope : "player";

        // Récupérer la valeur de la variable source
        Object sourceValue = Main.getInstance().getVariableRegistry().getVariable(triggerPlayer, trimmedSourceName, sourceScopeToUse);

        if (sourceValue == null) {
            if (Main.getLoggerUtil().isDebugEnabled()) {
                Main.getLoggerUtil().info("Variable " + trimmedSourceName + " not found, using empty string");
            }
            sourceValue = "";
        }

        // Stocker dans la variable destination
        Main.getInstance().getVariableRegistry().setVariable(triggerPlayer, trimmedDestinationName, sourceValue, destinationScopeToUse);

        // Aussi stocker dans le contexte actuel
        data.put(trimmedDestinationName, sourceValue);

        if (Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info("Got variable " + trimmedSourceName + " = " + sourceValue + " and stored in " + trimmedDestinationName);
        }

        return true;
    }
}
