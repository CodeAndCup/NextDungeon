package fr.perrier.dungeons.spigot.workflow.action.impl;

import fr.perrier.dungeons.common.module.ModuleActionHandler;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Action that delegates execution to a dynamic module action handler.
 * Created by ActionFactory when the action type matches a module-registered block.
 */
@Getter
public class ModuleAction extends Action implements BlocklyAction {

    private final Map<String, Object> parameters;
    private final ModuleActionHandler handler;

    public ModuleAction(String type, Map<String, Object> parameters, ModuleActionHandler handler) {
        super("ModuleAction:" + type, type);
        this.parameters = parameters;
        this.handler = handler;
    }

    @Override
    public boolean execute(Player player, Location location, Map<String, Object> data) {
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
}
