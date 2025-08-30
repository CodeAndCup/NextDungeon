package fr.perrier.dungeons.spigot.manager;

import com.alessiodp.parties.api.interfaces.Party;
import fr.perrier.dungeons.spigot.Main;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

public class PartyManager {

    /**
     * Find parties whose description contains the given substring (case insensitive).
     *
     * @param partOfDesc Substring to search for in party descriptions.
     * @return List of parties with descriptions containing the substring.
     */
    public static List<Party> findPartiesByDesc(String partOfDesc) {
        return Main.getInstance().getPartiesAPI().getOnlineParties().stream()
                .filter(party -> party.getDescription() != null)
                .filter(party -> party.getDescription().toLowerCase().contains(partOfDesc.toLowerCase()))
                .toList();
    }

    /**
     * Find a party by its leader.
     *
     * @param leader The player who is the leader of the party.
     * @return The party led by the specified player, or null if not found.
     */
    public static Party findPartyByLeader(Player leader) {
        return Main.getInstance().getPartiesAPI().getOnlineParties().stream()
                .filter(party -> Objects.equals(party.getLeader(), leader.getUniqueId()))
                .findFirst().orElse(null);
    }

    /**
     * Check if a player is a member of a given party.
     *
     * @param player The player to check.
     * @param party  The party to check against.
     * @return True if the player is a member of the party, false otherwise.
     */
    public static boolean isInsideParty(Player player, Party party) {
        return party.getMembers().stream()
                .anyMatch(member -> Objects.equals(member, player.getUniqueId()));
    }
}
