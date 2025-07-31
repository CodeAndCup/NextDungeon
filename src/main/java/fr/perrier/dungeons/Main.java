package fr.perrier.dungeons;

import com.alessiodp.parties.api.Parties;
import com.alessiodp.parties.api.interfaces.PartiesAPI;
import com.grinderwolf.swm.api.SlimePlugin;
import fr.perrier.cupcodeapi.CupCodeAPI;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    @Getter
    private static Main instance;

    // Plugin API instance
    @Getter
    private PartiesAPI partiesAPI;
    @Getter
    private SlimePlugin aswmAPI;

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
    }

    @Override
    public void onDisable() {
        CupCodeAPI.disable();
    }
}
