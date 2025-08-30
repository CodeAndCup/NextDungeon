package fr.perrier.dungeons.spigot.workflow.trigger.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.utils.ServerUtil;
import fr.perrier.dungeons.spigot.webserver.blockly.BlocklyTrigger;
import fr.perrier.dungeons.spigot.webserver.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webserver.blockly.annotations.BlocklyInfo;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@BlocklyInfo(
        name = "function_trigger",
        color = "#673AB7",
        displayText = "🔧 Fonction",
        tooltip = "Définit une fonction personnalisée réutilisable",
        category = "Triggers"
)
public class FunctionTrigger extends Trigger implements BlocklyTrigger {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Nom:",
            defaultValue = "ma_fonction", order = 1)
    private String functionName;

    public FunctionTrigger() {
        super("Function Definition");
        this.functionName = "ma_fonction";
    }

    public FunctionTrigger(String name) {
        super(name);
        this.functionName = "ma_fonction";
    }

    @Override
    public boolean execute(Player player, Location location, Map<String, Object> data) {
        if(ServerUtil.isInEditMode()) return false;

        return executeActions(player, location, data);
    }

    @Override
    public boolean checkConditions(Player player, Map<String, Object> data) {
        // Function definitions are always valid if they have a name
        return enabled && functionName != null && !functionName.trim().isEmpty();
    }

    @Override
    public String getType() {
        return "function_trigger";
    }

    @Override
    public boolean hasActions() {
        return true;
    }

    /**
     * Register this function definition with the global function registry
     */
    public void registerFunction() {
        if (functionName == null || functionName.trim().isEmpty()) {
            return;
        }

        // Store this function definition in the global trigger manager
        Main.getInstance().getGlobalTriggerManager().registerFunction(this);

        Main.getInstance().getLogger().info("Registered function: " + functionName + " with " +
                (getActions() != null ? getActions().size() : 0) + " actions");
    }

    /**
     * Execute this function
     */
    public boolean executeFunction(Player player, Location location, Map<String, Object> data) {
        if (!enabled) {
            return false;
        }

        // Create a new data context
        Map<String, Object> functionData = new HashMap<>(data);
        functionData.put("function_name", this.functionName);

        // Execute all actions in this function
        return executeActions(player, location, functionData);
    }
}
