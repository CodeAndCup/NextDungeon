package fr.perrier.dungeons.spigot.workflow.trigger.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.workflow.validation.JsonValidator;
import fr.perrier.dungeons.spigot.Main;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry for trigger type factories.
 * Implements Open/Closed Principle by allowing new trigger types to be registered at runtime.
 * Replaces the static TRIGGER_CLASSES map in TriggerFactory.
 */
public class TriggerTypeRegistry {
    
    private final Map<String, TriggerTypeFactory> factories = new HashMap<>();
    private final Gson gson;
    
    public TriggerTypeRegistry() {
        this.gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
                .create();
    }
    
    /**
     * Registers a trigger type with a factory.
     * 
     * @param type the trigger type identifier (e.g., "region_trigger")
     * @param factory the factory that creates instances of this trigger type
     */
    public void register(String type, TriggerTypeFactory factory) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Trigger type cannot be null or empty");
        }
        if (factory == null) {
            throw new IllegalArgumentException("Factory cannot be null");
        }
        
        factories.put(type, factory);
        
        if (Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info("Registered trigger type: " + type);
        }
    }
    
    /**
     * Registers a trigger type using Gson deserialization.
     * This is a convenience method for triggers that can be deserialized directly.
     * 
     * @param type the trigger type identifier
     * @param triggerClass the trigger class to deserialize to
     */
    public void registerGsonBased(String type, Class<? extends Trigger> triggerClass) {
        register(type, jsonData -> {
            try {
                return gson.fromJson(jsonData, triggerClass);
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error deserializing trigger of type " + type + ": " + e.getMessage());
                e.printStackTrace(System.err);
                return null;
            }
        });
    }
    
    /**
     * Unregisters a trigger type factory.
     * 
     * @param type the trigger type identifier
     */
    public void unregister(String type) {
        factories.remove(type);
    }
    
    /**
     * Creates a trigger from JSON data using the registered factory for its type.
     * Now includes validation to prevent errors from missing required fields.
     * 
     * @param jsonData the JSON object containing trigger properties (must include "type" field)
     * @return the created Trigger instance, or null if type is unknown or creation fails
     */
    public Trigger createTrigger(JsonObject jsonData) {
        // Validate that JSON and type field exist
        if (jsonData == null) {
            Main.getLoggerUtil().warning("Cannot create trigger: JSON data is null");
            return null;
        }
        
        if (!JsonValidator.hasField(jsonData, "type")) {
            Main.getLoggerUtil().warning("Cannot create trigger: missing 'type' field in JSON");
            return null;
        }
        
        String type = JsonValidator.getString(jsonData, "type", "");
        if (type.isEmpty()) {
            Main.getLoggerUtil().warning("Cannot create trigger: 'type' field is empty");
            return null;
        }
        
        TriggerTypeFactory factory = factories.get(type);
        
        if (factory == null) {
            Main.getLoggerUtil().warning("No factory registered for trigger type: " + type);
            return null;
        }
        
        try {
            Trigger trigger = factory.createFromJson(jsonData);
            
            if (trigger == null) {
                Main.getLoggerUtil().warning("Factory returned null trigger for type: " + type);
                return null;
            }
            
            // Set name if provided
            if (JsonValidator.hasField(jsonData, "name")) {
                trigger.setName(JsonValidator.getString(jsonData, "name", "Trigger_" + System.currentTimeMillis()));
            }
            
            // Set enabled if provided
            if (JsonValidator.hasField(jsonData, "enabled")) {
                trigger.setEnabled(JsonValidator.getBoolean(jsonData, "enabled", true));
            }
            
            return trigger;
        } catch (Exception e) {
            Main.getLoggerUtil().severe("Error creating trigger of type " + type + ": " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }
    
    /**
     * Checks if a trigger type is registered.
     * 
     * @param type the trigger type identifier
     * @return true if a factory is registered for this type
     */
    public boolean isRegistered(String type) {
        return factories.containsKey(type);
    }
    
    /**
     * Gets all registered trigger types.
     * 
     * @return a set of registered trigger type identifiers
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
