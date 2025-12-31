package fr.perrier.dungeons.spigot.manager;

import com.alessiodp.parties.api.interfaces.Party;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.parties.IParty;
import fr.perrier.dungeons.spigot.parties.PartyService;
import fr.perrier.dungeons.spigot.parties.impl.AlessioDPParty;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Legacy PartyManager for backward compatibility.
 * @deprecated Use {@link PartyService} instead.
 */
@Deprecated
public class PartyManager {

    /**
     * Find parties whose description contains the given substring (case insensitive).
     *
     * @param partOfDesc Substring to search for in party descriptions.
     * @return List of parties with descriptions containing the substring.
     * @deprecated Use {@link PartyService#findPartiesByDescription(String)} instead.
     */
    @Deprecated
    public static List<Party> findPartiesByDesc(String partOfDesc) {
        // Use the new system if available
        PartyService partyService = PartyService.getInstance();
        if (partyService != null) {
            return partyService.findPartiesByDescription(partOfDesc).stream()
                    .filter(p -> p instanceof AlessioDPParty)
                    .map(p -> ((AlessioDPParty) p).getUnderlyingParty())
                    .collect(Collectors.toList());
        }

        // Fallback to legacy implementation
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
     * @deprecated Use {@link PartyService#getPartyByLeader(Player)} instead.
     */
    @Deprecated
    public static Party findPartyByLeader(Player leader) {
        // Use the new system if available
        PartyService partyService = PartyService.getInstance();
        if (partyService != null) {
            return partyService.getPartyByLeader(leader)
                    .filter(p -> p instanceof AlessioDPParty)
                    .map(p -> ((AlessioDPParty) p).getUnderlyingParty())
                    .orElse(null);
        }

        // Fallback to legacy implementation
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
     * @deprecated Use {@link IParty#isMember(Player)} instead.
     */
    @Deprecated
    public static boolean isInsideParty(Player player, Party party) {
        return party.getMembers().stream()
                .anyMatch(member -> Objects.equals(member, player.getUniqueId()));
    }

    /**
     * Check if a player is a member of a given IParty.
     *
     * @param player The player to check.
     * @param party  The party to check against.
     * @return True if the player is a member of the party, false otherwise.
     */
    public static boolean isInsideParty(Player player, IParty party) {
        return party.isMember(player);
    }
}

