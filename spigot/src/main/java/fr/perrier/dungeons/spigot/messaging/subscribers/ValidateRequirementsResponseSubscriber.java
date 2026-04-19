package fr.perrier.dungeons.spigot.messaging.subscribers;

import fr.perrier.dungeons.common.messaging.pidgin.IncomingPacketHandler;
import fr.perrier.dungeons.common.messaging.pidgin.PacketListener;
import fr.perrier.dungeons.spigot.messaging.packets.ValidateRequirementsResponsePacket;
import fr.perrier.dungeons.spigot.parties.CrossServerValidationService;

/**
 * Feeds a {@link ValidateRequirementsResponsePacket} back to the leader-side
 * {@link CrossServerValidationService} so the pending future completes.
 *
 * <p>Responses are broadcast on the same topic as requests, so every server receives them. Peers
 * that never issued the matching request have no pending entry for {@code requestId} and the
 * service quietly ignores the packet.</p>
 */
public class ValidateRequirementsResponseSubscriber implements PacketListener {

    @IncomingPacketHandler
    public void onResponse(ValidateRequirementsResponsePacket packet) {
        CrossServerValidationService service = CrossServerValidationService.getInstance();
        if (service != null) service.onResponse(packet);
    }
}
