package fr.perrier.dungeons.bungee;

import fr.perrier.dungeons.bungee.messaging.packets.webeditor.WebEditorResponsePacket;
import fr.perrier.dungeons.bungee.messaging.subscribers.WebEditorResponseSubscriber;
import fr.perrier.dungeons.bungee.webeditor.ProxyWebEditorServer;
import fr.perrier.dungeons.common.messaging.Pidgin;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;
import java.io.File;
import java.io.InputStream;

@Getter
public class NextDungeonBungee extends Plugin {

    @Getter
    private static NextDungeonBungee instance;

    private long startTime;
    @Getter
    private ProxyWebEditorServer webEditorServer;
    @Getter
    private Pidgin messaging;
    
    private int webEditorPort = 7734; // Port par défaut

    @Override
    public void onEnable() {
        instance = this;
        this.startTime = System.currentTimeMillis();
        
        // Charger la configuration
        loadConfig();
        
        // Initialize messaging system
        try {
            // TODO: Read from config file
            this.messaging = new Pidgin(
                "dungeons:packets",  // topic name
                "localhost",  // redis host
                6379,  // redis port
                null,  // redis username
                null,   // redis password
                0
            );
            this.messaging.registerAdapter(WebEditorResponsePacket.class, new WebEditorResponseSubscriber());
            getLogger().info("✅ Système de messagerie Redis initialisé");
        } catch (Exception e) {
            getLogger().severe("❌ Erreur initialisation messaging Redis: " + e.getMessage());
        }
        
        // Démarrer le serveur web centralisé avec le port configuré
        webEditorServer = new ProxyWebEditorServer(webEditorPort);
        
        // Initialiser le tableau de bord avec le client Redisson
        if (messaging != null) {
            webEditorServer.initializeDashboard(messaging.getClient());
        }
        
        if (webEditorServer.startServer()) {
            getLogger().info("✅ Serveur web éditeur centralisé démarré sur le port " + webEditorPort);
            getLogger().info("📊 Dashboard disponible sur http://localhost:" + webEditorPort + "/dashboard");
        } else {
            getLogger().severe("❌ Impossible de démarrer le serveur web éditeur");
        }

    }
    
    private void loadConfig() {
        try {
            File configFile = new File(getDataFolder(), "config.yml");
            
            // Créer le dossier si nécessaire
            if (!getDataFolder().exists()) {
                getDataFolder().mkdir();
            }
            
            // Créer le fichier de config par défaut s'il n'existe pas
            if (!configFile.exists()) {
                try (InputStream in = getResourceAsStream("config.yml")) {
                    if (in != null) {
                        java.nio.file.Files.copy(in, configFile.toPath());
                        getLogger().info("Fichier de configuration créé: " + configFile.getPath());
                    }
                }
            }
            
            // Lire la configuration avec Bungee Config
            if (configFile.exists()) {
                net.md_5.bungee.config.Configuration config = net.md_5.bungee.config.ConfigurationProvider.getProvider(
                    net.md_5.bungee.config.YamlConfiguration.class
                ).load(configFile);
                
                webEditorPort = config.getInt("webeditor.port", 7734);
                getLogger().info("Port web éditeur configuré: " + webEditorPort);
            }
        } catch (Exception e) {
            getLogger().warning("Erreur lors du chargement de la configuration: " + e.getMessage() + ", utilisation des valeurs par défaut");
        }
    }

    @Override
    public void onDisable() {
        if (webEditorServer != null) {
            webEditorServer.stopServer();
        }
        if (messaging != null) {
            Pidgin.shutdown();
        }
        getLogger().info("🛑 NextDungeon BungeeCord désactivé");
    }
}