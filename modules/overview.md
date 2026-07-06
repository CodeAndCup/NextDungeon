---
description: Add-on modules extend the workflow editor with extra triggers, actions, and conditions.
icon: puzzle-piece
---

# Modules Overview

**Modules** are optional add-ons that give the workflow editor new blocks — extra triggers, actions, and conditions — without changing the core plugin. They're loaded from a folder and appear in the editor automatically.

---

## How Modules Work

Modules live in:

```
plugins/NextDungeon/modules/
  ├── module-cinematic.jar
  └── module-worldedit.jar
```

When the server starts (or when you load one manually), each module registers its blocks. They then show up in the editor under their own toolbox category, and you use them exactly like the built-in blocks. Workflows that use module blocks keep working even if the module is unloaded later.

---

## Built-in Modules

| Module | Adds | Details |
|--------|------|---------|
| **Cinematic** | Camera cutscenes, titles, sounds, screen effects | [Cinematic Module](cinematic.md) |
| **WorldEdit** | Region operations — set, cut, replace, copy, paste schematics | [WorldEdit Module](worldedit.md) |
| **Memory Labyrinth** | A full roguelike dungeon mode — random rooms, door choices, bosses, and Infinite runs | [Memory Labyrinth Module](memory-labyrinth.md) |

---

## Installing a Module

1. Put the module's `.jar` in `plugins/NextDungeon/modules/`.
2. Restart the server — it loads automatically — **or** load it live:

```
/dungeon admin module load module-cinematic.jar
```

The new blocks appear immediately in the editor toolbox. (Re-open the editor to refresh the toolbox after loading or reloading a module.)

---

## Managing Modules

| Command | Description |
|---------|-------------|
| `/dungeon admin module list` | List loaded modules (ID, name, version) |
| `/dungeon admin module load <file.jar>` | Load a module from the modules folder |
| `/dungeon admin module unload <moduleId>` | Unload a module |
| `/dungeon admin module reload <moduleId>` | Reload a module (after updating its file) |

---

## Building Your Own Module

Modules are Java add-ons, so creating one is a development task rather than a configuration task. A dedicated developer guide covering the module API is planned. If you need it sooner, contact the NextDungeon team.
