package fr.perrier.dungeons.common.module;

/**
 * Context passed to modules during initialization.
 * Provides access to the block registry for registering
 * custom triggers, actions, and conditions.
 */
public interface ModuleContext {

    /**
     * @return the module block registry for registering dynamic blocks
     */
    ModuleBlockRegistry getBlockRegistry();
}
