package fr.perrier.dungeons.menu.dungeon;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.UUID;

@Getter
@Setter
@RequiredArgsConstructor
public class PartyFinderConfiguration {
    private static final HashMap<UUID, PartyFinderConfiguration> partyFinderConfigurations = new HashMap<>();

    private final String dungeonName;
    private String floorFilter = "";
    private String descriptionFilter = "";
    private int minimumLevelFilter = 0;

    public static PartyFinderConfiguration getConfigForPlayer(UUID uuid, String dungeonName) {
        if(!partyFinderConfigurations.containsKey(uuid)) {
            partyFinderConfigurations.put(uuid, new PartyFinderConfiguration(dungeonName));
        } else {
            PartyFinderConfiguration configuration = partyFinderConfigurations.get(uuid);
            if(!configuration.getDungeonName().equals(dungeonName)) {
                partyFinderConfigurations.remove(uuid);
                partyFinderConfigurations.put(uuid, new PartyFinderConfiguration(dungeonName));
            }
        }
        return partyFinderConfigurations.get(uuid);
    }
}
