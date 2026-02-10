package fr.perrier.dungeons.spigot.messaging.packets;

import fr.perrier.dungeons.common.messaging.pidgin.Packet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class CancelInstancePacket implements Packet {
    private final UUID instanceId;
}
