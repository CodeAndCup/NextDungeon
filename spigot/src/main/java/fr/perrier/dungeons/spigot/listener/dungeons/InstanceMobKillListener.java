package fr.perrier.dungeons.spigot.listener.dungeons;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.common.model.player.PlayerStats;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Listener for mob kill events in a dungeon instance.
 * Increments the killer's enemy kill count in their stats.
 */
public class InstanceMobKillListener implements Listener {

    /**
     * Handles the entity death event.
     * If a player kills a non-player entity, increments their enemies killed stat.
     *
     * @param event the entity death event
     */
    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        Entity entity = event.getEntity();

        if(entity instanceof Player) return;

        if(killer != null) {
            PlayerStats stats = Main.getInstance().getDungeonService().getCurrentInstance()
                    .getPlayerStats().get(killer.getUniqueId());
            if(stats != null)
                stats.incrementEnemiesKilled();
        }
    }
}
