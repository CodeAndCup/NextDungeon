---
icon: square-terminal
---

# Commands & Permissions

Every command works with any of these root aliases: `/dungeon`, `/dungeons`, `/nextdungeon`, `/nextdungeons`, `/nd`.

Players start dungeons from the in-game **Dungeon Gate menu** (opened from the lobby), not from a command — see [Parties → Launching a Dungeon](../integrations/parties.md#launching-a-dungeon).

***

## Player Commands

No permission required — available to everyone.

| Command | Description |
|---------|-------------|
| `/dungeon` | Show the player command help |
| `/dungeon help` | Same as above |
| `/dungeon plugin` | Show plugin information |
| `/dungeon status` | Show your current dungeon/queue status |
| `/dungeon leave` | Leave the dungeon you're currently in |

***

## Party Commands

No permission required. These manage NextDungeon's built-in party system. (If you use the AlessioDP Parties plugin instead, use its commands to form parties — see [Parties Integration](../integrations/parties.md).)

| Command | Description |
|---------|-------------|
| `/dungeon party` | Show party command help |
| `/dungeon party create [name]` | Create a party (you become the leader) |
| `/dungeon party invite <player>` | Invite a player to your party |
| `/dungeon party accept` | Accept a party invite |
| `/dungeon party deny` | Decline a party invite |
| `/dungeon party leave` | Leave your current party |
| `/dungeon party kick <player>` | Remove a member (leader only) |
| `/dungeon party promote <player>` | Make another member the leader |
| `/dungeon party disband` | Disband your party (leader only) |
| `/dungeon party info` | Show your party's members and details |

***

## Admin Commands

Require an admin permission (see [Permission Nodes](#permission-nodes)).

| Command | Description |
|---------|-------------|
| `/dungeon admin help` | Show admin command help |
| `/dungeon admin run <floorId>` | Launch a floor for yourself — the quickest way to test a dungeon |
| `/dungeon admin edit start <floorId>` | Open a floor in **edit mode** and send you there |
| `/dungeon admin edit stop [--confirm]` | Leave edit mode and save the world; add `--confirm` to skip the prompt |
| `/dungeon admin webeditor start` | Open the visual trigger editor for the floor you're editing |
| `/dungeon admin webeditor stop` | Close the trigger editor |
| `/dungeon admin list` | List all dungeons and floors with live queue/instance stats |
| `/dungeon admin status [instanceId]` | Show status of this server, or of a specific instance |
| `/dungeon admin queue status` | Show active queues and instance counts per floor |
| `/dungeon admin queue clear <floorId>` | Clear a floor's queue |
| `/dungeon admin queue list <floorId>` | List players waiting for a floor |
| `/dungeon admin module list` | List loaded modules |
| `/dungeon admin module load <file.jar>` | Load a module from the modules folder |
| `/dungeon admin module unload <moduleId>` | Unload a module |
| `/dungeon admin module reload <moduleId>` | Reload a module (after updating its file) |

***

## Debug Commands

Tools for troubleshooting. Require the debug permission.

| Command | Description |
|---------|-------------|
| `/dungeon debug help` | Show debug command help |
| `/dungeon debug toggle` | Toggle debug mode *(available to everyone)* |
| `/dungeon debug setlogbroadcast <type>` | Choose how debug logs are shown |
| `/dungeon debug list dungeons` | List loaded dungeons |
| `/dungeon debug list floors` | List loaded floors |
| `/dungeon debug list instances` | List active instances |
| `/dungeon debug openmenu` | Open the example dungeon gate menu |
| `/dungeon debug floor <floorId>` | Show a floor's details |
| `/dungeon debug trigger <floorId>` | Inspect a floor's triggers |
| `/dungeon debug itemreq <floorId>` | Check a floor's item requirements against your inventory |
| `/dungeon debug party list` | List known dungeon parties |
| `/dungeon debug party clean` | Clear stale party data |

***

## Console Commands

Run from the server console. Require the console permission.

| Command | Description |
|---------|-------------|
| `/dungeon console openmenu <dungeonId> <player>` | Open a dungeon's gate menu for a player (used by lobby NPCs) |
| `/dungeon console end <completed>` | End the current dungeon run (`true` = completed, `false` = failed) |

***

## Permission Nodes

Assign permissions with your permission manager (e.g. LuckPerms).

| Permission | Grants | Default |
|------------|--------|---------|
| `nextdungeon.admin` | Most `/dungeon admin` commands (help, run, edit, webeditor, list, status) | OP only |
| `nextdungeons.admin` | The `/dungeon admin queue …` and `/dungeon admin module …` commands | OP only |
| `nextdungeon.debug` | `/dungeon debug …` commands (except `debug toggle`, which is open to all) | OP only |
| `nextdungeon.console` | `/dungeon console …` commands | OP only |
| *(none)* | All player and party commands | Everyone |

> **Give your admins both `nextdungeon.admin` and `nextdungeons.admin`.** The plugin currently checks the singular spelling for some admin commands and the plural for the queue/module commands, so granting both is the reliable way to cover every admin command.
