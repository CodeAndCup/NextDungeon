---
description: Complete reference for all available trigger types in NextDungeon.
icon: bolt
---

# Triggers

Triggers listen for game events inside a dungeon floor instance and fire a chain of actions when their conditions are met. Each trigger is configured in the Blockly web editor and stored in the database per floor.

All triggers extend the base `Trigger` class (`fr.perrier.dungeons.spigot.workflow.trigger.Trigger`) and are registered in `TriggersRegistry`.

<!-- INSERT HERE: diagram showing trigger → condition check → action sequence execution flow -->

---

## BlockClickTrigger

Fires when a player clicks on a block.

**Source:** `spigot/src/main/java/fr/perrier/dungeons/spigot/workflow/trigger/impl/BlockClickTrigger.java`

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| Click type | dropdown | `right_click` | `left_click`, `right_click`, or `both` |
| Detection type | dropdown | `block` | `block` (block break/place) or `interaction` (right-click use) |
| Block material | text | `STONE` | Minecraft material name (e.g. `STONE`, `CHEST`). Use `ANY` to match any material. |
| Block position | location | (0,64,0) | Target block coordinates. Leave as (0,64,0) to match any location. |
| Exact position only | checkbox | `false` | If true, only triggers at the exact configured coordinates |

**Use cases:** pressure plates, levers, buttons, chests, interactive NPCs.

---

## ChatMessageTrigger

Fires when a player sends a chat message matching a pattern.

**Source:** `spigot/src/main/java/fr/perrier/dungeons/spigot/workflow/trigger/impl/ChatMessageTrigger.java`

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| Message pattern | text | — | The message text to match (exact or partial, depending on match mode) |
| Match mode | dropdown | `exact` | `exact`, `contains`, or `starts_with` |

**Use cases:** riddle answers, NPC dialogue, secret phrases to open passages.

---

## EntityDeathTrigger

Fires when an entity of a specified type dies.

**Source:** `spigot/src/main/java/fr/perrier/dungeons/spigot/workflow/trigger/impl/EntityDeathTrigger.java`

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| Entity type | text | `ZOMBIE` | Vanilla entity type (e.g. `SKELETON`) or MythicMobs mob name |
| Track count | integer | `1` | Number of entities of this type that must die before the trigger fires |

**Use cases:** boss defeat detection, mob wave clearing, spawner kill quests.

---

## FunctionTrigger

A reusable named sub-workflow. Does not listen for any game event on its own; it must be called explicitly by a `CallFunctionAction`.

**Source:** `spigot/src/main/java/fr/perrier/dungeons/spigot/workflow/trigger/impl/FunctionTrigger.java`

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| Function name | text | — | Unique name used to call this function from other triggers |

**Use cases:** shared reward logic, repeated animation sequences, modular scripting.

---

## ItemPickupTrigger

Fires when a player picks up an item with a matching display name.

**Source:** `spigot/src/main/java/fr/perrier/dungeons/spigot/workflow/trigger/impl/ItemPickupTrigger.java`

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| Item display name | text | — | The exact display name of the item to detect |

**Use cases:** collecting key items, activating story events on pickup.

---

## PlayerDamageTrigger

Fires when a player receives damage.

**Source:** `spigot/src/main/java/fr/perrier/dungeons/spigot/workflow/trigger/impl/PlayerDamageTrigger.java`

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| Damage cause | dropdown | `ANY` | Minecraft `EntityDamageEvent.DamageCause` (e.g. `FALL`, `FIRE`, `ENTITY_ATTACK`) or `ANY` |
| Min damage | decimal | `0.0` | Minimum damage amount to trigger (0 = any amount) |

**Use cases:** trap damage alerts, healing on fire damage, special boss attack effects.

---

## PlayerJumpTrigger

Fires when a player jumps.

**Source:** `spigot/src/main/java/fr/perrier/dungeons/spigot/workflow/trigger/impl/PlayerJumpTrigger.java`

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| Region | location | — | Optional region where jump detection is active |

**Use cases:** jump puzzles, spring pads, environmental traps.

---

## RegionTrigger

Fires when a player enters or leaves a cuboid region.

**Source:** `spigot/src/main/java/fr/perrier/dungeons/spigot/workflow/trigger/impl/RegionTrigger.java`

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| Region name | text | — | Name of the region (must match a step name or custom region) |
| Event | dropdown | `enter` | `enter` or `leave` |

Regions are defined as cuboid volumes by two corner coordinates (`pos1` / `pos2`). They correspond to the `steps` defined in the floor configuration.

**Use cases:** area entry events, room completion detection, boss room activation.

---

## Trigger Event Handler Architecture

Each trigger type has a corresponding handler class in `spigot/src/main/java/fr/perrier/dungeons/spigot/workflow/trigger/handler/impl/`:

| Handler | Event Listened |
|---------|---------------|
| `BlockClickTriggerHandler` | `PlayerInteractEvent` |
| `ChatMessageTriggerHandler` | `AsyncPlayerChatEvent` |
| `EntityDeathTriggerHandler` | `EntityDeathEvent` |
| `ItemPickupTriggerHandler` | `EntityPickupItemEvent` |
| `PlayerDamageTriggerHandler` | `EntityDamageEvent` |
| `PlayerJumpTriggerHandler` | `PlayerMoveEvent` (jump detection) |
| `RegionTriggerHandler` | `PlayerMoveEvent` (region check) |

Handlers are registered via `TriggersRegistry` on instance servers. The registry caches loaded triggers for the current floor and routes incoming events to matching triggers.
