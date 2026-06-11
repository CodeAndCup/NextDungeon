package fr.perrier.dungeons.spigot.messaging.subscribers;

import fr.perrier.dungeons.common.messaging.pidgin.IncomingPacketHandler;
import fr.perrier.dungeons.common.messaging.pidgin.PacketListener;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.messaging.packets.CancelInstancePacket;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import fr.perrier.dungeons.spigot.storage.DungeonService;
import org.bukkit.Bukkit;

public class CancelInstanceSubscriber implements PacketListener {

    @IncomingPacketHandler
    public void onCancelInstance(CancelInstancePacket packet) {
        DungeonService dungeonService = Main.getInstance().getDungeonService();

        // The adapter is registered cluster-wide, but lobby servers have no current instance —
        // getCurrentInstance() would throw IllegalStateException there. Guard first.
        if (!dungeonService.hasActiveInstance()) return;

        FloorInstance current = dungeonService.getCurrentInstance();
        if (current.getInstanceId().equals(packet.getInstanceId())) {
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                dungeonService.removeInstance(packet.getInstanceId());
                Bukkit.shutdown();
            }, 20L * 5L);
        }
    }
}
