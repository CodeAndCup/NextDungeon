---
description: >-
  A detailed look at every feature NextDungeon brings to your Minecraft server.
icon: magnifying-glass
---

# Feature Overview

## 1. Multi-Floor Dungeon System

Each **Dungeon** (`fr.perrier.dungeons.spigot.model.Dungeon`) acts as a container for one or more **Floors** (`fr.perrier.dungeons.spigot.model.Floor`). Floors are the playable units; every floor is independently configured with:

* A **world configuration** (difficulty, spawn coordinates)
* **Requirements** (minimum MMOCore level, prerequisite floors, required/forbidden items, party size)
* **Game rules** (max lives, death-ban duration, gamemode, flight, max concurrent instances)
* **Steps** (named cuboid regions that mark progression checkpoints)
* **Triggers and actions** (workflow automation attached to this floor)

Dungeon and floor data is serialised with Gson and stored in Redis, so all lobby and instance servers share the same up-to-date configuration.

<!-- INSERT HERE: UML diagram showing Dungeon → Floor → FloorInstance hierarchy -->

## 2. Trigger-Action Workflow Engine

The workflow system lets you script complex dungeon behaviour without writing any Java code.

### Triggers

| Trigger | Description |
|---------|-------------|
| `BlockClickTrigger` | Player clicks/interacts with a specific block (by material and/or exact coordinates) |
| `ChatMessageTrigger` | Player sends a chat message matching a pattern |
| `EntityDeathTrigger` | An entity of a specific type dies |
| `FunctionTrigger` | Reusable named sub-workflow callable via `CallFunctionAction` |
| `ItemPickupTrigger` | Player picks up an item with a specific display name |
| `PlayerDamageTrigger` | Player receives damage (optionally filtered by damage cause) |
| `PlayerJumpTrigger` | Player jumps |
| `RegionTrigger` | Player enters or exits a named cuboid region |

### Actions

| Action | Description |
|--------|-------------|
| `SendMessageAction` | Send a chat message to a player or broadcast to all (`@all`) |
| `SendTitleAction` | Display a title/subtitle on screen |
| `TeleportLocationAction` | Teleport a player to specific coordinates |
| `SummonMobAction` | Spawn a vanilla or MythicMobs entity at a location |
| `SummonMobInRegionAction` | Spawn mobs at random positions within a region |
| `EndDungeonAction` | Mark the dungeon as complete (success) or failed |
| `GiveItemAction` | Give an item to the player |
| `DropItemAction` | Drop an item on the ground |
| `PlaySoundAction` | Play a sound at a location |
| `SpawnParticleAction` | Spawn particle effects |
| `ApplyPotionEffectAction` | Apply a potion effect |
| `SetHealthAction` | Set or modify a player's health |
| `DelayAction` | Wait a number of ticks before continuing the action sequence |
| `CallFunctionAction` | Call a named `FunctionTrigger` |
| `BroadcastCommandAction` | Execute a console command |
| `SetVariableAction` | Set a named variable (global or per-player) |
| `GetVariableAction` | Read a variable into another variable |
| `AddToVariableAction` | Add a value to a numeric variable |
| `SubtractFromVariableAction` | Subtract a value from a numeric variable |
| `MathOperationAction` | Perform arithmetic on variables |
| `WorldEditSchematicAction` | Paste a WorldEdit schematic at a location |
| `WorldEditSetAction` | Fill a region with a block material |
| `WorldEditReplaceAction` | Replace one block material with another in a region |
| `WorldEditCutAction` | Cut (remove) blocks within a region |

### Conditions

| Condition | Description |
|-----------|-------------|
| `IfCondition` | If/else branching with operators (`==`, `!=`, `<`, `<=`, `>`, `>=`, `contains`, `startsWith`, `endsWith`) |
| `BlockTypeIsCondition` | Check the material of a block at a location |
| `EntityTypeIsCondition` | Check the type of a nearby entity |
| `LocationIsSafeCondition` | Check whether a location is safe to teleport to |
| `PlayerHasItemCondition` | Check whether the player holds a specific item |
| `PlayerInRegionCondition` | Check whether the player is inside a named region |
| `PlayerPermissionCondition` | Check whether the player has a specific permission |
| `TimeOfDayCondition` | Check the current in-game time |

### Variable System

Global and per-player variables (`VariableRegistry`) allow triggers to share state across action sequences. Variables are referenced as `{global.name}` or `{player.name}` inside message strings.

<!-- INSERT HERE: screenshot of the Blockly web editor showing a trigger-action workflow -->

## 3. Blockly Web Editor

When a floor instance is started in **edit mode** (`/dungeon admin edit start`), an HTTP server is launched on the configured `WebEditor.proxy-port` (default `7734`). The admin navigates to the editor URL and uses a drag-and-drop Google Blockly interface to build trigger workflows. Saving from the editor persists the workflow to the database.

<!-- INSERT HERE: video demonstration of the Blockly editor in action -->

## 4. CloudNet Instance Management

Every floor runs as a separate CloudNet service. The plugin (`CloudNetProvider`):

1. Creates a service from the floor CloudNet task template
2. Injects instance metadata (floor ID, UUID, edit mode flag, creation timestamp) as service properties
3. Registers the instance in Redis so lobby servers monitor readiness
4. Routes players to the instance once `ready = true`
5. Cleans up the service and Redis entries when the run ends

## 5. Queue System

The `DungeonQueueService` (Redis-backed) maintains a FIFO queue per floor. When a player requests to join:

1. `QueueManager` checks if a ready instance exists
2. If capacity is available, a new instance is launched and the player is sent directly
3. Otherwise the player enters the queue and receives position updates
4. As instances free up, the queue manager dequeues the next entry automatically

## 6. Revive and Ghost System

On death inside a dungeon the player becomes a ghost for a configurable duration (`ghostDuration`). Teammates can use the **revive item** to bring them back. When the timer expires without a revive the player loses a life. Exhausting all lives triggers the configurable `banCommand`.

## 7. Party Integration

Two party backends are supported via `PartyService`:

* **AlessioDP Parties** — Uses the popular Parties plugin API
* **Internal Party System** — Built-in lightweight group management

Provider selection is controlled by `PartyProvider.type` in `config.yml` (`AUTO`, `AlessioDPParties`, `Internal`).

## 8. Player Profiles and Statistics

`ProfileService` tracks per-player data in Redis with persistence to MySQL or MongoDB:

* Completed floor IDs
* Per-run stats: completion time, enemies killed, death count, success/failure flag

## 9. Dashboard Web Interface

The proxy modules (Velocity/BungeeCord) expose an HTTP server for the web dashboard. Changes sync to lobby servers automatically via the `{topic}:sync` Redis channel.

<!-- INSERT HERE: screenshot of the web dashboard -->

## 10. Admin and Debug Tools

Full suite of in-game commands for floor editing, queue management, instance inspection, and dungeon migration — see [Commands and Permissions](commands-and-permissions.md).

***

Explore the [Getting Started](../getting-started/installation.md) section to begin setting up your own dungeons!
