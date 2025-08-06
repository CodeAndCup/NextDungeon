package fr.perrier.dungeons.manager;

import fr.perrier.dungeons.model.Floor;
import fr.perrier.dungeons.utils.ServerUtil;
import lombok.Getter;
import org.bukkit.World;

import java.util.HashMap;
import java.util.UUID;

@Getter
public class FloorInstance {

    @Getter
    private static final HashMap<UUID, FloorInstance> instances = new HashMap<>();

    private final UUID instanceId;
    private final String floorId;
    //private final World world;

    public FloorInstance(String floorId) {
        this.instanceId = UUID.randomUUID();
        this.floorId = floorId;
        //this.world = generateFloorWorld();
        instances.put(instanceId, this);
        generateFloorWorld();
    }

    private void generateFloorWorld() {
        ServerUtil.makeFloorInstance(this);
    }

    public String getInstanceName() {
        return floorId + "_" + instanceId.toString();
    }

    public Floor getFloor() {
        return Floor.getFloor(floorId);
    }
}
