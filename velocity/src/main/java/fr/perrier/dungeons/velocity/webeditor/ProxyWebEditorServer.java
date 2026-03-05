package fr.perrier.dungeons.velocity.webeditor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import fr.perrier.dungeons.velocity.NextDungeonVelocity;
import fr.perrier.dungeons.velocity.dashboard.DashboardService;
import fr.perrier.dungeons.velocity.dashboard.DungeonManagementService;
import fr.perrier.dungeons.velocity.messaging.SpigotCommunicationService;
import fr.perrier.dungeons.velocity.webeditor.EditorSessionManager.EditorSession;
import lombok.Getter;
import org.redisson.api.RedissonClient;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * Serveur web centralisé pour l'éditeur Blockly sur le proxy
 * Remplace les multiples serveurs web des serveurs Spigot
 */
public class ProxyWebEditorServer {

    private final int port;
    private HttpServer server;
    private final Gson gson;
    @Getter
    private final EditorSessionManager sessionManager;
    private final SpigotCommunicationService communicationService;
    private DashboardService dashboardService;
    private DungeonManagementService dungeonManagementService;
    private DashboardHandler dashboardHandler;    public ProxyWebEditorServer(int port) {
        this.port = port;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.sessionManager = new EditorSessionManager();
        this.communicationService = new SpigotCommunicationService();
    }

    /**
     * Initialise le service de tableau de bord avec le client Redisson
     */
    public void initializeDashboard(RedissonClient redissonClient) {
        this.dashboardService = new DashboardService(redissonClient, sessionManager);
        String topic = NextDungeonVelocity.getInstance().getConfigManager().getTable("redis").getString("topic", "nextdungeon");
        this.dungeonManagementService = new DungeonManagementService(redissonClient, topic);
        NextDungeonVelocity.getInstance().getLogger().info("✅ Service de tableau de bord initialisé");
    }

    public int getPort() {
        return port;
    }

