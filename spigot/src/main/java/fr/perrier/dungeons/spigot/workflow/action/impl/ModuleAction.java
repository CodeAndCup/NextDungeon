package fr.perrier.dungeons.spigot.workflow.action.impl;

import fr.perrier.dungeons.common.module.ModuleActionHandler;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Action that delegates execution to a dynamic module action handler.
 * Created by ActionFactory when the action type matches a module-registered block.
 *
 * <p>The {@code handler} field is transient so Gson can serialize/deserialize this class.
 * On deserialization the handler is resolved lazily from the ModuleLoader at execution time.</p>
 */
@Getter
public class ModuleAction extends Action implements BlocklyAction {

    private final Map<String, Object> parameters;

    /** Transient: looked up from ModuleLoader at execution time */
    @Setter
    private transient ModuleActionHandler handler;

    public ModuleAction(String type, Map<String, Object> parameters, ModuleActionHandler handler) {
        super("ModuleAction:" + type, type);
        this.parameters = parameters;
        this.handler = handler;
    }

    @Override
    public boolean execute(Player player, Location location, Map<String, Object> data) {
        // Lazy handler resolution: if handler is null (e.g. after deserialization), look it up
        if (handler == null) {
            handler = resolveHandler();
        }
        if (handler == null) {
            Main.getLoggerUtil().warning("No handler registered for module action '" + type + "', cannot execute");
            return false;
        }

        Map<String, Object> merged = new HashMap<>(parameters);
        merged.put("player", player);
        merged.put("location", location);
        if (data != null) {
            merged.putAll(data);
        }
        try {
            return handler.execute(merged);
        } catch (Exception e) {
            Main.getLoggerUtil().severe("Error executing module action '" + type + "': " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    /**
     * Resolves the action handler from ModuleLoader using the action type.
     */
    private ModuleActionHandler resolveHandler() {
        if (Main.getInstance().getModuleLoader() == null) return null;
        return Main.getInstance().getModuleLoader().getActionHandler(type);
    }
}
