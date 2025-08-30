package fr.perrier.dungeons.webserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import fr.perrier.dungeons.NextDungeonBungee;
import net.md_5.bungee.config.Configuration;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class DungeonWebServer {

    private HttpServer server;
    private final Gson gson;
    private final ProxyServerManager proxyManager;
    private final Logger logger;
    private final int webPort;

    public DungeonWebServer(Logger logger) {
        this.logger = logger;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.proxyManager = NextDungeonBungee.getInstance().getServerManager();

        Configuration config = NextDungeonBungee.getInstance().getConfiguration();
        this.webPort = config.getInt("webserver.web-port", 8080);
    }

    public boolean start() {
        try {
            Configuration config = NextDungeonBungee.getInstance().getConfiguration();
            String bindAddress = config.getString("webserver.bind-address", "0.0.0.0");

            server = HttpServer.create(new InetSocketAddress(bindAddress, webPort), 0);

            // Routes API
            server.createContext("/api/servers", new ServersHandler());
            server.createContext("/api/floors", new FloorsHandler());
            server.createContext("/api/triggers", new TriggersHandler());
            server.createContext("/api/save", new SaveHandler());
            server.createContext("/api/blockly.js", new BlocklyHandler());
            server.createContext("/api/status", new StatusHandler());

            // Interface web
            server.createContext("/", new StaticHandler());

            server.setExecutor(Executors.newFixedThreadPool(8));
            server.start();

            logger.info("🌐 Serveur web NextDungeon démarré sur " + bindAddress + ":" + webPort);
            return true;

        } catch (IOException e) {
            logger.severe("❌ Erreur démarrage serveur web: " + e.getMessage());
            return false;
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(2);
            logger.info("🛑 Serveur web NextDungeon arrêté");
        }
    }

    // Handler pour lister les serveurs disponibles
    private class ServersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            try {
                String serversJson = proxyManager.getAvailableServers();
                sendJsonResponse(exchange, 200, serversJson);

            } catch (Exception e) {
                logger.severe("Erreur récupération serveurs: " + e.getMessage());
                sendError(exchange, 500, "Erreur serveur");
            }
        }
    }

    // Handler pour les floors d'un serveur spécifique
    private class FloorsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                String serverName = extractParam(query, "server");

                if (serverName == null) {
                    sendError(exchange, 400, "Paramètre 'server' manquant");
                    return;
                }

                String floorsJson = proxyManager.getServerFloors(serverName);
                sendJsonResponse(exchange, 200, floorsJson);

            } catch (Exception e) {
                logger.severe("Erreur récupération floors: " + e.getMessage());
                sendError(exchange, 500, "Erreur serveur");
            }
        }
    }

    // Handler pour charger les triggers
    private class TriggersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                String serverName = extractParam(query, "server");
                String floorId = extractParam(query, "floor");

                if (serverName == null || floorId == null) {
                    sendError(exchange, 400, "Paramètres 'server' et 'floor' requis");
                    return;
                }

                String triggersJson = proxyManager.loadTriggers(serverName, floorId);
                sendJsonResponse(exchange, 200, triggersJson);

            } catch (Exception e) {
                logger.severe("Erreur chargement triggers: " + e.getMessage());
                sendError(exchange, 500, "Erreur chargement triggers");
            }
        }
    }

    // Handler pour sauvegarder les triggers
    private class SaveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                String serverName = extractParam(query, "server");
                String floorId = extractParam(query, "floor");

                if (serverName == null || floorId == null) {
                    sendError(exchange, 400, "Paramètres 'server' et 'floor' requis");
                    return;
                }

                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

                boolean success = proxyManager.saveTriggers(serverName, floorId, requestBody);

                String response = success ?
                        "{\"success\": true, \"message\": \"Triggers sauvegardés avec succès\"}" :
                        "{\"success\": false, \"message\": \"Erreur lors de la sauvegarde\"}";

                sendJsonResponse(exchange, success ? 200 : 500, response);

            } catch (Exception e) {
                logger.severe("Erreur sauvegarde triggers: " + e.getMessage());
                sendError(exchange, 500, "Erreur sauvegarde");
            }
        }
    }

    // Handler pour générer le JavaScript Blockly
    private class BlocklyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                String serverName = extractParam(query, "server");

                if (serverName == null) {
                    sendError(exchange, 400, "Paramètre 'server' manquant");
                    return;
                }

                String blocklyJs = proxyManager.generateBlocklyJS(serverName);

                exchange.getResponseHeaders().set("Content-Type", "application/javascript; charset=UTF-8");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Cache-Control", "no-cache");

                byte[] jsBytes = blocklyJs.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, jsBytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsBytes);
                }

            } catch (Exception e) {
                logger.severe("Erreur génération Blockly: " + e.getMessage());
                sendError(exchange, 500, "Erreur génération JavaScript");
            }
        }
    }

    // Handler pour le statut global
    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            try {
                String statusJson = String.format("""
                    {
                        "success": true,
                        "proxy": {
                            "name": "NextDungeon BungeeCord",
                            "version": "1.0.0",
                            "uptime": %d,
                            "connectedServers": %d
                        },
                        "timestamp": %d
                    }
                    """,
                        System.currentTimeMillis() - NextDungeonBungee.getInstance().getStartTime(),
                        proxyManager.getConnectedServersCount(),
                        System.currentTimeMillis()
                );

                sendJsonResponse(exchange, 200, statusJson);

            } catch (Exception e) {
                logger.severe("Erreur récupération status: " + e.getMessage());
                sendError(exchange, 500, "Erreur serveur");
            }
        }
    }

    // Handler pour les fichiers statiques
    private class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();

            if ("/".equals(path)) {
                path = "/index.html";
            }

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

                } finally {
                    is.close();
                }
            } else {
                if ("/index.html".equals(path)) {
                    String fallbackHtml = createDashboardHtml();
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
            return "text/plain";
        }

        private String createDashboardHtml() {
            return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>🏰 NextDungeon - Dashboard Central</title>
                    <style>
                        * { margin: 0; padding: 0; box-sizing: border-box; }
                        body { 
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            color: white; min-height: 100vh; padding: 20px;
                        }
                        .container { 
                            max-width: 1400px; margin: 0 auto;
                            background: rgba(255,255,255,0.1);
                            padding: 30px; border-radius: 20px;
                            backdrop-filter: blur(15px);
                            box-shadow: 0 25px 45px rgba(0,0,0,0.1);
                        }
                        .header {
                            text-align: center; margin-bottom: 40px;
                            border-bottom: 2px solid rgba(255,255,255,0.2);
                            padding-bottom: 20px;
                        }
                        .header h1 { font-size: 3em; margin-bottom: 10px; }
                        .status-bar {
                            display: flex; justify-content: space-around;
                            margin-bottom: 30px; flex-wrap: wrap;
                        }
                        .status-item {
                            background: rgba(255,255,255,0.1);
                            padding: 15px 25px; border-radius: 10px;
                            text-align: center; min-width: 150px;
                            border: 1px solid rgba(255,255,255,0.2);
                        }
                        .server-grid {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
                            gap: 25px; margin-top: 30px;
                        }
                        .server-card {
                            background: rgba(255,255,255,0.1);
                            border-radius: 15px; padding: 25px;
                            border: 1px solid rgba(255,255,255,0.2);
                            transition: all 0.3s ease;
                        }
                        .server-card:hover {
                            transform: translateY(-5px);
                            box-shadow: 0 15px 30px rgba(0,0,0,0.2);
                        }
                        .server-header {
                            display: flex; justify-content: space-between;
                            align-items: center; margin-bottom: 15px;
                        }
                        .server-name { font-size: 1.3em; font-weight: bold; }
                        .server-status {
                            padding: 5px 12px; border-radius: 20px;
                            font-size: 0.85em; font-weight: bold;
                        }
                        .status-online { background: rgba(76, 175, 80, 0.8); }
                        .status-offline { background: rgba(244, 67, 54, 0.8); }
                        .server-info {
                            display: grid; grid-template-columns: 1fr 1fr;
                            gap: 10px; margin-bottom: 20px;
                            font-size: 0.9em;
                        }
                        .floor-grid {
                            display: grid;
                            grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
                            gap: 10px; margin-top: 15px;
                        }
                        .floor-item {
                            background: rgba(0,0,0,0.3);
                            padding: 12px; border-radius: 8px;
                            text-align: center; cursor: pointer;
                            transition: all 0.3s; font-size: 0.85em;
                        }
                        .floor-item:hover {
                            background: rgba(255,255,255,0.2);
                            transform: scale(1.05);
                        }
                        .btn {
                            background: rgba(103, 58, 183, 0.8);
                            color: white; border: none;
                            padding: 10px 20px; border-radius: 8px;
                            cursor: pointer; transition: all 0.3s;
                            font-size: 0.9em;
                        }
                        .btn:hover { background: rgba(103, 58, 183, 1); }
                        .btn:disabled {
                            background: rgba(128, 128, 128, 0.5);
                            cursor: not-allowed;
                        }
                        .loading {
                            text-align: center; padding: 50px;
                            font-size: 1.2em;
                        }
                        .loading::after {
                            content: '';
                            display: inline-block;
                            width: 20px; height: 20px;
                            border: 2px solid rgba(255,255,255,0.3);
                            border-radius: 50%;
                            border-top-color: white;
                            animation: spin 1s ease-in-out infinite;
                        }
                        @keyframes spin {
                            to { transform: rotate(360deg); }
                        }
                        .error {
                            background: rgba(244, 67, 54, 0.2);
                            border: 1px solid rgba(244, 67, 54, 0.5);
                            padding: 15px; border-radius: 8px;
                            margin: 20px 0;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🏰 NextDungeon</h1>
                            <p>Dashboard Central - Éditeur Web Unifié</p>
                        </div>
                        
                        <div class="status-bar">
                            <div class="status-item">
                                <div style="font-size: 0.8em; opacity: 0.8;">Proxy Status</div>
                                <div id="proxy-status">🟢 En ligne</div>
                            </div>
                            <div class="status-item">
                                <div style="font-size: 0.8em; opacity: 0.8;">Serveurs Connectés</div>
                                <div id="connected-servers">-</div>
                            </div>
                            <div class="status-item">
                                <div style="font-size: 0.8em; opacity: 0.8;">Uptime</div>
                                <div id="uptime">-</div>
                            </div>
                        </div>
                        
                        <div id="servers-container" class="loading">
                            🔄 Chargement des serveurs...
                        </div>
                    </div>
                    
                    <script>
                        let servers = [];
                        let proxyStatus = {};
                        
                        // Charger le statut du proxy
                        async function loadProxyStatus() {
                            try {
                                const response = await fetch('/api/status');
                                const data = await response.json();
                                
                                if (data.success) {
                                    proxyStatus = data.proxy;
                                    updateStatusBar();
                                }
                            } catch (error) {
                                console.error('Erreur chargement statut proxy:', error);
                            }
                        }
                        
                        // Mettre à jour la barre de statut
                        function updateStatusBar() {
                            document.getElementById('connected-servers').textContent = proxyStatus.connectedServers || 0;
                            
                            if (proxyStatus.uptime) {
                                const hours = Math.floor(proxyStatus.uptime / (1000 * 60 * 60));
                                const minutes = Math.floor((proxyStatus.uptime % (1000 * 60 * 60)) / (1000 * 60));
                                document.getElementById('uptime').textContent = `${hours}h ${minutes}m`;
                            }
                        }
                        
                        // Charger les serveurs disponibles
                        async function loadServers() {
                            try {
                                const response = await fetch('/api/servers');
                                const data = await response.json();
                                
                                if (data.success) {
                                    servers = data.servers || [];
                                    renderServers();
                                } else {
                                    throw new Error(data.error || 'Erreur inconnue');
                                }
                            } catch (error) {
                                document.getElementById('servers-container').innerHTML = 
                                    '<div class="error">❌ Erreur chargement serveurs: ' + error.message + '</div>';
                            }
                        }
                        
                        // Afficher les serveurs
                        function renderServers() {
                            const container = document.getElementById('servers-container');
                            
                            if (servers.length === 0) {
                                container.innerHTML = '<div class="loading">📭 Aucun serveur connecté</div>';
                                return;
                            }
                            
                            let html = '<div class="server-grid">';
                            servers.forEach(server => {
                                const statusClass = server.online ? 'status-online' : 'status-offline';
                                const statusText = server.online ? '🟢 En ligne' : '🔴 Hors ligne';
                                
                                html += `
                                    <div class="server-card">
                                        <div class="server-header">
                                            <div class="server-name">🖥️ ${server.name}</div>
                                            <div class="server-status ${statusClass}">${statusText}</div>
                                        </div>
                                        
                                        <div class="server-info">
                                            <div><strong>Joueurs:</strong> ${server.playerCount}/${server.maxPlayers}</div>
                                            <div><strong>Ping:</strong> ${formatLastPing(server.lastPing)}</div>
                                        </div>
                                        
                                        <div id="floors-${server.name}" class="floor-grid">
                                            <div style="grid-column: 1/-1; text-align: center; opacity: 0.7; font-size: 0.8em;">
                                                Cliquez sur "Charger" pour voir les floors
                                            </div>
                                        </div>
                                        
                                        <button class="btn" onclick="loadFloors('${server.name}')" 
                                                ${server.online ? '' : 'disabled'}>
                                            📋 Charger les floors
                                        </button>
                                    </div>
                                `;
                            });
                            html += '</div>';
                            
                            container.innerHTML = html;
                        }
                        
                        // Charger les floors d'un serveur
                        async function loadFloors(serverName) {
                            const floorsContainer = document.getElementById(`floors-${serverName}`);
                            floorsContainer.innerHTML = '<div style="grid-column: 1/-1; text-align: center;">🔄 Chargement...</div>';
                            
                            try {
                                const response = await fetch(`/api/floors?server=${serverName}`);
                                const data = await response.json();
                                
                                if (data.success && data.floors && data.floors.length > 0) {
                                    let html = '';
                                    data.floors.forEach(floor => {
                                        html += `
                                            <div class="floor-item" onclick="openEditor('${serverName}', '${floor.id}')">
                                                <div style="font-weight: bold;">${floor.name}</div>
                                                <div style="font-size: 0.7em; opacity: 0.8;">ID: ${floor.id}</div>
                                                <div style="font-size: 0.7em; opacity: 0.8;">Triggers: ${floor.triggerCount || 0}</div>
                                            </div>
                                        `;
                                    });
                                    floorsContainer.innerHTML = html;
                                } else {
                                    floorsContainer.innerHTML = '<div style="grid-column: 1/-1; text-align: center; opacity: 0.7;">📭 Aucun floor trouvé</div>';
                                }
                                
                            } catch (error) {
                                floorsContainer.innerHTML = `<div style="grid-column: 1/-1; text-align: center; color: #f44336;">❌ Erreur: ${error.message}</div>`;
                            }
                        }
                        
                        // Ouvrir l'éditeur pour un floor spécifique
                        function openEditor(serverName, floorId) {
                            const editorUrl = `/editor.html?server=${serverName}&floor=${floorId}`;
                            window.open(editorUrl, '_blank', 'width=1200,height=800');
                        }
                        
                        // Formater le dernier ping
                        function formatLastPing(timestamp) {
                            if (!timestamp) return 'Jamais';
                            const diff = Date.now() - timestamp;
                            if (diff < 60000) return 'Maintenant';
                            const minutes = Math.floor(diff / 60000);
                            return `${minutes}m ago`;
                        }
                        
                        // Initialisation
                        async function init() {
                            await loadProxyStatus();
                            await loadServers();
                        }
                        
                        // Démarrage
                        init();
                        
                        // Actualisation automatique
                        setInterval(() => {
                            loadProxyStatus();
                            loadServers();
                        }, 30000);
                    </script>
                </body>
                </html>
                """;
        }
    }

    // Méthodes utilitaires
    private void sendJsonResponse(HttpExchange exchange, int status, String json) throws IOException {
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, jsonBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(jsonBytes);
        }
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        String errorJson = String.format("{\"success\": false, \"error\": \"%s\"}",
                message.replace("\"", "'"));
        sendJsonResponse(exchange, status, errorJson);
    }

    private String extractParam(String query, String paramName) {
        if (query == null) return null;

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(paramName)) {
                return keyValue[1];
            }
        }
        return null;
    }
}