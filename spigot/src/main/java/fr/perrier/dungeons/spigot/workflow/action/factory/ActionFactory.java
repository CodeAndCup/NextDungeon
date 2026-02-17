package fr.perrier.dungeons.spigot.workflow.action.factory;

import com.google.gson.JsonPrimitive;
import fr.perrier.dungeons.common.workflow.action.ActionData;
import fr.perrier.dungeons.spigot.workflow.action.impl.*;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.workflow.action.conditional.ConditionalAction;
import fr.perrier.dungeons.spigot.workflow.action.registry.ActionTypeRegistry;
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
 * Factory pour créer des actions depuis JSON.
 * Now uses ActionTypeRegistry for extensibility following Open/Closed Principle.
 * 
 * ARCHITECTURE CHANGE: This factory now delegates to ActionTypeRegistry for action creation.
 * The legacy switch statement is maintained for backward compatibility but will be phased out.
 */
public class ActionFactory {
    
    private static final ActionTypeRegistry registry = new ActionTypeRegistry();
    private static boolean registryInitialized = false;
    
    /**
     * Initialize the action type registry with all standard action types.
     * This method is called automatically on first use.
     */
    private static synchronized void initializeRegistry() {
        if (registryInitialized) {
            return;
        }
        
        registerStandardActions();
        registryInitialized = true;
    }
    
    /**
     * Register all standard action types in the registry.
     * Each action type gets a factory lambda that handles JSON parsing.
     */
    private static void registerStandardActions() {
        // Register all action types - each with its own factory
        registry.register("send_message_action", ActionFactory::createSendMessageAction);
        registry.register("send_title_action", ActionFactory::createSendTitleAction);
        registry.register("call_function_action", ActionFactory::createCallFunctionAction);
        registry.register("set_variable_action", ActionFactory::createSetVariableAction);
        registry.register("if_action", ActionFactory::createIfAction);
        registry.register("end_dungeon_action", data -> new EndDungeonAction());
        registry.register("summon_mob_action", ActionFactory::createSummonMobAction);
        registry.register("worldedit_schematic_action", ActionFactory::createWorldEditSchematicAction);
        registry.register("broadcast_command_action", ActionFactory::createBroadcastCommandAction);
        registry.register("delay_action", ActionFactory::createDelayAction);
        registry.register("get_variable_action", ActionFactory::createGetVariableAction);
        registry.register("math_operation_action", ActionFactory::createMathOperationAction);
        registry.register("play_sound_action", ActionFactory::createPlaySoundAction);
        registry.register("give_item_action", ActionFactory::createGiveItemAction);
        registry.register("apply_potion_effect_action", ActionFactory::createApplyPotionEffectAction);
        registry.register("set_health_action", ActionFactory::createSetHealthAction);
        registry.register("spawn_particle_action", ActionFactory::createSpawnParticleAction);
        registry.register("teleport_location_action", ActionFactory::createTeleportLocationAction);
        
        // Register condition actions
        registry.register("player_has_item_condition", ActionFactory::createPlayerHasItemCondition);
        registry.register("location_is_safe_condition", ActionFactory::createLocationIsSafeCondition);
        registry.register("time_of_day_condition", ActionFactory::createTimeOfDayCondition);
        registry.register("entity_type_is_condition", ActionFactory::createEntityTypeIsCondition);
        registry.register("player_in_region_condition", ActionFactory::createPlayerInRegionCondition);
        registry.register("block_type_is_condition", ActionFactory::createBlockTypeIsCondition);
        registry.register("player_permission_condition", ActionFactory::createPlayerPermissionCondition);
    }
    
    /**
     * Gets the action type registry for external registration of new action types.
     * This allows plugins to extend the system without modifying this class (Open/Closed Principle).
     * 
     * @return the action type registry
     */
    public static ActionTypeRegistry getRegistry() {
        initializeRegistry();
        return registry;
    }

    /**
     * Creates an action from JSON data.
     * Now uses the registry pattern for extensibility.
     */
    public static Action createActionFromJson(JsonObject actionData) {
        initializeRegistry();
        
        // Use registry to create action
        return registry.createAction(actionData);
    }
    
    // ========== Individual Action Factory Methods ==========
    // These methods extract JSON parsing logic into reusable, testable units
    
    private static Action createSendMessageAction(JsonObject data) {
        String targetPlayer = data.has("targetplayer") ?
                data.get("targetplayer").getAsString() : "player";
        String message = data.has("message") ?
                data.get("message").getAsString() : "";
        return new SendMessageAction(targetPlayer, message);
    }
    
