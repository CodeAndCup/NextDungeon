package fr.perrier.dungeons.spigot.module;

import fr.perrier.dungeons.common.module.ModuleActionHandler;
import fr.perrier.dungeons.common.module.ModuleBlockRegistry;
import fr.perrier.dungeons.common.module.ModuleContext;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.ModuleTrigger;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Default implementation of ModuleContext provided to modules at load time.
 */
public class DefaultModuleContext implements ModuleContext {

    private final ModuleBlockRegistry blockRegistry;
    private final BiConsumer<String, ModuleActionHandler> handlerRegistrar;

    public DefaultModuleContext(ModuleBlockRegistry blockRegistry, BiConsumer<String, ModuleActionHandler> handlerRegistrar) {
        this.blockRegistry = blockRegistry;
        this.handlerRegistrar = handlerRegistrar;
    }

    @Override
    public ModuleBlockRegistry getBlockRegistry() {
        return blockRegistry;
    }

    @Override
    public void registerActionHandler(String blockId, ModuleActionHandler handler) {
        handlerRegistrar.accept(blockId, handler);
    }

    @Override
    public void fireTrigger(String triggerType, Object playerObj, Object locationObj, Map<String, Object> data) {
        var registry = Main.getInstance().getTriggersRegistry();
        if (registry == null) return;
        if (!(playerObj instanceof Player player)) return;
        if (!(locationObj instanceof Location location)) return;

        List<Trigger> triggers = registry.getTriggersByType(triggerType);
        for (Trigger trigger : triggers) {
            if (!trigger.isEnabled()) continue;
            // For ModuleTriggers, honour the optional cinematicId filter if present in data
            if (trigger instanceof ModuleTrigger moduleTrigger) {
                Object filterObj = moduleTrigger.getParameters().get("cinematicId");
                if (filterObj != null && !filterObj.toString().isEmpty()) {
                    Object dataId = data.get("cinematicId");
                    if (dataId == null || !filterObj.toString().equals(dataId.toString())) continue;
                }
            }
            try {
                trigger.execute(player, location, data);
            } catch (Exception e) {
                Main.getLoggerUtil().severe("[ModuleContext] Error firing trigger '" + triggerType + "': " + e.getMessage());
            }
        }
    }
}
