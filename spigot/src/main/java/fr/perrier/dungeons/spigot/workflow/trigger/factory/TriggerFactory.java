package fr.perrier.dungeons.spigot.workflow.trigger.factory;

import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.workflow.action.factory.ActionFactory;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.*;
import fr.perrier.dungeons.spigot.Main;
import com.google.gson.*;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Factory pour créer des triggers depuis les données JSON de Blockly
 *
 * ARCHITECTURE: Uses automatic Gson deserialization for all trigger types
 * - Trigger types are mapped to their concrete classes
 * - Gson automatically populates all fields from JSON, including PRIVATE fields
 * - No manual property copying needed for new trigger types
 * - Simply add new Trigger class to the map
 * - CRITICAL: Gson is configured to access private fields used in all Trigger classes
 * - IMPORTANT: Custom JsonDeserializer ensures triggerId is initialized with UUID.randomUUID() if not in JSON
 */
public class TriggerFactory {

    /**
     * Custom JsonDeserializer that ensures triggerId is initialized
     * Solves the problem where Gson bypasses constructors during deserialization
     */
    private static class TriggerDeserializer<T extends Trigger> implements JsonDeserializer<T> {
        private final Class<T> triggerClass;
        private final Gson defaultGson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
                .create();

        public TriggerDeserializer(Class<T> triggerClass) {
            this.triggerClass = triggerClass;
        }

        @Override
        public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                JsonObject jsonObject = json.getAsJsonObject();

                // First, deserialize normally using default Gson
                T trigger = defaultGson.fromJson(json, triggerClass);

                // Check if triggerId is null (not in JSON or Gson bypassed constructor)
                if (trigger.getTriggerId() == null) {
                    // Use reflection to set the triggerId field
                    java.lang.reflect.Field triggerIdField = TriggerData.class.getDeclaredField("triggerId");
                    triggerIdField.setAccessible(true);
                    triggerIdField.set(trigger, UUID.randomUUID());

                    if (Main.getLoggerUtil().isDebugEnabled()) {
                        Main.getLoggerUtil().info("Generated new UUID for trigger: " + trigger.getTriggerId());
                    }
                }

                // Check if enabled is not in JSON, set it to true (default value from constructor)
                if (!jsonObject.has("enabled")) {
                    java.lang.reflect.Field enabledField = TriggerData.class.getDeclaredField("enabled");
                    enabledField.setAccessible(true);
                    enabledField.set(trigger, true);

                    if (Main.getLoggerUtil().isDebugEnabled()) {
                        Main.getLoggerUtil().info("Set enabled=true by default for trigger: " + trigger.getName());
                    }
                }

