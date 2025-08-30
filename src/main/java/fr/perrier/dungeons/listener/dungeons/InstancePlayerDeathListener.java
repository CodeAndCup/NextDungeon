package fr.perrier.dungeons.listener.dungeons;

import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.model.FloorInstance;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class InstancePlayerDeathListener implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        FloorInstance instance = Main.getInstance().getRedisStorageService().getCurrentInstance().get();

        FloorInstance.PlayerStats stats = instance.getPlayerStats().get(event.getEntity().getUniqueId());
        if(stats != null)
            stats.incrementDeaths();
    }
}
