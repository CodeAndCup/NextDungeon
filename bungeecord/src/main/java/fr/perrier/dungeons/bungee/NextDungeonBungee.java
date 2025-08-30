package fr.perrier.dungeons.bungee;

import fr.perrier.dungeons.bungee.webserver.DungeonWebServer;
import fr.perrier.dungeons.bungee.webserver.DungeonWebSocketServer;
import fr.perrier.dungeons.bungee.webserver.ProxyServerManager;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

@Getter
public class NextDungeonBungee extends Plugin {

    @Getter
    private static NextDungeonBungee instance;
    private Configuration config;

    private ProxyServerManager serverManager;
    private DungeonWebServer webServer;
    private DungeonWebSocketServer webSocketServer;

    private long startTime;

    @Override
    public void onEnable() {
        instance = this;
        this.startTime = System.currentTimeMillis();

        // Charger la configuration
        loadConfig();

        // Initialiser les composants
        serverManager = new ProxyServerManager();

        // Démarrer le serveur WebSocket pour les connexions Spigot
        webSocketServer = new DungeonWebSocketServer(serverManager);
        webSocketServer.start();

        // Démarrer le serveur web pour l'interface
        webServer = new DungeonWebServer(getLogger());
        if (webServer.start()) {
            getLogger().info("✅ NextDungeon BungeeCord activé avec succès");
        } else {
            getLogger().severe("❌ Erreur démarrage du serveur web");
        }

        // Ping périodique des serveurs connectés
        getProxy().getScheduler().schedule(this, () -> {
            serverManager.pingAllServers();
        }, 30, 30, TimeUnit.SECONDS);
    }

    @Override
    public void onDisable() {
        if (webSocketServer != null) {
            try {
                webSocketServer.stop();
            } catch (InterruptedException e) {
                getLogger().warning("Erreur arrêt WebSocket: " + e.getMessage());
            }
        }

        if (webServer != null) {
            webServer.stop();
        }

        getLogger().info("🛑 NextDungeon BungeeCord désactivé");
    }

    private void loadConfig() {
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdir();
            }

            File configFile = new File(getDataFolder(), "config.yml");

            if (!configFile.exists()) {
                try (InputStream in = getResourceAsStream("config.yml")) {
                    Files.copy(in, configFile.toPath());
                }
            }

            config = ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .load(configFile);

        } catch (IOException e) {
            getLogger().severe("Erreur chargement config: " + e.getMessage());
        }
    }

    public Configuration getConfiguration() {
        return config;
    }
}