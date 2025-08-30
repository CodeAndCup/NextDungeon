package fr.perrier.dungeons.bungee;

import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;

@Getter
public class NextDungeonBungee extends Plugin {

    @Getter
    private static NextDungeonBungee instance;

    private long startTime;

    @Override
    public void onEnable() {
        instance = this;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {

        getLogger().info("🛑 NextDungeon BungeeCord désactivé");
    }
}