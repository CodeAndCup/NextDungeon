---
description: Get your first dungeon up and running in minutes with this quick start guide.
icon: rocket
---

# Quick Start Guide

This guide will help you quickly set up and run your first dungeon with NextDungeon. Make sure you have completed the [Installation](installation.md) before proceeding.

## Step 1: Verify Installation

After installing the plugin and starting your server, verify that NextDungeon is loaded:

```
/dungeon debug list dungeons
```

This should show an empty list or any pre-configured dungeons.

## Step 2: Prepare Your Dungeon World

Before creating a dungeon, you need a world that will serve as your dungeon template:

1. Build your dungeon in a separate world (or use a pre-built one)
2. Make note of important coordinates:
   * Spawn point where players will start
   * Region boundaries for each step/area
   * Boss room locations

> **Tip:** Use WorldEdit or similar tools to help identify coordinates and regions.

## Step 3: Create Your First Dungeon Configuration

Navigate to your `plugins/NextDungeon/dungeons/` folder and create a new file called `my-first-dungeon.yml`:

```yaml
dungeon:
  id: "starter_dungeon"
  name: "Starter Dungeon"
  floors:
    - id: "floor1"
      name: "The Beginning"
      description: "Your first dungeon floor"

      world:
        difficulty: "normal"
        spawn: { x: 0, y: 100, z: 0 }

      requirements:
        retry_cooldown: "10m"
        required_floor: []
        minimum_level: 0
        party:
          min_size: 1
          max_size: 5
        required_items: []
        forbidden_items: []

      rules:
        death_ban: "10m"
        gamemode: "SURVIVAL"
        allow_flight: false

      steps:
        - id: "entrance"
          name: "Entrance"
          region:
            pos1: { x: -10, y: 60, z: -10 }
            pos2: { x: 10, y: 80, z: 10 }

        - id: "boss_room"
          name: "Boss Chamber"
          region:
            pos1: { x: 50, y: 60, z: 50 }
            pos2: { x: 70, y: 80, z: 70 }
```

> **Note:** Adjust coordinates to match your actual dungeon world.

## Step 4: Load Your Dungeon

Reload or restart your server, then load the dungeon configuration:

```
/dungeon admin load starter_dungeon
```

Check if the dungeon was loaded successfully:

```
/dungeon debug list dungeons
```

You should now see your "starter_dungeon" in the list.

## Step 5: Enter the Dungeon

To test your dungeon:

1. Make sure you have the required party size (or adjust the config for solo play)
2. Use the command to join/start the dungeon (exact command depends on your setup)
3. The plugin will create an instance and teleport you to the dungeon

> **Note:** If using CloudNet, ensure your CloudNet nodes are properly configured to handle dungeon instances.

## Step 6: Edit and Customize

Now that you have a basic dungeon running, you can:

* Edit the dungeon using `/dungeon admin edit starter_dungeon floor1`
* Add custom mobs using MythicMobs integration
* Configure triggers and actions using the web editor
* Add rewards and progression mechanics

## Common Issues

### Dungeon won't load
* Check console logs for errors
* Verify CloudNet/ASP is properly configured
* Ensure Redis is running and accessible

### Can't teleport to dungeon
* Check party requirements (min/max size)
* Verify player has required permissions
* Check if required items are configured correctly

### Instance creation fails
* For CloudNet: Check node status and templates
* For ASP: Verify world data is accessible
* Check available server resources (RAM/CPU)

## Next Steps

* Read the [Dungeon Configuration](../configuration/dungeon-config.md) guide for advanced options
* Learn about [Creating Dungeons](../dungeon-management/creating-dungeons.md) in detail
* Explore [Integrations](../integrations/cloudnet.md) to enhance your dungeons
* Set up the [Web Editor](../dungeon-management/editing-dungeons.md) for visual dungeon design

***

Need help? Check the troubleshooting section or join our community Discord for support.

