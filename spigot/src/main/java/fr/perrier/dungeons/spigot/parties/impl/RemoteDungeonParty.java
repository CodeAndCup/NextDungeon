package fr.perrier.dungeons.spigot.parties.impl;

import fr.perrier.dungeons.common.model.party.DungeonPartyData;
import fr.perrier.dungeons.spigot.parties.IDungeonParty;
import fr.perrier.dungeons.spigot.parties.IParty;
import fr.perrier.dungeons.spigot.parties.IPartyMember;
import fr.perrier.dungeons.spigot.parties.impl.internal.InternalPartyMember;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Read-only view of a dungeon party hosted on a peer server.
 *
 * <p>Used by the party finder UI so players can see parties from other lobbies. All mutating
 * operations (setters, addMember, removeMember, disband) are unsupported — the caller is expected
 * to detect remote parties via {@link DungeonPartyRegistry#isOwnedLocally(DungeonPartyData)} and
 * route the mutation through a Pidgin packet to the owner server instead.</p>
 */
public class RemoteDungeonParty implements IDungeonParty {

    private final DungeonPartyData data;

    public RemoteDungeonParty(DungeonPartyData data) {
        this.data = data;
    }

    public DungeonPartyData getData() {
        return data;
    }

    @Override public IParty getParty() {
        throw new UnsupportedOperationException("RemoteDungeonParty has no local IParty — route mutations via packet");
    }

    @Override public String getDungeonId() { return data.getDungeonId(); }
    @Override public void setDungeonId(String dungeonId) { throw readOnly(); }

    @Override public String getFloorId() { return data.getFloorId(); }
    @Override public void setFloorId(String floorId) { throw readOnly(); }

    @Override public int getMinLevel() { return data.getMinLevel(); }
    @Override public void setMinLevel(int minLevel) { throw readOnly(); }

    @Override public String getDescription() { return data.getDescription(); }
    @Override public void setDescription(String description) { throw readOnly(); }

    @Override public UUID getLeaderId() { return data.getLeaderId(); }

    @Override
    public Set<UUID> getMemberIds() {
        return data.getMemberIds() != null ? Set.copyOf(data.getMemberIds()) : Set.of();
    }

    @Override
    public Set<IPartyMember> getMembers() {
        return getMemberIds().stream()
                .map(uuid -> (IPartyMember) new InternalPartyMember(uuid))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<IPartyMember> getOnlineMembers() {
        return getMemberIds().stream()
                .filter(uuid -> Bukkit.getPlayer(uuid) != null)
                .map(uuid -> (IPartyMember) new InternalPartyMember(uuid))
                .collect(Collectors.toSet());
    }

    @Override public boolean addMember(Player player) { throw readOnly(); }
    @Override public boolean removeMember(Player player) { throw readOnly(); }

    @Override
    public boolean areAllMembersOnline() {
        for (UUID id : getMemberIds()) {
            if (Bukkit.getPlayer(id) == null) return false;
        }
        return true;
    }

    @Override
    public boolean isMember(Player player) {
        return getMemberIds().contains(player.getUniqueId());
    }

    @Override
    public int getSize() {
        return getMemberIds().size();
    }

    @Override public void disband() { throw readOnly(); }

    @Override public boolean isListed() { return data.isListed(); }

    private static UnsupportedOperationException readOnly() {
        return new UnsupportedOperationException("RemoteDungeonParty is read-only");
    }
}
