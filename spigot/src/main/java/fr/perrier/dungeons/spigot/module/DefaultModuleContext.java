package fr.perrier.dungeons.spigot.module;

import fr.perrier.dungeons.common.module.ModuleBlockRegistry;
import fr.perrier.dungeons.common.module.ModuleContext;

/**
 * Default implementation of ModuleContext provided to modules at load time.
 */
public class DefaultModuleContext implements ModuleContext {

    private final ModuleBlockRegistry blockRegistry;

    public DefaultModuleContext(ModuleBlockRegistry blockRegistry) {
        this.blockRegistry = blockRegistry;
    }

    @Override
    public ModuleBlockRegistry getBlockRegistry() {
        return blockRegistry;
    }
}
