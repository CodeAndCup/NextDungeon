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

## WorldEdit Actions

These actions require WorldEdit or FAWE to be installed on the instance server.

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

## Action Execution

`ActionSequenceExecutor` (`fr.perrier.dungeons.spigot.workflow.action.ActionSequenceExecutor`) runs actions in the order they are added to a trigger. `DelayAction` inserts timed pauses using Bukkit's scheduler. Each action receives:

* `player` — the player who triggered the event
* `location` — the location associated with the event
* `data` — a map of context-specific values (e.g. `click_type`, `clicked_block`)
