package fr.perrier.dungeons.spigot.database;

import com.google.gson.*;
import fr.perrier.dungeons.common.workflow.action.ActionData;
import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.workflow.action.factory.ActionFactory;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializer/Deserializer to convert triggers to and from JSON.
 * Uses Gson with custom adapters to handle polymorphism of triggers and actions.
 */
public class TriggerSerializer {

    // Gson pour la sérialisation de base (sans les adapters custom pour éviter les boucles)
    private static final Gson baseGson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    /**
     * Converts a list of triggers to JSON.
     * @param triggers the list of triggers to serialize
     * @return the JSON string representing the triggers
     */
    public static String serializeTriggers(List<TriggerData> triggers) {
        if (triggers == null) {
            return "[]";
        }

        JsonArray jsonArray = new JsonArray();
        for (TriggerData trigger : triggers) {
            if (trigger != null) {
                JsonElement serialized = serializeTrigger(trigger);
                if (serialized != null) {
                    jsonArray.add(serialized);
                }
            }
        }

        return baseGson.toJson(jsonArray);
    }

    /**
     * Serializes a single trigger to JSON with className/data wrapper.
     */
    private static JsonElement serializeTrigger(TriggerData trigger) {
        JsonObject result = new JsonObject();
        result.addProperty("className", trigger.getClass().getName());

        // Serialize trigger data without custom adapter to avoid infinite loop
        JsonObject data = baseGson.toJsonTree(trigger, trigger.getClass()).getAsJsonObject();

        // Handle actions separately to ensure they have className/data format
        if (trigger.getActions() != null && !trigger.getActions().isEmpty()) {
            JsonArray actionsArray = new JsonArray();
            for (ActionData action : trigger.getActions()) {
                if (action != null) {
                    JsonObject actionWrapper = new JsonObject();
                    actionWrapper.addProperty("className", action.getClass().getName());
                    actionWrapper.add("data", baseGson.toJsonTree(action, action.getClass()));
                    actionsArray.add(actionWrapper);
                }
            }
            data.add("actions", actionsArray);
        }

        result.add("data", data);
        return result;
    }

    /**
     * Converts a JSON string to a list of triggers.
     * @param json the JSON string to deserialize
     * @return the deserialized list of triggers
     */
    public static List<TriggerData> deserializeTriggers(String json) {
        if (json == null || json.trim().isEmpty() || json.equals("[]")) {
            return new ArrayList<>();
        }

        try {
            JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();
            List<TriggerData> triggers = new ArrayList<>();

            for (JsonElement element : jsonArray) {
                try {
                    TriggerData trigger = deserializeTrigger(element);
                    if (trigger != null) {
                        triggers.add(trigger);
                    }
                } catch (Exception e) {
                    Main.getInstance().getLogger().severe("&#FF0000Error deserializing a trigger: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            return triggers;
        } catch (JsonSyntaxException e) {
            Main.getInstance().getLogger().severe("&#FF0000Error deserializing triggers: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Deserializes a single trigger from JSON.
     * Supports both new format (className/data) and legacy format.
     */
    private static TriggerData deserializeTrigger(JsonElement element) {
        if (!element.isJsonObject()) {
            return null;
        }

        JsonObject jsonObject = element.getAsJsonObject();

        // Check for new format with className/data
        JsonElement classNameElement = jsonObject.get("className");
        JsonElement dataElement = jsonObject.get("data");

        if (classNameElement == null || classNameElement.isJsonNull()) {
            Main.getInstance().getLogger().warning("&eInvalid JSON trigger: 'className' field missing or null:\n" + jsonObject);
            return null;
        }

        if (dataElement == null || dataElement.isJsonNull()) {
            Main.getInstance().getLogger().warning("&eInvalid JSON trigger: 'data' field missing or null:\n" + jsonObject);
            return null;
        }

        String className = classNameElement.getAsString();

        try {
            Class<?> clazz = Class.forName(className);
            JsonObject dataObject = dataElement.getAsJsonObject();

            // Extract actions before deserializing trigger
            JsonElement actionsElement = dataObject.remove("actions");

            // Deserialize the trigger without actions first
            Trigger trigger = (Trigger) baseGson.fromJson(dataObject, clazz);

            if (trigger == null) {
                Main.getInstance().getLogger().warning("&eFailed to deserialize trigger: " + className);
                return null;
            }

            // Handle actions manually
            if (actionsElement != null && actionsElement.isJsonArray()) {
                List<ActionData> actions = deserializeActions(actionsElement.getAsJsonArray());
                trigger.setActions(actions);
            } else {
                trigger.setActions(new ArrayList<>());
            }

            return trigger;

        } catch (ClassNotFoundException e) {
            Main.getInstance().getLogger().warning("&eUnknown trigger class: " + className);
            return null;
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("&#FF0000Error deserializing trigger: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Deserializes a list of actions from JSON array.
     * Supports both new format (className/data) and legacy format (type field).
     */
    private static List<ActionData> deserializeActions(JsonArray actionsArray) {
        List<ActionData> actions = new ArrayList<>();

        for (JsonElement actionElement : actionsArray) {
            if (!actionElement.isJsonObject()) {
                continue;
            }

            JsonObject actionObj = actionElement.getAsJsonObject();
            Action action = deserializeAction(actionObj);

            if (action != null) {
                actions.add(action);
            }
        }

        return actions;
    }

    /**
     * Deserializes a single action from JSON.
     * Supports both new format (className/data) and legacy format (type field).
     */
    private static Action deserializeAction(JsonObject actionObj) {
        // Check for new format (className/data)
        JsonElement classNameElement = actionObj.get("className");
        JsonElement dataElement = actionObj.get("data");

        if (classNameElement != null && !classNameElement.isJsonNull()
            && dataElement != null && !dataElement.isJsonNull()) {
            // New format with className/data
            String className = classNameElement.getAsString();
            try {
                Class<?> clazz = Class.forName(className);
                return (Action) baseGson.fromJson(dataElement, clazz);
            } catch (ClassNotFoundException e) {
                Main.getInstance().getLogger().warning("&eUnknown action class: " + className);
                return null;
            } catch (Exception e) {
                Main.getInstance().getLogger().severe("&#FF0000Error deserializing action: " + e.getMessage());
                return null;
            }
        }

        // Legacy format: use "type" field to create action via ActionFactory
        JsonElement typeElement = actionObj.get("type");
        if (typeElement != null && !typeElement.isJsonNull()) {
            try {
                return ActionFactory.createActionFromJson(actionObj);
            } catch (Exception e) {
                Main.getInstance().getLogger().warning("&eError creating action from legacy format: " + e.getMessage());
                return null;
            }
        }

        Main.getInstance().getLogger().warning("&eInvalid JSON action: neither 'className' nor 'type' field found:\n" + actionObj);
        return null;
    }

}
