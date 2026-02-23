# NextDungeon Dynamic Module System

## Architecture Overview

NextDungeon supports **dynamic modules** — external JAR files that extend the workflow engine with custom triggers, actions, and conditions. Modules are loaded at runtime without modifying the core server or web panel code.

### How It Works

```
┌──────────────────────────────────────────────────────┐
│  Web Panel (Blockly)                                 │
│  ┌──────────────────────────────────────────────┐    │
│  │ Toolbox Categories (auto-generated)          │    │
│  │  🎯 Triggers  ⚡ Actions  🧩 Cinematic ...  │    │
│  └──────────────────────────────────────────────┘    │
│        ↕  REST/API  ↕                                │
├──────────────────────────────────────────────────────┤
│  Backend (Spigot Plugin)                             │
│  ┌──────────────┐  ┌──────────────────────────────┐  │
│  │ ModuleLoader  │→│ ModuleBlockRegistry            │ │
│  │ loads /modules│  │ stores block descriptors      │ │
│  └──────┬───────┘  └────────────┬─────────────────┘  │
│         │                       │                    │
│  ┌──────▼──────────────────────▼──────────────────┐  │
│  │ BlocklyJavaScriptGenerator                      │  │
│  │ (generates JS for Blockly from annotations +    │  │
│  │  module descriptors)                            │  │
│  └─────────────────────────────────────────────────┘  │
│                                                      │
│  ┌─────────────────────────────────────────────────┐  │
│  │ ActionFactory / TriggerFactory                  │  │
│  │ (maps action/trigger types to handlers,         │  │
│  │  including module-provided ones)                │  │
│  └─────────────────────────────────────────────────┘  │
│                                                      │
│  ┌────────────────────────┐                          │
│  │ Database (MySQL/Mongo) │                          │
│  │ - cinematics table     │                          │
│  │ - workflows table      │                          │
│  │ - floor_triggers table │                          │
│  └────────────────────────┘                          │
└──────────────────────────────────────────────────────┘
```

---

## Part 1: Creating a Module

### 1. Module Interface

Every module must implement `NextDungeonModule`:

```java
public interface NextDungeonModule {
    void onEnable(ModuleContext ctx);  // Register blocks here
    void onDisable();                 // Cleanup
    String getId();                   // Unique ID e.g. "cinematic"
    String getName();                 // Human-readable name
    String getVersion();              // Version string
}
```

### 2. Registering Blocks

In `onEnable()`, use the `ModuleBlockRegistry` to register block descriptors:

```java
@Override
public void onEnable(ModuleContext ctx) {
    ModuleBlockDescriptor block = new ModuleBlockDescriptor(
        "cinematic.start",                    // unique block ID
        ModuleBlockDescriptor.BlockType.ACTION,
        "🎬 Start Cinematic",                 // label in Blockly
        "Starts a cinematic sequence",        // tooltip
        getId()                               // module ID
    );
    block.setColor("#9C27B0");
    block.setCategory("Cinematic");
    block.setParameters(List.of(
        new BlockParameter("cinematicId", "string", "Cinematic ID:", "...", "")
    ));
    ctx.getBlockRegistry().registerBlock(block);
}
```

### 3. Block Descriptor JSON Schema

Each block descriptor has this structure (auto-serialized to JSON for the panel):

```json
{
  "id": "cinematic.start",
  "type": "ACTION",
  "label": "🎬 Start Cinematic",
  "description": "Starts a cinematic sequence from JSON in the database",
  "moduleId": "cinematic",
  "color": "#9C27B0",
  "category": "Cinematic",
  "parameters": [
    {
      "name": "cinematicId",
      "type": "string",
      "label": "Cinematic ID:",
      "description": "ID de la cinématique à lancer",
      "defaultValue": ""
    }
  ]
}
```

### 4. Module Packaging

Build your module as a standard Maven JAR. Dependencies on `NextDungeon-Common` should be `<scope>provided</scope>` since the host plugin provides them.

