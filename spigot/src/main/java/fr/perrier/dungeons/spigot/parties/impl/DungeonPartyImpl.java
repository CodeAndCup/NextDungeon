package fr.perrier.dungeons.spigot.parties.impl;

import fr.perrier.dungeons.common.model.party.DungeonPartyData;
import fr.perrier.dungeons.spigot.parties.IDungeonParty;
import fr.perrier.dungeons.spigot.parties.IParty;
import fr.perrier.dungeons.spigot.parties.IPartyMember;
import fr.perrier.dungeons.spigot.parties.PartyService;
import net.Indyuce.mmocore.api.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;

import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of IDungeonParty for a party hosted on this server.
 *
 * <p>A {@code DungeonPartyImpl} owns a live {@link IParty} and publishes its metadata
 * (as a {@link DungeonPartyData}) into the cluster-wide {@link DungeonPartyRegistry} so peer
 * servers can list it in their party finder UI.</p>
 *
 * <p>Non-local parties are exposed through {@link RemoteDungeonParty}, built from the DTO.</p>
 */
public class DungeonPartyImpl implements IDungeonParty {

    /**
     * Local cache of parties whose IParty object lives on THIS server. Keyed by leader UUID so
     * getters called with a leader's UUID (e.g. {@code getDungeonPartyOf(player)}) stay O(1).
     */
    private static final Map<UUID, DungeonPartyImpl> localParties = new ConcurrentHashMap<>();

    private final IParty party;
    private String dungeonId;
    private String floorId;
    private int minLevel;
    private String description;

    /**
     * Whether this party shows up in the party finder. Independent from existence so the leader
     * can pull the party out of the finder (e.g. when starting a dungeon) without disbanding.
     */
    private boolean listed = true;

    /**
     * Display info received over Pidgin when cross-server members joined. Keeps name, MMOCore
     * class and level visible in the party finder on every server even though the owner can't
     * resolve them via Bukkit/MMOCore locally.
     */
    private final Map<UUID, RemoteMemberInfo> remoteMemberInfo = new ConcurrentHashMap<>();

    public DungeonPartyImpl(IParty party, String dungeonId, String floorId, int minLevel, String description) {
        this.party = party;
        this.dungeonId = dungeonId;
        this.floorId = floorId;
        this.minLevel = minLevel;
        this.description = description;

        localParties.put(party.getLeaderId(), this);
        publishToRegistry();
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
        publishToRegistry();
    }

    @Override
    public String getFloorId() {
        return floorId;
    }

    @Override
    public void setFloorId(String floorId) {
        this.floorId = floorId;
        publishToRegistry();
    }

    @Override
    public int getMinLevel() {
        return minLevel;
    }

    @Override
    public void setMinLevel(int minLevel) {
        this.minLevel = minLevel;
        publishToRegistry();
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
        publishToRegistry();
    }

    @Override
    public boolean isListed() {
        return listed;
    }

    /**
     * Toggles whether the party appears in the finder UI. Re-publishes the DTO so every lobby's
     * cache reflects the change without waiting for the heartbeat.
     */
    public void setListed(boolean listed) {
        if (this.listed == listed) return;
        this.listed = listed;
        publishToRegistry();
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
        boolean added = party.addMember(player);
        if (added) publishToRegistry();
        return added;
    }

    /**
     * Adds a member by UUID. Used by the cross-server join flow where the joining player may
     * not be online on this server at the time the mutation is applied.
     */
    public boolean addMember(UUID memberId) {
        boolean added = party.addMember(memberId);
        if (added) publishToRegistry();
        return added;
    }

