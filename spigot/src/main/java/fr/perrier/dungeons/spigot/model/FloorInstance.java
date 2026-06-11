package fr.perrier.dungeons.spigot.model;

import com.cryptomorin.xseries.messages.Titles;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.cupcodeapi.utils.TimeUtil;
import fr.perrier.dungeons.common.model.dungeon.config.FloorInstanceData;
import fr.perrier.dungeons.common.model.player.PlayerStats;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.messaging.packets.CancelInstancePacket;
import fr.perrier.dungeons.spigot.messaging.packets.CrossServerSendToInstancePacket;
import fr.perrier.dungeons.spigot.parties.IDungeonParty;
import fr.perrier.dungeons.spigot.utils.LoggerUtil;
import fr.perrier.dungeons.spigot.utils.ServerUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.bukkit.scheduler.BukkitTask;

@Getter
public class FloorInstance extends FloorInstanceData {

    // Loading-bar tasks scheduled by sendToServer(Player), keyed by player
    // UUID so cancelInstance() (or a repeated send) can cancel them and
    // avoid leaking an async timer that references a player who may have
    // already disconnected.
    private final transient Map<UUID, BukkitTask> pendingLoadTasks = new ConcurrentHashMap<>();

    @Getter
    private enum LoadingBar {
        WAVE(Arrays.asList(
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
        )),
        BOX(Arrays.asList(
                "&#00DD00▮&0▯▯▯▯▯▯▯▯▯",
                "&0▯&#00DD00▮&0▯▯▯▯▯▯▯▯",
                "&0▯▯&#00DD00▮&0▯▯▯▯▯▯▯",
                "&0▯▯▯&#00DD00▮&0▯▯▯▯▯▯",
                "&0▯▯▯▯&#00DD00▮&0▯▯▯▯▯",
                "&0▯▯▯▯▯&#00DD00▮&0▯▯▯▯",
                "&0▯▯▯▯▯▯&#00DD00▮&0▯▯▯",
                "&0▯▯▯▯▯▯▯&#00DD00▮&0▯▯",
                "&0▯▯▯▯▯▯▯▯&#00DD00▮&0▯",
                "&0▯▯▯▯▯▯▯▯▯&#00DD00▮",
                "&0▯▯▯▯▯▯▯▯&#00DD00▮&0▯",
                "&0▯▯▯▯▯▯▯&#00DD00▮&0▯▯",
                "&0▯▯▯▯▯▯&#00DD00▮&0▯▯▯",
                "&0▯▯▯▯▯&#00DD00▮&0▯▯▯▯",
                "&0▯▯▯▯&#00DD00▮&0▯▯▯▯▯",
                "&0▯▯▯&#00DD00▮&0▯▯▯▯▯▯",
                "&0▯▯&#00DD00▮&0▯▯▯▯▯▯▯",
                "&0▯&#00DD00▮&0▯▯▯▯▯▯▯▯"
        ))
        ;
        private final List<String> frames;
        LoadingBar(List<String> frames) {
            this.frames = frames;
        }

        public static LoadingBar getRandom() {
            LoadingBar[] values = LoadingBar.values();
            return values[new Random().nextInt(values.length)];
        }
    }

    private FloorInstance(String floorId, Set<UUID> players, boolean editMode) {
        super(floorId,players);
        this.instanceId = generateFloorServer(getFloor(), editMode);
        this.ready = false;

        syncInstance();
    }

    public FloorInstance(FloorInstanceData floorInstanceData) {
        super(floorInstanceData.getInstanceId(), floorInstanceData.getFloorId());
        this.ready = floorInstanceData.isReady();
        this.players.addAll(floorInstanceData.getPlayers());
        this.playerStats.putAll(floorInstanceData.getPlayerStats());
        this.playerCurrentLives.putAll(floorInstanceData.getPlayerCurrentLives());
        if (floorInstanceData.getOriginInstances() != null) {
            this.originInstances.putAll(floorInstanceData.getOriginInstances());
        }
    }

