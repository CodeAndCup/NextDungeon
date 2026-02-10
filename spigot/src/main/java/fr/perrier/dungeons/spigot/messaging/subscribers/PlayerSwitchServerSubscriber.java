package fr.perrier.dungeons.spigot.messaging.subscribers;

import fr.perrier.dungeons.spigot.listener.global.GlobalLeaveListener;
import fr.perrier.dungeons.spigot.messaging.packets.PlayerSwitchServerPacket;
import fr.perrier.dungeons.common.messaging.pidgin.IncomingPacketHandler;
import fr.perrier.dungeons.common.messaging.pidgin.PacketListener;

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
