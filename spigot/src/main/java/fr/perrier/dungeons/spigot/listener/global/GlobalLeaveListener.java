package fr.perrier.dungeons.spigot.listener.global;

import fr.perrier.dungeons.spigot.Main;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.UUID;

public class GlobalLeaveListener implements Listener {

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

        if(Main.getInstance().getVariableRegistry() != null)
            Main.getInstance().getVariableRegistry().clearPlayerVariables(player);

        if( Main.getInstance().getTriggersRegistry() != null)
            Main.getInstance().getTriggersRegistry().cleanupPlayer(player.getUniqueId());

        BukkitTask task = Bukkit.getScheduler().runTaskLaterAsynchronously(Main.getInstance(), () -> {
            Main.getInstance().getProfileService().saveProfileData(player.getUniqueId());
            waitingApprovalSaveTasks.remove(player.getUniqueId());
        }, 20 * 60L * 2);
        waitingApprovalSaveTasks.put(player.getUniqueId(), task);
    }
}

