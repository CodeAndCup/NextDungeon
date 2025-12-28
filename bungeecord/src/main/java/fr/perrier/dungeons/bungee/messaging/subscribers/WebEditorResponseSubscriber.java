package fr.perrier.dungeons.bungee.messaging.subscribers;

import fr.perrier.dungeons.bungee.NextDungeonBungee;
import fr.perrier.dungeons.bungee.messaging.packets.webeditor.WebEditorResponsePacket;
import fr.perrier.dungeons.bungee.messaging.pidgin.IncomingPacketHandler;
import fr.perrier.dungeons.bungee.messaging.pidgin.PacketListener;

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
        NextDungeonBungee.getInstance().getLogger().info("Response received to request: " + packet.getRequestId());
        
        // Récupérer et compléter la CompletableFuture correspondante
        CompletableFuture<WebEditorResponsePacket> future = pendingRequests.remove(packet.getRequestId());
        if (future != null) {
            future.complete(packet);
        } else {
            NextDungeonBungee.getInstance().getLogger().warning("&eNo pending requests found for the ID: " + packet.getRequestId());
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