    public static void generateNewInstanceAsync(String floorId, Set<UUID> players, boolean editMode, Consumer<FloorInstance> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            FloorInstance floorInstance = new FloorInstance(floorId, players, editMode);
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
    private UUID generateFloorServer(Floor floor, boolean editMode) {
        return ServerUtil.makeFloorInstance(floor,editMode);
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
    @Override
    public void setReady(boolean ready) {
        boolean wasReady = this.ready;
        this.ready = ready;
        syncInstance();
        // Fire FloorInstanceReadyEvent on the false→true edge so modules
        // (e.g. memory-labyrinth) can take over a fresh instance without
        // polling. Always dispatched on the main thread.
        if (ready && !wasReady) {
            Runnable fire = () -> Bukkit.getPluginManager().callEvent(
                    new fr.perrier.dungeons.spigot.event.FloorInstanceReadyEvent(this));
            if (Bukkit.isPrimaryThread()) {
                fire.run();
            } else {
                Bukkit.getScheduler().runTask(Main.getInstance(), fire);
            }
        }
    }

    /**
     * Sends all members of the given dungeon party to the cloud service associated with this instance.
     * <p>
     * This method first checks if all members of the party are online using {@link IDungeonParty#areAllMembersOnline()}.
     * If they are, it iterates through each member's UUID, retrieves the corresponding Player object using
     * {@link Bukkit#getPlayer(UUID)}, and sends them to the instance using {@link #sendToServer(Player)}.
     * If not all members are online, it sends a message to the party leader informing them that all members
     * must be online to join the instance.
     * </p>
     * @param dungeonParty the dungeon party whose members are to be sent to the cloud service
     */
    public void sendToServer(IDungeonParty dungeonParty) {
        for (UUID uuid : dungeonParty.getMemberIds()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                // Member is on this server — run the normal loading-bar flow locally.
                sendToServer(player);
            } else {
                // Member is on a peer server. Broadcast a packet so their home server can run
                // the same sendToServer(Player) flow (loading bar, readiness wait, teleport).
                Main.getInstance().getMessaging().sendPacket(
                        new CrossServerSendToInstancePacket(uuid, this.instanceId));
            }
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
        Main.getLoggerUtil().info(String.format("Attempting to send %s to instance %s", player.getName(), instanceId));

        // Record the player's current lobby cloud service so complete() can send them back
        UUID currentServiceId = Main.getInstance().getInstanceProvider().getCurrentServiceUniqueId();
        if (currentServiceId != null) {
            this.originInstances.put(player.getUniqueId(), currentServiceId);
            syncInstance();
        } else {
            Main.getLoggerUtil().warning(String.format(
                    "Could not resolve origin service UUID for %s — will fall back to kick on completion",
                    player.getName()));
        }

        AtomicInteger timerDelay = new AtomicInteger(0);
        AtomicInteger currentLoad = new AtomicInteger(0);
        List<String> loadingBar = LoadingBar.getRandom().getFrames();
        UUID playerId = player.getUniqueId();

        BukkitTask task = new BukkitRunnable() {
            private final long startTime = System.currentTimeMillis();
            private final long timeout = Main.getInstance().getConfig().getInt("InstanceSettings.loadingTimeout",120) * 1000L;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    pendingLoadTasks.remove(playerId);
                    return;
                }
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    Titles.sendTitle(player, 0, 3, 0, ChatUtil.translate("&f" + ChatUtil.toSmallCaps("Loading Dungeon..")), ChatUtil.translate(loadingBar.get(currentLoad.get())));
                    if(currentLoad.get() +1 >= loadingBar.size()) {
                        currentLoad.set(0);
                        return;
                    }
                    currentLoad.incrementAndGet();
                });

                if (timerDelay.get() == 9) {
                    FloorInstance instance = Main.getInstance().getDungeonService().getInstance(instanceId);

                    if (instance == null) {
                        Main.getLoggerUtil().warning("Instance " + instanceId + "no longer exists.");
                        Main.getInstance().getDungeonService().removeInstance(this.instanceId);
                        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000This dungeon instance no longer exists!"));
                        this.cancel();
                        pendingLoadTasks.remove(playerId);
                        return;
                    }

                    if (instance.isReady()) {
                        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fInstance is &#00FF00ready&f! Sending you to the dungeon..."));
                        ServerUtil.sendToServer(player, instanceId);
                        this.cancel();
                        pendingLoadTasks.remove(playerId);
                    } else {
                        if (System.currentTimeMillis() - startTime > timeout) {
                            Main.getLoggerUtil().warning("Timed out waiting for instance " + instanceId + " to be ready. (Now try cancelling instance..)");
                            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Timed out waiting for dungeon instance to be ready!"));
                            Main.getInstance().getDungeonService().removeInstance(this.instanceId);
                            instance.cancelInstance();
                            this.cancel();
                            pendingLoadTasks.remove(playerId);
                        }
                    }
                    timerDelay.set(0);
                } else {
                    timerDelay.incrementAndGet();
                }
            }
        }.runTaskTimerAsynchronously(Main.getInstance(), 0L, 2L);

        // If a previous loading task for the same player is still pending
        // (rapid re-send), cancel it so we don't accumulate orphan timers.
        BukkitTask previous = pendingLoadTasks.put(playerId, task);
        if (previous != null) previous.cancel();
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
    public void complete(boolean success) {
        for(UUID playerId : getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if(player == null) continue;

            PlayerStats playerStats = getPlayerStats().get(player.getUniqueId());
            if(playerStats == null) {
                if(LoggerUtil.getInstance().isDebugEnabled())
                    LoggerUtil.getInstance().warning("PlayerStats for player " + playerId + " is null");
                continue;
            }

            ProfileData profileData = Main.getInstance().getProfileService().getProfileData(player.getUniqueId());
            if(success) {
                profileData.addCompletedFloor(floorId);
                
                // Remove floors from completion if specified in requirements
                if (getFloor().getRequirements() != null && getFloor().getRequirements().getRemoveCompletion() != null) {
                    for (String floorToRemove : getFloor().getRequirements().getRemoveCompletion()) {
                        profileData.getCompletedFloors().remove(floorToRemove);
                        Main.getLoggerUtil().info("Removed completed floor: " + floorToRemove + " from player: " + player.getName());
                    }
                }
            }
            profileData.addFloorStat(
                    new ProfileData.FloorStats(
                            floorId,
                            System.currentTimeMillis() - playerStats.getStartTime(),
                            playerStats.getEnemiesKilled(),
                            playerStats.getEnemiesKilled(),
                            playerStats.getDeaths(),
                            playerStats.getDeaths(),
                            1,
                            success ? 1 : 0

                    )
            );
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

        Bukkit.broadcastMessage(ChatUtil.translate(Main.getPrefix() + "&fThe dungeon instance &e" + getInstanceName() + " &fwill shut down in &#FF000030 &fseconds."));

        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () ->
            Bukkit.getOnlinePlayers().forEach(this::returnPlayerToOriginOrKick)
        , 20L * 20);

        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            Main.getInstance().getDungeonService().removeInstance(this.instanceId);
            Bukkit.shutdown();
        }, 20L * 30);
    }

    /**
     * Sends the player back to the cloud service they came from when they started the dungeon.
     * Falls back to kicking the player if no origin was recorded, if the origin service no longer
     * exists, or if the transfer fails.
     *
     * @param player the player to move out of the dungeon instance
     */
    private void returnPlayerToOriginOrKick(Player player) {
        UUID originServiceId = originInstances.get(player.getUniqueId());
        String kickMessage = ChatUtil.translate("&#FF0000The dungeon instance is shutting down! Thanks for playing!");

        if (originServiceId == null) {
            Main.getLoggerUtil().info(String.format(
                    "No origin recorded for %s — kicking instead of redirecting", player.getName()));
            player.kickPlayer(kickMessage);
            return;
        }

        Main.getInstance().getInstanceProvider().sendPlayerToInstance(player, originServiceId)
                .whenComplete((success, error) -> {
                    if (error != null || !Boolean.TRUE.equals(success)) {
                        Main.getLoggerUtil().warning(String.format(
                                "Failed to return %s to origin %s — kicking. Reason: %s",
                                player.getName(), originServiceId,
                                error != null ? error.getMessage() : "provider returned false"));
                        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                            if (player.isOnline()) player.kickPlayer(kickMessage);
                        });
                    }
                });
    }

    /**
     * Fails the dungeon instance for all players currently in it.
     *
     * <p>This method simply calls the complete method with a success parameter
     * set to false, indicating that the instance was not completed successfully.</p>
     */
    public void fail() {
        complete(false);
    }

    /**
     * Converts this FloorInstance to a FloorInstanceData object.
     *
     * <p>This method creates a new FloorInstanceData object using the
     * instanceId and floorId of this FloorInstance. It then sets the
     * ready state and copies the playerStats and playerCurrentLives
     * maps to the new object before returning it.</p>
     *
     * @return a FloorInstanceData object representing this FloorInstance
     */
    public FloorInstanceData toFloorInstanceData() {
        FloorInstanceData data = new FloorInstanceData(this.instanceId, this.floorId);
        data.setReady(this.ready);
        data.getPlayers().addAll(this.players);
        data.getPlayerStats().putAll(this.playerStats);
        data.getPlayerCurrentLives().putAll(this.playerCurrentLives);
        data.getOriginInstances().putAll(this.originInstances);
        return data;
    }

    /**
     * Checks if all players in the instance are dead.
     *
     * <p>This method iterates through the playerCurrentLives map, sums up the
     * current lives of all players, and returns true if the total is less than
     * or equal to zero, indicating that all players are dead.</p>
     *
     * @return true if all players are dead, false otherwise
     */
    public boolean isAllPlayersDead() {
        return playerCurrentLives.values().stream()
                .mapToInt(Integer::intValue)
                .sum() <= 0;
    }

    /**
     * Cancels the dungeon instance by sending a CancelInstancePacket to all servers.
     *
     * <p>This method creates a new CancelInstancePacket with the instanceId of this
     * FloorInstance and sends it through the messaging system. This will notify all
     * servers that the instance should be cancelled and removed.</p>
     */
    public void cancelInstance() {
        // Cancel any pending sendToServer loading bars so we don't leak
        // async timers referencing players that will never join.
        for (BukkitTask task : pendingLoadTasks.values()) task.cancel();
        pendingLoadTasks.clear();

        Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), () -> {
            FloorInstance instance = Main.getInstance().getDungeonService().getInstance(instanceId);
            if(instance != null && instance.isReady()) {
                Main.getInstance().getMessaging().sendPacket(new CancelInstancePacket(this.instanceId));
            }
        }, 20L, 20L);
    }

    /**
     * Synchronizes the current state of this FloorInstance with Redis.
     *
     * <p>This method converts this FloorInstance to a FloorInstanceData object
     * and then calls the syncInstance method of the DungeonService to update
     * the instance data in Redis. This ensures that all servers have the
     * latest information about this instance.</p>
     */
    public void syncInstance() {
        Main.getInstance().getDungeonService().syncInstance(this.toFloorInstanceData());
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
                "instanceId=" + instanceId + "," +
                "floorId='" + floorId + "'," +
                "instanceData=" + toFloorInstanceData() +
                '}';
    }
}
