package fr.perrier.dungeons.common.module;

import java.util.Map;

/**
 * Functional interface for handling the execution of a module-provided action.
 * Modules register handlers for their custom action block IDs.
 */
@FunctionalInterface
public interface ModuleActionHandler {

    /**
     * Execute the module action.
     *
     * @param parameters the action parameters from the workflow JSON
     * @return true if execution was successful
     */
    boolean execute(Map<String, Object> parameters);
}
