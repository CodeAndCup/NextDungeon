package fr.perrier.dungeons.spigot.database;

import com.google.gson.*;
import fr.perrier.dungeons.common.workflow.action.ActionData;
import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Sérialiseur/Désérialiseur pour convertir les triggers en JSON et vice-versa.
 * Utilise Gson avec des adaptateurs personnalisés pour gérer la polymorphie des triggers et actions.
 */
public class TriggerSerializer {

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Trigger.class, new TriggerTypeAdapter())
            .registerTypeAdapter(Action.class, new ActionTypeAdapter())
            .setPrettyPrinting()
            .create();

    /**
     * Convertit une liste de triggers en JSON
     * @param triggers la liste de triggers à sérialiser
     * @return la chaîne JSON représentant les triggers
     */
    public static String serializeTriggers(List<TriggerData> triggers) {
        if (triggers == null) {
            return "[]";
        }

        // Forcer la sérialisation avec le type Trigger pour que l'adaptateur soit utilisé
        JsonArray jsonArray = new JsonArray();
        for (TriggerData trigger : triggers) {
            if (trigger != null) {
                JsonElement serialized = gson.toJsonTree(trigger, TriggerData.class);
                jsonArray.add(serialized);
            }
        }

        return gson.toJson(jsonArray);
    }

    /**
     * Convertit une chaîne JSON en liste de triggers
     * @param json la chaîne JSON à désérialiser
     * @return la liste de triggers désérialisée
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
     * Adaptateur de type pour gérer la polymorphie des Triggers
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
     * Adaptateur de type pour gérer la polymorphie des Actions
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

            // Validation: vérifier que les champs requis existent
            JsonElement classNameElement = jsonObject.get("className");
            JsonElement dataElement = jsonObject.get("data");

            if (classNameElement == null || classNameElement.isJsonNull()) {
                Main.getInstance().getLogger().warning("&eInvalid JSON action: 'className' field missing or null");
                return null;
            }

            if (dataElement == null || dataElement.isJsonNull()) {
                Main.getInstance().getLogger().warning("&eInvalid JSON action: missing or null 'data' field");
                return null;
            }

            String className = classNameElement.getAsString();

            try {
                Class<?> clazz = Class.forName(className);
                return context.deserialize(dataElement, clazz);
            } catch (ClassNotFoundException e) {
                Main.getInstance().getLogger().warning("&eClass of action unknown: " + className);
                return null;
            }
        }
    }
}
