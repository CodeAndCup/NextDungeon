package fr.perrier.dungeons.spigot.database;

import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.utils.ServerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Manager for saving and loading dungeon triggers in the database.
 */
public class DatabaseTriggersManager {

    /**
     * Saves the triggers for a floor in the database.
     *
     * @param floorId the floor ID
     * @param triggers the list of triggers to save
     * @return a CompletableFuture indicating success or failure
     */
    public static CompletableFuture<Boolean> saveTriggers(String floorId, List<TriggerData> triggers) {
        return Main.getInstance().getDatabaseManager()
                .saveTriggers(floorId, triggers)
                .thenApply(v -> {
                    Main.getLoggerUtil().info("Triggers saved for " + floorId + " in the database");
                    return true;
                })
                .exceptionally(ex -> {
                    Main.getLoggerUtil().severe("Error while saving triggers for " + floorId + ": " + ex.getMessage());
                    ex.printStackTrace(System.err);
                    return false;
                });
    }

    /**
     * Loads the triggers for a floor from the database.
     * This method only loads triggers on lobby servers.
     * On instance servers, triggers are retrieved from Redis via FloorData.
     *
     * @param floorId the floor ID
     * @return the list of triggers, or an empty list if none found or on instance servers
     */
    public static List<TriggerData> loadTriggers(String floorId) {
        // Sur une instance, ne pas charger depuis la BDD - les triggers viennent de Redis
        if (ServerUtil.isInstanceServer()) {
            Main.getLoggerUtil().info("Instance server: skipping DB trigger load for " + floorId + " (using Redis data)");
            return new ArrayList<>();
        }

        // Sur le lobby, charger les triggers depuis la base de données
        try {
            CompletableFuture<List<TriggerData>> future = Main.getInstance().getDatabaseManager().loadTriggers(floorId);
            List<TriggerData> triggers = future.join();

            if (triggers != null && !triggers.isEmpty()) {
                Main.getLoggerUtil().info("Triggers loaded for " + floorId + " (" + triggers.size() + " triggers)");
                return triggers;
            }

            Main.getLoggerUtil().info("No triggers found for " + floorId + " in the database.");
            return new ArrayList<>();

        } catch (Exception e) {
            Main.getLoggerUtil().severe("Error loading triggers for " + floorId + ": " + e.getMessage());
            e.printStackTrace(System.err);
            return new ArrayList<>();
        }
    }

    /**
     * Checks if triggers exist for a floor.
     *
     * @param floorId the floor ID
     * @return true if triggers exist, false otherwise
     */
    public static boolean triggersExist(String floorId) {
        try {
            return Main.getInstance().getDatabaseManager().triggersExist(floorId).join();
        } catch (Exception e) {
            Main.getLoggerUtil().severe("Error checking triggers for " + floorId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes the triggers for a floor from the database.
     *
     * @param floorId the floor ID
     * @return true if deletion was successful, false otherwise
     */
    public static boolean deleteDungeonFile(String floorId) {
        try {
            Main.getInstance().getDatabaseManager().deleteTriggers(floorId).join();
            return true;
        } catch (Exception e) {
            Main.getLoggerUtil().severe("Error deleting triggers for " + floorId + ": " + e.getMessage());
            return false;
        }
    }
}