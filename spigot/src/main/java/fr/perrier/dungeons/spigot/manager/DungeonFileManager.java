package fr.perrier.dungeons.spigot.manager;

import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.utils.ServerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Gestionnaire pour sauvegarder les triggers des donjons dans la base de données.
 */
public class DungeonFileManager {

    /**
     * Sauvegarde les triggers d'un floor dans la base de données
     */
    public static CompletableFuture<Boolean> saveTriggers(String floorId, List<TriggerData> triggers) {
        return Main.getInstance().getDatabaseManager()
                .saveTriggers(floorId, triggers)
                .thenApply(v -> {
                    Main.getInstance().getLogger().info("Triggers sauvegardés pour " + floorId + " dans la base de données");
                    return true;
                })
                .exceptionally(ex -> {
                    Main.getInstance().getLogger().severe("&cErreur lors de la sauvegarde des triggers pour " + floorId + ": " + ex.getMessage());
                    ex.printStackTrace();
                    return false;
                });
    }

    /**
     * Charge les triggers d'un floor depuis la base de données.
     * Cette méthode ne charge les triggers que sur les serveurs lobby.
     * Sur les instances, les triggers sont récupérés depuis Redis via FloorData.
     */
    public static List<TriggerData> loadTriggers(String floorId) {
        // Sur une instance, ne pas charger depuis la BDD - les triggers viennent de Redis
        if (ServerUtil.isInstanceServer()) {
            Main.getInstance().getLogger().info("Instance server: skipping DB trigger load for " + floorId + " (using Redis data)");
            return new ArrayList<>();
        }

        // Sur le lobby, charger les triggers depuis la base de données
        try {
            CompletableFuture<List<TriggerData>> future = Main.getInstance().getDatabaseManager().loadTriggers(floorId);
            List<TriggerData> triggers = future.join();

            if (triggers != null && !triggers.isEmpty()) {
                Main.getInstance().getLogger().info("Triggers loaded for " + floorId + " (" + triggers.size() + " triggers)");
                return triggers;
            }

            Main.getInstance().getLogger().info("No triggers found for " + floorId + " in the database.");
            return new ArrayList<>();

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("&cError loading triggers for " + floorId + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Vérifie si des triggers existent pour un floor
     */
    public static boolean triggersExist(String floorId) {
        try {
            return Main.getInstance().getDatabaseManager().triggersExist(floorId).join();
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("&cError checking triggers for " + floorId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Supprime les triggers d'un floor de la base de données
     */
    public static boolean deleteDungeonFile(String floorId) {
        try {
            Main.getInstance().getDatabaseManager().deleteTriggers(floorId).join();
            return true;
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("&cError deleting triggers for " + floorId + ": " + e.getMessage());
            return false;
        }
    }
}