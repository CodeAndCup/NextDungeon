---
description: Add logic and memory to your workflows with conditions, loops, and variables.
icon: code-branch
---

# Conditions & Variables

**Conditions** let a workflow make decisions — run some actions only when a rule is true. **Variables** let a workflow remember values (like a score or a counter) and use them later.

---

## Conditions

Every condition has an **Then** branch (runs when the check passes) and an optional **Else** branch (runs when it fails). Drop actions inside whichever branch you need.

### 🔀 If

Compares two values and runs *Then* or *Else* based on the result. Values can be plain text/numbers or variables like `{global.score}`.

| Field | Default | Description |
|-------|---------|-------------|
| Value #1 | `value1` | First value |
| Operator | `==` | `==`, `!=`, `<`, `<=`, `>`, `>=`, `contains`, `startsWith`, `endsWith` |
| Value #2 | `value2` | Second value |

### 🎒 Player Has Item

Passes when the player is carrying enough of an item.

| Field | Default | Description |
|-------|---------|-------------|
| Item type | `DIAMOND` | The item to look for |
| Minimum amount | 1 | How many are required |
| Check the name | Off | Also require a specific display name |
| Item name | — | The display name to match (when *Check the name* is on) |

### 👹 Entity Is of Type

Checks the entity involved in the event.

| Field | Default | Description |
|-------|---------|-------------|
| Entity type | `ZOMBIE` | Type to compare against |
| Comparison | is | *is* or *is not* |

### 🧱 Block Is of Type

Checks the block at a set of coordinates.

| Field | Default | Description |
|-------|---------|-------------|
| X / Y / Z | 0 / 64 / 0 | Block coordinates |
| Block type | `STONE` | Type to compare against |
| Comparison | is | *is* or *is not* |

### 📍 Player Is in Region

Checks whether the player is inside a region defined by two corners.

| Field | Default | Description |
|-------|---------|-------------|
| Position 1 / Position 2 | — | The two corners of the region |
| Comparison | inside | *inside* or *outside* |

### 🔐 Player Has Permission

Checks a permission node.

| Field | Default | Description |
|-------|---------|-------------|
| Permission | `dungeons.admin` | The permission node to check |
| Comparison | has | *has* or *has not* |

### 🌞 Time Is

Checks the in-game time.

| Field | Default | Description |
|-------|---------|-------------|
| Period | Day | Day, Night, Dawn, Dusk, or Custom |
| Custom time (ticks) | 6000 | Used when Period is *Custom* (0 = midnight, 6000 = noon) |
| Operator | `==` | How to compare against the custom time |

### 🛡️ Location Is Safe

Checks whether a spot is safe to teleport to (enough room, solid ground, no hazards). Useful before a Teleport action.

| Field | Default | Description |
|-------|---------|-------------|
| Location | — | The spot to check |
| Check solid ground | On | Require solid ground below |
| Check dangerous blocks | On | Reject lava, fire, cactus, etc. |

---

## Loops

### 🔁 For

Repeats its inner actions, counting a variable from a start value to an end value.

| Field | Default | Description |
|-------|---------|-------------|
| Loop variable | `i` | Counter name — read it as `{player.i}` inside the loop |
| Initial value | 0 | Where the counter starts |
| Final value | 10 | Where the counter stops |
| Increment | 1 | How much the counter grows each pass |

**Example — spawn 5 flame bursts, one second apart:**

```
For (variable: i, 0 → 5, step 1)
  ├─ Spawn Particles (Flame, at trigger location)
  └─ Delay (20 ticks)
```

---

## Variables

Variables store values during a run so your workflows can share and remember state.

### Scopes

| Scope | Prefix | Meaning |
|-------|--------|---------|
| Global | `global.` | Shared by everyone in the run |
| Per-player | `player.` | Separate value for each player |

### Placeholders in Text

Use these inside any message, title, or command text:

| Placeholder | Becomes |
|-------------|---------|
| `{player}` | The name of the player who triggered the action |
| `{trigger}` | The name of the trigger that fired |
| `{global.name}` | The value of the global variable `name` |
| `{player.name}` | The value of the per-player variable `name` |

### Variable Actions

| Action | What it does |
|--------|--------------|
| 📝 **Set Variable** | Sets a variable to a value — fields: variable name, value, scope |
| 🔍 **Get Variable** | Copies one variable into another — fields: source, destination, and a scope for each |
| ➕ **Add to Variable** | Adds a value to a variable (numbers add, text joins) — fields: variable name, value to add, scope |
| ➖ **Subtract from Variable** | Subtracts a value from a numeric variable — fields: variable name, value to subtract, scope |
| 🎲 **Random Number** | Stores a random number in a variable — fields: variable name, min, max, scope |
| 🧮 **Math Operation** | Combines two values and stores the result — fields: first value, operation (add, subtract, multiply, divide, concatenate), second value, result variable, scope |

### Example — a kill counter

```
When an Entity Dies (ZOMBIE)
  ├─ Add to Variable (player.zombieKills += 1)
  └─ If ({player.zombieKills} >= 10)
       Then:
         ├─ Send Message ("You cleared the horde!")
         └─ End Dungeon
```

> **Variables reset each run.** They live only for the duration of a dungeon instance and are not saved afterwards. Completed floors and per-run stats *are* saved to each player's profile automatically.
