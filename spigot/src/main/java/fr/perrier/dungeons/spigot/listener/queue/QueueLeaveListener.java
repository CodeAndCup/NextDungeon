package fr.perrier.dungeons.spigot.listener.queue;

import fr.perrier.dungeons.spigot.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener that handles queue cleanup when players disconnect.
 */
public class QueueLeaveListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        if (Main.getInstance().getDungeonQueueService() == null) {
            return;
        }

        // Use the player membership map to remove from only the queues they're in (O(1) lookup)
        for (String floorId : Main.getInstance().getDungeonQueueService().getPlayerQueueFloors(player.getUniqueId())) {
            Main.getInstance().getDungeonQueueService().removeFromQueue(player.getUniqueId(), floorId);
        }
    }
}
