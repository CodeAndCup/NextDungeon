package fr.perrier.dungeons.spigot.listener.dungeons;

import com.cryptomorin.xseries.messages.Titles;
import com.github.unldenis.corpse.api.CorpseAPI;
import com.github.unldenis.corpse.corpse.Corpse;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.*;

public class InstancePlayerDeathListener implements Listener {

    @Getter
    private static final Map<UUID,GhostData> GHOST_DATA = new HashMap<>();

    @Setter
    @Getter
    @AllArgsConstructor
    private static class GhostData {
        private final UUID playerUUID;
        private final Location deathLocation;
        private int timeLeftAsGhost;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        player.setRespawnLocation(player.getLocation().clone().add(0,2,0));

        //TODO: Put player in ghost mode to view if teamate revives them before respawn timer ends and consume a life.
        if(!Main.getInstance().getGhostFactory().isGhost(player)) {
            GHOST_DATA.put(player.getUniqueId(), new GhostData(player.getUniqueId(), player.getLocation(), 15));

            Corpse corpse = CorpseAPI.getInstance().spawnCorpse(player);
            Main.getInstance().getGhostFactory().addPlayer(player);
            player.setAllowFlight(true);
            player.setFlying(true);
            player.setInvulnerable(true);

            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () ->
                    player.teleport(player.getLocation().clone().add(0,2,0))
            , 10L);

            Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), task -> {
                GhostData data = GHOST_DATA.get(player.getUniqueId());
                if(data != null) {
                    data.setTimeLeftAsGhost(data.getTimeLeftAsGhost() - 1);
                    Titles.sendTitle(player, 0, 20, 0,
                            ChatUtil.toSmallCaps(ChatUtil.translate("&7You are a ghost!")),
                            ChatUtil.toSmallCaps(ChatUtil.translate("&fTime before respawn &#8B0000" + data.getTimeLeftAsGhost() + "s"))
                    );
                    if(data.getTimeLeftAsGhost() <= 0) {
                        Main.getInstance().getGhostFactory().removePlayer(player);
                        player.setAllowFlight(false);
                        player.setFlying(false);
                        player.setInvulnerable(false);
                        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                            player.teleport(GHOST_DATA.get(player.getUniqueId()).getDeathLocation());
                            GHOST_DATA.remove(player.getUniqueId());
                            applyDeathTo(player);
                            CorpseAPI.getInstance().removeCorpse(corpse);
                        });
                        task.cancel();
                    }
                }
            }, 20L, 20L);
        }
    }

    private void applyDeathTo(Player player) {
        FloorInstance instance = Main.getInstance().getRedisStorageService().getCurrentInstance().get();

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
