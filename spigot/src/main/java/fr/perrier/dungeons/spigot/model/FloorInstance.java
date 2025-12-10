package fr.perrier.dungeons.spigot.model;

import com.cryptomorin.xseries.messages.Titles;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.cupcodeapi.utils.TimeUtil;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.parties.DungeonParty;
import fr.perrier.dungeons.spigot.utils.ServerUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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

    private final HashMap<UUID, PlayerStats> playerStats = new HashMap<>();
    private final HashMap<UUID, Integer> playerCurrentLives = new HashMap<>();

    private FloorInstance(String floorId, boolean editMode) {
        this.floorId = floorId;
        this.instanceId = generateFloorServer(editMode);
        this.ready = false;

        Main.getInstance().getRedisStorageService().syncInstance(this);
    }

    public static void generateNewInstanceAsync(String floorId, boolean editMode, Consumer<FloorInstance> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            FloorInstance floorInstance = new FloorInstance(floorId, editMode);
            Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                callback.accept(floorInstance)
            );
        });
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
     * Completes the dungeon instance for all players currently in it.
     *
     * <p>This method iterates through all online players, checks if they have
     * associated PlayerStats, and if so, updates their ProfileData with the
     * completed floor information and statistics. It then sends a congratulatory
     * title and message to each player with their performance details. Finally,
     * it schedules a task to shut down the server instance after a 30-second delay,
     * notifying all players of the impending shutdown.</p>
     */
    public void complete() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            PlayerStats playerStats = this.playerStats.get(player.getUniqueId());
            if(playerStats == null) continue;

            ProfileData profileData = Main.getInstance().getProfileService().getProfileData(player.getUniqueId());
            profileData.addCompletedFloor(floorId);
            profileData.addFloorStat(new ProfileData.FloorStats(floorId, playerStats.getStartTime(), playerStats.getEnemiesKilled(), playerStats.getDeaths()));
            Main.getInstance().getProfileService().saveProfileData(player.getUniqueId());

            Titles.sendTitle(player, 10, 70, 20,
                    ChatUtil.translate("&f&l" + ChatUtil.toSmallCaps("Congratulations!!")),
                    ChatUtil.translate("&#FFBB00&l" + ChatUtil.toSmallCaps("Dungeon Complete")));

            Dungeon currentDungeon = Dungeon.getDungeon(floorId.split("_")[0]);

            player.sendMessage(ChatUtil.getBar());
            player.sendMessage(ChatUtil.translate("&#D10000Dungeon: &#D63333" + ChatUtil.toSmallCaps(currentDungeon.getName())));
            player.sendMessage(ChatUtil.translate("&#D10000Floor: &#D68533" + ChatUtil.toSmallCaps(getFloor().getName())));
            player.sendMessage(ChatUtil.translate("&#D10000Time: &f" + TimeUtil.getDuration(System.currentTimeMillis() - playerStats.getStartTime())));
            player.sendMessage(ChatUtil.translate("&#D10000Enemies killed: &f" + playerStats.getEnemiesKilled()));
            player.sendMessage(ChatUtil.translate("&#D10000Deaths: &f" + playerStats.getDeaths()));
            player.sendMessage(ChatUtil.getBar());
        }

        Bukkit.broadcastMessage(ChatUtil.translate(Main.getPrefix() + "&fThe dungeon instance &e" + getInstanceName() + " &fwill shut down in &c30 &fseconds."));

        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            Main.getInstance().getRedisStorageService().removeInstance(this.instanceId);
            Bukkit.shutdown();
        }, 20L * 30);
    }

    @Getter
    @Setter
    public static class PlayerStats {
        private final UUID playerId;
        private int enemiesKilled;
        private int deaths;
        private long startTime;

        public PlayerStats(UUID playerId) {
            this.playerId = playerId;
            this.enemiesKilled = 0;
            this.deaths = 0;
            this.startTime = System.currentTimeMillis();
        }

        /**
         * Increments the count of enemies killed by the player.
         */
        public void incrementEnemiesKilled() {
            this.enemiesKilled++;
        }

        /**
         * Increments the count of deaths for the player.
         */
        public void incrementDeaths() {
            this.deaths++;
        }
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
