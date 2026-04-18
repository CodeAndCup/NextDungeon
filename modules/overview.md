---
description: How the NextDungeon dynamic module system works and how to create your own modules.
icon: puzzle-piece
---

# Modules Overview

NextDungeon supports **dynamic modules** — external JAR files that extend the workflow engine with custom triggers, actions, and conditions. Modules are loaded at runtime without modifying the core plugin or the web editor code.

<!-- INSERT HERE: diagram showing the module architecture (ModuleLoader → ModuleBlockRegistry → Blockly toolbox) -->

---

## How Modules Work

```
plugins/NextDungeon/modules/
  ├── module-cinematic.jar
  └── module-worldedit.jar
```

At startup, `ModuleLoader` scans the `modules/` directory, loads every JAR with its own `URLClassLoader` for isolation, and calls `NextDungeonModule.onEnable(ctx)`. Each module registers **block descriptors** into `ModuleBlockRegistry`. These descriptors are then:

1. Used by `BlocklyJavaScriptGenerator` to auto-generate the Blockly toolbox categories in the web editor.
2. Mapped to action/trigger handlers by `ActionFactory` / `TriggerFactory` so the workflow engine knows how to execute them.

Module blocks are stored in the database the same way as built-in blocks, ensuring backward compatibility if a module is later unloaded.

---

## Built-in Modules

| Module | Module ID | JAR name | Description |
|--------|-----------|----------|-------------|
| [Cinematic](cinematic.md) | `cinematic` | `module-cinematic.jar` | Data-driven cinematic sequences with camera paths, NPC actors, and timeline events |
| [WorldEdit](worldedit.md) | `worldedit` | `module-worldedit.jar` | WorldEdit workflow actions (set, cut, replace, schematic paste) |

---

## Installing a Module

1. Download or build the module JAR.
2. Place it in `plugins/NextDungeon/modules/`.
3. Restart the server — the module is loaded automatically — **or** load it at runtime:

```
/dungeon admin module load module-cinematic.jar
```

Module blocks appear immediately in the Blockly editor toolbox under their own category.

---

## Managing Modules

| Command | Description |
|---------|-------------|
| `/dungeon admin module list` | List all loaded modules (ID, name, version) |
| `/dungeon admin module load <file.jar>` | Load a module JAR from the `modules/` directory at runtime |
| `/dungeon admin module unload <moduleId>` | Unload a module (`onDisable()` is called and its blocks are removed) |
| `/dungeon admin module reload <moduleId>` | Unload and immediately reload a module (useful after updating a JAR) |

> **Tip:** After reloading a module, open the web editor again to see the refreshed toolbox.

---

## Creating a Custom Module

### 1. Module Interface

Your module class must implement `fr.perrier.dungeons.common.module.NextDungeonModule`:

```java
public interface NextDungeonModule {
    void onEnable(ModuleContext ctx);   // Register blocks here
    void onDisable();                  // Cleanup
    String getId();                    // Unique ID, e.g. "myplugin"
    String getName();                  // Human-readable name
    String getVersion();               // Version string
}
```

### 2. Registering Blocks

In `onEnable()`, use `ModuleContext.getBlockRegistry()` to register descriptors:

```java
@Override
public void onEnable(ModuleContext ctx) {
    ModuleBlockDescriptor block = new ModuleBlockDescriptor(
        "myplugin.fireworks",                 // unique block ID
        ModuleBlockDescriptor.BlockType.ACTION,
        "🎆 Launch Fireworks",                // label in Blockly
        "Launches fireworks at the player",   // tooltip
        getId()                               // module ID
    );
    block.setColor("#E91E63");
    block.setCategory("MyPlugin");

    // Add configurable input fields
    block.addParameter(new ModuleBlockDescriptor.BlockParameter(
        "count",
        ModuleBlockDescriptor.BlockParameter.ParameterType.NUMBER,
        "Number of fireworks:",
        "3"
    ));

    ctx.getBlockRegistry().register(block);

    // Register execution handler
    ctx.registerActionHandler("myplugin.fireworks", params -> {
        Player player = (Player) params.get("player");
        int count = ((Number) params.getOrDefault("count", 3)).intValue();
        // ... launch fireworks ...
        return true;
    });
}
```

### 3. Block Types

| Type | Description |
|------|-------------|
| `ACTION` | Executed as part of an action sequence |
| `TRIGGER` | Listens for a game event and starts a workflow |
| `CONDITION` | Used inside an `IfCondition` to gate actions |

### 4. Packaging

Package your module as a standard JAR. The JAR must contain the `NextDungeonModule` implementation class on the classpath. No `plugin.yml` or other descriptor is needed — `ModuleLoader` discovers the implementation by scanning all classes in the JAR for the `NextDungeonModule` interface.

### 5. Accessing Spigot APIs

Inside `onEnable()`, obtain the `Plugin` instance with:

```java
Plugin plugin = Bukkit.getPluginManager().getPlugin("NextDungeon");
```

Use this to register Bukkit listeners, schedule tasks, and access the Bukkit API.

---

## Architecture Reference

| Class | Location | Responsibility |
|-------|----------|----------------|
| `ModuleLoader` | `spigot/.../module/ModuleLoader.java` | Scans `modules/`, loads JARs with isolated `URLClassLoader`, calls lifecycle methods |
| `DefaultModuleBlockRegistry` | `spigot/.../module/DefaultModuleBlockRegistry.java` | Stores registered `ModuleBlockDescriptor` objects |
| `ModuleBlockDescriptor` | `common/.../module/ModuleBlockDescriptor.java` | Describes a single Blockly block (type, label, category, parameters) |
| `NextDungeonModule` | `common/.../module/NextDungeonModule.java` | Interface every module must implement |
| `ModuleContext` | `common/.../module/ModuleContext.java` | Passed to `onEnable()` — provides registry and handler registration API |
| `ModuleActionHandler` | `common/.../module/ModuleActionHandler.java` | Functional interface for module action execution |
| `ModuleAction` | `spigot/.../workflow/action/impl/ModuleAction.java` | Runtime action wrapper that delegates execution to the registered handler |
