package fr.perrier.dungeons.spigot.parties.impl.internal;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.parties.IParty;
import fr.perrier.dungeons.spigot.parties.IPartyMember;
import fr.perrier.dungeons.spigot.parties.IPartyProvider;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of IPartyProvider for the internal party system.
 * This is a standalone party system that doesn't require any external plugins.
 */
public class InternalPartyProvider implements IPartyProvider {

    private static final String PROVIDER_NAME = "Internal";

    @Getter
    private static InternalPartyProvider instance;

    // Party storage
    private final Map<UUID, InternalParty> partiesById;
    private final Map<UUID, UUID> memberToParty; // Maps member UUID to party UUID

    public InternalPartyProvider() {
        this.partiesById = new ConcurrentHashMap<>();
        this.memberToParty = new ConcurrentHashMap<>();
        instance = this;
    }

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isAvailable() {
        return true; // Always available
    }

    @Override
    public Optional<IParty> createParty(Player leader, String name) {
        // Check if the player is already in a party
        if (memberToParty.containsKey(leader.getUniqueId())) {
            Main.getInstance().getLogger().warning("Player " + leader.getName() + " is already in a party");
            return Optional.empty();
        }

        UUID partyId = UUID.randomUUID();
        InternalParty party = new InternalParty(partyId, name, leader.getUniqueId());

        partiesById.put(partyId, party);
        memberToParty.put(leader.getUniqueId(), partyId);

        Main.getInstance().getLogger().info("Created internal party: " + name + " with leader " + leader.getName());
        return Optional.of(party);
    }

    @Override
    public Optional<IParty> getParty(UUID partyId) {
        return Optional.ofNullable(partiesById.get(partyId));
    }

    @Override
    public Optional<IParty> getPartyByLeader(Player leader) {
        return getPartyByLeader(leader.getUniqueId());
    }

    @Override
    public Optional<IParty> getPartyByLeader(UUID leaderId) {
        return partiesById.values().stream()
                .filter(party -> party.getLeaderId().equals(leaderId))
                .map(party -> (IParty) party)
                .findFirst();
    }

    @Override
    public Optional<IParty> getPartyOf(Player player) {
        return getPartyOf(player.getUniqueId());
    }

    @Override
    public Optional<IParty> getPartyOf(UUID memberId) {
        UUID partyId = memberToParty.get(memberId);
        if (partyId == null) {
            return Optional.empty();
        }
        return getParty(partyId);
    }

    @Override
    public Collection<IParty> getOnlineParties() {
        return partiesById.values().stream()
                .filter(party -> !party.getOnlineMembers().isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    public Collection<IParty> findPartiesByDescription(String partOfDesc) {
        return partiesById.values().stream()
                .filter(party -> party.getDescription() != null)
                .filter(party -> party.getDescription().toLowerCase().contains(partOfDesc.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<IPartyMember> getPartyMember(Player player) {
        return Optional.of(new InternalPartyMember(player));
    }

    @Override
    public Optional<IPartyMember> getPartyMember(UUID memberId) {
        return Optional.of(new InternalPartyMember(memberId));
    }

    @Override
    public boolean isInParty(Player player) {
        return memberToParty.containsKey(player.getUniqueId());
    }

    @Override
    public boolean isPartyLeader(Player player) {
        UUID partyId = memberToParty.get(player.getUniqueId());
        if (partyId == null) {
            return false;
        }
        InternalParty party = partiesById.get(partyId);
        return party != null && party.getLeaderId().equals(player.getUniqueId());
    }

    /**
     * Adds a member to a party's membership tracking.
     *
     * @param memberId the UUID of the member
     * @param partyId the UUID of the party
     */
    public void registerMember(UUID memberId, UUID partyId) {
        memberToParty.put(memberId, partyId);
    }

    /**
     * Removes a member from the membership tracking.
     *
     * @param memberId the UUID of the member
     */
    public void unregisterMember(UUID memberId) {
        memberToParty.remove(memberId);
    }

    /**
     * Removes a party from the provider.
     *
     * @param partyId the UUID of the party to remove
     */
    public void removeParty(UUID partyId) {
        InternalParty party = partiesById.remove(partyId);
        if (party != null) {
            // Remove all member mappings
            for (UUID memberId : party.getMemberIds()) {
                memberToParty.remove(memberId);
            }
        }
    }

    /**
     * Clears all parties. Useful for plugin shutdown.
     */
    public void clearAll() {
        partiesById.clear();
        memberToParty.clear();
    }

    /**
     * Gets the total number of active parties.
     *
     * @return the party count
     */
    public int getPartyCount() {
        return partiesById.size();
    }

    /**
     * Gets all parties.
     *
     * @return collection of all parties
     */
    public Collection<IParty> getAllParties() {
        return new ArrayList<>(partiesById.values());
    }
}

