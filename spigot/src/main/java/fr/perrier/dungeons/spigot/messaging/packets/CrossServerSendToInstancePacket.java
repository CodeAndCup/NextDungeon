package fr.perrier.dungeons.spigot.messaging.packets;

import fr.perrier.dungeons.common.messaging.pidgin.Packet;

import java.util.UUID;

/**
 * Tells any server currently hosting {@code playerId} to run the usual
 * {@code FloorInstance.sendToServer(Player)} flow (loading bar + readiness wait + teleport)
 * against the given instance. Broadcast from the leader's server so cross-server party members
 * get the same UX as the leader when the dungeon starts.
 *
 * <p>Peers that don't have the player online ignore the packet.</p>
 *
 * @param playerId   UUID of the party member to teleport
 * @param instanceId UUID of the dungeon instance cloud service
 */
public record CrossServerSendToInstancePacket(UUID playerId, UUID instanceId) implements Packet {
}
