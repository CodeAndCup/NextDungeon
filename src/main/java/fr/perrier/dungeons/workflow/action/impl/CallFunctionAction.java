package fr.perrier.dungeons.workflow.action.impl;

import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.webserver.blockly.BlocklyAction;
import fr.perrier.dungeons.webserver.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.webserver.blockly.annotations.BlocklyInfo;
import fr.perrier.dungeons.workflow.action.Action;
import fr.perrier.dungeons.workflow.trigger.impl.FunctionTrigger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.util.Map;

@Setter
@Getter
@BlocklyInfo(
        name = "call_function_action",
        color = "#9C27B0",
        displayText = "📞 Appeler",
        tooltip = "Appelle une fonction définie",
        category = "Actions"
)
public class CallFunctionAction extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Nom fonction:",
            defaultValue = "ma_fonction", order = 1)
    private String functionName;

    public CallFunctionAction() {
        super("Call Function", "call_function_action");
        this.functionName = "ma_fonction";
    }

    public CallFunctionAction(String functionName) {
        super("Call Function", "call_function_action");
        this.functionName = functionName;
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        if (functionName == null || functionName.trim().isEmpty()) {
            Main.getInstance().getLogger().warning("&eFunction name is empty in CallFunctionAction");
            return false;
        }

        String trimmedFunctionName = functionName.trim();

        // Get the function from the global trigger manager
        FunctionTrigger function = Main.getInstance().getGlobalTriggerManager()
                .getFunction(trimmedFunctionName);

        if (function == null) {
            Main.getInstance().getLogger().warning("&eFunction not found: " + trimmedFunctionName);
            return false;
        }

        Main.getInstance().getLogger().info("Calling function: " + trimmedFunctionName);

        // Execute the function
        try {
            boolean result = function.executeFunction(triggerPlayer, location, data);

            if (result) {
                Main.getInstance().getLogger().info("Function " + trimmedFunctionName + " executed successfully");
            } else {
                Main.getInstance().getLogger().warning("&eFunction " + trimmedFunctionName + " execution failed");
            }

            return result;
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("&cError executing function " + trimmedFunctionName + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean isChainable() {
        return true;
    }
}
