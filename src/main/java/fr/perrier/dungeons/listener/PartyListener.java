package fr.perrier.dungeons.listener;

import com.alessiodp.parties.api.enums.DeleteCause;
import com.alessiodp.parties.api.events.bukkit.party.BukkitPartiesPartyPreDeleteEvent;
import com.alessiodp.parties.api.events.bukkit.player.BukkitPartiesPlayerPostLeaveEvent;
import com.alessiodp.parties.api.events.bukkit.player.BukkitPartiesPlayerPreLeaveEvent;
import com.alessiodp.parties.api.interfaces.Party;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.parties.DungeonParty;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Objects;

public class PartyListener implements Listener {

    @EventHandler
    public void onPartyDisband(BukkitPartiesPartyPreDeleteEvent event) {
        Party party = event.getParty();
        party.getOnlineMembers().forEach(member ->
            Bukkit.getPlayer(member.getPlayerUUID()).sendMessage(ChatUtil.translate("&cYour party has been removed from the party finder!"))
        );
        DungeonParty.getParties().remove(party.getLeader());
    }

    @EventHandler
    public void onPartyLeaderLeave(BukkitPartiesPlayerPreLeaveEvent event) {
        Party party = event.getParty();
        if(Objects.equals(party.getLeader(), event.getPartyPlayer().getPlayerUUID())) {
            if(DungeonParty.getParties().containsKey(party.getLeader())) {
                DungeonParty.getParties().remove(party.getLeader());
                party.getOnlineMembers().forEach(member ->
                        Bukkit.getPlayer(member.getPlayerUUID()).sendMessage(ChatUtil.translate("&cYour party has been removed from the party finder!"))
                );
            }
        }
    }
}
