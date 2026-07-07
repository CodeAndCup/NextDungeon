package fr.perrier.dungeons.spigot;

import com.alessiodp.parties.api.Parties;
import com.alessiodp.parties.api.interfaces.PartiesAPI;
import com.github.juliarn.npclib.api.NpcActionController;
import com.github.juliarn.npclib.api.Platform;
import com.github.juliarn.npclib.bukkit.BukkitPlatform;
import com.github.juliarn.npclib.bukkit.BukkitWorldAccessor;
import com.github.juliarn.npclib.bukkit.protocol.BukkitProtocolAdapter;
import fr.perrier.cupcodeapi.CupCodeAPI;
import fr.perrier.cupcodeapi.commands.CommandHandler;
import fr.perrier.cupcodeapi.menuapi.MenuAPI;
import fr.perrier.dungeons.common.model.dungeon.FloorData;
import fr.perrier.dungeons.spigot.commands.AdminCommands;
import fr.perrier.dungeons.spigot.commands.ConsoleCommands;
import fr.perrier.dungeons.spigot.commands.DebugCommands;
import fr.perrier.dungeons.spigot.commands.PartyCommands;
import fr.perrier.dungeons.spigot.commands.PlayerCommands;
import fr.perrier.dungeons.spigot.commands.params.DungeonParameterType;
import fr.perrier.dungeons.spigot.commands.params.FloorParameterType;
import fr.perrier.dungeons.spigot.commands.params.ModuleParameterType;
import fr.perrier.dungeons.spigot.configuration.RedisConfigLoader;
import fr.perrier.dungeons.spigot.database.DatabaseFactory;
import fr.perrier.dungeons.spigot.database.DatabaseManager;
import fr.perrier.dungeons.spigot.model.Dungeon;
import fr.perrier.dungeons.spigot.instance.InstanceInfo;
import fr.perrier.dungeons.spigot.listener.dungeons.InstanceJoinListener;
import fr.perrier.dungeons.spigot.listener.dungeons.InstanceMobKillListener;
import fr.perrier.dungeons.spigot.listener.dungeons.InstancePlayerDeathListener;
import fr.perrier.dungeons.spigot.listener.dungeons.ReviveItemListener;
import fr.perrier.dungeons.spigot.listener.editor.EditorJoinListener;
import fr.perrier.dungeons.spigot.listener.global.GlobalJoinListener;
import fr.perrier.dungeons.spigot.listener.global.GlobalLeaveListener;
import fr.perrier.dungeons.spigot.listener.global.GlobalPartyListener;
import fr.perrier.dungeons.spigot.manager.GhostFactory;
import fr.perrier.dungeons.spigot.messaging.packets.CancelInstancePacket;
import fr.perrier.dungeons.spigot.messaging.packets.CrossServerSendToInstancePacket;
import fr.perrier.dungeons.spigot.messaging.packets.ConsumeRequirementsRequestPacket;
import fr.perrier.dungeons.spigot.messaging.packets.DungeonPartyJoinRequestPacket;
import fr.perrier.dungeons.spigot.messaging.packets.ValidateRequirementsRequestPacket;
import fr.perrier.dungeons.spigot.messaging.packets.ValidateRequirementsResponsePacket;
import fr.perrier.dungeons.spigot.messaging.subscribers.CancelInstanceSubscriber;
import fr.perrier.dungeons.spigot.messaging.subscribers.CrossServerSendToInstanceSubscriber;
import fr.perrier.dungeons.spigot.messaging.subscribers.ConsumeRequirementsRequestSubscriber;
import fr.perrier.dungeons.spigot.messaging.subscribers.DungeonPartyJoinRequestSubscriber;
import fr.perrier.dungeons.spigot.messaging.subscribers.ValidateRequirementsRequestSubscriber;
import fr.perrier.dungeons.spigot.messaging.subscribers.ValidateRequirementsResponseSubscriber;
import fr.perrier.dungeons.spigot.parties.CrossServerValidationService;
import fr.perrier.dungeons.spigot.parties.impl.DungeonPartyImpl;
import fr.perrier.dungeons.spigot.parties.impl.DungeonPartyRegistry;
import fr.perrier.dungeons.spigot.utils.LoggerUtil;
import fr.perrier.dungeons.spigot.workflow.registry.TriggersRegistry;
import fr.perrier.dungeons.spigot.workflow.registry.VariableRegistry;
import fr.perrier.dungeons.common.messaging.Pidgin;
import fr.perrier.dungeons.spigot.messaging.ServerNameService;
import fr.perrier.dungeons.spigot.model.ProfileData;
import fr.perrier.dungeons.spigot.monitoring.CacheHealthMonitor;
import fr.perrier.dungeons.spigot.messaging.packets.PlayerSwitchServerPacket;
import fr.perrier.dungeons.spigot.messaging.packets.webeditor.WebEditorRequestPacket;
import fr.perrier.dungeons.spigot.messaging.packets.webeditor.WebEditorResponsePacket;
import fr.perrier.dungeons.spigot.messaging.subscribers.PlayerSwitchServerSubscriber;
import fr.perrier.dungeons.spigot.messaging.subscribers.WebEditorRequestSubscriber;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import fr.perrier.dungeons.spigot.storage.ProfileService;
import fr.perrier.dungeons.spigot.storage.LeaderboardService;
import fr.perrier.dungeons.spigot.storage.DungeonService;
import fr.perrier.dungeons.spigot.queue.DungeonQueueService;
import fr.perrier.dungeons.spigot.queue.QueueManager;
import fr.perrier.dungeons.spigot.utils.ServerUtil;
import fr.perrier.dungeons.spigot.module.ModuleLoader;
import fr.perrier.dungeons.spigot.webeditor.DungeonWebEditorManager;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.units.qual.C;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import fr.perrier.dungeons.spigot.instance.InstanceProvider;
import fr.perrier.dungeons.spigot.instance.InstanceProviderFactory;
import fr.perrier.dungeons.spigot.parties.PartyService;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter
public final class Main extends JavaPlugin {

