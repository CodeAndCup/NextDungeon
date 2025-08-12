package fr.perrier.dungeons;

import com.alessiodp.parties.api.interfaces.PartiesAPI;
import fr.perrier.cupcodeapi.CupCodeAPI;
import fr.perrier.cupcodeapi.commands.CommandHandler;
import fr.perrier.dungeons.commands.AdminCommands;
import fr.perrier.dungeons.commands.DebugCommands;
import fr.perrier.dungeons.commands.EditorCommands;
import fr.perrier.dungeons.commands.PlayerCommands;
import fr.perrier.dungeons.messaging.Pidgin;
import fr.perrier.dungeons.messaging.packets.InstanceReadyPacket;
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

public final class Main extends JavaPlugin {

    @Getter
    private static Main instance;
    @Getter
    private static String prefix = "[Dungeons] ";

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
        putDungeonServerReady();
    }

    @Override
    public void onDisable() {
        CupCodeAPI.disable();
        Pidgin.shutdown();
    }

    /**
     * Loads all commands using {@link CommandHandler#registerCommands(Class...)}.
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
     * Puts the server into a ready state.
     *
     * <p>This method schedules a task to run 100 ticks (5 seconds) after
     * the server has enabled, and sends an {@link InstanceReadyPacket} to
     * all connected clients. This is used to signal to the clients that the
     * server is ready to accept players.
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
