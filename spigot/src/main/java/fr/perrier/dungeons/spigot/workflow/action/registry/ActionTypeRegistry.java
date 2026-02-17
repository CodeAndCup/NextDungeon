package fr.perrier.dungeons.spigot.workflow.action.registry;

import com.google.gson.JsonObject;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.workflow.validation.JsonValidator;
import fr.perrier.dungeons.spigot.Main;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry for action type factories.
 * Implements Open/Closed Principle by allowing new action types to be registered at runtime.
 * Replaces the monolithic switch statement in ActionFactory.
 */
public class ActionTypeRegistry {
    
    private final Map<String, ActionTypeFactory> factories = new HashMap<>();
    
    /**
     * Registers an action type factory.
     * 
     * @param type the action type identifier (e.g., "send_message_action")
     * @param factory the factory that creates instances of this action type
     */
    public void register(String type, ActionTypeFactory factory) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Action type cannot be null or empty");
        }
        if (factory == null) {
            throw new IllegalArgumentException("Factory cannot be null");
        }
        
        factories.put(type, factory);
        
        if (Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info("Registered action type: " + type);
        }
    }
    
    /**
     * Unregisters an action type factory.
     * 
     * @param type the action type identifier
     */
    public void unregister(String type) {
        factories.remove(type);
    }
    
    /**
     * Creates an action from JSON data using the registered factory for its type.
     * Now includes validation to prevent errors from missing required fields.
     * 
     * @param jsonData the JSON object containing action properties (must include "type" field)
     * @return the created Action instance, or null if type is unknown or creation fails
     */
    public Action createAction(JsonObject jsonData) {
        // Validate that JSON and type field exist
        if (jsonData == null) {
            Main.getLoggerUtil().warning("Cannot create action: JSON data is null");
            return null;
        }
        
        if (!JsonValidator.hasField(jsonData, "type")) {
            Main.getLoggerUtil().warning("Cannot create action: missing 'type' field in JSON");
            return null;
        }
        
        String type = JsonValidator.getString(jsonData, "type", "");
        if (type.isEmpty()) {
            Main.getLoggerUtil().warning("Cannot create action: 'type' field is empty");
            return null;
        }
        
        ActionTypeFactory factory = factories.get(type);
        
        if (factory == null) {
            Main.getLoggerUtil().warning("No factory registered for action type: " + type);
            return null;
        }
        
        try {
            return factory.createFromJson(jsonData);
        } catch (Exception e) {
            Main.getLoggerUtil().severe("Error creating action of type " + type + ": " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }
    
    /**
     * Checks if an action type is registered.
     * 
     * @param type the action type identifier
     * @return true if a factory is registered for this type
     */
    public boolean isRegistered(String type) {
        return factories.containsKey(type);
    }
    
    /**
     * Gets all registered action types.
     * 
     * @return a set of registered action type identifiers
     */
    public Set<String> getRegisteredTypes() {
        return factories.keySet();
    }
    
    /**
     * Clears all registered factories.
     */
    public void clear() {
        factories.clear();
    }
}
