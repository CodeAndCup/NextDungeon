---
icon: square-terminal
---

# Commands & Permissions

## Command Overview

All NextDungeon commands share the root aliases: `/dungeon`, `/dungeons`, `/nextdungeon`, `/nextdungeons`, `/nd`.

---

## Player Commands

| Command | Description |
|---------|-------------|
| `/dungeon` | Show the list of available player commands |
| `/dungeon help` | Same as above |
| `/dungeon join <dungeonId> <floorId>` | Join the queue for a floor (e.g. `/dungeon join example floor1`) |
| `/dungeon leave <floorId>` | Leave the queue for a specific floor |
| `/dungeon status` | Check your current queue position across all floors |
| `/dungeon list` | List all available dungeons and their floors with live queue/instance stats |

> **Note:** The `join` command automatically creates a solo internal party if the player does not already belong to a party.

---

## Admin Commands

All admin commands require the permission `nextdungeons.admin`.

| Command | Description |
|---------|-------------|
| `/dungeon admin help` | Show admin command help |
| `/dungeon admin edit start <dungeonId> <floorId>` | Launch a new floor instance in **edit mode** and send the admin there |
| `/dungeon admin edit stop [--confirm]` | End the current edit session and save the world template to CloudNet; add `--confirm` to skip the confirmation step |
| `/dungeon admin webeditor start` | Start the Blockly web editor HTTP server for the current floor instance |
| `/dungeon admin webeditor stop` | Stop the active web editor session |
| `/dungeon admin test <dungeonId> <floorId>` | Launch a test instance (non-edit) and send the admin there immediately |
| `/dungeon admin load <configName>` | Load (or reload) a dungeon from a YAML file in the `dungeons/` folder |
| `/dungeon admin migrate-to-redis <configName>` | Migrate a single YAML dungeon config to Redis storage |
| `/dungeon admin migrate-all` | Migrate **all** YAML dungeons in the `dungeons/` folder to Redis |
| `/dungeon admin status [instanceId]` | Show the status of the current server (instance or lobby) or a specific instance by UUID |
| `/dungeon admin goto <serverName>` | Connect the admin to a specific server by name |
| `/dungeon admin queue` | Show queue management sub-commands |
| `/dungeon admin queue status` | Display active queues with size and instance counts for each floor |
| `/dungeon admin queue clear <floorId>` | Clear the entire queue for a specific floor |
| `/dungeon admin queue list <floorId>` | List every player currently waiting in a floor queue |

---

## Debug Commands

Debug commands are available under `nextdungeons.debug` permission (or OP).

| Command | Description |
|---------|-------------|
| `/dungeon debug help` | Show debug command help |
| `/dungeon debug list dungeons` | List all loaded dungeons |
| `/dungeon debug list floors` | List all loaded floors |
| `/dungeon debug list instances` | List all active Redis instances |
| `/dungeon debug openmenu` | Open the example dungeon gate menu |
| `/dungeon debug print <message>` | Print a translated message to your chat (for colour/format testing) |

---

## Console Commands

Some commands are available from the server console via `ConsoleCommands` (see `spigot/src/main/java/fr/perrier/dungeons/spigot/commands/ConsoleCommands.java`).

---

## Permission Nodes

Assign permissions with your permission manager (e.g. LuckPerms).

| Permission | Description | Default |
|-----------|-------------|---------|
| `nextdungeons.admin` | Access to all `/dungeon admin` commands | OP only |
| `nextdungeons.debug` | Access to all `/dungeon debug` commands | OP only |
| *(none required)* | All `/dungeon` player commands are accessible to every player | Everyone |

> There are no per-command player permission nodes in the current codebase. All admin commands share the single `nextdungeons.admin` node.
