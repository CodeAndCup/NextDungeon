package fr.perrier.dungeons.model;

import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.utils.ServerUtil;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Instant;
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

    /**
     * Generates a unique server instance for the current floor.
     *
     * <p>This method utilizes the ServerUtil to create a cloud service
     * instance corresponding to this FloorInstance. It returns the unique
     * identifier (UUID) of the created server instance.</p>
     *
     * @return the unique UUID of the generated server instance
     */
    private UUID generateFloorServer() {
        return ServerUtil.makeFloorInstance(this);
    }

    /**
     * Gets the name of this instance.
     * <p>
     * The name is in the format of {@code <floorId>_<instanceId>}.
     * @return the name of this instance
     */
    public String getInstanceName() {
        return floorId + "_" + instanceId.toString();
    }

    /**
     * Retrieves the Floor object associated with this FloorInstance.
     *
     * <p>This method utilizes the floorId of this instance to fetch
     * the corresponding Floor from the storage mechanism.</p>
     *
     * @return the Floor object linked to this instance, or null if not found
     */
    public Floor getFloor() {
        return Floor.getFloor(floorId);
    }

    /**
     * Sets the readiness state of this floor instance.
     *
     * <p>This method updates the ready state of the instance and
     * synchronizes the updated state with Redis to ensure that the
     * instance's status is consistent across all servers.</p>
     *
     * @param ready the new readiness state to set for this instance
     */
    public void setReady(boolean ready) {
        this.ready = ready;
        Main.getInstance().getRedisStorageService().syncInstance(this);
    }


    /**
     * Sends the given player to the cloud service associated with this instance.
     * <p>
     * This method will first check if the instance is ready. If it is, it will
     * send the player to the instance using {@link ServerUtil#sendToServer(Player, UUID)}.
     * If the instance is not ready, it will wait for up to 1 minute and check every
     * 20 ticks if the instance is ready. If the instance is still not ready after
     * the timeout, it will send a message to the player and stop waiting.
     * </p>
     * @param player the player to send to the cloud service
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


    /**
     * Returns a string representation of the FloorInstance.
     * The string includes the instance ID and the floor ID.
     *
     * @return a string in the format "FloorInstance{instanceId=<instanceId>, floorId='<floorId>'}"
     */
    @Override
    public String toString() {
        return "FloorInstance{" +
                "instanceId=" + instanceId +
                ", floorId='" + floorId + '\'' +
                '}';
    }
}
