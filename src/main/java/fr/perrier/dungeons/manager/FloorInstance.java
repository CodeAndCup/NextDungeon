package fr.perrier.dungeons.manager;

import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.model.Floor;
import fr.perrier.dungeons.utils.ServerUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

@Getter
public class FloorInstance {

    @Getter
    private static final HashMap<UUID, FloorInstance> instances = new HashMap<>();

    private final UUID instanceId;
    private final String floorId;
    @Setter
    private boolean ready;

    public FloorInstance(String floorId) {
        this.floorId = floorId;
        this.instanceId = generateFloorServer();
        this.ready = false;
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

    public void sendToServer(Player player) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), () -> {
            if(isReady())
                ServerUtil.sendToServer(player, instanceId);

        },0,20L);
    }


    @Override
    public String toString() {
        return "FloorInstance{" +
                "instanceId=" + instanceId +
                ", floorId='" + floorId + '\'' +
                '}';
    }
}
