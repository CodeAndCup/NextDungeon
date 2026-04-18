package fr.perrier.dungeons.spigot.parties;

import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

/**
 * Interface representing a dungeon party with dungeon-specific properties.
 * Extends the party concept with dungeon-related attributes.
 */
public interface IDungeonParty {

    /**
     * Gets the underlying party implementation.
     *
     * @return the party
     */
    IParty getParty();

    /**
     * Gets the dungeon ID associated with this party.
     *
     * @return the dungeon ID
     */
    String getDungeonId();

    /**
     * Sets the dungeon ID for this party.
     *
     * @param dungeonId the dungeon ID
     */
    void setDungeonId(String dungeonId);

    /**
     * Gets the floor ID associated with this party.
     *
     * @return the floor ID
     */
    String getFloorId();

    /**
     * Sets the floor ID for this party.
     *
     * @param floorId the floor ID
     */
    void setFloorId(String floorId);

    /**
     * Gets the minimum level requirement for this party.
     *
     * @return the minimum level
     */
    int getMinLevel();

    /**
     * Sets the minimum level requirement for this party.
     *
     * @param minLevel the minimum level
     */
    void setMinLevel(int minLevel);

    /**
     * Gets the description of the dungeon party.
     *
     * @return the description
     */
    String getDescription();

    /**
     * Sets the description of the dungeon party.
     *
     * @param description the description
     */
    void setDescription(String description);

    /**
     * Gets the UUID of the party leader.
     *
     * @return the leader's UUID
     */
    UUID getLeaderId();

    /**
     * Gets all member UUIDs of the party.
     *
     * @return set of member UUIDs
     */
    Set<UUID> getMemberIds();

    /**
     * Gets all members of the party.
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
     * @return true if successful
     */
    boolean addMember(Player player);

    /**
     * Removes a player from the party.
     *
     * @param player the player to remove
     * @return true if successful
     */
    boolean removeMember(Player player);

    /**
     * Checks if all members are online.
     *
     * @return true if all members are online
     */
    boolean areAllMembersOnline();

    /**
     * Checks if a player is a member.
     *
     * @param player the player to check
     * @return true if the player is a member
     */
    boolean isMember(Player player);

    /**
     * Gets the size of the party.
     *
     * @return the number of members
     */
    int getSize();

    /**
     * Disbands the party, removing all members and clearing the party data.
     */
    void disband();

    /**
     * Whether this party should appear in the party finder UI. Orthogonal to existence — a
     * party can be alive (members grouped together) without being listed (e.g. mid-dungeon).
     * Defaults to {@code true} so providers that don't care about listing behave as before.
     */
    default boolean isListed() {
        return true;
    }
}

