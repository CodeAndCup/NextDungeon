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


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Floor floor = Main.getInstance().getRedisStorageService().getCurrentFloor().get();
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
