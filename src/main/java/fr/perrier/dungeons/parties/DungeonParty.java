package fr.perrier.dungeons.parties;

import com.alessiodp.parties.api.interfaces.Party;
import com.alessiodp.parties.api.interfaces.PartyPlayer;
import fr.perrier.dungeons.Main;
import lombok.Getter;
import lombok.Setter;
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

    public void setDungeonId(String dungeonId) {
        this.dungeonId = dungeonId;
        parties.put(party.getLeader(), this);
    }

    public void setFloorId(String floorId) {
        this.floorId = floorId;
        parties.put(party.getLeader(), this);
    }

    public void setMinLevel(int minLevel) {
        this.minLevel = minLevel;
        parties.put(party.getLeader(), this);
    }

    public void setDescription(String description) {
        this.description = description;
        parties.put(party.getLeader(), this);
    }

    public void addMember(Player player) {
        PartyPlayer partyPlayer = Main.getInstance().getPartiesAPI().getPartyPlayer(player.getUniqueId());
        party.addMember(Objects.requireNonNull(partyPlayer));
    }

    public void removeMember(Player player) {
        PartyPlayer partyPlayer = Main.getInstance().getPartiesAPI().getPartyPlayer(player.getUniqueId());
        party.removeMember(Objects.requireNonNull(partyPlayer));
    }

    public Set<PartyPlayer> getMembers() {
        return party.getOnlineMembers();
    }

    public boolean hasAllMembersOnline() {
        return party.getMembers().size() == party.getOnlineMembers().size();
    }

    public static class Builder {
        private String dungeonId;
        private String floorId;
        private int minLevel;
        private String description;
        private Party party;
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

        public DungeonParty build() {
            if(leader == null)
                throw new IllegalStateException("No leader has been set");

            Main.getInstance().getPartiesAPI().getOnlineParties().stream()
                    .filter(parties -> parties.getLeader().equals(leader.getUniqueId()))
                    .findFirst()
                    .ifPresent(party -> this.party = party);

            if(party == null) {
                Main.getInstance().getPartiesAPI().createParty(
                        "Party of " + leader.getName(),
                        Main.getInstance().getPartiesAPI().getPartyPlayer(leader.getUniqueId())
                );
                party = Main.getInstance().getPartiesAPI().getParty(leader.getUniqueId());
            }
            return new DungeonParty(dungeonId, floorId, minLevel, description, party);
        }
    }
}
