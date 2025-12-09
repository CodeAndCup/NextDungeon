package fr.perrier.dungeons.spigot.manager;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Gestionnaire pour sauvegarder les triggers des donjons.
 * Utilise maintenant la base de données (MySQL/MongoDB) au lieu des fichiers .dungeon
 */
public class DungeonFileManager {
    private static final String DUNGEON_EXTENSION = ".dungeon";
    private static boolean migrationCompleted = false;

    /**
     * Sauvegarde les triggers d'un floor dans la base de données
     */
    public static CompletableFuture<Boolean> saveTriggers(String floorId, List<Trigger> triggers) {
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
     * Si aucun trigger n'existe en base, tente une migration depuis les anciens fichiers .dungeon
     */
    public static List<Trigger> loadTriggers(String floorId) {
        try {
            // D'abord, vérifier si des triggers existent dans la base de données
            CompletableFuture<Boolean> existsFuture = Main.getInstance().getDatabaseManager().triggersExist(floorId);
            Boolean exists = existsFuture.join();

            if (!exists) {
                // Si pas de données en base, essayer de migrer depuis l'ancien fichier
                List<Trigger> migratedTriggers = migrateFromLegacyFile(floorId);
                if (!migratedTriggers.isEmpty()) {
                    // Sauvegarder les triggers migrés dans la base de données
                    saveTriggers(floorId, migratedTriggers).join();
                    return migratedTriggers;
                }
            }

            // Charger depuis la base de données
            CompletableFuture<List<Trigger>> future = Main.getInstance().getDatabaseManager().loadTriggers(floorId);
            List<Trigger> triggers = future.join();

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

            // Supprimer aussi l'ancien fichier .dungeon s'il existe
            deleteLegacyFile(floorId);

            return true;
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("&cErreur lors de la suppression des triggers pour " + floorId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * @deprecated Cette méthode est conservée pour compatibilité mais utilise maintenant la base de données
     */
    @Deprecated
    public static boolean dungeonFileExists(String floorId) {
        return triggersExist(floorId);
    }

    // ==================== LEGACY MIGRATION METHODS ====================

    /**
     * Migre les triggers depuis un ancien fichier .dungeon vers la base de données
     */
    @SuppressWarnings("unchecked")
    private static List<Trigger> migrateFromLegacyFile(String floorId) {
        String fileName = floorId + DUNGEON_EXTENSION;
        File file = new File(Main.getInstance().getDataFolder() + "/dungeons/", fileName);

        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            List<Trigger> triggers = (List<Trigger>) ois.readObject();
            Main.getInstance().getLogger().info("&aMigration des triggers depuis le fichier legacy pour " + floorId + " (" + triggers.size() + " triggers)");

            // Archiver l'ancien fichier au lieu de le supprimer immédiatement
            archiveLegacyFile(file);

            return triggers;
        } catch (IOException | ClassNotFoundException e) {
            Main.getInstance().getLogger().severe("&cErreur lors de la migration des triggers depuis le fichier pour " + floorId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Archive un fichier .dungeon legacy en le renommant
     */
    private static void archiveLegacyFile(File file) {
        try {
            File archiveFile = new File(file.getParent(), file.getName() + ".migrated");
            if (file.renameTo(archiveFile)) {
                Main.getInstance().getLogger().info("Fichier legacy archivé: " + archiveFile.getName());
            }
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("Impossible d'archiver le fichier legacy: " + e.getMessage());
        }
    }

    /**
     * Supprime un ancien fichier .dungeon
     */
    private static void deleteLegacyFile(String floorId) {
        String fileName = floorId + DUNGEON_EXTENSION;
        File file = new File(Main.getInstance().getDataFolder() + "/dungeons/", fileName);

        if (file.exists()) {
            if (file.delete()) {
                Main.getInstance().getLogger().info("Ancien fichier .dungeon supprimé: " + fileName);
            }
        }
    }

    /**
     * Migre tous les fichiers .dungeon existants vers la base de données
     * Cette méthode doit être appelée au démarrage du plugin
     */
    public static void migrateAllLegacyFiles() {
        if (migrationCompleted) {
            return;
        }

        File dungeonsFolder = new File(Main.getInstance().getDataFolder() + "/dungeons/");
        if (!dungeonsFolder.exists() || !dungeonsFolder.isDirectory()) {
            migrationCompleted = true;
            return;
        }

        File[] dungeonFiles = dungeonsFolder.listFiles((dir, name) -> name.endsWith(DUNGEON_EXTENSION));
        if (dungeonFiles == null || dungeonFiles.length == 0) {
            Main.getInstance().getLogger().info("Aucun fichier legacy .dungeon à migrer");
            migrationCompleted = true;
            return;
        }

        Main.getInstance().getLogger().info("&eDémarrage de la migration de " + dungeonFiles.length + " fichiers .dungeon vers la base de données...");
        int successCount = 0;
        int failCount = 0;

        for (File file : dungeonFiles) {
            String floorId = file.getName().replace(DUNGEON_EXTENSION, "");

            try {
                // Vérifier si déjà migré
                if (Main.getInstance().getDatabaseManager().triggersExist(floorId).join()) {
                    Main.getInstance().getLogger().info("Floor " + floorId + " déjà migré, skip");
                    archiveLegacyFile(file);
                    continue;
                }

                // Charger depuis le fichier
                List<Trigger> triggers = migrateFromLegacyFile(floorId);
                if (!triggers.isEmpty()) {
                    // Sauvegarder dans la base
                    saveTriggers(floorId, triggers).join();
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                Main.getInstance().getLogger().severe("&cErreur lors de la migration de " + floorId + ": " + e.getMessage());
                failCount++;
            }
        }

        Main.getInstance().getLogger().info("&aMigration terminée: " + successCount + " réussies, " + failCount + " échecs");
        migrationCompleted = true;
    }
}