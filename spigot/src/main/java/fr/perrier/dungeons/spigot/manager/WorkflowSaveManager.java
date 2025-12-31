package fr.perrier.dungeons.spigot.manager;

import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.common.workflow.action.ActionData;
import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.workflow.action.impl.*;
import fr.perrier.dungeons.spigot.workflow.condition.IfAction;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.workflow.trigger.factory.TriggerFactory;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.EntityDeathTrigger;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.FunctionTrigger;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.RegionTrigger;
import fr.perrier.dungeons.spigot.Main;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Manager for saving and loading workflows (triggers and actions)
 */
public class WorkflowSaveManager {
    private final Gson gson = new Gson();

    /**
     * Save workflows (triggers and actions) from JSON data
     * @param dungeonName Name of the dungeon
     * @param floorId ID of the floor
     * @param jsonData JSON data containing triggers and actions
     * @param editor Player who is editing (can be null)
     * @return True if save was successful, false otherwise
     */
    public boolean saveWorkflows(String dungeonName, String floorId, String jsonData, Player editor) {
        try {
            Main.getInstance().getLogger().info("Starting trigger save process for " + dungeonName + " floor " + floorId);
            Main.getInstance().getLogger().info("Json data received: " + jsonData);

            // Parser les données JSON
            JsonObject data = gson.fromJson(jsonData, JsonObject.class);

            if (!data.has("triggers")) {
                Main.getInstance().getLogger().warning("&eNo data of triggers found in JSON");
                return false;
            }

            JsonArray triggersArray = data.getAsJsonArray("triggers");

            // Convertir en objets Trigger
            List<TriggerData> triggers = TriggerFactory.parseTriggersFromJson(triggersArray);

            Main.getInstance().getLogger().info("Number of triggers parsed: " + triggers.size());

            // Sauvegarder les triggers dans la mémoire
            Floor floor = Main.getInstance().getRedisStorageService().getCurrentFloor();
            floor.setTriggers(triggers);
            Main.getInstance().getRedisStorageService().syncFloor(floor.toFloorData());
            Main.getInstance().getLogger().info("Trigger saved in memory for floor: " + floorId);

            Main.getInstance().getGlobalTriggerManager().refreshTriggerCache();

            // Sauvegarder dans la base de données en utilisant DungeonFileManager
            DungeonFileManager.saveTriggers(floorId, triggers).thenAccept(fileSaved -> {
                if (fileSaved) {
                    Main.getInstance().getLogger().info("Triggers saved in the database for floor: " + floorId);
                } else {
                    Main.getInstance().getLogger().warning("&eFailed to save triggers in the database for floor: " + floorId);
                }
            }).exceptionally(ex -> {
                Main.getInstance().getLogger().severe("&#FF0000An error occurred during the saving of triggers: " + ex.getMessage());
                ex.printStackTrace();
                return null;
            });

            // Notifier l'éditeur
            if (editor != null && editor.isOnline()) {
                editor.sendMessage(ChatUtil.translate("&#00FF00✓ Triggers sauvegardés avec succès !"));
                editor.sendMessage(ChatUtil.translate("&7➤ " + triggers.size() + " trigger(s) sauvegardé(s)"));
                editor.sendMessage(ChatUtil.translate("&7➤ Base de données: &e" + Main.getInstance().getConfig().getString("DatabaseConfiguration.type")));

                // Détail des triggers
                for (TriggerData triggerData : triggers) {
                    if(triggerData instanceof Trigger trigger)
                        editor.sendMessage(ChatUtil.translate("&8  • &f" + trigger.getName() + " &7(" + trigger.getType() + ")"));
                    else {
                        editor.sendMessage(ChatUtil.translate("&8  • &fUnknown Trigger Type"));
                        Main.getInstance().getLogger().warning("&eUnknown TriggerData type: " + triggerData.toString());
                    }
                }
            }

            Main.getInstance().getLogger().info("Save process completed successfully: " + triggers.size() + " triggers");
            return true;

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("&#FF0000An error occurred while saving triggers: " + e.getMessage());
            e.printStackTrace();

            if (editor != null && editor.isOnline()) {
                editor.sendMessage(ChatUtil.translate("&#FF0000❌ Erreur lors de la sauvegarde !"));
                editor.sendMessage(ChatUtil.translate("&#FF0000" + e.getMessage()));
            }

            return false;
        }
    }

