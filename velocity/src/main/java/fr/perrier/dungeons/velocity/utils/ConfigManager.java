package fr.perrier.dungeons.velocity.utils;

import com.moandjiezana.toml.Toml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private final Path dataDirectory;
    private Toml config;

    public ConfigManager(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        loadConfig();
    }

    private void loadConfig() {
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectory(dataDirectory);
            }

            File configFile = dataDirectory.resolve("config.toml").toFile();

            // Copy the default config from resources if it doesn't exist
            if (!configFile.exists()) {
                try (InputStream in = getClass().getResourceAsStream("/config.toml")) {
                    if (in != null) {
                        Files.copy(in, configFile.toPath());
                    }
                }
            }

            // Load the config TOML file
            config = new Toml().read(configFile);

        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    // Utility methods to get values from the config
    public String getString(String key) {
        if(config == null) {
            throw new IllegalStateException("Configuration not initialized; cannot get String for key: " + key);
        }
        return config.getString(key);
    }

    public Long getLong(String key) {
        if(config == null) {
            throw new IllegalStateException("Configuration not initialized; cannot get Long for key: " + key);
        }
        return config.getLong(key);
    }

    public Boolean getBoolean(String key) {
        if(config == null) {
            throw new IllegalStateException("Configuration not initialized; cannot get Boolean for key: " + key);
        }
        return config.getBoolean(key);
    }

    public Toml getTable(String key) {
        if(config == null) {
            throw new IllegalStateException("Configuration not initialized; cannot get Table for key: " + key);
        }
        return config.getTable(key);
    }
}