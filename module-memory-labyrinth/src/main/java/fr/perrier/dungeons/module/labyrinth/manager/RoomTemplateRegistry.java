package fr.perrier.dungeons.module.labyrinth.manager;

import fr.perrier.dungeons.common.model.labyrinth.LabyrinthDungeonConfig;
import fr.perrier.dungeons.common.model.labyrinth.LabyrinthRoom;
import fr.perrier.dungeons.common.model.labyrinth.RoomType;
import fr.perrier.dungeons.spigot.Main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Per-instance room pool registry.
 *
 * <p>Pools are sourced from each run's {@link LabyrinthDungeonConfig}
 * (loaded by the run manager at start-of-run from the dashboard-edited
 * dungeon payload — R3 redesign). No more boot-time DB scan ; each
 * active labyrinth instance gets its own immutable snapshot of the
 * dungeon's shared room pool.</p>
 */
public class RoomTemplateRegistry {

    private final Logger logger;
    private final Map<UUID, Map<RoomType, List<LabyrinthRoom>>> byInstance = new ConcurrentHashMap<>();

    public RoomTemplateRegistry() {
        this.logger = Main.getInstance().getLogger();
    }

    /**
     * Build and cache the pool for a starting instance. Indexes the
     * dungeon's shared room list by {@link RoomType} so the picker can
     * grab candidates in O(1) per type.
     */
    public void load(UUID instanceId, LabyrinthDungeonConfig config) {
        if (instanceId == null || config == null || config.getRooms() == null) return;
        Map<RoomType, List<LabyrinthRoom>> pool = new EnumMap<>(RoomType.class);
        for (RoomType t : RoomType.values()) pool.put(t, new ArrayList<>());
        for (LabyrinthRoom room : config.getRooms()) {
            if (room == null || room.getType() == null) continue;
            pool.get(room.getType()).add(room);
        }
        byInstance.put(instanceId, pool);
        sanityCheck(instanceId, pool);
    }

    /**
     * Drop the pool of a finished/cancelled instance.
     */
    public void release(UUID instanceId) {
        if (instanceId == null) return;
        byInstance.remove(instanceId);
    }

    public List<LabyrinthRoom> getByType(UUID instanceId, RoomType type) {
        Map<RoomType, List<LabyrinthRoom>> pool = byInstance.get(instanceId);
        if (pool == null) return List.of();
        return Collections.unmodifiableList(pool.getOrDefault(type, List.of()));
    }

    /**
     * Returns rooms of {@code type} that carry every tag in {@code requiredTags}.
     * Empty {@code requiredTags} → behaves like {@link #getByType(UUID, RoomType)}.
     */
    public List<LabyrinthRoom> getByTypeAndTags(UUID instanceId, RoomType type, List<String> requiredTags) {
        List<LabyrinthRoom> base = getByType(instanceId, type);
        if (requiredTags == null || requiredTags.isEmpty()) return base;
        List<LabyrinthRoom> out = new ArrayList<>();
        for (LabyrinthRoom r : base) {
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

    public boolean hasInstance(UUID instanceId) {
        return instanceId != null && byInstance.containsKey(instanceId);
    }

    public int activePools() {
        return byInstance.size();
    }

    public void clear() {
        byInstance.clear();
    }

    private void sanityCheck(UUID instanceId, Map<RoomType, List<LabyrinthRoom>> pool) {
        int combat = pool.getOrDefault(RoomType.COMBAT, List.of()).size();
        int boss = pool.getOrDefault(RoomType.BOSS, List.of()).size();
        int lobby = pool.getOrDefault(RoomType.LOBBY, List.of()).size();
        if (lobby == 0) {
            logger.warning("[MemoryLabyrinth] Instance " + instanceId + " has no LOBBY room — run cannot start");
        }
        if (boss == 0) {
            logger.warning("[MemoryLabyrinth] Instance " + instanceId + " has no BOSS room — boss encounters will fail");
        }
        if (combat < 8) {
            logger.warning("[MemoryLabyrinth] Instance " + instanceId + " has " + combat
                    + " COMBAT rooms — repetition will be visible (recommend 8+)");
        }
    }
}
