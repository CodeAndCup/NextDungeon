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

        // Remove player from all queues they might be in
        for (String floorId : Main.getInstance().getDungeonQueueService().getActiveQueueFloors()) {
            Main.getInstance().getDungeonQueueService().removeFromQueue(player.getUniqueId(), floorId);
        }
    }
}
