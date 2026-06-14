package fr.perrier.dungeons.spigot.listener.dungeons;

import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import fr.perrier.dungeons.common.model.player.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

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
                floor.getWorldConfig().getSpawn().getZ(),
                floor.getWorldConfig().getSpawn().getYaw(),
                floor.getWorldConfig().getSpawn().getPitch()
        );
        player.teleport(spawnLocation);

        FloorInstance instance = Main.getInstance().getDungeonService().getCurrentInstance();

        if(!instance.getPlayers().contains(player.getUniqueId())) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FFCC00Warning: You joined an instance that you were not part of. If you are seeing this message and you are not a part of the player or a staff member, please report this to the staff team as soon as possible."));
        } else {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#00FF00You have joined the instance for floor " + floor.getName() + "."));
            // Initialize player stats and lives if not already present
            if(!instance.getPlayerStats().containsKey(player.getUniqueId())) {
                instance.getPlayerStats().put(player.getUniqueId(), new PlayerStats(player.getUniqueId()));
            }
            if(!instance.getPlayerCurrentLives().containsKey(player.getUniqueId())) {
                instance.getPlayerCurrentLives().put(player.getUniqueId(), floor.getRules().getMaxLives());
            }
            instance.syncInstance();
        }
    }
}
