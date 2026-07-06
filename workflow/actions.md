---
description: Complete reference for every action you can run from a trigger.
icon: play
---

# Actions

**Actions** are the things a trigger does when it fires. You attach them inside a trigger (or inside a condition or loop), and they run one after another, top to bottom.

> **Targets.** Many actions let you choose who they affect: **Player** (the player who caused the trigger) or **Everyone** (all players in the run).
>
> **Placeholders.** Text fields (messages, titles, commands) support placeholders like `{player}` and variables like `{global.name}` — see [Conditions & Variables](conditions-and-variables.md#placeholders-in-text).

Modules add more actions of their own — see the [Cinematic](../modules/cinematic.md) and [WorldEdit](../modules/worldedit.md) module pages.

---

## Messages & Feedback

### 💬 Send Message

Sends a chat message.

| Field | Default | Description |
|-------|---------|-------------|
| To | Player | Player or Everyone |
| Message | — | The text to send (supports colour codes and placeholders) |

### 💬 Send Title

Shows a big title / subtitle on screen.

| Field | Default | Description |
|-------|---------|-------------|
| To | Player | Player or Everyone |
| Title | — | Main line (optional) |
| Subtitle | — | Second line (optional) |
| Fade in / Stay / Fade out (ticks) | 10 / 70 / 20 | Timing (20 ticks = 1 second) |

### 🔊 Play Sound

Plays a sound.

| Field | Default | Description |
|-------|---------|-------------|
| Sound | Level up | Sound to play (chosen from a list) |
| Volume | 1.0 | Loudness |
| Pitch | 1.0 | Higher = higher-pitched |
| Play for | Player | Player or Everyone |

### ✨ Spawn Particles

Displays particle effects.

| Field | Default | Description |
|-------|---------|-------------|
| Particle | Flame | Particle type (chosen from a list) |
| Amount | 10 | How many particles |
| Offset X / Y / Z | 0.5 | How far they spread on each axis |
| Speed | 0.1 | Particle speed |
| Position | Player | At the player, or at the trigger location |

---

## Players & Entities

### 🌐 Teleport

Moves a player to a location.

| Field | Default | Description |
|-------|---------|-------------|
| Target | Player | Player or Everyone |
| Location | — | Where to teleport |

### ❤️ Set Health

Changes a player's health.

| Field | Default | Description |
|-------|---------|-------------|
| Operation | Set | Set, Add, or Subtract |
| Value | 20 | Amount (capped at the player's max health) |
| Apply to | Player | Player or Everyone |

### 🧪 Apply Potion Effect

Applies a potion effect.

| Field | Default | Description |
|-------|---------|-------------|
| Effect | Speed | Any vanilla effect |
| Duration (seconds) | 10 | How long it lasts |
| Level (1–255) | 1 | Effect strength |
| Ambient | Off | Softer effect particles |
| Show particles | On | Whether the effect shows particles |
| Apply to | Player | Player or Everyone |

### 🎁 Give Item

Gives an item.

| Field | Default | Description |
|-------|---------|-------------|
| Item type | `DIAMOND` | The item to give |
| Amount | 1 | How many |
| Custom name | — | Optional display name |
| Give to | Player | Player or Everyone |

### Drop Item

Drops an item on the ground.

| Field | Default | Description |
|-------|---------|-------------|
| Item | `STONE` | The item to drop |
| Quantity | 1 | 1–64 |
| Location | — | Where it drops |

### 💠 Summon Mob

Spawns a single mob.

| Field | Default | Description |
|-------|---------|-------------|
| Mob type | `ZOMBIE` | Vanilla type or MythicMobs mob name |
| Position | — | Where it spawns |

### 👾 Summon Mobs in Region

Spawns several mobs at random safe spots inside a region.

| Field | Default | Description |
|-------|---------|-------------|
| Mob type | `ZOMBIE` | Vanilla type or MythicMobs mob name |
| Number of mobs | 1 | 1–100 |
| Position 1 / Position 2 | — | The two corners of the region |

### 📦 Spawn Loot Chest

Places a chest filled with loot.

| Field | Default | Description |
|-------|---------|-------------|
| Position | — | Where the chest appears |
| Chest type | Chest | Chest, Trapped chest, or Barrel |
| Loot mode | Global | **Global** = one shared chest; **Per-player** = each player gets their own loot |
| Loot | `DIAMOND:3,GOLD_INGOT:1-5` | Comma-separated `ITEM:amount` (amount can be a range like `1-5`) |

---

## Decoration (Block Displays)

Block displays are floating, decorative blocks you can scale and rotate freely — handy for statues, floating runes, or moving set pieces.

### 🧱 Summon Block Display

Spawns a decorative block display.

| Field | Default | Description |
|-------|---------|-------------|
| Block type | `DIAMOND_BLOCK` | The block to show |
| Position | — | Where it appears |
| Scale / Translation / Rotation | 1× / none | Size, offset and orientation of the display |
| Block Display ID | — | Optional tag so you can modify it later |

### 🔧 Modify Block Display

Changes a block display you spawned earlier (found by its ID).

| Field | Default | Description |
|-------|---------|-------------|
| Block Display ID | — | The ID you gave when spawning it |
| Property to change | All | Block type, Scale, Translation, Rotation, or All |
| New block type | — | Optional, when changing the block type |
| Scale / Translation / Rotation | 1× / none | New transform values |

---

## Flow & Utility

### ⏳ Delay

Pauses the action sequence before continuing.

| Field | Default | Description |
|-------|---------|-------------|
| Ticks | 20 | 20 ticks = 1 second |

### 📞 Call Function

Runs a [Function](triggers.md#function) you defined elsewhere, by name.

| Field | Default | Description |
|-------|---------|-------------|
| Function name | `ma_fonction` | The name of the function to run |

### ⌨️ Run Console Command

Runs a command as the server console.

| Field | Default | Description |
|-------|---------|-------------|
| Command | `say Hello World!` | The command to run (supports `@player` and `@all`) |

### 🏁 End Dungeon

Ends the current run as a **success**. Use it when players reach the objective.

*(No fields.)*

---

## Variable Actions

Actions that store and change values (Set, Add, Subtract, Random Number, Math, Get) are covered on the [Conditions & Variables](conditions-and-variables.md#variable-actions) page.
