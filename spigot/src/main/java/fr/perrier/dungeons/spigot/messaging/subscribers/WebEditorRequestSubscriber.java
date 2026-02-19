package fr.perrier.dungeons.spigot.messaging.subscribers;

import com.google.gson.Gson;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.messaging.packets.webeditor.WebEditorRequestPacket;
import fr.perrier.dungeons.spigot.messaging.packets.webeditor.WebEditorResponsePacket;
import fr.perrier.dungeons.common.messaging.pidgin.IncomingPacketHandler;
import fr.perrier.dungeons.common.messaging.pidgin.PacketListener;
import fr.perrier.dungeons.spigot.webeditor.ProxyEditorMessageHandler;

/**
 * Subscriber for managing web editor requests from the proxy.
 *
 * This class handles incoming web editor request packets and routes them to appropriate handlers.
 * It verifies that requests are targeted to this server before processing them.
 */
public class WebEditorRequestSubscriber implements PacketListener {

    private final ProxyEditorMessageHandler messageHandler;
    private final Gson gson = new Gson();

    /**
     * Constructs a new WebEditorRequestSubscriber with a ProxyEditorMessageHandler instance.
     */
    public WebEditorRequestSubscriber() {
        this.messageHandler = new ProxyEditorMessageHandler();
    }

    /**
     * Handles incoming web editor request packets asynchronously.
     *
     * This method retrieves the server name asynchronously and checks if the request
     * is targeted to this server. If targetServerId is null, the request is broadcast
     * to all servers (legacy behavior).
     *
     * @param packet the incoming WebEditorRequestPacket to process
     */
    @IncomingPacketHandler
    public void onWebEditorRequest(WebEditorRequestPacket packet) {
        // Asynchronously retrieve the server name and process the request
        Main.getInstance().getServerNameService().getServerName().thenAccept(currentServerId -> {
            // Check if this server is the target of the message
            // If targetServerId is null, the message is broadcast to all servers (legacy behavior)
            if (packet.getTargetServerId() != null && !packet.getTargetServerId().equals(currentServerId)) {
                // This message is not for this server, ignore it
                Main.getLoggerUtil().info("Request ignored: target=" + packet.getTargetServerId() + ", server=" + currentServerId);
                return;
            }

            try {
                String response = processRequest(packet);

                // Determine the response type based on the request type
                WebEditorResponsePacket.WebEditorResponseType responseType = switch (packet.getRequestType()) {
                    case LOAD_TRIGGERS -> WebEditorResponsePacket.WebEditorResponseType.LOAD_TRIGGERS_RESPONSE;
                    case SAVE_TRIGGERS -> WebEditorResponsePacket.WebEditorResponseType.SAVE_TRIGGERS_RESPONSE;
                    case GET_TRIGGER_TYPES -> WebEditorResponsePacket.WebEditorResponseType.GET_TRIGGER_TYPES_RESPONSE;
                    case GENERATE_BLOCKLY_JS -> WebEditorResponsePacket.WebEditorResponseType.GENERATE_BLOCKLY_JS_RESPONSE;
                    case GET_FLOOR_INFO -> WebEditorResponsePacket.WebEditorResponseType.GET_FLOOR_INFO_RESPONSE;
                };

                // Send the response via Redis
                WebEditorResponsePacket responsePacket = WebEditorResponsePacket.success(
                        packet.getRequestId(),
                        currentServerId,
                        responseType,
                        response
                );

                Main.getInstance().getMessaging().sendPacket(responsePacket);

            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error processing web editor request: " + e.getMessage());

                // Send an error response
                WebEditorResponsePacket errorPacket = WebEditorResponsePacket.error(
                        packet.getRequestId(),
                        currentServerId,
                        "Processing error: " + e.getMessage()
                );

                Main.getInstance().getMessaging().sendPacket(errorPacket);
            }
        }).exceptionally(e -> {
            Main.getLoggerUtil().severe("Error retrieving server name: " + e.getMessage());
            return null;
        });
    }

    /**
     * Processes the incoming web editor request and returns the response.
     *
     * @param packet the WebEditorRequestPacket containing the request data
     * @return the response string from the handler
     * @throws IllegalArgumentException if the request type is unknown
     */
    private String processRequest(WebEditorRequestPacket packet) {
        switch (packet.getRequestType()) {
            case LOAD_TRIGGERS -> {
                WebEditorRequestPacket.LoadTriggersData data = gson.fromJson(
                    gson.toJson(packet.getData()), 
                    WebEditorRequestPacket.LoadTriggersData.class
                );
                return messageHandler.handleLoadTriggersRequest(data.getDungeonName(), data.getFloorId());
            }
            case SAVE_TRIGGERS -> {
                WebEditorRequestPacket.SaveTriggersData data = gson.fromJson(
                    gson.toJson(packet.getData()), 
                    WebEditorRequestPacket.SaveTriggersData.class
                );
                return messageHandler.handleSaveTriggersRequest(
                    data.getDungeonName(), 
                    data.getFloorId(), 
                    data.getTriggersJson(), 
                    data.getEditorUuid()
                );
            }
            case GET_TRIGGER_TYPES -> {
                return messageHandler.handleGetTriggerTypesRequest();
            }
            case GENERATE_BLOCKLY_JS -> {
                WebEditorRequestPacket.GenerateBlocklyJsData data = gson.fromJson(
                    gson.toJson(packet.getData()), 
                    WebEditorRequestPacket.GenerateBlocklyJsData.class
                );
                return messageHandler.handleGenerateBlocklyJsRequest(data.getEditorUuid());
            }
            case GET_FLOOR_INFO -> {
                WebEditorRequestPacket.FloorInfoData data = gson.fromJson(
                    gson.toJson(packet.getData()), 
                    WebEditorRequestPacket.FloorInfoData.class
                );
                return messageHandler.handleGetFloorInfoRequest(
                    data.getDungeonName(), 
                    data.getFloorId(), 
                    data.getEditorName()
                );
            }
            default -> throw new IllegalArgumentException("Unknown request type: " + packet.getRequestType());
        }
    }
}