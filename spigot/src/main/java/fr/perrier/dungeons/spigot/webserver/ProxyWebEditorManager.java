package fr.perrier.dungeons.spigot.webserver;

import fr.perrier.dungeons.spigot.Main;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
public class ProxyWebEditorManager {

    private ProxyWebSocketClient webSocketClient;
    private final String serverName;
    private final String proxyHost;
    private final int proxyPort;

    public ProxyWebEditorManager() {
        FileConfiguration config = Main.getInstance().getConfig();

        // Configuration dans config.yml
        this.serverName = config.getString("webeditor.server-name", getDefaultServerName());
        this.proxyHost = config.getString("webeditor.proxy-host", "localhost");
        this.proxyPort = config.getInt("webeditor.proxy-port", 8081);
    }

    private String getDefaultServerName() {
        // Générer un nom par défaut basé sur le port du serveur
        return "spigot-" + Main.getInstance().getServer().getPort();
    }

    /**
     * Démarre la connexion au proxy
     */
    public boolean start() {
        try {
            webSocketClient = new ProxyWebSocketClient(proxyHost, proxyPort, serverName);
            webSocketClient.connect();

            Main.getInstance().getLogger().info("🔌 Connexion au proxy BungeeCord initiée...");
            Main.getInstance().getLogger().info("📡 Serveur: " + serverName + " -> " + proxyHost + ":" + proxyPort);
            return true;

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("❌ Erreur connexion proxy: " + e.getMessage());
            return false;
        }
    }

    /**
     * Arrête la connexion au proxy
     */
    public void stop() {
        if (webSocketClient != null) {
            webSocketClient.shutdown();
            Main.getInstance().getLogger().info("🛑 Connexion proxy fermée");
        }
    }

    /**
     * Vérifie si connecté au proxy
     */
    public boolean isConnected() {
        return webSocketClient != null && webSocketClient.isOpen();
    }
}