package fr.perrier.dungeons.spigot.queue;

import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.common.queue.QueueEntry;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

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
        // Check if we can create an instance immediately
        if (canCreateInstance(floor)) {
            // Create instance - it will register itself when it starts
            FloorInstance.generateNewInstanceAsync(floor.getId(),false, floorInstance -> {
                floorInstance.sendToServer(player);
            });
        } else {
            // Add to queue
            addPlayerToQueue(player, floor);
        }
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
            FloorInstance.generateNewInstanceAsync(floor.getId(),false, floorInstance -> {
                notifyPlayer(player, "Your turn! Creating dungeon instance...");
                floorInstance.sendToServer(player);
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
        String configType = Main.getInstance().getConfig().getString("NotificationConfiguration.type", "CHAT");
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
