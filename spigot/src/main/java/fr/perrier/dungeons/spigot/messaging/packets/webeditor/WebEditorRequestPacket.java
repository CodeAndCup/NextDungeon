package fr.perrier.dungeons.spigot.messaging.packets.webeditor;

import fr.perrier.dungeons.spigot.messaging.pidgin.Packet;
import lombok.Data;

import java.util.UUID;

/**
 * Base packet for web editor requests from the proxy to Spigot.
 */
@Data
public class WebEditorRequestPacket implements Packet {
    private final String requestId;
    private final String proxyServerId;
    /**
     * ID of the Spigot server that should handle the request.
     */
    private final String targetServerId;
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
     * Data for the trigger loading request.
     */
    @Data
    public static class LoadTriggersData {
        private final String dungeonName;
        private final String floorId;
    }
    
    /**
     * Data for the trigger saving request.
     */
    @Data
    public static class SaveTriggersData {
        private final String dungeonName;
        private final String floorId;
        private final String triggersJson;
        private final UUID editorUuid;
    }
    
    /**
     * Data for the Blockly JavaScript generation request.
     */
    @Data
    public static class GenerateBlocklyJsData {
        private final UUID editorUuid;
    }
    
    /**
     * Data for floor information requests.
     */
    @Data
    public static class FloorInfoData {
        private final String dungeonName;
        private final String floorId;
        private final String editorName;
    }
}