                return trigger;
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error deserializing trigger: " + e.getMessage());
                e.printStackTrace(System.err);
                throw new JsonParseException("Failed to deserialize trigger", e);
            }
        }
    }

    // Map trigger type strings to their concrete class
    private static final Map<String, Class<? extends Trigger>> TRIGGER_CLASSES = new HashMap<>();

    static {
        TRIGGER_CLASSES.put("region_trigger", RegionTrigger.class);
        TRIGGER_CLASSES.put("function_trigger", FunctionTrigger.class);
        TRIGGER_CLASSES.put("entity_death_trigger", EntityDeathTrigger.class);
        TRIGGER_CLASSES.put("block_click_trigger", BlockClickTrigger.class);
        TRIGGER_CLASSES.put("player_damage_trigger", PlayerDamageTrigger.class);
        TRIGGER_CLASSES.put("item_pickup_trigger", ItemPickupTrigger.class);
        TRIGGER_CLASSES.put("chat_message_trigger", ChatMessageTrigger.class);
        TRIGGER_CLASSES.put("player_jump_trigger", PlayerJumpTrigger.class);
    }

    /**
     * Register a new trigger type dynamically (used by dynamic modules).
     *
     * @param type         the trigger type string identifier
     * @param triggerClass the concrete trigger class
     */
    public static void registerTriggerType(String type, Class<? extends Trigger> triggerClass) {
        TRIGGER_CLASSES.put(type, triggerClass);
    }

    private static final Gson gson = createGsonInstance();

    /**
     * Creates a Gson instance configured with custom deserializers for all trigger types
     * This ensures triggerId is properly initialized during deserialization
     */
    private static Gson createGsonInstance() {
        GsonBuilder builder = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC);

        // Register JsonDeserializer for each trigger type to ensure proper UUID initialization
        for (Class<? extends Trigger> triggerClass : TRIGGER_CLASSES.values()) {
            builder.registerTypeAdapter(triggerClass, new TriggerDeserializer<>(triggerClass));
        }

        return builder.create();
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
        try {
            String type = triggerData.get("type").getAsString();
            String name = triggerData.has("name") ? triggerData.get("name").getAsString() : "Trigger_" + System.currentTimeMillis();

            if (Main.getLoggerUtil().isDebugEnabled()) {
                Main.getLoggerUtil().info("Creating trigger: " + type + " - " + name);
            }

            // Get the concrete class for this trigger type
            Class<? extends Trigger> triggerClass = TRIGGER_CLASSES.get(type);
            if (triggerClass == null) {
                // Check if a dynamic module provides a trigger descriptor for this type
                Trigger moduleTrigger = tryCreateModuleTrigger(type, name, triggerData);
                if (moduleTrigger != null) {
                    return moduleTrigger;
                }
                Main.getLoggerUtil().warning("Trigger type unknown: " + type);
                return null;
            }

            // AUTOMATIC DESERIALIZATION: Gson automatically populates all fields
            Trigger trigger = gson.fromJson(triggerData, triggerClass);

            if (trigger == null) {
                Main.getLoggerUtil().warning("Failed to deserialize trigger of type: " + type);
                return null;
            }

            // Set name (might not be in JSON)
            trigger.setName(name);

            // Apply common properties
            if (triggerData.has("enabled")) {
                trigger.setEnabled(triggerData.get("enabled").getAsBoolean());
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
     * Attempts to create a ModuleTrigger from JSON if the type matches a module trigger descriptor.
     */
    private static Trigger tryCreateModuleTrigger(String type, String name, JsonObject triggerData) {
        if (Main.getInstance().getModuleLoader() == null) return null;

        fr.perrier.dungeons.common.module.ModuleBlockDescriptor descriptor =
                Main.getInstance().getModuleLoader().getBlockRegistry().getBlock(type);
        if (descriptor == null) {
            // Also try with dots → underscores conversion
            descriptor = Main.getInstance().getModuleLoader().getBlockRegistry().getBlock(type.replace('_', '.'));
        }
        if (descriptor == null || descriptor.getType() != fr.perrier.dungeons.common.module.ModuleBlockDescriptor.BlockType.TRIGGER) {
            return null;
        }

        // Extract parameters from JSON (everything except type, name, actions, enabled)
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, JsonElement> entry : triggerData.entrySet()) {
            String key = entry.getKey();
            if ("type".equals(key) || "name".equals(key) || "actions".equals(key) || "enabled".equals(key)) {
                continue;
            }
            JsonElement val = entry.getValue();
            if (val.isJsonPrimitive()) {
                if (val.getAsJsonPrimitive().isString()) {
                    params.put(key, val.getAsString());
                } else if (val.getAsJsonPrimitive().isNumber()) {
                    params.put(key, val.getAsNumber());
                } else if (val.getAsJsonPrimitive().isBoolean()) {
                    params.put(key, val.getAsBoolean());
                }
            }
        }

        ModuleTrigger trigger = new ModuleTrigger(name, type, params);
        trigger.setEnabled(!triggerData.has("enabled") || triggerData.get("enabled").getAsBoolean());

        // Parse actions
        if (triggerData.has("actions")) {
            JsonArray actionsArray = triggerData.getAsJsonArray("actions");
            trigger.setActions(ActionFactory.parseActionsFromJson(actionsArray));
        }

        if (Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info("ModuleTrigger created: " + trigger.getName() + " (" + type + ") with "
                    + trigger.getActions().size() + " action(s)");
        }

        return trigger;
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
