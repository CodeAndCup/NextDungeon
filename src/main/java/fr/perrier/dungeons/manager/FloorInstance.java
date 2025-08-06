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

    public FloorInstance(String floorId) {
        this.instanceId = generateFloorServer();
        this.floorId = floorId;
        instances.put(instanceId, this);
    }

    private UUID generateFloorServer() {
        return ServerUtil.makeFloorInstance(this);
    }

    public String getInstanceName() {
        return floorId + "_" + instanceId.toString();
    }

    public Floor getFloor() {
        return Floor.getFloor(floorId);
    }
}