    /**
     * Load triggers as JSON for Blockly
     * @param dungeonName Name of the dungeon
     * @param floorId ID of the floor
     * @return JSON string representing the triggers
     */
    public String loadTriggersAsJson(String dungeonName, String floorId) {
        try {
            List<TriggerData> triggers = Main.getInstance().getRedisStorageService().getCurrentFloor().getTriggers();

            // Convertir en format JSON pour Blockly
            JsonArray triggersArray = new JsonArray();

            for (TriggerData triggerData : triggers) {
                if(!(triggerData instanceof Trigger trigger))
                    continue;

                JsonObject triggerObj = new JsonObject();
                triggerObj.addProperty("id", trigger.getTriggerId().toString());
                triggerObj.addProperty("type", trigger.getType());
                triggerObj.addProperty("name", trigger.getName());
                triggerObj.addProperty("enabled", trigger.isEnabled());

                addPropertyOfTrigger(triggerObj, trigger);

                JsonArray actionsArray = new JsonArray();
                for (ActionData actionData : trigger.getActions()) {

                    JsonObject actionObj = new JsonObject();
                    actionObj.addProperty("type", actionData.getType());
                    actionObj.addProperty("name", actionData.getName());

                    addPropertyOfAction(actionObj, actionData);

                    actionsArray.add(actionObj);
                }
                triggerObj.add("actions", actionsArray);

                triggersArray.add(triggerObj);
            }

            JsonObject result = new JsonObject();
            result.add("triggers", triggersArray);
            result.addProperty("dungeon", dungeonName);
            result.addProperty("floor", floorId);
            result.addProperty("count", triggers.size());

            return gson.toJson(result);

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("&#FF0000Erreur lors du chargement des triggers: " + e.getMessage());
            e.printStackTrace();

            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            return gson.toJson(error);
        }
    }

    /**
     * Add specific properties of a trigger to its JSON representation
     * @param triggerObj JSON object representing the trigger
     * @param trigger Trigger object
     */
    private void addPropertyOfTrigger(JsonObject triggerObj, Trigger trigger) {
        if(trigger instanceof RegionTrigger regionTrigger) {
            triggerObj.addProperty("pos1x", regionTrigger.getPos1X());
            triggerObj.addProperty("pos1y", regionTrigger.getPos1Y());
            triggerObj.addProperty("pos1z", regionTrigger.getPos1Z());
            triggerObj.addProperty("pos2x", regionTrigger.getPos2X());
            triggerObj.addProperty("pos2y", regionTrigger.getPos2Y());
            triggerObj.addProperty("pos2z", regionTrigger.getPos2Z());
            triggerObj.addProperty("worldname", regionTrigger.getWorldName());
            triggerObj.addProperty("regionevent", regionTrigger.getRegionEvent());
            triggerObj.addProperty("onlyonce", regionTrigger.isOnlyOnce());
            triggerObj.addProperty("cooldownseconds", regionTrigger.getCooldownSeconds());
        } else if (trigger instanceof FunctionTrigger functionTrigger) {
            triggerObj.addProperty("functionname", functionTrigger.getFunctionName());
        } else if (trigger instanceof EntityDeathTrigger deathTrigger) {
            triggerObj.addProperty("entitytype", deathTrigger.getEntityType());

        }
    }

