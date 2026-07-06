package fr.perrier.dungeons.spigot.listener.global;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.messaging.packets.PlayerSwitchServerPacket;
import fr.perrier.dungeons.spigot.model.ProfileData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class GlobalJoinListener implements Listener {

    /**
     * When a player joins, we check if we have cached profile data for them.
     * If we do, it means they are switching servers, so we send a packet to
     * notify other servers and avoid saving profile data in the database.
     * If we don't, we load their profile data from the database and cache it.
     *
     * @param event the PlayerJoinEvent
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if(Main.getInstance().getProfileService().hasCachedProfileData(player.getUniqueId())) {
            Main.getInstance().getMessaging().sendPacket(new PlayerSwitchServerPacket(event.getPlayer().getUniqueId()));
        } else {
            ProfileData profileData = Main.getInstance().getProfileService().getProfileData(player.getUniqueId());
            if(profileData.getDisplayName().isEmpty()) {
                profileData.setDisplayName(player.getName());
                Main.getInstance().getProfileService().syncProfileData(player.getUniqueId(), profileData);
            }
        }
    }
}
