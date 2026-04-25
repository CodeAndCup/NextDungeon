package fr.perrier.dungeons.module.labyrinth.lifecycle;

import fr.perrier.dungeons.module.labyrinth.manager.LabyrinthRunManager;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthRun;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.MetadataValue;

import java.util.List;
import java.util.UUID;

/**
 * Bukkit listener that maps a vanilla {@link EntityDeathEvent} back to a
 * Memory Labyrinth run via the metadata attached by {@link MobSpawner}.
 *
 * <p>Only acts on entities that carry the labyrinth metadata — every other
 * mob death is ignored (the core {@code InstanceMobKillListener} keeps
 * doing its own thing).</p>
 */
public class LabyrinthMobDeathListener implements Listener {

    private final LabyrinthRunManager runManager;

    public LabyrinthMobDeathListener(LabyrinthRunManager runManager) {
        this.runManager = runManager;
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        UUID instanceId = readUuidMeta(entity, MobSpawner.META_INSTANCE_ID);
        UUID roomUuid = readUuidMeta(entity, MobSpawner.META_ROOM_UUID);
        if (instanceId == null || roomUuid == null) return;

        LabyrinthRun run = runManager.getRun(instanceId);
        if (run == null) return;

        runManager.onMobDeath(run, roomUuid);
    }

    private static UUID readUuidMeta(Entity entity, String key) {
        List<MetadataValue> values = entity.getMetadata(key);
        if (values == null || values.isEmpty()) return null;
        try {
            return UUID.fromString(values.get(0).asString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
