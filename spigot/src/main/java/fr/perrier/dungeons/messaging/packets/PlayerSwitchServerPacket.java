package fr.perrier.dungeons.messaging.packets;

import fr.perrier.dungeons.messaging.pidgin.Packet;
import lombok.Getter;

import java.util.UUID;

public record PlayerSwitchServerPacket(UUID uuid) implements Packet {
}
