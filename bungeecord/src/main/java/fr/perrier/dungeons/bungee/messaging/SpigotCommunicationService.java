package fr.perrier.dungeons.bungee.messaging;

import com.google.gson.Gson;
import fr.perrier.dungeons.bungee.NextDungeonBungee;
import fr.perrier.dungeons.bungee.messaging.packets.webeditor.WebEditorRequestPacket;
import fr.perrier.dungeons.bungee.messaging.packets.webeditor.WebEditorResponsePacket;
import fr.perrier.dungeons.bungee.messaging.subscribers.WebEditorResponseSubscriber;

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
        WebEditorRequestPacket.LoadTriggersData data = new WebEditorRequestPacket.LoadTriggersData(dungeonName, floorId);
        
        WebEditorResponsePacket response = sendRequestAndWaitResponse(
            spigotServer, 
            WebEditorRequestPacket.WebEditorRequestType.LOAD_TRIGGERS, 
            data
        );
        
        if (response != null && response.isSuccess()) {
            NextDungeonBungee.getInstance().getLogger().info("📥 Triggers chargés pour " + floorId + " depuis " + spigotServer);
            return response.getData();
        } else {
            NextDungeonBungee.getInstance().getLogger().warning("❌ Échec chargement triggers pour " + floorId);
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
            NextDungeonBungee.getInstance().getLogger().info("📥 Triggers sauvegardés pour " + floorId + " sur " + spigotServer);
            return true;
        } else {
            NextDungeonBungee.getInstance().getLogger().warning("❌ Échec sauvegarde triggers pour " + floorId);
            return false;
        }
    }

    /**
     * Récupère les types de triggers disponibles
     */
    public String getTriggerTypes(String spigotServer) throws Exception {
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
     * Génère le JavaScript Blockly
     */
    public String generateBlocklyJs(String spigotServer, UUID editorUuid) throws Exception {
        WebEditorRequestPacket.GenerateBlocklyJsData data = new WebEditorRequestPacket.GenerateBlocklyJsData(editorUuid);
        
        WebEditorResponsePacket response = sendRequestAndWaitResponse(
            spigotServer, 
            WebEditorRequestPacket.WebEditorRequestType.GENERATE_BLOCKLY_JS, 
            data
        );
        
        if (response != null && response.isSuccess()) {
            return response.getData();
        } else {
            return "/* Erreur communication Spigot */ console.error('Communication error');";
        }
    }

    /**
     * Récupère les informations du floor
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
     * Envoie une requête Redis et attend la réponse
     */
    private WebEditorResponsePacket sendRequestAndWaitResponse(String spigotServer, 
                                                              WebEditorRequestPacket.WebEditorRequestType requestType, 
                                                              Object data) throws Exception {
        String requestId = UUID.randomUUID().toString();
        
        // Enregistrer la requête en attente
        CompletableFuture<WebEditorResponsePacket> future = WebEditorResponseSubscriber.registerPendingRequest(
            requestId, DEFAULT_TIMEOUT_SECONDS
        );
        
        // Créer et envoyer le packet de requête
        WebEditorRequestPacket requestPacket = new WebEditorRequestPacket(
            requestId,
            "bungee-proxy", // TODO: obtenir l'ID du proxy depuis la config
            requestType,
            data
        );
        
        NextDungeonBungee.getInstance().getMessaging().sendPacket(requestPacket);
        NextDungeonBungee.getInstance().getLogger().info("📤 Requête envoyée: " + requestType + " (ID: " + requestId + ")");
        
        try {
            // Attendre la réponse
            return future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            NextDungeonBungee.getInstance().getLogger().warning("⏱️ Timeout ou erreur attente réponse: " + e.getMessage());
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