package fr.perrier.dungeons.bungee;

import fr.perrier.dungeons.bungee.commands.WebEditorProxyCommand;
import fr.perrier.dungeons.bungee.webeditor.ProxyWebEditorServer;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;

@Getter
public class NextDungeonBungee extends Plugin {

    @Getter
    private static NextDungeonBungee instance;

    private long startTime;
    @Getter
    private ProxyWebEditorServer webEditorServer;

    @Override
    public void onEnable() {
        instance = this;
        this.startTime = System.currentTimeMillis();
        
        // Démarrer le serveur web centralisé
        webEditorServer = new ProxyWebEditorServer();
        if (webEditorServer.startServer()) {
            getLogger().info("✅ Serveur web éditeur centralisé démarré");
        } else {
            getLogger().severe("❌ Impossible de démarrer le serveur web éditeur");
        }

        // Enregistrer les commandes
        getProxy().getPluginManager().registerCommand(this, new WebEditorProxyCommand());
    }

    @Override
    public void onDisable() {
        if (webEditorServer != null) {
            webEditorServer.stopServer();
        }
        getLogger().info("🛑 NextDungeon BungeeCord désactivé");
    }
}