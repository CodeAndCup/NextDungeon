package fr.perrier.dungeons.spigot.messaging.packets;

import fr.perrier.dungeons.common.messaging.pidgin.Packet;

import java.util.UUID;

/**
 * Response to a {@link ValidateRequirementsRequestPacket}. The server hosting the player sends
 * this after running {@code Floor.isRequirementsValid} against the local {@code Player}.
 *
 * @param requestId correlation id copied from the original request
 * @param playerId  UUID of the validated player (for logging / safety)
 * @param passed    true if every floor requirement is satisfied
 */
public record ValidateRequirementsResponsePacket(UUID requestId, UUID playerId, boolean passed) implements Packet {
}
