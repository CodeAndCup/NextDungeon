package fr.perrier.dungeons;

import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;

public class Main extends Plugin {
    @Getter
    private static Main instance;
    
    @Override
    public void onEnable() {
        instance = this;
    }

    @Override
    public void onDisable() {
    }
}
