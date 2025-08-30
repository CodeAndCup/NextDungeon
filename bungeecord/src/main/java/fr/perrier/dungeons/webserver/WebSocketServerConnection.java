package fr.perrier.dungeons.webserver;

import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class WebSocketServerConnection implements ServerConnection {

    private final WebSocket webSocket;
    private final String serverName;
    private final Map<String, CompletableFuture<String>> pendingRequests;
    private final Gson gson;
    private final Logger logger;

    private int playerCount = 0;
    private int maxPlayers = 100;
    private long lastPing = System.currentTimeMillis();

    public WebSocketServerConnection(WebSocket webSocket, String serverName) {
        this.webSocket = webSocket;
        this.serverName = serverName;
        this.pendingRequests = new ConcurrentHashMap<>();
        this.gson = new Gson();
        this.logger = Logger.getLogger(getClass().getName());
    }

    @Override
    public CompletableFuture<String> sendRequest(String action, String data) {
        if (!isConnected()) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Connexion fermée"));
            return future;
        }

        String requestId = UUID.randomUUID().toString();
        CompletableFuture<String> future = new CompletableFuture<>();

        // Timeout de 30 secondes
        future.orTimeout(30, TimeUnit.SECONDS);

        pendingRequests.put(requestId, future);

        // Construire le message
        Map<String, Object> message = new HashMap<>();
        message.put("id", requestId);
        message.put("action", action);
        message.put("data", data);

        try {
            String messageJson = gson.toJson(message);
            webSocket.send(messageJson);
            logger.info("📤 Requête envoyée à " + serverName + ": " + action);

        } catch (Exception e) {
            pendingRequests.remove(requestId);
            future.completeExceptionally(e);
        }

        return future;
    }

    /**
     * Traite une réponse reçue du serveur Spigot
     */
    public void handleResponse(String message) {
        try {
            Map<String, Object> responseMap = gson.fromJson(message, Map.class);
            String requestId = (String) responseMap.get("id");

            if (requestId != null) {
                CompletableFuture<String> future = pendingRequests.remove(requestId);
                if (future != null) {
                    String responseData = (String) responseMap.get("response");
                    future.complete(responseData);
                    logger.info("📥 Réponse reçue de " + serverName + " pour " + requestId);
                }
            } else {
                // Message sans ID = mise à jour d'état
                handleStatusUpdate(responseMap);
            }

        } catch (Exception e) {
            logger.severe("Erreur traitement réponse de " + serverName + ": " + e.getMessage());
        }
    }

    /**
     * Traite les mises à jour d'état du serveur
     */
    private void handleStatusUpdate(Map<String, Object> data) {
        String type = (String) data.get("type");

        if ("STATUS_UPDATE".equals(type)) {
            Object playerCountObj = data.get("playerCount");
            Object maxPlayersObj = data.get("maxPlayers");

            if (playerCountObj instanceof Number) {
                this.playerCount = ((Number) playerCountObj).intValue();
            }
            if (maxPlayersObj instanceof Number) {
                this.maxPlayers = ((Number) maxPlayersObj).intValue();
            }

            logger.fine("📊 Mise à jour statut " + serverName + ": " + playerCount + "/" + maxPlayers);
        }
    }

    @Override
    public boolean isConnected() {
        return webSocket != null && webSocket.isOpen();
    }

    @Override
    public int getPlayerCount() {
        return playerCount;
    }

    @Override
    public int getMaxPlayers() {
        return maxPlayers;
    }

    @Override
    public long getLastPing() {
        return lastPing;
    }

    @Override
    public void ping() {
        if (isConnected()) {
            sendRequest("PING", "{}").thenAccept(response -> {
                this.lastPing = System.currentTimeMillis();
            }).exceptionally(throwable -> {
                logger.warning("Ping échoué pour " + serverName);
                return null;
            });
        }
    }

    @Override
    public void disconnect() {
        // Annuler toutes les requêtes en attente
        pendingRequests.forEach((id, future) -> {
            future.cancel(true);
        });
        pendingRequests.clear();

        if (webSocket != null && webSocket.isOpen()) {
            webSocket.close();
        }

        logger.info("🔌 Connexion fermée avec " + serverName);
    }

    /**
     * Méthode appelée quand la connexion WebSocket se ferme
     */
    public void onConnectionClosed() {
        disconnect();
    }
}
