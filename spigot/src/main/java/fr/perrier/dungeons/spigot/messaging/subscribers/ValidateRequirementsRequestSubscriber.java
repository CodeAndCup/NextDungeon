package fr.perrier.dungeons.spigot.messaging.subscribers;

import fr.perrier.dungeons.common.messaging.pidgin.IncomingPacketHandler;
import fr.perrier.dungeons.common.messaging.pidgin.PacketListener;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.messaging.packets.ValidateRequirementsRequestPacket;
import fr.perrier.dungeons.spigot.messaging.packets.ValidateRequirementsResponsePacket;
import fr.perrier.dungeons.spigot.model.Floor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Applies a {@link ValidateRequirementsRequestPacket}.
 *
 * <p>Only replies when the target player is currently online on this server — inventory and
 * MMOCore data are inherently server-local, so only the hosting server can validate. Peers
 * that don't have the player simply drop the packet; the leader's timeout handles the case
 * where no server claims the player.</p>
 *
 * <p>Validation runs on the main thread because {@link Floor#isRequirementsValid} touches the
 * player's inventory, which Bukkit guards against off-thread access.</p>
 */
public class ValidateRequirementsRequestSubscriber implements PacketListener {

    @IncomingPacketHandler
    public void onRequest(ValidateRequirementsRequestPacket packet) {
        Player player = Bukkit.getPlayer(packet.playerId());
        if (player == null) {
            // Not our player — another server will reply, or the leader will hit the timeout.
            return;
        }

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            Floor floor = Main.getInstance().getDungeonService().getFloor(packet.floorId());
            boolean passed = floor != null && floor.isRequirementsValid(player);

            Main.getInstance().getMessaging().sendPacket(
                    new ValidateRequirementsResponsePacket(packet.requestId(), packet.playerId(), passed));
        });
    }
}
