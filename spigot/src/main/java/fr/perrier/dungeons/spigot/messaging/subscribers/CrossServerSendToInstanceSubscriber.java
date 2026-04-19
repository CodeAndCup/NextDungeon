package fr.perrier.dungeons.spigot.messaging.subscribers;

import fr.perrier.dungeons.common.messaging.pidgin.IncomingPacketHandler;
import fr.perrier.dungeons.common.messaging.pidgin.PacketListener;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.messaging.packets.CrossServerSendToInstancePacket;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Runs {@link FloorInstance#sendToServer(Player)} against the local player when the leader's
 * server broadcasts a {@link CrossServerSendToInstancePacket}. Peers that don't have the player
 * online drop the packet — this is a natural filter because exactly one server hosts a player
 * at a time.
 */
public class CrossServerSendToInstanceSubscriber implements PacketListener {

    @IncomingPacketHandler
    public void onReceive(CrossServerSendToInstancePacket packet) {
        Player player = Bukkit.getPlayer(packet.playerId());
        if (player == null) return;

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            FloorInstance instance = Main.getInstance().getDungeonService().getInstance(packet.instanceId());
            if (instance == null) {
                Main.getLoggerUtil().warning(String.format(
                        "CrossServerSendToInstance: no instance %s in Redis — player %s not teleported",
                        packet.instanceId(), player.getName()));
                return;
            }
            instance.sendToServer(player);
        });
    }
}
