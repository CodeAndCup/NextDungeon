package fr.perrier.dungeons.webserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class ProxyServerManager {

    private final Map<String, ServerConnection> connectedServers;
    private final Gson gson;
    private final Logger logger;

    public ProxyServerManager() {
        this.connectedServers = new ConcurrentHashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.logger = Logger.getLogger(getClass().getName());
    }

    /**
     * Enregistre une nouvelle connexion serveur
     */
    public void registerServer(String serverName, ServerConnection connection) {
        connectedServers.put(serverName, connection);
        logger.info("✅ Serveur Spigot enregistré: " + serverName);
    }

    /**
     * Désenregistre un serveur
     */
    public void unregisterServer(String serverName) {
        connectedServers.remove(serverName);
        logger.info("❌ Serveur Spigot déconnecté: " + serverName);
    }

    /**
     * Récupère la liste des serveurs disponibles
     */
    public String getAvailableServers() {
        List<Map<String, Object>> servers = new ArrayList<>();

        connectedServers.forEach((name, connection) -> {
            Map<String, Object> serverInfo = new HashMap<>();
            serverInfo.put("name", name);
            serverInfo.put("online", connection.isConnected());
            serverInfo.put("playerCount", connection.getPlayerCount());
            serverInfo.put("maxPlayers", connection.getMaxPlayers());
            serverInfo.put("lastPing", connection.getLastPing());
            servers.add(serverInfo);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("servers", servers);
        response.put("count", servers.size());

        return gson.toJson(response);
    }

    /**
     * Récupère les floors d'un serveur spécifique
     */
    public String getServerFloors(String serverName) {
        ServerConnection connection = connectedServers.get(serverName);

        if (connection == null || !connection.isConnected()) {
            return createErrorResponse("Serveur non disponible: " + serverName);
        }

        try {
            CompletableFuture<String> future = connection.sendRequest("GET_FLOORS", "{}");
            return future.get();

        } catch (Exception e) {
            logger.severe("Erreur récupération floors pour " + serverName + ": " + e.getMessage());
            return createErrorResponse("Erreur communication serveur");
        }
    }

    /**
     * Charge les triggers d'un floor spécifique
     */
    public String loadTriggers(String serverName, String floorId) {
        ServerConnection connection = connectedServers.get(serverName);

        if (connection == null || !connection.isConnected()) {
            return createErrorResponse("Serveur non disponible: " + serverName);
        }

        try {
            Map<String, String> requestData = new HashMap<>();
            requestData.put("floorId", floorId);

            CompletableFuture<String> future = connection.sendRequest("LOAD_TRIGGERS", gson.toJson(requestData));
            return future.get();

        } catch (Exception e) {
            logger.severe("Erreur chargement triggers: " + e.getMessage());
            return createErrorResponse("Erreur chargement triggers");
        }
    }

    /**
     * Sauvegarde les triggers d'un floor
     */
    public boolean saveTriggers(String serverName, String floorId, String triggersData) {
        ServerConnection connection = connectedServers.get(serverName);

        if (connection == null || !connection.isConnected()) {
            return false;
        }

        try {
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("floorId", floorId);
            requestData.put("triggersData", triggersData);

            CompletableFuture<String> future = connection.sendRequest("SAVE_TRIGGERS", gson.toJson(requestData));
            String response = future.get();

            Map<String, Object> responseMap = gson.fromJson(response, Map.class);
            return Boolean.TRUE.equals(responseMap.get("success"));

        } catch (Exception e) {
            logger.severe("Erreur sauvegarde triggers: " + e.getMessage());
            return false;
        }
    }

    /**
     * Génère le JavaScript Blockly pour un serveur
     */
    public String generateBlocklyJS(String serverName) {
        ServerConnection connection = connectedServers.get(serverName);

        if (connection == null || !connection.isConnected()) {
            return "// Erreur: Serveur non disponible - " + serverName;
        }

        try {
            CompletableFuture<String> future = connection.sendRequest("GENERATE_BLOCKLY", "{}");
            String response = future.get();

            Map<String, Object> responseMap = gson.fromJson(response, Map.class);
            return (String) responseMap.get("javascript");

        } catch (Exception e) {
            logger.severe("Erreur génération Blockly: " + e.getMessage());
            return "// Erreur génération Blockly: " + e.getMessage();
        }
    }

    /**
     * Ping tous les serveurs connectés
     */
    public void pingAllServers() {
        connectedServers.forEach((name, connection) -> {
            try {
                connection.ping();
            } catch (Exception e) {
                logger.warning("Ping failed pour " + name + ": " + e.getMessage());
            }
        });
    }

    /**
     * Retourne le nombre de serveurs connectés
     */
    public int getConnectedServersCount() {
        return (int) connectedServers.values().stream()
                .filter(ServerConnection::isConnected)
                .count();
    }

    private String createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        return gson.toJson(error);
    }
}