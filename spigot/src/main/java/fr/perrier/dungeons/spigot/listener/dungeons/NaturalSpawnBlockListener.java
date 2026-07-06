package fr.perrier.dungeons.spigot.listener.dungeons;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.EnumSet;
import java.util.Set;

/**
 * Blocks natural mob spawning inside dungeon instance servers.
 *
 * <p>Dungeons are fully scripted — the only mobs that belong in them are the
 * ones spawned on purpose by the Memory Labyrinth module ({@code MobSpawner})
 * and by workflow actions ({@code SummonMobAction}, {@code SummonMobInRegionAction}).
 * Every one of those paths — whether it goes through MythicMobs or the vanilla
 * {@code World#spawnEntity} fallback — produces a {@link CreatureSpawnEvent}
 * with {@link CreatureSpawnEvent.SpawnReason#CUSTOM}. Natural / ambient spawning
 * (the regular spawn cycle, patrols, raids, reinforcements, trial spawners, ...)
 * uses other reasons.</p>
 *
 * <p>Rather than enumerate every "natural" reason — Mojang adds new ones across
 * versions — this works as a whitelist: a spawn is cancelled unless its reason
 * is explicitly intended. That keeps every scripted mob ({@code CUSTOM}) and the
 * handful of deliberate admin/redstone actions, while dropping anything the world
 * tries to spawn on its own. This is registered only on instance servers (see
 * {@code Main#loadInstanceListeners()}), so the lobby/editor are unaffected.</p>
 */
public class NaturalSpawnBlockListener implements Listener {

    /**
     * Spawn reasons that are deliberate, not "natural", and must be allowed:
     * <ul>
     *   <li>{@code CUSTOM} — every dungeon mob (Memory Labyrinth + summon actions,
     *       MythicMobs and vanilla alike).</li>
     *   <li>{@code COMMAND} — an admin {@code /summon} (debugging / setup).</li>
     *   <li>{@code SPAWNER_EGG} / {@code DISPENSE_EGG} — an explicit spawn egg use.</li>
     * </ul>
     */
    private static final Set<CreatureSpawnEvent.SpawnReason> ALLOWED_REASONS = EnumSet.of(
            CreatureSpawnEvent.SpawnReason.CUSTOM,
            CreatureSpawnEvent.SpawnReason.COMMAND,
            CreatureSpawnEvent.SpawnReason.SPAWNER_EGG,
            CreatureSpawnEvent.SpawnReason.DISPENSE_EGG
    );

    /**
     * Cancels any creature spawn whose reason is not explicitly intended.
     *
     * @param event the creature spawn event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!ALLOWED_REASONS.contains(event.getSpawnReason())) {
            event.setCancelled(true);
        }
    }
}
