package fr.perrier.dungeons.common.module;

/**
 * Context passed to modules during initialization.
 * Provides access to the block registry for registering
 * custom triggers, actions, and conditions, and to the
 * action handler registry for registering execution handlers.
 */
public interface ModuleContext {

    /**
     * @return the module block registry for registering dynamic blocks
     */
    ModuleBlockRegistry getBlockRegistry();

    /**
     * Register an execution handler for a module action block.
     *
     * @param blockId the action block ID (e.g. "cinematic_start")
     * @param handler the handler to invoke when this action is executed
     */
    void registerActionHandler(String blockId, ModuleActionHandler handler);
}
