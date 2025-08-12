package fr.perrier.dungeons.manager;

import fr.perrier.dungeons.model.FloorInstance;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class RaidManager {

    @Getter
    private final static HashMap<Player, FloorInstance> playerFloorInstance = new HashMap<>();
}
