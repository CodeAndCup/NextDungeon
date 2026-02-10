package fr.perrier.dungeons.spigot.messaging.subscribers;

import fr.perrier.dungeons.common.messaging.pidgin.IncomingPacketHandler;
import fr.perrier.dungeons.common.messaging.pidgin.PacketListener;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.messaging.packets.CancelInstancePacket;
import org.bukkit.Bukkit;

public class CancelInstanceSubscriber implements PacketListener {

    @IncomingPacketHandler
    public void onCancelInstance(CancelInstancePacket packet) {
        if(Main.getInstance().getDungeonService().getCurrentInstance().getInstanceId().equals(packet.getInstanceId())) {
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                Main.getInstance().getDungeonService().removeInstance(packet.getInstanceId());
                Bukkit.shutdown();
            }, 20L * 5L);
        }
    }
}
