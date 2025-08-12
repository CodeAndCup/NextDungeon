package fr.perrier.dungeons.messaging.subscribers;

import fr.perrier.dungeons.messaging.packets.InstanceReadyPacket;
import fr.perrier.dungeons.messaging.pidgin.IncomingPacketHandler;
import fr.perrier.dungeons.messaging.pidgin.PacketListener;
import fr.perrier.dungeons.storage.global.FloorInstanceRegistry;

public class InstanceReadySubscriber implements PacketListener {

    @IncomingPacketHandler
    public void onReceive(InstanceReadyPacket packet) {
        if(FloorInstanceRegistry.hasInstance(packet.getFloorInstance().getInstanceId()))
            FloorInstanceRegistry.registerInstance(packet.getFloorInstance());
        else
            throw new RuntimeException("Unknown instance: " + packet.getFloorInstance().getInstanceId());
    }
}
