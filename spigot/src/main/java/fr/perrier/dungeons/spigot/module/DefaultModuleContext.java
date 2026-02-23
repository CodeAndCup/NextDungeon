package fr.perrier.dungeons.spigot.module;

import fr.perrier.dungeons.common.module.ModuleActionHandler;
import fr.perrier.dungeons.common.module.ModuleBlockRegistry;
import fr.perrier.dungeons.common.module.ModuleContext;

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
}
