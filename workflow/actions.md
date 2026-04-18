---
description: Complete reference for all available action types in NextDungeon workflows.
icon: play
---

# Actions

Actions are the effects executed when a trigger fires. They are attached to trigger blocks in the Blockly editor and executed in sequence by `ActionSequenceExecutor`.

All actions extend the base `Action` class (`fr.perrier.dungeons.spigot.workflow.action.Action`).

---

## Message Actions

### SendMessageAction

Sends a text message to a player or broadcasts to all.

| Field | Default | Description |
|-------|---------|-------------|
| Target | `player` | `player` (triggering player) or `@all` (broadcast) |
| Message | `Hello {player}!` | Message text. Supports `&` colour codes, `&#RRGGBB` hex colours, and variable placeholders |

**Variable placeholders:** `{player}`, `{global.varName}`, `{player.varName}`, `{trigger}`.

### SendTitleAction

Displays a title and subtitle on the player's screen.

| Field | Default | Description |
|-------|---------|-------------|
| Target | `player` | `player` or `@all` |
| Title | — | Main title text (colour codes supported) |
| Subtitle | — | Subtitle text (colour codes supported) |
| Fade in | `10` | Fade-in ticks |
| Stay | `70` | Display duration in ticks |
| Fade out | `20` | Fade-out ticks |

---

## Movement Actions

### TeleportLocationAction

Teleports the player to a specific location.

| Field | Default | Description |
|-------|---------|-------------|
| Location | (0,64,0) | Target X, Y, Z coordinates |
| World | current world | Target world name |
| Yaw | `0` | Player horizontal rotation |
| Pitch | `0` | Player vertical rotation |

---

## Mob Actions

### SummonMobAction

Spawns a single entity at a location.

| Field | Default | Description |
|-------|---------|-------------|
| Entity type | `ZOMBIE` | Vanilla entity type or MythicMobs mob name |
| Location | (0,64,0) | Spawn coordinates |
| Count | `1` | Number of entities |

### SummonMobInRegionAction

Spawns entities at random positions within a cuboid region.

| Field | Default | Description |
|-------|---------|-------------|
| Entity type | `ZOMBIE` | Vanilla entity type or MythicMobs mob name |
| Region pos1 | (0,64,0) | First corner of the region |
| Region pos2 | (10,74,10) | Second corner of the region |
| Count | `5` | Number of entities to spawn |

---

## Player Actions

### GiveItemAction

Gives an item to the player.

| Field | Default | Description |
|-------|---------|-------------|
| Material | `DIAMOND` | Minecraft material name |
| Amount | `1` | Quantity |
| Display name | — | Custom display name (colour codes supported) |

### DropItemAction

Drops an item on the ground at a location.

| Field | Default | Description |
|-------|---------|-------------|
| Material | `DIAMOND` | Minecraft material name |
| Amount | `1` | Quantity |
| Location | (0,64,0) | Drop location coordinates |

### ApplyPotionEffectAction

Applies a potion effect to the player.

| Field | Default | Description |
|-------|---------|-------------|
| Effect type | `SPEED` | Minecraft PotionEffectType name |
| Duration | `100` | Duration in ticks |
| Amplifier | `0` | Effect level (0 = level 1) |

### SetHealthAction

Sets or modifies the player's health.

| Field | Default | Description |
|-------|---------|-------------|
| Health | `20` | New health value (0.0 - 20.0) |
| Mode | `set` | `set` (absolute) or `add` (relative) |

---

## Effects Actions

### PlaySoundAction

Plays a sound at a location.

| Field | Default | Description |
|-------|---------|-------------|
| Sound | `ENTITY_PLAYER_LEVELUP` | Minecraft Sound enum name |
| Volume | `1.0` | Volume (0.0 - 1.0+) |
| Pitch | `1.0` | Pitch (0.5 - 2.0) |
| Location | player location | Where to play the sound |

### SpawnParticleAction

Spawns particle effects at a location.

| Field | Default | Description |
|-------|---------|-------------|
| Particle | `FLAME` | Minecraft Particle enum name |
| Count | `10` | Number of particles |
| Location | (0,64,0) | Spawn location |
| Offset X/Y/Z | `0.5` | Spread offset per axis |

---

## Control Actions

### DelayAction

Pauses the action sequence for a specified number of ticks.

| Field | Default | Description |
|-------|---------|-------------|
| Ticks | `20` | Wait time (20 ticks = 1 second) |

### CallFunctionAction

Calls a named `FunctionTrigger` sub-workflow.

| Field | Default | Description |
|-------|---------|-------------|
| Function name | — | Name of the `FunctionTrigger` to execute |

### BroadcastCommandAction

Executes a command from the console.

| Field | Default | Description |
|-------|---------|-------------|
| Command | — | Console command string (without leading `/`) |

### EndDungeonAction

Marks the dungeon run as complete or failed.

