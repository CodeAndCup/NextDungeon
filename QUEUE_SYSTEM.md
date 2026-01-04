# Global Dungeon Queue System

## Overview

The global dungeon queue system manages player access to dungeon instances when the maximum number of instances is reached. The queue is synchronized across all Spigot servers via Redis, ensuring fair distribution of dungeon access.

## Configuration

### Notification Method

Configure how players receive queue notifications in `config.yml`:

```yaml
NotificationConfiguration:
    type: "ACTION_BAR"  # Options: ACTION_BAR, CHAT, TITLE
```

### Max Instance Limit

Set the maximum number of concurrent instances for each floor in the dungeon configuration file:

```yaml
rules:
  max_instance: 2  # Maximum concurrent instances (0 or omitted = unlimited)
```

## Player Commands

- `/dungeon join <floor_id>` - Join the queue for a specific dungeon floor
- `/dungeon leave <floor_id>` - Leave the queue for a specific floor
- `/dungeon status` - Check your current position in all queues
- `/dungeon list` - List all available dungeons with queue information

## Admin Commands

- `/dungeon admin queue` - Show queue management commands
- `/dungeon admin queue status` - View queue status for all floors
- `/dungeon admin queue list <floor_id>` - List all players in queue for a floor
- `/dungeon admin queue clear <floor_id>` - Clear the queue for a specific floor

## API Access

The queue system provides API access for external systems (e.g., dashboard) through the `QueueAPI` class:

```java
// Get all queue information
Map<String, QueueInfo> queues = QueueAPI.getAllQueues();

// Get queue information for a specific floor
QueueInfo info = QueueAPI.getQueueInfo("floor_id");

// Get overall statistics
QueueStats stats = QueueAPI.getQueueStats();
```

## How It Works

### Queue Flow

1. **Player Request**: When a player requests to join a dungeon:
   - System checks if max instances is reached for that floor
   - If space available: Instance is created immediately
   - If limit reached: Player is added to the global queue

2. **Queue Processing**: A background task runs every second:
   - Checks each floor's queue
   - If instances become available, processes next player in queue
   - Creates instance and teleports player automatically

3. **Instance Registration**: When an instance starts:
   - Automatically registers with the queue system
   - Counts toward the floor's max instance limit
   - Unregisters when instance shuts down

### Notifications

Players receive notifications at key points:
- When added to queue (with position)
- When their turn arrives (instance creating)
- When being teleported to instance
- Configurable via `NotificationConfiguration.type`

### Cross-Server Synchronization

- Queue data stored in Redis (globally accessible)
- Player can be in queue on any server
- Instance creation triggered from any server
- Queue position visible across all servers

## Technical Details

### Components

1. **DungeonQueueService**: Manages Redis queue operations
   - Add/remove players from queue
   - Track instance counts per floor
   - Queue position lookups

2. **QueueManager**: Handles queue processing and notifications
   - Background queue processor
   - Player notification system
   - Instance creation coordination

3. **QueueAPI**: Provides read-only access for external systems
   - Dashboard integration
   - Monitoring and statistics

### Redis Data Structures

- `dungeons:queue:<floor_id>` - Deque of QueueEntry objects
- `dungeons:instance_count:<floor_id>` - Map of active instance IDs

### Lifecycle

**Lobby Server**:
- Initializes queue services
- Runs queue processor
- Handles player commands

**Instance Server**:
- Registers with queue on startup
- Unregisters on shutdown
- No queue processing

## Example Scenario

1. Floor has `max_instance: 2`
2. Player1 requests dungeon → Instance1 created (1/2)
3. Player2 requests dungeon → Instance2 created (2/2)
4. Player3 requests dungeon → Added to queue (position 1/1)
5. Player4 requests dungeon → Added to queue (position 2/2)
6. Instance1 completes → Player3 automatically processed (creates Instance3)
7. Instance2 completes → Player4 automatically processed (creates Instance4)

## Notes

- Queue does not persist on server restart (lives in memory/Redis)
- No special handling for disconnects (player removed from queue)
- If a player is offline when their turn arrives, they are skipped
- Queue is FIFO (First In, First Out)
- All messages are in English as per requirements
