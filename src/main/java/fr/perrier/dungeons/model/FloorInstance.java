package fr.perrier.dungeons.model;

import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.utils.ServerUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

@Getter
public class FloorInstance {

    private final UUID instanceId;
    private final String floorId;
    private boolean ready;

    public FloorInstance(String floorId) {
        this.floorId = floorId;
        this.instanceId = generateFloorServer();
        this.ready = false;

        Main.getInstance().getRedisStorageService().syncInstance(this);
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

    public void setReady(boolean ready) {
        this.ready = ready;
        Main.getInstance().getRedisStorageService().syncInstance(this);
    }

    /**
     * Send a player to this instance's server
     * @param player The player to send
     */
    public void sendToServer(Player player) {
        Main.getInstance().getLogger().info(String.format("[%s] Attempting to send %s to instance %s",
                Instant.now(), player.getName(), instanceId));

        new BukkitRunnable() {
            private final long startTime = System.currentTimeMillis();
            private static final long TIMEOUT = 60000;

            @Override
            public void run() {
                FloorInstance instance = Main.getInstance().getRedisStorageService().getInstance(instanceId);

                if (instance == null) {
                    Main.getInstance().getLogger().warning(String.format("[%s] Instance %s no longer exists",
                            Instant.now(), instanceId));
                    player.sendMessage(ChatUtil.translate("&cThis dungeon instance no longer exists!"));
                    this.cancel();
                    return;
                }

                if (instance.isReady()) {
                    ServerUtil.sendToServer(player, instanceId);
                    this.cancel();
                } else {
                    if (System.currentTimeMillis() - startTime > TIMEOUT) {
                        Main.getInstance().getLogger().warning(String.format("[%s] Timed out waiting for instance %s to be ready",
                                Instant.now(), instanceId));
                        player.sendMessage(ChatUtil.translate("&cTimed out waiting for dungeon instance to be ready!"));
                        this.cancel();
                    }
                }
            }
        }.runTaskTimerAsynchronously(Main.getInstance(), 0L, 20L);
    }


    @Override
    public String toString() {
        return "FloorInstance{" +
                "instanceId=" + instanceId +
                ", floorId='" + floorId + '\'' +
                '}';
    }
}
