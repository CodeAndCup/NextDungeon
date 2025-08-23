package fr.perrier.dungeons.listener;

import fr.perrier.dungeons.Main;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.UUID;

public class LeaveListener implements Listener {

    @Getter
    private static final HashMap<UUID, BukkitTask> waitingApprovalSaveTasks = new HashMap<>();

    /**
     * When a player leaves, we schedule a task to save their profile data after 2 minutes.
     * This allows for a grace period in case the player is switching servers, preventing unnecessary database
     * writes.
     * @param event the PlayerQuitEvent
     */
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        BukkitTask task = Bukkit.getScheduler().runTaskLaterAsynchronously(Main.getInstance(), () -> {
            Main.getInstance().getProfileService().saveProfileData(player.getUniqueId());
            waitingApprovalSaveTasks.remove(player.getUniqueId());
        }, 20 * 60L * 2);
        waitingApprovalSaveTasks.put(player.getUniqueId(), task);
    }
}