    @Getter
    private static Main instance;
    @Getter
    private static final String prefix = "<gradient:#8B0000:bold>NextDungeon</gradient:#D10000> &8» &r";

    @Getter@Setter
    private static LoggerUtil loggerUtil;

    // Plugin API instance
    private PartiesAPI partiesAPI;
    private CommandHandler commandHandler;
    private MenuAPI menuAPI;
    private GhostFactory ghostFactory;
    private Platform<World, Player, ItemStack, Plugin> npcLibPlatform;

    // Plugin packets pub/sub and sync storage
    private Pidgin messaging;
    private DungeonService dungeonService;
    private ProfileService profileService;
    private LeaderboardService leaderboardService;
    private DatabaseManager databaseManager;
    private ServerNameService serverNameService;

    // Web editor manager
    private DungeonWebEditorManager webEditorManager;

    // Dynamic module loader
    private ModuleLoader moduleLoader;


    // Global trigger manager
    private TriggersRegistry triggersRegistry;
    private VariableRegistry variableRegistry;

    // Instance provider (CloudNet, ASP, ou Vanilla)
    private InstanceProvider instanceProvider;

    // Party service
    private PartyService partyService;

    // Cluster-wide dungeon party registry (Redis-backed). Only initialized on lobby servers.
    private DungeonPartyRegistry dungeonPartyRegistry;

    // Cross-server floor requirements validation (request/response over Pidgin)
    private CrossServerValidationService crossServerValidationService;

    // Queue management
    private DungeonQueueService dungeonQueueService;
    private QueueManager queueManager;

    // Cache health monitoring
    private CacheHealthMonitor cacheHealthMonitor;