Place the JAR in `plugins/NextDungeon/modules/` — it will be loaded automatically at startup.

---

## Part 2: Cinematic Module (Built-in Example)

The **module-cinematic** subproject demonstrates a complete dynamic module.

### Registered Blocks

| Block ID | Type | Description |
|----------|------|-------------|
| `cinematic.start` | ACTION | Start a cinematic sequence |
| `cinematic.stop` | ACTION | Stop the current cinematic |
| `cinematic.add_camera_waypoint` | ACTION | Add a camera waypoint to timeline |
| `cinematic.move_npc` | ACTION | Move/sync an NPC on the timeline |
| `cinematic.timeline_event` | ACTION | Inject a timed event (command, sound, etc.) |
| `cinematic.on_end` | TRIGGER | Fires when a cinematic finishes |
| `cinematic.is_playing` | CONDITION | Check if a cinematic is playing |

### Cinematic JSON Schema

Stored in the `cinematics` table as `payload_json`:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Intro Cutscene",
  "creator": "admin",
  "durationTicks": 200,
  "lockCamera": true,
  "hideHud": true,
  "cameraWaypoints": [
    {
      "tick": 0,
      "x": 100.0, "y": 70.0, "z": 200.0,
      "yaw": 0.0, "pitch": -10.0,
      "interpolation": "CATMULL_ROM"
    },
    {
      "tick": 100,
      "x": 120.0, "y": 75.0, "z": 210.0,
      "yaw": 45.0, "pitch": -5.0,
      "interpolation": "LINEAR"
    }
  ],
  "npcActors": [
    {
      "actorId": "guard_1",
      "displayName": "Royal Guard",
      "skinTexture": "base64...",
      "skinSignature": "base64...",
      "waypoints": [
        { "tick": 0, "x": 105.0, "y": 65.0, "z": 205.0, "yaw": 90.0, "pitch": 0.0, "animation": "IDLE" },
        { "tick": 60, "x": 110.0, "y": 65.0, "z": 205.0, "yaw": 90.0, "pitch": 0.0, "animation": "WALK" }
      ]
    }
  ],
  "events": [
    { "tick": 0, "type": "TITLE", "parameters": { "title": "Chapter 1", "subtitle": "The Beginning" } },
    { "tick": 50, "type": "SOUND", "parameters": { "sound": "minecraft:entity.experience_orb.pickup" } },
    { "tick": 100, "type": "COMMAND", "parameters": { "command": "say The guard approaches!" } }
  ]
}
```

### Camera Interpolation Modes

- **LINEAR** — Straight-line movement between waypoints
- **CATMULL_ROM** — Smooth spline interpolation (recommended for fluid camera paths)
- **CUBIC** — Smoothstep cubic easing

---

## Part 3: API CRUD Specification

### Cinematics API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/cinematics` | List all cinematics (id, name, creator) |
| GET | `/cinematics/{id}` | Get cinematic payload JSON |
| POST | `/cinematics` | Create a new cinematic |
| PUT | `/cinematics/{id}` | Update an existing cinematic |
| DELETE | `/cinematics/{id}` | Delete a cinematic |

### Workflows API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/workflows/{id}` | Get workflow graph JSON |
| POST | `/workflows` | Create/save a workflow |
| PUT | `/workflows/{id}` | Update a workflow |
| DELETE | `/workflows/{id}` | Delete a workflow |

### Module Catalog API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/module-blocks` | Get all module block descriptors as JSON |
| GET | `/api/blockly.js` | Get generated Blockly JS (includes module blocks) |

---

## Part 4: Database Schema

### MySQL Tables

