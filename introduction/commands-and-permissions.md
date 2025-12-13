---
icon: square-terminal
---

# Commands & Permissions

## Command Reference

The Dungeons Plugin provides a range of commands for both administrators and players, along with debug and editor tools.

### Admin Commands

| Command                                       | Description                                            |
| --------------------------------------------- | ------------------------------------------------------ |
| `/dungeon admin help`                         | Show admin commands help menu                          |
| `/dungeon admin edit <dungeonId> <floorId>`   | Edit a dungeon floor                                   |
| `/dungeon admin webeditor start`              | Start the Blockly web editor session for current floor |
| `/dungeon admin webeditor stop`               | Stop the Blockly web editor session                    |
| `/dungeon admin load <dungeonConfig>`         | Load a dungeon from config                             |
| `/dungeon admin status <dungeonId> <floorId>` | Show status of dungeon/floor                           |
| `/dungeon admin goto <serverName>`            | Teleport to a dungeon server                           |

### Player Commands

| Command    | Description |
| ---------- | ----------- |
| `/dungeon` | ...         |

### Debug Commands

| Command                                                 | Description                                  |
| ------------------------------------------------------- | -------------------------------------------- |
| `/dungeon debug help`                                   | Show debug commands help menu                |
| `/dungeon debug list [instances/dungeons/floors]`       | List instances/dungeons/floors all for debug |

***

## Permission Nodes

Permissions in Dungeons Plugin control access to commands and features. Assign these using your permissions manager (e.g. LuckPerms).

> <mark style="color:$info;">Soon..</mark>
