package fr.perrier.dungeons.utils;

import com.grinderwolf.swm.api.loaders.SlimeLoader;
import fr.perrier.dungeons.Main;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ASWMUtil {

    private static String loaderType;

    public static void loadConfig() {
        YamlConfiguration cfg = (YamlConfiguration) Main.getInstance().getConfig();
        loaderType = cfg.getString("ASWMConfiguration.Loader");
    }

    public static SlimeLoader getLoaderType() {
        return Main.getInstance().getAswmAPI().getLoader(loaderType);
    }
}