```sql
-- Cinematics (JSON payloads stored in DB)
CREATE TABLE IF NOT EXISTS cinematics (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    creator VARCHAR(32),
    payload_json MEDIUMTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Workflows (Blockly graph JSON)
CREATE TABLE IF NOT EXISTS workflows (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    graph_json MEDIUMTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Existing: floor_triggers
CREATE TABLE IF NOT EXISTS floor_triggers (
    floor_id VARCHAR(255) PRIMARY KEY,
    triggers_data MEDIUMTEXT NOT NULL,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### MongoDB Collections

- `cinematics` — Documents with `_id`, `name`, `creator`, `payload_json`, `updated_at`
- `workflows` — Documents with `_id`, `name`, `graph_json`, `updated_at`
- `floor_triggers` — Existing collection

---

## Part 5: End-to-End Execution Flow

### 1. Module Registration (Server Startup)

```
Server starts
  → ModuleLoader scans plugins/NextDungeon/modules/
  → Loads each JAR via URLClassLoader
  → Finds NextDungeonModule implementations
  → Calls module.onEnable(ctx)
  → Module registers blocks in ModuleBlockRegistry
```

### 2. Web Panel Block Discovery

```
Editor opens in browser
  → Requests /api/blockly.js
  → BlocklyJavaScriptGenerator runs:
      1. Scans annotated classes (existing triggers/actions)
      2. Reads ModuleBlockRegistry for dynamic blocks
      3. Generates JS block definitions + toolbox categories
  → Blockly workspace shows all blocks (core + module)
```

### 3. Workflow Creation

```
User drags blocks in Blockly
  → Constructs visual workflow
  → "Save" button clicked
  → Blockly workspace serialized to JSON
  → POST /api/save → stored in floor_triggers table
```

### 4. Workflow Execution (Runtime)

```
In-game event fires (player enters region, clicks block, etc.)
  → TriggersRegistry checks registered triggers
  → TriggerFactory deserializes from JSON if needed
  → Trigger.execute() checks conditions
  → ActionSequenceExecutor runs action chain:
      - Core actions: handled by built-in Action classes
      - Module actions: ActionFactory checks ModuleLoader.getActionHandler()
        → ModuleAction delegates to ModuleActionHandler.execute()
```

### 5. Cinematic Playback

```
Workflow calls "cinematic.start" action with cinematicId
  → Module handler loads CinematicData from DB (JSON → POJO via Gson)
  → Creates CinematicPlayer for the viewer
  → Each server tick:
      - CameraInterpolation computes camera position
      - NPC positions interpolated
      - Timeline events fired at matching ticks
  → On completion: callback.onComplete() fires
```

---

## Part 6: Web Panel Integration

The web panel automatically discovers module blocks because:

1. `BlocklyJavaScriptGenerator.generateModuleBlocks()` creates Blockly block definitions from `ModuleBlockDescriptor` objects
2. `BlocklyJavaScriptGenerator.generateModuleToolboxCategories()` adds them to the toolbox

No rebuild of the web panel is needed — blocks appear dynamically when the editor is opened.

### Example: How a module block appears in Blockly

For `cinematic.start`, the generator produces:

```javascript
Blockly.Blocks['cinematic_start'] = {
    init: function() {
        this.appendDummyInput()
            .appendField("🎬 Start Cinematic");
        this.appendDummyInput()
            .appendField("Cinematic ID:")
            .appendField(new Blockly.FieldTextInput(""), 'cinematicId');
        this.setPreviousStatement(true, "Action");
        this.setNextStatement(true, "Action");
        this.setColour('#9C27B0');
        this.setTooltip("Starts a cinematic sequence from JSON in the database");
    }
};
```

---

## Part 7: Hot Reload and Security

### Hot Reload

Currently, modules are loaded once at server startup. To reload:
1. Stop the server
2. Replace/update module JARs in `plugins/NextDungeon/modules/`
3. Restart the server

Future: live reload via `/dungeon admin reload-modules` command.

### Security

- Each module runs in its own `URLClassLoader` for class isolation
- Module JARs should only be placed by server administrators
- All database operations use parameterized queries (SQL injection prevention)
- Module actions receive only the parameters declared in their block descriptors

### Versioning

- Workflow JSON is versioned via the `updated_at` timestamp
- Cinematics have `created_at` and `updated_at` timestamps
- Module version is exposed via `NextDungeonModule.getVersion()`
