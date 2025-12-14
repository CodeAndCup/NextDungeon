package fr.perrier.dungeons.spigot.manager;

import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;

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
     * Charge les triggers d'un floor depuis la base de données
     */
    public static List<TriggerData> loadTriggers(String floorId) {
        try {
            CompletableFuture<List<TriggerData>> future = Main.getInstance().getDatabaseManager().loadTriggers(floorId);
            List<TriggerData> triggers = future.join();

            if (triggers != null && !triggers.isEmpty()) {
                Main.getInstance().getLogger().info("Triggers chargés pour " + floorId + " (" + triggers.size() + " triggers)");
                return triggers;
            }

            Main.getInstance().getLogger().warning("&eAucun trigger trouvé pour " + floorId);
            return new ArrayList<>();

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("&cErreur lors du chargement des triggers pour " + floorId + ": " + e.getMessage());
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
            Main.getInstance().getLogger().severe("&cErreur lors de la vérification des triggers pour " + floorId + ": " + e.getMessage());
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
            Main.getInstance().getLogger().severe("&cErreur lors de la suppression des triggers pour " + floorId + ": " + e.getMessage());
            return false;
        }
    }
}