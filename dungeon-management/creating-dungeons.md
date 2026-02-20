---
description: Step-by-step instructions for creating a new dungeon and its floors.
icon: plus
---

# Creating Dungeons

This guide walks you through creating a fully functional dungeon from scratch — from the world build to the live queue.

## Overview

A **dungeon** is a container (`Dungeon` object) that holds one or more **floors** (`Floor` objects). Each floor is an independent playable level with its own world, requirements, rules, and trigger-action workflows. Floors are stored in Redis and managed via the `DungeonService`.

<!-- INSERT HERE: diagram showing the relationship between Dungeon, Floor, FloorInstance, and Queue -->

---

## Method 1: Web Dashboard

The recommended method for production servers.

1. Open the dashboard in your browser (served by the Velocity/BungeeCord proxy on the configured `WebEditor.proxy-port`, default `7734`).
2. Click **New Dungeon** and fill in the ID and name.
3. Add floors with the **Add Floor** button and configure each floor's settings.
4. Save. The dungeon is pushed to Redis and all lobby servers load it automatically.

<!-- INSERT HERE: screenshot of the dashboard dungeon creation form -->

---

## Method 2: YAML Configuration

### 1. Create the YAML File

Create `plugins/NextDungeon/dungeons/my_dungeon.yml`:

```yaml
dungeon:
  id: "my_dungeon"
  name: "My Dungeon"
  floors:
    - id: "floor1"
      name: "Floor One"
      description: "The first challenge."

      world:
        difficulty: "normal"
        spawn: { x: 0, y: 64, z: 0 }

      requirements:
        retry_cooldown: "10m"
        required_floor: []
        minimum_level: 0
        party:
          min_size: 1
          max_size: 10
        required_items: []
        forbidden_items: []

      rules:
        max_lives: 3
        death_ban: "15m"
        gamemode: "SURVIVAL"
        allow_flight: false
        max_instance: 3

      steps:
        - id: "step1"
          name: "Entry Hall"
          region:
            pos1: { x: -5, y: 60, z: -5 }
            pos2: { x: 5, y: 75, z: 5 }
```

See [Dungeon Config](../configuration/dungeon-config.md) for the full field reference.

### 2. Load the Config In-Game

```
/dungeon admin load my_dungeon
```

### 3. Migrate to Redis

```
/dungeon admin migrate-to-redis my_dungeon
```

Then update `config.yml`:

```yaml
DungeonLoader: "redis"
```

---

## Setting Up the CloudNet Task Template

Every floor that will run as an isolated CloudNet instance needs a corresponding **CloudNet task**.

1. In the CloudNet management panel, create a new task named exactly **`<dungeonId>_<floorId>`** — for example `my_dungeon_floor1`.
2. Set the task type to `MINECRAFT_SERVER`.
3. Assign it to one or more CloudNet nodes.
4. Set the minimum/maximum service count and memory allocation.
5. Upload your dungeon world as a **static template** in CloudNet's template storage under the same name.

> The `CloudNetProvider` looks up the task by the floor ID when creating an instance. If no task is found, instance creation fails with `Cannot create task for <floorId>`.

<!-- INSERT HERE: screenshot of a CloudNet task configuration for a dungeon floor -->

---

## Starting Edit Mode

Edit mode lets you modify the floor world and configure triggers without affecting live players.

```
/dungeon admin edit start my_dungeon floor1
```

This:
1. Creates a new CloudNet service in edit mode (the `editMode` property is injected into the service)
2. Sends you to the edit server
3. Enables edit-only listeners (`EditorJoinListener`)

On the edit server you can:
* Build and modify the world normally
* Run `/dungeon admin webeditor start` to open the Blockly editor

### Using the Blockly Editor

1. Run `/dungeon admin webeditor start` in-game.
2. The plugin starts an HTTP server and provides the URL in chat.
3. Open the URL in your browser.
4. Drag triggers from the **Triggers** category onto the workspace.
5. Attach actions from the **Actions** category to each trigger.
6. Configure conditions with blocks from the **Logic** category.
7. Save when finished. The workflow is persisted to the database immediately.

<!-- INSERT HERE: screenshot of the Blockly editor with a region trigger and send-message action -->

---

## Saving and Publishing

When editing is complete:

```
/dungeon admin edit stop --confirm
```

This:
1. Saves the world template back to CloudNet (`InstanceProvider.saveEditWorldToTemplate`)
2. Shuts down the edit server
3. Triggers are already saved to the database from the web editor

After saving, the floor is ready for players.

---

## Verifying the New Dungeon

From a lobby server:

```
/dungeon list
```

Your new dungeon and floor should appear with queue and instance statistics. Test it with:

```
/dungeon admin test my_dungeon floor1
```

---

## Multi-Floor Dungeons

To add a second floor that requires the first:

```yaml
    - id: "floor2"
      name: "Floor Two"
      requirements:
        required_floor: ["my_dungeon_floor1"]   # Must complete floor1 first
        minimum_level: 5
        ...
```

Players who have not completed `my_dungeon_floor1` will be blocked from entering `floor2`. Completion records are stored in `ProfileService` and checked in `Floor.isRequirementsValid()`.
