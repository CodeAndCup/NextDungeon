package fr.perrier.dungeons.spigot.database;

import com.google.gson.*;
import fr.perrier.dungeons.common.workflow.action.ActionData;
import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.workflow.action.factory.ActionFactory;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Serializer/Deserializer to convert triggers to and from JSON.
 * Uses Gson with custom adapters to handle polymorphism of triggers and actions.
 */
public class TriggerSerializer {

    private static final Gson gson = new GsonBuilder()
            .registerTypeHierarchyAdapter(TriggerData.class, new TriggerTypeAdapter())
            .registerTypeHierarchyAdapter(ActionData.class, new ActionTypeAdapter())
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
                JsonElement serialized = gson.toJsonTree(trigger, Trigger.class);
                jsonArray.add(serialized);
            }
        }

        return gson.toJson(jsonArray);
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
                    TriggerData trigger = gson.fromJson(element, Trigger.class);
                    if (trigger != null) {
                        triggers.add(trigger);
                    }
                } catch (JsonSyntaxException e) {
                    Main.getInstance().getLogger().severe("&cError deserializing a trigger: " + e.getMessage());
                }
            }

            return triggers;
        } catch (JsonSyntaxException e) {
            Main.getInstance().getLogger().severe("&cError deserializing triggers: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Type adapter to handle polymorphism of Triggers.
     */
    private static class TriggerTypeAdapter implements JsonSerializer<TriggerData>, JsonDeserializer<TriggerData> {

        @Override
        public JsonElement serialize(TriggerData src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject result = new JsonObject();
            result.addProperty("className", src.getClass().getName());
            result.add("data", context.serialize(src, src.getClass()));
            return result;
        }

        @Override
        public Trigger deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            // Validation: vérifier que les champs requis existent
            JsonElement classNameElement = jsonObject.get("className");
            JsonElement dataElement = jsonObject.get("data");

            if (classNameElement == null || classNameElement.isJsonNull()) {
                Main.getInstance().getLogger().warning("&eInvalid JSON trigger: 'className' field missing or null");
                return null;
            }

            if (dataElement == null || dataElement.isJsonNull()) {
                Main.getInstance().getLogger().warning("&eInvalid JSON trigger: 'data' field missing or null");
                return null;
            }

            String className = classNameElement.getAsString();

            try {
                Class<?> clazz = Class.forName(className);
                return context.deserialize(dataElement, clazz);
            } catch (ClassNotFoundException e) {
                Main.getInstance().getLogger().warning("&eUnknown trigger class: " + className);
                return null;
            }
        }
    }

    /**
     * Type adapter to handle polymorphism of Actions.
     */
    private static class ActionTypeAdapter implements JsonSerializer<ActionData>, JsonDeserializer<ActionData> {

        @Override
        public JsonElement serialize(ActionData src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject result = new JsonObject();
            result.addProperty("className", src.getClass().getName());
            result.add("data", context.serialize(src, src.getClass()));
            return result;
        }

        @Override
        public Action deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            // Vérifier si c'est le nouveau format (avec className/data)
            JsonElement classNameElement = jsonObject.get("className");
            JsonElement dataElement = jsonObject.get("data");

            if (classNameElement != null && !classNameElement.isJsonNull()
                && dataElement != null && !dataElement.isJsonNull()) {
                // Nouveau format avec className/data
                String className = classNameElement.getAsString();
                try {
                    Class<?> clazz = Class.forName(className);
                    return context.deserialize(dataElement, clazz);
                } catch (ClassNotFoundException e) {
                    Main.getInstance().getLogger().warning("&eClass of action unknown: " + className);
                    return null;
                }
            }

            // Format legacy : utiliser le champ "type" pour recréer l'action via ActionFactory
            JsonElement typeElement = jsonObject.get("type");
            if (typeElement != null && !typeElement.isJsonNull()) {
                String type = typeElement.getAsString();
                Main.getInstance().getLogger().info("Deserializing legacy action format with type: " + type);
                try {
                    return ActionFactory.createActionFromJson(jsonObject);
                } catch (Exception e) {
                    Main.getInstance().getLogger().warning("&eError creating action from legacy format: " + e.getMessage());
                    return null;
                }
            }

            Main.getInstance().getLogger().warning("&eInvalid JSON action: neither 'className' nor 'type' field found");
            return null;
        }
    }
}
