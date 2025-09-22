package fr.perrier.dungeons.spigot.workflow.action.factory;

import com.google.gson.JsonPrimitive;
import fr.perrier.dungeons.spigot.workflow.action.impl.*;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.Main;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import fr.perrier.dungeons.spigot.workflow.condition.IfAction;

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
                case "set_variable_action" -> {
                    String variableName = actionData.has("variablename") ?
                            actionData.get("variablename").getAsString() : "ma_variable";
                    String value = actionData.has("value") ?
                            actionData.get("value").getAsString() : "0";
                    String scope = actionData.has("scope") ?
                            actionData.get("scope").getAsString() : "player";
                    yield new SetVariableAction(variableName, value, scope);
                }
                case "if_action" -> {
                    IfAction ifAction = new IfAction();

                    // Set condition if provided
                    if (actionData.has("leftvalue")) {
                        Object leftValue = parseValue(actionData.get("leftvalue"));
                        String operator = actionData.has("operator") ?
                                actionData.get("operator").getAsString() : "==";
                        Object rightValue = actionData.has("rightvalue") ?
                                parseValue(actionData.get("rightvalue")) : null;

                        ifAction.setCondition(leftValue, operator, rightValue);
                    }

                    // Load IF actions
                    if (actionData.has("ifactions")) {
                        JsonArray ifActionsArray = actionData.getAsJsonArray("ifactions");
                        for (JsonElement actionElement : ifActionsArray) {
                            Action action = createActionFromJson(actionElement.getAsJsonObject());
                            if (action != null) {
                                ifAction.addIfAction(action);
                            }
                        }
                    }

                    // Load ELSE actions
                    if (actionData.has("elseactions")) {
                        JsonArray elseActionsArray = actionData.getAsJsonArray("elseactions");
                        for (JsonElement actionElement : elseActionsArray) {
                            Action action = createActionFromJson(actionElement.getAsJsonObject());
                            if (action != null) {
                                ifAction.addElseAction(action);
                            }
                        }
                    }

                    yield ifAction;
                }
                case "end_dungeon_action" -> new EndDungeonAction();
                case "summon_mob_action" -> {
                    String mobType = actionData.has("mobtype") ?
                            actionData.get("mobtype").getAsString() : "ZOMBIE";
                    float x = actionData.has("x") ?
                            actionData.get("x").getAsFloat() : 0;
                    float y = actionData.has("y") ?
                            actionData.get("y").getAsFloat() : 0;
                    float z = actionData.has("z") ?
                            actionData.get("z").getAsFloat() : 0;
                    String worldName = actionData.has("worldname") ?
                            actionData.get("worldname").getAsString() : "world";
                    yield new SummonMobAction(mobType, x, y, z, worldName);
                }
                case "worldedit_schematic_action" -> {
                    String filename = actionData.has("filename") ?
                            actionData.get("filename").getAsString() : "schematic.schem";
                    float x = actionData.has("x") ?
                            actionData.get("x").getAsFloat() : 0;
                    float y = actionData.has("y") ?
                            actionData.get("y").getAsFloat() : 64;
                    float z = actionData.has("z") ?
                            actionData.get("z").getAsFloat() : 0;
                    yield new WorldEditSchematicAction(filename, x, y, z);
                }
                case "broadcast_command_action" -> {
                    String command = actionData.has("command") ?
                            actionData.get("command").getAsString() : "say Hello World!";
                    yield new BroadcastCommandAction(command);
                }
                case "delay_action" -> {
                    int ticks = actionData.has("ticks") ?
                        actionData.get("ticks").getAsInt() : 20;
                    yield new DelayAction(ticks);
                }
                default -> {
                    Main.getInstance().getLogger().warning("&eType d'action inconnu: " + type);
                    yield null;
                }
            };

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("&cErreur lors de la creation de l'action: " + e.getMessage());
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

    private static Object parseValue(JsonElement element) {
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isString()) {
                return primitive.getAsString();
            } else if (primitive.isNumber()) {
                return primitive.getAsNumber();
            } else if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            }
        }
        return element.toString();
    }
}