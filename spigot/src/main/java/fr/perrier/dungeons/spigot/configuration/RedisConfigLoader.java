package fr.perrier.dungeons.spigot.configuration;

import fr.perrier.dungeons.common.model.dungeon.FloorData;
import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.database.DatabaseManager;
import fr.perrier.dungeons.spigot.database.DatabaseTriggersManager;
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
     * Bootstraps the in-memory floor cache. Priority order:
     * <ol>
     *   <li>Redis map (fast path when dashboard already populated it).</li>
     *   <li>Database via {@link DatabaseManager#getAllFloors(int)} when Redis is empty.
     *       After loading, Redis is rebuilt so later reads hit the fast path.</li>
     * </ol>
     */
    public static void loadAllDungeonsFromRedis() {
        RMap<String, FloorData> floorsMap = Main.getInstance().getDungeonService().getFloorsMap();
        DatabaseManager dbManager = Main.getInstance().getDatabaseManager();

        Map<String, List<FloorData>> byDungeon;
        if (floorsMap.isEmpty()) {
            Main.getLoggerUtil().warning("[RedisConfigLoader] Redis floors map vide — fallback BDD.");
            Map<String, List<FloorData>> fromDb = loadFloorsFromDatabase(dbManager);
            if (fromDb.isEmpty()) {
                Main.getLoggerUtil().info("[RedisConfigLoader] Aucun floor en BDD non plus. " +
                        "Créez des donjons via le dashboard.");
                return;
            }
            rebuildRedisFromDatabase(fromDb, floorsMap);
            byDungeon = fromDb;
        } else {
            byDungeon = new LinkedHashMap<>();
            int healed = 0;
            for (Map.Entry<String, FloorData> entry : floorsMap.entrySet()) {
                String floorId   = entry.getKey();
                FloorData fd     = entry.getValue();
                String dungeonId = fd.getDungeonId() != null ? fd.getDungeonId() : extractDungeonId(floorId);
                // Heal legacy floors that have been sitting in Redis since before the versioning migration.
                // Without this, subsequent boots skip the DB-fallback heal path and the monitor warns forever.
                if (fd.getChecksum() == null || fd.getChecksum().isEmpty()) {
                    if (fd.getDungeonId() == null) fd.setDungeonId(dungeonId);
                    if (fd.getUpdatedAt() <= 0L) fd.setUpdatedAt(System.currentTimeMillis());
                    if (fd.getUpdatedBy() == null) fd.setUpdatedBy("legacy-heal");
                    fd.setChecksum(fd.calculateChecksum());
                    floorsMap.fastPut(floorId, fd);
                    if (dbManager != null) {
                        try {
                            dbManager.saveFloor(floorId, dungeonId, fd).get();
                        } catch (Exception persistError) {
                            Main.getLoggerUtil().warning("[RedisConfigLoader] Could not persist healed checksum for "
                                    + floorId + ": " + persistError.getMessage());
                        }
                    }
                    healed++;
                }
                byDungeon.computeIfAbsent(dungeonId, k -> new ArrayList<>()).add(fd);
            }
            if (healed > 0) {
                Main.getLoggerUtil().info("[RedisConfigLoader] " + healed
                        + " floor(s) legacy réparé(s) (checksum recalculé + persisté).");
            }
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
                floorCount + " floor(s) chargés.");
    }

    /**
     * Reads every floor from the database and groups them by {@code dungeonId}.
     * Uses {@link DatabaseManager#getAllFloors(int)} which iterates the result set
     * so we never hold all rows as a materialised map in memory.
     * <p>
     * Legacy rows (predating the versioning migration) may have {@code checksum IS NULL}.
     * We self-heal them here: compute the missing checksum on the in-memory object and
     * persist it back via {@link DatabaseManager#saveFloor(String, String, FloorData)} so
     * that subsequent health checks pass and future loads are O(1).
     */
    static Map<String, List<FloorData>> loadFloorsFromDatabase(DatabaseManager dbManager) {
        Map<String, List<FloorData>> grouped = new LinkedHashMap<>();
        if (dbManager == null) {
            Main.getLoggerUtil().severe("[RedisConfigLoader] DatabaseManager unavailable — cannot fallback");
            return grouped;
        }
        try {
            List<FloorData> all = dbManager.getAllFloors(0).get();
            int healed = 0;
            for (FloorData fd : all) {
                String dungeonId = fd.getDungeonId() != null ? fd.getDungeonId() : extractDungeonId(fd.getId());
                if (fd.getDungeonId() == null) fd.setDungeonId(dungeonId);
                if (fd.getChecksum() == null || fd.getChecksum().isEmpty()) {
                    if (fd.getUpdatedAt() <= 0L) fd.setUpdatedAt(System.currentTimeMillis());
                    if (fd.getUpdatedBy() == null) fd.setUpdatedBy("legacy-heal");
                    fd.setChecksum(fd.calculateChecksum());
                    healed++;
                    try {
                        dbManager.saveFloor(fd.getId(), dungeonId, fd).get();
                    } catch (Exception persistError) {
                        Main.getLoggerUtil().warning("[RedisConfigLoader] Could not persist healed checksum for "
                                + fd.getId() + ": " + persistError.getMessage());
                    }
                }
                grouped.computeIfAbsent(dungeonId, k -> new ArrayList<>()).add(fd);
            }
            Main.getLoggerUtil().info("[RedisConfigLoader] " + all.size() + " floor(s) chargés depuis la BDD"
                    + (healed > 0 ? " (" + healed + " checksum(s) legacy régénéré(s))." : "."));
        } catch (Exception e) {
            Main.getLoggerUtil().severe("[RedisConfigLoader] Échec du chargement BDD : " + e.getMessage());
        }
        return grouped;
    }

    /**
     * Re-populates the Redis floor map from data freshly loaded from the database.
     */
    static void rebuildRedisFromDatabase(Map<String, List<FloorData>> floorsFromDb,
                                         RMap<String, FloorData> floorsMap) {
        int written = 0;
        for (List<FloorData> floors : floorsFromDb.values()) {
            for (FloorData fd : floors) {
                try {
                    floorsMap.fastPut(fd.getId(), fd);
                    written++;
                    if (written % 50 == 0) {
                        Main.getLoggerUtil().info("[RedisConfigLoader] Redis rebuild: " + written + " floor(s)");
                    }
                } catch (Exception e) {
                    Main.getLoggerUtil().warning("[RedisConfigLoader] Impossible d'écrire " + fd.getId()
                            + " dans Redis : " + e.getMessage());
                }
            }
        }
        Main.getLoggerUtil().info("[RedisConfigLoader] Redis rebuild terminé (" + written + " floor(s)).");
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
     * Les triggers sont toujours chargés depuis la BDD afin de ne pas écraser
     * les triggers existants lors d'un rechargement Redis.
     */
    private static Floor loadFloor(FloorData fd) {
        try {
            Floor floor = new Floor(fd);

            // Charger les triggers depuis la BDD (source de vérité pour les triggers)
            List<TriggerData> triggers =
                    DatabaseTriggersManager.loadTriggers(fd.getId());
            if (!triggers.isEmpty()) {
                floor.setTriggers(triggers);
                Main.getLoggerUtil().info("[RedisConfigLoader] " + triggers.size() +
                        " trigger(s) chargé(s) depuis la BDD pour : " + fd.getId());
            }

            // Re-synchroniser vers Redis avec les triggers inclus
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

