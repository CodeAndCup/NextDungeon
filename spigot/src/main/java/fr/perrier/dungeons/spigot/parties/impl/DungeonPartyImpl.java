package fr.perrier.dungeons.spigot.parties.impl;

import fr.perrier.dungeons.spigot.parties.IDungeonParty;
import fr.perrier.dungeons.spigot.parties.IParty;
import fr.perrier.dungeons.spigot.parties.IPartyMember;
import fr.perrier.dungeons.spigot.parties.PartyService;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of IDungeonParty.
 * Wraps an IParty with dungeon-specific properties.
 */
public class DungeonPartyImpl implements IDungeonParty {

    private static final Map<UUID, DungeonPartyImpl> dungeonParties = new ConcurrentHashMap<>();

    private final IParty party;
    private String dungeonId;
    private String floorId;
    private int minLevel;
    private String description;

    public DungeonPartyImpl(IParty party, String dungeonId, String floorId, int minLevel, String description) {
        this.party = party;
        this.dungeonId = dungeonId;
        this.floorId = floorId;
        this.minLevel = minLevel;
        this.description = description;

        // Register in the static map
        dungeonParties.put(party.getLeaderId(), this);
    }

    @Override
    public IParty getParty() {
        return party;
    }

    @Override
    public String getDungeonId() {
        return dungeonId;
    }

    @Override
    public void setDungeonId(String dungeonId) {
        this.dungeonId = dungeonId;
        updateCache();
    }

    @Override
    public String getFloorId() {
        return floorId;
    }

    @Override
    public void setFloorId(String floorId) {
        this.floorId = floorId;
        updateCache();
    }

    @Override
    public int getMinLevel() {
        return minLevel;
    }

    @Override
    public void setMinLevel(int minLevel) {
        this.minLevel = minLevel;
        updateCache();
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
        updateCache();
    }

    @Override
    public UUID getLeaderId() {
        return party.getLeaderId();
    }

    @Override
    public Set<UUID> getMemberIds() {
        return party.getMemberIds();
    }

    @Override
    public Set<IPartyMember> getMembers() {
        return party.getMembers();
    }

    @Override
    public Set<IPartyMember> getOnlineMembers() {
        return party.getOnlineMembers();
    }

    @Override
    public boolean addMember(Player player) {
        return party.addMember(player);
    }

    @Override
    public boolean removeMember(Player player) {
        return party.removeMember(player);
    }

    @Override
    public boolean areAllMembersOnline() {
        return party.areAllMembersOnline();
    }

    @Override
    public boolean isMember(Player player) {
        return party.isMember(player);
    }

    @Override
    public int getSize() {
        return party.getSize();
    }

    @Override
    public void disband() {
        party.disband();
        unregister();
    }

    private void updateCache() {
        dungeonParties.put(party.getLeaderId(), this);
    }

    /**
     * Removes this dungeon party from the cache.
     */
    public void unregister() {
        dungeonParties.remove(party.getLeaderId());
    }

    // ==================== Static methods ====================

    /**
     * Gets all registered dungeon parties.
     *
     * @return map of leader UUID to dungeon party
     */
    public static Map<UUID, DungeonPartyImpl> getDungeonParties() {
        return new HashMap<>(dungeonParties);
    }

    /**
     * Checks if a player has a lead dungeon party.
     *
     * @param player the player to check
     * @return true if the player leads a dungeon party
     */
    public static boolean hasLeadParty(Player player) {
        return hasLeadParty(player.getUniqueId());
    }

    /**
     * Checks if a UUID has a lead dungeon party.
     *
     * @param uuid the UUID to check
     * @return true if the UUID leads a dungeon party
     */
    public static boolean hasLeadParty(UUID uuid) {
        return dungeonParties.containsKey(uuid);
    }

    /**
     * Gets the dungeon party led by a player.
     *
     * @param leader the player
     * @return the dungeon party, or null if not found
     */
    public static DungeonPartyImpl getDungeonPartyOf(Player leader) {
        return getDungeonPartyOf(leader.getUniqueId());
    }

    /**
     * Gets the dungeon party led by a UUID.
     *
     * @param leaderId the leader's UUID
     * @return the dungeon party, or null if not found
     */
    public static DungeonPartyImpl getDungeonPartyOf(UUID leaderId) {
        return dungeonParties.get(leaderId);
    }

    /**
     * Clears all cached dungeon parties.
     */
    public static void clearAll() {
        dungeonParties.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DungeonPartyImpl that = (DungeonPartyImpl) o;
        return party.getPartyId().equals(that.party.getPartyId());
    }

    @Override
    public int hashCode() {
        return party.getPartyId().hashCode();
    }

    @Override
    public String toString() {
        return "DungeonPartyImpl{" +
                "dungeonId='" + dungeonId + '\'' +
                ", floorId='" + floorId + '\'' +
                ", minLevel=" + minLevel +
                ", description='" + description + '\'' +
                ", party=" + party +
                '}';
    }

    /**
     * Builder class for creating DungeonPartyImpl instances.
     */
    public static class Builder {
        private String dungeonId;
        private String floorId;
        private int minLevel;
        private String description;
        private Player leader;

        public Builder setDungeonId(String dungeonId) {
            this.dungeonId = dungeonId;
            return this;
        }

        public Builder setFloorId(String floorId) {
            this.floorId = floorId;
            return this;
        }

        public Builder setMinLevel(int minLevel) {
            this.minLevel = minLevel;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setLeader(Player leader) {
            this.leader = leader;
            return this;
        }

        /**
         * Builds a DungeonPartyImpl using the PartyService.
         *
         * @return the created DungeonPartyImpl
         * @throws IllegalStateException if no leader has been set or party creation failed
         */
        public DungeonPartyImpl build() {
            if (leader == null) {
                throw new IllegalStateException("No leader has been set");
            }

            PartyService partyService = PartyService.getInstance();

            // Try to get existing party or create a new one
            IParty party = partyService.getPartyByLeader(leader)
                    .orElseGet(() -> partyService.createParty(leader, "Party of " + leader.getName())
                            .orElseThrow(() -> new IllegalStateException("Failed to create party")));

            return new DungeonPartyImpl(party, dungeonId, floorId, minLevel, description);
        }
    }
}

