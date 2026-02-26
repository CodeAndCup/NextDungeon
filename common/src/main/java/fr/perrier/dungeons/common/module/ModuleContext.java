package fr.perrier.dungeons.common.module;

import java.util.Map;

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

    /**
     * Fire all registered triggers of the given type for the given player.
     * Modules use this to fire their own trigger types (e.g. "cinematic_on_end")
     * without depending on spigot internals.
     *
     * <p>The {@code player} and {@code location} are passed as {@link Object} to avoid
     * a Bukkit dependency in the common module. Implementations cast them to
     * {@code org.bukkit.entity.Player} and {@code org.bukkit.Location} respectively.</p>
     *
     * @param triggerType the trigger type string (e.g. "cinematic_on_end")
     * @param player      the player who triggered the event (cast to Player in impl)
     * @param location    the location of the event (cast to Location in impl)
     * @param data        additional context data passed to the trigger
     */
    void fireTrigger(String triggerType, Object player, Object location, Map<String, Object> data);
}
