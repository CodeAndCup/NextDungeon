---
description: Complete reference for every trigger you can use in the workflow editor.
icon: bolt
---

# Triggers

A **trigger** listens for something that happens inside a dungeon floor and starts a chain of actions when it does. You add and configure triggers visually in the web editor — drag a trigger onto the workspace, set its fields, and attach the actions you want it to run.

Every trigger can also have **conditions** attached, so the actions only run when your rules are met (see [Conditions & Variables](conditions-and-variables.md)).

> **Common options.** Several triggers share these switches:
> * **Trigger once** — fires only once per player, then never again for that player.
> * **Trigger once (global)** — fires only once for the whole dungeon run, no matter who caused it.
> * **Cooldown (seconds)** — a minimum delay before the trigger can fire again.

---

## 🖱️ Block Click

Fires when a player clicks a block.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| Click type | dropdown | Right click | Left click, right click, or both |
| Detection type | dropdown | Block | *Block* (break/place) or *Interaction* (right-click use) |
| Block material | text | `STONE` | The block type to match (e.g. `CHEST`). Use `ANY` to match any block |
| Block position | location | — | The target block's coordinates |
| Exact position only | checkbox | Off | If on, only fires at the exact configured coordinates |
| Trigger once / once (global) | checkbox | Off | See common options above |

**Use it for:** buttons, levers, pressure plates, chests, clickable NPCs.

---

## Chat Message

Fires when a player sends a chat message that matches a keyword. Can optionally block the message from appearing.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| Keyword | text | — | The text to look for |
| Match type | dropdown | Contains | *Contains*, *Equals*, *Starts with*, or *Ends with* |
| Case sensitive | checkbox | Off | Match upper/lower case exactly |
| Cancel the message | checkbox | Off | Hide the message from chat when it matches |

**Use it for:** riddle answers, secret phrases, NPC dialogue keywords.

---

## Console Command

Fires when a specific command is run from the server console. Can optionally cancel the command.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| Command | text | — | The command text to match (without a leading slash) |
| Match type | dropdown | Equals | *Equals*, *Starts with*, or *Contains* |
| Case sensitive | checkbox | Off | Match upper/lower case exactly |
| Cancel the command | checkbox | On | Stop the command from running when it matches |

**Use it for:** hooking dungeon logic to commands sent by other plugins or automation.

---

## Entity Death

Fires when an entity of a given type dies.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| Entity | text | `ZOMBIE` | A vanilla entity type (e.g. `SKELETON`) or a MythicMobs mob name |

**Use it for:** boss defeats, clearing mob waves, kill objectives.

---

## Item Pickup

Fires when a player picks up a matching item, in at least the amount you set. Can optionally cancel the pickup.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| Item type | text | `DIAMOND` | The item to detect |
| Minimum amount | number | `1` | How many must be picked up to fire |
| Cancel the pickup | checkbox | Off | Prevent the player from actually picking the item up |

**Use it for:** collecting key items, triggering story events on pickup.

---

## Player Damage

Fires when a player takes damage of a chosen type, above a minimum amount. Can optionally cancel the damage.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| Damage type | dropdown | All | All, Fall, Fire, Entity attack, Projectile, Drowning, Lava, or Void |
| Minimum damage | number | `0` | Minimum damage to fire (0 = any amount) |
| Cancel the damage | checkbox | Off | Negate the damage when it matches |

**Use it for:** trap alerts, protecting players from fall/void damage, special attack effects.

---

## Player Jump

Fires when a player jumps.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| Cooldown (seconds) | number | `0` | Minimum delay before it can fire again |
| Trigger once / once (global) | checkbox | Off | See common options above |

**Use it for:** jump puzzles, spring pads, environmental gimmicks.

---

## Region Enter/Exit

Fires when a player enters or leaves a rectangular (cuboid) region that you define with two corners.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| Position 1 | location | — | One corner of the region |
| Position 2 | location | — | The opposite corner |
| World | text | `world` | The world the region is in |
| Event | dropdown | — | *Enter*, *Exit*, or *Both* |
| Cooldown (seconds) | number | `0` | Minimum delay before it can fire again |
| Trigger once / once (global) | checkbox | Off | See common options above |

**Use it for:** room entry events, boss-room activation, checkpoint detection.

---

## Function

A **function** is a reusable set of actions you name once and run from anywhere. It does not listen for a game event on its own — you run it with the [Call Function](actions.md#call-function) action.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| Name | text | `ma_fonction` | A unique name used to call this function |

**Use it for:** shared reward logic, reusable animations, keeping large workflows tidy.
