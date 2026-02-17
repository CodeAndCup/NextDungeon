package fr.perrier.dungeons.spigot.workflow.trigger.registry;

import com.google.gson.JsonObject;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;

/**
 * Interface for trigger type factories.
 * Enables Open/Closed Principle by allowing new trigger types to be registered without modifying existing code.
 */
@FunctionalInterface
public interface TriggerTypeFactory {
    
    /**
     * Creates a trigger instance from JSON data.
     * 
     * @param jsonData the JSON object containing trigger properties
     * @return the created Trigger instance, or null if creation fails
     */
    Trigger createFromJson(JsonObject jsonData);
}
