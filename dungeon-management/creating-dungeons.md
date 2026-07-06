---
description: Step-by-step instructions for creating a new dungeon and its floors.
icon: plus
---

# Creating Dungeons

A **dungeon** is a container for one or more **floors**. Each floor is an independent, playable level with its own world, entry requirements, rules, and trigger workflows. This guide takes you from an empty world to a floor players can enter.

## Overview

Creating a floor has four parts:

1. **Build the world** for the floor.
2. **Create the dungeon and floor** in the web dashboard.
3. **Set up a CloudNet task** so the floor can run as its own server.
4. **Enter edit mode** to fine-tune the world and add triggers, then save.

***

## 1. Build the World

Build (or import) your dungeon in a normal Minecraft world on one of your servers. While building, note down:

* The **spawn point** where players arrive.
* The **corners of each region** you'll use for triggers (rooms, boss arenas, trap zones). WorldEdit's `//pos1` / `//pos2` are handy for reading coordinates.

***

## 2. Create the Dungeon in the Dashboard

1. Open the web dashboard in your browser (served by your proxy — see [Installation](../getting-started/installation.md)).
2. Click **New Dungeon** and give it an ID and a name.
3. Add one or more floors with **Add Floor**, and set each floor's:
   * **World** settings (difficulty, spawn point)
   * **Requirements** (minimum level, prerequisite floors, required/forbidden items, party size)
   * **Rules** (max lives, death penalty, flight, max concurrent instances)
   * **Steps** — named regions that mark progress through the floor
4. Save. Your changes are shared with every lobby server automatically.

***

## 3. Set Up the CloudNet Task

Each floor runs as its own isolated server, so it needs a matching **CloudNet task**.

1. In CloudNet, create a task named exactly **`<dungeonId>_<floorId>`** — for example `my_dungeon_floor1`.
2. Set its type to a Minecraft server and assign it to one or more nodes.
3. Configure memory and service counts.
4. Upload your dungeon world as a **static template** under the same name.

> If no task exists for a floor, launching it will fail. Double-check the task name matches `<dungeonId>_<floorId>` exactly.

For more detail, see the [CloudNet integration](../integrations/cloudnet.md) page.

***

## 4. Edit Mode: Finish the World & Add Triggers

Edit mode gives you a private copy of the floor to build in and script, without affecting live players.

From a lobby server:

```
/dungeon admin edit start <floorId>
```

This sends you to a dedicated edit server. There you can:

* Build and change the world freely.
* Open the visual trigger editor:

```
/dungeon admin webeditor start
```

The plugin gives you a clickable link in chat. Open it, drag **triggers** onto the workspace, attach **actions**, add **conditions** where needed, and click **Save**. Your workflow is saved immediately — see [Editing Dungeons](editing-dungeons.md) for the full editor walkthrough.

When you're done, save the world and leave edit mode:

```
/dungeon admin edit stop --confirm
```

This saves the world back to the CloudNet template and shuts the edit server down. (Without `--confirm`, you'll get a confirmation prompt first.)

***

## 5. Test the Floor

From a lobby server, launch the floor yourself:

```
/dungeon admin run <floorId>
```

Check that everything is loaded with:

```
/dungeon admin list
```

***

## Multi-Floor Dungeons

To make one floor require another, set the later floor's **prerequisite floor** in the dashboard (under Requirements). Players who haven't completed the earlier floor are blocked from entering. Completion is tracked per player automatically, so once someone finishes `floor1`, `floor2` unlocks for them.
