package fr.perrier.dungeons.spigot.workflow.action.factory;

import com.google.gson.JsonPrimitive;
import fr.perrier.dungeons.common.workflow.action.ActionData;
import fr.perrier.dungeons.spigot.workflow.action.impl.*;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.Main;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import fr.perrier.dungeons.spigot.workflow.blocks.LocationBlock;
import fr.perrier.dungeons.spigot.workflow.blocks.LocationBlockParser;
import fr.perrier.dungeons.spigot.workflow.condition.*;

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
                case "player_has_item_condition" -> {
                    PlayerHasItemCondition condition = new PlayerHasItemCondition();

                    if (actionData.has("itemmaterial")) {
                        condition.setItemMaterial(actionData.get("itemmaterial").getAsString());
                    }
                    if (actionData.has("minamount")) {
                        condition.setMinAmount(actionData.get("minamount").getAsInt());
                    }
                    if (actionData.has("checkname")) {
                        condition.setCheckName(actionData.get("checkname").getAsBoolean());
                    }
                    if (actionData.has("itemname")) {
                        condition.setItemName(actionData.get("itemname").getAsString());
                    }

                    // Load IF actions
                    if (actionData.has("ifactions")) {
                        JsonArray ifActionsArray = actionData.getAsJsonArray("ifactions");
                        for (JsonElement actionElement : ifActionsArray) {
                            Action action = createActionFromJson(actionElement.getAsJsonObject());
                            if (action != null) {
                                condition.addIfAction(action);
                            }
                        }
                    }

                    // Load ELSE actions
                    if (actionData.has("elseactions")) {
                        JsonArray elseActionsArray = actionData.getAsJsonArray("elseactions");
                        for (JsonElement actionElement : elseActionsArray) {
                            Action action = createActionFromJson(actionElement.getAsJsonObject());
                            if (action != null) {
                                condition.addElseAction(action);
                            }
                        }
                    }

                    yield condition;
                }
                case "location_is_safe_condition" -> {
                    LocationIsSafeCondition condition = new LocationIsSafeCondition();

                    if (actionData.has("location")) {
                        condition.setLocation(fr.perrier.dungeons.spigot.workflow.blocks.LocationBlockParser.parseFromJson(actionData, "location"));
                    }
                    if (actionData.has("checksolidground")) {
                        condition.setCheckSolidGround(actionData.get("checksolidground").getAsBoolean());
                    }
                    if (actionData.has("checkdangerousblocks")) {
                        condition.setCheckDangerousBlocks(actionData.get("checkdangerousblocks").getAsBoolean());
                    }

                    // Load IF actions
                    if (actionData.has("ifactions")) {
                        JsonArray ifActionsArray = actionData.getAsJsonArray("ifactions");
                        for (JsonElement actionElement : ifActionsArray) {
                            Action action = createActionFromJson(actionElement.getAsJsonObject());
                            if (action != null) {
                                condition.addIfAction(action);
                            }
                        }
                    }

                    // Load ELSE actions
                    if (actionData.has("elseactions")) {
                        JsonArray elseActionsArray = actionData.getAsJsonArray("elseactions");
                        for (JsonElement actionElement : elseActionsArray) {
                            Action action = createActionFromJson(actionElement.getAsJsonObject());
                            if (action != null) {
                                condition.addElseAction(action);
                            }
                        }
                    }

                    yield condition;
                }
                case "time_of_day_condition" -> {
                    TimeOfDayCondition condition = new TimeOfDayCondition();

                    if (actionData.has("timeperiod")) {
                        condition.setTimePeriod(actionData.get("timeperiod").getAsString());
                    }
                    if (actionData.has("customtime")) {
                        condition.setCustomTime(actionData.get("customtime").getAsLong());
                    }
                    if (actionData.has("operator")) {
                        condition.setOperator(actionData.get("operator").getAsString());
                    }

                    // Load IF actions
                    if (actionData.has("ifactions")) {
                        JsonArray ifActionsArray = actionData.getAsJsonArray("ifactions");
                        for (JsonElement actionElement : ifActionsArray) {
                            Action action = createActionFromJson(actionElement.getAsJsonObject());
                            if (action != null) {
                                condition.addIfAction(action);
                            }
                        }
                    }

                    // Load ELSE actions
                    if (actionData.has("elseactions")) {
                        JsonArray elseActionsArray = actionData.getAsJsonArray("elseactions");
                        for (JsonElement actionElement : elseActionsArray) {
                            Action action = createActionFromJson(actionElement.getAsJsonObject());
                            if (action != null) {
                                condition.addElseAction(action);
                            }
                        }
                    }

                    yield condition;
                }
                case "entity_type_is_condition" -> {
                    EntityTypeIsCondition condition = new EntityTypeIsCondition();

                    if (actionData.has("entitytype")) {
                        condition.setEntityType(actionData.get("entitytype").getAsString());
                    }
                    if (actionData.has("comparison")) {
                        condition.setComparison(actionData.get("comparison").getAsString());
                    }

                    // Load IF actions
                    if (actionData.has("ifactions")) {
                        JsonArray ifActionsArray = actionData.getAsJsonArray("ifactions");
                        for (JsonElement actionElement : ifActionsArray) {
                            Action action = createActionFromJson(actionElement.getAsJsonObject());
                            if (action != null) {
                                condition.addIfAction(action);
                            }
                        }
                    }

                    // Load ELSE actions
                    if (actionData.has("elseactions")) {
                        JsonArray elseActionsArray = actionData.getAsJsonArray("elseactions");
                        for (JsonElement actionElement : elseActionsArray) {
                            Action action = createActionFromJson(actionElement.getAsJsonObject());
                            if (action != null) {
                                condition.addElseAction(action);
                            }
                        }
                    }

                    yield condition;
                }
                case "player_in_region_condition" -> {
                    PlayerInRegionCondition condition = new PlayerInRegionCondition();

                    if (actionData.has("pos1x")) {
                        condition.setPos1X(actionData.get("pos1x").getAsDouble());
                    }
                    if (actionData.has("pos1y")) {
                        condition.setPos1Y(actionData.get("pos1y").getAsDouble());
                    }
                    if (actionData.has("pos1z")) {
                        condition.setPos1Z(actionData.get("pos1z").getAsDouble());
                    }
                    if (actionData.has("pos2x")) {
                        condition.setPos2X(actionData.get("pos2x").getAsDouble());
                    }
                    if (actionData.has("pos2y")) {
                        condition.setPos2Y(actionData.get("pos2y").getAsDouble());
                    }
                    if (actionData.has("pos2z")) {
                        condition.setPos2Z(actionData.get("pos2z").getAsDouble());
                    }
                    if (actionData.has("comparison")) {
                        condition.setComparison(actionData.get("comparison").getAsString());
                    }

                    // Load IF actions
                    if (actionData.has("ifactions")) {
                        JsonArray ifActionsArray = actionData.getAsJsonArray("ifactions");
                        for (JsonElement actionElement : ifActionsArray) {
                            Action action = createActionFromJson(actionElement.getAsJsonObject());
                            if (action != null) {
                                condition.addIfAction(action);
                            }
                        }
                    }

                    // Load ELSE actions
                    if (actionData.has("elseactions")) {
                        JsonArray elseActionsArray = actionData.getAsJsonArray("elseactions");
                        for (JsonElement actionElement : elseActionsArray) {
                            Action action = createActionFromJson(actionElement.getAsJsonObject());
                            if (action != null) {
                                condition.addElseAction(action);
                            }
                        }
                    }

                    yield condition;
                }
                case "block_type_is_condition" -> {
                    BlockTypeIsCondition condition = new BlockTypeIsCondition();

                    if (actionData.has("x")) {
                        condition.setX(actionData.get("x").getAsDouble());
                    }
                    if (actionData.has("y")) {
                        condition.setY(actionData.get("y").getAsDouble());
                    }
                    if (actionData.has("z")) {
                        condition.setZ(actionData.get("z").getAsDouble());
                    }
                    if (actionData.has("blocktype")) {
                        condition.setBlockType(actionData.get("blocktype").getAsString());
                    }
                    if (actionData.has("comparison")) {
                        condition.setComparison(actionData.get("comparison").getAsString());
                    }

                    // Load IF actions
                    if (actionData.has("ifactions")) {
                        JsonArray ifActionsArray = actionData.getAsJsonArray("ifactions");
                        for (JsonElement actionElement : ifActionsArray) {
                            Action action = createActionFromJson(actionElement.getAsJsonObject());
                            if (action != null) {
                                condition.addIfAction(action);
                            }
                        }
                    }

                    // Load ELSE actions
                    if (actionData.has("elseactions")) {
                        JsonArray elseActionsArray = actionData.getAsJsonArray("elseactions");
                        for (JsonElement actionElement : elseActionsArray) {
                            Action action = createActionFromJson(actionElement.getAsJsonObject());
                            if (action != null) {
                                condition.addElseAction(action);
                            }
                        }
                    }

                    yield condition;
                }
                case "player_permission_condition" -> {
                    PlayerPermissionCondition condition = new PlayerPermissionCondition();

                    if (actionData.has("permission")) {
                        condition.setPermission(actionData.get("permission").getAsString());
                    }
                    if (actionData.has("comparison")) {
                        condition.setComparison(actionData.get("comparison").getAsString());
                    }

                    // Load IF actions
                    if (actionData.has("ifactions")) {
                        JsonArray ifActionsArray = actionData.getAsJsonArray("ifactions");
                        for (JsonElement actionElement : ifActionsArray) {
                            Action action = createActionFromJson(actionElement.getAsJsonObject());
                            if (action != null) {
                                condition.addIfAction(action);
                            }
                        }
                    }

                    // Load ELSE actions
                    if (actionData.has("elseactions")) {
                        JsonArray elseActionsArray = actionData.getAsJsonArray("elseactions");
                        for (JsonElement actionElement : elseActionsArray) {
                            Action action = createActionFromJson(actionElement.getAsJsonObject());
                            if (action != null) {
                                condition.addElseAction(action);
                            }
                        }
                    }

                    yield condition;
                }
                case "end_dungeon_action" -> new EndDungeonAction();
                case "summon_mob_action" -> {
                    String mobType = actionData.has("mobtype") ?
                            actionData.get("mobtype").getAsString() : "ZOMBIE";
                    LocationBlock location = LocationBlockParser.parseFromJson(actionData, "location");
                    yield new SummonMobAction(mobType, location);
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
                case "get_variable_action" -> {
                    String sourceVariableName = actionData.has("sourcevariablename") ?
                            actionData.get("sourcevariablename").getAsString() : "ma_variable";
                    String destinationVariableName = actionData.has("destinationvariablename") ?
                            actionData.get("destinationvariablename").getAsString() : "resultat_variable";
                    String sourceScope = actionData.has("sourcescope") ?
                            actionData.get("sourcescope").getAsString() : "player";
                    String destinationScope = actionData.has("destinationscope") ?
                            actionData.get("destinationscope").getAsString() : "player";
                    yield new GetVariableAction(sourceVariableName, destinationVariableName, sourceScope, destinationScope);
                }
                case "math_operation_action" -> {
                    String firstValue = actionData.has("firstvalue") ?
                            actionData.get("firstvalue").getAsString() : "5";
                    String operation = actionData.has("operation") ?
                            actionData.get("operation").getAsString() : "add";
                    String secondValue = actionData.has("secondvalue") ?
                            actionData.get("secondvalue").getAsString() : "3";
                    String resultVariableName = actionData.has("resultvariablename") ?
                            actionData.get("resultvariablename").getAsString() : "resultat";
                    String resultScope = actionData.has("resultscope") ?
                            actionData.get("resultscope").getAsString() : "player";
                    yield new MathOperationAction(firstValue, operation, secondValue, resultVariableName, resultScope);
                }
                case "play_sound_action" -> {
                    String sound = actionData.has("sound") ?
                            actionData.get("sound").getAsString() : "ENTITY_PLAYER_LEVELUP";
                    float volume = actionData.has("volume") ?
                            actionData.get("volume").getAsFloat() : 1.0f;
                    float pitch = actionData.has("pitch") ?
                            actionData.get("pitch").getAsFloat() : 1.0f;
                    String target = actionData.has("target") ?
                            actionData.get("target").getAsString() : "player";
                    yield new PlaySoundAction(sound, volume, pitch, target);
                }
                case "give_item_action" -> {
                    String itemMaterial = actionData.has("itemmaterial") ?
                            actionData.get("itemmaterial").getAsString() : "DIAMOND";
                    int amount = actionData.has("amount") ?
                            actionData.get("amount").getAsInt() : 1;
                    String customName = actionData.has("customname") ?
                            actionData.get("customname").getAsString() : "";
                    String target = actionData.has("target") ?
                            actionData.get("target").getAsString() : "player";
                    yield new GiveItemAction(itemMaterial, amount, customName, target);
                }
                case "apply_potion_effect_action" -> {
                    String effectType = actionData.has("effecttype") ?
                            actionData.get("effecttype").getAsString() : "SPEED";
                    int duration = actionData.has("duration") ?
                            actionData.get("duration").getAsInt() : 10;
                    int amplifier = actionData.has("amplifier") ?
                            actionData.get("amplifier").getAsInt() : 1;
                    boolean ambient = actionData.has("ambient") ?
                            actionData.get("ambient").getAsBoolean() : false;
                    boolean particles = actionData.has("particles") ?
                            actionData.get("particles").getAsBoolean() : true;
                    String target = actionData.has("target") ?
                            actionData.get("target").getAsString() : "player";
                    yield new ApplyPotionEffectAction(effectType, duration, amplifier, ambient, particles, target);
                }
                case "set_health_action" -> {
                    String operation = actionData.has("operation") ?
                            actionData.get("operation").getAsString() : "set";
                    double value = actionData.has("value") ?
                            actionData.get("value").getAsDouble() : 20;
                    String target = actionData.has("target") ?
                            actionData.get("target").getAsString() : "player";
                    yield new SetHealthAction(operation, value, target);
                }
                case "spawn_particle_action" -> {
                    String particleType = actionData.has("particletype") ?
                            actionData.get("particletype").getAsString() : "FLAME";
                    int count = actionData.has("count") ?
                            actionData.get("count").getAsInt() : 10;
                    double offsetX = actionData.has("offsetx") ?
                            actionData.get("offsetx").getAsDouble() : 0.5;
                    double offsetY = actionData.has("offsety") ?
                            actionData.get("offsety").getAsDouble() : 0.5;
                    double offsetZ = actionData.has("offsetz") ?
                            actionData.get("offsetz").getAsDouble() : 0.5;
                    double speed = actionData.has("speed") ?
                            actionData.get("speed").getAsDouble() : 0.1;
                    String locationSource = actionData.has("locationsource") ?
                            actionData.get("locationsource").getAsString() : "player";
                    yield new SpawnParticleAction(particleType, count, offsetX, offsetY, offsetZ, speed, locationSource);
                }
                case "teleport_location_action" -> {
                    String targetPlayer = actionData.has("targetplayer") ?
                            actionData.get("targetplayer").getAsString() : "player";
                    LocationBlock location = LocationBlockParser.parseFromJson(actionData, "location");
                    yield new TeleportLocationAction(targetPlayer, location);
                }
                default -> {
                    Main.getInstance().getLogger().warning("&eType d'action inconnu: " + type);
                    yield null;
                }
            };

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("&#FF0000Erreur lors de la creation de l'action: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static List<ActionData> parseActionsFromJson(JsonArray actionsArray) {
        List<ActionData> actions = new ArrayList<>();

        if (actionsArray != null) {
            for (JsonElement element : actionsArray) {
                if (element.isJsonObject()) {
                    ActionData action = createActionFromJson(element.getAsJsonObject());
                    if (action != null) {
                        actions.add(action);
                    }
                }
            }
        }

        if (Main.isDebug()) {
            Main.getInstance().getLogger().info("Actions parsees: " + actions.size() + " action(s) creee(s)");
        }
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