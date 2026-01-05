package fr.perrier.dungeons.velocity.messaging.subscribers;

import fr.perrier.dungeons.velocity.NextDungeonVelocity;
import fr.perrier.dungeons.velocity.messaging.packets.webeditor.WebEditorResponsePacket;
import fr.perrier.dungeons.common.messaging.pidgin.IncomingPacketHandler;
import fr.perrier.dungeons.common.messaging.pidgin.PacketListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Subscriber pour gérer les réponses de l'éditeur web depuis les serveurs Spigot
 */
public class WebEditorResponseSubscriber implements PacketListener {

    // Map pour stocker les CompletableFuture en attente de réponse
    private static final ConcurrentHashMap<String, CompletableFuture<WebEditorResponsePacket>> pendingRequests = new ConcurrentHashMap<>();

    @IncomingPacketHandler
    public void onWebEditorResponse(WebEditorResponsePacket packet) {
        NextDungeonVelocity.getInstance().getLogger().info("Response received for request id: {}", packet.getRequestId());
        
        // Récupérer et compléter la CompletableFuture correspondante
        CompletableFuture<WebEditorResponsePacket> future = pendingRequests.remove(packet.getRequestId());
        if (future != null) {
            future.complete(packet);
        } else {
            NextDungeonVelocity.getInstance().getLogger().warn("No request with id: {} has been found.", packet.getRequestId());
        }
    }

    /**
     * Enregistre une requête en attente de réponse
     */
    public static CompletableFuture<WebEditorResponsePacket> registerPendingRequest(String requestId, int timeoutSeconds) {
        CompletableFuture<WebEditorResponsePacket> future = new CompletableFuture<>();
        
        // Ajouter un timeout automatique
        future.orTimeout(timeoutSeconds, TimeUnit.SECONDS);
        
        // Nettoyer automatiquement si timeout
        future.exceptionally(throwable -> {
            pendingRequests.remove(requestId);
            return null;
        });
        
        pendingRequests.put(requestId, future);
        return future;
    }

    /**
     * Nettoie les requêtes en attente (appelé périodiquement ou au shutdown)
     */
    public static void cleanupPendingRequests() {
        pendingRequests.entrySet().removeIf(entry -> entry.getValue().isDone());
    }
}