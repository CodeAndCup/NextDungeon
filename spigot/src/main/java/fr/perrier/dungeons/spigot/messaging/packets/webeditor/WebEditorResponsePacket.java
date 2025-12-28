package fr.perrier.dungeons.spigot.messaging.packets.webeditor;

import fr.perrier.dungeons.spigot.messaging.pidgin.Packet;
import lombok.Data;

/**
 * Response packet for web editor requests from Spigot to the proxy.
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
     * Creates a success response.
     *
     * @param requestId        the request identifier
     * @param spigotServerId   the Spigot server identifier
     * @param responseType     the type of response
     * @param data             the response data
     * @return a WebEditorResponsePacket indicating success
     */
    public static WebEditorResponsePacket success(String requestId, String spigotServerId,
                                                 WebEditorResponseType responseType, String data) {
        return new WebEditorResponsePacket(requestId, spigotServerId, responseType, true, data, null);
    }

    /**
     * Creates an error response.
     *
     * @param requestId        the request identifier
     * @param spigotServerId   the Spigot server identifier
     * @param error            the error message
     * @return a WebEditorResponsePacket indicating an error
     */
    public static WebEditorResponsePacket error(String requestId, String spigotServerId, String error) {
        return new WebEditorResponsePacket(requestId, spigotServerId, WebEditorResponseType.ERROR_RESPONSE, false, null, error);
    }
}