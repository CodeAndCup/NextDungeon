package fr.perrier.dungeons.spigot.workflow.context;

import fr.perrier.dungeons.spigot.workflow.registry.TriggersRegistry;
import fr.perrier.dungeons.spigot.workflow.registry.VariableRegistry;

/**
 * Interface providing access to workflow-related services.
 * This enables dependency injection and follows the Dependency Inversion Principle.
 * 
 * Implementations provide access to registries without tight coupling to Main singleton.
 */
public interface WorkflowContext {
    
    /**
     * Gets the triggers registry for managing trigger registration and execution.
     * @return the triggers registry
     */
    TriggersRegistry getTriggersRegistry();
    
    /**
     * Gets the variable registry for managing workflow variables.
     * @return the variable registry
     */
    VariableRegistry getVariableRegistry();
    
    /**
     * Checks if debug logging is enabled.
     * @return true if debug mode is enabled
     */
    boolean isDebugEnabled();
    
    /**
     * Logs an info message.
     * @param message the message to log
     */
    void logInfo(String message);
    
    /**
     * Logs a warning message.
     * @param message the message to log
     */
    void logWarning(String message);
    
    /**
     * Logs a severe error message.
     * @param message the message to log
     */
    void logSevere(String message);
}
