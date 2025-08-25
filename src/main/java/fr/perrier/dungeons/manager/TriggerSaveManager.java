package fr.perrier.dungeons.manager;

import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.model.Floor;
import fr.perrier.dungeons.workflow.action.Action;
import fr.perrier.dungeons.workflow.action.impl.*;
import fr.perrier.dungeons.workflow.trigger.Trigger;
import fr.perrier.dungeons.workflow.trigger.factory.TriggerFactory;
import fr.perrier.dungeons.workflow.trigger.impl.*;
import fr.perrier.dungeons.Main;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Service de sauvegarde des triggers depuis Blockly
 */
public class TriggerSaveManager {
    private final Gson gson = new Gson();

    /**
     * Sauvegarde les triggers depuis les données JSON de Blockly
     */
    public boolean saveTriggers(String dungeonName, String floorId, String jsonData, Player editor) {
        try {
            Main.getInstance().getLogger().info("Début de la sauvegarde des triggers pour " + dungeonName + " floor " + floorId);
            Main.getInstance().getLogger().info("Données JSON reçues: " + jsonData);

            // Parser les données JSON
            JsonObject data = gson.fromJson(jsonData, JsonObject.class);

            if (!data.has("triggers")) {
                Main.getInstance().getLogger().warning("Aucune donnée de triggers trouvée dans le JSON");
                return false;
            }

            JsonArray triggersArray = data.getAsJsonArray("triggers");

            // Convertir en objets Trigger
            List<Trigger> triggers = TriggerFactory.parseTriggersFromJson(triggersArray);

            Main.getInstance().getLogger().info("Triggers convertis: " + triggers.size() + " triggers");

            // Sauvegarder les triggers dans la mémoire
            Floor floor = Main.getInstance().getRedisStorageService().getCurrentFloor().get();
            floor.setTriggers(triggers);
            Main.getInstance().getRedisStorageService().syncFloor(floor);
            Main.getInstance().getLogger().info("Triggers assignés à l'instance en mémoire");

            Main.getInstance().getGlobalTriggerManager().refreshTriggerCache();

            // Sauvegarder dans le fichier .dungeon en utilisant votre DungeonFileManager
            boolean fileSaved = DungeonFileManager.saveTriggers(floorId, triggers);
            if (!fileSaved) {
                Main.getInstance().getLogger().warning("Échec de la sauvegarde du fichier .dungeon");
                return false;
            }

            // Notifier l'éditeur
            if (editor != null && editor.isOnline()) {
                editor.sendMessage(ChatUtil.translate("&a✓ Triggers sauvegardés avec succès !"));
                editor.sendMessage(ChatUtil.translate("&7➤ " + triggers.size() + " trigger(s) sauvegardé(s)"));
                editor.sendMessage(ChatUtil.translate("&7➤ Fichier: &e" + floorId + ".dungeon"));

                // Détail des triggers
                for (Trigger trigger : triggers) {
                    editor.sendMessage(ChatUtil.translate("&8  • &f" + trigger.getName() + " &7(" + trigger.getType() + ")"));
                }
            }

            Main.getInstance().getLogger().info("Sauvegarde terminée avec succès: " + triggers.size() + " triggers");
            return true;

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Erreur lors de la sauvegarde des triggers: " + e.getMessage());
            e.printStackTrace();

            if (editor != null && editor.isOnline()) {
                editor.sendMessage(ChatUtil.translate("&c❌ Erreur lors de la sauvegarde !"));
                editor.sendMessage(ChatUtil.translate("&c" + e.getMessage()));
            }

            return false;
        }
    }

    /**
     * Charge les triggers existants pour l'éditeur web
     */
    public String loadTriggersAsJson(String dungeonName, String floorId) {
        try {
            //List<Trigger> triggers = DungeonFileManager.loadTriggers(floorId);
            List<Trigger> triggers = Main.getInstance().getRedisStorageService().getCurrentFloor().get().getTriggers();

            // Convertir en format JSON pour Blockly
            JsonArray triggersArray = new JsonArray();

            for (Trigger trigger : triggers) {
                JsonObject triggerObj = new JsonObject();
                triggerObj.addProperty("id", trigger.getTriggerId().toString());
                triggerObj.addProperty("type", trigger.getType());
                triggerObj.addProperty("name", trigger.getName());
                triggerObj.addProperty("enabled", trigger.isEnabled());

                addPropertyOfTrigger(triggerObj, trigger);

                JsonArray actionsArray = new JsonArray();
                for (Action action : trigger.getActions()) {
                    JsonObject actionObj = new JsonObject();
                    actionObj.addProperty("type", action.getType());
                    actionObj.addProperty("name", action.getName());

                    addPropertyOfAction(actionObj, action);

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
            Main.getInstance().getLogger().severe("Erreur lors du chargement des triggers: " + e.getMessage());
            e.printStackTrace();

            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            return gson.toJson(error);
        }
    }

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
        }
        // Ajouter d'autres types de triggers ici
    }

    private void addPropertyOfAction(JsonObject actionObj, Action action) {
        if(action instanceof SendMessageAction sendAction) {
            actionObj.addProperty("targetplayer", sendAction.getTargetPlayer());
            actionObj.addProperty("message", sendAction.getMessage());
        } else if (action instanceof SendTitleAction titleAction) {
            actionObj.addProperty("targetplayer", titleAction.getTargetPlayer());
            actionObj.addProperty("title", titleAction.getTitle());
            actionObj.addProperty("subtitle", titleAction.getSubtitle());
            actionObj.addProperty("fadein", titleAction.getFadeIn());
            actionObj.addProperty("stay", titleAction.getStay());
            actionObj.addProperty("fadeout", titleAction.getFadeOut());

        } else if (action instanceof TeleporterAction teleporterAction) {
            actionObj.addProperty("targetplayer", teleporterAction.getTargetPlayer());
            actionObj.addProperty("x", teleporterAction.getX());
            actionObj.addProperty("y", teleporterAction.getY());
            actionObj.addProperty("z", teleporterAction.getZ());
            actionObj.addProperty("yaw", teleporterAction.getYaw());
            actionObj.addProperty("pitch", teleporterAction.getPitch());
            actionObj.addProperty("worldname", teleporterAction.getWorldName());

        }
        // Ajouter d'autres types d'actions ici
    }
}