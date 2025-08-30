package fr.perrier.dungeons.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.perrier.dungeons.velocity.commands.WebEditorProxyCommand;
import fr.perrier.dungeons.velocity.webeditor.ProxyWebEditorServer;
import lombok.Getter;
import org.slf4j.Logger;

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

    private long startTime;
    @Getter
    private ProxyWebEditorServer webEditorServer;

    @Inject
    public NextDungeonVelocity(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        instance = this;
        this.startTime = System.currentTimeMillis();
        
        // Démarrer le serveur web centralisé
        webEditorServer = new ProxyWebEditorServer();
        if (webEditorServer.startServer()) {
            logger.info("✅ Serveur web éditeur centralisé démarré");
        } else {
            logger.error("❌ Impossible de démarrer le serveur web éditeur");
        }

        // Enregistrer les commandes
        server.getCommandManager().register(server.getCommandManager().metaBuilder("webeditor-proxy").aliases("webeditor").build(), new WebEditorProxyCommand());
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (webEditorServer != null) {
            webEditorServer.stopServer();
        }
        logger.info("🛑 NextDungeon Velocity désactivé");
    }
}