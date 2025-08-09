package fr.perrier.dungeons.messaging.subscribers;

import fr.perrier.dungeons.manager.FloorInstance;
import fr.perrier.dungeons.messaging.packets.InstanceReadyPacket;
import fr.perrier.dungeons.messaging.pidgin.IncomingPacketHandler;
import fr.perrier.dungeons.messaging.pidgin.PacketListener;

public class InstanceReadySubscriber implements PacketListener {

    @IncomingPacketHandler
    public void onReceive(InstanceReadyPacket packet) {
        if(FloorInstance.getInstances().containsKey(packet.getInstanceId()))
            FloorInstance.getInstances().get(packet.getInstanceId()).setReady(true);
        else
            throw new RuntimeException("Unknown instance: " + packet.getInstanceId());
    }
}
