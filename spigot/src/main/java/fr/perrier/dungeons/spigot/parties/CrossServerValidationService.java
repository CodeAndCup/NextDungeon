package fr.perrier.dungeons.spigot.parties;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.messaging.packets.ValidateRequirementsRequestPacket;
import fr.perrier.dungeons.spigot.messaging.packets.ValidateRequirementsResponsePacket;
import lombok.Getter;
import org.bukkit.Bukkit;

import java.util.Map;
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
