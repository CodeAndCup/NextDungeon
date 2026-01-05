package fr.perrier.dungeons.spigot.parties;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface for party providers/factories.
 * Each implementation handles a different party system (e.g., AlessioDPParties, internal).
 */
public interface IPartyProvider {

    /**
     * Gets the name of this party provider.
     *
     * @return the provider name
     */
    String getName();

    /**
     * Checks if this provider is available and properly initialized.
     *
     * @return true if the provider is available
     */
    boolean isAvailable();

    /**
     * Creates a new party with the given leader.
     *
     * @param leader the party leader
     * @param name the party name
     * @return the created party, or empty if creation failed
     */
    Optional<IParty> createParty(Player leader, String name);

    /**
     * Gets a party by its ID.
     *
     * @param partyId the party's unique identifier
     * @return the party, or empty if not found
     */
    Optional<IParty> getParty(UUID partyId);

    /**
     * Gets a party by its leader.
     *
     * @param leader the party leader
     * @return the party led by the player, or empty if not found
     */
    Optional<IParty> getPartyByLeader(Player leader);

    /**
     * Gets a party by its leader's UUID.
     *
     * @param leaderId the leader's UUID
     * @return the party, or empty if not found
     */
    Optional<IParty> getPartyByLeader(UUID leaderId);

    /**
     * Gets the party that a player is a member of.
     *
     * @param player the player
     * @return the player's party, or empty if not in a party
     */
    Optional<IParty> getPartyOf(Player player);

    /**
     * Gets the party that a UUID is a member of.
     *
     * @param memberId the member's UUID
     * @return the party, or empty if not in a party
     */
    Optional<IParty> getPartyOf(UUID memberId);

    /**
     * Gets all online parties.
     *
     * @return collection of online parties
     */
    Collection<IParty> getOnlineParties();

    /**
     * Finds parties whose description contains the given substring (case insensitive).
     *
     * @param partOfDesc substring to search for
     * @return collection of matching parties
     */
    Collection<IParty> findPartiesByDescription(String partOfDesc);

    /**
     * Gets or creates a party member representation for a player.
     *
     * @param player the player
     * @return the party member, or empty if not available
     */
    Optional<IPartyMember> getPartyMember(Player player);

    /**
     * Gets or creates a party member representation for a UUID.
     *
     * @param memberId the member's UUID
     * @return the party member, or empty if not available
     */
    Optional<IPartyMember> getPartyMember(UUID memberId);

    /**
     * Checks if a player is in any party.
     *
     * @param player the player to check
     * @return true if the player is in a party
     */
    boolean isInParty(Player player);

    /**
     * Checks if a player is the leader of their party.
     *
     * @param player the player to check
     * @return true if the player is a party leader
     */
    boolean isPartyLeader(Player player);
}

