package fr.perrier.dungeons.spigot.listener.dungeons;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import fr.perrier.dungeons.common.model.player.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Listener for player join events in a dungeon instance.
 * Teleports the player to the floor spawn and initializes their stats and lives.
 */
public class InstanceJoinListener implements Listener {

    /**
     * Handles the player join event.
     * Teleports the player to the dungeon spawn and initializes their stats and lives.
     *
     * @param event the player join event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Floor floor = Main.getInstance().getDungeonService().getCurrentFloor();
        Location spawnLocation =  new Location(
                Bukkit.getWorld("world"),
                floor.getWorldConfig().getSpawn().getX(),
                floor.getWorldConfig().getSpawn().getY(),
                floor.getWorldConfig().getSpawn().getZ()
        );
        player.teleport(spawnLocation);

        FloorInstance instance = Main.getInstance().getDungeonService().getCurrentInstance();
        instance.getPlayerStats().put(player.getUniqueId(), new PlayerStats(player.getUniqueId()));

        // Initialize player's lives if not already present
        // This ensures that players rejoining the instance retain their remaining lives from before they left
        instance.getPlayerCurrentLives().putIfAbsent(player.getUniqueId(), instance.getFloor().getRules().getMaxLives());
    }
}
