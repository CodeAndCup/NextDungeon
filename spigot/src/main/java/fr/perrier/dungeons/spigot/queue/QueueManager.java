package fr.perrier.dungeons.spigot.queue;

import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.common.queue.QueueEntry;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import fr.perrier.dungeons.spigot.parties.CrossServerValidationService;
import fr.perrier.dungeons.spigot.parties.impl.DungeonPartyImpl;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Manages the dungeon queue and player notifications.
 * This class handles queue processing and player interactions.
 */
@RequiredArgsConstructor
public class QueueManager {
    private final DungeonQueueService queueService;
    private BukkitTask queueProcessorTask;

    /**
     * Initializes the queue manager and starts the queue processor.
     */
    public void initialize() {
        // Start queue processor that runs every second
        queueProcessorTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
            Main.getInstance(),
            this::processQueues,
            20L, // Initial delay (1 second)
            20L  // Period (1 second)
        );
        
        Main.getLoggerUtil().info("QueueManager initialized and processor started");
    }

    /**
     * Shuts down the queue manager.
     */
    public void shutdown() {
        if (queueProcessorTask != null) {
            queueProcessorTask.cancel();
        }
        Main.getLoggerUtil().info("QueueManager shut down");
    }

    /**
     * Adds a player to the queue for a specific floor.
     *
     * @param player the player to add
     * @param floor  the floor to queue for
     */
    public void addPlayerToQueue(Player player, Floor floor) {
        // Check if player is already in queue
        if (queueService.isPlayerInQueue(player.getUniqueId(), floor.getId())) {
            notifyPlayer(player, "You are already in the queue for " + floor.getName());
            return;
        }

        QueueEntry entry = new QueueEntry(
            player.getUniqueId(),
            player.getName(),
            floor.getId(),
            Bukkit.getServer().getName()
        );

        if (queueService.addToQueue(entry)) {
            QueuePosition position = queueService.getQueuePosition(player.getUniqueId(), floor.getId());
            if (position != null) {
                notifyPlayer(player, String.format(
                    "Added to queue for %s - Position: %d/%d",
                    floor.getName(),
                    position.getPosition(),
                    position.getTotalInQueue()
                ));
            }
        }

    }

    /**
     * Removes a player from the queue.
     *
     * @param player the player to remove
     * @param floorId the floor ID
     * @return true if removed successfully
     */
    public boolean removePlayerFromQueue(Player player, String floorId) {
        if (queueService.removeFromQueue(player.getUniqueId(), floorId)) {
            notifyPlayer(player, "Removed from queue");
            return true;
        }
        return false;
    }

    /**
     * Gets a player's position in the queue.
     *
     * @param playerId the player's UUID
     * @param floorId the floor ID
     * @return the queue position, or null if not in queue
     */
    public QueuePosition getPlayerPosition(UUID playerId, String floorId) {
        return queueService.getQueuePosition(playerId, floorId);
    }

    /**
     * Checks if instances are available for a floor.
     *
     * @param floor the floor to check
     * @return true if an instance can be created
     */
    public boolean canCreateInstance(Floor floor) {
        if(floor.getRules() == null)
            throw new IllegalStateException("Floor rules are not defined for floor: " + floor.getId());

        int maxInstances = floor.getRules().getMaxInstance();
        if (maxInstances <= 0) {
            // No limit set
            return true;
        }

        int activeInstances = queueService.getActiveInstanceCount(floor.getId());
        return activeInstances < maxInstances;
    }

    /**
     * Checks if any player in the set meets the floor requirements.
     *
     * @param players the set of player UUIDs to check
     * @param floor the floor to check against
     * @return true if at least one player meets the requirements
     */
    public boolean isNotMatchingRequirements(Set<UUID> players, Floor floor) {
        return players.stream().noneMatch(uuid -> floor.isRequirementsValid(Bukkit.getPlayer(uuid)));
    }

    /**
     * Validates every party member against the floor's requirements, routing cross-server
     * members through {@link CrossServerValidationService}. Returns a future that completes
     * with {@code true} only if every member passes.
     */
    public CompletableFuture<Boolean> validateAllMembersAsync(Floor floor, Set<UUID> memberIds) {
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (UUID memberId : memberIds) {
            Player local = Bukkit.getPlayer(memberId);
            if (local != null) {
                futures.add(CompletableFuture.completedFuture(floor.isRequirementsValid(local)));
            } else {
                CrossServerValidationService service = CrossServerValidationService.getInstance();
                futures.add(service != null
                        ? service.validateRemote(floor.getId(), memberId)
                        : CompletableFuture.completedFuture(false));
            }
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().allMatch(CompletableFuture::join));
    }

    /**
     * Requests an instance for a player, adding them to queue if necessary.
     * <p>
     * Note: There is a potential race condition between checking instance availability
     * and creating the instance. In practice, this is acceptable as the queue processor
     * will handle the player when an instance becomes available.
     *
     * @param player the player requesting the instance
     * @param floor  the floor to create instance for
     */
    public void requestInstance(Player player, Floor floor) {
        DungeonPartyImpl party = DungeonPartyImpl.getDungeonPartyOf(player);
        Set<UUID> memberIds = party.getMemberIds();

        validateAllMembersAsync(floor, memberIds).thenAccept(allPassed -> {
            if (!allPassed) {
                Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000One or more players in your party break the requirements to enter this floor.")));
                return;
            }

            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                // Check if we can create an instance immediately
                if (canCreateInstance(floor)) {
                    UUID leaderId = party.getLeaderId();
                    if (Main.getInstance().getDungeonService().isPlayerInAnyInstance(leaderId)) {
                        notifyPlayer(player, "An instance for your party is already being prepared or active. Please wait...");
                        return;
                    }

                    FloorInstance.generateNewInstanceAsync(floor.getId(), memberIds, false, floorInstance -> {
                        if (floorInstance == null) {
                            notifyPlayer(player, "Failed to create the dungeon instance. Please try again.");
                            return;
                        }
                        // Pull the party out of the finder as soon as the dungeon starts —
                        // the party itself stays alive so members return together afterwards.
                        party.setListed(false);
                        floorInstance.sendToServer(party);
                    });
                } else {
                    // Add to queue
                    addPlayerToQueue(player, floor);
                }
            });
        });
    }

    /**
     * Processes all active queues.
     * This method is called periodically by the queue processor.
     */
    private void processQueues() {
        for (String floorId : queueService.getActiveQueueFloors()) {
            processQueueForFloor(floorId);
        }
    }

    /**
     * Processes the queue for a specific floor.
     *
     * @param floorId the floor ID
     */
    private void processQueueForFloor(String floorId) {
        Floor floor = Floor.getFloor(floorId);
        if (floor == null) {
            return;
        }

        // Check if we can create an instance
        if (!canCreateInstance(floor)) {
            return;
        }

        // Get next player from queue
        QueueEntry entry = queueService.pollNext(floorId);
        if (entry == null) {
            return;
        }

        // Try to find the player and create instance
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            Player player = Bukkit.getPlayer(entry.getPlayerId());
            if (player == null || !player.isOnline()) {
                // Player is offline, skip
                Main.getLoggerUtil().warning(String.format(
                    "Player %s is offline, skipping queue entry",
                    entry.getPlayerName()
                ));
                return;
            }

            // Create instance for player - it will register itself when it starts
            UUID leaderId = DungeonPartyImpl.getDungeonPartyOf(player).getLeaderId();
            if (Main.getInstance().getDungeonService().isPlayerInAnyInstance(leaderId)) {
                notifyPlayer(player, "An instance for your party is already being prepared or active. Please wait...");
                return;
            }

            DungeonPartyImpl party = DungeonPartyImpl.getDungeonPartyOf(player);
            Set<UUID> memberIds = party.getMemberIds();
            validateAllMembersAsync(floor, memberIds).thenAccept(allPassed -> {
                if (!allPassed) {
                    Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000One or more players in your party break the requirements during the loading phase. The instance has been cancelled.")));
                    return;
                }
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    FloorInstance.generateNewInstanceAsync(floor.getId(), memberIds, false, floorInstance -> {
                        if (floorInstance == null) {
                            notifyPlayer(player, "Failed to create the dungeon instance. Please try again.");
                            return;
                        }
                        notifyPlayer(player, "Your turn! Creating dungeon instance...");
                        party.setListed(false);
                        floorInstance.sendToServer(party);
                    });
                });
            });
        });

        queueService.executeForEachInQueue(floorId, (queueEntry) -> {
            Player queuedPlayer = Bukkit.getPlayer(queueEntry.getPlayerId());
            if (queuedPlayer != null && queuedPlayer.isOnline()) {
                QueuePosition queuePosition = queueService.getQueuePosition(queuedPlayer.getUniqueId(), floorId);
                notifyPlayer(queuedPlayer, String.format(
                    "Queue Update for %s - Position: %d/%d",
                    floor.getName(),
                    queuePosition.getPosition(),
                    queuePosition.getTotalInQueue()
                ));
            }
        });
    }

    /**
     * Notifies a player with a message.
     *
     * @param player the player to notify
     * @param message the message to send
     */
    public void notifyPlayer(Player player, String message) {
        String configType = Objects.requireNonNull(Main.getInstance().getConfig().getString("NotificationConfiguration.type"));
        NotificationType type;
        
        try {
            type = NotificationType.valueOf(configType.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = NotificationType.ACTION_BAR;
        }

        String formattedMessage = ChatUtil.translate(Main.getPrefix() + message);

        switch (type) {
            case ACTION_BAR -> player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(formattedMessage, ChatColor.WHITE));
            case CHAT -> player.sendMessage(formattedMessage);
            case TITLE -> player.sendTitle("", formattedMessage, 10, 70, 20);
        }
    }

    /**
     * Unregisters an instance when it's closed.
     *
     * @param floorId the floor ID
     * @param instanceId the instance ID
     */
    public void unregisterInstance(String floorId, UUID instanceId) {
        queueService.unregisterInstance(floorId, instanceId);
    }
}
