package fr.perrier.dungeons.spigot.listener.global;

import com.alessiodp.parties.api.events.bukkit.party.BukkitPartiesPartyPreDeleteEvent;
import com.alessiodp.parties.api.events.bukkit.player.BukkitPartiesPlayerPreLeaveEvent;
import com.alessiodp.parties.api.interfaces.Party;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.parties.impl.DungeonPartyImpl;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Objects;

public class GlobalPartyListener implements Listener {

    /**
     * Called when a party is disbanded.
     * Removes the party from the DungeonParty registry and notifies members.
     *
     * @param event The event triggered when a party is about to be deleted.
     */
    @EventHandler
    public void onPartyDisband(BukkitPartiesPartyPreDeleteEvent event) {
        Party party = event.getParty();
        party.getMembers().forEach(uuid -> {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null)
                member.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Your party has been removed from the party finder!"));
        });
        DungeonPartyImpl.getDungeonParties().remove(party.getLeader());
    }

    /**
     * Called when a party leader leaves the party.
     * If the leader leaves, the party is removed from
     * the DungeonParty registry and members are notified.
     *
     * @param event The event triggered when a player is about to leave a party.
     */
    @EventHandler
    public void onPartyLeaderLeave(BukkitPartiesPlayerPreLeaveEvent event) {
        Party party = event.getParty();
        if(Objects.equals(party.getLeader(), event.getPartyPlayer().getPlayerUUID())) {
            if(DungeonPartyImpl.getDungeonParties().containsKey(party.getLeader())) {
                DungeonPartyImpl.getDungeonParties().remove(party.getLeader());
                party.getMembers().forEach(uuid -> {
                    Player member = Bukkit.getPlayer(uuid);
                    if (member != null)
                        member.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Your party has been removed from the party finder!"));
                });
            }
        }
    }
}