    /**
     * Add specific properties of an action to its JSON representation
     * @param actionObj JSON object representing the action
     * @param actionData Action object
     */
    private void addPropertyOfAction(JsonObject actionObj, ActionData actionData) {
        if(!(actionData instanceof Action action)) {
            Main.getInstance().getLogger().warning("Unknown ActionData type: " + actionData.getClass().getName());
            return;
        }

        switch (action) {
            case SendMessageAction sendAction -> {
                actionObj.addProperty("targetplayer", sendAction.getTargetPlayer());
                actionObj.addProperty("message", sendAction.getMessage());
            }
            case SendTitleAction titleAction -> {
                actionObj.addProperty("targetplayer", titleAction.getTargetPlayer());
                actionObj.addProperty("title", titleAction.getTitle());
                actionObj.addProperty("subtitle", titleAction.getSubtitle());
                actionObj.addProperty("fadein", titleAction.getFadeIn());
                actionObj.addProperty("stay", titleAction.getStay());
                actionObj.addProperty("fadeout", titleAction.getFadeOut());
            }
            case TeleporterAction teleporterAction -> {
                actionObj.addProperty("targetplayer", teleporterAction.getTargetPlayer());
                actionObj.addProperty("x", teleporterAction.getX());
                actionObj.addProperty("y", teleporterAction.getY());
                actionObj.addProperty("z", teleporterAction.getZ());
                actionObj.addProperty("yaw", teleporterAction.getYaw());
                actionObj.addProperty("pitch", teleporterAction.getPitch());
                actionObj.addProperty("worldname", teleporterAction.getWorldName());
            }
            case CallFunctionAction functionAction ->
                    actionObj.addProperty("functionname", functionAction.getFunctionName());
            case SetVariableAction setVariableAction -> {
                actionObj.addProperty("variablename", setVariableAction.getVariableName());
                actionObj.addProperty("value", setVariableAction.getValue());
                actionObj.addProperty("scope", setVariableAction.getScope());
            }
            case IfAction ifAction -> {
                actionObj.addProperty("operator", ifAction.getOperator());
                actionObj.addProperty("leftvalue", ifAction.getLeftValue() != null ? ifAction.getLeftValue().toString() : "");
                actionObj.addProperty("rightvalue", ifAction.getRightValue() != null ? ifAction.getRightValue().toString() : "");

                JsonArray ifActionsArray = new JsonArray();
                for (Action ifActions : ifAction.getIfActions()) {
                    JsonObject thenActionObj = new JsonObject();
                    thenActionObj.addProperty("type", ifActions.getType());
                    thenActionObj.addProperty("name", ifActions.getName());
                    addPropertyOfAction(thenActionObj, ifActions);
                    ifActionsArray.add(thenActionObj);
                }
                actionObj.add("ifactions", ifActionsArray);

                JsonArray elseActionsArray = new JsonArray();
                for (Action elseActions : ifAction.getElseActions()) {
                    JsonObject elseActionObj = new JsonObject();
                    elseActionObj.addProperty("type", elseActions.getType());
                    elseActionObj.addProperty("name", elseActions.getName());
                    addPropertyOfAction(elseActionObj, elseActions);
                    elseActionsArray.add(elseActionObj);
                }
                actionObj.add("elseactions", elseActionsArray);
            }
            case SummonMobAction summonMobAction -> {
                actionObj.addProperty("mobtype", summonMobAction.getMobType());
                actionObj.addProperty("x", summonMobAction.getX());
                actionObj.addProperty("y", summonMobAction.getY());
                actionObj.addProperty("z", summonMobAction.getZ());
                actionObj.addProperty("worldname", summonMobAction.getWorldName());
            }
            case WorldEditSchematicAction schematicAction -> {
                actionObj.addProperty("filename", schematicAction.getFilename());
                actionObj.addProperty("x", schematicAction.getX());
                actionObj.addProperty("y", schematicAction.getY());
                actionObj.addProperty("z", schematicAction.getZ());
            }
            case BroadcastCommandAction broadcastCommandAction ->
                    actionObj.addProperty("command", broadcastCommandAction.getCommand());
            case DelayAction delayAction -> actionObj.addProperty("ticks", delayAction.getTicks());
            default -> {
            }
        }
    }
}