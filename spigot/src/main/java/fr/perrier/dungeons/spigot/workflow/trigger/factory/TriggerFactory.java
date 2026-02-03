package fr.perrier.dungeons.spigot.workflow.trigger.factory;

import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.workflow.action.factory.ActionFactory;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.FunctionTrigger;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.RegionTrigger;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.EntityDeathTrigger;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.BlockClickTrigger;
import fr.perrier.dungeons.spigot.Main;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory pour créer des triggers depuis les données JSON de Blockly
 */
public class TriggerFactory {

    public static Trigger createTriggerFromJson(JsonObject triggerData) {
        try {
            String type = triggerData.get("type").getAsString();
            String name = triggerData.has("name") ? triggerData.get("name").getAsString() : "Trigger_" + System.currentTimeMillis();

            if (Main.isDebug()) {
                Main.getInstance().getLogger().info("Creating trigger: " + type + " - " + name);
            }

            Trigger trigger = switch (type) {
                case "region_trigger" -> createRegionTrigger(triggerData, name);
                case "function_trigger" -> createFunctionTrigger(triggerData, name);
                case "entity_death_trigger" -> createEntityDeathTrigger(triggerData, name);
                case "block_click_trigger" -> createBlockClickTrigger(triggerData, name);
                default -> {
                    Main.getInstance().getLogger().warning("&eTrigger type unknown: " + type);
                    yield null;
                }
            };

            if (trigger != null) {
                // Appliquer les propriétés communes
                if (triggerData.has("enabled")) {
                    trigger.setEnabled(triggerData.get("enabled").getAsBoolean());
                }

                // Parser les actions
                if (triggerData.has("actions")) {
                    JsonArray actionsArray = triggerData.getAsJsonArray("actions");
                    trigger.setActions(ActionFactory.parseActionsFromJson(actionsArray));
                }

                if (Main.isDebug()) {
                    Main.getInstance().getLogger().info("Trigger created with success: " + trigger.getName() + " with " + trigger.getActions().size() + " action(s)");
                }
            }

            return trigger;

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("&#FF0000An error occurred while creating trigger from JSON: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static RegionTrigger createRegionTrigger(JsonObject data, String name) {
        RegionTrigger trigger = new RegionTrigger(name);

        if (data.has("pos1x")) trigger.setPos1X(data.get("pos1x").getAsDouble());
        if (data.has("pos1y")) trigger.setPos1Y(data.get("pos1y").getAsDouble());
        if (data.has("pos1z")) trigger.setPos1Z(data.get("pos1z").getAsDouble());
        if (data.has("pos2x")) trigger.setPos2X(data.get("pos2x").getAsDouble());
        if (data.has("pos2y")) trigger.setPos2Y(data.get("pos2y").getAsDouble());
        if (data.has("pos2z")) trigger.setPos2Z(data.get("pos2z").getAsDouble());
        if (data.has("worldname")) trigger.setWorldName(data.get("worldname").getAsString());
        if (data.has("regionevent")) trigger.setRegionEvent(data.get("regionevent").getAsString());
        if (data.has("onlyonce")) trigger.setOnlyOnce(data.get("onlyonce").getAsBoolean());
        if (data.has("cooldownseconds")) trigger.setCooldownSeconds(data.get("cooldownseconds").getAsInt());

        return trigger;
    }

    private static FunctionTrigger createFunctionTrigger(JsonObject data, String name) {
        FunctionTrigger trigger = new FunctionTrigger(name);

        if (data.has("functionname")) trigger.setFunctionName(data.get("functionname").getAsString());

        return trigger;
    }

    private static EntityDeathTrigger createEntityDeathTrigger(JsonObject data, String name) {
        EntityDeathTrigger trigger = new EntityDeathTrigger(name);

        if (data.has("entitytype")) trigger.setEntityType(data.get("entitytype").getAsString());

        return trigger;
    }

    private static BlockClickTrigger createBlockClickTrigger(JsonObject data, String name) {
        BlockClickTrigger trigger = new BlockClickTrigger(name);

        if (data.has("clicktype")) trigger.setClickType(data.get("clicktype").getAsString());
        if (data.has("detectiontype")) trigger.setDetectionType(data.get("detectiontype").getAsString());
        if (data.has("blockmaterial")) trigger.setBlockMaterial(data.get("blockmaterial").getAsString());
        if (data.has("blockx")) trigger.setBlockX(data.get("blockx").getAsInt());
        if (data.has("blocky")) trigger.setBlockY(data.get("blocky").getAsInt());
        if (data.has("blockz")) trigger.setBlockZ(data.get("blockz").getAsInt());
        if (data.has("worldname")) trigger.setWorldName(data.get("worldname").getAsString());
        if (data.has("exactpositiononly")) trigger.setExactPositionOnly(data.get("exactpositiononly").getAsBoolean());

        return trigger;
    }

    public static List<TriggerData> parseTriggersFromJson(JsonArray triggersArray) {
        List<TriggerData> triggers = new ArrayList<>();

        for (JsonElement element : triggersArray) {
            if (element.isJsonObject()) {
                Trigger trigger = createTriggerFromJson(element.getAsJsonObject());
                if (trigger != null) {
                    // Important: garder l'instance Trigger au lieu de convertir en TriggerData
                    // pour préserver le type concret et les actions
                    triggers.add(trigger);
                    if(trigger instanceof FunctionTrigger functionTrigger) {
                        functionTrigger.registerFunction();
                    }
                }
            }
        }

        if (Main.isDebug()) {
            Main.getInstance().getLogger().info("Parsing triggers: " + triggers.size() + " trigger(s) created");
        }
        return triggers;
    }
}