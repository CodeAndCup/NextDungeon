package fr.perrier.dungeons.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.perrier.dungeons.velocity.messaging.PluginMessageVelocity;
import fr.perrier.dungeons.velocity.messaging.ProxyPidgin;
import fr.perrier.dungeons.velocity.webeditor.ProxyWebEditorServer;
import lombok.Getter;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

@Plugin(
    id = "nextdungeon-velocity",
    name = "NextDungeon Velocity",
    version = "1.0-SNAPSHOT",
    description = "Web editor centralise pour les dungeons NextDungeon",
    authors = {"PerrierBouteille"}
)
@Getter
public class NextDungeonVelocity {

    @Getter
    private static NextDungeonVelocity instance;

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private long startTime;
    @Getter
    private ProxyWebEditorServer webEditorServer;
    @Getter
    private ProxyPidgin messaging;
    @Getter
    private PluginMessageVelocity pluginMessageHandler;

    private int webEditorPort = 7734; // Port par défaut

    @Inject
    public NextDungeonVelocity(ProxyServer server, Logger logger, @com.velocitypowered.api.plugin.annotation.DataDirectory java.nio.file.Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        instance = this;
        this.startTime = System.currentTimeMillis();
        
        // Charger la configuration
        loadConfig();

        // Initialiser et enregistrer le gestionnaire de messages plugin
        pluginMessageHandler = new PluginMessageVelocity();
        pluginMessageHandler.initialize();
        server.getEventManager().register(this, pluginMessageHandler);
        logger.info("✅ Système de messagerie Plugin initialisé");

        // Initialize messaging system
        try {
            // TODO: Read from config file
            this.messaging = new ProxyPidgin(
                "dungeons:packets",  // topic name
                "localhost",  // redis host
                6379,  // redis port
                null,  // redis username
                null   // redis password
            );
            logger.info("✅ Système de messagerie Redis initialisé");
        } catch (Exception e) {
            logger.error("❌ Erreur initialisation messaging Redis: " + e.getMessage());
        }
        
        // Démarrer le serveur web centralisé avec le port configuré
        webEditorServer = new ProxyWebEditorServer(webEditorPort);

        if(messaging != null) {
            // Initialiser le tableau de bord avec le client Redisson
            webEditorServer.initializeDashboard(messaging.getClient());
        }

        if (webEditorServer.startServer()) {
            logger.info("✅ Serveur web éditeur centralisé démarré sur le port " + webEditorPort);
            logger.info("📊 Dashboard disponible sur http://localhost:" + webEditorPort + "/dashboard");
        } else {
            logger.error("❌ Impossible de démarrer le serveur web éditeur");
        }

    }

    private void loadConfig() {
        try {
            Path configPath = dataDirectory.resolve("config.toml");

            // Créer le dossier si nécessaire
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }

            // Créer le fichier de config par défaut s'il n'existe pas
            if (!Files.exists(configPath)) {
                try (java.io.InputStream in = getClass().getResourceAsStream("/config.toml")) {
                    if (in != null) {
                        Files.copy(in, configPath);
                        logger.info("Fichier de configuration créé: " + configPath);
                    }
                }
            }

            // Lire la configuration
            if (Files.exists(configPath)) {
                String content = Files.readString(configPath);
                // Parse simple du TOML pour récupérer le port
                for (String line : content.split("\n")) {
                    line = line.trim();
                    if (line.startsWith("port") && line.contains("=")) {
                        String portStr = line.split("=")[1].trim();
                        try {
                            webEditorPort = Integer.parseInt(portStr);
                            logger.info("Port web éditeur configuré: " + webEditorPort);
                        } catch (NumberFormatException e) {
                            logger.warn("Port invalide dans la configuration, utilisation du port par défaut: 7734");
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Erreur lors du chargement de la configuration: " + e.getMessage() + ", utilisation des valeurs par défaut");
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (webEditorServer != null) {
            webEditorServer.stopServer();
        }
        if (messaging != null) {
            ProxyPidgin.shutdown();
        }
        logger.info("🛑 NextDungeon Velocity désactivé");
    }
}