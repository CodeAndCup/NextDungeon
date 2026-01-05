package fr.perrier.dungeons.spigot.parties.impl.alessio;

import com.alessiodp.parties.api.interfaces.Party;
import com.alessiodp.parties.api.interfaces.PartyPlayer;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.parties.IParty;
import fr.perrier.dungeons.spigot.parties.IPartyMember;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of IParty that wraps AlessioDPParties' Party.
 */
public class AlessioDPParty implements IParty {

    private final Party party;

    public AlessioDPParty(Party party) {
        this.party = Objects.requireNonNull(party, "Party cannot be null");
    }

    /**
     * Gets the underlying Party object.
     *
     * @return the Party
     */
    public Party getUnderlyingParty() {
        return party;
    }

    @Override
    public UUID getPartyId() {
        return party.getId();
    }

    @Override
    public String getName() {
        return party.getName();
    }

    @Override
    public void setName(String name) {
        party.rename(name);
    }

    @Override
    public String getDescription() {
        return party.getDescription();
    }

    @Override
    public void setDescription(String description) {
        party.setDescription(description);
    }

    @Override
    public UUID getLeaderId() {
        return party.getLeader();
    }

    @Override
    public void setLeader(UUID leaderId) {
        PartyPlayer pp = Main.getInstance().getPartiesAPI().getPartyPlayer(leaderId);
        if (pp != null) {
            party.changeLeader(pp);
        }
    }

    @Override
    public Set<UUID> getMemberIds() {
        return new HashSet<>(party.getMembers());
    }

    @Override
    public Set<IPartyMember> getMembers() {
        return party.getMembers().stream()
                .map(AlessioDPPartyMember::fromUUID)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<IPartyMember> getOnlineMembers() {
        return party.getMembers().stream()
                .filter(uuid -> Bukkit.getPlayer(uuid) != null)
                .map(AlessioDPPartyMember::fromUUID)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean addMember(Player player) {
        PartyPlayer pp = Main.getInstance().getPartiesAPI().getPartyPlayer(player.getUniqueId());
        if (pp != null) {
            party.addMember(pp);
            return true;
        }
        return false;
    }

    @Override
    public boolean addMember(UUID memberId) {
        PartyPlayer pp = Main.getInstance().getPartiesAPI().getPartyPlayer(memberId);
        if (pp != null) {
            party.addMember(pp);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeMember(Player player) {
        PartyPlayer pp = Main.getInstance().getPartiesAPI().getPartyPlayer(player.getUniqueId());
        if (pp != null) {
            party.removeMember(pp);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeMember(UUID memberId) {
        PartyPlayer pp = Main.getInstance().getPartiesAPI().getPartyPlayer(memberId);
        if (pp != null) {
            party.removeMember(pp);
            return true;
        }
        return false;
    }

    @Override
    public boolean isMember(Player player) {
        return party.getMembers().contains(player.getUniqueId());
    }

    @Override
    public boolean isMember(UUID memberId) {
        return party.getMembers().contains(memberId);
    }

    @Override
    public int getSize() {
        return party.getMembers().size();
    }

    @Override
    public boolean areAllMembersOnline() {
        for (UUID uuid : party.getMembers()) {
            if (Bukkit.getPlayer(uuid) == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void disband() {
        party.delete();
    }

    @Override
    public boolean exists() {
        return Main.getInstance().getPartiesAPI().getParty(party.getId()) != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlessioDPParty that = (AlessioDPParty) o;
        return party.getId().equals(that.party.getId());
    }

    @Override
    public int hashCode() {
        return party.getId().hashCode();
    }

    @Override
    public String toString() {
        return "AlessioDPParty{" +
                "id=" + party.getId() +
                ", name='" + party.getName() + '\'' +
                ", leader=" + party.getLeader() +
                ", members=" + party.getMembers().size() +
                '}';
    }
}

