package fr.perrier.dungeons.velocity.messaging;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.perrier.dungeons.velocity.NextDungeonVelocity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Service de communication avec les serveurs Spigot via Redis
 */
public class SpigotCommunicationService {

    private final Gson gson = new Gson();

    /**
     * Crée une session d'édition pour un joueur depuis un serveur Spigot
     */
    public String createEditorSession(String dungeonName, String floorId, UUID editorUuid, String editorName, String spigotServer) {
        try {
            String sessionId = NextDungeonVelocity.getInstance().getWebEditorServer()
                .getSessionManager()
                .createSessionFromProxy(dungeonName, floorId, editorUuid, editorName, spigotServer);
                
            NextDungeonVelocity.getInstance().getLogger().info("✅ Session créée: " + sessionId + " pour " + editorName);
            return sessionId;
        } catch (Exception e) {
            NextDungeonVelocity.getInstance().getLogger().error("Erreur création session: " + e.getMessage());
            return null;
        }
    }

    /**
     * Charge les triggers d'un floor depuis le serveur Spigot
     */
    public String loadTriggers(String spigotServer, String dungeonName, String floorId) throws Exception {
        JsonObject request = new JsonObject();
        request.addProperty("type", "LOAD_TRIGGERS");
        request.addProperty("dungeonName", dungeonName);
        request.addProperty("floorId", floorId);
        
        NextDungeonVelocity.getInstance().getLogger().info("📤 Demande de chargement triggers pour " + floorId + " sur " + spigotServer);
        
        String response = sendSpigotRequest(spigotServer, request.toString());
        return response != null ? response : createMockTriggersResponse(dungeonName, floorId);
    }

    /**
     * Sauvegarde les triggers sur le serveur Spigot
     */
    public boolean saveTriggers(String spigotServer, String dungeonName, String floorId, String triggersJson, UUID editorUuid) throws Exception {
        JsonObject request = new JsonObject();
        request.addProperty("type", "SAVE_TRIGGERS");
        request.addProperty("dungeonName", dungeonName);
        request.addProperty("floorId", floorId);
        request.addProperty("editorUuid", editorUuid.toString());
        request.addProperty("triggersData", triggersJson);
        
        NextDungeonVelocity.getInstance().getLogger().info("📤 Demande de sauvegarde triggers pour " + floorId + " sur " + spigotServer);
        
        String response = sendSpigotRequest(spigotServer, request.toString());
        if (response != null) {
            JsonObject result = gson.fromJson(response, JsonObject.class);
            return result.has("success") && result.get("success").getAsBoolean();
        }
        return false;
    }

    /**
     * Récupère les types de triggers disponibles
     */
    public String getTriggerTypes(String spigotServer) throws Exception {
        JsonObject request = new JsonObject();
        request.addProperty("type", "GET_TRIGGER_TYPES");
        
        String response = sendSpigotRequest(spigotServer, request.toString());
        return response != null ? response : createMockTriggerTypesResponse();
    }

    /**
     * Génère le JavaScript Blockly
     */
    public String generateBlocklyJs(String spigotServer, UUID editorUuid) throws Exception {
        JsonObject request = new JsonObject();
        request.addProperty("type", "GENERATE_BLOCKLY_JS");
        request.addProperty("editorUuid", editorUuid.toString());
        
        String response = sendSpigotRequest(spigotServer, request.toString());
        
        // Pour Blockly JS, la réponse est du JavaScript pur, pas du JSON
        if (response != null) {
            try {
                // Vérifier si c'est du JSON avec erreur
                JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
                if (jsonResponse.has("error")) {
                    return "// Erreur: " + jsonResponse.get("error").getAsString() + "\nconsole.error('Erreur génération Blockly');";
                }
            } catch (Exception e) {
                // Si ce n'est pas du JSON, c'est probablement du JavaScript valide
                return response;
            }
        }
        
        return "// Erreur communication Spigot\nconsole.error('Communication error');";
    }

    /**
     * Récupère les informations du floor
     */
    public String getFloorInfo(String spigotServer, String dungeonName, String floorId, String editorName) throws Exception {
        JsonObject request = new JsonObject();
        request.addProperty("type", "GET_FLOOR_INFO");
        request.addProperty("dungeonName", dungeonName);
        request.addProperty("floorId", floorId);
        request.addProperty("editorName", editorName);
        
        String response = sendSpigotRequest(spigotServer, request.toString());
        return response != null ? response : createMockFloorInfoResponse(dungeonName, floorId, editorName);
    }

    /**
     * Envoie une requête HTTP au serveur Spigot
     */
    private String sendSpigotRequest(String spigotServer, String jsonData) {
        try {
            // Pour simplifier, on suppose que le serveur Spigot est sur localhost:8081
            // Dans une vraie implémentation, mapper spigotServer vers l'IP réelle
            java.net.URL url = new java.net.URL("http://localhost:8081/spigot-api/request");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(30000);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    return response.toString();
                }
            } else {
                NextDungeonVelocity.getInstance().getLogger().warn("Erreur communication Spigot: HTTP " + responseCode);
                return null;
            }
        } catch (Exception e) {
            NextDungeonVelocity.getInstance().getLogger().warn("Communication Spigot échouée: " + e.getMessage());
            return null;
        }
    }

    // Méthodes de fallback en cas d'échec de communication
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