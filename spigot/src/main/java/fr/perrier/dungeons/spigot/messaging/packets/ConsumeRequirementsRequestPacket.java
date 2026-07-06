package fr.perrier.dungeons.spigot.messaging.packets;

import fr.perrier.dungeons.common.messaging.pidgin.Packet;

import java.util.UUID;

/**
 * Broadcast by the leader's server at launch time to consume a remote party member's required
 * items before any loading happens. Only the server currently hosting the player acts on it
 * (see {@code ConsumeRequirementsRequestSubscriber}); peers without the player ignore it.
 *
 * <p>Fire-and-forget: the leader has already validated every member, so no response is needed.
 * Consuming on the lobby server before the instance is provisioned closes the window where a
 * player could drop the item during loading to dodge the cost.</p>
 *
 * @param floorId  id of the floor whose required items must be consumed
 * @param playerId UUID of the party member whose items are consumed
 */
public record ConsumeRequirementsRequestPacket(String floorId, UUID playerId) implements Packet {
}
