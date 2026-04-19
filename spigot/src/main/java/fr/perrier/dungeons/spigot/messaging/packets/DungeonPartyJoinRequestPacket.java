package fr.perrier.dungeons.spigot.messaging.packets;

import fr.perrier.dungeons.common.messaging.pidgin.Packet;

import java.util.UUID;

/**
 * Sent by a lobby when a player clicks "join" on a party that is hosted by another lobby.
 *
 * <p>The packet is broadcast cluster-wide; only the server whose cloud service UUID matches
 * {@code ownerServiceId} is expected to apply the mutation. Other servers discard it.</p>
 *
 * <p>Carries the joining player's display fields (name, MMOCore class, MMOCore level) because
 * the owner server cannot resolve them on its own — those live on the joiner's home server.
 * The owner caches them so the published {@code DungeonPartyData} snapshot shown in every
 * lobby's party finder includes the new member with correct info.</p>
 *
 * @param leaderId            UUID of the party leader (registry key)
 * @param joiningPlayerId     UUID of the player trying to join
 * @param ownerServiceId      UUID of the cloud service that owns the live IParty
 * @param joiningPlayerName   Joining player's display name, resolved on the joiner's server
 * @param joiningPlayerClass  MMOCore class name (may be null if MMOCore data is missing)
 * @param joiningPlayerLevel  MMOCore level (0 when unknown)
 */
public record DungeonPartyJoinRequestPacket(
        UUID leaderId,
        UUID joiningPlayerId,
        UUID ownerServiceId,
        String joiningPlayerName,
        String joiningPlayerClass,
        int joiningPlayerLevel
) implements Packet {
}