    private static Action createSendTitleAction(JsonObject data) {
        String targetPlayer = data.has("targetplayer") ?
                data.get("targetplayer").getAsString() : "player";
        String title = data.has("title") ?
                data.get("title").getAsString() : "";
        String subtitle = data.has("subtitle") ?
                data.get("subtitle").getAsString() : "";
        int fadeIn = data.has("fadein") ?
                data.get("fadein").getAsInt() : 10;
        int stay = data.has("stay") ?
                data.get("stay").getAsInt() : 70;
        int fadeOut = data.has("fadeout") ?
                data.get("fadeout").getAsInt() : 20;
        return new SendTitleAction(targetPlayer, title, subtitle, fadeIn, stay, fadeOut);
    }
    
    private static Action createCallFunctionAction(JsonObject data) {
        String functionName = data.has("functionname") ?
                data.get("functionname").getAsString() : "ma_fonction";
        return new CallFunctionAction(functionName);
    }
    
    private static Action createSetVariableAction(JsonObject data) {
        String variableName = data.has("variablename") ?
                data.get("variablename").getAsString() : "ma_variable";
        String value = data.has("value") ?
                data.get("value").getAsString() : "0";
        String scope = data.has("scope") ?
                data.get("scope").getAsString() : "player";
        return new SetVariableAction(variableName, value, scope);
    }
    
    private static Action createIfAction(JsonObject data) {
        IfAction ifAction = new IfAction();
        
        // Set condition if provided
        if (data.has("leftvalue")) {
            Object leftValue = parseValue(data.get("leftvalue"));
            String operator = data.has("operator") ?
                    data.get("operator").getAsString() : "==";
            Object rightValue = data.has("rightvalue") ?
                    parseValue(data.get("rightvalue")) : null;
            
            ifAction.setCondition(leftValue, operator, rightValue);
        }
        
        // Load IF actions
        if (data.has("ifactions")) {
            JsonArray ifActionsArray = data.getAsJsonArray("ifactions");
            for (JsonElement actionElement : ifActionsArray) {
                Action action = createActionFromJson(actionElement.getAsJsonObject());
                if (action != null) {
                    ifAction.addIfAction(action);
                }
            }
        }
        
        // Load ELSE actions
        if (data.has("elseactions")) {
            JsonArray elseActionsArray = data.getAsJsonArray("elseactions");
            for (JsonElement actionElement : elseActionsArray) {
                Action action = createActionFromJson(actionElement.getAsJsonObject());
                if (action != null) {
                    ifAction.addElseAction(action);
                }
            }
        }
        
        return ifAction;
    }
    
    private static Action createSummonMobAction(JsonObject data) {
        String mobType = data.has("mobtype") ?
                data.get("mobtype").getAsString() : "ZOMBIE";
        LocationBlock location = LocationBlockParser.parseFromJson(data, "location");
        return new SummonMobAction(mobType, location);
    }
    
    private static Action createWorldEditSchematicAction(JsonObject data) {
        String filename = data.has("filename") ?
                data.get("filename").getAsString() : "schematic.schem";
        LocationBlock location = LocationBlockParser.parseFromJson(data, "location");
        return new WorldEditSchematicAction(filename, location);
    }
    
    private static Action createBroadcastCommandAction(JsonObject data) {
        String command = data.has("command") ?
                data.get("command").getAsString() : "say Hello World!";
        return new BroadcastCommandAction(command);
    }
    
    private static Action createDelayAction(JsonObject data) {
        int ticks = data.has("ticks") ?
                data.get("ticks").getAsInt() : 20;
        return new DelayAction(ticks);
    }
    
    private static Action createGetVariableAction(JsonObject data) {
        String sourceVariableName = data.has("sourcevariablename") ?
                data.get("sourcevariablename").getAsString() : "ma_variable";
        String destinationVariableName = data.has("destinationvariablename") ?
                data.get("destinationvariablename").getAsString() : "resultat_variable";
        String sourceScope = data.has("sourcescope") ?
                data.get("sourcescope").getAsString() : "player";
        String destinationScope = data.has("destinationscope") ?
                data.get("destinationscope").getAsString() : "player";
        return new GetVariableAction(sourceVariableName, destinationVariableName, sourceScope, destinationScope);
    }
    
    private static Action createMathOperationAction(JsonObject data) {
        String firstValue = data.has("firstvalue") ?
                data.get("firstvalue").getAsString() : "5";
        String operation = data.has("operation") ?
                data.get("operation").getAsString() : "add";
        String secondValue = data.has("secondvalue") ?
                data.get("secondvalue").getAsString() : "3";
        String resultVariableName = data.has("resultvariablename") ?
                data.get("resultvariablename").getAsString() : "resultat";
        String resultScope = data.has("resultscope") ?
                data.get("resultscope").getAsString() : "player";
        return new MathOperationAction(firstValue, operation, secondValue, resultVariableName, resultScope);
    }
    
