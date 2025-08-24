package fr.perrier.dungeons.manager;

import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.workflow.trigger.Trigger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestionnaire de fichiers .dungeon pour sauvegarder les triggers
 */
public class DungeonFileManager {
    private static final String DUNGEON_EXTENSION = ".dungeon";

    /**
     * Sauvegarde les triggers d'un floor dans un fichier .dungeon
     */
    public static boolean saveTriggers(String floorId, List<Trigger> triggers) {
        String fileName = floorId + DUNGEON_EXTENSION;
        File file = new File("plugins/Dungeons/dungeons/", fileName);

        try {
            file.getParentFile().mkdirs();

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                oos.writeObject(triggers);
                Main.getInstance().getLogger().info("Triggers sauvegardés pour " + floorId);
                return true;
            }
        } catch (IOException e) {
            Main.getInstance().getLogger().severe("Erreur lors de la sauvegarde des triggers: " + e.getMessage());
            return false;
        }
    }

    /**
     * Charge les triggers d'un floor depuis un fichier .dungeon
     */
    @SuppressWarnings("unchecked")
    public static List<Trigger> loadTriggers(String floorId) {
        String fileName = floorId + DUNGEON_EXTENSION;
        File file = new File("plugins/Dungeons/dungeons/", fileName);

        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            List<Trigger> triggers = (List<Trigger>) ois.readObject();
            Main.getInstance().getLogger().info("Triggers chargés pour " + floorId + " (" + triggers.size() + " triggers)");
            return triggers;
        } catch (IOException | ClassNotFoundException e) {
            Main.getInstance().getLogger().severe("Erreur lors du chargement des triggers: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Vérifie si un fichier .dungeon existe
     */
    public static boolean dungeonFileExists(String floorId) {
        String fileName = floorId + DUNGEON_EXTENSION;
        File file = new File("plugins/Dungeons/dungeons/", fileName);
        return file.exists();
    }

    /**
     * Supprime un fichier .dungeon
     */
    public static boolean deleteDungeonFile(String floorId) {
        String fileName = floorId + DUNGEON_EXTENSION;
        File file = new File("plugins/Dungeons/dungeons/", fileName);

        if (file.exists()) {
            return file.delete();
        }
        return true;
    }
}