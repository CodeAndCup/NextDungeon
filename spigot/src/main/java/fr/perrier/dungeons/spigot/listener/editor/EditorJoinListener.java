package fr.perrier.dungeons.spigot.listener.editor;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.Floor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class EditorJoinListener implements Listener {


    /**
     * When a player joins the server, teleport them to the current floor's spawn location
     * and set their game mode to creative.
     *
     * @param event The PlayerJoinEvent triggered when a player joins the server.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Floor floor = Main.getInstance().getDungeonService().getCurrentFloor();
        Location spawnLocation = new Location(
                Bukkit.getWorld("world"),
                floor.getWorldConfig().getSpawn().getX(),
                floor.getWorldConfig().getSpawn().getY(),
                floor.getWorldConfig().getSpawn().getZ()
        );
        player.teleport(spawnLocation);
        player.setGameMode(GameMode.CREATIVE);
    }
}
