package fr.perrier.dungeons.spigot.queue;

import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.utils.ServerUtil;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

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
        
        Main.getInstance().getLogger().info("QueueManager initialized and processor started");
    }

    /**
     * Shuts down the queue manager.
     */
    public void shutdown() {
        if (queueProcessorTask != null) {
            queueProcessorTask.cancel();
        }
        Main.getInstance().getLogger().info("QueueManager shut down");
    }

    /**
     * Adds a player to the queue for a specific floor.
     *
     * @param player the player to add
     * @param floor the floor to queue for
     * @return true if added successfully
     */
    public boolean addPlayerToQueue(Player player, Floor floor) {
        // Check if player is already in queue
        if (queueService.isPlayerInQueue(player.getUniqueId(), floor.getId())) {
            notifyPlayer(player, "You are already in the queue for " + floor.getName());
            return false;
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
            return true;
        }

        return false;
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
        int maxInstances = floor.getRules() != null ? floor.getRules().getMaxInstance() : 0;
        if (maxInstances <= 0) {
            // No limit set
            return true;
        }

        int activeInstances = queueService.getActiveInstanceCount(floor.getId());
        return activeInstances < maxInstances;
    }

    /**
     * Requests an instance for a player, adding them to queue if necessary.
     *
     * @param player the player requesting the instance
     * @param floor the floor to create instance for
     * @return a future that completes with the instance ID, or null if queued
     */
    public CompletableFuture<UUID> requestInstance(Player player, Floor floor) {
        CompletableFuture<UUID> future = new CompletableFuture<>();

        // Check if we can create an instance immediately
        if (canCreateInstance(floor)) {
            // Create instance - it will register itself when it starts
            Main.getInstance().getInstanceProvider().createInstance(floor, false)
                .thenAccept(instanceId -> {
                    if (instanceId != null) {
                        notifyPlayer(player, "Creating your dungeon instance...");
                        future.complete(instanceId);
                    } else {
                        notifyPlayer(player, "Failed to create instance. Please try again.");
                        future.complete(null);
                    }
                });
        } else {
            // Add to queue
            addPlayerToQueue(player, floor);
            future.complete(null);
        }

        return future;
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
                Main.getInstance().getLogger().warning(String.format(
                    "Player %s is offline, skipping queue entry",
                    entry.getPlayerName()
                ));
                return;
            }

            // Create instance for player - it will register itself when it starts
            Main.getInstance().getInstanceProvider().createInstance(floor, false)
                .thenAccept(instanceId -> {
                    if (instanceId != null) {
                        notifyPlayer(player, "Your turn! Creating dungeon instance...");
                        
                        // Send player to instance once it's ready
                        waitForInstanceReady(player, instanceId, floor);
                    } else {
                        notifyPlayer(player, "Failed to create instance. Please try again.");
                    }
                });
        });
    }

    /**
     * Waits for an instance to be ready and then sends the player to it.
     *
     * @param player the player to send
     * @param instanceId the instance ID
     * @param floor the floor
     */
    private void waitForInstanceReady(Player player, UUID instanceId, Floor floor) {
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            Main.getInstance().getInstanceProvider().sendPlayerToInstance(player, instanceId)
                .thenAccept(success -> {
                    if (success) {
                        notifyPlayer(player, "Teleporting to " + floor.getName() + "...");
                    } else {
                        notifyPlayer(player, "Failed to teleport to instance.");
                    }
                });
        }, 100L); // Wait 5 seconds for instance to be ready
    }

    /**
     * Notifies a player with a message.
     *
     * @param player the player to notify
     * @param message the message to send
     */
    public void notifyPlayer(Player player, String message) {
        String configType = Main.getInstance().getConfig().getString("NotificationConfiguration.type", "ACTION_BAR");
        NotificationType type;
        
        try {
            type = NotificationType.valueOf(configType.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = NotificationType.ACTION_BAR;
        }

        String formattedMessage = ChatUtil.translate(Main.getPrefix() + message);

        switch (type) {
            case ACTION_BAR -> {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(formattedMessage));
            }
            case CHAT -> {
                player.sendMessage(formattedMessage);
            }
            case TITLE -> {
                player.sendTitle("", formattedMessage, 10, 70, 20);
            }
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
