package fr.perrier.dungeons.velocity.messaging;

import com.google.gson.Gson;
import fr.perrier.dungeons.velocity.NextDungeonVelocity;
import fr.perrier.dungeons.velocity.messaging.packets.webeditor.WebEditorRequestPacket;
import fr.perrier.dungeons.velocity.messaging.packets.webeditor.WebEditorResponsePacket;
import fr.perrier.dungeons.velocity.messaging.subscribers.WebEditorResponseSubscriber;
import jodd.util.Base64;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service de communication avec les serveurs Spigot via Redis
 */
public class SpigotCommunicationService {

    private final Gson gson = new Gson();
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    /**
     * Crée une session d'édition pour un joueur depuis un serveur Spigot
     */
    public String createEditorSession(String dungeonName, String floorId, UUID editorUuid, String editorName, String spigotServer) {
        try {
            String sessionId = NextDungeonVelocity.getInstance().getWebEditorServer()
                .getSessionManager()
                .createSessionFromProxy(dungeonName, floorId, editorUuid, editorName, spigotServer);

            NextDungeonVelocity.getInstance().getLogger().info("Session created: {} for {}", sessionId, editorName);
            return sessionId;
        } catch (Exception e) {
            NextDungeonVelocity.getInstance().getLogger().error("Session creation error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Charge les triggers d'un floor depuis le serveur Spigot
     */
    public String loadTriggers(String spigotServer, String dungeonName, String floorId) throws Exception {
        WebEditorRequestPacket.LoadTriggersData data = new WebEditorRequestPacket.LoadTriggersData(dungeonName, floorId);
        
        WebEditorResponsePacket response = sendRequestAndWaitResponse(
            spigotServer, 
            WebEditorRequestPacket.WebEditorRequestType.LOAD_TRIGGERS, 
            data
        );
        
        if (response != null && response.isSuccess()) {
            NextDungeonVelocity.getInstance().getLogger().info("Triggers loaded for {} from {}", floorId, spigotServer);
            return response.getData();
        } else {
            NextDungeonVelocity.getInstance().getLogger().warn("Failed to load triggers for {}", floorId);
            return createMockTriggersResponse(dungeonName, floorId);
        }
    }

    /**
     * Sauvegarde les triggers sur le serveur Spigot
     */
    public boolean saveTriggers(String spigotServer, String dungeonName, String floorId, String triggersJson, UUID editorUuid) throws Exception {
        WebEditorRequestPacket.SaveTriggersData data = new WebEditorRequestPacket.SaveTriggersData(
            dungeonName, floorId, triggersJson, editorUuid
        );
        
        WebEditorResponsePacket response = sendRequestAndWaitResponse(
            spigotServer, 
            WebEditorRequestPacket.WebEditorRequestType.SAVE_TRIGGERS, 
            data
        );
        
        if (response != null && response.isSuccess()) {
            NextDungeonVelocity.getInstance().getLogger().info("Triggers saved for {} on {}", floorId, spigotServer);
            return true;
        } else {
            NextDungeonVelocity.getInstance().getLogger().warn("Backup failure triggers for {}", floorId);
            return false;
        }
    }


    /**
     * Get trigger types from the spigot server
     * @param spigotServer The target Spigot server
     * @return
     */
    public String getTriggerTypes(String spigotServer) {
        WebEditorResponsePacket response = sendRequestAndWaitResponse(
            spigotServer, 
            WebEditorRequestPacket.WebEditorRequestType.GET_TRIGGER_TYPES, 
            null
        );
        
        if (response != null && response.isSuccess()) {
            return response.getData();
        } else {
            return createMockTriggerTypesResponse();
        }
    }

    /**
     * Request to spigot server to generate the blockly JavaScript file to get available blocks
     *
     * @param spigotServer The target Spigot server
     * @param editorUuid The UUID of the used editor
     * @return Javascript file for blockly configuration
     */
    public String generateBlocklyJs(String spigotServer, UUID editorUuid) {
        WebEditorRequestPacket.GenerateBlocklyJsData data = new WebEditorRequestPacket.GenerateBlocklyJsData(editorUuid);
        
        WebEditorResponsePacket response = sendRequestAndWaitResponse(
            spigotServer, 
            WebEditorRequestPacket.WebEditorRequestType.GENERATE_BLOCKLY_JS, 
            data
        );
        
        if (response != null && response.isSuccess()) {
            return Base64.decodeToString(response.getData());
        } else {
            return "/* Erreur communication Spigot */ console.error('Communication error');";
        }
    }

    /**
     * Get floor information (like name, editor, etc.) from the Spigot server. This can be used to display contextual information in the web editor interface.
     *
     * @param spigotServer The target Spigot server to request the floor information from
     * @param dungeonName The name of the dungeon for which to retrieve floor information
     * @param floorId The ID of the floor for which to retrieve information
     * @param editorName The name of the editor requesting the information (can be used for logging or permission checks on the Spigot side)
     * @return A JSON string containing the floor information, or a mock response if the communication fails
     * @throws Exception if there is an error during the communication process
     */
    public String getFloorInfo(String spigotServer, String dungeonName, String floorId, String editorName) throws Exception {
        WebEditorRequestPacket.FloorInfoData data = new WebEditorRequestPacket.FloorInfoData(dungeonName, floorId, editorName);
        
        WebEditorResponsePacket response = sendRequestAndWaitResponse(
            spigotServer, 
            WebEditorRequestPacket.WebEditorRequestType.GET_FLOOR_INFO, 
            data
        );
        
        if (response != null && response.isSuccess()) {
            return response.getData();
        } else {
            return createMockFloorInfoResponse(dungeonName, floorId, editorName);
        }
    }

    /**
     * Send a request to the specified Spigot server and wait for the response with a timeout.
     * Note: This method assumes that the WebEditorResponseSubscriber is properly set up to handle incoming responses and match them with the requestId.
     *
     * @param spigotServer The target Spigot server to send the request to
     * @param requestType The type of request being sent
     * @param data The data payload for the request (can be null if not needed)
     * @return The response packet received from the Spigot server, or null if a timeout or error occurred
     */
    private WebEditorResponsePacket sendRequestAndWaitResponse(String spigotServer, 
                                                              WebEditorRequestPacket.WebEditorRequestType requestType, 
                                                              Object data) {
        String requestId = UUID.randomUUID().toString();
        
        // Register the pending request and get a future for the response
        CompletableFuture<WebEditorResponsePacket> future = WebEditorResponseSubscriber.registerPendingRequest(
            requestId, DEFAULT_TIMEOUT_SECONDS
        );
        
        // Create and send the request packet
        WebEditorRequestPacket requestPacket = new WebEditorRequestPacket(
            requestId,
            "velocity-proxy",
            spigotServer, // Target Spigot server
            requestType,
            data
        );
        
        NextDungeonVelocity.getInstance().getMessaging().sendPacket(requestPacket);
        NextDungeonVelocity.getInstance().getLogger().info("Request sent to {}: {} (ID: {})", spigotServer, requestType, requestId);
        
        try {
            // Waiting for the response with a timeout
            return future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            NextDungeonVelocity.getInstance().getLogger().warn("Timeout or error waiting for response: {}", e.getMessage());
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