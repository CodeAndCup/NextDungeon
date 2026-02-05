package fr.perrier.dungeons.spigot.workflow.trigger.factory;

import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.workflow.action.factory.ActionFactory;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.*;
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
 * ARCHITECTURE: Uses automatic Gson deserialization for all trigger types
 * - Trigger types are mapped to their concrete classes
 * - Gson automatically populates all fields from JSON, including PRIVATE fields
 * - No manual property copying needed for new trigger types
 * - Simply add new Trigger class to the map
 * - CRITICAL: Gson is configured to access private fields used in all Trigger classes
 */
public class TriggerFactory {
    private static final Gson gson = new GsonBuilder()
            .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
            .create();

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

            if (Main.isDebug()) {
                Main.getInstance().getLogger().info("Creating trigger: " + type + " - " + name);
            }

            // Get the concrete class for this trigger type
            Class<? extends Trigger> triggerClass = TRIGGER_CLASSES.get(type);
            if (triggerClass == null) {
                Main.getInstance().getLogger().warning("&eTrigger type unknown: " + type);
                return null;
            }

            // AUTOMATIC DESERIALIZATION: Gson automatically populates all fields
            Trigger trigger = gson.fromJson(triggerData, triggerClass);

            if (trigger == null) {
                Main.getInstance().getLogger().warning("&eFailed to deserialize trigger of type: " + type);
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

            if (Main.isDebug()) {
                Main.getInstance().getLogger().info("Trigger created successfully: " + trigger.getName() + " with " + trigger.getActions().size() + " action(s)");
            }

            return trigger;

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("&#FF0000Error creating trigger from JSON: " + e.getMessage());
            e.printStackTrace();
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

        if (Main.isDebug()) {
            Main.getInstance().getLogger().info("Parsed " + triggers.size() + " trigger(s) from JSON");
        }

        return triggers;
    }
}
