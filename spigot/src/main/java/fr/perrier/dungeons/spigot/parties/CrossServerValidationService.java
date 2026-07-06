package fr.perrier.dungeons.spigot.parties;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.messaging.packets.ConsumeRequirementsRequestPacket;
import fr.perrier.dungeons.spigot.messaging.packets.ValidateRequirementsRequestPacket;
import fr.perrier.dungeons.spigot.messaging.packets.ValidateRequirementsResponsePacket;
import fr.perrier.dungeons.spigot.model.Floor;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Routes floor-requirements validation across the cluster.
 *
 * <p>The leader's server owns the decision to start a dungeon, but not every party member is
 * necessarily online on that same server. For each remote member we broadcast a
 * {@link ValidateRequirementsRequestPacket} and wait for a reply from whichever server currently
 * hosts the player. A response sets the future to {@code passed}; a timeout defaults it to
 * {@code false} so a silent peer never blocks the start silently — the UI surfaces it as a
 * requirement failure.</p>
 */
public class CrossServerValidationService {

    private static final long DEFAULT_TIMEOUT_TICKS = 20L * 5L; // 5s — generous enough for packet round-trip

    @Getter
    private static CrossServerValidationService instance;

    private final Map<UUID, CompletableFuture<Boolean>> pending = new ConcurrentHashMap<>();

    public CrossServerValidationService() {
        instance = this;
    }

    /**
     * Broadcasts a validation request and returns a future that completes with the peer's answer
     * or {@code false} on timeout.
     */
    public CompletableFuture<Boolean> validateRemote(String floorId, UUID playerId) {
        UUID requestId = UUID.randomUUID();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        pending.put(requestId, future);

        Bukkit.getScheduler().runTaskLaterAsynchronously(Main.getInstance(), () -> {
            CompletableFuture<Boolean> f = pending.remove(requestId);
            if (f != null && !f.isDone()) {
                Main.getLoggerUtil().info(String.format(
                        "Cross-server validation timeout for player %s on floor %s", playerId, floorId));
                f.complete(false);
            }
        }, DEFAULT_TIMEOUT_TICKS);

        Main.getInstance().getMessaging().sendPacket(
                new ValidateRequirementsRequestPacket(requestId, floorId, playerId));

        return future;
    }

    /**
     * Consumes the floor's required items for every party member at launch time, before any
     * instance loading happens. This is the moment the cost is charged: doing it here (and not
     * when the player joins the instance) removes the window where a player could drop the item
     * during loading to keep it.
     *
     * <p>Members online on this server are consumed inline and their removed items are returned
     * (keyed by UUID) so the caller can refund them if the launch is aborted afterwards. Members
     * on a peer server are consumed there via {@link ConsumeRequirementsRequestPacket} and are
     * not part of the returned refund map.</p>
     *
     * <p>Must run on the main thread (inventory access).</p>
     *
     * @return per-player snapshots of the items removed from <em>local</em> members
     */
    public static Map<UUID, List<ItemStack>> consumeForAllMembers(Floor floor, Set<UUID> memberIds) {
        Map<UUID, List<ItemStack>> localRefunds = new HashMap<>();
        for (UUID memberId : memberIds) {
            Player local = Bukkit.getPlayer(memberId);
            if (local != null) {
                localRefunds.put(memberId, floor.consumeRequiredItems(local));
            } else {
                CrossServerValidationService service = getInstance();
                if (service != null) {
                    service.consumeRemote(floor.getId(), memberId);
                }
            }
        }
        return localRefunds;
    }

    /**
     * Refunds items previously removed by {@link #consumeForAllMembers} to the local members
     * still online here. Used when a launch is aborted after consumption (e.g. the cloud
     * instance failed to provision). Remote members are not refunded.
     */
    public static void refundLocalMembers(Map<UUID, List<ItemStack>> localRefunds) {
        for (Map.Entry<UUID, List<ItemStack>> entry : localRefunds.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) continue;
            for (ItemStack item : entry.getValue()) {
                player.getInventory().addItem(item.clone());
            }
            player.updateInventory();
        }
    }

    /**
     * Broadcasts a consume request for a member hosted on another server. Fire-and-forget: the
     * member already passed validation, so no acknowledgement is awaited.
     */
    public void consumeRemote(String floorId, UUID playerId) {
        Main.getInstance().getMessaging().sendPacket(
                new ConsumeRequirementsRequestPacket(floorId, playerId));
    }

    /**
     * Called by the response subscriber. Completes the pending future if it's still live.
     */
    public void onResponse(ValidateRequirementsResponsePacket packet) {
        CompletableFuture<Boolean> future = pending.remove(packet.requestId());
        if (future != null && !future.isDone()) {
            future.complete(packet.passed());
        }
    }

    public void shutdown() {
        for (CompletableFuture<Boolean> future : pending.values()) {
            if (!future.isDone()) future.complete(false);
        }
        pending.clear();
    }
}
