package fr.perrier.dungeons.messaging.packets;

import fr.perrier.dungeons.manager.FloorInstance;
import fr.perrier.dungeons.messaging.pidgin.Packet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class InstanceReadyPacket implements Packet {

    private final FloorInstance floorInstance;
}
