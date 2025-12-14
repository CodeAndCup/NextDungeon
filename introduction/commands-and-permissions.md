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

| Command                      | Description                                      |
| ---------------------------- | ------------------------------------------------ |
| `/dungeon`                   | Show available dungeons and player dungeon info  |
| `/dungeon help`              | Display player command help                      |
| `/dungeon join <dungeonId>`  | Join or queue for a dungeon                      |
| `/dungeon leave`             | Leave current dungeon instance                   |
| `/dungeon info <dungeonId>`  | View information about a specific dungeon        |
| `/dungeon progress`          | Check your dungeon completion progress           |

### Debug Commands

| Command                                                 | Description                                  |
| ------------------------------------------------------- | -------------------------------------------- |
| `/dungeon debug help`                                   | Show debug commands help menu                |
| `/dungeon debug list [instances/dungeons/floors]`       | List instances/dungeons/floors all for debug |

***

## Permission Nodes

Permissions in Dungeons Plugin control access to commands and features. Assign these using your permissions manager (e.g. LuckPerms).

### Admin Permissions

| Permission Node                    | Description                              | Default |
| ---------------------------------- | ---------------------------------------- | ------- |
| `nextdungeon.admin`                | Access to all admin commands             | OP      |
| `nextdungeon.admin.edit`           | Edit dungeons and floors                 | OP      |
| `nextdungeon.admin.webeditor`      | Start/stop the web editor                | OP      |
| `nextdungeon.admin.load`           | Load dungeon configurations              | OP      |
| `nextdungeon.admin.status`         | View dungeon and floor status            | OP      |
| `nextdungeon.admin.goto`           | Teleport to dungeon servers              | OP      |
| `nextdungeon.admin.reload`         | Reload plugin configuration              | OP      |
| `nextdungeon.debug`                | Access to debug commands                 | OP      |

### Player Permissions

| Permission Node                    | Description                              | Default |
| ---------------------------------- | ---------------------------------------- | ------- |
| `nextdungeon.use`                  | Basic dungeon access                     | true    |
| `nextdungeon.join`                 | Join dungeons                            | true    |
| `nextdungeon.leave`                | Leave dungeons                           | true    |
| `nextdungeon.info`                 | View dungeon information                 | true    |
| `nextdungeon.progress`             | Check completion progress                | true    |

### Dungeon-Specific Permissions

You can create per-dungeon permissions for fine-grained access control:

| Permission Node                           | Description                              |
| ----------------------------------------- | ---------------------------------------- |
| `nextdungeon.dungeon.<dungeonId>`         | Access to a specific dungeon             |
| `nextdungeon.dungeon.<dungeonId>.bypass`  | Bypass requirements for specific dungeon |

**Example:**
* `nextdungeon.dungeon.starter_dungeon` - Access to starter dungeon
* `nextdungeon.dungeon.raid_dungeon` - Access to raid dungeon
* `nextdungeon.dungeon.premium_dungeon.bypass` - Bypass level/item requirements

### Feature Permissions

| Permission Node                    | Description                              | Default |
| ---------------------------------- | ---------------------------------------- | ------- |
| `nextdungeon.bypass.level`         | Bypass level requirements                | OP      |
| `nextdungeon.bypass.party`         | Bypass party size requirements           | OP      |
| `nextdungeon.bypass.cooldown`      | Bypass retry cooldowns                   | OP      |
| `nextdungeon.bypass.items`         | Bypass required items                    | OP      |
| `nextdungeon.revive`               | Use revive system on teammates           | true    |

### Group Permissions

For permission plugins like LuckPerms, you can set up permission groups:

#### Default Player Group
```
/lp group default permission set nextdungeon.use true
/lp group default permission set nextdungeon.join true
/lp group default permission set nextdungeon.leave true
/lp group default permission set nextdungeon.info true
/lp group default permission set nextdungeon.progress true
/lp group default permission set nextdungeon.revive true
```

#### VIP Group (Access to Premium Dungeons)
```
/lp group vip permission set nextdungeon.dungeon.premium_dungeon true
/lp group vip permission set nextdungeon.bypass.cooldown true
```

#### Admin Group
```
/lp group admin permission set nextdungeon.admin true
/lp group admin permission set nextdungeon.debug true
```

### Permission Hierarchy

Using wildcards for easier management:

```
nextdungeon.*                    # All permissions
nextdungeon.admin.*              # All admin permissions
nextdungeon.bypass.*             # All bypass permissions
nextdungeon.dungeon.*            # Access to all dungeons
```

### Command Aliases

The following aliases are available:

* `/dungeon` → `/nd` or `/dung`
* `/dungeon admin` → `/nd admin` or `/nda`
* `/dungeon debug` → `/nd debug` or `/ndd`

All permission nodes work the same regardless of which alias is used.

***

## Usage Examples

### Player Usage
```
# View available dungeons
/dungeon

# Get info about a specific dungeon
/dungeon info castle_dungeon

# Join a dungeon
/dungeon join castle_dungeon

# Check your progress
/dungeon progress

# Leave current dungeon
/dungeon leave
```

### Admin Usage
```
# Load a dungeon configuration
/dungeon admin load castle_dungeon

# Edit a dungeon floor
/dungeon admin edit castle_dungeon floor1

# Start web editor for current floor
/dungeon admin webeditor start

# Check dungeon status
/dungeon admin status castle_dungeon floor1

# Debug: List all dungeons
/dungeon debug list dungeons

# Debug: List active instances
/dungeon debug list instances
```

### Permission Configuration Examples

#### Allow specific dungeons per rank

**Bronze Rank:**
```
/lp group bronze permission set nextdungeon.dungeon.easy_dungeon true
```

**Silver Rank:**
```
/lp group silver permission set nextdungeon.dungeon.easy_dungeon true
/lp group silver permission set nextdungeon.dungeon.medium_dungeon true
```

**Gold Rank:**
```
/lp group gold permission set nextdungeon.dungeon.* true
```

#### Tester/Staff Bypass Permissions
```
/lp group tester permission set nextdungeon.bypass.level true
/lp group tester permission set nextdungeon.bypass.cooldown true
/lp group staff permission set nextdungeon.bypass.* true
```

***

## Notes

* **Default Permissions**: Most player permissions default to `true` for all players
* **OP Override**: Server operators have all permissions by default
* **Permission Manager Required**: Use LuckPerms, PermissionsEx, or similar plugin
* **Case Sensitive**: Permission nodes are case-sensitive
* **Reload After Changes**: Some permission changes may require plugin reload

For more information on permission management, consult your permission plugin's documentation.
