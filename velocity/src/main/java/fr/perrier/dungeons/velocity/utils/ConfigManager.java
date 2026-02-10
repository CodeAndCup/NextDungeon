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

            // Copier le fichier par défaut si absent
            if (!configFile.exists()) {
                try (InputStream in = getClass().getResourceAsStream("/config.toml")) {
                    if (in != null) {
                        Files.copy(in, configFile.toPath());
                    }
                }
            }

            // Charger le fichier TOML
            config = new Toml().read(configFile);

        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    // Méthodes pour récupérer les valeurs
    public String getString(String key) {
        return config.getString(key);
    }

    public Long getLong(String key) {
        return config.getLong(key);
    }

    public Boolean getBoolean(String key) {
        return config.getBoolean(key);
    }

    public Toml getTable(String key) {
        return config.getTable(key);
    }
}