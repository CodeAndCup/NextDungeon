package fr.perrier.dungeons.spigot.messaging.packets.webeditor;

import fr.perrier.dungeons.spigot.messaging.pidgin.Packet;
import lombok.Data;

import java.util.UUID;

/**
 * Packet de base pour les requêtes de l'éditeur web depuis le proxy vers Spigot
 */
@Data
public class WebEditorRequestPacket implements Packet {
    private final String requestId;
    private final String proxyServerId;
    private final String targetServerId; // ID du serveur Spigot qui doit traiter la requête
    private final WebEditorRequestType requestType;
    private final Object data;
    
    public enum WebEditorRequestType {
        LOAD_TRIGGERS,
        SAVE_TRIGGERS,
        GET_TRIGGER_TYPES,
        GENERATE_BLOCKLY_JS,
        GET_FLOOR_INFO
    }
    
    /**
     * Données pour la demande de chargement des triggers
     */
    @Data
    public static class LoadTriggersData {
        private final String dungeonName;
        private final String floorId;
    }
    
    /**
     * Données pour la sauvegarde des triggers
     */
    @Data
    public static class SaveTriggersData {
        private final String dungeonName;
        private final String floorId;
        private final String triggersJson;
        private final UUID editorUuid;
    }
    
    /**
     * Données pour la génération du JavaScript Blockly
     */
    @Data
    public static class GenerateBlocklyJsData {
        private final UUID editorUuid;
    }
    
    /**
     * Données pour les informations du floor
     */
    @Data
    public static class FloorInfoData {
        private final String dungeonName;
        private final String floorId;
        private final String editorName;
    }
}