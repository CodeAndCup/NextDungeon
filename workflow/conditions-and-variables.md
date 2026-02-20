---
description: Conditions and variable system for workflow logic in NextDungeon.
icon: code-branch
---

# Conditions & Variables

Conditions add logical branching to your trigger workflows. Variables let triggers share and persist state across action sequences.

---

## Condition Blocks

### IfCondition

The most general condition. Compares two values with an operator and executes the `then` branch if true, or the `else` branch if false.

**Source:** `spigot/src/main/java/fr/perrier/dungeons/spigot/workflow/condition/IfCondition.java`

| Field | Default | Description |
|-------|---------|-------------|
| Left value | `value1` | First operand (can be a variable reference, e.g. `{global.score}`) |
| Operator | `==` | Comparison operator: `==`, `!=`, `<`, `<=`, `>`, `>=`, `contains`, `startsWith`, `endsWith` |
| Right value | `value2` | Second operand |
| Then actions | — | Actions to execute if the condition is true |
| Else actions | — | Actions to execute if the condition is false (optional) |

### BlockTypeIsCondition

Checks whether the block at a specified location has a specific material.

**Source:** `spigot/src/main/java/fr/perrier/dungeons/spigot/workflow/condition/BlockTypeIsCondition.java`

| Field | Default | Description |
|-------|---------|-------------|
| Location | (0,64,0) | Block coordinates |
| Material | `STONE` | Expected Minecraft material |

### EntityTypeIsCondition

Checks whether an entity involved in the event matches a specific type.

**Source:** `spigot/src/main/java/fr/perrier/dungeons/spigot/workflow/condition/EntityTypeIsCondition.java`

| Field | Default | Description |
|-------|---------|-------------|
| Entity type | `ZOMBIE` | Expected Minecraft EntityType name |

### LocationIsSafeCondition

Checks whether a location is safe to teleport to (not inside a solid block, not above a void).

**Source:** `spigot/src/main/java/fr/perrier/dungeons/spigot/workflow/condition/LocationIsSafeCondition.java`

| Field | Default | Description |
|-------|---------|-------------|
| Location | (0,64,0) | Coordinates to check |

### PlayerHasItemCondition

Checks whether the triggering player holds an item with a specific display name.

**Source:** `spigot/src/main/java/fr/perrier/dungeons/spigot/workflow/condition/PlayerHasItemCondition.java`

| Field | Default | Description |
|-------|---------|-------------|
| Item display name | — | Exact display name of the item (colour codes stripped for comparison) |

### PlayerInRegionCondition

Checks whether the triggering player is currently inside a named region.

**Source:** `spigot/src/main/java/fr/perrier/dungeons/spigot/workflow/condition/PlayerInRegionCondition.java`

| Field | Default | Description |
|-------|---------|-------------|
| Region name | — | Name of the region (matches step names defined in the floor config) |

### PlayerPermissionCondition

Checks whether the triggering player has a specific permission node.

**Source:** `spigot/src/main/java/fr/perrier/dungeons/spigot/workflow/condition/PlayerPermissionCondition.java`

| Field | Default | Description |
|-------|---------|-------------|
| Permission | — | Permission node string (e.g. `nextdungeons.admin`) |

### TimeOfDayCondition

Checks whether the current in-game time is within a specified range.

**Source:** `spigot/src/main/java/fr/perrier/dungeons/spigot/workflow/condition/TimeOfDayCondition.java`

| Field | Default | Description |
|-------|---------|-------------|
| Min time | `0` | Minimum world time in ticks (0 = midnight, 6000 = noon) |
| Max time | `24000` | Maximum world time in ticks |

---

## Variable System

Variables are managed by `VariableRegistry` (`fr.perrier.dungeons.spigot.workflow.registry.VariableRegistry`). They allow action sequences to store, retrieve, and manipulate state.

### Variable Scopes

| Scope | Prefix | Description |
|-------|--------|-------------|
| Global | `global.` | Shared across all players in the current instance |
| Per-player | `player.` | Scoped to a specific player |

### Variable Placeholders in Strings

Use these inside any text field of message/title actions:

| Placeholder | Resolved Value |
|-------------|---------------|
| `{global.varName}` | Value of the global variable named `varName` |
| `{player.varName}` | Value of the per-player variable named `varName` for the triggering player |
| `{player}` | Name of the triggering player |
| `{trigger}` | Name of the trigger that fired |

### Variable Actions

| Action | Description |
|--------|-------------|
| `SetVariableAction` | Set a variable to a specific value |
| `GetVariableAction` | Copy a variable's value into another variable |
| `AddToVariableAction` | Add a numeric value to a variable |
| `SubtractFromVariableAction` | Subtract a numeric value from a variable |
| `MathOperationAction` | Perform arithmetic (`+`, `-`, `*`, `/`, `%`) on two values and store the result |

### Example: Score Counter

Track how many mobs a player has killed in a run:

```
EntityDeathTrigger (type: ZOMBIE)
  -- AddToVariableAction (variable: player.zombieKills, amount: 1)
  -- IfCondition (left: {player.zombieKills}, op: >=, right: 10)
       then:
         -- SendMessageAction (message: "You killed 10 zombies!")
         -- EndDungeonAction (success: true)
```

---

## Persistence

Variables are **in-memory only** for the duration of the dungeon instance. They are not persisted to Redis or the database when the instance ends. For persistent player data, use the `ProfileService` (which tracks completed floors and per-run statistics automatically).
