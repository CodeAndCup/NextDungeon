package fr.perrier.dungeons.module.labyrinth.manager;

import fr.perrier.dungeons.common.model.labyrinth.LootTable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-instance loot table cache.
 *
 * <p>R3 redesign — loot tables now live inline inside each floor's
 * {@code LabyrinthFloorConfig} ; the run manager pushes the table into
 * this registry at start-of-run and releases it at end-of-run. No more
 * boot-time DB scan, no more dedicated DB table.</p>
 */
public class LootTableRegistry {

    private final Map<UUID, LootTable> byInstance = new ConcurrentHashMap<>();

    public void load(UUID instanceId, LootTable table) {
        if (instanceId == null || table == null) return;
        byInstance.put(instanceId, table);
    }

    public void release(UUID instanceId) {
        if (instanceId == null) return;
        byInstance.remove(instanceId);
    }

    public LootTable getByInstance(UUID instanceId) {
        return instanceId == null ? null : byInstance.get(instanceId);
    }

    public int activeTables() {
        return byInstance.size();
    }

    public void clear() {
        byInstance.clear();
    }
}
