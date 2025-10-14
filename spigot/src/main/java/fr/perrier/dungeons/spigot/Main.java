package fr.perrier.dungeons.spigot;

import com.alessiodp.parties.api.Parties;
import com.alessiodp.parties.api.interfaces.PartiesAPI;
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
import fr.perrier.dungeons.spigot.listener.dungeons.InstanceJoinListener;
import fr.perrier.dungeons.spigot.listener.dungeons.InstanceMobKillListener;
import fr.perrier.dungeons.spigot.listener.dungeons.InstancePlayerDeathListener;
import fr.perrier.dungeons.spigot.listener.editor.EditorJoinListener;
import fr.perrier.dungeons.spigot.listener.global.GlobalJoinListener;
import fr.perrier.dungeons.spigot.listener.global.GlobalLeaveListener;
import fr.perrier.dungeons.spigot.listener.global.GlobalPartyListener;
import fr.perrier.dungeons.spigot.manager.GlobalTriggerManager;
import fr.perrier.dungeons.spigot.manager.VariableManager;
import fr.perrier.dungeons.spigot.messaging.Pidgin;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import fr.perrier.dungeons.spigot.storage.ProfileService;
import fr.perrier.dungeons.spigot.storage.RedisStorageService;
import fr.perrier.dungeons.spigot.utils.ServerUtil;
import fr.perrier.dungeons.spigot.webeditor.SpigotProxyBridge;
import fr.perrier.dungeons.spigot.webserver.DungeonWebEditorManager;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public final class Main extends JavaPlugin {

    @Getter
    private static Main instance;
    @Getter
    private static final String prefix = "<gradient:#8B0000:bold>NextDungeon</gradient:#D10000> &8» &r";

    @Getter@Setter
    private static boolean debug = false;

    // Plugin API instance
    @Getter
    private PartiesAPI partiesAPI;

    // Plugin commands
    @Getter
    private CommandHandler commandHandler;

    // Plugin menu
    @Getter
    private MenuAPI menuAPI;

    // Plugin packets pub/sub and sync storage
    @Getter
    private Pidgin messaging;
    @Getter
    private RedisStorageService redisStorageService;
    @Getter
    private ProfileService profileService;
    @Getter
    private DatabaseManager databaseManager;

    // Web editor manager
    @Getter@Deprecated
    private DungeonWebEditorManager webEditorManager;
    
    // Proxy bridge for web editor communication
    @Getter
    private fr.perrier.dungeons.spigot.webeditor.SpigotProxyBridge proxyBridge;

    // Global trigger manager
    @Getter
    private GlobalTriggerManager globalTriggerManager;
    @Getter
    private VariableManager variableManager;

    @Override
    public void onEnable() {
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
                .setPassword(Main.getInstance().getConfig().getString("RedisConfiguration.password"));

        try {
            // Create Redis client
            RedissonClient redissonClient = Redisson.create(config);

            // Initialize Redis storage service
            redisStorageService = new RedisStorageService(redissonClient);
            redisStorageService.initialize();
            getLogger().info("Redis storage service initialized successfully");


            // Initialize Profile service
            profileService = new ProfileService(redissonClient);
            profileService.initialize();
            getLogger().info("Profile service initialized successfully");

        } catch (Exception e) {
            getLogger().severe("&cFailed to initialize Redis services: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        databaseManager = DatabaseFactory.createDatabase();

        // Load Dungeons
        ConfigLoader.loadAllDungeons();

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

        // Enabling other plugins API
        CupCodeAPI.enable(this);
        menuAPI = new MenuAPI(this);
        partiesAPI = Parties.getApi();

        // Enabling messaging system
        this.messaging = new Pidgin(Main.getInstance().getConfig().getString("RedisConfiguration.topic"));

        // Loading commands
        this.commandHandler = new CommandHandler(this);
        loadCommands();

        // Loading listeners
        loadGlobalListeners();

        // Initialize trigger system
        globalTriggerManager = new GlobalTriggerManager();
        globalTriggerManager.initialize();
        globalTriggerManager.refreshTriggerCache();

        // Initialize variable manager
        variableManager = new VariableManager();


        // Initialize web editor manager
        webEditorManager = new DungeonWebEditorManager();
        
        // Initialize proxy bridge for web editor communication
        proxyBridge = new SpigotProxyBridge();
        if (proxyBridge.startBridge()) {
            getLogger().info("✅ Pont de communication proxy démarré");
        } else {
            getLogger().warning("⚠️ Impossible de démarrer le pont proxy");
        }
    }

    @Override
    public void onDisable() {
        // If this is an instance server, cleanup the instance data
        if (ServerUtil.isInstanceServer()) {
            ServerUtil.InstanceInfo info = ServerUtil.getInstanceInfo();
            if (info != null) {
                // Remove instance from Redis
                redisStorageService.removeInstance(info.instanceId());
                getLogger().info(String.format("Cleaned up instance %s from Redis", info.instanceId()));
            }
        }

        // Clear local Redis data
        if (redisStorageService != null) {
            redisStorageService.clearLocal();
        }

        CupCodeAPI.disable();
        Pidgin.shutdown();
        webEditorManager.shutdownAllEditors();
        
        // Arrêter le pont de communication proxy
        if (proxyBridge != null) {
            proxyBridge.stopBridge();
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
        ServerUtil.InstanceInfo info = ServerUtil.getInstanceInfo();
        if (info == null) {
            getLogger().severe("&cFailed to get instance information");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info(String.format(
                "Initializing dungeon instance server (ID: %s, Floor: %s, Created at: %s)",
                info.instanceId(),
                info.floorId(),
                info.createdAt()
        ));

        loadInstanceListeners();

        // Initialize instance in Redis
        redisStorageService.initializeInstance(info.instanceId(), info.floorId());

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
                    FloorInstance instance = Main.getInstance().getRedisStorageService().getCurrentInstance().get();
                    if (instance != null) {
                        instance.setReady(true);
                        Main.getInstance().getLogger().info("Instance " + instance.getInstanceId() + " is now ready!");
                    } else {
                        Main.getInstance().getLogger().severe("&cNo instance found!");
                    }
                }, 100L);
            }
        });
    }
}
