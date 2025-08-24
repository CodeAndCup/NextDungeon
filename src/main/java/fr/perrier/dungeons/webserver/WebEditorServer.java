package fr.perrier.dungeons.webserver;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import fr.perrier.dungeons.manager.TriggerSaveManager;
import fr.perrier.dungeons.Main;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import fr.perrier.dungeons.workflow.trigger.impl.RegionTrigger;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class WebEditorServer {
    private static final int PORT = 8080;

    private HttpServer server;
    private final Gson gson;
    private final TriggerSaveManager triggerSaveManager;
    private String currentDungeon;
    private String currentFloor;
    private Player currentEditor;

    public WebEditorServer(Player editor) {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.triggerSaveManager = new TriggerSaveManager();
        this.currentEditor = editor;
    }

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

    public void stopServer() {
        if (server != null) {
            server.stop(0);
            Main.getInstance().getLogger().info("Serveur web arrêté");
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
            try {
                // Lire le contenu de la requête
                String body;
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {

                    StringBuilder bodyBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        bodyBuilder.append(line);
                    }
                    body = bodyBuilder.toString();
                }

                Main.getInstance().getLogger().info("Données reçues pour sauvegarde (" + body.length() + " caractères): " +
                        body.substring(0, Math.min(body.length(), 200)) + "...");

                // Sauvegarder via le service
                boolean success = triggerSaveManager.saveTriggers(currentDungeon, currentFloor, body, currentEditor);

                // Préparer la réponse
                JsonObject response = new JsonObject();
                response.addProperty("success", success);
                response.addProperty("timestamp", System.currentTimeMillis());
                response.addProperty("dungeon", currentDungeon);
                response.addProperty("floor", currentFloor);

                if (success) {
                    response.addProperty("message", "Triggers sauvegardés avec succès");
                    Main.getInstance().getLogger().info("Sauvegarde réussie pour " + currentDungeon + " floor " + currentFloor);
                } else {
                    response.addProperty("message", "Erreur lors de la sauvegarde");
                    Main.getInstance().getLogger().warning("Échec de la sauvegarde pour " + currentDungeon + " floor " + currentFloor);
                }

                String jsonResponse = gson.toJson(response);
                byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(success ? 200 : 500, responseBytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                    os.flush();
                }

            } catch (Exception e) {
                Main.getInstance().getLogger().severe("Erreur lors de la sauvegarde: " + e.getMessage());
                e.printStackTrace();
                sendErrorResponse(exchange, "Erreur serveur lors de la sauvegarde: " + e.getMessage());
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
     * Handler pour charger les triggers
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
                // Charger les triggers via le service
                String jsonData = triggerSaveManager.loadTriggersAsJson(currentDungeon, String.valueOf(currentFloor));
                byte[] jsonBytes = jsonData.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, jsonBytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonBytes);
                    os.flush();
                }

                Main.getInstance().getLogger().info("Triggers envoyés (" + jsonBytes.length + " bytes)");

            } catch (Exception e) {
                Main.getInstance().getLogger().severe("Erreur lors de la récupération des triggers: " + e.getMessage());
                e.printStackTrace();
                sendErrorResponse(exchange, "Erreur serveur lors de la récupération des triggers");
            }
        }
    }

    /**
     * Handler pour les types de triggers
     */
    // Dans la classe TriggerTypesHandler, remplacer le contenu par :
    private class TriggerTypesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Main.getInstance().getLogger().info("Demande des types de triggers reçue");

                // Utiliser les configurations Blockly de vos triggers existants
                Map<String, Object> triggerTypes = new HashMap<>();

                // Region Trigger
                triggerTypes.put("region", new RegionTrigger("temp").getBlocklyConfig());

                // Ajouter les types d'actions
                Map<String, Object> actionTypes = new HashMap<>();
                Map<String, Object> sendMessageConfig = new HashMap<>();
                sendMessageConfig.put("type", "send_message");
                sendMessageConfig.put("color", "#2196F3");
                sendMessageConfig.put("icon", "message");

                Map<String, Object> sendMessageFields = new HashMap<>();
                sendMessageFields.put("targetPlayer", Map.of("type", "text", "label", "Joueur cible", "default", "player"));
                sendMessageFields.put("message", Map.of("type", "text", "label", "Message", "default", "Hello {player}!"));
                sendMessageConfig.put("fields", sendMessageFields);

                actionTypes.put("send_message", sendMessageConfig);

                Map<String, Object> response = new HashMap<>();
                response.put("triggers", triggerTypes);
                response.put("actions", actionTypes);

                String json = gson.toJson(response);
                byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

                Main.getInstance().getLogger().info("Types de triggers et actions générés (" + jsonBytes.length + " bytes)");

                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, jsonBytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonBytes);
                    os.flush();
                }

            } catch (Exception e) {
                Main.getInstance().getLogger().severe("Erreur lors de la génération des types: " + e.getMessage());
                e.printStackTrace();
                sendErrorResponse(exchange,"Erreur serveur");
            }
        }
    }

    /**
     * Handler pour les fichiers statiques - UTILISE VOS FICHIERS EXISTANTS
     */
    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            Main.getInstance().getLogger().info("Demande de fichier statique: " + path);

            if ("/".equals(path)) {
                path = "/index.html";
            }

            // Charger depuis resources/webserver/ au lieu de /web/
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
                // Si le fichier n'est pas trouvé, essayer de créer un fallback basique
                Main.getInstance().getLogger().warning("Fichier non trouvé: /webserver" + path);

                if (path.equals("/index.html")) {
                    // Créer un HTML minimal si le fichier n'existe pas
                    String fallbackHtml = createFallbackHtml();
                    byte[] htmlBytes = fallbackHtml.getBytes(StandardCharsets.UTF_8);

                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, htmlBytes.length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(htmlBytes);
                        os.flush();
                    }
                    Main.getInstance().getLogger().info("Fallback HTML servi (" + htmlBytes.length + " bytes)");
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

        /**
         * HTML de fallback au cas où le fichier resources/webserver/index.html n'existe pas
         */
        private String createFallbackHtml() {
            return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8">
                    <title>Éditeur de Triggers - Dungeons</title>
                    <style>
                        body { font-family: Arial; text-align: center; padding: 50px; background: #f0f2f5; }
                        .container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                        .error { color: #d32f2f; background: #ffebee; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>🏰 Éditeur de Triggers - Dungeons</h1>
                        <div class="error">
                            <h3>⚠️ Fichier HTML non trouvé</h3>
                            <p>Le fichier <code>resources/webserver/index.html</code> n'a pas été trouvé.</p>
                            <p>Veuillez créer ce fichier avec votre interface Blockly.</p>
                        </div>

                        <h3>🔧 API Disponibles :</h3>
                        <ul style="text-align: left;">
                            <li><strong>GET /api/triggers</strong> - Charger les triggers existants</li>
                            <li><strong>POST /api/save</strong> - Sauvegarder les triggers</li>
                            <li><strong>GET /api/trigger-types</strong> - Obtenir les types de triggers</li>
                        </ul>

                        <p>
                            <strong>Donjon actuel:</strong>""" + currentDungeon + """
                        </p>
                        <p>
                            <strong>Floor actuel:</strong>""" + currentFloor + """
                        </p>

                        <script>
                            // Test des APIs
                            console.log('=== TEST DES APIs ===');

                            fetch('/api/trigger-types')
                                .then(response => response.json())
                                .then(data => {
                                    console.log('Types de triggers:', data);
                                    document.querySelector('.container').innerHTML += '<div style="background: #e8f5e8; padding: 10px; margin: 10px 0; border-radius: 5px;">✅ API trigger-types OK</div>';
                                })
                                .catch(error => {
                                    console.error('Erreur API trigger-types:', error);
                                    document.querySelector('.container').innerHTML += '<div style="background: #ffebee; padding: 10px; margin: 10px 0; border-radius: 5px;">❌ API trigger-types ERROR</div>';
                                });

                            fetch('/api/triggers')
                                .then(response => response.json())
                                .then(data => {
                                    console.log('Triggers:', data);
                                    document.querySelector('.container').innerHTML += '<div style="background: #e8f5e8; padding: 10px; margin: 10px 0; border-radius: 5px;">✅ API triggers OK</div>';
                                })
                                .catch(error => {
                                    console.error('Erreur API triggers:', error);
                                    document.querySelector('.container').innerHTML += '<div style="background: #ffebee; padding: 10px; margin: 10px 0; border-radius: 5px;">❌ API triggers ERROR</div>';
                                });
                        </script>
                    </div>
                </body>
                </html>
                """;
        }
    }

    // Méthodes utilitaires
    private void sendErrorResponse(HttpExchange exchange, String message) throws IOException {
        String errorJson = "{\"error\":\"" + message.replace("\"", "'") + "\"}";
        byte[] errorBytes = errorJson.getBytes(StandardCharsets.UTF_8);

        try {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(500, errorBytes.length);

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

    // Ajouter cette méthode à votre WebEditorServer existant

    /**
     * Handler pour générer le JavaScript Blockly dynamiquement
     */
    private class BlocklyGeneratorHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Main.getInstance().getLogger().info("Génération du JavaScript Blockly...");

                String blocklyJs = generateBlocklyJavaScript();
                byte[] jsBytes = blocklyJs.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set("Content-Type", "application/javascript; charset=UTF-8");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, jsBytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsBytes);
                    os.flush();
                }

                Main.getInstance().getLogger().info("JavaScript Blockly généré (" + jsBytes.length + " bytes)");

            } catch (Exception e) {
                Main.getInstance().getLogger().severe("Erreur lors de la génération du JavaScript: " + e.getMessage());
                e.printStackTrace();
                sendErrorResponse(exchange, "Erreur serveur");
            }
        }
    }

    /**
     * Génère dynamiquement le JavaScript pour Blockly
     */
    private String generateBlocklyJavaScript() {
        StringBuilder js = new StringBuilder();

        js.append("// Auto-généré par le serveur Java\n");
        js.append("console.log('🔧 Chargement des blocs auto-générés...');\n\n");

        // Générer les blocs de triggers
        generateTriggerBlocks(js);

        // Générer les blocs d'actions
        generateActionBlocks(js);

        // Générer la toolbox
        generateToolbox(js);

        // Générer les fonctions utilitaires
        generateUtilityFunctions(js);

        return js.toString();
    }

    private void generateTriggerBlocks(StringBuilder js) {
        js.append("// ===== BLOCS TRIGGERS =====\n");

        // Region Trigger
        js.append("""
        Blockly.Blocks['region_trigger'] = {
            init: function() {
                this.appendDummyInput()
                    .appendField("📍 Quand le joueur entre en région")
                    .appendField("Pos1:");
                this.appendDummyInput()
                    .appendField("X:")
                    .appendField(new Blockly.FieldNumber(0), "POS1_X")
                    .appendField("Y:")
                    .appendField(new Blockly.FieldNumber(64), "POS1_Y")  
                    .appendField("Z:")
                    .appendField(new Blockly.FieldNumber(0), "POS1_Z");
                this.appendDummyInput()
                    .appendField("Pos2:")
                    .appendField("X:")
                    .appendField(new Blockly.FieldNumber(10), "POS2_X")
                    .appendField("Y:")
                    .appendField(new Blockly.FieldNumber(74), "POS2_Y")
                    .appendField("Z:")
                    .appendField(new Blockly.FieldNumber(10), "POS2_Z");
                this.appendDummyInput()
                    .appendField("Monde:")
                    .appendField(new Blockly.FieldTextInput("world"), "WORLD");
                this.appendStatementInput("ACTIONS")
                    .setCheck("Action")
                    .appendField("Faire:");
                this.setColour('#4CAF50');
                this.setTooltip("Déclenche quand un joueur entre dans une région définie");
            }
        };
        
        """);

        // Debug Trigger (pour les tests)
        js.append("""
        Blockly.Blocks['debug_chat_trigger'] = {
            init: function() {
                this.appendDummyInput()
                    .appendField("🐛 Quand le message")
                    .appendField(new Blockly.FieldTextInput("test"), "MESSAGE")
                    .appendField("est écrit");
                this.appendValueInput("CASE_SENSITIVE")
                    .setCheck("Boolean")
                    .appendField("Sensible à la casse:");
                this.appendValueInput("EXACT_MATCH")
                    .setCheck("Boolean")
                    .appendField("Correspondance exacte:");
                this.appendStatementInput("ACTIONS")
                    .setCheck("Action")
                    .appendField("Faire:");
                this.setColour('#9B59B6');
                this.setTooltip("Trigger de debug pour tester les messages");
            }
        };
        
        """);
    }

    private void generateActionBlocks(StringBuilder js) {
        js.append("// ===== BLOCS ACTIONS =====\n");

        // Send Message Action
        js.append("""
        Blockly.Blocks['send_message_action'] = {
            init: function() {
                this.appendDummyInput()
                    .appendField("💬 Envoyer message à")
                    .appendField(new Blockly.FieldTextInput("player"), "TARGET_PLAYER")
                    .appendField(":");
                this.appendDummyInput()
                    .appendField("Message:")
                    .appendField(new Blockly.FieldTextInput("&aBonjour {player}!"), "MESSAGE");
                this.setPreviousStatement(true, "Action");
                this.setNextStatement(true, "Action");
                this.setColour('#2196F3');
                this.setTooltip("Envoie un message à un joueur spécifique\\n{player} = joueur déclencheur\\n{target} = joueur cible\\n& = codes couleur");
            }
        };
        
        """);

        // Boolean blocks for conditions
        js.append("""
        Blockly.Blocks['boolean_true'] = {
            init: function() {
                this.appendDummyInput()
                    .appendField("✅ Vrai");
                this.setOutput(true, "Boolean");
                this.setColour('#4CAF50');
            }
        };
        
        Blockly.Blocks['boolean_false'] = {
            init: function() {
                this.appendDummyInput()
                    .appendField("❌ Faux");
                this.setOutput(true, "Boolean");
                this.setColour('#F44336');
            }
        };
        
        """);
    }

    private void generateToolbox(StringBuilder js) {
        js.append("""
        // ===== TOOLBOX CONFIGURATION =====
        const DUNGEON_TOOLBOX = {
            "kind": "categoryToolbox",
            "contents": [
                {
                    "kind": "category",
                    "name": "🎯 Triggers",
                    "colour": "#FF6B6B",
                    "contents": [
                        {"kind": "block", "type": "region_trigger"}
                    ]
                },
                {
                    "kind": "category", 
                    "name": "⚡ Actions",
                    "colour": "#2196F3",
                    "contents": [
                        {"kind": "block", "type": "send_message_action"}
                    ]
                },
                {
                    "kind": "category",
                    "name": "🔧 Utilitaires", 
                    "colour": "#9E9E9E",
                    "contents": [
                        {"kind": "block", "type": "boolean_true"},
                        {"kind": "block", "type": "boolean_false"}
                    ]
                },
                {
                    "kind": "category",
                    "name": "🐛 Debug",
                    "colour": "#9B59B6", 
                    "contents": [
                        {"kind": "block", "type": "debug_chat_trigger"}
                    ]
                }
            ]
        };
        
        """);
    }

    private void generateUtilityFunctions(StringBuilder js) {
        js.append("""
        // ===== FONCTIONS UTILITAIRES =====
        
        // Génération des triggers depuis l'espace de travail
        function generateTriggersFromWorkspace() {
            console.log('🔄 Génération des triggers...');
            const triggers = [];
            const blocks = workspace.getTopBlocks();
            
            blocks.forEach(block => {
                console.log('Bloc trouvé:', block.type);
                
                if (block.type === 'region_trigger') {
                    triggers.push({
                        type: 'region',
                        name: 'Region_' + Date.now(),
                        pos1X: parseFloat(block.getFieldValue('POS1_X')) || 0,
                        pos1Y: parseFloat(block.getFieldValue('POS1_Y')) || 64,
                        pos1Z: parseFloat(block.getFieldValue('POS1_Z')) || 0,
                        pos2X: parseFloat(block.getFieldValue('POS2_X')) || 10,
                        pos2Y: parseFloat(block.getFieldValue('POS2_Y')) || 74,
                        pos2Z: parseFloat(block.getFieldValue('POS2_Z')) || 10,
                        world: block.getFieldValue('WORLD') || 'world',
                        actions: getActionsFromBlock(block)
                    });
                } else if (block.type === 'debug_chat_trigger') {
                    const caseSensitiveBlock = block.getInputTargetBlock('CASE_SENSITIVE');
                    const exactMatchBlock = block.getInputTargetBlock('EXACT_MATCH');
                    
                    triggers.push({
                        type: 'debug_chat',
                        name: 'Debug_' + Date.now(),
                        message: block.getFieldValue('MESSAGE') || 'test',
                        caseSensitive: caseSensitiveBlock ? caseSensitiveBlock.type === 'boolean_true' : false,
                        exactMatch: exactMatchBlock ? exactMatchBlock.type === 'boolean_true' : true,
                        actions: getActionsFromBlock(block)
                    });
                }
            });
            
            console.log('Triggers générés:', triggers);
            return triggers;
        }
        
        // Extraction des actions d'un bloc
        function getActionsFromBlock(block) {
            const actions = [];
            let actionBlock = block.getInputTargetBlock('ACTIONS');
            
            while (actionBlock) {
                console.log('Action trouvée:', actionBlock.type);
                
                if (actionBlock.type === 'send_message_action') {
                    actions.push({
                        type: 'send_message',
                        targetPlayer: actionBlock.getFieldValue('TARGET_PLAYER') || 'player',
                        message: actionBlock.getFieldValue('MESSAGE') || 'Hello!'
                    });
                }
                
                actionBlock = actionBlock.getNextBlock();
            }
            
            console.log('Actions extraites:', actions);
            return actions;
        }
        
        // Initialisation automatique
        console.log('✅ Blocs auto-générés chargés!');
        
        """);
    }
}