package fr.perrier.dungeons.spigot.webeditor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import fr.perrier.dungeons.spigot.Main;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Mini serveur HTTP pour répondre aux requêtes du proxy
 * Écoute sur un port local pour communication proxy <-> spigot
 */
public class SpigotProxyBridge {

    private static final int BRIDGE_PORT = 8081;
    private HttpServer bridgeServer;
    private final ProxyEditorMessageHandler messageHandler;
    private final Gson gson = new Gson();

    public SpigotProxyBridge() {
        this.messageHandler = new ProxyEditorMessageHandler();
    }

    /**
     * Démarre le pont de communication avec le proxy
     */
    public boolean startBridge() {
        try {
            bridgeServer = HttpServer.create(new InetSocketAddress(BRIDGE_PORT), 0);
            bridgeServer.createContext("/spigot-api/", new SpigotApiHandler());
            bridgeServer.setExecutor(Executors.newFixedThreadPool(4));
            bridgeServer.start();

            Main.getInstance().getLogger().info("🌉 Pont de communication proxy démarré sur port " + BRIDGE_PORT);
            return true;
        } catch (IOException e) {
            Main.getInstance().getLogger().severe("Erreur démarrage pont proxy: " + e.getMessage());
            return false;
        }
    }

    /**
     * Arrête le pont de communication
     */
    public void stopBridge() {
        if (bridgeServer != null) {
            bridgeServer.stop(1);
            Main.getInstance().getLogger().info("🛑 Pont de communication proxy arrêté");
        }
    }

    /**
     * Handler pour les requêtes API du proxy
     */
    private class SpigotApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendErrorResponse(exchange, "Seules les requêtes POST sont supportées");
                return;
            }

            try {
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                JsonObject request = gson.fromJson(requestBody, JsonObject.class);
                
                String type = request.get("type").getAsString();
                String response = processRequest(type, request);
                
                // Déterminer le type de contenu selon le type de requête
                if ("GENERATE_BLOCKLY_JS".equals(type)) {
                    sendJavaScriptResponse(exchange, response);
                } else {
                    sendJsonResponse(exchange, response);
                }
                
            } catch (Exception e) {
                Main.getInstance().getLogger().severe("Erreur traitement requête proxy: " + e.getMessage());
                sendErrorResponse(exchange, "Erreur traitement: " + e.getMessage());
            }
        }

        private String processRequest(String type, JsonObject request) {
            try {
                return switch (type) {
                    case "LOAD_TRIGGERS" -> {
                        String dungeonName = request.get("dungeonName").getAsString();
                        String floorId = request.get("floorId").getAsString();
                        yield messageHandler.handleLoadTriggersRequest(dungeonName, floorId);
                    }
                    case "SAVE_TRIGGERS" -> {
                        String dungeonName = request.get("dungeonName").getAsString();
                        String floorId = request.get("floorId").getAsString();
                        String triggersData = request.get("triggersData").getAsString();
                        String editorUuidStr = request.get("editorUuid").getAsString();
                        yield messageHandler.handleSaveTriggersRequest(dungeonName, floorId, triggersData, java.util.UUID.fromString(editorUuidStr));
                    }
                    case "GET_TRIGGER_TYPES" -> messageHandler.handleGetTriggerTypesRequest();
                    case "GENERATE_BLOCKLY_JS" -> {
                        String editorUuidStr = request.get("editorUuid").getAsString();
                        // Pour Blockly JS, retourner directement le JavaScript, pas du JSON
                        yield messageHandler.handleGenerateBlocklyJsRequest(java.util.UUID.fromString(editorUuidStr));
                    }
                    case "GET_FLOOR_INFO" -> {
                        String dungeonName = request.get("dungeonName").getAsString();
                        String floorId = request.get("floorId").getAsString();
                        String editorName = request.get("editorName").getAsString();
                        yield messageHandler.handleGetFloorInfoRequest(dungeonName, floorId, editorName);
                    }
                    default -> createErrorResponse("Type de requête inconnu: " + type);
                };
            } catch (Exception e) {
                Main.getInstance().getLogger().severe("Erreur traitement requête " + type + ": " + e.getMessage());
                return createErrorResponse("Erreur traitement: " + e.getMessage());
            }
        }

        private String createErrorResponse(String message) {
            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("error", message);
            return gson.toJson(error);
        }
    }

    // Méthodes utilitaires
    private void sendJsonResponse(HttpExchange exchange, String json) throws IOException {
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, jsonBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(jsonBytes);
        }
    }

    private void sendJavaScriptResponse(HttpExchange exchange, String javascript) throws IOException {
        byte[] jsBytes = javascript.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/javascript; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.sendResponseHeaders(200, jsBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(jsBytes);
        }
    }

    private void sendErrorResponse(HttpExchange exchange, String message) throws IOException {
        String errorJson = String.format("{\"success\": false, \"error\": \"%s\"}",
                message.replace("\"", "'"));
        byte[] errorBytes = errorJson.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(500, errorBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(errorBytes);
        }
    }
}