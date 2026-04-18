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

## Step 3: Create a Dungeon Configuration

### Option A: Web Dashboard&#x20;

Open your browser and navigate to the dashboard served by your proxy module (e.g. `http://your-proxy-ip:7734`). Use the web interface to create a new dungeon and add floors visually.

Changes are pushed to Redis automatically and all lobby servers reload them in real time.

## Step 4: Start Edit Mode

To place your dungeon world into the CloudNet template and configure triggers:

```
/dungeon admin edit start dungeon_floor1
```

This creates a new CloudNet instance in edit mode and teleports you to it. You can then:

* Modify the world (blocks, structures, etc.)
* Use `/dungeon admin webeditor start` to open the Blockly web editor and add triggers/actions
* When finished, save and stop edit mode:

```
/dungeon admin edit stop
```

This saves the world template back to CloudNet and shuts down the edit server.

## Step 6: Test the Dungeon (as admin)

You can now test the dungeon.

```
/dungeon admin run dungeon_floor1
```

***

Your first dungeon is live! See [Editing Dungeons](../dungeon-management/editing-dungeons.md) to refine workflows, or explore the [Integrations](../integrations/cloudnet.md) section for advanced setups.
