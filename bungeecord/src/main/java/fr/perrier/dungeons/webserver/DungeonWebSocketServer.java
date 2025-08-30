package fr.perrier.dungeons.webserver;

import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class DungeonWebSocketServer extends WebSocketServer {

    private static final int WEBSOCKET_PORT = 8081;

    private final ProxyServerManager serverManager;
    private final Map<WebSocket, WebSocketServerConnection> connections;
    private final Gson gson;
    private final Logger logger;

    public DungeonWebSocketServer(ProxyServerManager serverManager) {
        super(new InetSocketAddress(WEBSOCKET_PORT));
        this.serverManager = serverManager;
        this.connections = new ConcurrentHashMap<>();
        this.gson = new Gson();
        this.logger = Logger.getLogger(getClass().getName());
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        logger.info("🔌 Nouvelle connexion WebSocket: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        WebSocketServerConnection connection = connections.remove(conn);
        if (connection != null) {
            connection.onConnectionClosed();
            logger.info("❌ Connexion WebSocket fermée: " + reason);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            Map<String, Object> messageMap = gson.fromJson(message, Map.class);
            String type = (String) messageMap.get("type");

            if ("REGISTER".equals(type)) {
                handleServerRegistration(conn, messageMap);
            } else {
                // Message de réponse ou mise à jour d'un serveur déjà enregistré
                WebSocketServerConnection connection = connections.get(conn);
                if (connection != null) {
                    connection.handleResponse(message);
                }
            }

        } catch (Exception e) {
            logger.severe("Erreur traitement message WebSocket: " + e.getMessage());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        logger.severe("Erreur WebSocket: " + ex.getMessage());
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        logger.info("🚀 Serveur WebSocket démarré sur le port " + WEBSOCKET_PORT);
    }

    /**
     * Traite l'enregistrement d'un nouveau serveur Spigot
     */
    private void handleServerRegistration(WebSocket conn, Map<String, Object> messageMap) {
        String serverName = (String) messageMap.get("serverName");

        if (serverName == null || serverName.trim().isEmpty()) {
            logger.warning("⚠️ Tentative d'enregistrement sans nom de serveur");
            conn.close(1000, "Nom de serveur requis");
            return;
        }

        // Créer la connexion et l'enregistrer
        WebSocketServerConnection connection = new WebSocketServerConnection(conn, serverName);
        connections.put(conn, connection);
        serverManager.registerServer(serverName, connection);

        // Confirmer l'enregistrement
        Map<String, Object> response = Map.of(
                "type", "REGISTRATION_CONFIRMED",
                "serverName", serverName,
                "success", true
        );

        conn.send(gson.toJson(response));
        logger.info("✅ Serveur enregistré: " + serverName);
    }
}