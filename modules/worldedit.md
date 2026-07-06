---
description: The WorldEdit Module adds region-editing actions to dungeon workflows.
icon: cube
---

# WorldEdit Module

The **WorldEdit Module** adds actions that reshape the dungeon world from your workflows — fill regions, clear them, replace blocks, copy areas, and paste schematics.

**Module ID:** `worldedit`

> **Prerequisite:** WorldEdit (or FastAsyncWorldEdit) must be installed on the dungeon instance servers for these actions to work.

---

## Installation

1. Install **WorldEdit** or **FastAsyncWorldEdit (FAWE)** on every dungeon instance server.
2. Place `module-worldedit.jar` in `plugins/NextDungeon/modules/`.
3. Restart the server, **or** run `/dungeon admin module load module-worldedit.jar`.
4. A new **WorldEdit** category appears in the editor toolbox.

---

## Blocks

### 🧱 WE Set Region

Fills a region between two corners with a block or a mix of blocks.

| Field | Default | Description |
|-------|---------|-------------|
| Pos1 / Pos2 | — | The two corners of the region |
| Pattern | `stone` | A single block, or a weighted mix like `70%stone,30%gravel` |

**Use it for:** clearing a room after a puzzle, filling a pit.

### ✂️ WE Cut Region

Clears a region (replaces everything with air).

| Field | Default | Description |
|-------|---------|-------------|
| Pos1 / Pos2 | — | The two corners of the region |

**Use it for:** opening a passage, collapsing a ceiling.

### 🔄 WE Replace Region

Swaps one set of blocks for another inside a region.

| Field | Default | Description |
|-------|---------|-------------|
| Pos1 / Pos2 | — | The two corners of the region |
| Blocks to replace | `stone` | One or more block types to look for (e.g. `stone,cobblestone`) |
| Replace with | `gravel` | A block or weighted mix, like `70%stone,30%gravel` |

**Use it for:** revealing hidden structures, changing a floor's look.

### 📋 Paste Schematic

Pastes a saved WorldEdit schematic.

| Field | Default | Description |
|-------|---------|-------------|
| Schematic name | `schematic.schem` | The schematic file to paste |
| X / Y / Z | 0 / 64 / 0 | Where to paste it |

**Use it for:** dropping in a prebuilt structure, opening a boss gate.

### 📦 WE Copy Region Between Worlds

Copies a cuboid from one world to a spot in another.

| Field | Default | Description |
|-------|---------|-------------|
| Source world | `world` | World to copy from |
| Source Pos1 / Pos2 | — | The area to copy |
| Destination world | `world` | World to paste into |
| Destination X / Y / Z | 200 / 64 / 0 | Where to paste it |

---

## Example: Open a Secret Door When a Horde Is Cleared

```
Entity Death (ZOMBIE)
  ├─ Send Title ("Puzzle Complete!", "The path opens...")
  ├─ Play Sound (anvil land)
  └─ WE Replace Region (the doorway: STONE_BRICKS → air)
```

<!-- INSERT HERE: screenshot of a WorldEdit action opening a doorway in-game -->

---

## Notes

* Operations run on the main server thread. For very large regions, use **FastAsyncWorldEdit (FAWE)** to avoid lag.
* Schematic files must be present on the instance server (in the WorldEdit schematics folder). When using CloudNet, include them in your floor's task template.
* If WorldEdit isn't installed on the instance server, these actions do nothing (they won't crash the workflow).
