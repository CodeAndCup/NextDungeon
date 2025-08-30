package fr.perrier.dungeons.listener.dungeons;

import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.model.FloorInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class InstanceMobKillListener implements Listener {

    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        Entity entity = event.getEntity();

        if(entity instanceof Player) return;

        if(killer != null) {
            FloorInstance.PlayerStats stats = Main.getInstance().getRedisStorageService().getCurrentInstance().get()
                    .getPlayerStats().get(killer.getUniqueId());
            if(stats != null)
                stats.incrementEnemiesKilled();
        }
    }
}
