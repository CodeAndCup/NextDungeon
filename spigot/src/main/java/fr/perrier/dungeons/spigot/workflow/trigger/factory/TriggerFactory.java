package fr.perrier.dungeons.spigot.workflow.trigger.factory;

import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.workflow.action.factory.ActionFactory;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.*;
import fr.perrier.dungeons.spigot.workflow.trigger.registry.TriggerTypeRegistry;
import fr.perrier.dungeons.spigot.Main;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory pour créer des triggers depuis les données JSON de Blockly
 *
 * ARCHITECTURE CHANGE: Now uses TriggerTypeRegistry for extensibility following Open/Closed Principle.
 * - Trigger types can be registered at runtime without modifying this class
 * - Gson automatically populates all fields from JSON
 * - Simply add new Trigger class to the registry
 * - CRITICAL: Gson is configured to access private fields used in all Trigger classes
 */
public class TriggerFactory {
    private static final Gson gson = new GsonBuilder()
            .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
            .create();

    private static final TriggerTypeRegistry registry = new TriggerTypeRegistry();
    private static boolean registryInitialized = false;
    
    /**
     * Initialize the trigger type registry with all standard trigger types.
     * This method is called automatically on first use.
     */
    private static synchronized void initializeRegistry() {
        if (registryInitialized) {
            return;
        }
        
        registerStandardTriggers();
        registryInitialized = true;
    }
    
    /**
     * Register all standard trigger types in the registry.
     * Uses Gson-based deserialization for all standard triggers.
     */
    private static void registerStandardTriggers() {
        registry.registerGsonBased("region_trigger", RegionTrigger.class);
        registry.registerGsonBased("function_trigger", FunctionTrigger.class);
        registry.registerGsonBased("entity_death_trigger", EntityDeathTrigger.class);
        registry.registerGsonBased("block_click_trigger", BlockClickTrigger.class);
        registry.registerGsonBased("player_damage_trigger", PlayerDamageTrigger.class);
        registry.registerGsonBased("item_pickup_trigger", ItemPickupTrigger.class);
        registry.registerGsonBased("chat_message_trigger", ChatMessageTrigger.class);
        registry.registerGsonBased("player_jump_trigger", PlayerJumpTrigger.class);
    }
    
    /**
     * Gets the trigger type registry for external registration of new trigger types.
     * This allows plugins to extend the system without modifying this class (Open/Closed Principle).
     * 
     * @return the trigger type registry
     */
    public static TriggerTypeRegistry getRegistry() {
        initializeRegistry();
        return registry;
    }

    /**
     * Creates a trigger from JSON data
     *
     * AUTOMATIC DESERIALIZATION: All properties are automatically populated
     * from the JSON object using Gson, regardless of the trigger type.
     *
     * @param triggerData JSON object containing trigger data
     * @return Deserialized Trigger object, or null if failed
     */
    public static Trigger createTriggerFromJson(JsonObject triggerData) {
        initializeRegistry();
        
        try {
            String type = triggerData.get("type").getAsString();
            String name = triggerData.has("name") ? triggerData.get("name").getAsString() : "Trigger_" + System.currentTimeMillis();

            if (Main.getLoggerUtil().isDebugEnabled()) {
                Main.getLoggerUtil().info("Creating trigger: " + type + " - " + name);
            }

            // Use registry to create trigger
            Trigger trigger = registry.createTrigger(triggerData);
            
            if (trigger == null) {
                Main.getLoggerUtil().warning("Failed to create trigger of type: " + type);
                return null;
            }

            // Parse actions
            if (triggerData.has("actions")) {
                JsonArray actionsArray = triggerData.getAsJsonArray("actions");
                trigger.setActions(ActionFactory.parseActionsFromJson(actionsArray));
            }

            if (Main.getLoggerUtil().isDebugEnabled()) {
                Main.getLoggerUtil().info("Trigger created successfully: " + trigger.getName() + " with " + trigger.getActions().size() + " action(s)");
            }

            return trigger;

        } catch (Exception e) {
            Main.getLoggerUtil().severe("Error creating trigger from JSON: " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
    }

    /**
     * Parses a list of triggers from JSON array
     * @param triggersArray JSON array containing trigger objects
     * @return List of deserialized Trigger objects
     */
    public static List<TriggerData> parseTriggersFromJson(JsonArray triggersArray) {
        List<TriggerData> triggers = new ArrayList<>();

        for (JsonElement element : triggersArray) {
            if (element.isJsonObject()) {
                Trigger trigger = createTriggerFromJson(element.getAsJsonObject());
                if (trigger != null) {
                    triggers.add(trigger);

                    // Register function trigger if applicable
                    if(trigger instanceof FunctionTrigger functionTrigger) {
                        functionTrigger.registerFunction();
                    }
                }
            }
        }

        if (Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info("Parsed " + triggers.size() + " trigger(s) from JSON");
        }

        return triggers;
    }
}
