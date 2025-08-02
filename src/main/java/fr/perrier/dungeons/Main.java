package fr.perrier.dungeons;

import com.alessiodp.parties.api.Parties;
import com.alessiodp.parties.api.interfaces.PartiesAPI;
import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.loaders.file.FileLoader;
//import com.infernalsuite.asp.loaders.mysql.MysqlLoader;
import fr.perrier.cupcodeapi.CupCodeAPI;
import fr.perrier.cupcodeapi.commands.CommandHandler;
import fr.perrier.dungeons.commands.AdminCommands;
import fr.perrier.dungeons.commands.PlayerCommands;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class Main extends JavaPlugin {

    @Getter
    private static Main instance;
    @Getter
    private static String prefix = "[Dungeons] ";

    // Plugin API instance
    @Getter
    private PartiesAPI partiesAPI;
    @Getter
    private AdvancedSlimePaperAPI aspAPI;
    @Getter
    private SlimeLoader aspLoader;

    // Plugin commands
    @Getter
    private CommandHandler commandHandler;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        // Enabling other plugins API
        CupCodeAPI.enable(this);
        partiesAPI = Parties.getApi();
        aspAPI = AdvancedSlimePaperAPI.instance();
        try {
            aspLoader = new FileLoader(new File(Main.getInstance().getDataFolder() + File.separator + "../../slime_worlds/"));
            /*aspLoader = new MysqlLoader(
                    "jdbc:mysql://" + getConfig().getString("ASWMConfiguration.LoaderConfiguration.host") + ":" + getConfig().getInt("ASWMConfiguration.LoaderConfiguration.port") + "/" + getConfig().getString("ASWMConfiguration.LoaderConfiguration.database") + "?autoReconnect=true&allowMultiQueries=true&useSSL=" + getConfig().getBoolean("ASWMConfiguration.LoaderConfiguration.useSSL"),
                    Objects.requireNonNull(getConfig().getString("ASWMConfiguration.LoaderConfiguration.host")),
                    getConfig().getInt("ASWMConfiguration.LoaderConfiguration.port"),
                    Objects.requireNonNull(getConfig().getString("ASWMConfiguration.LoaderConfiguration.database")),
                    getConfig().getBoolean("ASWMConfiguration.LoaderConfiguration.useSSL"),
                    getConfig().getString("ASWMConfiguration.LoaderConfiguration.user"),
                    getConfig().getString("ASWMConfiguration.LoaderConfiguration.password")
            );*/
        }catch (Exception e) {
            e.fillInStackTrace();
        }

        // Loading commands
        commandHandler = new CommandHandler(this);
        loadCommands();

        // Loading listeners
        loadListeners();

    }

    @Override
    public void onDisable() {
        CupCodeAPI.disable();
    }

    private void loadCommands() {
        commandHandler.registerCommands(AdminCommands.class);
        commandHandler.registerCommands(PlayerCommands.class);
    }

    private void loadListeners() {
        PluginManager pluginManager = getServer().getPluginManager();
    }
}
