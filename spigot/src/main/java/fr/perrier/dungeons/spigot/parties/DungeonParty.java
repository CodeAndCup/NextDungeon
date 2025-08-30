package fr.perrier.dungeons.spigot.parties;

import com.alessiodp.parties.api.interfaces.Party;
import com.alessiodp.parties.api.interfaces.PartyPlayer;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.manager.PartyManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

@Getter
public class DungeonParty {

    @Getter
    private static final HashMap<UUID, DungeonParty> parties = new HashMap<>();

    private String dungeonId;
    private String floorId;

    private int minLevel;
    private String description;
    private Party party;

    public DungeonParty(String dungeonId, String floorId, int minLevel, String description, Party party) {
        this.dungeonId = dungeonId;
        this.floorId = floorId;
        this.minLevel = minLevel;
        this.description = description;
        this.party = party;

        parties.put(party.getLeader(), this);
    }

    /**
     * Sets the dungeon ID of the party and updates the cache.
     *
     * @param dungeonId the new dungeon ID
     */
    public void setDungeonId(String dungeonId) {
        this.dungeonId = dungeonId;
        parties.put(party.getLeader(), this);
    }

    /**
     * Sets the floor ID of the party and updates the cache.
     *
     * @param floorId the new floor ID
     */
    public void setFloorId(String floorId) {
        this.floorId = floorId;
        parties.put(party.getLeader(), this);
    }

    /**
     * Sets the minimum level requirement for the party.
     *
     * @param minLevel the new minimum level requirement
     */
    public void setMinLevel(int minLevel) {
        this.minLevel = minLevel;
        parties.put(party.getLeader(), this);
    }

    /**
     * Sets the description of the party.
     *
     * @param description the new description of the party
     */
    public void setDescription(String description) {
        this.description = description;
        parties.put(party.getLeader(), this);
    }

    /**
     * Adds a member to the party.
     *
     * @param player the player to add as a member
     * @throws NullPointerException if the party player for the player is null
     */
    public void addMember(Player player) {
        PartyPlayer partyPlayer = Main.getInstance().getPartiesAPI().getPartyPlayer(player.getUniqueId());
        party.addMember(Objects.requireNonNull(partyPlayer));
    }

    /**
     * Removes a member from the party.
     *
     * @param player the player to remove as a member
     * @throws NullPointerException if the party player for the player is null
     */
    public void removeMember(Player player) {
        PartyPlayer partyPlayer = Main.getInstance().getPartiesAPI().getPartyPlayer(player.getUniqueId());
        party.removeMember(Objects.requireNonNull(partyPlayer));
    }

    /**
     * Returns the set of members of the party, including only the members who are currently online.
     *
     * @return the set of online members of the party
     */
    public Set<PartyPlayer> getMembers() {
        HashSet<PartyPlayer> onlineMembers = new HashSet<>();
        for (UUID uuid : party.getMembers()) {
            if (Bukkit.getPlayer(uuid) != null) {
                onlineMembers.add(Main.getInstance().getPartiesAPI().getPartyPlayer(uuid));
            }
        }
        return onlineMembers;
    }

    /**
     * Returns whether all members of the party are currently online.
     *
     * @return {@code true} if all members of the party are online, {@code false} otherwise
     */
    public boolean hasAllMembersOnline() {
        for(UUID uuid : party.getMembers()) {
            if(Bukkit.getPlayer(uuid) == null)
                return false;
        }
        return true;
    }

    /**
     * Returns the UUID of the leader of the party.
     *
     * @return the UUID of the party leader
     */
    public UUID getLeader() {
        return party.getLeader();
    }

    public static class Builder {
        private String dungeonId;
        private String floorId;
        private int minLevel;
        private String description;
        private Party party;
        private Player leader;

        /**
         * Sets the dungeon ID of the party being built.
         *
         * @param dungeonId the dungeon ID to set
         * @return this builder object for method chaining
         */
        public Builder setDungeonId(String dungeonId) {
            this.dungeonId = dungeonId;
            return this;
        }

        /**
         * Sets the floor ID of the party being built.
         *
         * @param floorId the floor ID to set
         * @return this builder object for method chaining
         */
        public Builder setFloorId(String floorId) {
            this.floorId = floorId;
            return this;
        }

        /**
         * Sets the minimum level requirement for the party being built.
         *
         * @param minLevel the minimum level requirement to set
         * @return this builder object for method chaining
         */
        public Builder setMinLevel(int minLevel) {
            this.minLevel = minLevel;
            return this;
        }

        /**
         * Sets the description of the party being built.
         *
         * @param description the description to set
         * @return this builder object for method chaining
         */
        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the leader of the party being built.
         *
         * @param leader the leader to set
         * @return this builder object for method chaining
         */
        public Builder setLeader(Player leader) {
            this.leader = leader;
            return this;
        }

        /**
         * Builds a {@link DungeonParty} using the current builder configuration.
         *
         * <p>This method creates a new party with the configured parameters if one does not already exist
         * for the leader. It then retrieves the party and returns a new {@link DungeonParty} instance
         * with the configured parameters and the retrieved party.</p>
         *
         * @return a new {@link DungeonParty} instance with the configured parameters and the retrieved party
         * @throws IllegalStateException if no leader has been set
         */
        public DungeonParty build() {
            if(leader == null)
                throw new IllegalStateException("No leader has been set");

            party = PartyManager.findPartyByLeader(leader);

            if(party == null) {
                Main.getInstance().getPartiesAPI().createParty(
                        "Party of " + leader.getName(),
                        Main.getInstance().getPartiesAPI().getPartyPlayer(leader.getUniqueId())
                );
                party = PartyManager.findPartyByLeader(leader);
            }
            return new DungeonParty(dungeonId, floorId, minLevel, description, party);
        }
    }

    /**
     * Checks if a player has a lead party.
     *
     * @param player the player to check
     * @return {@code true} if the player has a lead party, {@code false} otherwise
     */
    public static boolean hasLeadParty(Player player) {
        return hasLeadParty(player.getUniqueId());
    }

    /**
     * Checks if a UUID has a lead party.
     *
     * @param uuid the UUID to check
     * @return {@code true} if the UUID has a lead party, {@code false} otherwise
     */
    public static boolean hasLeadParty(UUID uuid) {
        return parties.containsKey(uuid);
    }

    /**
     * Returns the DungeonParty associated with the given player.
     *
     * @param leader the player whose DungeonParty is to be retrieved
     * @return the DungeonParty associated with the player, or {@code null} if the player does not have a DungeonParty
     */
    public static DungeonParty getDungeonPartyOf(Player leader) {
        return getDungeonPartyOf(leader.getUniqueId());
    }

    /**
     * Returns the DungeonParty associated with the given UUID.
     *
     * @param leaderId the UUID whose DungeonParty is to be retrieved
     * @return the DungeonParty associated with the UUID, or {@code null} if the UUID does not have a DungeonParty
     */
    public static DungeonParty getDungeonPartyOf(UUID leaderId) {
        return parties.get(leaderId);
    }

    /**
     * Returns a string representation of the DungeonParty object.
     * The string includes the dungeon ID, floor ID, minimum level, description, and party.
     *
     * @return a string in the format "DungeonParty{dungeonId='<dungeonId>', floorId='<floorId>',
     *         minLevel=<minLevel>, description='<description>', party=<party>}"
     */
    @Override
    public String toString() {
        return "DungeonParty{" +
                "dungeonId='" + dungeonId + '\'' +
                ", floorId='" + floorId + '\'' +
                ", minLevel=" + minLevel +
                ", description='" + description + '\'' +
                ", party=" + party +
                '}';
    }
}
