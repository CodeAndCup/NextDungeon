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
import fr.perrier.dungeons.spigot.commands.AdminCommands;
import fr.perrier.dungeons.spigot.commands.ConsoleCommands;
import fr.perrier.dungeons.spigot.commands.DebugCommands;
import fr.perrier.dungeons.spigot.commands.PlayerCommands;
import fr.perrier.dungeons.spigot.configuration.ConfigLoader;
import fr.perrier.dungeons.spigot.database.DatabaseFactory;
import fr.perrier.dungeons.spigot.database.DatabaseManager;
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
import fr.perrier.dungeons.spigot.messaging.subscribers.CancelInstanceSubscriber;
import fr.perrier.dungeons.spigot.workflow.registry.TriggersRegistry;
import fr.perrier.dungeons.spigot.workflow.registry.VariableRegistry;
import fr.perrier.dungeons.common.messaging.Pidgin;
import fr.perrier.dungeons.spigot.messaging.ServerNameService;
import fr.perrier.dungeons.spigot.messaging.packets.PlayerSwitchServerPacket;
import fr.perrier.dungeons.spigot.messaging.packets.webeditor.WebEditorRequestPacket;
import fr.perrier.dungeons.spigot.messaging.packets.webeditor.WebEditorResponsePacket;
import fr.perrier.dungeons.spigot.messaging.subscribers.PlayerSwitchServerSubscriber;
import fr.perrier.dungeons.spigot.messaging.subscribers.WebEditorRequestSubscriber;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import fr.perrier.dungeons.spigot.storage.ProfileService;
import fr.perrier.dungeons.spigot.storage.DungeonService;
import fr.perrier.dungeons.spigot.queue.DungeonQueueService;
import fr.perrier.dungeons.spigot.queue.QueueManager;
import fr.perrier.dungeons.spigot.utils.ServerUtil;
import fr.perrier.dungeons.spigot.webeditor.DungeonWebEditorManager;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
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

@Getter
public final class Main extends JavaPlugin {

    @Getter
    private static Main instance;
    @Getter
    private static final String prefix = "<gradient:#8B0000:bold>NextDungeon</gradient:#D10000> &8» &r";

    @Getter@Setter
    private static boolean debug = false;

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
    private DatabaseManager databaseManager;
    private ServerNameService serverNameService;

    // Web editor manager
    private DungeonWebEditorManager webEditorManager;


    // Global trigger manager
    private TriggersRegistry triggersRegistry;
    private VariableRegistry variableRegistry;

    // Instance provider (CloudNet, ASP, ou Vanilla)
    private InstanceProvider instanceProvider;

    // Party service
    private PartyService partyService;

    // Queue management
    private DungeonQueueService dungeonQueueService;
    private QueueManager queueManager;

    @Override
    public void onEnable() {
        getLogger().info("Starting NextDungeon plugin...");
        long startTime = System.currentTimeMillis();

        instance = this;

        // Save default config
        saveDefaultConfig();

        // Initialize Redis Configuration
        Config config = new Config();
        config.useSingleServer().setAddress("redis://"
                        + Main.getInstance().getConfig().getString("RedisConfiguration.host")
                        + ":"
                        + Main.getInstance().getConfig().getInt("RedisConfiguration.port"))
                .setUsername(Main.getInstance().getConfig().getString("RedisConfiguration.username"))
                .setPassword(Main.getInstance().getConfig().getString("RedisConfiguration.password"))
                .setDatabase(Main.getInstance().getConfig().getInt("RedisConfiguration.database"));

        // Create Redis client
        RedissonClient redissonClient = Redisson.create(config);

        try {
            // Initialize Redis storage service
            dungeonService = new DungeonService(redissonClient);
            dungeonService.initialize();
            getLogger().info("Redis storage service initialized successfully");
        }catch (Exception e) {
            getLogger().severe("&#FF0000Failed to initialize Redis: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize Instance Provider
        try {
            instanceProvider = InstanceProviderFactory.createProvider();
            instanceProvider.initialize().thenAccept(success -> {
                if (!success) {
                    getLogger().severe("&#FF0000Failed to initialize instance provider");
                    getServer().getPluginManager().disablePlugin(this);
                }
            });
        } catch (Exception e) {
            getLogger().severe("&#FF0000Failed to initialize instance provider: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {

            // Initialize Profile service
            profileService = new ProfileService(redissonClient);
            profileService.initialize();
            getLogger().info("Profile service initialized successfully");

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
            getLogger().severe("&#FF0000Failed to initialize Redis services: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        databaseManager = DatabaseFactory.createDatabase();


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
        this.messaging = new Pidgin(getConfig().getString("RedisConfiguration.topic"),config);
        this.messaging.registerAdapter(PlayerSwitchServerPacket.class, new PlayerSwitchServerSubscriber());
        this.messaging.registerAdapter(WebEditorRequestPacket.class, new WebEditorRequestSubscriber());
        this.messaging.registerAdapter(WebEditorResponsePacket.class, null);
        this.messaging.registerAdapter(CancelInstancePacket.class, new CancelInstanceSubscriber());

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

        getLogger().info("NextDungeon " + this.getDescription().getVersion()  + " started in " + (System.currentTimeMillis() - startTime) + " ms");
    }

    @Override
    public void onDisable() {
        // Shutdown instance provider
        if (instanceProvider != null) {
            instanceProvider.shutdown();
        }

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
                dungeonService.removeInstance(info.getInstanceId());
                
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

        CupCodeAPI.disable();
        Pidgin.shutdown();

        // Ces objets ne sont initialisés que sur les instances
        if (webEditorManager != null) {
            webEditorManager.shutdownAllEditors();
        }
        if (ghostFactory != null) {
            ghostFactory.close();
        }
    }

    /**
     * Loads all commands using {@link CommandHandler#registerCommands(Class)}.
     *
     * <p>This method is called in {@link #onEnable()} and loads all commands
     * from {@link AdminCommands}, {@link DebugCommands} and {@link PlayerCommands}.
     */
    private void loadCommands() {
        commandHandler.registerCommands(AdminCommands.class);
        commandHandler.registerCommands(DebugCommands.class);
        commandHandler.registerCommands(PlayerCommands.class);
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
        pluginManager.registerEvents(new InstancePlayerDeathListener(), this);
        pluginManager.registerEvents(new ReviveItemListener(), this);
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
            getLogger().severe("&#FF0000Failed to get instance information");
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

        // Register instance with queue system
        if (dungeonQueueService != null) {
            dungeonQueueService.registerInstance(info.getFloorId(), info.getInstanceId());
            getLogger().info(String.format("Registered instance %s with queue system", info.getInstanceId()));
        }

        // Schedule ready state
        putServerReady();
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

        // Load all dungeons only on lobby server
        // Instance servers only need to retrieve their specific floor from Redis
        ConfigLoader.loadAllDungeons();

        // Lobby specific initialization if needed
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
                        Main.getInstance().getLogger().info("Instance " + instance.getInstanceId() + " is now ready!");
                    } else {
                        Main.getInstance().getLogger().severe("&#FF0000No instance found!");
                    }
                }, 100L);
            }
        });
    }
}
