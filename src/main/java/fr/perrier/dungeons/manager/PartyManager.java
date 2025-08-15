package fr.perrier.dungeons.manager;

import com.alessiodp.parties.api.interfaces.Party;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.parties.DungeonParty;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

public class PartyManager {

    public static List<Party> findPartiesByDesc(String partOfDesc) {
        return Main.getInstance().getPartiesAPI().getOnlineParties().stream()
                .filter(party -> party.getDescription() != null)
                .filter(party -> party.getDescription().toLowerCase().contains(partOfDesc.toLowerCase()))
                .toList();
    }

    public static Party findPartyByLeader(Player leader) {
        return Main.getInstance().getPartiesAPI().getOnlineParties().stream()
                .filter(party -> Objects.equals(party.getLeader(), leader.getUniqueId()))
                .findFirst().orElse(null);
    }

    public static boolean isInsideParty(Player player, Party party) {
        return party.getMembers().stream()
                .anyMatch(member -> Objects.equals(member, player.getUniqueId()));
    }
}
