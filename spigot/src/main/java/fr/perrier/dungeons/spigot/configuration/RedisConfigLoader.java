package fr.perrier.dungeons.spigot.configuration;

import fr.perrier.dungeons.common.model.dungeon.FloorData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.Dungeon;
import fr.perrier.dungeons.spigot.model.Floor;
import org.redisson.api.RMap;

import java.util.*;

/**
 * Charge les configurations de donjons depuis Redis.
 * Tous les donjons sont créés et gérés via le dashboard web (proxy).
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
                    "Créez des donjons via le dashboard.");
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
                if (fd.getDungeonId() == null || fd.getDungeonId().isEmpty()) {
                    fd.setDungeonId(dungeonId);
                }
                Floor floor = loadFloor(fd);
                if (floor != null) {
                    loadedFloors.add(floor);
                    floorCount++;
                }
            }

            Dungeon dungeon = new Dungeon(dungeonId, dungeonId);
            dungeon.setFloors(loadedFloors);
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
            floor.updateMap();
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
    //  Helpers
    // =========================================================

    /** Extrait le dungeonId depuis un floorId de format "dungeonId_floorRawId". */
    public static String extractDungeonId(String floorId) {
        int idx = floorId.lastIndexOf("_");
        return idx > 0 ? floorId.substring(0, idx) : floorId;
    }
}

