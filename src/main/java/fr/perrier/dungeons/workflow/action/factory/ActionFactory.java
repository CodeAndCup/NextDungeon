package fr.perrier.dungeons.workflow.action.factory;

import fr.perrier.dungeons.workflow.action.Action;
import fr.perrier.dungeons.workflow.action.impl.SendMessageAction;
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

            return switch (type) {
                case "send_message" -> {
                    String targetPlayer = actionData.has("targetPlayer") ?
                            actionData.get("targetPlayer").getAsString() : "player";
                    String message = actionData.has("message") ?
                            actionData.get("message").getAsString() : "";
                    yield new SendMessageAction(targetPlayer, message);
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