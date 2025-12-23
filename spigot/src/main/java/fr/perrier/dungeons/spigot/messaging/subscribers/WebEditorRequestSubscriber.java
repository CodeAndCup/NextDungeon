package fr.perrier.dungeons.spigot.messaging.subscribers;

import com.google.gson.Gson;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.messaging.packets.webeditor.WebEditorRequestPacket;
import fr.perrier.dungeons.spigot.messaging.packets.webeditor.WebEditorResponsePacket;
import fr.perrier.dungeons.spigot.messaging.pidgin.IncomingPacketHandler;
import fr.perrier.dungeons.spigot.messaging.pidgin.PacketListener;
import fr.perrier.dungeons.spigot.webeditor.ProxyEditorMessageHandler;
import org.bukkit.Bukkit;

/**
 * Subscriber pour gérer les requêtes de l'éditeur web depuis le proxy
 */
public class WebEditorRequestSubscriber implements PacketListener {

    private final ProxyEditorMessageHandler messageHandler;
    private final Gson gson = new Gson();

    public WebEditorRequestSubscriber() {
        this.messageHandler = new ProxyEditorMessageHandler();
    }

    @IncomingPacketHandler
    public void onWebEditorRequest(WebEditorRequestPacket packet) {
        // Vérifier si ce serveur est la cible du message
        // Si targetServerId est null, le message est broadcast à tous les serveurs (comportement legacy)
        String currentServerId = getServerName();
        if (packet.getTargetServerId() != null && !packet.getTargetServerId().equals(currentServerId)) {
            // Ce message n'est pas pour ce serveur, l'ignorer
            Main.getInstance().getLogger().fine("Requête ignorée: cible=" + packet.getTargetServerId() + ", serveur=" + currentServerId);
            return;
        }
        
        try {
            String response = processRequest(packet);
            
            // Déterminer le type de réponse selon le type de requête
            WebEditorResponsePacket.WebEditorResponseType responseType = switch (packet.getRequestType()) {
                case LOAD_TRIGGERS -> WebEditorResponsePacket.WebEditorResponseType.LOAD_TRIGGERS_RESPONSE;
                case SAVE_TRIGGERS -> WebEditorResponsePacket.WebEditorResponseType.SAVE_TRIGGERS_RESPONSE;
                case GET_TRIGGER_TYPES -> WebEditorResponsePacket.WebEditorResponseType.GET_TRIGGER_TYPES_RESPONSE;
                case GENERATE_BLOCKLY_JS -> WebEditorResponsePacket.WebEditorResponseType.GENERATE_BLOCKLY_JS_RESPONSE;
                case GET_FLOOR_INFO -> WebEditorResponsePacket.WebEditorResponseType.GET_FLOOR_INFO_RESPONSE;
            };
            
            // Envoyer la réponse via Redis
            WebEditorResponsePacket responsePacket = WebEditorResponsePacket.success(
                packet.getRequestId(),
                getServerName(),
                responseType,
                response
            );
            
            Main.getInstance().getMessaging().sendPacket(responsePacket);
            
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Erreur traitement requête web editor: " + e.getMessage());
            
            // Envoyer une réponse d'erreur
            WebEditorResponsePacket errorPacket = WebEditorResponsePacket.error(
                packet.getRequestId(),
                getServerName(),
                "Erreur traitement: " + e.getMessage()
            );
            
            Main.getInstance().getMessaging().sendPacket(errorPacket);
        }
    }

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
            default -> throw new IllegalArgumentException("Type de requête inconnu: " + packet.getRequestType());
        }
    }
    
    private String getServerName() {
        // Utiliser le service de récupération du nom du serveur
        // Celui-ci récupère le nom depuis le proxy (BungeeCord/Velocity)
        // via le système de plugin messaging
        return Main.getInstance().getServerNameService().getServerName();
    }
}