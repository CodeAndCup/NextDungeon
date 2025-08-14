package fr.perrier.dungeons.manager;

import com.alessiodp.parties.api.interfaces.Party;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.parties.DungeonParty;

import java.util.List;

public class PartyManager {

    public static List<Party> findPartiesByDesc(String partOfDesc) {
        return Main.getInstance().getPartiesAPI().getOnlineParties().stream()
                .filter(party -> party.getDescription() != null)
                .filter(party -> party.getDescription().toLowerCase().contains(partOfDesc.toLowerCase()))
                .toList();
    }
}
