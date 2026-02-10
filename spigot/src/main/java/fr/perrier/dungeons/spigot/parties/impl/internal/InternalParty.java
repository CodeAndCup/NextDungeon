package fr.perrier.dungeons.spigot.parties.impl.internal;

import fr.perrier.dungeons.spigot.parties.IParty;
import fr.perrier.dungeons.spigot.parties.IPartyMember;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of IParty for the internal party system.
 */
public class InternalParty implements IParty {

    private final UUID partyId;
    private String name;
    private String description;
    private UUID leaderId;
    private final Set<UUID> members;
    private boolean exists;

    public InternalParty(UUID partyId, String name, UUID leaderId) {
        this.partyId = partyId;
        this.name = name;
        this.leaderId = leaderId;
        this.members = ConcurrentHashMap.newKeySet();
        this.members.add(leaderId);
        this.exists = true;
    }

    @Override
    public UUID getPartyId() {
        return partyId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public UUID getLeaderId() {
        return leaderId;
    }

    @Override
    public void setLeader(UUID leaderId) {
        if (members.contains(leaderId)) {
            this.leaderId = leaderId;
        }
    }

    @Override
    public Set<UUID> getMemberIds() {
        return new HashSet<>(members);
    }

    @Override
    public Set<IPartyMember> getMembers() {
        return members.stream()
                .map(InternalPartyMember::new)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<IPartyMember> getOnlineMembers() {
        return members.stream()
                .filter(uuid -> Bukkit.getPlayer(uuid) != null)
                .map(InternalPartyMember::new)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean addMember(Player player) {
        return addMember(player.getUniqueId());
    }

    @Override
    public boolean addMember(UUID memberId) {
        return members.add(memberId);
    }

    @Override
    public boolean removeMember(Player player) {
        return removeMember(player.getUniqueId());
    }

    @Override
    public boolean removeMember(UUID memberId) {
        // Cannot remove the leader
        if (memberId.equals(leaderId)) {
            return false;
        }
        return members.remove(memberId);
    }

    @Override
    public boolean isMember(Player player) {
        return isMember(player.getUniqueId());
    }

    @Override
    public boolean isMember(UUID memberId) {
        return members.contains(memberId);
    }

    @Override
    public int getSize() {
        return members.size();
    }

    @Override
    public boolean areAllMembersOnline() {
        for (UUID uuid : members) {
            if (Bukkit.getPlayer(uuid) == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void disband() {
        members.clear();
        exists = false;
        InternalPartyProvider.getInstance().removeParty(partyId);
    }

    @Override
    public boolean exists() {
        return exists && InternalPartyProvider.getInstance().getParty(partyId).isPresent();
    }

    /**
     * Transfers leadership to another member.
     *
     * @param newLeaderId the UUID of the new leader
     * @return true if the transfer was successful
     */
    public boolean transferLeadership(UUID newLeaderId) {
        if (!members.contains(newLeaderId)) {
            return false;
        }
        this.leaderId = newLeaderId;
        return true;
    }

    /**
     * Forces removal of a member, including the leader.
     * If the leader is removed, transfers leadership to another member if available.
     *
     * @param memberId the UUID of the member to remove
     * @return true if the member was removed
     */
    public boolean forceRemoveMember(UUID memberId) {
        if (!members.contains(memberId)) {
            return false;
        }

        // If removing the leader, transfer leadership first
        if (memberId.equals(leaderId)) {
            Optional<UUID> newLeader = members.stream()
                    .filter(uuid -> !uuid.equals(memberId))
                    .findFirst();

            if (newLeader.isPresent()) {
                leaderId = newLeader.get();
            } else {
                // No other members, disband the party
                disband();
                return true;
            }
        }

        return members.remove(memberId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InternalParty that = (InternalParty) o;
        return partyId.equals(that.partyId);
    }

    @Override
    public int hashCode() {
        return partyId.hashCode();
    }

    @Override
    public String toString() {
        return "InternalParty{" +
                "partyId=" + partyId +
                ", name='" + name + '\'' +
                ", leaderId=" + leaderId +
                ", members=" + members.size() +
                '}';
    }
}

