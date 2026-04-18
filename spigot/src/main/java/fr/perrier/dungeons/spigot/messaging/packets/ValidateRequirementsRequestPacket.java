package fr.perrier.dungeons.spigot.messaging.packets;

import fr.perrier.dungeons.common.messaging.pidgin.Packet;

import java.util.UUID;

/**
 * Broadcast by the leader's server when it needs to validate that a party member meets the
 * requirements for a floor. Only the server that currently hosts the player replies (with
 * {@link ValidateRequirementsResponsePacket}); peers without the player ignore the packet.
 *
 * @param requestId unique id used to correlate the response
 * @param floorId   id of the floor whose requirements must be checked
 * @param playerId  UUID of the party member being validated
 */
public record ValidateRequirementsRequestPacket(UUID requestId, String floorId, UUID playerId) implements Packet {
}
