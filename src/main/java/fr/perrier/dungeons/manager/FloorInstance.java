package fr.perrier.dungeons.manager;

import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.model.Floor;
import fr.perrier.dungeons.utils.ASWMUtil;
import lombok.Getter;
import org.bukkit.World;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Getter
public class FloorInstance {

    private final UUID instanceId;
    private final String floorId;
    private final World world;

    public FloorInstance(String floorId) {
        this.instanceId = UUID.randomUUID();
        this.floorId = floorId;
        this.world = generateFloorWorld();

    }

    private World generateFloorWorld() {
        return ASWMUtil.makeFloorInstance(this);
    }

    public String getInstanceName() {
        return floorId + "_" + instanceId.toString();
    }

    public Floor getFloor() {
        return Floor.getFloor(floorId);
    }
}
