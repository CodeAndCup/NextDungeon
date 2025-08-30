package fr.perrier.dungeons.bungee.messaging;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.perrier.dungeons.bungee.NextDungeonBungee;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service de communication avec les serveurs Spigot via Redis
 */
public class SpigotCommunicationService {

    private final Gson gson = new Gson();
    private final Map<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();
    
    // Pour l'instant, simulation - dans l'implémentation réelle utiliser Redis
    // TODO: Intégrer avec le système Redis existant

    /**
     * Crée une session d'édition pour un joueur depuis un serveur Spigot
     */
    public String createEditorSession(String dungeonName, String floorId, UUID editorUuid, String editorName, String spigotServer) {
        try {
            String sessionId = NextDungeonBungee.getInstance().getWebEditorServer()
                .getSessionManager()
                .createSessionFromProxy(dungeonName, floorId, editorUuid, editorName, spigotServer);
                
            NextDungeonBungee.getInstance().getLogger().info("✅ Session créée: " + sessionId + " pour " + editorName);
            return sessionId;
        } catch (Exception e) {
            NextDungeonBungee.getInstance().getLogger().severe("Erreur création session: " + e.getMessage());
            return null;
        }
    }

    /**
     * Charge les triggers d'un floor depuis le serveur Spigot
     */
    public String loadTriggers(String spigotServer, String dungeonName, String floorId) throws Exception {
        String requestId = UUID.randomUUID().toString();
        
        // TODO: Envoyer message Redis au serveur Spigot
        JsonObject request = new JsonObject();
        request.addProperty("type", "LOAD_TRIGGERS");
        request.addProperty("requestId", requestId);
        request.addProperty("dungeonName", dungeonName);
        request.addProperty("floorId", floorId);
        
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);
        
        // TODO: Publier sur Redis channel "dungeon_webeditor_request_" + spigotServer
        NextDungeonBungee.getInstance().getLogger().info("📤 Demande de chargement triggers pour " + floorId + " sur " + spigotServer);
        
        // Simulation pour l'instant - à remplacer par vraie communication Redis
        simulateSpigotResponse(requestId, createMockTriggersResponse(dungeonName, floorId));
        
        return future.get(30, TimeUnit.SECONDS);
    }

    /**
     * Sauvegarde les triggers sur le serveur Spigot
     */
    public boolean saveTriggers(String spigotServer, String dungeonName, String floorId, String triggersJson, UUID editorUuid) throws Exception {
        String requestId = UUID.randomUUID().toString();
        
        JsonObject request = new JsonObject();
        request.addProperty("type", "SAVE_TRIGGERS");
        request.addProperty("requestId", requestId);
        request.addProperty("dungeonName", dungeonName);
        request.addProperty("floorId", floorId);
        request.addProperty("editorUuid", editorUuid.toString());
        request.addProperty("triggersData", triggersJson);
        
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);
        
        NextDungeonBungee.getInstance().getLogger().info("📤 Demande de sauvegarde triggers pour " + floorId + " sur " + spigotServer);
        
        // Simulation
        simulateSpigotResponse(requestId, "{\"success\": true}");
        
        String response = future.get(30, TimeUnit.SECONDS);
        JsonObject result = gson.fromJson(response, JsonObject.class);
        return result.has("success") && result.get("success").getAsBoolean();
    }

    /**
     * Récupère les types de triggers disponibles
     */
    public String getTriggerTypes(String spigotServer) throws Exception {
        String requestId = UUID.randomUUID().toString();
        
        JsonObject request = new JsonObject();
        request.addProperty("type", "GET_TRIGGER_TYPES");
        request.addProperty("requestId", requestId);
        
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);
        
        // Simulation
        simulateSpigotResponse(requestId, createMockTriggerTypesResponse());
        
        return future.get(30, TimeUnit.SECONDS);
    }

    /**
     * Génère le JavaScript Blockly
     */
    public String generateBlocklyJs(String spigotServer, UUID editorUuid) throws Exception {
        String requestId = UUID.randomUUID().toString();
        
        JsonObject request = new JsonObject();
        request.addProperty("type", "GENERATE_BLOCKLY_JS");
        request.addProperty("requestId", requestId);
        request.addProperty("editorUuid", editorUuid.toString());
        
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);
        
        // Simulation
        simulateSpigotResponse(requestId, "// Blockly JS généré\nconsole.log('Blockly blocks loaded');");
        
        return future.get(30, TimeUnit.SECONDS);
    }

    /**
     * Récupère les informations du floor
     */
    public String getFloorInfo(String spigotServer, String dungeonName, String floorId, String editorName) throws Exception {
        String requestId = UUID.randomUUID().toString();
        
        JsonObject request = new JsonObject();
        request.addProperty("type", "GET_FLOOR_INFO");
        request.addProperty("requestId", requestId);
        request.addProperty("dungeonName", dungeonName);
        request.addProperty("floorId", floorId);
        request.addProperty("editorName", editorName);
        
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);
        
        // Simulation
        simulateSpigotResponse(requestId, createMockFloorInfoResponse(dungeonName, floorId, editorName));
        
        return future.get(30, TimeUnit.SECONDS);
    }

    /**
     * Gère la réponse d'un serveur Spigot
     */
    public void handleSpigotResponse(String requestId, String response) {
        CompletableFuture<String> future = pendingRequests.remove(requestId);
        if (future != null) {
            future.complete(response);
        }
    }

    // Méthodes de simulation - à supprimer lors de l'intégration Redis réelle
    private void simulateSpigotResponse(String requestId, String response) {
        // Simule un délai de réponse
        CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS).execute(() -> {
            handleSpigotResponse(requestId, response);
        });
    }

    private String createMockTriggersResponse(String dungeonName, String floorId) {
        return String.format("""
            {
                "triggers": [],
                "dungeon": "%s",
                "floor": "%s",
                "count": 0
            }
            """, dungeonName, floorId);
    }

    private String createMockTriggerTypesResponse() {
        return """
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
    }

    private String createMockFloorInfoResponse(String dungeonName, String floorId, String editorName) {
        return String.format("""
            {
                "success": true,
                "dungeon": "%s",
                "floorId": "%s",
                "floorName": "Floor %s",
                "editor": "%s"
            }
            """, dungeonName, floorId, floorId, editorName);
    }
}