    @Override
    public void onEnable() {
        getLogger().info("Starting NextDungeon plugin...");
        long startTime = System.currentTimeMillis();

        instance = this;

        // Starting logger
        loggerUtil = LoggerUtil.getInstance();
        loggerUtil.setDebugEnabled(getConfig().getBoolean("DebugMode.activated"));
        loggerUtil.setLogBroadcastType(LoggerUtil.LogBroadcastType.valueOf(Objects.requireNonNull(getConfig().getString("DebugMode.logType")).toUpperCase()));

        // Save default config
        saveDefaultConfig();
        if(getConfig().getString("config-version") == null) {
            getLogger().severe("Invalid configuration file (missing config-version), please check your config.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize Redis Configuration
        Config config = new Config();
        config.useSingleServer().setAddress("redis://"
                        + Objects.requireNonNull(Main.getInstance().getConfig().getString("RedisConfiguration.host"))
                        + ":"
                        + Main.getInstance().getConfig().getInt("RedisConfiguration.port"))
                .setUsername(Objects.requireNonNull(Main.getInstance().getConfig().getString("RedisConfiguration.username")))
                .setPassword(Objects.requireNonNull(Main.getInstance().getConfig().getString("RedisConfiguration.password")))
                .setDatabase(Main.getInstance().getConfig().getInt("RedisConfiguration.database"));

        // Create Redis client. Wrap the rest of onEnable() in a try/finally
        // so that if any subsequent init step fails (and we disable the
        // plugin via early return / throws), we still shut the Redisson
        // client down — otherwise its Netty threads keep the JVM busy and
        // leak connections to Redis.
        RedissonClient redissonClient = Redisson.create(config);
        boolean redissonHandedOff = false;
        try {

        try {
            // Initialize Redis storage service
            dungeonService = new DungeonService(redissonClient);
            dungeonService.initialize();
            getLogger().info("Redis storage service initialized successfully");
        }catch (Exception e) {
            getLogger().severe("Failed to initialize Redis: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize Instance Provider
        try {
            instanceProvider = InstanceProviderFactory.createProvider();
            instanceProvider.initialize().thenAccept(success -> {
                if (!success) {
                    getLogger().severe("Failed to initialize instance provider");
                    getServer().getPluginManager().disablePlugin(this);
                }
            });
        } catch (Exception e) {
            getLogger().severe("Failed to initialize instance provider: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {

            // Initialize Profile service
            profileService = new ProfileService(redissonClient);
            profileService.initialize();
            getLogger().info("Profile service initialized successfully");

            // Initialize Leaderboard service (per-floor Redis sorted sets)
            leaderboardService = new LeaderboardService(redissonClient);
            getLogger().info("Leaderboard service initialized successfully");

            // Initialize Dungeon Queue Service
            dungeonQueueService = new DungeonQueueService(redissonClient);
            dungeonQueueService.initialize();
            getLogger().info("Dungeon queue service initialized successfully");

            // Initialize Queue Manager (only on lobby servers)
            if (!ServerUtil.isInstanceServer()) {
                queueManager = new QueueManager(dungeonQueueService);
                queueManager.initialize();
                getLogger().info("Queue manager initialized successfully");
            }

        } catch (Exception e) {
            getLogger().severe("Failed to initialize Redis services: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        databaseManager = DatabaseFactory.createDatabase();

        // Load dynamic modules from /modules/ directory
        moduleLoader = new ModuleLoader(new java.io.File(getDataFolder(), "modules"));
        moduleLoader.loadAll();

        // Enabling other plugins API
        CupCodeAPI.enable(this);
        menuAPI = new MenuAPI(this);
        partiesAPI = Parties.getApi();
        ghostFactory = new GhostFactory();

        // Initialize Party Service (uses config for provider selection)
        partyService = new PartyService();

        npcLibPlatform = BukkitPlatform.bukkitNpcPlatformBuilder()
                .extension(this)
                .debug(true)
                .actionController(builder -> builder
                        .flag(NpcActionController.SPAWN_DISTANCE, 60)
                        .flag(NpcActionController.IMITATE_DISTANCE, 30)
                )
                .worldAccessor(BukkitWorldAccessor.nameBasedAccessor())
                .packetFactory(BukkitProtocolAdapter.packetEvents())
                .build();

        // Initialize server based on type
        if (ServerUtil.isInstanceServer()) {
            if(ServerUtil.isInEditMode()) {
                getLogger().info("Server is in EDIT mode");
                initializeEditorServer();
            }
            initializeInstanceServer();
        } else {
            initializeLobbyServer();
        }

        // Enabling messaging system
        this.messaging = new Pidgin(Objects.requireNonNull(getConfig().getString("RedisConfiguration.topic")),config);
        this.messaging.registerAdapter(PlayerSwitchServerPacket.class, new PlayerSwitchServerSubscriber());
        this.messaging.registerAdapter(WebEditorRequestPacket.class, new WebEditorRequestSubscriber());
        this.messaging.registerAdapter(WebEditorResponsePacket.class, null);
        this.messaging.registerAdapter(CancelInstancePacket.class, new CancelInstanceSubscriber());
        this.messaging.registerAdapter(DungeonPartyJoinRequestPacket.class, new DungeonPartyJoinRequestSubscriber());
        this.messaging.registerAdapter(ValidateRequirementsRequestPacket.class, new ValidateRequirementsRequestSubscriber());
        this.messaging.registerAdapter(ValidateRequirementsResponsePacket.class, new ValidateRequirementsResponseSubscriber());
        this.messaging.registerAdapter(ConsumeRequirementsRequestPacket.class, new ConsumeRequirementsRequestSubscriber());
        this.messaging.registerAdapter(CrossServerSendToInstancePacket.class, new CrossServerSendToInstanceSubscriber());

        // Cross-server validation service — shared between lobbies and instance servers; the
        // latter forward responses back to the requesting lobby.
        this.crossServerValidationService = new CrossServerValidationService();

        // Cluster-wide dungeon party registry — lobby servers only (instance servers never own parties).
        if (!ServerUtil.isInstanceServer()) {
            try {
                UUID currentServiceId = instanceProvider.getCurrentServiceUniqueId();
                if (currentServiceId == null) {
                    getLogger().warning("DungeonPartyRegistry: could not resolve current service UUID — cross-server party finder will be unavailable");
                } else {
                    dungeonPartyRegistry = new DungeonPartyRegistry(redissonClient, currentServiceId);
                    dungeonPartyRegistry.initialize();
                }
            } catch (Exception e) {
                getLogger().severe("Failed to initialize DungeonPartyRegistry: " + e.getMessage());
            }
        }

        // Initialize server name service
        this.serverNameService = new ServerNameService();
        this.serverNameService.initialize();

        // Loading commands
        this.commandHandler = new CommandHandler(this);
        loadCommands();

        // Loading listeners
        loadGlobalListeners();

        // Only on instance servers
        if(ServerUtil.isInstanceServer()) {
            // Initialize trigger system
            triggersRegistry = new TriggersRegistry();
            triggersRegistry.initialize();
            triggersRegistry.refreshTriggerCache();

            // Initialize variable manager
            variableRegistry = new VariableRegistry();

            if(ServerUtil.isInEditMode()) {
                // Initialize web editor manager
                webEditorManager = new DungeonWebEditorManager();
            }
        }

        // Start cache health monitoring once all services are wired up
        try {
            cacheHealthMonitor = new CacheHealthMonitor(this);
            cacheHealthMonitor.startMonitoring();
        } catch (Exception e) {
            getLogger().warning("Cache health monitor failed to start: " + e.getMessage());
        }

        getLogger().info("NextDungeon " + this.getDescription().getVersion()  + " started in " + (System.currentTimeMillis() - startTime) + " ms");
        redissonHandedOff = true;
        } finally {
            if (!redissonHandedOff) {
                try { redissonClient.shutdown(); } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public void onDisable() {
        // Stop health monitor FIRST so it does not run mid-shutdown.
        if (cacheHealthMonitor != null) {
            try { cacheHealthMonitor.stop(); } catch (Exception e) {
                getLogger().warning("Health monitor stop error: " + e.getMessage());
            }
        }

        try {
            saveAllPendingData();
        } catch (Exception e) {
            getLogger().severe("saveAllPendingData error: " + e.getMessage());
        }

        try {
            tagRedisCache();
        } catch (Exception e) {
            getLogger().severe("tagRedisCache error: " + e.getMessage());
        }

        // Shutdown instance provider
        if (instanceProvider != null) {
            instanceProvider.shutdown();
        }

        // Complete any pending cross-server validation futures so callers unblock on shutdown.
        if (crossServerValidationService != null) {
            try { crossServerValidationService.shutdown(); } catch (Exception e) {
                getLogger().warning("CrossServerValidationService shutdown error: " + e.getMessage());
            }
        }

        // Shutdown dungeon party registry first so heartbeat/cleanup tasks stop before we
        // also tear down local DungeonPartyImpl caches via partyService.shutdown().
        if (dungeonPartyRegistry != null) {
            try { dungeonPartyRegistry.shutdown(); } catch (Exception e) {
                getLogger().warning("DungeonPartyRegistry shutdown error: " + e.getMessage());
            }
        }

        // Clear local DungeonPartyImpl cache
        DungeonPartyImpl.clearAll();

        // Shutdown party service
        if (partyService != null) {
            partyService.shutdown();
        }

        // Shutdown queue manager
        if (queueManager != null) {
            queueManager.shutdown();
        }

        // If this is an instance server, cleanup the instance data
        if (getInstanceProvider() != null && ServerUtil.isInstanceServer()) {
            InstanceInfo info = ServerUtil.getInstanceInfo();
            if (info != null) {
                // Remove instance from Redis
                if(dungeonService != null)
                    dungeonService.removeInstance(info.getInstanceId());
                else
                    getLogger().warning("Dungeon service is null while trying to remove instance " + info.getInstanceId());

                // Unregister from queue system
                if (dungeonQueueService != null) {
                    Floor floor = Floor.getFloor(info.getFloorId());
                    if (floor != null) {
                        dungeonQueueService.unregisterInstance(floor.getId(), info.getInstanceId());
                    }
                }

                getLogger().info(String.format("Cleaned up instance %s from Redis", info.getInstanceId()));
            }
        }

        // Clear local Redis data
        if (dungeonService != null) {
            dungeonService.clearLocal();
        }

        // Unload dynamic modules
        if (moduleLoader != null) {
            moduleLoader.unloadAll();
        }

        CupCodeAPI.disable();
        if (messaging != null) messaging.close();

        // Ces objets ne sont initialisés que sur les instances
        if (webEditorManager != null) {
            webEditorManager.shutdownAllEditors();
        }
        if (ghostFactory != null) {
            ghostFactory.close();
        }

        try {
            closeConnections();
        } catch (Exception e) {
            getLogger().severe("closeConnections error: " + e.getMessage());
        }
    }

    /**
     * Iterates every cached profile and persists it to the database. We use
     * {@link RMap#entrySet()} (not {@code readAllMap()}) so Redisson streams
     * entries over the wire instead of materialising all of them at once.
     */
    private void saveAllPendingData() {
        if (profileService == null || profileService.getProfilesMap() == null) {
            getLogger().info("[shutdown] No profile map to flush");
            return;
        }
        if (databaseManager == null) {
            getLogger().warning("[shutdown] databaseManager null — profiles will NOT be persisted");
            return;
        }
        int saved = 0;
        int failed = 0;
        for (Map.Entry<UUID, ProfileData> entry : profileService.getProfilesMap().entrySet()) {
            UUID pid = entry.getKey();
            ProfileData data = entry.getValue();
            if (pid == null || data == null) continue;
            try {
                data.setChecksum(data.calculateChecksum());
                databaseManager.saveProfileData(pid, data);
                saved++;
                if (saved % 100 == 0) {
                    getLogger().info("[shutdown] Flushed " + saved + " profile(s)...");
                }
            } catch (Exception e) {
                failed++;
                getLogger().warning("[shutdown] Failed to save profile " + pid + ": " + e.getMessage());
            }
        }
        getLogger().info("[shutdown] Profile flush complete — saved=" + saved + " failed=" + failed);
    }

    /**
     * Writes a small metadata bucket in Redis describing the last clean
     * shutdown — version, timestamp, server name. This is consumed on the
     * next startup by the health monitor to detect inconsistent states.
     * TTL: 7 days (long enough for a maintenance window, short enough to
     * avoid stale records lingering forever).
     */
    private void tagRedisCache() {
        if (dungeonService == null || dungeonService.getRedissonClient() == null) {
            return;
        }
        try {
            String topic = Objects.requireNonNull(getConfig().getString("RedisConfiguration.topic"));
            String key = topic + ":cache_tag";
            com.google.gson.JsonObject tag = new com.google.gson.JsonObject();
            tag.addProperty("pluginVersion", getDescription().getVersion());
            tag.addProperty("shutdownAt", System.currentTimeMillis());
            tag.addProperty("server", Bukkit.getServer().getName());
            dungeonService.getRedissonClient()
                    .getBucket(key, org.redisson.client.codec.StringCodec.INSTANCE)
                    .set(tag.toString(), 7, TimeUnit.DAYS);
            getLogger().info("[shutdown] Redis cache tagged (" + key + ")");
        } catch (Exception e) {
            getLogger().warning("[shutdown] tagRedisCache failed: " + e.getMessage());
        }
    }

    /**
     * Tears down external client connections. DB first (so pending writes from
     * {@link #saveAllPendingData()} can settle), then Redisson.
     */
    private void closeConnections() {
        if (databaseManager != null) {
            try { databaseManager.disconnect(); } catch (Exception e) {
                getLogger().warning("[shutdown] DB disconnect error: " + e.getMessage());
            }
        }
        if (dungeonService != null && dungeonService.getRedissonClient() != null) {
            try { dungeonService.getRedissonClient().shutdown(); } catch (Exception e) {
                getLogger().warning("[shutdown] Redisson shutdown error: " + e.getMessage());
            }
        }
    }

    /**
     * Loads all commands using {@link CommandHandler#registerCommands(Class)}.
     *
     * <p>This method is called in {@link #onEnable()} and loads all commands
     * from {@link AdminCommands}, {@link DebugCommands} and {@link PlayerCommands}.
     */
    private void loadCommands() {
        CommandHandler.registerParameterType(Dungeon.class, new DungeonParameterType());
        CommandHandler.registerParameterType(FloorData.class, new FloorParameterType());
        CommandHandler.registerParameterType(ModuleParameterType.class, new ModuleParameterType());

        commandHandler.registerCommands(AdminCommands.class);
        commandHandler.registerCommands(DebugCommands.class);
        commandHandler.registerCommands(PlayerCommands.class);
        commandHandler.registerCommands(PartyCommands.class);
        commandHandler.registerCommands(ConsoleCommands.class);
    }


    /**
     * Loads all event listeners for the plugin.
     *
     * <p>This method registers all necessary event listeners with the
     * server's plugin manager, allowing the plugin to respond to various
     * events occurring within the game environment.</p>
     */
    private void loadGlobalListeners() {
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new GlobalPartyListener(), this);
        pluginManager.registerEvents(new GlobalJoinListener(), this);
        pluginManager.registerEvents(new GlobalLeaveListener(), this);
        
        // Queue listener (only on lobby servers)
        if (!ServerUtil.isInstanceServer() && queueManager != null) {
            pluginManager.registerEvents(new fr.perrier.dungeons.spigot.listener.queue.QueueLeaveListener(), this);
        }
    }

    /**
     * Loads event listeners specific to dungeon instances.
     *
     * <p>This method registers event listeners that are relevant only
     * when the server is operating as a dungeon instance. It is called
     * during the initialization of an instance server in
     * {@link #initializeInstanceServer()}.</p>
     */
    private void loadInstanceListeners() {
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new InstanceJoinListener(), this);
        pluginManager.registerEvents(new InstanceMobKillListener(), this);
        pluginManager.registerEvents(new fr.perrier.dungeons.spigot.listener.dungeons.NaturalSpawnBlockListener(), this);
        pluginManager.registerEvents(new InstancePlayerDeathListener(), this);
        pluginManager.registerEvents(new ReviveItemListener(), this);
        pluginManager.registerEvents(new fr.perrier.dungeons.spigot.listener.dungeons.LootChestListener(), this);
    }

    /**
     * Loads event listeners specific to the editor server.
     *
     * <p>This method registers event listeners that are relevant only
     * when the server is operating in editor mode. It is called during
     * the initialization of an editor server in
     * {@link #initializeEditorServer()}.</p>
     */
    private void loadEditorListeners() {
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new EditorJoinListener(), this);
    }

    /**
     * Initializes the dungeon instance server.
     *
     * <p>This method retrieves the instance information using {@link ServerUtil#getInstanceInfo()}.
     * If the information is not available, it logs an error and disables the plugin.
     * Otherwise, it logs the initialization details, initializes the instance in Redis,
     * and schedules the server to be marked as ready.</p>
     *
     * <p>Note: The instance information includes the instance ID, floor ID,
     * and creation timestamp.</p>
     */
    private void initializeInstanceServer() {
        InstanceInfo info = ServerUtil.getInstanceInfo();
        if (info == null) {
            getLogger().severe("Failed to get instance information");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info(String.format(
                "Initializing dungeon instance server (ID: %s, Floor: %s, Created at: %s)",
                info.getInstanceId(),
                info.getFloorId(),
                info.getCreatedAt()
        ));

        loadInstanceListeners();

        // Initialize instance in Redis
        dungeonService.initializeInstance(info.getInstanceId(), info.getFloorId());

        // Make sure every dungeon world keeps inventory on death — players must
        // never drop their gear inside an instance. An instance server only
        // hosts the dungeon, so every loaded world is a dungeon world.
        enforceKeepInventory();

        // Register instance with queue system
        if (dungeonQueueService != null) {
            dungeonQueueService.registerInstance(info.getFloorId(), info.getInstanceId());
            getLogger().info(String.format("Registered instance %s with queue system", info.getInstanceId()));
        }

        // Reclaim this cloud service once it has sat empty for too long — players
        // all left, the run wiped, or nobody ever joined. Drops the Redis record
        // and shuts the server down so CloudNet can tear the service back down.
        new fr.perrier.dungeons.spigot.monitoring.EmptyInstanceWatchdog(this, info.getInstanceId()).start();

        // Schedule ready state
        putServerReady();
    }

    /**
     * Ensures every loaded world on this instance server has the
     * {@code keepInventory} gamerule enabled, setting it when it isn't.
     *
     * <p>Dungeon deaths must never drop the player's items. This is a safety
     * net in case a world template ships without the gamerule. Runs on the
     * main thread (called from {@link #initializeInstanceServer()} during
     * {@code onEnable}) — {@link World#setGameRule} requires it.</p>
     */
    private void enforceKeepInventory() {
        for (World world : Bukkit.getWorlds()) {
            Boolean current = world.getGameRuleValue(GameRule.KEEP_INVENTORY);
            if (current == null || !current) {
                world.setGameRule(GameRule.KEEP_INVENTORY, true);
                getLogger().info("[Dungeon] keepInventory was off in world '"
                        + world.getName() + "' — forced to true.");
            }
        }
    }

    /**
     * Initializes the editor server.
     *
     * <p>This method is called during the enabling of the plugin and
     * initializes the editor server. This method is a stub and can be
     * overridden in subclasses to provide custom initialization for the
     * editor server.</p>
     *
     * <p>This method is called after the instance server has been initialized
     * in {@link #initializeInstanceServer()}.</p>
     */
    private void initializeEditorServer() {
        getLogger().info("Initializing editor server");
        loadEditorListeners();
        // Editor specific initialization if needed
    }

    /**
     * Initializes the lobby server.
     *
     * <p>This method is called during the enabling of the plugin and
     * initializes the lobby server. This method is a stub and can be
     * overridden in subclasses to provide custom initialization for the
     * lobby server.</p>
     *
     * <p>This method is called after the instance server has been initialized
     * in {@link #initializeInstanceServer()}.</p>
     */
    private void initializeLobbyServer() {
        getLogger().info("Initializing lobby server");

        // Subscribe au canal de synchronisation dashboard pour recharger les floors en live.
        // Fait en premier (synchrone, non bloquant) afin de ne manquer aucun event dashboard
        // qui pourrait survenir pendant l'hydratation asynchrone ci-dessous.
        subscribeDashboardSyncChannel();

        // Charger tous les donjons depuis Redis (dashboard web) HORS du main thread :
        // cette hydratation touche la BDD (triggers, dungeons) et Redis et ne doit jamais
        // bloquer onEnable (risque watchdog sur un gros catalogue de floors).
        // Plus de chargement YAML — tout passe par Redis désormais
        // Pour migrer d'anciens donjons YAML: /dungeon admin migrate-all
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                RedisConfigLoader.loadAllDungeonsFromRedis();
            } catch (Exception e) {
                getLogger().severe("[initializeLobbyServer] Async dungeon hydration failed: " + e.getMessage());
            }
            // Rebuild the leaderboard sorted sets from the profiles table when missing
            // (first deploy / after a Redis flush). No-op once the sentinel is set.
            try {
                if (leaderboardService != null) {
                    leaderboardService.backfillIfEmpty(databaseManager);
                }
            } catch (Exception e) {
                getLogger().severe("[initializeLobbyServer] Leaderboard backfill failed: " + e.getMessage());
            }
        });
    }

    /**
     * S'abonne au canal Redis du dashboard pour recharger automatiquement
     * les floors quand ils sont créés/modifiés depuis l'interface web.
     */
    private void subscribeDashboardSyncChannel() {
        try {
            String topic = Objects.requireNonNull(getConfig().getString("RedisConfiguration.topic"));
            // Subscribe au canal string (messages JSON du dashboard proxy)
            dungeonService.getRedissonClient()
                    .getTopic(topic + ":sync")
                    .addListener(String.class, (channel, msg) -> {
                try {
                    com.google.gson.JsonObject json =
                            new com.google.gson.JsonParser().parse(msg).getAsJsonObject();
                    String type = json.has("type") ? json.get("type").getAsString() : "";
                    String id   = json.has("id")   ? json.get("id").getAsString()   : "";
                    if (id.isEmpty()) return;
                    switch (type) {
                        case "FLOOR_UPDATE" -> {
                            getLogger().info("[DashboardSync] Rechargement floor : " + id);
                            Bukkit.getScheduler().runTaskAsynchronously(this,
                                    () -> RedisConfigLoader.reloadFloorFromRedis(id));
                        }
                        case "FLOOR_DELETE" -> {
                            getLogger().info("[DashboardSync] Floor supprimé : " + id + " — suppression du template...");
                            ServerUtil.deleteFloorTemplate(id).thenAccept(success -> {
                                if (success) {
                                    getLogger().info("[DashboardSync] Template supprimé pour le floor : " + id);
                                } else {
                                    getLogger().warning("[DashboardSync] Echec suppression template pour le floor : " + id);
                                }
                            });
                        }
                        case "DUNGEON_UPDATE" -> {
                            getLogger().info("[DashboardSync] Donjon DUNGEON_UPDATE : " + id);
                            Dungeon existing = dungeonService.getDungeon(id);
                            if (existing == null) {
                                // Lire le nom depuis la clé dd:{id} (StringCodec, JSON)
                                String ddKey = Objects.requireNonNull(getConfig().getString("RedisConfiguration.topic")) + ":dd:" + id;
                                String entryJson = (String) dungeonService.getRedissonClient()
                                        .getBucket(ddKey, org.redisson.client.codec.StringCodec.INSTANCE).get();
                                String name = id; // fallback
                                if (entryJson != null) {
                                    try {
                                        com.google.gson.JsonObject e = new com.google.gson.JsonParser().parse(entryJson).getAsJsonObject();
                                        if (e.has("name")) name = e.get("name").getAsString();
                                    } catch (Exception ignored) {}
                                }
                                Dungeon newDungeon = new Dungeon(id, name);
                                dungeonService.syncDungeon(newDungeon);
                                getLogger().info("[DashboardSync] Donjon créé en mémoire : " + id + " (name=" + name + ")");
                            }
                        }
                        case "DUNGEON_DELETE" -> {
                            getLogger().info("[DashboardSync] Donjon DUNGEON_DELETE : " + id);
                            dungeonService.removeDungeon(id);
                        }
                    }
                } catch (Exception ignored) {
                    // Message non-JSON (RedisMessage Kryo natif) — ignorer silencieusement
                }
            });
            getLogger().info("✅ Abonnement dashboard sync channel actif (" + topic + ":sync)");
        } catch (Exception e) {
            getLogger().warning("⚠️ Impossible de s'abonner au dashboard sync channel : " + e.getMessage());
        }
    }


    /**
     * Puts the current instance server into a ready state.
     *
     * <p>This method is called during the enabling of the plugin and
     * schedules the instance server to be marked as ready after a short delay.
     * This method is necessary to ensure that the instance server is properly
     * initialized before being marked as ready.</p>
     *
     * <p>This method is called after the instance server has been initialized
     * in {@link #initializeInstanceServer()}.</p>
     */
    private void putServerReady() {
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
            @Override
            public void run(){
                Bukkit.getScheduler().runTaskLaterAsynchronously(Main.getInstance(), () -> {
                    FloorInstance instance = Main.getInstance().getDungeonService().getCurrentInstance();
                    if (instance != null) {
                        instance.setReady(true);
                        Main.getLoggerUtil().info("Instance " + instance.getInstanceId() + " is now ready!");
                    } else {
                        Main.getLoggerUtil().severe("No instance found!");
                    }
                }, 100L);
            }
        });
    }
}
