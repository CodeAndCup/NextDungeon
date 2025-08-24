package fr.perrier.dungeons.storage;

import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.model.Dungeon;
import fr.perrier.dungeons.model.FloorInstance;
import fr.perrier.dungeons.messaging.redis.RedisMessage;
import fr.perrier.dungeons.model.Floor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.RMap;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public class RedisStorageService {
    private final RedissonClient redissonClient;

    // Redis Maps and Topics
    private static final String DUNGEON_MAP = "dungeons:dungeons";
    private static final String FLOOR_MAP = "dungeons:floors";
    private static final String INSTANCE_MAP = "dungeons:instances";
    private static final String SYNC_CHANNEL = "dungeons:sync";

    // Local references to current floor and instance
    @Getter
    private final AtomicReference<Floor> currentFloor = new AtomicReference<>();
    @Getter
    private final AtomicReference<FloorInstance> currentInstance = new AtomicReference<>();

    // Redis Maps
    @Getter
    private RMap<String, Dungeon> dungeonsMap;
    @Getter
    private RMap<String, Floor> floorsMap;
    @Getter
    private RMap<UUID, FloorInstance> instancesMap;

    private RTopic syncTopic;

    /**
     * Initializes the Redis storage service.
     * This method should be called once when the plugin is enabled.
     * It will initialize the maps and topics needed for the Redis storage service.
     * It will also subscribe to the sync topic, which is used to notify other servers
     * of floor and instance updates.
     */
    public void initialize() {
        this.dungeonsMap = redissonClient.getMap(DUNGEON_MAP);
        this.floorsMap = redissonClient.getMap(FLOOR_MAP);
        this.instancesMap = redissonClient.getMap(INSTANCE_MAP);
        this.syncTopic = redissonClient.getTopic(SYNC_CHANNEL);

        // Subscribe to sync messages
        syncTopic.addListener(RedisMessage.class, (channel, msg) -> {
            handleSyncMessage(msg);
        });
    }

    /**
     * Initializes the current instance and floor from Redis.
     * This method should be called when the plugin is enabled, and the instance
     * and floor should be retrieved from Redis.
     * If the instance or floor is not found, it will log an error and return.
     *
     * @param instanceId the unique ID of the instance to initialize
     * @param floorId    the ID of the floor to initialize
     */
    public void initializeInstance(UUID instanceId, String floorId) {
        // Get instance from Redis
        FloorInstance instance = instancesMap.get(instanceId);
        if (instance == null) {
            Main.getInstance().getLogger().severe(String.format(
                    "Could not find instance %s in Redis",
                    instanceId
            ));
            return;
        }

        // Set current instance
        currentInstance.set(instance);

        // Get and set floor
        Floor floor = floorsMap.get(floorId);
        if (floor == null) {
            Main.getInstance().getLogger().severe(String.format(
                    "Could not find floor %s in Redis",
                    floorId
            ));
            return;
        }

        currentFloor.set(floor);

        Main.getInstance().getLogger().info(String.format(
                "Successfully initialized instance server (Instance: %s, Floor: %s)",
                instanceId,
                floorId
        ));
    }

    /**
     * Synchronizes the given dungeon to Redis and notifies other servers.
     * This method will update the Redis dungeon map and notify other servers
     * of the update.
     *
     * @param dungeon the dungeon to synchronize.
     */
    public void syncDungeon(Dungeon dungeon) {
        // Update Redis
        dungeonsMap.fastPut(dungeon.getId(),dungeon);

        Main.getInstance().getLogger().info(
                String.format("Synced dungeon %s to Redis", dungeon.getId())
        );
    }

    /**
     * Synchronizes the given floor to Redis and notifies other servers.
     * This method will update the local reference, update the Redis floor map,
     * and notify other servers of the update.
     *
     * @param floor the floor to synchronize.
     */
    public void syncFloor(Floor floor) {
        RedisMessage<Floor> message = RedisMessage.create(
                SYNC_CHANNEL,
                Bukkit.getServer().getName(),
                RedisMessage.MessageType.FLOOR_UPDATE,
                floor
        );

        // Update local reference
        currentFloor.set(floor);

        // Update Redis
        floorsMap.fastPut(floor.getId(), floor);

        // Notify other servers
        syncTopic.publish(message);

        Main.getInstance().getLogger().info(
                String.format("Synced floor %s to Redis", floor.getId())
        );
    }

    /**
     * Synchronizes the given instance to Redis and notifies other servers.
     * This method will update the local reference, update the Redis instance map,
     * and notify other servers of the update.
     *
     * @param instance the floor instance to synchronize.
     */
    public void syncInstance(FloorInstance instance) {
        RedisMessage<FloorInstance> message = RedisMessage.create(
                SYNC_CHANNEL,
                Bukkit.getServer().getIp() + ":" + Bukkit.getServer().getPort(),
                RedisMessage.MessageType.INSTANCE_UPDATE,
                instance
        );

        // Update local reference
        currentInstance.set(instance);

        // Update Redis
        instancesMap.fastPut(instance.getInstanceId(), instance);

        // Notify other servers
        syncTopic.publish(message);

        Main.getInstance().getLogger().info(
                String.format("Synced instance %s to Redis", instance.getInstanceId())
        );
    }

    /**
     * Handle a sync message received from Redis.
     * If the message is a floor or instance update, only update the local reference
     * if the message is not from this server and the message is about our current
     * floor or instance.
     *
     * @param message the message received
     */
    private void handleSyncMessage(RedisMessage<?> message) {
        String serverIdentity = Bukkit.getServer().getIp() + ":" + Bukkit.getServer().getPort();
        if (!message.getSender().equals(serverIdentity)) { // Don't handle own messages
            switch (message.getType()) {
                case FLOOR_UPDATE -> {
                    Floor floor = (Floor) message.getData();
                    // Only update if it's our current floor
                    if (currentFloor.get() != null &&
                            currentFloor.get().getId().equals(floor.getId())) {
                        currentFloor.set(floor);
                        Main.getInstance().getLogger().info(
                                String.format("Updated local floor: %s from Redis", floor.getId())
                        );
                    }
                }
                case INSTANCE_UPDATE -> {
                    FloorInstance instance = (FloorInstance) message.getData();
                    // Only update if it's our current instance
                    if (currentInstance.get() != null &&
                            currentInstance.get().getInstanceId().equals(instance.getInstanceId())) {
                        currentInstance.set(instance);
                        Main.getInstance().getLogger().info(
                                String.format("Updated local instance: %s from Redis",
                                        instance.getInstanceId())
                        );
                    }
                }
                case INSTANCE_REMOVE -> {
                    FloorInstance instance = (FloorInstance) message.getData();
                    if (currentInstance.get() != null &&
                            currentInstance.get().getInstanceId().equals(instance.getInstanceId())) {
                        currentInstance.set(null);
                        currentFloor.set(null);
                        Main.getInstance().getLogger().info(String.format("Removed local instance: %s", instance.getInstanceId()));
                    }
                }
            }
        }
    }

    /**
     * Retrieve a dungeon by its unique ID.
     *
     * @param id the unique ID of the dungeon to retrieve
     * @return the Dungeon object with the given ID, or null if not found
     */
    public Dungeon getDungeon(String id) {
        return dungeonsMap.get(id);
    }

    /**
     * Retrieve a floor by its unique ID.
     * This method first checks if the requested floor is the current local floor.
     * If it is, the local floor is returned. If not, the method retrieves the
     * floor from the Redis storage.
     *
     * @param id the unique ID of the floor to retrieve
     * @return the Floor object with the given ID, or null if not found
     */
    public @Nullable Floor getFloor(String id) {
        Floor localFloor = currentFloor.get();
        if (localFloor != null && localFloor.getId().equals(id)) {
            return localFloor;
        }

        return floorsMap.get(id);
    }

    /**
     * Retrieve a floor instance by its unique ID.
     *
     * @param id the unique ID of the instance to retrieve
     * @return the instance with the given ID, or null if not found
     */
    public FloorInstance getInstance(UUID id) {
        FloorInstance localInstance = currentInstance.get();
        if (localInstance != null && localInstance.getInstanceId().equals(id)) {
            return localInstance;
        }

        return instancesMap.get(id);
    }

    /**
     * Clear local references when server is shutting down or instance is complete
     */
    public void clearLocal() {
        currentFloor.set(null);
        currentInstance.set(null);
        Main.getInstance().getLogger().info("Cleared local floor and instance references");
    }

    public void removeDungeon(String id) {
        // Remove from Redis
        dungeonsMap.remove(id);
        Main.getInstance().getLogger().info(String.format("Removed dungeon %s from Redis", id));
    }

    /**
     * Remove an instance from Redis and notify other servers
     * @param instanceId the ID of the instance to remove
     */
    public void removeInstance(UUID instanceId) {
        FloorInstance instance = instancesMap.get(instanceId);
        if (instance == null) {
            Main.getInstance().getLogger().warning(String.format("Tried to remove non-existent instance: %s", instanceId));
            return;
        }

        // Create removal message
        RedisMessage<FloorInstance> message = RedisMessage.create(
                SYNC_CHANNEL,
                Bukkit.getServer().getName(),
                RedisMessage.MessageType.INSTANCE_REMOVE,
                instance
        );

        // Remove from Redis
        instancesMap.remove(instanceId);

        // Clear local reference if it's our instance
        FloorInstance localInstance = currentInstance.get();
        if (localInstance != null && localInstance.getInstanceId().equals(instanceId)) {
            currentInstance.set(null);
            currentFloor.set(null);
        }

        // Notify other servers
        syncTopic.publish(message);

        Main.getInstance().getLogger().info(String.format("Removed instance %s from Redis", instanceId));
    }

    /**
     * Check if an instance exists in Redis
     * @param instanceId the ID to check
     * @return true if the instance exists
     */
    public boolean instanceExists(UUID instanceId) {
        return instancesMap.containsKey(instanceId);
    }

    /**
     * Check if this server has an active floor instance
     */
    public boolean hasActiveInstance() {
        return currentInstance.get() != null;
    }

    /**
     * Get the current floor instance state
     */
    public FloorInstanceState getInstanceState() {
        FloorInstance instance = currentInstance.get();
        if (instance == null) {
            return FloorInstanceState.NONE;
        }
        return instance.isReady() ? FloorInstanceState.READY : FloorInstanceState.PREPARING;
    }

    public enum FloorInstanceState {
        NONE,
        PREPARING,
        READY
    }

}