    private static Action createPlaySoundAction(JsonObject data) {
        String sound = data.has("sound") ?
                data.get("sound").getAsString() : "ENTITY_PLAYER_LEVELUP";
        float volume = data.has("volume") ?
                data.get("volume").getAsFloat() : 1.0f;
        float pitch = data.has("pitch") ?
                data.get("pitch").getAsFloat() : 1.0f;
        String target = data.has("target") ?
                data.get("target").getAsString() : "player";
        return new PlaySoundAction(sound, volume, pitch, target);
    }
    
    private static Action createGiveItemAction(JsonObject data) {
        String itemMaterial = data.has("itemmaterial") ?
                data.get("itemmaterial").getAsString() : "DIAMOND";
        int amount = data.has("amount") ?
                data.get("amount").getAsInt() : 1;
        String customName = data.has("customname") ?
                data.get("customname").getAsString() : "";
        String target = data.has("target") ?
                data.get("target").getAsString() : "player";
        return new GiveItemAction(itemMaterial, amount, customName, target);
    }
    
    private static Action createApplyPotionEffectAction(JsonObject data) {
        String effectType = data.has("effecttype") ?
                data.get("effecttype").getAsString() : "SPEED";
        int duration = data.has("duration") ?
                data.get("duration").getAsInt() : 10;
        int amplifier = data.has("amplifier") ?
                data.get("amplifier").getAsInt() : 1;
        boolean ambient = data.has("ambient") ?
                data.get("ambient").getAsBoolean() : false;
        boolean particles = data.has("particles") ?
                data.get("particles").getAsBoolean() : true;
        String target = data.has("target") ?
                data.get("target").getAsString() : "player";
        return new ApplyPotionEffectAction(effectType, duration, amplifier, ambient, particles, target);
    }
    
    private static Action createSetHealthAction(JsonObject data) {
        String operation = data.has("operation") ?
                data.get("operation").getAsString() : "set";
        double value = data.has("value") ?
                data.get("value").getAsDouble() : 20;
        String target = data.has("target") ?
                data.get("target").getAsString() : "player";
        return new SetHealthAction(operation, value, target);
    }
    
    private static Action createSpawnParticleAction(JsonObject data) {
        String particleType = data.has("particletype") ?
                data.get("particletype").getAsString() : "FLAME";
        int count = data.has("count") ?
                data.get("count").getAsInt() : 10;
        double offsetX = data.has("offsetx") ?
                data.get("offsetx").getAsDouble() : 0.5;
        double offsetY = data.has("offsety") ?
                data.get("offsety").getAsDouble() : 0.5;
        double offsetZ = data.has("offsetz") ?
                data.get("offsetz").getAsDouble() : 0.5;
        double speed = data.has("speed") ?
                data.get("speed").getAsDouble() : 0.1;
        String locationSource = data.has("locationsource") ?
                data.get("locationsource").getAsString() : "player";
        return new SpawnParticleAction(particleType, count, offsetX, offsetY, offsetZ, speed, locationSource);
    }
    
    private static Action createTeleportLocationAction(JsonObject data) {
        String targetPlayer = data.has("targetplayer") ?
                data.get("targetplayer").getAsString() : "player";
        LocationBlock location = LocationBlockParser.parseFromJson(data, "location");
        return new TeleportLocationAction(targetPlayer, location);
    }
    
    // Condition actions
    private static Action createPlayerHasItemCondition(JsonObject data) {
        PlayerHasItemCondition condition = new PlayerHasItemCondition();
        
        if (data.has("itemmaterial")) {
            condition.setItemMaterial(data.get("itemmaterial").getAsString());
        }
        if (data.has("minamount")) {
            condition.setMinAmount(data.get("minamount").getAsInt());
        }
        if (data.has("checkname")) {
            condition.setCheckName(data.get("checkname").getAsBoolean());
        }
        if (data.has("itemname")) {
            condition.setItemName(data.get("itemname").getAsString());
        }
        
        loadConditionActions(condition, data);
        return condition;
    }
    
    private static Action createLocationIsSafeCondition(JsonObject data) {
        LocationIsSafeCondition condition = new LocationIsSafeCondition();
        
        if (data.has("location")) {
            condition.setLocation(LocationBlockParser.parseFromJson(data, "location"));
        }
        if (data.has("checksolidground")) {
            condition.setCheckSolidGround(data.get("checksolidground").getAsBoolean());
        }
        if (data.has("checkdangerousblocks")) {
            condition.setCheckDangerousBlocks(data.get("checkdangerousblocks").getAsBoolean());
        }
        
        loadConditionActions(condition, data);
        return condition;
    }
    
    private static Action createTimeOfDayCondition(JsonObject data) {
        TimeOfDayCondition condition = new TimeOfDayCondition();
        
        if (data.has("timeperiod")) {
            condition.setTimePeriod(data.get("timeperiod").getAsString());
        }
        if (data.has("customtime")) {
            condition.setCustomTime(data.get("customtime").getAsLong());
        }
        if (data.has("operator")) {
            condition.setOperator(data.get("operator").getAsString());
        }
        
        loadConditionActions(condition, data);
        return condition;
    }
    