    /**
     * Démarre le serveur web centralisé
     */
    public boolean startServer() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);

            dashboardHandler = new DashboardHandler();

            // Routes API avec pattern /{floorId-uuid}/api/*
            server.createContext("/", new RouteHandler());
            
            // API proxy pour communication avec les serveurs Spigot
            server.createContext("/proxy-api/", new ProxyApiHandler());

            // Dashboard routes — enregistré après "/" pour avoir priorité
            server.createContext("/dashboard", dashboardHandler);

            server.setExecutor(Executors.newFixedThreadPool(8));
            server.start();

            NextDungeonVelocity.getInstance().getLogger().info("🌐 Centralized web server start on http://localhost:" + port);
            return true;

        } catch (IOException e) {
            NextDungeonVelocity.getInstance().getLogger().error("Error starting web server: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    /**
     * Arrête le serveur web
     */
    public void stopServer() {
        if (server != null) {
            server.stop(1);
            NextDungeonVelocity.getInstance().getLogger().info("🛑 Centralized web server shut down");
        }
    }

    /**
     * Handler principal qui route les requêtes selon l'URL
     */
    private class RouteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            System.out.println("[RouteHandler] " + method + " " + path);
            // Si c'est une requête dashboard qui arrive ici, la rediriger
            if (path.startsWith("/dashboard")) {
                System.out.println("[RouteHandler] !! INTERCEPTED DASHBOARD REQUEST - forwarding to dashboardHandler instance");
                dashboardHandler.handle(exchange);
                return;
            }

            // Parse l'URL: /{floorId-uuid}/api/{endpoint} ou /{floorId-uuid}/editor/
            String[] pathParts = path.split("/");

            if (pathParts.length < 2) {
                sendNotFound(exchange);
                return;
            }

            String sessionId = pathParts[1]; // floorId-uuid
            EditorSession session = sessionManager.getSession(sessionId);

            if (session == null) {
                sendErrorResponse(exchange, "Edit session not found: " + sessionId);
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
            sendErrorResponse(exchange, "Error loading triggers: " + e.getMessage());
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
                "{\"success\": true, \"message\": \"Triggers successfully saved\"}" :
                "{\"success\": false, \"message\": \"Error while saving\"}";
                
            sendJsonResponse(exchange, responseJson);
        } catch (Exception e) {
            sendErrorResponse(exchange, "Error while saving: " + e.getMessage());
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
            sendErrorResponse(exchange, "Error loading types: " + e.getMessage());
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
            sendErrorResponse(exchange, "Error generating JavaScript: " + e.getMessage());
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
            sendErrorResponse(exchange, "Error loading information: " + e.getMessage());
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
                exchange.getResponseHeaders().set("Cache-Control", "no-store, no-cache, must-revalidate");
                exchange.getResponseHeaders().set("Pragma", "no-cache");
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
                        response.addProperty("url", "http://" + getPrivateIp() + ":" + port + "/" + sessionId + "/editor/");

                        NextDungeonVelocity.getInstance().getLogger().info("✅ Session créée: " + sessionId + " pour " + playerName);
                    }
                    case "stop_session" -> {
                        String sessionId = request.get("sessionId").getAsString();
                        boolean success = sessionManager.removeSession(sessionId);
                        
                        response.addProperty("success", success);
                        response.addProperty("message", success ? "Session fermée" : "Session non trouvée");
                        
                        NextDungeonVelocity.getInstance().getLogger().info("🛑 Session closed: " + sessionId);
                    }
                    default -> {
                        response.addProperty("success", false);
                        response.addProperty("error", "Unknown action: " + action);
                    }
                }
                
                sendJsonResponse(exchange, gson.toJson(response));
                
            } catch (Exception e) {
                NextDungeonVelocity.getInstance().getLogger().error("Proxy API error: " + e.getMessage());
                sendErrorResponse(exchange, "Error processing request: " + e.getMessage());
            }
        }
    }

    private String getPrivateIp() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            if (iface.isLoopback() || !iface.isUp()) continue;
            for (InterfaceAddress addr : iface.getInterfaceAddresses()) {
                InetAddress inetAddr = addr.getAddress();
                if (inetAddr instanceof Inet4Address && !inetAddr.isLoopbackAddress() && inetAddr.isSiteLocalAddress()) {
                    return inetAddr.getHostAddress();
                }
            }
        }
        return null;
    }

    /**
     * Handler pour le tableau de bord
     */
    private class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if (dashboardService == null) {
                sendErrorResponse(exchange, "Dashboard service not initialized");
                return;
            }

            // Dashboard UI
            if (path.equals("/dashboard") || path.equals("/dashboard/")) {
                serveStaticFile(exchange, "dashboard.html");
                return;
            }

            // Dungeon editor page
            if (path.equals("/dashboard/dungeons") || path.equals("/dashboard/dungeons/")) {
                serveStaticFile(exchange, "dungeons-editor.html");
                return;
            }

            // Dashboard API routes
            if (path.startsWith("/dashboard/api/")) {
                handleDashboardApiRequest(exchange, path);
            } else {
                sendNotFound(exchange);
            }
        }

        private void handleDashboardApiRequest(HttpExchange exchange, String path) throws IOException {
            String method = exchange.getRequestMethod();
            System.out.println("[DashboardAPI] method=" + method + " path=" + path);

            // ── OPTIONS preflight
            if ("OPTIONS".equals(method)) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            // ── Dungeon management endpoints ──────────────────────────
            if ("GET".equals(method) && path.equals("/dashboard/api/dungeons")) {
                sendJsonResponse(exchange, dungeonManagementService.getAllDungeonsJson());
                return;
            }
            if ("GET".equals(method) && path.startsWith("/dashboard/api/dungeons/")) {
                String dungeonId = path.substring("/dashboard/api/dungeons/".length());
                if (dungeonId.contains("/floors")) {
                    dungeonId = dungeonId.substring(0, dungeonId.indexOf("/floors"));
                    sendJsonResponse(exchange, dungeonManagementService.getDungeonJson(dungeonId));
                } else if (!dungeonId.isEmpty() && !dungeonId.contains("/")) {
                    sendJsonResponse(exchange, dungeonManagementService.getDungeonJson(dungeonId));
                } else { sendNotFound(exchange); }
                return;
            }
            if ("POST".equals(method) && path.equals("/dashboard/api/dungeons")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                sendJsonResponse(exchange, dungeonManagementService.createOrUpdateDungeon(body, false));
                return;
            }
            if ("PUT".equals(method) && path.startsWith("/dashboard/api/dungeons/")) {
                String sub = path.substring("/dashboard/api/dungeons/".length());
                if (!sub.contains("/") && !sub.isEmpty()) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    sendJsonResponse(exchange, dungeonManagementService.createOrUpdateDungeon(body, true));
                }
                return;
            }
            // DELETE floors AVANT DELETE dungeon (chemin plus spécifique d'abord)
            if ("DELETE".equals(method) && path.matches("/dashboard/api/dungeons/[^/]+/floors/[^/]+")) {
                String[] parts = path.split("/");
                String dungeonId = parts[4];
                String floorId = parts[6];
                sendJsonResponse(exchange, dungeonManagementService.deleteFloor(dungeonId, floorId));
                return;
            }
            if ("DELETE".equals(method) && path.startsWith("/dashboard/api/dungeons/")) {
                String sub = path.substring("/dashboard/api/dungeons/".length());
                if (!sub.contains("/") && !sub.isEmpty()) {
                    sendJsonResponse(exchange, dungeonManagementService.deleteDungeon(sub));
                }
                return;
            }
            if ("POST".equals(method) && path.matches("/dashboard/api/dungeons/[^/]+/floors")) {
                String dungeonId = path.substring("/dashboard/api/dungeons/".length(), path.lastIndexOf("/floors"));
                String body = new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                sendJsonResponse(exchange, dungeonManagementService.addFloor(dungeonId, body));
                return;
            }
            if ("PUT".equals(method) && path.startsWith("/dashboard/api/floors/")) {
                String floorId = path.substring("/dashboard/api/floors/".length());
                if (!floorId.isEmpty() && !floorId.contains("/")) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    sendJsonResponse(exchange, dungeonManagementService.updateFloor(floorId, body));
                }
                return;
            }

            // Handle POST requests for queue clear
            if ("POST".equals(method) && path.startsWith("/dashboard/api/queue/clear/")) {
                String floorId = path.substring("/dashboard/api/queue/clear/".length());
                if (floorId.isEmpty() || floorId.contains("..") || floorId.contains("/") || floorId.contains("\\")) {
                    sendJsonResponse(exchange, "{\"success\": false, \"error\": \"Invalid floor ID\"}");
                    return;
                }
                String response = dashboardService.clearQueueForFloor(floorId);
                sendJsonResponse(exchange, response);
                return;
            }

            if (!"GET".equals(method)) {
                sendMethodNotAllowed(exchange);
                return;
            }

            try {
                String response = switch (path) {
                    case "/dashboard/api/floors" -> dashboardService.getFloorsJson();
                    case "/dashboard/api/instances" -> dashboardService.getInstancesJson();
                    case "/dashboard/api/sessions" -> dashboardService.getSessionsJson();
                    case "/dashboard/api/stats" -> dashboardService.getStatsJson();
                    case "/dashboard/api/queue" -> dashboardService.getQueueJson();
                    default -> {
                        // Handle /dashboard/api/floor/{floorId}
                        if (path.startsWith("/dashboard/api/floor/")) {
                            String floorId = path.substring("/dashboard/api/floor/".length());
                            // Validate floorId to prevent path traversal
                            if (floorId.isEmpty() || floorId.contains("..") || floorId.contains("/") || floorId.contains("\\")) {
                                yield "{\"success\": false, \"error\": \"Invalid floor ID\"}";
                            }
                            yield dashboardService.getFloorConfigJson(floorId);
                        }
                        yield null;
                    }
                };

                if (response != null) {
                    sendJsonResponse(exchange, response);
                } else {
                    sendNotFound(exchange);
                }
            } catch (Exception e) {
                NextDungeonVelocity.getInstance().getLogger().error("Erreur API dashboard: " + e.getMessage());
                e.printStackTrace(System.err);
                sendErrorResponse(exchange, "Erreur traitement requête: " + e.getMessage());
            }
        }
    }
}

