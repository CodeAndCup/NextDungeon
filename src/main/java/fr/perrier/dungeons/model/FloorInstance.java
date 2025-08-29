package fr.perrier.dungeons.model;

import com.alessiodp.parties.api.interfaces.Party;
import com.cryptomorin.xseries.messages.Titles;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.parties.DungeonParty;
import fr.perrier.dungeons.utils.ServerUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class FloorInstance {

    private static final List<String> LOADING = Arrays.asList(
            "<gradient:#00AA00>▁▂▃▅▆▇▉▉▇▆▅▃▂▁</gradient:#00EE00>",
            "<gradient:#009900>▁▁▂▃▅▆▇▉▉▇▆▅▃▂</gradient:#00FF00>",
            "<gradient:#00AA00>▂▁▁▂▃▅▆▇▉▉▇▆▅▃</gradient:#00EE00>",
            "<gradient:#00BB00>▃▂▁▁▂▃▅▆▇▉▉▇▆▅</gradient:#00DD00>",
            "<gradient:#00CC00>▅▃▂▁▁▂▃▅▆▇▉▉▇▆</gradient:#00CC00>",
            "<gradient:#00DD00>▆▅▃▂▁▁▂▃▅▆▇▉▉▇</gradient:#00BB00>",
            "<gradient:#00EE00>▇▆▅▃▂▁▁▂▃▅▆▇▉▉</gradient:#00AA00>",
            "<gradient:#00FF00>▉▇▆▅▃▂▁▁▂▃▅▆▇▉</gradient:#009900>",
            "<gradient:#00FF00>▉▉▇▆▅▃▂▁▁▂▃▅▆▇</gradient:#00AA00>",
            "<gradient:#00EE00>▇▉▉▇▆▅▃▂▁▁▂▃▅▆</gradient:#00BB00>",
            "<gradient:#00DD00>▅▆▇▉▉▇▆▅▃▂▁▁▂▃</gradient:#00CC00>",
            "<gradient:#00BB00>▂▃▅▆▇▉▉▇▆▅▃▂▁▁</gradient:#00DD00>"
    );

    private final UUID instanceId;
    private final String floorId;
    private boolean ready;

    public FloorInstance(String floorId) {
        this.floorId = floorId;
        this.instanceId = generateFloorServer(false);
        this.ready = false;

        Main.getInstance().getRedisStorageService().syncInstance(this);
    }

    public FloorInstance(String floorId, boolean editMode) {
        this.floorId = floorId;
        this.instanceId = generateFloorServer(editMode);
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
    private UUID generateFloorServer(boolean editMode) {
        return ServerUtil.makeFloorInstance(this,editMode);
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
     * Sends all members of the given dungeon party to the cloud service associated with this instance.
     * <p>
     * This method first checks if all members of the party are online using {@link DungeonParty#hasAllMembersOnline()}.
     * If they are, it iterates through each member's UUID, retrieves the corresponding Player object using
     * {@link Bukkit#getPlayer(UUID)}, and sends them to the instance using {@link #sendToServer(Player)}.
     * If not all members are online, it sends a message to the party leader informing them that all members
     * must be online to join the instance.
     * </p>
     * @param dungeonParty the dungeon party whose members are to be sent to the cloud service
     */
    public void sendToServer(DungeonParty dungeonParty) {
        if(dungeonParty.hasAllMembersOnline()) {
            for (UUID uuid : dungeonParty.getParty().getMembers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null)
                    sendToServer(player);
            }
        }else {
            Player leader = Bukkit.getPlayer(Objects.requireNonNull(dungeonParty.getLeader()));
            if (leader != null)
                leader.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cAll your party members must be online to join the instance!"));
        }
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
        Main.getInstance().getLogger().info(String.format("Attempting to send %s to instance %s", player.getName(), instanceId));

        AtomicInteger timerDelay = new AtomicInteger(0);
        AtomicInteger currentLoad = new AtomicInteger(0);

        new BukkitRunnable() {
            private final long startTime = System.currentTimeMillis();
            private static final long TIMEOUT = 60000;

            @Override
            public void run() {
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    Titles.sendTitle(player, 0, 3, 0, " ", ChatUtil.translate(LOADING.get(currentLoad.get())));
                    if(currentLoad.get() +1 >= LOADING.size()) {
                        currentLoad.set(0);
                        return;
                    }
                    currentLoad.incrementAndGet();
                });

                if (timerDelay.get() == 9) {
                    FloorInstance instance = Main.getInstance().getRedisStorageService().getInstance(instanceId);

                    if (instance == null) {
                        Main.getInstance().getLogger().warning(String.format("&eInstance %s no longer exists", instanceId));
                        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cThis dungeon instance no longer exists!"));
                        this.cancel();
                        return;
                    }

                    if (instance.isReady()) {
                        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fInstance is &aready&f! Sending you to the dungeon..."));
                        ServerUtil.sendToServer(player, instanceId);
                        this.cancel();
                    } else {
                        if (System.currentTimeMillis() - startTime > TIMEOUT) {
                            Main.getInstance().getLogger().warning(String.format("&eTimed out waiting for instance %s to be ready", instanceId));
                            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cTimed out waiting for dungeon instance to be ready!"));
                            this.cancel();
                        }
                    }
                    timerDelay.set(0);
                } else {
                    timerDelay.incrementAndGet();
                }
            }
        }.runTaskTimerAsynchronously(Main.getInstance(), 0L, 2L);
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
