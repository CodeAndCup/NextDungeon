package fr.perrier.dungeons;

import com.alessiodp.parties.api.Parties;
import com.alessiodp.parties.api.interfaces.PartiesAPI;
import fr.perrier.cupcodeapi.CupCodeAPI;
import fr.perrier.cupcodeapi.commands.CommandHandler;
import fr.perrier.dungeons.commands.AdminCommands;
import fr.perrier.dungeons.commands.DebugCommands;
import fr.perrier.dungeons.commands.EditorCommands;
import fr.perrier.dungeons.commands.PlayerCommands;
import fr.perrier.dungeons.configuration.ConfigLoader;
import fr.perrier.dungeons.messaging.Pidgin;
import fr.perrier.dungeons.utils.ServerUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    @Getter
    private static Main instance;
    @Getter
    private static String prefix = "[Dungeons] ";

    @Getter
    private static boolean lobbyServer;
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

        lobbyServer = getConfig().getBoolean("ServerConfiguration.isLobby");

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
        CupCodeAPI.disable();
        Pidgin.shutdown();
    }

    private void loadCommands() {
        commandHandler.registerCommands(AdminCommands.class);
        commandHandler.registerCommands(DebugCommands.class);
        commandHandler.registerCommands(EditorCommands.class);
        commandHandler.registerCommands(PlayerCommands.class);
    }

    private void loadListeners() {
        PluginManager pluginManager = getServer().getPluginManager();
    }
}
