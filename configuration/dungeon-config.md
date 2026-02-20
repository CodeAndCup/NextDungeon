---
description: Complete reference for dungeon and floor YAML configuration files.
icon: dungeon
---

# Dungeon Config

Dungeon configurations are YAML files stored in `plugins/NextDungeon/dungeons/`. Each file defines one dungeon with one or more floors. After initial setup it is recommended to migrate them to Redis using `/dungeon admin migrate-all` and then set `DungeonLoader: "redis"` in `config.yml`.

The example file `plugins/NextDungeon/dungeons/dungeon_exemple.yml` is included with every installation.

---

## Top-Level Structure

```yaml
dungeon:
  id: "example"             # Unique dungeon identifier (alphanumeric, underscores)
  name: "Dungeon Example"   # Human-readable display name
  floors:
    - ...                   # One or more floor definitions (see below)
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Unique ID for this dungeon. Used as the first part of floor IDs (`dungeonId_floorId`). Must be unique across all dungeons. |
| `name` | string | Yes | Display name shown to players in menus and messages |
| `floors` | list | Yes | List of floor definitions (at least one) |

---

## Floor Definition

Each entry in the `floors` list is a complete floor configuration.

```yaml
- id: "floor1"
  name: "The Forgotten Crypts"
  description: "Explore ancient crypts full of mystery and danger."

  world:
    difficulty: "normal"
    spawn: { x: 0, y: 100, z: 0 }

  requirements:
    retry_cooldown: "15m"
    required_floor: []
    minimum_level: 0
    party:
      min_size: 2
      max_size: 25
    required_items:
      - "Old Key"
    forbidden_items:
      - "Magic Wand"

  rules:
    max_lives: 3
    death_ban: "15m"
    gamemode: "SURVIVAL"
    allow_flight: false
    max_instance: 1

  steps:
    - id: "step1"
      name: "Catacomb Entrance"
      region:
        pos1: { x: -10, y: 60, z: -10 }
        pos2: { x: 10, y: 80, z: 10 }
```

### Floor — Top-Level Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Unique floor ID within this dungeon. The full floor ID is `dungeonId_floorId` (e.g. `example_floor1`). |
| `name` | string | Yes | Display name for this floor |
| `description` | string | No | Multi-line description shown to players. Use `\n` for line breaks. |

---

### `world` — World Configuration

```yaml
world:
  difficulty: "normal"           # easy | normal | hard
  spawn: { x: 0, y: 100, z: 0 } # Player spawn point for this floor
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `difficulty` | string | `normal` | Minecraft world difficulty: `easy`, `normal`, or `hard` |
| `spawn.x` | integer | `0` | X coordinate of the spawn point |
| `spawn.y` | integer | `100` | Y coordinate of the spawn point |
| `spawn.z` | integer | `0` | Z coordinate of the spawn point |

---

### `requirements` — Entry Requirements

Requirements are checked by `Floor.isRequirementsValid(player)` before allowing a player to enter.

```yaml
requirements:
  retry_cooldown: "15m"          # Cooldown after a failed attempt (e.g. "10s", "5m", "1h")
  required_floor: []             # Floor IDs that must be completed first (e.g. ["example_floor1"])
  minimum_level: 0               # Minimum MMOCore player level (0 = no requirement)
  party:
    min_size: 2                  # Minimum number of party members
    max_size: 25                 # Maximum number of party members
  required_items:                # Items (by display name) the player must have in inventory
    - "Old Key"
  forbidden_items:               # Items the player must NOT have in inventory
    - "Magic Wand"
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `retry_cooldown` | duration string | — | How long a player must wait before retrying after a failure (e.g. `15m`, `1h30m`) |
| `required_floor` | list of strings | `[]` | IDs of floors that must be completed before this one is accessible |
| `minimum_level` | integer | `0` | Minimum MMOCore level required. `0` means no restriction. |
| `party.min_size` | integer | `1` | Minimum party size to enter |
| `party.max_size` | integer | unlimited | Maximum party size allowed |
| `required_items` | list of strings | `[]` | Item display names the player must hold in their inventory |
| `forbidden_items` | list of strings | `[]` | Item display names the player must NOT have in their inventory |

---

### `rules` — Gameplay Rules

```yaml
rules:
  max_lives: 3           # Lives per player (0 = unlimited, negative = instant-fail on death)
  death_ban: "15m"       # Ban duration if all lives are exhausted
  gamemode: "SURVIVAL"   # SURVIVAL | ADVENTURE | CREATIVE | SPECTATOR
  allow_flight: false    # Whether players are allowed to fly
  max_instance: 1        # Maximum simultaneous running instances of this floor (0 = unlimited)
```

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `max_lives` | integer | `3` | Number of lives per player. When all lives are gone, `banCommand` is executed. Set to `0` for unlimited lives. |
| `death_ban` | duration string | — | Duration of the ban applied when lives run out (passed to `banCommand` as `{time}`) |
| `gamemode` | string | `SURVIVAL` | Minecraft gamemode for players inside this floor |
| `allow_flight` | boolean | `false` | Whether flight is enabled inside the floor world |
| `max_instance` | integer | `1` | How many simultaneous instances of this floor can exist. `0` means unlimited. Enforced by the queue system. |

---

### `steps` — Progression Steps

Steps are named waypoints / regions that define the progression path through the floor. Each step corresponds to a cuboid region in the world.

```yaml
steps:
  - id: "step1"
    name: "Catacomb Entrance"
    region:
      pos1: { x: -10, y: 60, z: -10 }
      pos2: { x: 10, y: 80, z: 10 }

  - id: "step2"
    name: "Dark Gallery"
    region:
      pos1: { x: 20, y: 60, z: -10 }
      pos2: { x: 40, y: 80, z: 10 }
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Unique step identifier within this floor |
| `name` | string | Yes | Display name shown in progress trackers |
| `region.pos1` | coordinates | Yes | First corner of the step region |
| `region.pos2` | coordinates | Yes | Second (opposite) corner of the step region |

Steps are exposed to the `RegionTrigger` workflow trigger by name. The trigger fires when players enter or leave the corresponding region.

---

## Complete Example

The full example dungeon config (`dungeon_exemple.yml`) ships with the plugin and defines two floors with multiple steps. Refer to it as a starting template.

<!-- INSERT HERE: screenshot of the dungeon config loaded successfully in-game -->

---

## Loading and Migrating Configs

| Operation | Command |
|-----------|---------|
| Load a YAML file | `/dungeon admin load <fileName>` (without `.yml`) |
| Migrate one file to Redis | `/dungeon admin migrate-to-redis <fileName>` |
| Migrate all YAML files to Redis | `/dungeon admin migrate-all` |

After migration, set `DungeonLoader: "redis"` in `config.yml` and restart (or reload) the server.
