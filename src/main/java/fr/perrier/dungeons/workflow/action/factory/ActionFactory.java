package fr.perrier.dungeons.workflow.action.factory;

import fr.perrier.dungeons.workflow.action.Action;
import fr.perrier.dungeons.workflow.action.impl.*;
import fr.perrier.dungeons.Main;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory pour créer des actions depuis JSON
 */
public class ActionFactory {

    public static Action createActionFromJson(JsonObject actionData) {
        try {
            String type = actionData.get("type").getAsString();

            // Edit here and edit in TriggerSaveManager too
            return switch (type) {
                case "send_message_action" -> {
                    String targetPlayer = actionData.has("targetplayer") ?
                            actionData.get("targetplayer").getAsString() : "player";
                    String message = actionData.has("message") ?
                            actionData.get("message").getAsString() : "";
                    yield new SendMessageAction(targetPlayer, message);
                }
                case "send_title_action" -> {
                    String targetPlayer = actionData.has("targetplayer") ?
                            actionData.get("targetplayer").getAsString() : "player";
                    String title = actionData.has("title") ?
                            actionData.get("title").getAsString() : "";
                    String subtitle = actionData.has("subtitle") ?
                            actionData.get("subtitle").getAsString() : "";
                    int fadeIn = actionData.has("fadein") ?
                            actionData.get("fadein").getAsInt() : 10;
                    int stay = actionData.has("stay") ?
                            actionData.get("stay").getAsInt() : 70;
                    int fadeOut = actionData.has("fadeout") ?
                            actionData.get("fadeout").getAsInt() : 20;
                    yield new SendTitleAction(targetPlayer, title, subtitle, fadeIn, stay, fadeOut);
                }
                case "teleporter_action" -> {
                    String targetPlayer = actionData.has("targetplayer") ?
                            actionData.get("targetplayer").getAsString() : "player";
                    float x = actionData.has("x") ?
                            actionData.get("x").getAsFloat() : 0;
                    float y = actionData.has("y") ?
                            actionData.get("y").getAsFloat() : 0;
                    float z = actionData.has("z") ?
                            actionData.get("z").getAsFloat() : 0;
                    float yaw = actionData.has("yaw") ?
                            actionData.get("yaw").getAsFloat() : 0;
                    float pitch = actionData.has("pitch") ?
                            actionData.get("pitch").getAsFloat() : 0;
                    String worldName = actionData.has("worldname") ?
                            actionData.get("worldname").getAsString() : "world";
                    yield new TeleporterAction(targetPlayer, x, y, z, yaw, pitch, worldName);
                }
                case "call_function_action" -> {
                    String functionName = actionData.has("functionname") ?
                            actionData.get("functionname").getAsString() : "ma_fonction";
                    yield new CallFunctionAction(functionName);
                }
                default -> {
                    Main.getInstance().getLogger().warning("Type d'action inconnu: " + type);
                    yield null;
                }
            };

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Erreur lors de la creation de l'action: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static List<Action> parseActionsFromJson(JsonArray actionsArray) {
        List<Action> actions = new ArrayList<>();

        if (actionsArray != null) {
            for (JsonElement element : actionsArray) {
                if (element.isJsonObject()) {
                    Action action = createActionFromJson(element.getAsJsonObject());
                    if (action != null) {
                        actions.add(action);
                    }
                }
            }
        }

        Main.getInstance().getLogger().info("Actions parsees: " + actions.size() + " action(s) creee(s)");
        return actions;
    }
}