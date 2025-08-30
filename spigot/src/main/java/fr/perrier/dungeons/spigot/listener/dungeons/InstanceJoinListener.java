package fr.perrier.dungeons.spigot.listener.dungeons;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class InstanceJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Floor floor = Main.getInstance().getRedisStorageService().getCurrentFloor().get();
        player.teleport(floor.getWorldConfig().getSpawn().toLocation());

        FloorInstance instance = Main.getInstance().getRedisStorageService().getCurrentInstance().get();
        instance.getPlayerStats().put(player.getUniqueId(), new FloorInstance.PlayerStats(player.getUniqueId()));
    }
}