| Field | Default | Description |
|-------|---------|-------------|
| Success | `true` | If true, calls `FloorInstance.complete(true)`; if false, calls `FloorInstance.fail()` |

When triggered, all players receive a completion summary (time, kills, deaths) and the instance shuts down after 30 seconds.

---

## Display Entity Actions

These actions manipulate Minecraft **Display entities** (available since Minecraft 1.19.4+). They are useful for building custom visual effects and decorations inside dungeons.

### SummonBlockDisplayAction

Spawns a new `BlockDisplay` entity at a location with a configurable 3D transformation.

**Source:** `spigot/src/main/java/fr/perrier/dungeons/spigot/workflow/action/impl/SummonBlockDisplayAction.java`

| Field | Default | Description |
|-------|---------|-------------|
| Block type | `DIAMOND_BLOCK` | Minecraft material name for the displayed block |
| Location | (0,64,0) | World coordinates where the entity will be spawned |
| Scale X/Y/Z | `1.0` | Size multiplier per axis |
| Translation X/Y/Z | `0` | Positional offset applied to the entity's display |
| Left Rotation X/Y/Z/W | `0`/`0`/`0`/`1` | Quaternion for the left rotation transform |
| Right Rotation X/Y/Z/W | `0`/`0`/`0`/`1` | Quaternion for the right rotation transform |
| Display ID | — | Optional unique identifier so `ModifyBlockDisplayAction` can reference this entity later |

Block displays are tracked globally by their Display ID, allowing later modification via `ModifyBlockDisplayAction`.

### ModifyBlockDisplayAction

Modifies an existing `BlockDisplay` entity that was previously spawned via `SummonBlockDisplayAction`.

**Source:** `spigot/src/main/java/fr/perrier/dungeons/spigot/workflow/action/impl/ModifyBlockDisplayAction.java`

| Field | Default | Description |
|-------|---------|-------------|
| Display ID | — | ID of the target `BlockDisplay` (must match the ID used at summon time) |
| Property to modify | `ALL` | Which properties to change: `BLOCK_TYPE`, `SCALE`, `TRANSLATION`, `ROTATION`, or `ALL` |
| Block type | — | New block material (used when property is `BLOCK_TYPE` or `ALL`) |
| Scale X/Y/Z | `1.0` | New scale values |
| Translation X/Y/Z | `0` | New translation offset |
| Left Rotation X/Y/Z/W | — | New left rotation quaternion |
| Right Rotation X/Y/Z/W | — | New right rotation quaternion |

> **Note:** The `ALL` option updates all transform properties in one call.

---

## WorldEdit Actions

These actions require the **WorldEdit module** (`module-worldedit`) to be installed in the `plugins/NextDungeon/modules/` folder. They are provided by the `WorldEditModule` class and not built into the core plugin.

See the [WorldEdit Module](../modules/worldedit.md) documentation for setup instructions.

### WorldEditSchematicAction

Pastes a WorldEdit schematic at a location.

| Field | Default | Description |
|-------|---------|-------------|
| Schematic name | — | Name of the schematic file (without path or extension) |
| Location | (0,64,0) | Paste anchor location |
| Air blocks | `false` | Whether to paste air blocks |

### WorldEditSetAction

Fills a cuboid region with a block material.

| Field | Default | Description |
|-------|---------|-------------|
| Region pos1 / pos2 | — | Region corners |
| Material | `STONE` | Block material to set |

### WorldEditReplaceAction

Replaces one block material with another in a region.

| Field | Default | Description |
|-------|---------|-------------|
| Region pos1 / pos2 | — | Region corners |
| From material | `STONE` | Material to replace |
| To material | `AIR` | Replacement material |

### WorldEditCutAction

Removes all blocks within a region (fills with air).

| Field | Default | Description |
|-------|---------|-------------|
| Region pos1 / pos2 | — | Region to clear |

---

## Module Actions

`ModuleAction` is the internal action type used when a trigger calls an action registered by a dynamic module (e.g. the Cinematic module). It wraps a `ModuleActionHandler` implementation provided by the module at runtime.

**Source:** `spigot/src/main/java/fr/perrier/dungeons/spigot/workflow/action/impl/ModuleAction.java`

Module actions appear in the Blockly editor as dedicated blocks under their module category (e.g. **Cinematic**). They are serialised to JSON with their parameters and delegated to the module handler at execution time. If the module is not loaded, the action logs a warning and returns `false` without crashing the workflow.

See the [Modules Overview](../modules/overview.md) for a full list of available module action blocks.

---

## Action Execution

`ActionSequenceExecutor` (`fr.perrier.dungeons.spigot.workflow.action.ActionSequenceExecutor`) runs actions in the order they are added to a trigger. `DelayAction` inserts timed pauses using Bukkit's scheduler. Each action receives:

* `player` — the player who triggered the event
* `location` — the location associated with the event
* `data` — a map of context-specific values (e.g. `click_type`, `clicked_block`)
