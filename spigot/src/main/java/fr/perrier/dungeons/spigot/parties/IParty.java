package fr.perrier.dungeons.spigot.parties;

import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

/**
 * Interface representing a party.
 * This abstraction allows for different party system implementations
 * (e.g., AlessioDPParties, internal system, etc.)
 */
public interface IParty {

    /**
     * Gets the unique identifier of the party.
     *
     * @return the party's unique identifier
     */
    UUID getPartyId();

    /**
     * Gets the name of the party.
     *
     * @return the party name
     */
    String getName();

    /**
     * Sets the name of the party.
     *
     * @param name the new party name
     */
    void setName(String name);

    /**
     * Gets the description of the party.
     *
     * @return the party description, or null if not set
     */
    String getDescription();

    /**
     * Sets the description of the party.
     *
     * @param description the new description
     */
    void setDescription(String description);

    /**
     * Gets the UUID of the party leader.
     *
     * @return the leader's UUID
     */
    UUID getLeaderId();

    /**
     * Sets the leader of the party.
     *
     * @param leaderId the UUID of the new leader
     */
    void setLeader(UUID leaderId);

    /**
     * Gets all member UUIDs of the party.
     *
     * @return set of member UUIDs
     */
    Set<UUID> getMemberIds();

    /**
     * Gets all members of the party as IPartyMember objects.
     *
     * @return set of party members
     */
    Set<IPartyMember> getMembers();

    /**
     * Gets only online members of the party.
     *
     * @return set of online party members
     */
    Set<IPartyMember> getOnlineMembers();

    /**
     * Adds a player to the party.
     *
     * @param player the player to add
     * @return true if the player was successfully added
     */
    boolean addMember(Player player);

    /**
     * Adds a member by UUID to the party.
     *
     * @param memberId the UUID of the member to add
     * @return true if the member was successfully added
     */
    boolean addMember(UUID memberId);

    /**
     * Removes a player from the party.
     *
     * @param player the player to remove
     * @return true if the player was successfully removed
     */
    boolean removeMember(Player player);

    /**
     * Removes a member by UUID from the party.
     *
     * @param memberId the UUID of the member to remove
     * @return true if the member was successfully removed
     */
    boolean removeMember(UUID memberId);

    /**
     * Checks if a player is a member of the party.
     *
     * @param player the player to check
     * @return true if the player is a member
     */
    boolean isMember(Player player);

    /**
     * Checks if a UUID is a member of the party.
     *
     * @param memberId the UUID to check
     * @return true if the UUID is a member
     */
    boolean isMember(UUID memberId);

    /**
     * Gets the number of members in the party.
     *
     * @return the member count
     */
    int getSize();

    /**
     * Checks if all members of the party are online.
     *
     * @return true if all members are online
     */
    boolean areAllMembersOnline();

    /**
     * Disbands/deletes the party.
     */
    void disband();

    /**
     * Checks if the party still exists.
     *
     * @return true if the party exists
     */
    boolean exists();
}

