package fr.perrier.dungeons.utils;

import fr.perrier.dungeons.Main;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class DungeonConfigurationReader {

    public DungeonConfiguration readFrom(String fileName) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File(Main.getInstance().getDataFolder() + File.separator + "dungeons" + File.separator + fileName + ".yml"));

        return new DungeonConfiguration(
                cfg.getString("DungeonName"),
                cfg.getString("WorldConfiguration.WorldName"),
                cfg.getString("WorldConfiguration.Difficulty"),
                cfg.getInt("WorldConfiguration.Spawn.X"),
                cfg.getInt("WorldConfiguration.Spawn.Y"),
                cfg.getInt("WorldConfiguration.Spawn.Z")
        );
    }

    public void saveTo(String fileName, DungeonConfiguration configuration) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File(Main.getInstance().getDataFolder() + File.separator + "dungeons" + File.separator + fileName + ".yml"));

        cfg.set("DungeonName", configuration.getDungeonName());
        cfg.set("WorldConfiguration.WorldName", configuration.getWorldName());
        cfg.set("WorldConfiguration.Difficulty", configuration.getDifficulty());
        cfg.set("WorldConfiguration.Spawn.X", configuration.getSpawnX());
        cfg.set("WorldConfiguration.Spawn.Y", configuration.getSpawnY());
        cfg.set("WorldConfiguration.Spawn.Z", configuration.getSpawnZ());

        try {
            cfg.save(new File(Main.getInstance().getDataFolder() + File.separator + "dungeons" + File.separator + fileName + ".yml"));
            Bukkit.getLogger().info(Main.getPrefix() + " Dungeon " + configuration.getDungeonName() + " saved.");
        } catch (Exception e) {
            Bukkit.getLogger().severe(Main.getPrefix() + " An error occurred while saving the dungeon " + configuration.getDungeonName() + ".");
            e.fillInStackTrace();
        }
    }


    @Getter
    @Setter
    @AllArgsConstructor
    public static class DungeonConfiguration {
        private String dungeonName;
        private String worldName;
        private String difficulty;
        private int spawnX;
        private int spawnY;
        private int spawnZ;
    }
}
