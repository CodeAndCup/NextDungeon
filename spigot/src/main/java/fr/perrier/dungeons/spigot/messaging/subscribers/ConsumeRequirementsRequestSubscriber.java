package fr.perrier.dungeons.spigot.messaging.subscribers;

import fr.perrier.dungeons.common.messaging.pidgin.IncomingPacketHandler;
import fr.perrier.dungeons.common.messaging.pidgin.PacketListener;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.messaging.packets.ConsumeRequirementsRequestPacket;
import fr.perrier.dungeons.spigot.model.Floor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Applies a {@link ConsumeRequirementsRequestPacket}.
 *
 * <p>Only acts when the target player is currently online on this server — inventory access is
 * server-local, so only the hosting server can consume. Peers that don't have the player simply
 * drop the packet. The leader already validated every member, so this just removes one of each
 * required item.</p>
 *
 * <p>Runs on the main thread because {@link Floor#consumeRequiredItems} touches the player's
 * inventory, which Bukkit guards against off-thread access.</p>
 */
public class ConsumeRequirementsRequestSubscriber implements PacketListener {

    @IncomingPacketHandler
    public void onRequest(ConsumeRequirementsRequestPacket packet) {
        Player player = Bukkit.getPlayer(packet.playerId());
        if (player == null) {
            // Not our player — another server hosts them, or they went offline.
            return;
        }

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            Floor floor = Main.getInstance().getDungeonService().getFloor(packet.floorId());
            if (floor != null) {
                floor.consumeRequiredItems(player);
            }
        });
    }
}
