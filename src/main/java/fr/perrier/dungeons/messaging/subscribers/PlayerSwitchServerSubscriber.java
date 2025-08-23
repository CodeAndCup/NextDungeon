package fr.perrier.dungeons.messaging.subscribers;

import fr.perrier.dungeons.listener.LeaveListener;
import fr.perrier.dungeons.messaging.packets.PlayerSwitchServerPacket;
import fr.perrier.dungeons.messaging.pidgin.IncomingPacketHandler;
import fr.perrier.dungeons.messaging.pidgin.PacketListener;

public class PlayerSwitchServerSubscriber implements PacketListener {

    @IncomingPacketHandler
    public void onReceive(PlayerSwitchServerPacket packet) {
        if(LeaveListener.getWaitingApprovalSaveTasks().containsKey(packet.getUuid())) {
            LeaveListener.getWaitingApprovalSaveTasks().get(packet.getUuid()).cancel();
            LeaveListener.getWaitingApprovalSaveTasks().remove(packet.getUuid());
        }
    }
}
