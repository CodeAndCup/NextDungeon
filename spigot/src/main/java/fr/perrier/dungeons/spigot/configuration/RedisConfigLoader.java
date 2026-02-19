package fr.perrier.dungeons.spigot.configuration;

import fr.perrier.dungeons.common.model.dungeon.FloorData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.Dungeon;
import fr.perrier.dungeons.spigot.model.Floor;
import org.redisson.api.RMap;

import java.util.*;

/**
 * Charge les configurations de donjons depuis Redis.
 * Remplace ConfigLoader (YAML) quand DungeonLoader=redis dans config.yml.
 *
 * Stratégie :
 *   - Lit la map "{topic}:floors" (FloorData, module common — compatible Kryo cross-JVM)
 *   - Reconstruit les donjons en groupant les floors par préfixe "dungeonId_floorRawId"
 *   - Appelle updateMap() + generateTemplate() pour chaque floor (comme ConfigLoader)
 *   - Ne touche JAMAIS à "{topic}:dungeons" (contient des objets Kryo Spigot-only)
 */
public class RedisConfigLoader {

    /**
     * Charge tous les floors depuis Redis et génère les templates manquants.
     * Appelé au démarrage du lobby server.
     */
    public static void loadAllDungeonsFromRedis() {
        RMap<String, FloorData> floorsMap = Main.getInstance().getDungeonService().getFloorsMap();

        if (floorsMap.isEmpty()) {
            Main.getLoggerUtil().info("[RedisConfigLoader] Aucun floor trouvé dans Redis. " +
                    "Créez des donjons via le dashboard ou utilisez /dungeon admin migrate-all.");
            return;
        }

        // Grouper les floors par dungeonId
        Map<String, List<FloorData>> byDungeon = new LinkedHashMap<>();
        for (Map.Entry<String, FloorData> entry : floorsMap.entrySet()) {
            String floorId   = entry.getKey();
            FloorData fd     = entry.getValue();
            String dungeonId = extractDungeonId(floorId);
            byDungeon.computeIfAbsent(dungeonId, k -> new ArrayList<>()).add(fd);
        }

        int dungeonCount = 0;
        int floorCount   = 0;

        for (Map.Entry<String, List<FloorData>> entry : byDungeon.entrySet()) {
            String dungeonId         = entry.getKey();
            List<FloorData> floors   = entry.getValue();
            List<Floor> loadedFloors = new ArrayList<>();

            for (FloorData fd : floors) {
                // S'assurer que le dungeonId est bien renseigné (migration depuis anciens YAML)
                if (fd.getDungeonId() == null || fd.getDungeonId().isEmpty()) {
                    fd.setDungeonId(dungeonId);
                }
                Floor floor = loadFloor(fd);
                if (floor != null) {
                    loadedFloors.add(floor);
                    floorCount++;
                }
            }

            // Enregistrer le donjon en mémoire ET dans Redis (dungeonsMap) pour Floor.getFloor() / Dungeon.getDungeon()
            Dungeon dungeon = new Dungeon(dungeonId, dungeonId);
            dungeon.setFloors(loadedFloors); // appelle syncDungeon → écrit dans dungeonsMap
            dungeonCount++;

            Main.getLoggerUtil().info("[RedisConfigLoader] Donjon chargé : " + dungeonId +
                    " (" + loadedFloors.size() + " floor(s))");
        }

        Main.getLoggerUtil().info("[RedisConfigLoader] " + dungeonCount + " donjon(s) et " +
                floorCount + " floor(s) chargés depuis Redis.");
    }

    /**
     * Recharge un floor spécifique depuis Redis.
     * Appelé lors de la réception d'un FLOOR_UPDATE depuis le dashboard.
     *
     * @param floorId l'ID complet du floor (ex: "dungeon1_floor1")
     */
    public static void reloadFloorFromRedis(String floorId) {
        RMap<String, FloorData> floorsMap = Main.getInstance().getDungeonService().getFloorsMap();
        FloorData fd = floorsMap.get(floorId);
        if (fd == null) {
            Main.getLoggerUtil().warning("[RedisConfigLoader] Floor introuvable dans Redis : " + floorId);
            return;
        }
        Floor floor = loadFloor(fd);
        if (floor != null) {
            Main.getLoggerUtil().info("[RedisConfigLoader] Floor rechargé depuis Redis : " + floorId);
        }
    }

    /**
     * Construit un Floor depuis un FloorData, l'enregistre dans Redis (updateMap)
     * et génère le template monde si nécessaire.
     */
    private static Floor loadFloor(FloorData fd) {
        try {
            Floor floor = new Floor(fd);
            // Sync dans Redis (floors + floor_metadata) et notifie les autres serveurs
            floor.updateMap();
            // Génère le template monde si absent
            floor.generateTemplate();
            Main.getLoggerUtil().info("[RedisConfigLoader] Floor chargé : " + fd.getId());
            return floor;
        } catch (Exception e) {
            Main.getLoggerUtil().severe("[RedisConfigLoader] Erreur chargement floor " +
                    fd.getId() + " : " + e.getMessage());
            return null;
        }
    }

    // =========================================================
    //  Migration YAML → Redis (commandes admin)
    // =========================================================

    /**
     * Migre un donjon depuis un fichier YAML vers Redis.
     * Commande : /dungeon admin migrate-to-redis {@literal <}nom{@literal >}
     */
    public static boolean migrateDungeonToRedis(String dungeonName) {
        try {
            Main.getLoggerUtil().info("[RedisConfigLoader] Migration de '" + dungeonName + "' vers Redis...");
            Dungeon dungeon = ConfigLoader.loadDungeon(dungeonName);
            if (dungeon == null) {
                Main.getLoggerUtil().warning("[RedisConfigLoader] Donjon '" + dungeonName + "' introuvable dans les fichiers YAML.");
                return false;
            }

            for (Floor floor : dungeon.getFloors()) {
                // S'assurer que le dungeonId est renseigné avant sync
                if (floor.getDungeonId() == null || floor.getDungeonId().isEmpty()) {
                    floor.setDungeonId(dungeon.getId());
                }
                Main.getInstance().getDungeonService().syncFloor(floor.toFloorData());
                Main.getLoggerUtil().info("[RedisConfigLoader] Floor migré : " + floor.getId());
            }

            Main.getLoggerUtil().info("[RedisConfigLoader] Migration réussie : '" + dungeonName +
                    "' (" + dungeon.getFloors().size() + " floor(s))");
            return true;
        } catch (Exception e) {
            Main.getLoggerUtil().severe("[RedisConfigLoader] Échec migration '" + dungeonName + "' : " + e.getMessage());
            return false;
        }
    }

    /**
     * Migre tous les donjons YAML vers Redis.
     * Commande : /dungeon admin migrate-all
     */
    public static int migrateAllDungeonsToRedis() {
        java.io.File dungeonsDir = new java.io.File(Main.getInstance().getDataFolder(), "dungeons");
        if (!dungeonsDir.isDirectory()) return 0;

        java.io.File[] files = dungeonsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) return 0;

        int count = 0;
        for (java.io.File file : files) {
            String name = file.getName().replace(".yml", "");
            if (migrateDungeonToRedis(name)) count++;
        }
        return count;
    }

    // =========================================================
    //  Helpers
    // =========================================================

    /** Extrait le dungeonId depuis un floorId de format "dungeonId_floorRawId". */
    public static String extractDungeonId(String floorId) {
        int idx = floorId.lastIndexOf("_");
        return idx > 0 ? floorId.substring(0, idx) : floorId;
    }
}