    @Override
    public boolean removeMember(Player player) {
        boolean removed = party.removeMember(player);
        if (removed) publishToRegistry();
        return removed;
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

    private void publishToRegistry() {
        DungeonPartyRegistry registry = DungeonPartyRegistry.getInstance();
        if (registry == null) return; // registry not initialized yet (e.g. during tests)

        Set<UUID> memberIds = party.getMemberIds();
        Map<UUID, String> memberNames = new HashMap<>();
        Map<UUID, String> memberClasses = new HashMap<>();
        Map<UUID, Integer> memberLevels = new HashMap<>();

        for (UUID id : memberIds) {
            fillMemberSnapshot(id, memberNames, memberClasses, memberLevels);
        }

        DungeonPartyData data = new DungeonPartyData(
                party.getPartyId(),
                party.getLeaderId(),
                memberIds,
                memberNames.get(party.getLeaderId()),
                resolveSkinUrl(party.getLeaderId()),
                memberNames,
                memberClasses,
                memberLevels,
                party.getName(),
                dungeonId,
                floorId,
                minLevel,
                description,
                null
        );
        data.setListed(listed);
        registry.publish(data);
    }

    /**
     * Extracts the Mojang skin URL from the given (online) player's profile so peer servers can
     * render the real head in the party finder. Returns null when the player is offline here or
     * when Mojang hasn't returned a custom skin yet (the renderer then falls back gracefully).
     */
    private static String resolveSkinUrl(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online == null) return null;
        try {
            PlayerProfile profile = online.getPlayerProfile();
            URL skin = profile.getTextures().getSkin();
            return skin != null ? skin.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Stores display metadata for a cross-server member as delivered by a join packet. The next
     * {@link #publishToRegistry()} call will surface it to every lobby's party finder.
     */
    public void cacheRemoteMemberInfo(UUID memberId, String name, String className, int level) {
        remoteMemberInfo.put(memberId, new RemoteMemberInfo(name, className, level));
    }

    /**
     * Populates the per-member snapshot maps for a single UUID, preferring live MMOCore data
     * when the player is online on this server and falling back to the cross-server cache
     * otherwise. Both are best-effort — absent keys simply don't appear in the DTO.
     */
    private void fillMemberSnapshot(UUID memberId,
                                    Map<UUID, String> names,
                                    Map<UUID, String> classes,
                                    Map<UUID, Integer> levels) {
        Player online = Bukkit.getPlayer(memberId);
        if (online != null) {
            names.put(memberId, online.getName());
            PlayerData data = PlayerData.get(online);
            if (data != null) {
                if (data.getProfess() != null) classes.put(memberId, data.getProfess().getName());
                levels.put(memberId, data.getLevel());
            }
            return;
        }

        RemoteMemberInfo cached = remoteMemberInfo.get(memberId);
        if (cached != null) {
            if (cached.name != null) names.put(memberId, cached.name);
            if (cached.className != null) classes.put(memberId, cached.className);
            if (cached.level > 0) levels.put(memberId, cached.level);
            return;
        }

        // No live data and nothing cached — fall back to whatever Bukkit knows locally.
        String fallback = resolveName(memberId);
        if (fallback != null) names.put(memberId, fallback);
    }

    /**
     * Resolves the best-known display name for a UUID. Prefers online Bukkit name, falls back
     * to OfflinePlayer's cached name. Returns null when the player has never been seen on this
     * server (which is common for cross-server members — their name is carried in the DTO
     * published by the server that owns the party).
     */
    private static String resolveName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        return offline.getName();
    }

    /**
     * Immutable bundle of cross-server display metadata for a single member. Populated from
     * the {@code DungeonPartyJoinRequestPacket} and read when (re)publishing the DTO.
     */
    private record RemoteMemberInfo(String name, String className, int level) { }

    /**
     * Removes this dungeon party from the local cache and the cluster-wide registry.
     */
    public void unregister() {
        UUID leaderId = party.getLeaderId();
        localParties.remove(leaderId);
        DungeonPartyRegistry registry = DungeonPartyRegistry.getInstance();
        if (registry != null) registry.remove(leaderId);
    }

    /**
     * Gets every dungeon party in the cluster, including those hosted on peer servers. Local
     * parties return as {@link DungeonPartyImpl} (with live {@link IParty}); remote parties
     * return as {@link RemoteDungeonParty} wrappers over their {@link DungeonPartyData}.
     *
     * @return map of leader UUID to dungeon party
     */
    public static Map<UUID, IDungeonParty> getDungeonParties() {
        Map<UUID, IDungeonParty> result = new HashMap<>();
        DungeonPartyRegistry registry = DungeonPartyRegistry.getInstance();
        if (registry == null) {
            // Fallback to local-only view when the registry is not available.
            result.putAll(localParties);
            return result;
        }

        for (DungeonPartyData data : registry.getAll()) {
            UUID leaderId = data.getLeaderId();
            DungeonPartyImpl local = localParties.get(leaderId);
            if (local != null) {
                result.put(leaderId, local);
            } else {
                result.put(leaderId, new RemoteDungeonParty(data));
            }
        }
        return result;
    }

    /**
     * Checks if a player has a lead dungeon party (local only — leaders are always local).
     */
    public static boolean hasLeadParty(Player player) {
        return hasLeadParty(player.getUniqueId());
    }

    public static boolean hasLeadParty(UUID uuid) {
        return localParties.containsKey(uuid);
    }

    public static DungeonPartyImpl getDungeonPartyOf(Player leader) {
        return getDungeonPartyOf(leader.getUniqueId());
    }

    /**
     * Returns the party led by the given UUID if it is hosted on this server. Returns null
     * for remote-hosted parties — use {@link #getDungeonParties()} for a cluster-wide view.
     */
    public static DungeonPartyImpl getDungeonPartyOf(UUID leaderId) {
        return localParties.get(leaderId);
    }

    /**
     * Clears all cached local parties. Used on plugin shutdown.
     */
    public static void clearAll() {
        localParties.clear();
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

            IParty party = partyService.getPartyByLeader(leader)
                    .orElseGet(() -> partyService.createParty(leader, "Party of " + leader.getName())
                            .orElseThrow(() -> new IllegalStateException("Failed to create party")));

            return new DungeonPartyImpl(party, dungeonId, floorId, minLevel, description);
        }
    }
}
