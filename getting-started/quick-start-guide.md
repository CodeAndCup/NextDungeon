---
description: Get your first dungeon up and running with this quick start guide.
icon: rocket
---

# Quick Start Guide

This guide assumes you have already completed the [Installation](installation.md) steps and that your server is running with NextDungeon loaded.

## Step 1: Verify Installation

Run the following command in-game or from the console:

```
/dungeon list
```

You should see an empty dungeon list (or the example dungeon if it was pre-loaded). If the plugin reports an error, check [Installation](installation.md) again.

## Step 2: Build Your Dungeon World

Before creating a floor configuration you need a Minecraft world that represents your dungeon:

1. Build (or import) your dungeon in a separate world on one of your Spigot servers.
2. Note down the **spawn coordinates** where players will enter the floor.
3. Identify **region boundaries** for each step/area (cuboid min/max corners).
4. Identify boss rooms, trap areas, and any locations where triggers will fire.

> **Tip:** Use WorldEdit's `//pos1` and `//pos2` to capture region coordinates precisely.

<!-- INSERT HERE: screenshot of a dungeon build with labelled regions -->

## Step 3: Create a Dungeon Configuration

### Option A: Web Dashboard (recommended)

Open your browser and navigate to the dashboard served by your proxy module (e.g. `http://your-proxy-ip:7734`). Use the web interface to create a new dungeon and add floors visually.

Changes are pushed to Redis automatically and all lobby servers reload them in real time.

### Option B: YAML Config File

Create a new YAML file in `plugins/NextDungeon/dungeons/` — for example `my_dungeon.yml`:

```yaml
dungeon:
  id: "my_dungeon"
  name: "My First Dungeon"
  floors:
    - id: "floor1"
      name: "The Entrance"
      description: "Survive the dungeon entrance."

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
        death_ban: "10m"
        gamemode: "SURVIVAL"
        allow_flight: false
        max_instance: 5

      steps:
        - id: "step1"
          name: "Entrance Hall"
          region:
            pos1: { x: -10, y: 60, z: -10 }
            pos2: { x: 10, y: 80, z: 10 }
```

Load the config in-game:

```
/dungeon admin load my_dungeon
```

To migrate this YAML config to Redis (so it persists and syncs to all servers):

```
/dungeon admin migrate-to-redis my_dungeon
```

After migrating, set `DungeonLoader: "redis"` in `config.yml` so future startups load from Redis.

## Step 4: Create a CloudNet Task Template

For every floor you want to run as an isolated instance, CloudNet needs a matching **task** with the same name as the floor ID (e.g. `my_dungeon_floor1`):

1. In the CloudNet web panel or CLI, create a new task named `my_dungeon_floor1`.
2. Assign it to your Minecraft nodes.
3. Set the task to use the dungeon world as the static world template.

Refer to [CloudNet Integration](../integrations/cloudnet.md) for detailed steps.

## Step 5: Start Edit Mode

To place your dungeon world into the CloudNet template and configure triggers:

```
/dungeon admin edit start my_dungeon floor1
```

This creates a new CloudNet instance in edit mode and teleports you to it. You can then:

* Modify the world (blocks, structures, etc.)
* Use `/dungeon admin webeditor start` to open the Blockly web editor and add triggers/actions
* When finished, save and stop edit mode:

```
/dungeon admin edit stop --confirm
```

This saves the world template back to CloudNet and shuts down the edit server.

<!-- INSERT HERE: screenshot of the Blockly editor with a sample trigger configured -->

## Step 6: Test Your Dungeon

Back on the lobby server:

```
/dungeon admin test my_dungeon floor1
```

A test instance is created and you are sent directly to it. Check that:

* The spawn location is correct
* Triggers fire as expected
* The floor completes when the end condition is met

## Step 7: Open the Dungeon to Players

Players can now queue for your floor:

```
/dungeon join my_dungeon floor1
```

They will be placed in the queue and transferred to the dungeon once an instance is ready.

Use `/dungeon admin queue status` to monitor the queue in real time.

***

Your first dungeon is live! See [Editing Dungeons](../dungeon-management/editing-dungeons.md) to refine workflows, or explore the [Integrations](../integrations/cloudnet.md) section for advanced setups.
