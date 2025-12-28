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
 * Service for obtaining the server name from the proxy (BungeeCord/Velocity)
 * via the plugin messaging system.
 */
public class ServerNameService implements PluginMessageListener {

    private static final String DUNGEONS_CHANNEL = "dungeons:main";

    private final AtomicReference<String> cachedServerName = new AtomicReference<>(null);
    private final AtomicReference<CompletableFuture<String>> pendingRequest = new AtomicReference<>(null);

    /**
     * Initializes the service and registers the plugin messaging channel.
     */
    public void initialize() {
        // Enregistrer le canal pour dungeons:main (communication avec Velocity)
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(Main.getInstance(), DUNGEONS_CHANNEL);
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(Main.getInstance(), DUNGEONS_CHANNEL, this);

        Main.getInstance().getLogger().info("Service de récupération du nom de serveur initialisé");
    }

    /**
     * Gets the server name, either from cache or by requesting it from the proxy.
     *
     * @return a CompletableFuture that will be completed with the server name
     */
    public CompletableFuture<String> getServerName() {
        String cached = cachedServerName.get();
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        CompletableFuture<String> future;
        try {
            future = requestServerName();
            if (future != null) {
                return future.thenApply(serverName -> {
                    cachedServerName.set(serverName);
                    Main.getInstance().getLogger().info("Server name retrieved: " + serverName);
                    return serverName;
                }).exceptionally(e -> {
                    String fallback = Bukkit.getServer().getName();
                    Main.getInstance().getLogger().severe("Unable to retrieve the server name from the proxy, using: " + fallback);
                    Main.getInstance().getLogger().severe("Error: " + e.getMessage());
                    cachedServerName.set(fallback);
                    return fallback;
                });
            }
        } catch (Exception e) {
            String fallback = Bukkit.getServer().getName();
            Main.getInstance().getLogger().severe("Unable to retrieve the server name from the proxy, using: " + fallback);
            Main.getInstance().getLogger().severe("Error: " + e.getMessage());
            cachedServerName.set(fallback);
            return CompletableFuture.completedFuture(fallback);
        }

        String fallback = Bukkit.getServer().getName();
        Main.getInstance().getLogger().severe("Unable to retrieve the server name from the proxy, using: " + fallback);
        Main.getInstance().getLogger().severe("Error: requestServerName returned null");
        cachedServerName.set(fallback);
        return CompletableFuture.completedFuture(fallback);
    }

    /**
     * Sends a request to the proxy to get the server name.
     *
     * @return a CompletableFuture that will be completed when the server name is received
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
                
                Main.getInstance().getLogger().info("Server name received from the proxy: " + serverName);
            }
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("&eError receiving server name: " + e.getMessage());
        }
    }

    /**
     * Clears the cache (useful for tests or reloads).
     */
    public void clearCache() {
        cachedServerName.set(null);
    }
}
