package fr.perrier.dungeons.velocity;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.perrier.dungeons.common.messaging.Pidgin;
import fr.perrier.dungeons.velocity.messaging.PluginMessageVelocity;
import fr.perrier.dungeons.velocity.messaging.packets.webeditor.WebEditorRequestPacket;
import fr.perrier.dungeons.velocity.messaging.packets.webeditor.WebEditorResponsePacket;
import fr.perrier.dungeons.velocity.messaging.subscribers.WebEditorResponseSubscriber;
import fr.perrier.dungeons.velocity.utils.ConfigManager;
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

    private ConfigManager configManager;

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private long startTime;
    @Getter
    private ProxyWebEditorServer webEditorServer;
    @Getter
    private Pidgin messaging;
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
        
        // Load configuration
        configManager = new ConfigManager(dataDirectory);

        // Initialize and register the messaging service
        pluginMessageHandler = new PluginMessageVelocity();
        pluginMessageHandler.initialize();
        server.getEventManager().register(this, pluginMessageHandler);
        logger.info("Messaging system has been initialized");

        // Initialize messaging system
        try {
            Toml redis = configManager.getTable("redis");
            this.messaging = new Pidgin(
                    redis.getString("topic", "nextdungeon"),
                    redis.getString("host", "localhost"),
                    redis.getLong("port", 6379L).intValue(),
                    redis.getString("username", ""),
                    redis.getString("password", ""),
                    redis.getLong("database", 0L).intValue()
            );
            this.messaging.registerAdapter(WebEditorRequestPacket.class, null);
            this.messaging.registerAdapter(WebEditorResponsePacket.class, new WebEditorResponseSubscriber());
            logger.info("Redis messaging system has been initialized");
        } catch (Exception e) {
            logger.error("Erreur initialisation messaging Redis: {}", e.getMessage());
        }
        
        // Démarrer le serveur web centralisé avec le port configuré
        Toml webEditorConfig = configManager.getTable("webeditor");
        webEditorPort = webEditorConfig.getLong("port", 7734L).intValue();

        webEditorServer = new ProxyWebEditorServer(webEditorPort);

        if(messaging != null) {
            // Initialiser le tableau de bord avec le client Redisson
            webEditorServer.initializeDashboard(messaging.getClient());
        }

        if (webEditorServer.startServer()) {
            logger.info("Web server has been started on port {}", webEditorPort);
            logger.info("Dashboard available at http://localhost:{}/dashboard", webEditorPort);
        } else {
            logger.error("An error occurred during the start of the web server.");
        }

    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (webEditorServer != null) {
            webEditorServer.stopServer();
        }
        if (messaging != null) {
            messaging.close();
        }
        logger.info("NextDungeon Velocity disabled");
    }
}