package fr.perrier.dungeons.module.labyrinth.lifecycle;

import fr.perrier.dungeons.module.labyrinth.model.DifficultyModifier;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthRun;
import fr.perrier.dungeons.module.labyrinth.model.RoomTemplate;
import fr.perrier.dungeons.spigot.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Spawns the mobs declared by a {@link RoomTemplate} when a combat/boss
 * room is entered.
 *
 * <p>Spawned mobs receive two metadata keys :</p>
 * <ul>
 *   <li>{@link #META_INSTANCE_ID} → run instance UUID</li>
 *   <li>{@link #META_ROOM_UUID} → current room UUID (per-entry)</li>
 * </ul>
 *
 * <p>The {@link LabyrinthMobDeathListener} reads those keys at death time
 * to decrement {@code aliveMobsByRoom}.</p>
 *
 * <p>v1 fallback : {@code mobId} is resolved via {@link EntityType#valueOf(String)}.
 * Real MMOCore / MythicMobs integration lands in P7 — see CDC §6.6.</p>
 */
public class MobSpawner {

    public static final String META_INSTANCE_ID = "labyrinth_instance_id";
    public static final String META_ROOM_UUID = "labyrinth_room_uuid";
    public static final String META_TIER = "labyrinth_tier";

    private final Plugin plugin;
    private final Logger logger;

    public MobSpawner() {
        this.plugin = Main.getInstance();
        this.logger = Main.getInstance().getLogger();
    }

    /**
     * Spawn every mob of the run's current room. Returns the count actually
     * spawned (so the lifecycle can initialize {@code aliveMobsByRoom}).
     */
    public int spawnRoomMobs(LabyrinthRun run) {
        if (run == null || run.getCurrentRoom() == null) return 0;
        RoomTemplate room = run.getCurrentRoom();
        if (room.getMobSpawns() == null || room.getMobSpawns().isEmpty()) return 0;

        World world = Bukkit.getWorld(room.getWorldId());
        if (world == null) {
            logger.warning("[MemoryLabyrinth] World not found: " + room.getWorldId() + " — skipping mob spawn");
            return 0;
        }

        DifficultyModifier modifier = run.getCurrentModifier() != null
                ? run.getCurrentModifier() : new DifficultyModifier();

        int spawned = 0;
        for (RoomTemplate.MobSpawn spawn : room.getMobSpawns()) {
            Location at = new Location(world, spawn.getX(), spawn.getY(), spawn.getZ());
            int count = Math.max(1, spawn.getCount());
            for (int i = 0; i < count; i++) {
                Entity entity = spawnSingle(spawn.getMobId(), at, world);
                if (entity == null) continue;
                tagMob(entity, run, modifier.getTier());
                if (entity instanceof LivingEntity living) {
                    applyTierScaling(living, modifier);
                }
                spawned++;
            }
        }
        return spawned;
    }

    /**
     * Resolution order (CDC Q4 = C — hybrid Mythic ref + vanilla fallback) :
     * <ol>
     *   <li>MythicMobs (when the plugin is present and the id is known)</li>
     *   <li>{@link EntityType#valueOf(String)} (so vanilla mob ids stay
     *       usable in test setups without Mythic)</li>
     * </ol>
     */
    private Entity spawnSingle(String mobId, Location at, World world) {
        if (mobId == null || mobId.isEmpty()) return null;
        Optional<Entity> mythic = MythicMobsBridge.spawnMob(mobId, at);
        if (mythic.isPresent()) return mythic.get();
        EntityType type = resolveVanilla(mobId);
        if (type == null) return null;
        return world.spawnEntity(at, type);
    }

    private EntityType resolveVanilla(String mobId) {
        try {
            return EntityType.valueOf(mobId.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("[MemoryLabyrinth] Unknown mobId '" + mobId
                    + "' — neither MythicMobs nor a vanilla EntityType match");
            return null;
        }
    }

    private void tagMob(Entity entity, LabyrinthRun run, int tier) {
        UUID instanceId = run.getInstanceId();
        UUID roomUuid = run.getCurrentRoomUuid();
        if (instanceId != null) {
            entity.setMetadata(META_INSTANCE_ID, new FixedMetadataValue(plugin, instanceId.toString()));
        }
        if (roomUuid != null) {
            entity.setMetadata(META_ROOM_UUID, new FixedMetadataValue(plugin, roomUuid.toString()));
        }
        entity.setMetadata(META_TIER, new FixedMetadataValue(plugin, tier));
    }

    private void applyTierScaling(LivingEntity mob, DifficultyModifier modifier) {
        AttributeInstance maxHealth = mob.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            double base = maxHealth.getBaseValue();
            double scaled = base * modifier.hpMultiplier();
            maxHealth.setBaseValue(scaled);
            mob.setHealth(scaled);
        }
        AttributeInstance attack = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(attack.getBaseValue() * modifier.damageMultiplier());
        }
    }
}
