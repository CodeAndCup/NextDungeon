package fr.perrier.dungeons.messaging.subscribers;

import fr.perrier.dungeons.listener.global.GlobalLeaveListener;
import fr.perrier.dungeons.messaging.packets.PlayerSwitchServerPacket;
import fr.perrier.dungeons.messaging.pidgin.IncomingPacketHandler;
import fr.perrier.dungeons.messaging.pidgin.PacketListener;

public class PlayerSwitchServerSubscriber implements PacketListener {

    /**
     * When a player switches server, if he was in the process of saving his data (waiting for approval),
     * we cancel the save task to avoid saving data for a player who is no longer online.
     * @param packet The received packet.
     */
    @IncomingPacketHandler
    public void onReceive(PlayerSwitchServerPacket packet) {
        if(GlobalLeaveListener.getWaitingApprovalSaveTasks().containsKey(packet.uuid())) {
            GlobalLeaveListener.getWaitingApprovalSaveTasks().get(packet.uuid()).cancel();
            GlobalLeaveListener.getWaitingApprovalSaveTasks().remove(packet.uuid());
        }
    }
}
