package fr.perrier.dungeons.workflow.action.factory;

import fr.perrier.dungeons.workflow.action.Action;
import fr.perrier.dungeons.workflow.action.impl.SendMessageAction;
import fr.perrier.dungeons.Main;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import fr.perrier.dungeons.workflow.action.impl.SendTitleAction;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory pour créer des actions depuis JSON
 */
public class ActionFactory {

    public static Action createActionFromJson(JsonObject actionData) {
        try {
            String type = actionData.get("type").getAsString();

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
                default -> {
                    Main.getInstance().getLogger().warning("Type d'action inconnu: " + type);
                    yield null;
                }
            };

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Erreur lors de la création de l'action: " + e.getMessage());
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

        Main.getInstance().getLogger().info("Actions parsées: " + actions.size() + " action(s) créée(s)");
        return actions;
    }
}