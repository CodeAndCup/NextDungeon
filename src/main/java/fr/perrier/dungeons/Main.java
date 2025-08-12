package fr.perrier.dungeons;

import com.alessiodp.parties.api.interfaces.PartiesAPI;
import fr.perrier.cupcodeapi.CupCodeAPI;
import fr.perrier.cupcodeapi.commands.CommandHandler;
import fr.perrier.dungeons.commands.AdminCommands;
import fr.perrier.dungeons.commands.DebugCommands;
import fr.perrier.dungeons.commands.EditorCommands;
import fr.perrier.dungeons.commands.PlayerCommands;
import fr.perrier.dungeons.messaging.Pidgin;
import fr.perrier.dungeons.model.FloorInstance;
import fr.perrier.dungeons.storage.RedisStorageService;
import fr.perrier.dungeons.utils.ServerUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.time.Instant;

public final class Main extends JavaPlugin {

    @Getter
    private static Main instance;
    @Getter
    private static final String prefix = "[Dungeons] ";

    @Getter@Setter
    private static boolean debug = false;

    // Plugin API instance
    @Getter
    private PartiesAPI partiesAPI;

    // Plugin commands
    @Getter
    private CommandHandler commandHandler;

    // Plugin packets pub/sub and sync storage
    @Getter
    private Pidgin messaging;
    @Getter
    private RedisStorageService redisStorageService;

    @Getter
    private ServerUtil serverUtil;

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
        } catch (Exception e) {
            getLogger().severe("Failed to initialize Redis storage service: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize server based on type
        if (ServerUtil.isInstanceServer()) {
            initializeInstanceServer();
        } else {
            initializeLobbyServer();
        }

        // Enabling other plugins API
        CupCodeAPI.enable(this);
        //partiesAPI = Parties.getApi();

        // Enabling messaging system
        this.messaging = new Pidgin(Main.getInstance().getConfig().getString("RedisConfiguration.topic"));

        // Loading commands
        this.commandHandler = new CommandHandler(this);
        loadCommands();

        // Loading listeners
        loadListeners();

        // Load Dungeons
        //ConfigLoader.loadAllDungeons();
    }

    @Override
    public void onDisable() {
        // If this is an instance server, cleanup the instance data
        if (ServerUtil.isInstanceServer()) {
            ServerUtil.InstanceInfo info = ServerUtil.getInstanceInfo();
            if (info != null) {
                // Remove instance from Redis
                redisStorageService.removeInstance(info.instanceId());
                getLogger().info(String.format("[%s] Cleaned up instance %s from Redis", Instant.now(), info.instanceId()));
            }
        }

        // Clear local Redis data
        if (redisStorageService != null) {
            redisStorageService.clearLocal();
        }

        CupCodeAPI.disable();
        Pidgin.shutdown();
    }

    /**
     * Loads all commands using {@link CommandHandler#registerCommands(Class)}.
     *
     * <p>This method is called in {@link #onEnable()} and loads all commands
     * from {@link AdminCommands}, {@link DebugCommands}, {@link EditorCommands},
     * and {@link PlayerCommands}.
     */
    private void loadCommands() {
        commandHandler.registerCommands(AdminCommands.class);
        commandHandler.registerCommands(DebugCommands.class);
        commandHandler.registerCommands(EditorCommands.class);
        commandHandler.registerCommands(PlayerCommands.class);
    }


    /**
     * Loads all event listeners for the plugin.
     *
     * <p>This method registers all necessary event listeners with the
     * server's plugin manager, allowing the plugin to respond to various
     * events occurring within the game environment.</p>
     */
    private void loadListeners() {
        PluginManager pluginManager = getServer().getPluginManager();
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
            getLogger().severe(String.format("[%s] Failed to get instance information", Instant.now()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info(String.format(
                "[%s] Initializing dungeon instance server (ID: %s, Floor: %s, Created at: %s)",
                Instant.now(),
                info.instanceId(),
                info.floorId(),
                info.createdAt()
        ));

        // Initialize instance in Redis
        redisStorageService.initializeInstance(info.instanceId(), info.floorId());

        // Schedule ready state
        putDungeonServerReady();
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
        getLogger().info(String.format("[%s] Initializing lobby server", "2025-08-12 13:49:06"));
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
    private void putDungeonServerReady() {
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
            @Override
            public void run(){
                Bukkit.getScheduler().runTaskLaterAsynchronously(Main.getInstance(), () -> {
                    FloorInstance instance = Main.getInstance().getRedisStorageService().getCurrentInstance().get();
                    if (instance != null) {
                        instance.setReady(true);
                        Main.getInstance().getLogger().info("Instance " + instance.getInstanceId() + " is now ready!");
                    } else {
                        Main.getInstance().getLogger().severe("No instance found!");
                    }
                }, 100L);
            }
        });
    }
}
