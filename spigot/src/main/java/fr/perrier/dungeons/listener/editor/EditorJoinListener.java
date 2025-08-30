package fr.perrier.dungeons.listener.editor;

import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.model.Floor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class EditorJoinListener implements Listener {


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Floor floor = Main.getInstance().getRedisStorageService().getCurrentFloor().get();
        player.teleport(floor.getWorldConfig().getSpawn().toLocation());
        player.setGameMode(GameMode.CREATIVE);
    }
}
