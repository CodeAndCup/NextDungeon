package fr.perrier.dungeons.spigot.messaging.subscribers;

import fr.perrier.dungeons.common.messaging.pidgin.IncomingPacketHandler;
import fr.perrier.dungeons.common.messaging.pidgin.PacketListener;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.messaging.packets.DungeonPartyJoinRequestPacket;
import fr.perrier.dungeons.spigot.parties.impl.DungeonPartyImpl;
import fr.perrier.dungeons.spigot.parties.impl.DungeonPartyRegistry;
import org.bukkit.Bukkit;

import java.util.UUID;

/**
 * Applies a {@link DungeonPartyJoinRequestPacket} when this server owns the target party.
 *
 * <p>Runs the mutation on the main thread because {@link DungeonPartyImpl#addMember} ends up
 * touching the underlying {@code IParty}, which for some providers (Parties plugin) must be
 * invoked synchronously. The joining player's display metadata (name, MMOCore class, level) is
 * cached on the owner server so the next published {@code DungeonPartyData} carries the
 * cross-server member's info — otherwise the leader sees only a truncated UUID.</p>
 */
public class DungeonPartyJoinRequestSubscriber implements PacketListener {

    @IncomingPacketHandler
    public void onJoinRequest(DungeonPartyJoinRequestPacket packet) {
        DungeonPartyRegistry registry = DungeonPartyRegistry.getInstance();
        if (registry == null) return;

        UUID currentServiceId = Main.getInstance().getInstanceProvider().getCurrentServiceUniqueId();
        if (currentServiceId == null || !currentServiceId.equals(packet.ownerServiceId())) {
            // Not our party — ignore.
            return;
        }

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            DungeonPartyImpl party = DungeonPartyImpl.getDungeonPartyOf(packet.leaderId());
            if (party == null) {
                Main.getLoggerUtil().warning(String.format(
                        "Received join request for unknown local party (leader=%s, player=%s)",
                        packet.leaderId(), packet.joiningPlayerId()));
                return;
            }
            party.cacheRemoteMemberInfo(
                    packet.joiningPlayerId(),
                    packet.joiningPlayerName(),
                    packet.joiningPlayerClass(),
                    packet.joiningPlayerLevel()
            );
            boolean added = party.addMember(packet.joiningPlayerId());
            if (added) {
                Main.getLoggerUtil().info(String.format(
                        "Cross-server join applied: %s (%s) → party led by %s",
                        packet.joiningPlayerId(), packet.joiningPlayerName(), packet.leaderId()));
            }
        });
    }
}
