package fr.perrier.dungeons.messaging.packets;

import fr.perrier.dungeons.model.FloorInstance;
import fr.perrier.dungeons.messaging.pidgin.Packet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class InstanceReadyPacket implements Packet {

    private final FloorInstance floorInstance;
}
