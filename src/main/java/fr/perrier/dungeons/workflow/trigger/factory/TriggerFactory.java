package fr.perrier.dungeons.workflow.trigger.factory;

import fr.perrier.dungeons.workflow.trigger.Trigger;
import fr.perrier.dungeons.workflow.trigger.impl.RegionTrigger;
import fr.perrier.dungeons.workflow.action.factory.ActionFactory;
import fr.perrier.dungeons.Main;
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

            Main.getInstance().getLogger().info("Création du trigger: " + type + " - " + name);

            Trigger trigger = switch (type) {
                case "region" -> createRegionTrigger(triggerData, name);
                default -> {
                    Main.getInstance().getLogger().warning("Type de trigger inconnu: " + type);
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

                Main.getInstance().getLogger().info("Trigger créé avec succès: " + trigger.getName() + " avec " + trigger.getActions().size() + " action(s)");
            }

            return trigger;

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Erreur lors de la création du trigger: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static RegionTrigger createRegionTrigger(JsonObject data, String name) {
        RegionTrigger trigger = new RegionTrigger(name);

        if (data.has("pos1X")) trigger.setPos1X(data.get("pos1X").getAsDouble());
        if (data.has("pos1Y")) trigger.setPos1Y(data.get("pos1Y").getAsDouble());
        if (data.has("pos1Z")) trigger.setPos1Z(data.get("pos1Z").getAsDouble());
        if (data.has("pos2X")) trigger.setPos2X(data.get("pos2X").getAsDouble());
        if (data.has("pos2Y")) trigger.setPos2Y(data.get("pos2Y").getAsDouble());
        if (data.has("pos2Z")) trigger.setPos2Z(data.get("pos2Z").getAsDouble());
        if (data.has("world")) trigger.setWorldName(data.get("world").getAsString());

        return trigger;
    }

    public static List<Trigger> parseTriggersFromJson(JsonArray triggersArray) {
        List<Trigger> triggers = new ArrayList<>();

        for (JsonElement element : triggersArray) {
            if (element.isJsonObject()) {
                Trigger trigger = createTriggerFromJson(element.getAsJsonObject());
                if (trigger != null) {
                    triggers.add(trigger);
                }
            }
        }

        Main.getInstance().getLogger().info("Triggers parsés: " + triggers.size() + " triggers créés");
        return triggers;
    }
}