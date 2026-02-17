package fr.perrier.dungeons.spigot.workflow.action.registry;

import com.google.gson.JsonObject;
import fr.perrier.dungeons.spigot.workflow.action.Action;

/**
 * Interface for action type factories.
 * Enables Open/Closed Principle by allowing new action types to be registered without modifying existing code.
 */
@FunctionalInterface
public interface ActionTypeFactory {
    
    /**
     * Creates an action instance from JSON data.
     * 
     * @param jsonData the JSON object containing action properties
     * @return the created Action instance, or null if creation fails
     */
    Action createFromJson(JsonObject jsonData);
}
