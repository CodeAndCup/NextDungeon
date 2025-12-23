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

/**
 * Service pour obtenir le nom du serveur depuis le proxy (BungeeCord/Velocity)
 * via le système de plugin messaging
 */
public class ServerNameService implements PluginMessageListener {

    private static String cachedServerName = null;
    private static CompletableFuture<String> pendingRequest = null;

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
        if (cachedServerName != null) {
            return cachedServerName;
        }

        // Si aucun joueur n'est connecté, utiliser un fallback
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            // Fallback vers le nom Bukkit si aucun joueur n'est connecté
            String fallback = Bukkit.getServer().getName();
            Main.getInstance().getLogger().warning("Aucun joueur connecté pour récupérer le nom du serveur, utilisation de: " + fallback);
            return fallback;
        }

        // Requête synchrone avec timeout
        try {
            CompletableFuture<String> future = requestServerName();
            cachedServerName = future.get(5, TimeUnit.SECONDS);
            Main.getInstance().getLogger().info("Nom du serveur récupéré: " + cachedServerName);
            return cachedServerName;
        } catch (Exception e) {
            // En cas d'erreur, utiliser le nom Bukkit comme fallback
            String fallback = Bukkit.getServer().getName();
            Main.getInstance().getLogger().warning("Impossible de récupérer le nom du serveur depuis le proxy, utilisation de: " + fallback);
            Main.getInstance().getLogger().warning("Erreur: " + e.getMessage());
            cachedServerName = fallback;
            return fallback;
        }
    }

    /**
     * Envoie une requête pour obtenir le nom du serveur
     */
    private CompletableFuture<String> requestServerName() {
        if (pendingRequest != null && !pendingRequest.isDone()) {
            return pendingRequest;
        }

        pendingRequest = new CompletableFuture<>();

        // Obtenir un joueur pour envoyer le message
        Player player = Bukkit.getOnlinePlayers().iterator().next();

        // Créer le message "GetServer"
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("GetServer");

        // Envoyer le message au proxy
        player.sendPluginMessage(Main.getInstance(), "BungeeCord", out.toByteArray());

        return pendingRequest;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.createDataInput(message);
        String subchannel = in.readUTF();

        if (subchannel.equals("GetServer")) {
            String serverName = in.readUTF();
            
            if (pendingRequest != null && !pendingRequest.isDone()) {
                pendingRequest.complete(serverName);
            }
            
            // Mettre en cache le nom du serveur
            if (cachedServerName == null) {
                cachedServerName = serverName;
                Main.getInstance().getLogger().info("Nom du serveur détecté: " + serverName);
            }
        }
    }

    /**
     * Réinitialise le cache (utile pour les tests ou le rechargement)
     */
    public void clearCache() {
        cachedServerName = null;
    }
}
