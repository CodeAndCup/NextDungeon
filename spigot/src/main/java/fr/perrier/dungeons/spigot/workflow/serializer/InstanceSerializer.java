package fr.perrier.dungeons.spigot.workflow.serializer;

import com.google.gson.*;
import fr.perrier.dungeons.common.workflow.action.ActionData;
import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.workflow.action.factory.ActionFactory;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.workflow.trigger.factory.TriggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Serializer/Deserializer to convert triggers to and from JSON.
 * Uses Gson with custom adapters to handle polymorphism of triggers and actions.
 */
public class InstanceSerializer {

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
                        if (Main.getLoggerUtil() != null && Main.getLoggerUtil().isDebugEnabled()) {
                            Main.getLoggerUtil().info("Deserialized trigger: " + trigger.getName() + " of type: " + trigger.getClass().getName());
                            Main.getLoggerUtil().info("Trigger data: " + baseGson.toJson(trigger));
                        }
                        triggers.add(trigger);
                    }
                } catch (Exception e) {
                    if (Main.getLoggerUtil() != null) {
                        Main.getLoggerUtil().severe("Error deserializing a trigger: " + e.getMessage());
                    } else {
                        System.err.println("Error deserializing a trigger: " + e.getMessage());
                    }
                    e.printStackTrace(System.err);
                }
            }

            return triggers;
        } catch (JsonSyntaxException e) {
            if (Main.getLoggerUtil() != null) {
                Main.getLoggerUtil().severe("Error deserializing triggers: " + e.getMessage());
            } else {
                System.err.println("Error deserializing triggers: " + e.getMessage());
            }
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
            if(Main.getLoggerUtil() != null) {
                Main.getLoggerUtil().warning("Trigger JSON missing 'className' field, attempting legacy deserialization:\n" + jsonObject);
            } else {
                System.err.println("Trigger JSON missing 'className' field, attempting legacy deserialization:\n" + jsonObject);
            }
            return null;
        }

        if (dataElement == null || dataElement.isJsonNull()) {
            if(Main.getLoggerUtil() != null) {
                Main.getLoggerUtil().warning("Trigger JSON missing 'data' field, cannot deserialize:\n" + jsonObject);
            } else {
                System.err.println("Trigger JSON missing 'data' field, cannot deserialize:\n" + jsonObject);
            }
            return null;
        }

        String className = classNameElement.getAsString();

        // Special handling for ModuleTrigger: reconstruct via TriggerFactory
        // so module block descriptor lookups work properly
        if (className.equals("fr.perrier.dungeons.spigot.workflow.trigger.impl.ModuleTrigger")) {
            JsonObject data = dataElement.getAsJsonObject();
            if (data.has("type")) {
                try {
                    // Build a flat trigger JSON for TriggerFactory
                    JsonObject flatTrigger = new JsonObject();
                    flatTrigger.addProperty("type", data.get("type").getAsString());
                    if (data.has("name")) flatTrigger.addProperty("name", data.get("name").getAsString());
                    if (data.has("enabled")) flatTrigger.add("enabled", data.get("enabled"));
                    // Copy parameters from the nested "parameters" map
                    if (data.has("parameters") && data.get("parameters").isJsonObject()) {
                        for (Map.Entry<String, JsonElement> entry : data.getAsJsonObject("parameters").entrySet()) {
                            flatTrigger.add(entry.getKey(), entry.getValue());
                        }
                    }
                    // Attach actions with the format-aware deserializer below rather than
                    // letting TriggerFactory's flat parser handle them — that parser only
                    // understands the legacy "type" action format and throws a NPE on the
                    // newer "className/data" format that the editor now saves.
                    Trigger moduleTrigger = TriggerFactory.createTriggerFromJson(flatTrigger);
                    if (moduleTrigger != null && data.has("actions") && data.get("actions").isJsonArray()) {
                        moduleTrigger.setActions(deserializeActions(data.getAsJsonArray("actions")));
                    }
                    return moduleTrigger;
                } catch (Exception e) {
                    if(Main.getLoggerUtil() != null) {
                        Main.getLoggerUtil().warning("Error recreating ModuleTrigger: " + e.getMessage());
                    } else {
                        System.err.println("Error recreating ModuleTrigger: " + e.getMessage());
                    }
                    return null;
                }
            }
        }

        try {
            Class<?> clazz = Class.forName(className);
            JsonObject dataObject = dataElement.getAsJsonObject();

            // Extract actions before deserializing trigger
            JsonElement actionsElement = dataObject.remove("actions");

            // Deserialize the trigger without actions first
            Trigger trigger = (Trigger) baseGson.fromJson(dataObject, clazz);

            if (trigger == null) {
                if(Main.getLoggerUtil() != null) {
                    Main.getLoggerUtil().warning("Failed to deserialize trigger (null result): " + className);
                } else {
                    System.err.println("Failed to deserialize trigger (null result): " + className);
                }
                return null;
            }

            // Handle actions manually
            if (actionsElement != null && actionsElement.isJsonArray()) {
                List<ActionData> actions = deserializeActions(actionsElement.getAsJsonArray());
                trigger.setActions(actions);
                if(Main.getLoggerUtil() != null && Main.getLoggerUtil().isDebugEnabled()) {
                    Main.getLoggerUtil().info("Deserialized " + actions.size() + " actions for trigger: " + trigger.getName());
                    Main.getLoggerUtil().info("Trigger: " + baseGson.toJson(trigger));
                }
            } else {
                trigger.setActions(new ArrayList<>());
            }

            return trigger;

        } catch (ClassNotFoundException e) {
            if(Main.getLoggerUtil() != null) {
                Main.getLoggerUtil().warning("Unknown trigger class: " + className);
            } else {
                System.err.println("Unknown trigger class: " + className);
            }
            return null;
        } catch (Exception e) {
            if(Main.getLoggerUtil() != null) {
                Main.getLoggerUtil().severe("Error deserializing trigger: " + e.getMessage());
            } else {
                System.err.println("Error deserializing trigger: " + e.getMessage());
            }
            e.printStackTrace(System.err);
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
                if(Main.getLoggerUtil() != null && Main.getLoggerUtil().isDebugEnabled()) {
                    Main.getLoggerUtil().info("Deserialized action: " + action.getClass().getName());
                    Main.getLoggerUtil().info("Action data: " + baseGson.toJson(action));
                }
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

            // Special handling for ModuleAction: use ActionFactory to properly recreate
            // (Gson alone can't reconstruct the transient handler field)
            if (className.equals("fr.perrier.dungeons.spigot.workflow.action.impl.ModuleAction")) {
                JsonObject data = dataElement.getAsJsonObject();
                if (data.has("type")) {
                    try {
                        // Reconstruct the action JSON in flat format for ActionFactory
                        JsonObject flatAction = new JsonObject();
                        flatAction.addProperty("type", data.get("type").getAsString());
                        if (data.has("name")) flatAction.addProperty("name", data.get("name").getAsString());
                        // Copy parameters from the nested "parameters" map
                        if (data.has("parameters") && data.get("parameters").isJsonObject()) {
                            for (java.util.Map.Entry<String, JsonElement> entry : data.getAsJsonObject("parameters").entrySet()) {
                                flatAction.add(entry.getKey(), entry.getValue());
                            }
                        }
                        return ActionFactory.createActionFromJson(flatAction);
                    } catch (Exception e) {
                        if(Main.getLoggerUtil() != null) {
                            Main.getLoggerUtil().warning("Error recreating ModuleAction: " + e.getMessage());
                        } else {
                            System.err.println("Error recreating ModuleAction: " + e.getMessage());
                        }
                        return null;
                    }
                }
            }

            // Container actions (IfCondition, ForLoopAction, conditions with branches) hold
            // List<Action> fields. Since Action is abstract, plain Gson cannot instantiate the
            // nested entries (stored in legacy/flat format with a "type" field) and throws,
            // dropping the whole action. Route them through ActionFactory which rebuilds
            // nested actions recursively.
            JsonObject data = dataElement.getAsJsonObject();
            if (data.has("type")
                    && (data.has("ifActions") || data.has("elseActions") || data.has("loopActions"))) {
                return ActionFactory.createActionFromJson(data);
            }

            try {
                Class<?> clazz = Class.forName(className);
                return (Action) baseGson.fromJson(dataElement, clazz);
            } catch (ClassNotFoundException e) {
                if(Main.getLoggerUtil() != null) {
                    Main.getLoggerUtil().warning("Unknown action class: " + className);
                } else {
                    System.err.println("Unknown action class: " + className);
                }
                return null;
            } catch (Exception e) {
                if(Main.getLoggerUtil() != null) {
                    Main.getLoggerUtil().severe("Error deserializing action: " + e.getMessage());
                } else {
                    System.err.println("Error deserializing action: " + e.getMessage());
                }
                return null;
            }
        }

        // Legacy format: use "type" field to create action via ActionFactory
        JsonElement typeElement = actionObj.get("type");
        if (typeElement != null && !typeElement.isJsonNull()) {
            try {
                return ActionFactory.createActionFromJson(actionObj);
            } catch (Exception e) {
                if(Main.getLoggerUtil() != null) {
                    Main.getLoggerUtil().warning("Error creating action from legacy format: " + e.getMessage());
                } else {
                    System.err.println("Error creating action from legacy format: " + e.getMessage());
                }
                return null;
            }
        }

        if(Main.getLoggerUtil() != null) {
            Main.getLoggerUtil().warning("Invalid JSON action: neither 'className' nor 'type' field found:\n" + actionObj);
        } else {
            System.err.println("Invalid JSON action: neither 'className' nor 'type' field found:\n" + actionObj);
        }
        return null;
    }

}
