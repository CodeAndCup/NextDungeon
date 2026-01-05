package fr.perrier.dungeons.velocity.messaging;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.perrier.dungeons.velocity.NextDungeonVelocity;

import java.net.InetSocketAddress;

public class PluginMessageVelocity {

    private static final MinecraftChannelIdentifier DUNGEONS_CHANNEL = MinecraftChannelIdentifier.create("dungeons", "main");

    public void initialize() {
        // Initialization logic for Velocity plugin messaging
        NextDungeonVelocity.getInstance().getServer().getChannelRegistrar().register(DUNGEONS_CHANNEL);
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(DUNGEONS_CHANNEL)) {
            return;
        }

        // Mark the event as handled to prevent forwarding
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        byte[] data = event.getData();
        ByteArrayDataInput in = ByteStreams.newDataInput(data);

        try {
            String subchannel = in.readUTF();

            if (subchannel.equals("GetServerName")) {
                String ip = in.readUTF();
                int port = in.readInt();

                String serverName = getServerNameByAddress(ip, port);

                // Send response back to the server
                if (event.getSource() instanceof ServerConnection) {
                    ServerConnection serverConnection = (ServerConnection) event.getSource();
                    sendServerNameResponse(serverConnection, serverName);
                } else if (event.getSource() instanceof Player) {
                    Player player = (Player) event.getSource();
                    player.getCurrentServer().ifPresent(conn -> sendServerNameResponse(conn, serverName));
                }

                NextDungeonVelocity.getInstance().getLogger().info(
                    "Requête GetServerName reçue - IP: " + ip + ":" + port + " -> Serveur: " + serverName
                );
            }
        } catch (Exception e) {
            NextDungeonVelocity.getInstance().getLogger().error(
                "Erreur lors du traitement du message plugin: " + e.getMessage()
            );
        }
    }

    /**
     * Recherche le nom du serveur par son adresse IP et port
     *
     * @param ip   L'adresse IP du serveur
     * @param port Le port du serveur
     * @return Le nom du serveur ou "unknown" si non trouvé
     */
    private String getServerNameByAddress(String ip, int port) {
        for (RegisteredServer server : NextDungeonVelocity.getInstance().getServer().getAllServers()) {
            InetSocketAddress address = server.getServerInfo().getAddress();

            String serverHost = address.getHostString();
            int serverPort = address.getPort();

            // Comparer le port d'abord
            if (serverPort != port) {
                continue;
            }

            // Normaliser les adresses localhost
            String normalizedServerHost = normalizeLocalhost(serverHost);
            String normalizedIp = normalizeLocalhost(ip);

            if (normalizedServerHost.equals(normalizedIp)) {
                return server.getServerInfo().getName();
            }
        }
        return "unknown";
    }

    /**
     * Normalise les adresses localhost (127.0.0.1 et localhost) en une forme commune
     */
    private String normalizeLocalhost(String host) {
        if (host.equals("localhost") || host.equals("127.0.0.1")) {
            return "127.0.0.1";
        }
        return host;
    }

    /**
     * Envoie le nom du serveur en réponse
     *
     * @param serverConnection La connexion serveur pour envoyer la réponse
     * @param serverName       Le nom du serveur trouvé
     */
    private void sendServerNameResponse(ServerConnection serverConnection, String serverName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ServerName");
        out.writeUTF(serverConnection.getServerInfo().getAddress().getHostString());
        out.writeInt(serverConnection.getServerInfo().getAddress().getPort());
        out.writeUTF(serverName);

        serverConnection.sendPluginMessage(DUNGEONS_CHANNEL, out.toByteArray());
    }
}
