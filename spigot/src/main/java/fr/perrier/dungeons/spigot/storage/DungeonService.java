package fr.perrier.dungeons.spigot.storage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.perrier.dungeons.common.model.dungeon.FloorData;
import fr.perrier.dungeons.common.model.dungeon.FloorMetadata;
import fr.perrier.dungeons.common.model.dungeon.config.FloorInstanceData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.database.DatabaseManager;
import fr.perrier.dungeons.spigot.model.Dungeon;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import fr.perrier.dungeons.spigot.messaging.redis.RedisMessage;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.utils.GsonProvider;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.RMap;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public class DungeonService {
    @Getter
    private final RedissonClient redissonClient;

    // Redis Maps and Topics
    private static final String DUNGEON_MAP = Objects.requireNonNull(Main.getInstance().getConfig().getString("RedisConfiguration.topic")) + ":dungeons";
    private static final String FLOOR_MAP = Objects.requireNonNull(Main.getInstance().getConfig().getString("RedisConfiguration.topic")) + ":floors";
    private static final String FLOOR_METADATA_MAP = Objects.requireNonNull(Main.getInstance().getConfig().getString("RedisConfiguration.topic")) + ":floor_metadata";
    private static final String INSTANCE_MAP = Objects.requireNonNull(Main.getInstance().getConfig().getString("RedisConfiguration.topic")) + ":instances";
    private static final String SYNC_CHANNEL = Objects.requireNonNull(Main.getInstance().getConfig().getString("RedisConfiguration.topic")) + ":sync";

    // Local references to current floor and instance
    private final AtomicReference<FloorData> currentFloor = new AtomicReference<>();
    private final AtomicReference<UUID> currentInstanceId = new AtomicReference<>();
    // Local cache of the current FloorInstanceData — never requires a Redis call on the server thread
    private final AtomicReference<FloorInstanceData> currentInstanceData = new AtomicReference<>();

    // Redis Maps
    @Getter
    private RMap<String, Dungeon> dungeonsMap;
    @Getter
    private RMap<String, FloorData> floorsMap;
    @Getter
    private RMap<String, FloorMetadata> floorMetadataMap;
    @Getter
    private RMap<UUID, FloorInstanceData> instancesMap;

    @Getter
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
        this.floorMetadataMap = redissonClient.getMap(FLOOR_METADATA_MAP);
        this.instancesMap = redissonClient.getMap(INSTANCE_MAP);
        this.syncTopic = redissonClient.getTopic(SYNC_CHANNEL);

        subscribeSyncChannel();
    }

    /**
     * Attaches listeners to the sync topic so this server reacts to both:
     * <ul>
     *   <li>{@link RedisMessage} payloads produced by peer Spigot instances</li>
     *   <li>Raw JSON payloads produced by the Velocity/Bungee dashboard</li>
     * </ul>
     */
    private void subscribeSyncChannel() {
        syncTopic.addListener(Object.class, (channel, msg) -> {
            if (msg instanceof RedisMessage<?> redisMessage) {
                handlePeerSyncMessage(redisMessage);
            } else if (msg instanceof String jsonString) {
                handleSyncMessage(jsonString);
            }
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
        FloorInstanceData instanceData = instancesMap.get(instanceId);
        if (instanceData == null) {
            Main.getLoggerUtil().severe(String.format(
                    "Could not find instance %s in Redis",
                    instanceId
            ));
            return;
        }

        // Set current instance ID only
        currentInstanceId.set(instanceId);
        // Cache instance data locally so it never needs a blocking Redis call
        currentInstanceData.set(instanceData);

        // Get and set floor
        FloorData floorData = floorsMap.get(floorId);
        if (floorData == null) {
            Main.getLoggerUtil().severe(String.format(
                    "Could not find floor %s in Redis",
                    floorId
            ));
            return;
        }

        Floor floor = new Floor(floorData);

        currentFloor.set(floor);

        Main.getLoggerUtil().info(String.format(
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
        if (dungeon == null) return;
        // Floor.triggers carries Spigot-only TriggerData subclasses that blow up
        // Kryo on the proxy or on a node whose classpath drifted (KryoBufferUnderflow
        // on the deserialize path). Stash → clear → fastPut → restore so the live
        // in-memory Dungeon keeps its triggers while the Redis copy is sanitised.
        java.util.Map<Floor, java.util.List<fr.perrier.dungeons.common.workflow.trigger.TriggerData>> stash
                = new java.util.IdentityHashMap<>();
        if (dungeon.getFloors() != null) {
            for (Floor f : dungeon.getFloors()) {
                if (f != null && f.getTriggers() != null) {
                    stash.put(f, f.getTriggers());
                    f.setTriggers(null);
                }
            }
        }
        try {
            dungeonsMap.fastPut(dungeon.getId(), dungeon);
        } finally {
            for (java.util.Map.Entry<Floor, java.util.List<fr.perrier.dungeons.common.workflow.trigger.TriggerData>> e
                    : stash.entrySet()) {
                e.getKey().setTriggers(e.getValue());
            }
        }

        if (Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info(
                    String.format("Synced dungeon %s to Redis", dungeon.getId())
            );
        }
    }

    /**
     * Synchronizes the given floor to Redis and notifies other servers.
     * This method will update the local reference, update the Redis floor map,
     * and notify other servers of the update.
     *
     * @param floorData the floor to synchronize.
     */
    public void syncFloor(FloorData floorData) {
        // Keep the local cache in sync first so later getCurrentFloor() calls return
        // the freshly-saved triggers. Without this, saves persist to DB but the in-memory
        // currentFloor still points at the pre-save FloorData — the trigger editor's
        // subsequent /api/triggers read then returns stale triggers (default values
        // from the pre-edit state).
        FloorData currentLocal = currentFloor.get();
        if (currentLocal != null && currentLocal.getId().equals(floorData.getId())) {
            currentFloor.set(floorData);
        }

        // The shared Redis map is consumed by the proxy (Velocity), which does not have
        // Spigot trigger classes on its classpath — Kryo throws ClassNotFoundException on
        // types like EntityDeathTrigger. Strip triggers before anything leaves this server;
        // triggers are loaded separately from the floor_triggers table wherever they are needed.
        FloorData sharable = stripTriggersForSharedStorage(floorData);

        RedisMessage<FloorData> message = RedisMessage.create(
                SYNC_CHANNEL,
                Bukkit.getServer().getName(),
                RedisMessage.MessageType.FLOOR_UPDATE,
                sharable
        );

        // Update Redis
        floorsMap.fastPut(sharable.getId(), sharable);

        // Also update metadata for dashboard
        FloorMetadata metadata = FloorMetadata.from(sharable);
        floorMetadataMap.fastPut(sharable.getId(), metadata);

        // Notify other servers
        syncTopic.publish(message);

        Main.getLoggerUtil().info(
                String.format("Synced floor %s to Redis", floorData.getId())
        );
    }

    /**
     * Synchronizes the given instance to Redis and notifies other servers.
     * This method will update the local reference, update the Redis instance map,
     * and notify other servers of the update.
     *
     * @param instanceData the floor instance to synchronize.
     */
    public void syncInstance(FloorInstanceData instanceData) {
        // Keep local cache up-to-date first (no Redis required for reads after this)
        if (currentInstanceId.get() != null &&
                currentInstanceId.get().equals(instanceData.getInstanceId())) {
            currentInstanceData.set(instanceData);
        }

        RedisMessage<FloorInstanceData> message = RedisMessage.create(
                SYNC_CHANNEL,
                Bukkit.getServer().getIp() + ":" + Bukkit.getServer().getPort(),
                RedisMessage.MessageType.INSTANCE_UPDATE,
                instanceData
        );

        // Update Redis
        instancesMap.fastPut(instanceData.getInstanceId(), instanceData);

        // Notify other servers
        syncTopic.publish(message);

        Main.getLoggerUtil().info(
                String.format("Synced instance %s to Redis", instanceData.getInstanceId())
        );
    }

    /**
     * Handle a peer-to-peer sync message produced by another Spigot server.
     */
    private void handlePeerSyncMessage(RedisMessage<?> message) {
        String serverIdentity = Bukkit.getServer().getIp() + ":" + Bukkit.getServer().getPort();
        if (message.getSender().equals(serverIdentity)) return; // ignore own messages

        switch (message.getType()) {
            case FLOOR_UPDATE -> {
                if (message.getData() instanceof FloorData floorData) {
                    handleFloorUpdate(floorData.getId(), floorData);
                }
            }
            case INSTANCE_UPDATE -> {
                FloorInstanceData instanceData = (FloorInstanceData) message.getData();
                if (currentInstanceId.get() != null &&
                        currentInstanceId.get().equals(instanceData.getInstanceId())) {
                    currentInstanceData.set(instanceData);
                    Main.getLoggerUtil().info(
                            String.format("Instance %s updated in local cache from Redis",
                                    instanceData.getInstanceId())
                    );
                }
            }
            case INSTANCE_REMOVE -> {
                FloorInstanceData instanceData = (FloorInstanceData) message.getData();
                if (currentInstanceId.get() != null &&
                        currentInstanceId.get().equals(instanceData.getInstanceId())) {
                    currentInstanceId.set(null);
                    currentInstanceData.set(null);
                    currentFloor.set(null);
                    Main.getLoggerUtil().info(String.format("Removed local instanceData: %s", instanceData.getInstanceId()));
                }
            }
            default -> Main.getLoggerUtil().warning(String.format("Received unknown RedisMessage type: %s", message.getType()));
        }
    }

    /**
     * Parses and dispatches a JSON sync message emitted by the dashboard.
     * Structure: {@code {type, id, name, description, data}} where {@code data}
     * is a stringified {@link FloorData} JSON for {@code FLOOR_UPDATE}.
     */
    private void handleSyncMessage(String jsonString) {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
            String type = jsonObject.has("type") ? jsonObject.get("type").getAsString() : null;
            String id = jsonObject.has("id") ? jsonObject.get("id").getAsString() : null;
            if (type == null || id == null) return;

            switch (type) {
                case "FLOOR_UPDATE" -> {
                    String dungeonId = jsonObject.has("description") ? jsonObject.get("description").getAsString() : null;
                    String name = jsonObject.has("name") ? jsonObject.get("name").getAsString() : null;
                    String data = jsonObject.has("data") ? jsonObject.get("data").getAsString() : null;
                    handleFloorUpdate(id, dungeonId, name, data);
                }
                case "DUNGEON_UPDATE" -> {
                    String name = jsonObject.has("name") ? jsonObject.get("name").getAsString() : null;
                    String description = jsonObject.has("description") ? jsonObject.get("description").getAsString() : null;
                    String data = jsonObject.has("data") ? jsonObject.get("data").getAsString() : null;
                    persistDungeonUpdate(id, name, description, data);
                }
                case "DUNGEON_DELETE" -> persistDelete(type, id);
                case "FLOOR_DELETE" -> persistDelete(type, id);
                case "INSTANCE_UPDATE", "INSTANCE_REMOVE" -> {
                    // Instance events are carried as RedisMessage<FloorInstanceData> on another path.
                }
                default -> Main.getLoggerUtil().warning("Unknown dashboard sync message type: " + type);
            }
        } catch (Exception e) {
            Main.getLoggerUtil().warning("Error handling dashboard sync message: " + e.getMessage());
        }
    }

    /**
     * Persists a {@link FloorData} update coming from the dashboard. Flow:
     * <ol>
     *   <li>Parse + verify the incoming JSON checksum.</li>
     *   <li>Compare against the version currently on disk — reject stale writes.</li>
     *   <li>DATABASE FIRST: save to DB (retry + transaction). On failure, abort
     *       AND roll back Redis so the cluster never keeps a write the DB rejected.</li>
     *   <li>Update local Redis floorsMap + metadata.</li>
     *   <li>Apply to {@link #currentFloor} if it matches this server.</li>
     * </ol>
     */
    private void handleFloorUpdate(String floorId, String dungeonId, String name, String dataJson) {
        if (Main.getInstance() == null) {
            Main.getLoggerUtil().severe("[handleFloorUpdate] Main instance unavailable — skipping " + floorId);
            return;
        }
        DatabaseManager db = Main.getInstance().getDatabaseManager();
        if (db == null) {
            Main.getLoggerUtil().warning("[handleFloorUpdate] Database manager not available for " + floorId);
            return;
        }

        FloorData incoming;
        try {
            incoming = GsonProvider.GSON.fromJson(dataJson, FloorData.class);
        } catch (Exception e) {
            Main.getLoggerUtil().severe("[handleFloorUpdate] Cannot parse FloorData for " + floorId + ": " + e.getMessage());
            return;
        }
        if (incoming == null) {
            Main.getLoggerUtil().severe("[handleFloorUpdate] Empty FloorData for " + floorId);
            return;
        }
        if (incoming.getChecksum() == null || !incoming.verifyChecksum()) {
            Main.getLoggerUtil().severe("[handleFloorUpdate] Incoming checksum invalid for " + floorId
                    + " — refusing to persist");
            return;
        }

        String resolvedDungeonId = incoming.getDungeonId() != null ? incoming.getDungeonId() : dungeonId;
        if (resolvedDungeonId == null || resolvedDungeonId.isEmpty()) {
            Main.getLoggerUtil().severe("[handleFloorUpdate] No dungeonId for " + floorId);
            return;
        }

        db.saveFloor(floorId, resolvedDungeonId, incoming)
                .whenComplete((unused, error) -> {
                    if (error != null) {
                        Main.getLoggerUtil().severe("[handleFloorUpdate] DB save FAILED for " + floorId
                                + " after retries: " + error.getMessage());
                        // Roll back the optimistic Redis write from the dashboard so the cluster
                        // does not hold state that the DB explicitly rejected.
                        try {
                            floorsMap.remove(floorId);
                        } catch (Exception ignored) { }
                        return;
                    }
                    // DB save succeeded — now safe to refresh Redis metadata + local floor reference.
                    FloorData sharable = stripTriggersForSharedStorage(incoming);
                    try {
                        floorsMap.fastPut(floorId, sharable);
                        floorMetadataMap.fastPut(floorId, FloorMetadata.from(sharable));
                    } catch (Exception e) {
                        Main.getLoggerUtil().warning("[handleFloorUpdate] Redis update failed (non-critical) for "
                                + floorId + ": " + e.getMessage());
                    }
                    handleFloorUpdate(floorId, incoming);
                });
    }

    /**
     * Applies a {@link FloorData} to {@link #currentFloor} when it matches this
     * server's active floor, only if the incoming version is newer than the local copy.
     * Falls back to DB if Redis held stale / corrupted bytes.
     */
    private void handleFloorUpdate(String floorId, FloorData incoming) {
        FloorData current = currentFloor.get();
        if (current == null || !current.getId().equals(floorId)) return;

        if (incoming.getVersion() <= current.getVersion()) {
            Main.getLoggerUtil().info(String.format(
                    "[handleFloorUpdate] Ignoring stale update for %s (incoming v%d <= current v%d)",
                    floorId, incoming.getVersion(), current.getVersion()));
            return;
        }

        FloorData authoritative = incoming;
        if (!incoming.verifyChecksum()) {
            Main.getLoggerUtil().severe("[handleFloorUpdate] Incoming checksum invalid for " + floorId
                    + " — falling back to DB");
            authoritative = loadFloorFromDatabase(floorId);
            if (authoritative == null) return;
        }

        currentFloor.set(authoritative);
        if (Main.getInstance() != null && Main.getInstance().getTriggersRegistry() != null) {
            Main.getInstance().getTriggersRegistry().refreshTriggerCache();
        }
        Main.getLoggerUtil().info(String.format(
                "Updated local floorData: %s → v%d", floorId, authoritative.getVersion()));
    }

    /**
     * Synchronous DB fallback used when Redis / message payload is unusable.
     * Blocks the current thread on the DB future because the caller is already
     * inside an async listener — we do NOT want to post to the main thread here.
     */
    private FloorData loadFloorFromDatabase(String floorId) {
        DatabaseManager db = Main.getInstance() != null ? Main.getInstance().getDatabaseManager() : null;
        if (db == null) return null;
        try {
            return db.getFloor(floorId).get();
        } catch (Exception e) {
            Main.getLoggerUtil().severe("[loadFloorFromDatabase] Failed for " + floorId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns a copy of {@code src} with {@code triggers} nulled out.
     * Triggers contain Spigot-only classes and live in their own table — they must
     * not end up in the shared Redis map consumed by the proxy. The checksum is
     * unaffected because {@link FloorData#calculateChecksum()} already excludes triggers.
     */
    private FloorData stripTriggersForSharedStorage(FloorData src) {
        if (src == null || src.getTriggers() == null) return src;
        FloorData copy = new FloorData(
                src.getId(), src.getName(), src.getDescription(),
                src.getWorldConfig(), src.getRequirements(), src.getRules(),
                src.getSteps(), null);
        copy.setDungeonId(src.getDungeonId());
        copy.setFloorType(src.getFloorType());
        copy.setLabyrinthFloorConfig(src.getLabyrinthFloorConfig());
        copy.setVersion(src.getVersion());
        copy.setSchemaVersion(src.getSchemaVersion());
        copy.setUpdatedAt(src.getUpdatedAt());
        copy.setUpdatedBy(src.getUpdatedBy());
        copy.setChecksum(src.getChecksum());
        return copy;
    }

    private void persistDungeonUpdate(String id, String name, String description, String data) {
        DatabaseManager db = Main.getInstance() != null ? Main.getInstance().getDatabaseManager() : null;
        if (db == null) return;
        db.saveDungeon(id, name, description, data).exceptionally(e -> {
            Main.getLoggerUtil().warning("Error saving dungeon " + id + ": " + e.getMessage());
            return null;
        });
    }

    private void persistDelete(String type, String id) {
        DatabaseManager db = Main.getInstance() != null ? Main.getInstance().getDatabaseManager() : null;
        if (db == null) return;
        if ("DUNGEON_DELETE".equals(type)) {
            db.deleteDungeon(id).exceptionally(e -> {
                Main.getLoggerUtil().warning("Error deleting dungeon " + id + ": " + e.getMessage());
                return null;
            });
        } else if ("FLOOR_DELETE".equals(type)) {
            db.deleteFloor(id).exceptionally(e -> {
                Main.getLoggerUtil().warning("Error deleting floor " + id + ": " + e.getMessage());
                return null;
            });
            try { floorsMap.remove(id); } catch (Exception ignored) { }
            try { floorMetadataMap.remove(id); } catch (Exception ignored) { }
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
        FloorData floorData =  floorsMap.get(id);

        if(floorData == null) {
            Main.getLoggerUtil().warning(String.format("Floor %s not found in Redis", id));
            return null;
        }

        return new Floor(floorData);
    }

    /**
     * Retrieve a floor instance by its unique ID.
     *
     * @param id the unique ID of the instance to retrieve
     * @return the instance with the given ID, or null if not found
     */
    public FloorInstance getInstance(UUID id) {
        FloorInstanceData instanceData = instancesMap.get(id);

        if(instanceData == null) {
            Main.getLoggerUtil().warning(String.format("Instance %s not found in Redis", id));
            return null;
        }

        return new FloorInstance(instanceData);
    }

    /**
     * Clear local references when server is shutting down or instance is complete
     */
    public void clearLocal() {
        currentFloor.set(null);
        currentInstanceId.set(null);
        currentInstanceData.set(null);
        Main.getLoggerUtil().info("Cleared local floor and instance references");
    }

    /**
     * Remove a dungeon from Redis
     * @param id the ID of the dungeon to remove
     */
    public void removeDungeon(String id) {
        // Remove from Redis
        dungeonsMap.remove(id);
        if (Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info(String.format("Removed dungeon %s from Redis", id));
        }
    }

    /**
     * Remove an instance from Redis and notify other servers
     * @param instanceId the ID of the instance to remove
     */
    public void removeInstance(UUID instanceId) {
        FloorInstanceData instanceData = instancesMap.get(instanceId);
        if (instanceData == null) {
            Main.getLoggerUtil().warning(String.format("Tried to remove non-existent instance: %s", instanceId));
            return;
        }

        FloorInstance instance = new FloorInstance(instanceData);

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
        UUID localInstanceId = currentInstanceId.get();
        if (localInstanceId != null && localInstanceId.equals(instanceId)) {
            currentInstanceId.set(null);
            currentFloor.set(null);
        }

        // Notify other servers
        syncTopic.publish(message);

        Main.getLoggerUtil().info(String.format("Removed instance %s from Redis", instanceId));
    }

    /**
     * Removes a single player from this server's active instance state and re-syncs.
     * <p>
     * Called when a player leaves an instance (quit / kick / return) so their per-player
     * entries in {@code players}, {@code playerStats}, {@code playerCurrentLives} and
     * {@code originInstances} do not linger in Redis after they are gone. Safe to call on
     * lobby servers and when no instance is active — it is a no-op in those cases, and
     * idempotent if the player was already removed.
     *
     * @param playerId the UUID of the player to drop from the instance state
     */
    public void removePlayerFromInstanceState(UUID playerId) {
        if (playerId == null) return;

        FloorInstanceData data = currentInstanceData.get();
        if (data == null) return; // lobby, or instance already torn down

        boolean changed = data.getPlayers().remove(playerId);
        if (data.getPlayerStats().remove(playerId) != null) changed = true;
        if (data.getPlayerCurrentLives().remove(playerId) != null) changed = true;
        if (data.getOriginInstances().remove(playerId) != null) changed = true;

        if (changed) {
            syncInstance(data);
            if (Main.getLoggerUtil().isDebugEnabled()) {
                Main.getLoggerUtil().info(String.format(
                        "Removed player %s from instance %s state", playerId, data.getInstanceId()));
            }
        }
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
        return currentInstanceId.get() != null;
    }

    /**
     * Get the current floor instance state — reads from local cache, never blocks on Redis.
     */
    public FloorInstanceState getInstanceState() {
        FloorInstanceData instance = currentInstanceData.get();
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

    /**
     * Get the current floor instance — reads from local cache, never blocks on Redis.
     * @return the current FloorInstance
     */
    public FloorInstance getCurrentInstance() {
        FloorInstanceData instanceData = currentInstanceData.get();
        if (instanceData == null) {
            throw new IllegalStateException("No current instance available");
        }
        return new FloorInstance(instanceData);
    }

    /**
     * Checks whether the given player UUID is already part of any instance (local cached or in Redis).
     * Useful to prevent creating duplicate instances for the same players.
     *
     * @param playerId the UUID of the player to check
     * @return true if the player is present in any known instance
     */
    public boolean isPlayerInAnyInstance(UUID playerId) {
        if (playerId == null) return false;

        // Check local cached current instance first
        FloorInstanceData local = currentInstanceData.get();
        if (local != null && local.getPlayers() != null && local.getPlayers().contains(playerId)) {
            return true;
        }

        // Check Redis-backed instances map (may be remote)
        if (instancesMap != null) {
            try {
                for (FloorInstanceData inst : instancesMap.values()) {
                    if (inst != null && inst.getPlayers() != null && inst.getPlayers().contains(playerId)) {
                        return true;
                    }
                }
            } catch (Exception e) {
                Main.getLoggerUtil().warning("Failed to check instancesMap for player membership: " + e.getMessage());
                // On failure to check remote map, conservatively return false so caller can proceed or implement additional locking
                return false;
            }
        }

        return false;
    }

    /**
     * Get the current floor
     * @return the current Floor
     */
    public Floor getCurrentFloor() {
        FloorData floorData = currentFloor.get();
        if (floorData == null) {
            throw new IllegalStateException("No current floor available");
        }
        return new Floor(floorData);
    }

}
