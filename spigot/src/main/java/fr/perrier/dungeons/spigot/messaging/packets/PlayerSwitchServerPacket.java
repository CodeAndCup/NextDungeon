package fr.perrier.dungeons.spigot.messaging.packets;

import fr.perrier.dungeons.common.messaging.pidgin.Packet;

import java.util.UUID;

public record PlayerSwitchServerPacket(UUID uuid) implements Packet {
}
