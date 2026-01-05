package fr.perrier.dungeons.velocity.messaging.packets.webeditor;

import fr.perrier.dungeons.common.messaging.pidgin.Packet;
import lombok.Data;

/**
 * Packet de réponse pour les requêtes de l'éditeur web depuis Spigot vers le proxy
 */
@Data
public class WebEditorResponsePacket implements Packet {
    private final String requestId;
    private final String spigotServerId;
    private final WebEditorResponseType responseType;
    private final boolean success;
    private final String data;
    private final String error;
    
    public enum WebEditorResponseType {
        LOAD_TRIGGERS_RESPONSE,
        SAVE_TRIGGERS_RESPONSE,
        GET_TRIGGER_TYPES_RESPONSE,
        GENERATE_BLOCKLY_JS_RESPONSE,
        GET_FLOOR_INFO_RESPONSE,
        ERROR_RESPONSE
    }
    
    /**
     * Crée une réponse de succès
     */
    public static WebEditorResponsePacket success(String requestId, String spigotServerId, 
                                                 WebEditorResponseType responseType, String data) {
        return new WebEditorResponsePacket(requestId, spigotServerId, responseType, true, data, null);
    }
    
    /**
     * Crée une réponse d'erreur
     */
    public static WebEditorResponsePacket error(String requestId, String spigotServerId, String error) {
        return new WebEditorResponsePacket(requestId, spigotServerId, WebEditorResponseType.ERROR_RESPONSE, false, null, error);
    }
}