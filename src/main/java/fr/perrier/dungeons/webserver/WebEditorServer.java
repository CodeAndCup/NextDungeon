package fr.perrier.dungeons.webserver;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.manager.DungeonFileManager;
import fr.perrier.dungeons.trigger.Trigger;
import fr.perrier.dungeons.trigger.impl.DebugTrigger;
import fr.perrier.dungeons.trigger.impl.MythicMobKillTrigger;
import fr.perrier.dungeons.trigger.impl.LocationTrigger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class WebEditorServer {
    private static final int PORT = 8080;

    private HttpServer server;
    private final Gson gson;
    private String currentDungeon;
    private String currentFloor;

    public WebEditorServer() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    /**
     * Démarre le serveur web pour éditer un donjon spécifique
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

            // Interface web statique
            server.createContext("/", new StaticFileHandler());

            server.setExecutor(Executors.newFixedThreadPool(4));
            server.start();

            Main.getInstance().getLogger().info("Serveur web démarré sur http://localhost:" + PORT);
            Main.getInstance().getLogger().info("Édition du donjon: " + dungeonName + " floor " + floorId);
            return true;

        } catch (IOException e) {
            Main.getInstance().getLogger().severe("Erreur lors du démarrage du serveur: " + e.getMessage());
            return false;
        }
    }

    /**
     * Arrête le serveur web
     */
    public void stopServer() {
        if (server != null) {
            server.stop(0);
            Main.getInstance().getLogger().info("Serveur web arrêté");
        }
    }

    /**
     * Handler pour les triggers API
     */
    private class TriggersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                handleGetTriggers(exchange);
            } else {
                sendMethodNotAllowed(exchange);
            }
        }

        private void handleGetTriggers(HttpExchange exchange) throws IOException {
            try {
                List<Trigger> triggers = DungeonFileManager.loadTriggers(currentDungeon, currentFloor);
                String json = gson.toJson(triggers);
                byte[] jsonBytes = json.getBytes("UTF-8");

                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, jsonBytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonBytes);
                    os.flush();
                }

                Main.getInstance().getLogger().info("Triggers envoyés: " + triggers.size() + " triggers (" + jsonBytes.length + " bytes)");

            } catch (Exception e) {
                Main.getInstance().getLogger().severe("Erreur lors de la récupération des triggers: " + e.getMessage());
                e.printStackTrace();

                sendErrorResponse(exchange, 500, "Erreur serveur lors de la récupération des triggers");
            }
        }
    }

    /**
     * Handler pour sauvegarder les triggers
     */
    private class SaveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                handleSaveTriggers(exchange);
            } else if ("OPTIONS".equals(exchange.getRequestMethod())) {
                handleCORS(exchange);
            } else {
                sendMethodNotAllowed(exchange);
            }
        }

        private void handleSaveTriggers(HttpExchange exchange) throws IOException {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(exchange.getRequestBody(), "UTF-8"))) {

                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }

                Main.getInstance().getLogger().info("Données reçues pour sauvegarde: " + body.toString());

                // TODO: Parser le JSON des triggers depuis Blockly
                // et les convertir en objets Trigger

                String successJson = "{\"status\":\"success\",\"message\":\"Triggers sauvegardés\"}";
                byte[] responseBytes = successJson.getBytes("UTF-8");

                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, responseBytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                    os.flush();
                }

            } catch (Exception e) {
                Main.getInstance().getLogger().severe("Erreur lors de la sauvegarde: " + e.getMessage());
                e.printStackTrace();
                sendErrorResponse(exchange, 500, "Erreur lors de la sauvegarde");
            }
        }

        private void handleCORS(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, 0);
            exchange.getResponseBody().close();
        }
    }

    /**
     * Méthodes utilitaires pour les réponses HTTP
     */
    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        String errorJson = "{\"error\":\"" + message.replace("\"", "'") + "\"}";
        byte[] errorBytes = errorJson.getBytes("UTF-8");

        try {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(statusCode, errorBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorBytes);
                os.flush();
            }
        } catch (IOException e) {
            Main.getInstance().getLogger().severe("Erreur lors de l'envoi de la réponse d'erreur: " + e.getMessage());
        }
    }

    private void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(405, 0);
        exchange.getResponseBody().close();
    }

    /**
     * Handler pour les types de triggers disponibles
     */
    /**
     * Handler pour les types de triggers disponibles
     */
    private class TriggerTypesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Main.getInstance().getLogger().info("Demande des types de triggers recue");

                // Création manuelle des configurations pour éviter les problèmes de sérialisation
                Map<String, Object> triggerTypes = new HashMap<>();

                // MythicMob Kill Trigger
                Map<String, Object> mythicMobConfig = new HashMap<>();
                mythicMobConfig.put("type", "mythicmob_kill");
                mythicMobConfig.put("color", "#FF6B6B");
                mythicMobConfig.put("icon", "skull");

                Map<String, Object> mythicMobFields = new HashMap<>();
                mythicMobFields.put("mob_internal_name", Map.of("type", "text", "label", "Nom interne du mob"));
                mythicMobFields.put("required_kills", Map.of("type", "number", "label", "Nombre de kills requis", "default", 1));
                mythicMobFields.put("boss_name", Map.of("type", "text", "label", "Nom du boss (optionnel)"));
                mythicMobConfig.put("fields", mythicMobFields);

                triggerTypes.put("mythicmob_kill", mythicMobConfig);

                // Location Trigger
                Map<String, Object> locationConfig = new HashMap<>();
                locationConfig.put("type", "location");
                locationConfig.put("color", "#4ECDC4");
                locationConfig.put("icon", "map-marker");

                Map<String, Object> locationFields = new HashMap<>();
                locationFields.put("x", Map.of("type", "number", "label", "Coordonnée X"));
                locationFields.put("y", Map.of("type", "number", "label", "Coordonnée Y"));
                locationFields.put("z", Map.of("type", "number", "label", "Coordonnée Z"));
                locationFields.put("radius", Map.of("type", "number", "label", "Rayon", "default", 2.0));
                locationConfig.put("fields", locationFields);

                triggerTypes.put("location", locationConfig);

                // Debug Chat Trigger
                Map<String, Object> debugConfig = new HashMap<>();
                debugConfig.put("type", "debug_chat");
                debugConfig.put("color", "#9B59B6");
                debugConfig.put("icon", "bug");
                debugConfig.put("category", "debug");

                Map<String, Object> debugFields = new HashMap<>();
                debugFields.put("trigger_message", Map.of(
                        "type", "text",
                        "label", "Message déclencheur",
                        "default", "test"
                ));
                debugFields.put("case_sensitive", Map.of(
                        "type", "checkbox",
                        "label", "Sensible à la casse",
                        "default", false
                ));
                debugFields.put("exact_match", Map.of(
                        "type", "checkbox",
                        "label", "Correspondance exacte",
                        "default", true
                ));
                debugConfig.put("fields", debugFields);

                triggerTypes.put("debug_chat", debugConfig);

                // Sérialisation en JSON
                String json = gson.toJson(triggerTypes);
                byte[] jsonBytes = json.getBytes("UTF-8");

                Main.getInstance().getLogger().info("JSON généré (" + jsonBytes.length + " bytes): " +
                        json.substring(0, Math.min(json.length(), 200)) + "...");

                // Configuration des headers HTTP
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

                // Envoi de la réponse avec la taille correcte
                exchange.sendResponseHeaders(200, jsonBytes.length);

                // Écriture des données
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonBytes);
                    os.flush();
                }

                Main.getInstance().getLogger().info("Types de triggers envoyés avec succès (" + jsonBytes.length + " bytes)");

            } catch (Exception e) {
                Main.getInstance().getLogger().severe("Erreur lors de la génération des types de triggers: " + e.getMessage());
                e.printStackTrace();

                // Réponse d'erreur simple
                String errorJson = "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
                byte[] errorBytes = errorJson.getBytes("UTF-8");

                try {
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    exchange.sendResponseHeaders(500, errorBytes.length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(errorBytes);
                        os.flush();
                    }
                } catch (IOException ioException) {
                    Main.getInstance().getLogger().severe("Erreur lors de l'envoi de la réponse d'erreur: " + ioException.getMessage());
                }
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
            Main.getInstance().getLogger().info("Demande de fichier statique: " + path);

            if ("/".equals(path)) {
                path = "/index.html";
            }

            // Essayer de lire depuis les resources
            InputStream is = getClass().getResourceAsStream("/webserver" + path);
            if (is != null) {
                try {
                    byte[] content = is.readAllBytes();

                    String contentType = getContentType(path);
                    exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
                    exchange.sendResponseHeaders(200, content.length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(content);
                        os.flush();
                    }
                    Main.getInstance().getLogger().info("Fichier servi: " + path + " (" + content.length + " bytes)");
                } finally {
                    is.close();
                }
            } else {
                // Fichier non trouvé, servir l'HTML par défaut
                if (path.equals("/index.html")) {
                    String defaultHtml = getDefaultHtml();
                    byte[] htmlBytes = defaultHtml.getBytes("UTF-8");

                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, htmlBytes.length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(htmlBytes);
                        os.flush();
                    }
                    Main.getInstance().getLogger().info("HTML par défaut servi (" + htmlBytes.length + " bytes)");
                } else {
                    Main.getInstance().getLogger().warning("Fichier non trouvé: " + path);
                    exchange.sendResponseHeaders(404, 0);
                    exchange.getResponseBody().close();
                }
            }
        }

        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".js")) return "application/javascript";
            return "text/plain";
        }

        private String getDefaultHtml() {
            return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Éditeur de Triggers - Chargement...</title>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial; text-align: center; padding: 50px; }
                    .loading { color: #666; }
                </style>
            </head>
            <body>
                <h1>🏰 Éditeur de Triggers</h1>
                <p class="loading">Interface en cours de chargement...</p>
                <p>Si cette page ne se charge pas, vérifiez que les fichiers web sont présents dans les resources.</p>
                <script>
                    // Test de l'API
                    fetch('/api/trigger-types')
                        .then(response => response.json())
                        .then(data => {
                            console.log('API fonctionnelle:', data);
                            document.querySelector('.loading').innerHTML = '✅ Serveur API opérationnel!';
                        })
                        .catch(error => {
                            console.error('Erreur API:', error);
                            document.querySelector('.loading').innerHTML = '❌ Erreur API: ' + error.message;
                        });
                </script>
            </body>
            </html>
            """;
        }
    }
}
