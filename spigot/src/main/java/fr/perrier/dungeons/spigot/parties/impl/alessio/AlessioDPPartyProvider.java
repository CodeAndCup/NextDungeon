package fr.perrier.dungeons.spigot.parties.impl.alessio;

import com.alessiodp.parties.api.Parties;
import com.alessiodp.parties.api.interfaces.PartiesAPI;
import com.alessiodp.parties.api.interfaces.Party;
import com.alessiodp.parties.api.interfaces.PartyPlayer;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.parties.IParty;
import fr.perrier.dungeons.spigot.parties.IPartyMember;
import fr.perrier.dungeons.spigot.parties.IPartyProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of IPartyProvider for AlessioDPParties plugin.
 */
public class AlessioDPPartyProvider implements IPartyProvider {

    private static final String PROVIDER_NAME = "AlessioDPParties";
    private PartiesAPI api;

    public AlessioDPPartyProvider() {
        initialize();
    }

    private void initialize() {
        if (Bukkit.getPluginManager().getPlugin("Parties") != null) {
            try {
                this.api = Parties.getApi();
            } catch (Exception e) {
                Main.getLoggerUtil().warning("Failed to initialize AlessioDPParties API: " + e.getMessage());
            }
        }
    }

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isAvailable() {
        return api != null;
    }

    private PartiesAPI getApi() {
        if (api == null) {
            initialize();
        }
        return api;
    }

    @Override
    public Optional<IParty> createParty(Player leader, String name) {
        PartiesAPI api = getApi();
        if (api == null) return Optional.empty();

        PartyPlayer pp = api.getPartyPlayer(leader.getUniqueId());
        if (pp == null) return Optional.empty();

        // AlessioDPParties createParty returns boolean, so we create and then get
        boolean created = api.createParty(name, pp);
        if (!created) return Optional.empty();

        // Get the party that was just created for this player
        return getPartyByLeader(leader);
    }

    @Override
    public Optional<IParty> getParty(UUID partyId) {
        PartiesAPI api = getApi();
        if (api == null) return Optional.empty();

        Party party = api.getParty(partyId);
        return party != null ? Optional.of(new AlessioDPParty(party)) : Optional.empty();
    }

    @Override
    public Optional<IParty> getPartyByLeader(Player leader) {
        return getPartyByLeader(leader.getUniqueId());
    }

    @Override
    public Optional<IParty> getPartyByLeader(UUID leaderId) {
        PartiesAPI api = getApi();
        if (api == null) return Optional.empty();

        return api.getOnlineParties().stream()
                .filter(party -> Objects.equals(party.getLeader(), leaderId))
                .findFirst()
                .map(AlessioDPParty::new);
    }

    @Override
    public Optional<IParty> getPartyOf(Player player) {
        return getPartyOf(player.getUniqueId());
    }

    @Override
    public Optional<IParty> getPartyOf(UUID memberId) {
        PartiesAPI api = getApi();
        if (api == null) return Optional.empty();

        PartyPlayer pp = api.getPartyPlayer(memberId);
        if (pp == null || !pp.isInParty()) return Optional.empty();

        UUID partyId = pp.getPartyId();
        if (partyId == null) return Optional.empty();

        Party party = api.getParty(partyId);
        return party != null ? Optional.of(new AlessioDPParty(party)) : Optional.empty();
    }

    @Override
    public Collection<IParty> getOnlineParties() {
        PartiesAPI api = getApi();
        if (api == null) return Collections.emptyList();

        return api.getOnlineParties().stream()
                .map(AlessioDPParty::new)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<IParty> findPartiesByDescription(String partOfDesc) {
        PartiesAPI api = getApi();
        if (api == null) return Collections.emptyList();

        return api.getOnlineParties().stream()
                .filter(party -> party.getDescription() != null)
                .filter(party -> party.getDescription().toLowerCase().contains(partOfDesc.toLowerCase()))
                .map(AlessioDPParty::new)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<IPartyMember> getPartyMember(Player player) {
        return getPartyMember(player.getUniqueId());
    }

    @Override
    public Optional<IPartyMember> getPartyMember(UUID memberId) {
        PartiesAPI api = getApi();
        if (api == null) return Optional.empty();

        PartyPlayer pp = api.getPartyPlayer(memberId);
        return pp != null ? Optional.of(new AlessioDPPartyMember(pp)) : Optional.empty();
    }

    @Override
    public boolean isInParty(Player player) {
        PartiesAPI api = getApi();
        if (api == null) return false;

        PartyPlayer pp = api.getPartyPlayer(player.getUniqueId());
        return pp != null && pp.isInParty();
    }

    @Override
    public boolean isPartyLeader(Player player) {
        PartiesAPI api = getApi();
        if (api == null) return false;

        PartyPlayer pp = api.getPartyPlayer(player.getUniqueId());
        if (pp == null || !pp.isInParty()) return false;

        UUID partyId = pp.getPartyId();
        if (partyId == null) return false;

        Party party = api.getParty(partyId);
        return party != null && Objects.equals(party.getLeader(), player.getUniqueId());
    }
}