    private static Action createEntityTypeIsCondition(JsonObject data) {
        EntityTypeIsCondition condition = new EntityTypeIsCondition();
        
        if (data.has("entitytype")) {
            condition.setEntityType(data.get("entitytype").getAsString());
        }
        if (data.has("comparison")) {
            condition.setComparison(data.get("comparison").getAsString());
        }
        
        loadConditionActions(condition, data);
        return condition;
    }
    
    private static Action createPlayerInRegionCondition(JsonObject data) {
        PlayerInRegionCondition condition = new PlayerInRegionCondition();
        
        if (data.has("pos1")) {
            LocationBlock pos1 = LocationBlockParser.parseFromJson(data, "pos1");
            condition.setPos1(pos1);
        }
        if (data.has("pos2")) {
            LocationBlock pos2 = LocationBlockParser.parseFromJson(data, "pos2");
            condition.setPos2(pos2);
        }
        if (data.has("comparison")) {
            condition.setComparison(data.get("comparison").getAsString());
        }
        
        loadConditionActions(condition, data);
        return condition;
    }
    
    private static Action createBlockTypeIsCondition(JsonObject data) {
        BlockTypeIsCondition condition = new BlockTypeIsCondition();
        
        if (data.has("x")) {
            condition.setX(data.get("x").getAsDouble());
        }
        if (data.has("y")) {
            condition.setY(data.get("y").getAsDouble());
        }
        if (data.has("z")) {
            condition.setZ(data.get("z").getAsDouble());
        }
        if (data.has("blocktype")) {
            condition.setBlockType(data.get("blocktype").getAsString());
        }
        if (data.has("comparison")) {
            condition.setComparison(data.get("comparison").getAsString());
        }
        
        loadConditionActions(condition, data);
        return condition;
    }
    
    private static Action createPlayerPermissionCondition(JsonObject data) {
        PlayerPermissionCondition condition = new PlayerPermissionCondition();
        
        if (data.has("permission")) {
            condition.setPermission(data.get("permission").getAsString());
        }
        if (data.has("comparison")) {
            condition.setComparison(data.get("comparison").getAsString());
        }
        
        loadConditionActions(condition, data);
        return condition;
    }
    
    /**
     * Helper method to load IF and ELSE actions for conditional objects.
     * Uses the ConditionalAction interface to eliminate instanceof checks (DRY principle).
     */
    private static void loadConditionActions(Action condition, JsonObject data) {
        if (!(condition instanceof ConditionalAction conditionalAction)) {
            // This shouldn't happen if factories are implemented correctly
            Main.getLoggerUtil().warning("Action does not implement ConditionalAction: " + condition.getClass().getName());
            return;
        }
        
        // Load IF actions
        if (data.has("ifactions")) {
            JsonArray ifActionsArray = data.getAsJsonArray("ifactions");
            for (JsonElement actionElement : ifActionsArray) {
                Action action = createActionFromJson(actionElement.getAsJsonObject());
                if (action != null) {
                    conditionalAction.addIfAction(action);
                }
            }
        }
        
        // Load ELSE actions
        if (data.has("elseactions")) {
            JsonArray elseActionsArray = data.getAsJsonArray("elseactions");
            for (JsonElement actionElement : elseActionsArray) {
                Action action = createActionFromJson(actionElement.getAsJsonObject());
                if (action != null) {
                    conditionalAction.addElseAction(action);
                }
            }
        }
    }
    
    /**
     * Legacy method preserved for backward compatibility.
     * @deprecated Use registry-based factories instead
     */
    @Deprecated
    private static Action createActionFromJsonLegacy(JsonObject actionData) {
        try {
            String type = actionData.get("type").getAsString();

            // Legacy switch statement - maintained for reference
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
                    if(actionData.has("pos1")) {
                        LocationBlock pos1 = LocationBlockParser.parseFromJson(actionData, "pos1");
                        condition.setPos1(pos1);
                    }
                    if(actionData.has("pos2")) {
                        LocationBlock pos2 = LocationBlockParser.parseFromJson(actionData, "pos2");
                        condition.setPos2(pos2);
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
                    LocationBlock location = LocationBlockParser.parseFromJson(actionData, "location");
                    yield new WorldEditSchematicAction(filename, location);
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
                    Main.getLoggerUtil().warning("Type d'action inconnu: " + type);
                    yield null;
                }
            };

        } catch (Exception e) {
            Main.getLoggerUtil().severe("Erreur lors de la creation de l'action: " + e.getMessage());
            e.printStackTrace(System.err);
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

        if (Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info("Actions parsees: " + actions.size() + " action(s) creee(s)");
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