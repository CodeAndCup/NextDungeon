package fr.perrier.dungeons.module.labyrinth.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.perrier.dungeons.module.labyrinth.model.RoomTemplate;
import fr.perrier.dungeons.module.labyrinth.model.RoomType;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.database.DatabaseManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * In-memory registry of all {@link RoomTemplate} loaded from the host database.
 *
 * <p>Loaded asynchronously at module enable via
 * {@link DatabaseManager#listLabyrinthRooms()}. Subsequent admin edits
 * coming from the web panel can refresh the registry through
 * {@link #upsert(RoomTemplate)} / {@link #remove(String)}.</p>
 *
 * <p>Three indexes are maintained for efficient picker lookup :
 * by id, by {@link RoomType}, and by tag.</p>
 */
public class RoomTemplateRegistry {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private final Logger logger;
    private final Map<String, RoomTemplate> byId = new ConcurrentHashMap<>();
    private final Map<RoomType, List<RoomTemplate>> byType = new EnumMap<>(RoomType.class);
    private final Map<String, List<RoomTemplate>> byTag = new ConcurrentHashMap<>();

    public RoomTemplateRegistry() {
        this.logger = Main.getInstance().getLogger();
        for (RoomType type : RoomType.values()) {
            byType.put(type, new ArrayList<>());
        }
    }

    /**
     * Load every persisted room template into memory.
     *
     * <p>Returns the future from the underlying DAO so the module can chain
     * {@link CompletableFuture#thenRun(Runnable)} for follow-up wiring.</p>
     */
    public CompletableFuture<Void> loadAll() {
        DatabaseManager db = Main.getInstance().getDatabaseManager();
        if (db == null) {
            logger.warning("[MemoryLabyrinth] DatabaseManager not ready — skipping room pool load");
            return CompletableFuture.completedFuture(null);
        }
        return db.listLabyrinthRooms().thenAccept(rows -> {
            int loaded = 0;
            int skipped = 0;
            for (String[] row : rows) {
                String id = row[0];
                String payload = row[3];
                try {
                    RoomTemplate room = GSON.fromJson(payload, RoomTemplate.class);
                    if (room == null) {
                        skipped++;
                        continue;
                    }
                    if (room.getId() == null || room.getId().isEmpty()) {
                        room.setId(id);
                    }
                    indexInPlace(room);
                    loaded++;
                } catch (Exception e) {
                    logger.log(Level.WARNING,
                            "[MemoryLabyrinth] Skipping unparseable room template " + id + ": " + e.getMessage());
                    skipped++;
                }
            }
            logger.info("[MemoryLabyrinth] Room pool loaded: " + loaded + " ok, " + skipped + " skipped");
            sanityCheck();
        });
    }

    /**
     * Insert or update a room template in the registry. Does not persist
     * to DB — the panel does that.
     */
    public synchronized void upsert(RoomTemplate room) {
        if (room == null || room.getId() == null) return;
        RoomTemplate previous = byId.put(room.getId(), room);
        if (previous != null) {
            removeFromIndexes(previous);
        }
        indexInPlace(room);
    }

    /**
     * Drop a room template from the registry. Returns the removed entry or null.
     */
    public synchronized RoomTemplate remove(String roomId) {
        if (roomId == null) return null;
        RoomTemplate previous = byId.remove(roomId);
        if (previous != null) removeFromIndexes(previous);
        return previous;
    }

    public RoomTemplate getById(String id) {
        return id == null ? null : byId.get(id);
    }

    /**
     * Returns an unmodifiable view of every room of the given type.
     */
    public List<RoomTemplate> getByType(RoomType type) {
        return Collections.unmodifiableList(byType.getOrDefault(type, List.of()));
    }

    /**
     * Returns rooms of {@code type} that have *all* the {@code requiredTags}.
     * If {@code requiredTags} is empty, behaves like {@link #getByType(RoomType)}.
     */
    public List<RoomTemplate> getByTypeAndTags(RoomType type, List<String> requiredTags) {
        List<RoomTemplate> base = byType.getOrDefault(type, List.of());
        if (requiredTags == null || requiredTags.isEmpty()) {
            return Collections.unmodifiableList(base);
        }
        List<RoomTemplate> out = new ArrayList<>();
        for (RoomTemplate r : base) {
            List<String> tags = r.getTags();
            if (tags == null) continue;
            boolean ok = true;
            for (String required : requiredTags) {
                if (!tags.contains(required)) { ok = false; break; }
            }
            if (ok) out.add(r);
        }
        return out;
    }

    public int size() {
        return byId.size();
    }

    public void clear() {
        byId.clear();
        for (List<RoomTemplate> list : byType.values()) list.clear();
        byTag.clear();
    }

    private void indexInPlace(RoomTemplate room) {
        byId.put(room.getId(), room);
        if (room.getType() != null) {
            byType.computeIfAbsent(room.getType(), k -> new ArrayList<>()).add(room);
        }
        if (room.getTags() != null) {
            for (String tag : room.getTags()) {
                if (tag == null || tag.isEmpty()) continue;
                byTag.computeIfAbsent(tag, k -> new ArrayList<>()).add(room);
            }
        }
    }

    private void removeFromIndexes(RoomTemplate room) {
        if (room.getType() != null) {
            List<RoomTemplate> list = byType.get(room.getType());
            if (list != null) list.removeIf(r -> r.getId().equals(room.getId()));
        }
        if (room.getTags() != null) {
            for (String tag : room.getTags()) {
                List<RoomTemplate> list = byTag.get(tag);
                if (list != null) list.removeIf(r -> r.getId().equals(room.getId()));
            }
        }
    }

    /**
     * Warns when the pool is too thin to avoid repetition (CDC §8 — at least
     * 8 distinct combat rooms per tier recommended).
     */
    private void sanityCheck() {
        int combat = byType.getOrDefault(RoomType.COMBAT, List.of()).size();
        int boss = byType.getOrDefault(RoomType.BOSS, List.of()).size();
        int lobby = byType.getOrDefault(RoomType.LOBBY, List.of()).size();
        if (lobby == 0) {
            logger.warning("[MemoryLabyrinth] No LOBBY rooms in pool — runs cannot start");
        }
        if (boss == 0) {
            logger.warning("[MemoryLabyrinth] No BOSS rooms in pool — boss encounters will fail");
        }
        if (combat < 8) {
            logger.warning("[MemoryLabyrinth] Only " + combat + " COMBAT rooms in pool — repetition will be visible (recommend 8+)");
        }
    }
}
