package fr.perrier.dungeons.spigot.messaging;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.perrier.dungeons.spigot.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service pour obtenir le nom du serveur depuis le proxy (BungeeCord/Velocity)
 * via le système de plugin messaging
 */
public class ServerNameService implements PluginMessageListener {

    private static final String DUNGEONS_CHANNEL = "dungeons:main";

    private final AtomicReference<String> cachedServerName = new AtomicReference<>(null);
    private final AtomicReference<CompletableFuture<String>> pendingRequest = new AtomicReference<>(null);

    /**
     * Initialise le service et enregistre le canal de plugin messaging
     */
    public void initialize() {
        // Enregistrer le canal pour dungeons:main (communication avec Velocity)
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(Main.getInstance(), DUNGEONS_CHANNEL);
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(Main.getInstance(), DUNGEONS_CHANNEL, this);

        Main.getInstance().getLogger().info("Service de récupération du nom de serveur initialisé");
    }

    /**
     * Obtient le nom du serveur depuis le proxy
     * Utilise un cache pour éviter les requêtes répétées
     * 
     * @return Le nom du serveur tel que défini dans la configuration du proxy
     */
    public String getServerName() {
        String cached = cachedServerName.get();
        if (cached != null) {
            return cached;
        }

        // Si aucun joueur n'est connecté, utiliser un fallback
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            // Fallback vers le nom Bukkit si aucun joueur n'est connecté
            String fallback = Bukkit.getServer().getName();
            Main.getInstance().getLogger().warning("Aucun joueur connecté pour récupérer le nom du serveur, utilisation de: " + fallback);
            return fallback;
        }

        // Requête asynchrone avec timeout
        try {
            CompletableFuture<String> future = requestServerName();
            if (future != null) {
                String serverName = future.get(3, TimeUnit.SECONDS);
                cachedServerName.set(serverName);
                Main.getInstance().getLogger().info("Nom du serveur récupéré: " + serverName);
                return serverName;
            }
        } catch (Exception e) {
            // En cas d'erreur, utiliser le nom Bukkit comme fallback
            String fallback = Bukkit.getServer().getName();
            Main.getInstance().getLogger().warning("Impossible de récupérer le nom du serveur depuis le proxy, utilisation de: " + fallback);
            Main.getInstance().getLogger().warning("Erreur: " + e.getMessage());
            cachedServerName.set(fallback);
            return fallback;
        }

        // Fallback final
        String fallback = Bukkit.getServer().getName();
        cachedServerName.set(fallback);
        return fallback;
    }

    /**
     * Envoie une requête pour obtenir le nom du serveur en envoyant l'IP et le port
     */
    private CompletableFuture<String> requestServerName() {
        CompletableFuture<String> existing = pendingRequest.get();
        if (existing != null && !existing.isDone()) {
            return existing;
        }

        CompletableFuture<String> newRequest = new CompletableFuture<>();
        if (!pendingRequest.compareAndSet(existing, newRequest)) {
            // Another thread created a request, use that one
            return pendingRequest.get();
        }

        // Vérifier à nouveau qu'il y a des joueurs (race condition)
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            newRequest.completeExceptionally(new IllegalStateException("No players online"));
            return newRequest;
        }

        try {
            // Obtenir un joueur pour envoyer le message
            Player player = Bukkit.getOnlinePlayers().iterator().next();

            // Récupérer l'IP et le port du serveur
            String serverIp = Bukkit.getServer().getIp();
            if (serverIp == null || serverIp.isEmpty()) {
                serverIp = "127.0.0.1"; // Fallback pour localhost
            }
            int serverPort = Bukkit.getServer().getPort();

            // Créer le message "GetServerName" avec IP et port
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("GetServerName");
            out.writeUTF(serverIp);
            out.writeInt(serverPort);

            // Envoyer le message au proxy via le canal dungeons:main
            Bukkit.getServer().sendPluginMessage(Main.getInstance(), DUNGEONS_CHANNEL, out.toByteArray());

            Main.getInstance().getLogger().info("Requête GetServerName envoyée - IP: " + serverIp + ":" + serverPort);
        } catch (Exception e) {
            newRequest.completeExceptionally(e);
        }

        return newRequest;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(DUNGEONS_CHANNEL)) {
            return;
        }

        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String subchannel = in.readUTF();

            if (subchannel.equals("ServerName")) {
                String serverName = in.readUTF();
                
                CompletableFuture<String> pending = pendingRequest.get();
                if (pending != null && !pending.isDone()) {
                    pending.complete(serverName);
                }
                
                // Mettre en cache le nom du serveur de manière thread-safe
                cachedServerName.compareAndSet(null, serverName);
                
                Main.getInstance().getLogger().info("Nom du serveur reçu depuis le proxy: " + serverName);
            }
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("Erreur lors de la réception du nom du serveur: " + e.getMessage());
        }
    }

    /**
     * Réinitialise le cache (utile pour les tests ou le rechargement)
     */
    public void clearCache() {
        cachedServerName.set(null);
    }
}
