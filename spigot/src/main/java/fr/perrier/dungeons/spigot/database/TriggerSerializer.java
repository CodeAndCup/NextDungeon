package fr.perrier.dungeons.spigot.database;

import com.google.gson.*;
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
    public static String serializeTriggers(List<Trigger> triggers) {
        if (triggers == null) {
            return "[]";
        }
        return gson.toJson(triggers);
    }

    /**
     * Convertit une chaîne JSON en liste de triggers
     * @param json la chaîne JSON à désérialiser
     * @return la liste de triggers désérialisée
     */
    public static List<Trigger> deserializeTriggers(String json) {
        if (json == null || json.trim().isEmpty() || json.equals("[]")) {
            return new ArrayList<>();
        }

        try {
            JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();
            List<Trigger> triggers = new ArrayList<>();

            for (JsonElement element : jsonArray) {
                try {
                    Trigger trigger = gson.fromJson(element, Trigger.class);
                    if (trigger != null) {
                        triggers.add(trigger);
                    }
                } catch (JsonSyntaxException e) {
                    Main.getInstance().getLogger().warning("Erreur lors de la désérialisation d'un trigger: " + e.getMessage());
                }
            }

            return triggers;
        } catch (JsonSyntaxException e) {
            Main.getInstance().getLogger().severe("Erreur lors de la désérialisation des triggers: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Adaptateur de type pour gérer la polymorphie des Triggers
     */
    private static class TriggerTypeAdapter implements JsonSerializer<Trigger>, JsonDeserializer<Trigger> {

        @Override
        public JsonElement serialize(Trigger src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject result = new JsonObject();
            result.addProperty("className", src.getClass().getName());
            result.add("data", context.serialize(src, src.getClass()));
            return result;
        }

        @Override
        public Trigger deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            String className = jsonObject.get("className").getAsString();
            JsonElement data = jsonObject.get("data");

            try {
                Class<?> clazz = Class.forName(className);
                return context.deserialize(data, clazz);
            } catch (ClassNotFoundException e) {
                Main.getInstance().getLogger().warning("Classe de trigger inconnue: " + className);
                return null;
            }
        }
    }

    /**
     * Adaptateur de type pour gérer la polymorphie des Actions
     */
    private static class ActionTypeAdapter implements JsonSerializer<Action>, JsonDeserializer<Action> {

        @Override
        public JsonElement serialize(Action src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject result = new JsonObject();
            result.addProperty("className", src.getClass().getName());
            result.add("data", context.serialize(src, src.getClass()));
            return result;
        }

        @Override
        public Action deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            String className = jsonObject.get("className").getAsString();
            JsonElement data = jsonObject.get("data");

            try {
                Class<?> clazz = Class.forName(className);
                return context.deserialize(data, clazz);
            } catch (ClassNotFoundException e) {
                Main.getInstance().getLogger().warning("Classe d'action inconnue: " + className);
                return null;
            }
        }
    }
}

