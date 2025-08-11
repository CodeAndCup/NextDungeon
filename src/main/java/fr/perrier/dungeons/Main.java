package fr.perrier.dungeons;

import com.alessiodp.parties.api.Parties;
import com.alessiodp.parties.api.interfaces.PartiesAPI;
import com.google.gson.JsonObject;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import fr.perrier.cupcodeapi.CupCodeAPI;
import fr.perrier.cupcodeapi.commands.CommandHandler;
import fr.perrier.dungeons.commands.AdminCommands;
import fr.perrier.dungeons.commands.DebugCommands;
import fr.perrier.dungeons.commands.EditorCommands;
import fr.perrier.dungeons.commands.PlayerCommands;
import fr.perrier.dungeons.configuration.ConfigLoader;
import fr.perrier.dungeons.messaging.Pidgin;
import fr.perrier.dungeons.messaging.packets.InstanceReadyPacket;
import fr.perrier.dungeons.storage.local.LocalInstanceStorage;
import fr.perrier.dungeons.storage.local.LocalStorage;
import fr.perrier.dungeons.utils.ServerUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    @Getter
    private static Main instance;
    @Getter
    private static String prefix = "[Dungeons] ";

    @Getter
    private static LocalStorage localStorage;
    @Getter
    private static LocalInstanceStorage localInstanceStorage;

    @Getter@Setter
    private static boolean debug = false;

    // Plugin API instance
    @Getter
    private PartiesAPI partiesAPI;

    // Plugin commands
    @Getter
    private CommandHandler commandHandler;

    // Plugin packets pub/sub
    @Getter
    private Pidgin messaging;

    @Getter
    private ServerUtil serverUtil;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        localStorage = new LocalStorage();

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
                    localStorage.setReady(true);
                    getMessaging().sendPacket(new InstanceReadyPacket(localInstanceStorage.getCurrentFloorInstance()));
                }, 100L);
            }
        });
    }
}
