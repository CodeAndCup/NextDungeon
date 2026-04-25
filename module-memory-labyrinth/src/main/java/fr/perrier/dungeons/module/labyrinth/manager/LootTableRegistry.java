package fr.perrier.dungeons.module.labyrinth.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.perrier.dungeons.module.labyrinth.model.LootTable;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.database.DatabaseManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * In-memory cache of loot tables keyed by floor id.
 *
 * <p>Loaded asynchronously at module enable via the floor id list, then
 * one {@link DatabaseManager#loadLootTable(String)} per floor. Panel
 * edits refresh the cache through {@link #upsert(LootTable)}.</p>
 */
public class LootTableRegistry {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private final Logger logger;
    private final Map<String, LootTable> byFloor = new ConcurrentHashMap<>();

    public LootTableRegistry() {
        this.logger = Main.getInstance().getLogger();
    }

    public CompletableFuture<Void> loadAll() {
        DatabaseManager db = Main.getInstance().getDatabaseManager();
        if (db == null) return CompletableFuture.completedFuture(null);
        return db.listLootTables().thenCompose(floorIds -> {
            if (floorIds == null || floorIds.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            CompletableFuture<?>[] futures = floorIds.stream()
                    .map(floorId -> db.loadLootTable(floorId).thenAccept(json -> {
                        try {
                            LootTable table = GSON.fromJson(json, LootTable.class);
                            if (table != null) {
                                if (table.getFloorId() == null) table.setFloorId(floorId);
                                byFloor.put(table.getFloorId(), table);
                            }
                        } catch (Exception e) {
                            logger.log(Level.WARNING,
                                    "[MemoryLabyrinth] Skipping unparseable loot table " + floorId
                                            + ": " + e.getMessage());
                        }
                    }))
                    .toArray(CompletableFuture[]::new);
            return CompletableFuture.allOf(futures).thenRun(() ->
                    logger.info("[MemoryLabyrinth] Loaded " + byFloor.size() + " loot table(s)"));
        });
    }

    public LootTable getByFloor(String floorId) {
        return floorId == null ? null : byFloor.get(floorId);
    }

    public void upsert(LootTable table) {
        if (table == null || table.getFloorId() == null) return;
        byFloor.put(table.getFloorId(), table);
    }

    public LootTable remove(String floorId) {
        return floorId == null ? null : byFloor.remove(floorId);
    }

    public List<String> floorIds() {
        return List.copyOf(byFloor.keySet());
    }

    public int size() {
        return byFloor.size();
    }

    public void clear() {
        byFloor.clear();
    }
}
