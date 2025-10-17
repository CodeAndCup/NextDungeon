package fr.perrier.dungeons.spigot.listener.dungeons;

import com.github.juliarn.npclib.api.Position;
import com.github.juliarn.npclib.api.flag.NpcFlag;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class InstancePlayerDeathListener implements Listener {

    @Getter
    private static final List<UUID> ghostPlayers = new ArrayList<>();

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        FloorInstance instance = Main.getInstance().getRedisStorageService().getCurrentInstance().get();

        //TODO: Put player in ghost mode to view if teamate revives them before respawn timer ends and consume a life.
        if(!Main.getInstance().getGhostFactory().isGhost(player)) {
            Location location = player.getLocation();


            Main.getInstance().getGhostFactory().addPlayer(player);
            player.setAllowFlight(true);
            player.setFlying(true);
        }

        FloorInstance.PlayerStats stats = instance.getPlayerStats().get(player.getUniqueId());
        if(stats != null) {
            stats.incrementDeaths();
        }

        if(instance.getPlayerCurrentLives().containsKey(player.getUniqueId()) && instance.getPlayerCurrentLives().get(player.getUniqueId()) > 0) {
            instance.getPlayerCurrentLives().compute(player.getUniqueId(), (k, currentLives) -> currentLives - 1);
        } else {
            Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    Objects.requireNonNull(Main.getInstance().getConfig().getString("ServerConfiguration.banCommand"))
                            .replace("{player}", player.getName())
                            .replace("{time}", instance.getFloor().getRules().getDeathBanDuration())
                            .replace("{reason}", "Vous avez épuisé toutes vos vies dans l'instance de donjon !")
            );
        }
    }
}
