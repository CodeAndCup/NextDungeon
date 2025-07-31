package fr.perrier.dungeons;

import com.alessiodp.parties.api.Parties;
import com.alessiodp.parties.api.interfaces.PartiesAPI;
import com.grinderwolf.swm.api.SlimePlugin;
import fr.perrier.cupcodeapi.CupCodeAPI;
import fr.perrier.cupcodeapi.commands.CommandHandler;
import fr.perrier.dungeons.commands.AdminCommands;
import fr.perrier.dungeons.commands.PlayerCommands;
import lombok.Getter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    @Getter
    private static Main instance;

    // Plugin API instance
    @Getter
    private PartiesAPI partiesAPI;
    @Getter
    private SlimePlugin aswmAPI;

    // Plugin commands
    @Getter
    private CommandHandler commandHandler;

    @Override
    public void onEnable() {
        instance = this;

        // Enabling other plugins API
        CupCodeAPI.enable(this);
        if(!getServer().getPluginManager().isPluginEnabled("Parties")) {
            throw new RuntimeException("You must have Parties plugin enabled to use this plugin, shuting down the plugin...");
        } else {
            partiesAPI = Parties.getApi();
        }
        if(!getServer().getPluginManager().isPluginEnabled("SlimeWorldManager")) {
            throw new RuntimeException("You must have SlimeWorldManager plugin enabled to use this plugin, shuting down the plugin...");
        } else {
            aswmAPI = (SlimePlugin) getServer().getPluginManager().getPlugin("SlimeWorldManager");
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
