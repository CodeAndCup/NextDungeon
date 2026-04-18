---
description: The WorldEdit Module provides WorldEdit-based workflow actions for dungeon floors.
icon: cube
---

# WorldEdit Module

The **WorldEdit Module** (`module-worldedit`) adds WorldEdit-powered workflow actions to NextDungeon. These actions allow you to manipulate the dungeon world programmatically from the Blockly workflow editor — fill regions, replace blocks, paste schematics, and more.

**Module ID:** `worldedit`
**Source:** `module-worldedit/src/main/java/fr/perrier/dungeons/module/worldedit/`

> **Prerequisite:** WorldEdit (or FAWE) must be installed on the instance server for these actions to work.

---

## Installation

1. Ensure **WorldEdit** or **FastAsyncWorldEdit (FAWE)** is installed on every dungeon instance server.
2. Place `module-worldedit.jar` in `plugins/NextDungeon/modules/`.
3. Restart the server, **or** run `/dungeon admin module load module-worldedit.jar`.
4. A new **WorldEdit** category appears in the Blockly editor toolbox.

---

## Workflow Blocks

### Set Blocks

Fills a cuboid region with a specified block material.

| Parameter | Default | Description |
|-----------|---------|-------------|
| Region pos1 | (0,64,0) | First corner of the target region |
| Region pos2 | (10,74,10) | Opposite corner of the target region |
| Material | `STONE` | Minecraft material name (e.g. `AIR`, `STONE`, `OAK_PLANKS`) |

**Use case:** Clearing a room after a puzzle is completed, filling a pit with lava.

### Cut Blocks

Removes all blocks inside a cuboid region by replacing them with `AIR`.

| Parameter | Default | Description |
|-----------|---------|-------------|
| Region pos1 | (0,64,0) | First corner |
| Region pos2 | (10,74,10) | Opposite corner |

**Use case:** Opening a secret passage, collapsing a ceiling.

### Replace Blocks

Replaces all blocks of one type with another inside a region.

| Parameter | Default | Description |
|-----------|---------|-------------|
| Region pos1 | (0,64,0) | First corner |
| Region pos2 | (10,74,10) | Opposite corner |
| From material | `STONE` | Block material to search for and replace |
| To material | `AIR` | Replacement material |

**Use case:** Revealing hidden structures, switching a puzzle floor's material.

### Paste Schematic

Pastes a saved WorldEdit schematic at a specific location.

| Parameter | Default | Description |
|-----------|---------|-------------|
| Schematic name | — | File name without path or extension (file must exist in the WorldEdit schematics folder) |
| Location | (0,64,0) | Anchor point for the paste operation |
| Include air | `false` | If `true`, air blocks from the schematic overwrite existing blocks |

**Use case:** Spawning a pre-built structure mid-dungeon, opening a boss room gate.

---

## Example: Opening a Secret Door on Puzzle Completion

```
EntityDeathTrigger (type: ZOMBIE, count: 10)
  -- SendTitleAction (title: "Puzzle Complete!", subtitle: "The path opens...")
  -- PlaySoundAction (sound: BLOCK_ANVIL_LAND, volume: 1.0)
  -- WorldEdit Replace Blocks (pos1: ..., pos2: ..., from: STONE_BRICKS, to: AIR)
```

<!-- INSERT HERE: gif or screenshot of a WorldEdit Set/Replace action opening a doorway in-game -->

---

## Architecture

| Class | Description |
|-------|-------------|
| `WorldEditModule` | Module entry point — registers all blocks and action handlers |
| `WorldEditManager` | Implements the WorldEdit operations using the WorldEdit API |

---

## Notes

* All WorldEdit operations run **synchronously** on the main server thread. For large regions (> 50,000 blocks), consider using **FastAsyncWorldEdit (FAWE)**, which processes operations asynchronously and avoids tick-lag.
* Schematic files must be accessible from the instance server's `plugins/WorldEdit/schematics/` folder (or FAWE equivalent). Make sure schematic files are included in your CloudNet task template.
* Regions are defined as plain coordinate pairs — the module internally constructs a `CuboidRegion` to perform the operation.
* If WorldEdit is not installed on the instance server, action handlers will log an error and return `false` without crashing the workflow.
