package fr.perrier.dungeons.spigot.workflow.serializer;

import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.common.workflow.action.ActionData;
import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.database.DatabaseTriggersManager;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.workflow.trigger.factory.TriggerFactory;
import fr.perrier.dungeons.spigot.Main;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Manager for saving and loading workflows (triggers and actions)
 *
 * ARCHITECTURE: Uses automatic Gson serialization for extensibility
 * - No need to manually add properties for new Trigger/Action types
 * - Simply add new Trigger/Action classes and they'll automatically serialize/deserialize
 * - Properties are automatically extracted from the object's fields via Gson reflection
 * - CRITICAL: Configured to access private fields which are used in all Trigger/Action classes
 */
public class EditorSerializer {
    private final Gson gson;

    public EditorSerializer() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

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
            if(Main.isDebug()) {
                Main.getInstance().getLogger().info("Json data received: " + jsonData);
            }

            // Parse the JSON data
            JsonObject data = gson.fromJson(jsonData, JsonObject.class);

            if (!data.has("triggers")) {
                Main.getInstance().getLogger().warning("&eNo triggers data found in JSON");
                return false;
            }

            JsonArray triggersArray = data.getAsJsonArray("triggers");

            // Convert to Trigger objects
            List<TriggerData> triggers = TriggerFactory.parseTriggersFromJson(triggersArray);

            Main.getInstance().getLogger().info("Number of triggers parsed: " + triggers.size());

            // Save triggers to memory
            Floor floor = Main.getInstance().getDungeonService().getCurrentFloor();
            floor.setTriggers(triggers);
            Main.getInstance().getDungeonService().syncFloor(floor.toFloorData());
            Main.getInstance().getLogger().info("Triggers saved in memory for floor: " + floorId);

            Main.getInstance().getTriggersRegistry().refreshTriggerCache();

            // Save to database asynchronously
            DatabaseTriggersManager.saveTriggers(floorId, triggers).thenAccept(fileSaved -> {
                if (fileSaved) {
                    Main.getInstance().getLogger().info("Triggers saved in the database for floor: " + floorId);
                } else {
                    Main.getInstance().getLogger().warning("&eFailed to save triggers in the database for floor: " + floorId);
                }
            }).exceptionally(ex -> {
                Main.getInstance().getLogger().severe("&#FF0000An error occurred during trigger save: " + ex.getMessage());
                ex.printStackTrace(System.err);
                return null;
            });

            // Notify the editor
            if (editor != null && editor.isOnline()) {
                editor.sendMessage(ChatUtil.translate("&#00FF00✓ Triggers saved successfully!"));
                editor.sendMessage(ChatUtil.translate("&7➤ " + triggers.size() + " trigger(s) saved"));
                editor.sendMessage(ChatUtil.translate("&7➤ Database: &e" + Main.getInstance().getConfig().getString("DatabaseConfiguration.type")));

                // Details of triggers
                for (TriggerData triggerData : triggers) {
                    if(triggerData instanceof Trigger trigger)
                        editor.sendMessage(ChatUtil.translate("&8  • &f" + trigger.getName() + " &7(" + trigger.getType() + ")"));
                    else {
                        editor.sendMessage(ChatUtil.translate("&8  • &fUnknown Trigger Type"));
                        Main.getInstance().getLogger().warning("&eUnknown TriggerData type: " + triggerData.toString());
                    }
                }
            }

            Main.getInstance().getLogger().info("Save process completed: " + triggers.size() + " triggers");
            return true;

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("&#FF0000An error occurred while saving triggers: " + e.getMessage());
            e.printStackTrace(System.err);

            if (editor != null && editor.isOnline()) {
                editor.sendMessage(ChatUtil.translate("&#FF0000❌ Error while saving!"));
                editor.sendMessage(ChatUtil.translate("&#FF0000" + e.getMessage()));
            }

            return false;
        }
    }

    /**
     * Load triggers as JSON for Blockly
     *
     * EXTENSIBILITY: This method automatically serializes all properties of every Trigger and Action
     * via Gson reflection. No manual property handling needed for new types.
     *
     * @param dungeonName Name of the dungeon
     * @param floorId ID of the floor
     * @return JSON string representing the triggers
     */
    public String loadTriggersAsJson(String dungeonName, String floorId) {
        try {
            List<TriggerData> triggers = Main.getInstance().getDungeonService().getCurrentFloor().getTriggers();

            JsonArray triggersArray = new JsonArray();

            for (TriggerData triggerData : triggers) {
                if(!(triggerData instanceof Trigger trigger))
                    continue;

                if(Main.isDebug()) {
                    Main.getInstance().getLogger().info("Loading trigger: " + trigger.getTriggerId() + " of type: " + trigger.getType());
                }

                JsonObject triggerObj = gson.toJsonTree(trigger, trigger.getClass()).getAsJsonObject();

                // Add metadata that Blockly requires
                triggerObj.addProperty("id", trigger.getTriggerId().toString());
                triggerObj.addProperty("type", trigger.getType());

                // Serialize actions the same way
                JsonArray actionsArray = new JsonArray();
                for (ActionData actionData : trigger.getActions()) {
                    if(!(actionData instanceof Action action)) {
                        Main.getInstance().getLogger().warning("Unknown ActionData type: " + actionData.getClass().getName());
                        continue;
                    }

                    JsonObject actionObj = gson.toJsonTree(action, action.getClass()).getAsJsonObject();
                    actionObj.addProperty("type", action.getType());

                    actionsArray.add(actionObj);
                }
                triggerObj.add("actions", actionsArray);

                triggersArray.add(triggerObj);

                if(Main.isDebug()) {
                    Main.getInstance().getLogger().info("Trigger loaded: " + trigger.getTriggerId() + " with " + trigger.getActions().size() + " actions");
                }
            }

            JsonObject result = new JsonObject();
            result.add("triggers", triggersArray);
            result.addProperty("dungeon", dungeonName);
            result.addProperty("floor", floorId);
            result.addProperty("count", triggers.size());

            return gson.toJson(result);

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("&#FF0000Error loading triggers: " + e.getMessage());
            e.printStackTrace(System.err);

            JsonObject error = new JsonObject();
            error.addProperty("error", e.getMessage());
            return gson.toJson(error);
        }
    }
}
