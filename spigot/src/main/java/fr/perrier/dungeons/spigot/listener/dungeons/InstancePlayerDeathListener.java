package fr.perrier.dungeons.spigot.listener.dungeons;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Objects;

public class InstancePlayerDeathListener implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        FloorInstance instance = Main.getInstance().getRedisStorageService().getCurrentInstance().get();

        FloorInstance.PlayerStats stats = instance.getPlayerStats().get(event.getEntity().getUniqueId());
        if(stats != null)
            stats.incrementDeaths();

        if(instance.getPlayerCurrentLives().containsKey(event.getEntity().getUniqueId()) && instance.getPlayerCurrentLives().get(event.getEntity().getUniqueId()) > 0) {
            instance.getPlayerCurrentLives().compute(event.getEntity().getUniqueId(), (k, currentLives) -> currentLives - 1);
        } else {
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    Objects.requireNonNull(Main.getInstance().getConfig().getString("ServerConfiguration.banCommand"))
                            .replace("{player}", event.getEntity().getName())
                            .replace("{time}", instance.getFloor().getRules().getDeathBanDuration())
                            .replace("{reason}", "Vous avez épuisé toutes vos vies dans l'instance de donjon !")
            );
        }
    }
}
