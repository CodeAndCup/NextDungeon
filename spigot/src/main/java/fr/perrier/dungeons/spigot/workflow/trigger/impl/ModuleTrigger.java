package fr.perrier.dungeons.spigot.workflow.trigger.impl;

import fr.perrier.dungeons.spigot.workflow.action.impl.ModuleAction;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic trigger for module-provided trigger types (e.g. cinematic_on_end).
 * Stores the module block type and any parameters from the Blockly editor.
 *
 * <p>Analogous to {@link ModuleAction} — this is a concrete Trigger subclass
 * that can hold arbitrary module trigger data and be serialized/deserialized.</p>
 */
@Getter
@Setter
public class ModuleTrigger extends Trigger {

    /** The Blockly block type (e.g. "cinematic_on_end") */
    private String type;

    /** Module-defined parameters from the Blockly editor */
    private Map<String, Object> parameters;

    public ModuleTrigger(String name, String type, Map<String, Object> parameters) {
        super(name);
        this.type = type;
        this.parameters = parameters != null ? parameters : new HashMap<>();
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public boolean execute(Player player, Location location, Map<String, Object> data) {
        if (!enabled) return false;
        return executeActions(player, location, data);
    }

    @Override
    public boolean checkConditions(Player player, Map<String, Object> data) {
        return enabled;
    }
}
