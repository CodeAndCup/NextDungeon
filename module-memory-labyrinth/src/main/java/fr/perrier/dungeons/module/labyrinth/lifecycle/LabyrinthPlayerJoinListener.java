package fr.perrier.dungeons.module.labyrinth.lifecycle;

import fr.perrier.dungeons.common.model.labyrinth.LabyrinthRoom;
import fr.perrier.dungeons.module.labyrinth.manager.LabyrinthRunManager;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthRun;
import fr.perrier.dungeons.spigot.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Replays the labyrinth room teleport when a player joins after the run
 * has already started.
 *
 * <p>The {@link LabyrinthInstanceReadyListener} fires its run setup on
 * {@code FloorInstanceReadyEvent}, which is dispatched a few hundred ms
 * before the player actually connects to the instance server. At that
 * point {@code Bukkit.getPlayer(uuid)} returns {@code null} and
 * {@link LabyrinthRoomLifecycle#enterRoom} silently skips the teleport.
 * The classic {@code InstanceJoinListener} then teleports the joining
 * player to {@code floor.worldConfig.spawn} — but a labyrinth run's
 * authoritative location is the {@code currentRoom.playerSpawn}.</p>
 *
 * <p>This listener runs at {@link EventPriority#MONITOR} so it observes
 * the join after every other handler has had a chance to react, then
 * schedules a 1-tick delayed teleport to the active labyrinth room. The
 * tick delay lets Bukkit settle the player on the world before the
 * override teleport, avoiding chunk-load races on freshly spawned
 * instances.</p>
 */
public class LabyrinthPlayerJoinListener implements Listener {

    private final LabyrinthRunManager runManager;
    // Pending delayed-teleport tasks keyed by player UUID. Cancelled on
    // quick rejoin or quit so we never run a teleport against a player
    // that has just disconnected.
    private final Map<UUID, BukkitTask> pendingTeleports = new ConcurrentHashMap<>();

    public LabyrinthPlayerJoinListener(LabyrinthRunManager runManager) {
        this.runManager = runManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        LabyrinthRun run = runManager.findRunByPlayer(player.getUniqueId());
        if (run == null) return;

        LabyrinthRoom target = run.getCurrentRoom();
        if (target == null || target.getPlayerSpawn() == null) return;

        String worldId = run.getInstanceWorldId() != null ? run.getInstanceWorldId() : target.getWorldId();
        if (worldId == null) return;
        World world = Bukkit.getWorld(worldId);
        if (world == null) {
            Main.getInstance().getLogger().warning(
                    "[MemoryLabyrinth] World not found on join TP: " + worldId);
            return;
        }
        // v2 procedural: TP to the offset-adjusted location of the pasted copy.
        LabyrinthRoom.Vec3 spawn = target.getPlayerSpawn();
        LabyrinthRoom.Vec3 srcMin = target.getRegion() != null ? target.getRegion().getMin() : null;
        double dx = srcMin != null ? (spawn.getX() - srcMin.getX()) : 0;
        double dy = srcMin != null ? (spawn.getY() - srcMin.getY()) : 0;
        double dz = srcMin != null ? (spawn.getZ() - srcMin.getZ()) : 0;
        Location dst = new Location(world,
                run.getCurrentRoomAnchorX() + dx,
                run.getCurrentRoomAnchorY() + dy,
                run.getCurrentRoomAnchorZ() + dz,
                spawn.getYaw(), spawn.getPitch());

        UUID playerId = player.getUniqueId();
        BukkitTask task = Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            pendingTeleports.remove(playerId);
            if (player.isOnline()) player.teleport(dst);
        }, 1L);
        BukkitTask previous = pendingTeleports.put(playerId, task);
        if (previous != null) previous.cancel();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        BukkitTask task = pendingTeleports.remove(event.getPlayer().getUniqueId());
        if (task != null) task.cancel();
    }

    /** Cancel every pending teleport — called by the module on disable. */
    public void shutdown() {
        for (BukkitTask t : pendingTeleports.values()) t.cancel();
        pendingTeleports.clear();
    }
}
