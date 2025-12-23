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

    private final AtomicReference<String> cachedServerName = new AtomicReference<>(null);
    private final AtomicReference<CompletableFuture<String>> pendingRequest = new AtomicReference<>(null);

    /**
     * Initialise le service et enregistre le canal de plugin messaging
     */
    public void initialize() {
        // Enregistrer le canal pour BungeeCord/Velocity
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(Main.getInstance(), "BungeeCord");
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(Main.getInstance(), "BungeeCord", this);
        
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
     * Envoie une requête pour obtenir le nom du serveur
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

            // Créer le message "GetServer"
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("GetServer");

            // Envoyer le message au proxy
            player.sendPluginMessage(Main.getInstance(), "BungeeCord", out.toByteArray());
        } catch (Exception e) {
            newRequest.completeExceptionally(e);
        }

        return newRequest;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }

        try {
            ByteArrayDataInput in = ByteStreams.createDataInput(message);
            String subchannel = in.readUTF();

            if (subchannel.equals("GetServer")) {
                String serverName = in.readUTF();
                
                CompletableFuture<String> pending = pendingRequest.get();
                if (pending != null && !pending.isDone()) {
                    pending.complete(serverName);
                }
                
                // Mettre en cache le nom du serveur de manière thread-safe
                cachedServerName.compareAndSet(null, serverName);
                
                Main.getInstance().getLogger().info("Nom du serveur détecté: " + serverName);
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
