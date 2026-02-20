---
description: How to integrate NextDungeon with CloudNet for dynamic dungeon instance management.
icon: cloud
---

# CloudNet Integration

CloudNet is the backbone of NextDungeon's instance management system. It spins up isolated Minecraft server instances for each dungeon floor run, providing players with a dedicated, lag-free environment.

## Requirements

* **CloudNet version**: 4.0.0-RC13 or newer
* **CloudNet Bridge module**: Required for the `PlayerManager` API used to send players between servers
* **Java 24 or 25**: Required by CloudNet itself (separate from the Minecraft server Java version)

> NextDungeon uses `CloudNetProvider` (`spigot/src/main/java/fr/perrier/dungeons/spigot/instance/impl/CloudNetProvider.java`). This is the only `InstanceProvider` type supported in version `1.0.4-SNAPSHOT`.

---

## How It Works

<!-- INSERT HERE: diagram showing CloudNet instance lifecycle: lobby → queue → CloudNet task → instance server → cleanup -->

1. **Instance Creation**: When a player (or party) is ready to enter a floor, `CloudNetProvider.createInstance(floor, editMode)` is called asynchronously. It looks up a CloudNet **task** matching the floor ID and creates a new service from it.

2. **Metadata Injection**: The following properties are injected into the CloudNet service at creation time:

   | Property | Type | Value |
   |----------|------|-------|
   | `isDungeonInstance` | boolean | `true` |
   | `floorId` | string | The floor's full ID (e.g. `example_floor1`) |
   | `createdAt` | string | ISO-8601 timestamp |
   | `editMode` | boolean | `true` only during edit sessions |

3. **Readiness Detection**: The instance server's `Main.onEnable()` calls `initializeInstanceServer()`, which reads these properties via `ServerUtil.getInstanceInfo()`. After a short delay (100 ticks ≈ 5 s) the instance sets `FloorInstance.ready = true` in Redis.

4. **Player Routing**: The lobby server polls Redis for the instance's `ready` state. Once ready, it calls `ServerUtil.sendToServer(player, instanceId)`, which uses the CloudNet Bridge `PlayerManager` to transfer the player.

5. **Cleanup**: When the dungeon ends (`FloorInstance.complete()` or `FloorInstance.fail()`), the instance removes itself from Redis (`dungeonService.removeInstance(instanceId)`) and calls `Bukkit.shutdown()`. CloudNet then handles the server teardown.

---

## Setting Up CloudNet Tasks

Every floor that will run as a CloudNet instance needs a matching **CloudNet task** named `<dungeonId>_<floorId>`.

### Step 1: Create the Task

In the CloudNet management panel (or CLI):

```
# CloudNet CLI example
task create my_dungeon_floor1
```

Set:
* **Task name**: exactly `<dungeonId>_<floorId>` (e.g. `my_dungeon_floor1`)
* **Task type**: `MINECRAFT_SERVER`
* **Node assignment**: your Minecraft nodes
* **Memory**: at least 512 MB per instance (1024 MB+ recommended)
* **Auto start count**: `0` (CloudNet should not auto-start instances)

### Step 2: Upload the World Template

Upload your dungeon world as a **local template** in CloudNet's template storage:

* Template storage: `local`
* Template prefix: `my_dungeon_floor1`
* Template name: `default`

The world folder should be placed at:
```
CloudNet/local/templates/my_dungeon_floor1/default/
```

Inside that folder, place your Minecraft server world folder (e.g. `world/`).

> The `saveEditWorldToTemplate` method in `CloudNetProvider` zips the current world and uploads it to this template path after an edit session.

### Step 3: Add the NextDungeon Plugin to the Template

Also place the NextDungeon plugin JAR and its dependencies in the template's `plugins/` folder:

```
CloudNet/local/templates/my_dungeon_floor1/default/plugins/NextDungeon.jar
CloudNet/local/templates/my_dungeon_floor1/default/plugins/MMOCore.jar
CloudNet/local/templates/my_dungeon_floor1/default/plugins/packetevents.jar
```

Include a pre-configured `plugins/NextDungeon/config.yml` pointing to your Redis instance.

<!-- INSERT HERE: screenshot of a CloudNet task configuration screen -->

---

## Configuration in NextDungeon

In `plugins/NextDungeon/config.yml`:

```yaml
InstanceProvider:
  type: "CLOUDNET"
```

No other CloudNet-specific configuration is required in the Spigot plugin. CloudNet connection is handled automatically by the CloudNet API loaded as a soft dependency.

---

## Instance Timeout

If an instance takes longer than `InstanceSettings.loadingTimeout` seconds (default 120) to become ready, the plugin:

1. Logs a warning to the console
2. Notifies the player
3. Calls `FloorInstance.cancelInstance()`, which broadcasts a `CancelInstancePacket` to all servers

The `CancelInstanceSubscriber` on all servers then removes the stale instance from Redis.

---

## Edit Mode with CloudNet

When an admin starts edit mode:

```
/dungeon admin edit start <dungeonId> <floorId>
```

The `editMode` property is set to `true` on the CloudNet service. The instance server detects this via `ServerUtil.isInEditMode()` and initialises in edit mode (registers `EditorJoinListener`, skips the queue system).

When the admin saves with `/dungeon admin edit stop --confirm`, `CloudNetProvider.saveEditWorldToTemplate(floor)` is called:

1. The current world is zipped
2. The archive is uploaded to CloudNet template storage as `<floorId>/default`
3. The server shuts down gracefully

---

## Troubleshooting

| Problem | Likely Cause | Solution |
|---------|-------------|---------|
| `Cannot create task for <floorId>` | CloudNet task not found | Create a task named exactly `<dungeonId>_<floorId>` in CloudNet |
| Instance never becomes ready | World template missing plugins or config | Check that `config.yml` and required JARs are in the task template |
| Players not transferred | CloudNet Bridge module missing | Install and enable the CloudNet Bridge module on the instance node |
| `An error occurred during the initialization phase` | CloudNet not available on this server | Ensure CloudNet is installed and the Bridge module is loaded before NextDungeon starts |
