package fr.perrier.dungeons.spigot.webserver;

import fr.perrier.dungeons.spigot.webserver.blockly.BlocklyJavaScriptGenerator;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.manager.TriggerSaveManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Serveur web pour l'éditeur Blockly de triggers/actions
 * Recodé entièrement pour éviter les erreurs d'implémentation
 * 
 * @deprecated Cette classe est remplacée par le système proxy centralisé.
 * Voir ProxyWebEditorServer et DungeonWebEditorManager pour la nouvelle implémentation.
 */
@Deprecated
public class WebEditorServer {

    private static final int PORT = 8080;

    private HttpServer server;
    private final Gson gson;
    private final TriggerSaveManager triggerSaveManager;
    @Getter
    private final Player currentEditor;
    private final BlocklyJavaScriptGenerator blocklyGenerator;

    @Getter
    private String currentDungeon;
    @Getter
    private String currentFloor;

    public WebEditorServer(Player editor) {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.triggerSaveManager = new TriggerSaveManager();
        this.currentEditor = editor;
        this.blocklyGenerator = new BlocklyJavaScriptGenerator();
    }

    /**
     * Démarre le serveur pour éditer un floor spécifique
     */
    public boolean startServer(String dungeonName, String floorId) {
        try {
            this.currentDungeon = dungeonName;
            this.currentFloor = floorId;

            server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // Routes API
            server.createContext("/api/triggers", new TriggersHandler());
            server.createContext("/api/save", new SaveHandler());
            server.createContext("/api/trigger-types", new TriggerTypesHandler());
            server.createContext("/api/blockly.js", new BlocklyGeneratorHandler());
            server.createContext("/api/floor-info", new FloorInfoHandler());

            // Interface web statique
            server.createContext("/", new StaticFileHandler());

            server.setExecutor(Executors.newFixedThreadPool(4));
            server.start();

            Main.getInstance().getLogger().info("🌐 Serveur web démarré sur http://localhost:" + PORT);
            Main.getInstance().getLogger().info("📝 Édition du donjon: " + dungeonName + " floor " + floorId);
            return true;

        } catch (IOException e) {
            Main.getInstance().getLogger().severe("&cAn error occurred white start the web server: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Handler pour l'auto-génération JavaScript des blocs Blockly
     */
    private class BlocklyGeneratorHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }

            try {
                Main.getInstance().getLogger().info("🔧 Auto-génération des blocs Blockly...");

                String blocklyJs = blocklyGenerator.generateJavaScript(currentEditor);
                byte[] jsBytes = blocklyJs.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set("Content-Type", "application/javascript; charset=UTF-8");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                exchange.sendResponseHeaders(200, jsBytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsBytes);
                }

                Main.getInstance().getLogger().info("✅ JavaScript généré (" + jsBytes.length + " bytes)");

            } catch (Exception e) {
                Main.getInstance().getLogger().severe("&c❌ Erreur génération JS: " + e.getMessage());
                e.printStackTrace();
                sendErrorResponse(exchange, "Erreur lors de la génération JavaScript");
            }
        }
    }

    /**
     * Handler pour charger les triggers du floor actuel
     */
    private class TriggersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }

            try {
                Main.getInstance().getLogger().info("📥 Chargement des triggers pour " + currentFloor);

                // Utiliser votre TriggerSaveManager existant
                String triggersJson = triggerSaveManager.loadTriggersAsJson(currentDungeon, currentFloor);
                byte[] jsonBytes = triggersJson.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, jsonBytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonBytes);
                }

                Main.getInstance().getLogger().info("✅ Triggers chargés pour " + currentFloor);

            } catch (Exception e) {
                Main.getInstance().getLogger().severe("&c❌ Erreur chargement triggers: " + e.getMessage());
                e.printStackTrace();
                sendErrorResponse(exchange, "Erreur lors du chargement des triggers");
            }
        }
    }

    /**
     * Handler pour sauvegarder les triggers du floor actuel
     */
    private class SaveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }

            try {
                Main.getInstance().getLogger().info("💾 Sauvegarde des triggers pour " + currentFloor);

                // Lire les données JSON du client
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

                // Utiliser votre TriggerSaveManager existant
                boolean success = triggerSaveManager.saveTriggers(currentDungeon, currentFloor, requestBody, currentEditor);

                String responseJson = success ?
                        "{\"success\": true, \"message\": \"Triggers sauvegardés avec succès pour " + currentFloor + "\"}" :
                        "{\"success\": false, \"message\": \"Erreur lors de la sauvegarde\"}";

                byte[] responseBytes = responseJson.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(success ? 200 : 500, responseBytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }

                if (success) {
                    Main.getInstance().getLogger().info("✅ Triggers sauvegardés pour " + currentFloor);
                    currentEditor.sendMessage("§a✅ Triggers sauvegardés avec succès!");
                } else {
                    Main.getInstance().getLogger().warning("&e❌ Échec sauvegarde " + currentFloor);
                    currentEditor.sendMessage("§c❌ Erreur lors de la sauvegarde!");
                }

            } catch (Exception e) {
                Main.getInstance().getLogger().severe("&c❌ Erreur sauvegarde: " + e.getMessage());
                e.printStackTrace();
                sendErrorResponse(exchange, "Erreur lors de la sauvegarde");
            }
        }
    }

    /**
     * Handler pour les types de triggers disponibles
     */
    private class TriggerTypesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }

            try {
                // Retourner les types de triggers disponibles
                String typesJson = """
                {
                    "success": true,
                    "types": [
                        {
                            "id": "region_trigger",
                            "name": "Region Trigger",
                            "description": "Se déclenche quand un joueur entre dans une région",
                            "category": "Location"
                        },
                        {
                            "id": "debug_chat",
                            "name": "Debug Chat",
                            "description": "Se déclenche sur un message de chat (debug)",
                            "category": "Debug"
                        }
                    ]
                }
                """;

                byte[] jsonBytes = typesJson.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, jsonBytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonBytes);
                }

            } catch (Exception e) {
                Main.getInstance().getLogger().severe("&c❌ Erreur types triggers: " + e.getMessage());
                sendErrorResponse(exchange, "Erreur lors du chargement des types");
            }
        }
    }

    /**
     * Handler pour les informations du floor actuel
     */
    private class FloorInfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendMethodNotAllowed(exchange);
                return;
            }

            try {
                Floor floor = Floor.getFloor(currentFloor);
                String floorName = floor != null ? floor.getName() : "Inconnu";

                String infoJson = String.format("""
                {
                    "success": true,
                    "dungeon": "%s",
                    "floorId": "%s",
                    "floorName": "%s",
                    "editor": "%s"
                }
                """, currentDungeon, currentFloor, floorName, currentEditor.getName());

                byte[] jsonBytes = infoJson.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, jsonBytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonBytes);
                }

            } catch (Exception e) {
                sendErrorResponse(exchange, "Erreur lors du chargement des informations du floor");
            }
        }
    }

    /**
     * Handler pour les fichiers statiques (HTML, CSS, JS)
     */
    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();

            if ("/".equals(path)) {
                path = "/index.html";
            }

            // Charger depuis resources/webserver/
            InputStream is = getClass().getResourceAsStream("/webserver" + path);

            if (is != null) {
                try {
                    byte[] content = is.readAllBytes();
                    String contentType = getContentType(path);

                    exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    exchange.sendResponseHeaders(200, content.length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(content);
                    }

                    Main.getInstance().getLogger().info("📄 Fichier servi: " + path);

                } finally {
                    is.close();
                }
            } else {
                // Fichier non trouvé
                Main.getInstance().getLogger().warning("&e❓ Fichier non trouvé: /webserver" + path);

                if ("/index.html".equals(path)) {
                    // Créer une page de fallback
                    String fallbackHtml = createFallbackHtml();
                    byte[] htmlBytes = fallbackHtml.getBytes(StandardCharsets.UTF_8);

                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, htmlBytes.length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(htmlBytes);
                    }
                } else {
                    exchange.sendResponseHeaders(404, 0);
                    exchange.getResponseBody().close();
                }
            }
        }

        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".js")) return "application/javascript";
            if (path.endsWith(".json")) return "application/json";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
            if (path.endsWith(".ico")) return "image/x-icon";
            return "text/plain";
        }

        private String createFallbackHtml() {
            return String.format("""
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8">
                    <title>Éditeur Blockly - %s</title>
                    <style>
                        body { 
                            font-family: 'Segoe UI', Arial, sans-serif; 
                            margin: 0; padding: 20px; 
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); 
                            color: white; text-align: center; 
                        }
                        .container { 
                            max-width: 800px; margin: 0 auto; 
                            background: rgba(255,255,255,0.1); 
                            padding: 30px; border-radius: 15px; 
                            backdrop-filter: blur(10px);
                            box-shadow: 0 20px 40px rgba(0,0,0,0.1);
                        }
                        .status { 
                            background: rgba(76, 175, 80, 0.2); 
                            padding: 15px; margin: 15px 0; 
                            border-radius: 8px; border-left: 4px solid #4caf50; 
                        }
                        .api-list { 
                            text-align: left; background: rgba(0,0,0,0.2); 
                            padding: 20px; border-radius: 10px; margin: 20px 0; 
                        }
                        .api-endpoint { 
                            font-family: 'Courier New', monospace; 
                            background: rgba(255,255,255,0.1); 
                            padding: 8px; margin: 5px 0; 
                            border-radius: 5px; 
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>🏰 Éditeur Blockly - Dungeons</h1>
                        
                        <div class="status">
                            <h3>✅ Serveur actif</h3>
                            <p><strong>Donjon:</strong> %s</p>
                            <p><strong>Floor:</strong> %s</p>
                            <p><strong>Éditeur:</strong> %s</p>
                        </div>
                        
                        <div class="api-list">
                            <h3>🔧 APIs disponibles</h3>
                            <div class="api-endpoint"><strong>GET</strong> /api/blockly.js - Blocs auto-générés</div>
                            <div class="api-endpoint"><strong>GET</strong> /api/triggers - Charger les triggers</div>
                            <div class="api-endpoint"><strong>POST</strong> /api/save - Sauvegarder les triggers</div>
                            <div class="api-endpoint"><strong>GET</strong> /api/trigger-types - Types disponibles</div>
                            <div class="api-endpoint"><strong>GET</strong> /api/floor-info - Infos du floor</div>
                        </div>
                        
                        <p>⚠️ Placez votre fichier <code>index.html</code> dans <code>src/main/resources/webserver/</code></p>
                        
                        <script>
                            // Test automatique des APIs
                            console.log('🔍 Test des APIs...');
                            
                            fetch('/api/floor-info')
                                .then(r => r.json())
                                .then(data => console.log('Floor info:', data))
                                .catch(e => console.error('Erreur floor-info:', e));
                                
                            fetch('/api/trigger-types')
                                .then(r => r.json())
                                .then(data => console.log('Trigger types:', data))
                                .catch(e => console.error('Erreur trigger-types:', e));
                        </script>
                    </div>
                </body>
                </html>
                """, currentFloor, currentDungeon, currentFloor, currentEditor.getName());
        }
    }

    /**
     * Arrête le serveur web
     */
    public void stopServer() {
        if (server != null) {
            server.stop(1);
            Main.getInstance().getLogger().info("🛑 Serveur web arrêté pour " + currentFloor);
        }
    }

    // Méthodes utilitaires
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
}