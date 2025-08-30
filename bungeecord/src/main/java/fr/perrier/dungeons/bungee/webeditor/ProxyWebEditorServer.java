package fr.perrier.dungeons.bungee.webeditor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import fr.perrier.dungeons.bungee.NextDungeonBungee;
import fr.perrier.dungeons.bungee.messaging.SpigotCommunicationService;
import fr.perrier.dungeons.bungee.webeditor.EditorSessionManager.EditorSession;
import lombok.Getter;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Serveur web centralisé pour l'éditeur Blockly sur le proxy
 * Remplace les multiples serveurs web des serveurs Spigot
 */
public class ProxyWebEditorServer {

    private static final int PORT = 8080;
    
    private HttpServer server;
    private final Gson gson;
    @Getter
    private final EditorSessionManager sessionManager;
    private final SpigotCommunicationService communicationService;

    public ProxyWebEditorServer() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.sessionManager = new EditorSessionManager();
        this.communicationService = new SpigotCommunicationService();
    }

    /**
     * Démarre le serveur web centralisé
     */
    public boolean startServer() {
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // Routes API avec pattern /{floorId-uuid}/api/*
            server.createContext("/", new RouteHandler());
            
            // API proxy pour communication avec les serveurs Spigot
            server.createContext("/proxy-api/", new ProxyApiHandler());

            server.setExecutor(Executors.newFixedThreadPool(8));
            server.start();

            NextDungeonBungee.getInstance().getLogger().info("🌐 Serveur web centralisé démarré sur http://localhost:" + PORT);
            return true;

        } catch (IOException e) {
            NextDungeonBungee.getInstance().getLogger().severe("Erreur lors du démarrage du serveur web: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Arrête le serveur web
     */
    public void stopServer() {
        if (server != null) {
            server.stop(1);
            NextDungeonBungee.getInstance().getLogger().info("🛑 Serveur web centralisé arrêté");
        }
    }

    /**
     * Handler principal qui route les requêtes selon l'URL
     */
    private class RouteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            
            // Parse l'URL: /{floorId-uuid}/api/{endpoint} ou /{floorId-uuid}/editor/
            String[] pathParts = path.split("/");
            
            if (pathParts.length < 2) {
                sendNotFound(exchange);
                return;
            }

            String sessionId = pathParts[1]; // floorId-uuid
            EditorSession session = sessionManager.getSession(sessionId);
            
            if (session == null) {
                sendErrorResponse(exchange, "Session d'édition non trouvée: " + sessionId);
                return;
            }

            if (pathParts.length >= 3 && "api".equals(pathParts[2])) {
                // Requête API
                handleApiRequest(exchange, session, pathParts);
            } else if (pathParts.length >= 3 && "editor".equals(pathParts[2])) {
                // Interface éditeur
                handleEditorRequest(exchange, session, pathParts);
            } else {
                sendNotFound(exchange);
            }
        }
    }

    /**
     * Gère les requêtes API
     */
    private void handleApiRequest(HttpExchange exchange, EditorSession session, String[] pathParts) throws IOException {
        if (pathParts.length < 4) {
            sendNotFound(exchange);
            return;
        }

        String endpoint = pathParts[3];
        
        switch (endpoint) {
            case "triggers" -> handleTriggersRequest(exchange, session);
            case "save" -> handleSaveRequest(exchange, session);
            case "trigger-types" -> handleTriggerTypesRequest(exchange, session);
            case "blockly.js" -> handleBlocklyJsRequest(exchange, session);
            case "floor-info" -> handleFloorInfoRequest(exchange, session);
            default -> sendNotFound(exchange);
        }
    }

    /**
     * Gère les requêtes de l'interface éditeur
     */
    private void handleEditorRequest(HttpExchange exchange, EditorSession session, String[] pathParts) throws IOException {
        String filePath = pathParts.length > 3 ? pathParts[3] : "index.html";
        
        if ("".equals(filePath) || "/".equals(filePath)) {
            filePath = "index.html";
        }

        serveStaticFile(exchange, filePath);
    }

    /**
     * Charge les triggers via Redis
     */
    private void handleTriggersRequest(HttpExchange exchange, EditorSession session) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }

        try {
            String triggersJson = communicationService.loadTriggers(session.getSpigotServer(), session.getDungeonName(), session.getFloorId());
            sendJsonResponse(exchange, triggersJson);
        } catch (Exception e) {
            sendErrorResponse(exchange, "Erreur lors du chargement des triggers: " + e.getMessage());
        }
    }

    /**
     * Sauvegarde les triggers via Redis
     */
    private void handleSaveRequest(HttpExchange exchange, EditorSession session) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }

        try {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            boolean success = communicationService.saveTriggers(session.getSpigotServer(), session.getDungeonName(), session.getFloorId(), requestBody, session.getEditorUuid());
            
            String responseJson = success ?
                "{\"success\": true, \"message\": \"Triggers sauvegardés avec succès\"}" :
                "{\"success\": false, \"message\": \"Erreur lors de la sauvegarde\"}";
                
            sendJsonResponse(exchange, responseJson);
        } catch (Exception e) {
            sendErrorResponse(exchange, "Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }

    /**
     * Retourne les types de triggers via Redis
     */
    private void handleTriggerTypesRequest(HttpExchange exchange, EditorSession session) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }

        try {
            String typesJson = communicationService.getTriggerTypes(session.getSpigotServer());
            sendJsonResponse(exchange, typesJson);
        } catch (Exception e) {
            sendErrorResponse(exchange, "Erreur lors du chargement des types: " + e.getMessage());
        }
    }

    /**
     * Génère le JavaScript Blockly via Redis
     */
    private void handleBlocklyJsRequest(HttpExchange exchange, EditorSession session) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }

        try {
            String blocklyJs = communicationService.generateBlocklyJs(session.getSpigotServer(), session.getEditorUuid());
            
            exchange.getResponseHeaders().set("Content-Type", "application/javascript; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, blocklyJs.getBytes(StandardCharsets.UTF_8).length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(blocklyJs.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            sendErrorResponse(exchange, "Erreur lors de la génération JavaScript: " + e.getMessage());
        }
    }

    /**
     * Retourne les informations du floor via Redis
     */
    private void handleFloorInfoRequest(HttpExchange exchange, EditorSession session) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }

        try {
            String floorInfoJson = communicationService.getFloorInfo(session.getSpigotServer(), session.getDungeonName(), session.getFloorId(), session.getEditorName());
            sendJsonResponse(exchange, floorInfoJson);
        } catch (Exception e) {
            sendErrorResponse(exchange, "Erreur lors du chargement des informations: " + e.getMessage());
        }
    }

    /**
     * Sert les fichiers statiques (HTML, CSS, JS)
     */
    private void serveStaticFile(HttpExchange exchange, String fileName) throws IOException {
        InputStream is = getClass().getResourceAsStream("/webserver/" + fileName);

        if (is != null) {
            try {
                byte[] content = is.readAllBytes();
                String contentType = getContentType(fileName);

                exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, content.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(content);
                }
            } finally {
                is.close();
            }
        } else {
            sendNotFound(exchange);
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

    private void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(405, 0);
        exchange.getResponseBody().close();
    }

    private void sendNotFound(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(404, 0);
        exchange.getResponseBody().close();
    }

    private String getContentType(String fileName) {
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".css")) return "text/css";
        if (fileName.endsWith(".js")) return "application/javascript";
        if (fileName.endsWith(".json")) return "application/json";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".ico")) return "image/x-icon";
        return "text/plain";
    }

    /**
     * Handler pour les requêtes API du proxy (communication avec Spigot)
     */
    private class ProxyApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            
            if (path.startsWith("/proxy-api/session")) {
                handleSessionRequest(exchange);
            } else {
                sendNotFound(exchange);
            }
        }

        private void handleSessionRequest(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }

            try {
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                JsonObject request = gson.fromJson(requestBody, JsonObject.class);
                
                String action = request.get("action").getAsString();
                JsonObject response = new JsonObject();
                
                switch (action) {
                    case "create_session" -> {
                        String dungeonName = request.get("dungeonName").getAsString();
                        String floorId = request.get("floorId").getAsString();
                        String playerUuid = request.get("playerUuid").getAsString();
                        String playerName = request.get("playerName").getAsString();
                        String spigotServer = request.get("spigotServer").getAsString();
                        
                        String sessionId = sessionManager.createSessionFromProxy(
                            dungeonName, floorId, UUID.fromString(playerUuid), playerName, spigotServer
                        );
                        
                        response.addProperty("success", true);
                        response.addProperty("sessionId", sessionId);
                        response.addProperty("url", "http://localhost:8080/" + sessionId + "/editor/");
                        
                        NextDungeonBungee.getInstance().getLogger().info("✅ Session créée: " + sessionId + " pour " + playerName);
                    }
                    case "stop_session" -> {
                        String sessionId = request.get("sessionId").getAsString();
                        boolean success = sessionManager.removeSession(sessionId);
                        
                        response.addProperty("success", success);
                        response.addProperty("message", success ? "Session fermée" : "Session non trouvée");
                        
                        NextDungeonBungee.getInstance().getLogger().info("🛑 Session fermée: " + sessionId);
                    }
                    default -> {
                        response.addProperty("success", false);
                        response.addProperty("error", "Action inconnue: " + action);
                    }
                }
                
                sendJsonResponse(exchange, gson.toJson(response));
                
            } catch (Exception e) {
                NextDungeonBungee.getInstance().getLogger().severe("Erreur API proxy: " + e.getMessage());
                sendErrorResponse(exchange, "Erreur traitement requête: " + e.getMessage());
            }
        }
    }
}