---
description: How to modify existing dungeons, update floor configurations, and manage workflows.
icon: pencil
---

# Editing Dungeons

This page covers how to modify an existing dungeon — changing floor settings, updating the world, and editing the trigger-action workflow.

---

## Editing Floor Configuration

### Via Web Dashboard

1. Open the dashboard in your browser.
2. Navigate to the dungeon and select the floor you want to edit.
3. Modify any field (name, requirements, rules, steps, etc.).
4. Click **Save**. The change is pushed to the `{topic}:sync` Redis channel and all connected lobby servers reload the floor automatically.

<!-- INSERT HERE: screenshot of the dashboard floor edit form -->

### Via YAML + Redis Migration

1. Edit the YAML file in `plugins/NextDungeon/dungeons/`.
2. Run `/dungeon admin load <config>` to reload the file into memory.
3. Run `/dungeon admin migrate-to-redis <config>` to persist the changes to Redis.

> **Note:** If `DungeonLoader` is set to `redis`, the in-memory changes from `/dungeon admin load` will be lost on the next server restart unless you also migrate to Redis.

---

## Editing the World (Build Changes)

To make changes to the physical dungeon world:

### Step 1: Enter Edit Mode

From a lobby server:

```
/dungeon admin edit start <dungeonId> <floorId>
```

You are sent to a dedicated CloudNet instance in edit mode. The `editMode` property is set on the service, so the instance server initialises in edit mode (`ServerUtil.isInEditMode()` returns `true`).

### Step 2: Make World Changes

Build, remove, or rearrange structures freely in the world. No players will be affected.

### Step 3: Save and Exit

```
/dungeon admin edit stop --confirm
```

This calls `InstanceProvider.saveEditWorldToTemplate(floor)`, which saves the current world back to the CloudNet task template. The edit server then shuts down.

> **Warning:** If you run `/dungeon admin edit stop` without `--confirm`, the plugin first checks whether triggers exist in the database for this floor. If no triggers are found it warns you. If triggers exist it asks for confirmation before proceeding. This prevents accidental saves.

---

## Editing Workflows (Triggers and Actions)

Workflows are edited via the **Blockly web editor** while in edit mode.

### Starting the Web Editor

While on the edit server:

```
/dungeon admin webeditor start
```

The plugin:
1. Starts an HTTP server on the proxy (via `DungeonWebEditorManager`)
2. Sends you a clickable URL in chat
3. Maintains the editor session in `EditorSessionManager`

### Using the Editor

The Blockly workspace is divided into categories:

| Category | Contents |
|----------|---------|
| **Triggers** | `BlockClickTrigger`, `ChatMessageTrigger`, `EntityDeathTrigger`, `FunctionTrigger`, `ItemPickupTrigger`, `PlayerDamageTrigger`, `PlayerJumpTrigger`, `RegionTrigger` |
| **Actions** | All action blocks (`SendMessageAction`, `TeleportLocationAction`, `SummonMobAction`, etc.) |
| **Logic** | `IfCondition` and other conditional blocks |
| **Variables** | `SetVariableAction`, `GetVariableAction`, `AddToVariableAction`, etc. |
| **WorldEdit** | WorldEdit-based actions |

**Configuring a Trigger:**

1. Drag a trigger block onto the workspace (e.g. `RegionTrigger`).
2. Set the trigger parameters (region name, enter/exit, etc.).
3. Attach action blocks inside the trigger block.
4. Configure each action (message text, target player, coordinates, etc.).
5. (Optional) Add condition blocks to gate the actions.

**Variable Placeholders in Messages:**

Use these in any message or title text:

| Placeholder | Value |
|-------------|-------|
| `{player}` | Name of the triggering player |
| `{global.varName}` | Value of a global variable |
| `{player.varName}` | Value of a player-scoped variable |
| `{trigger}` | Name of the triggering trigger |

### Saving the Workflow

Click the **Save** button in the editor. The workflow is serialised to JSON by `EditorSerializer`, sent to the Spigot server via Redis messaging, and persisted to the database by `DatabaseTriggersManager`.

> Changes are saved to the database immediately. You do not need to stop edit mode to persist workflow changes.

### Stopping the Web Editor

```
/dungeon admin webeditor stop
```

---

## Live Trigger Refresh

On lobby servers, triggers are cached in `TriggersRegistry`. When a floor is updated via the dashboard sync channel, the registry is refreshed automatically:

```
[DashboardSync] Rechargement floor : example_floor1
```

On instance servers, triggers are loaded when the instance initialises (`TriggersRegistry.refreshTriggerCache()`).

---

## Renaming or Deleting a Dungeon

### Via Code / Dashboard

The `Dungeon.rename(String newName)` method updates the name and syncs to Redis. `Dungeon.delete()` removes the dungeon from Redis storage.

### Via Admin Commands

Currently there are no in-game rename/delete commands. Use the dashboard or direct Redis management for these operations.

---

## Verifying Your Changes

After editing:

```
/dungeon admin status
/dungeon admin test <dungeonId> <floorId>
```

Confirm that:
* The new floor configuration is loaded (`/dungeon list`)
* Triggers fire at the expected moments during the test run
* The dungeon completes correctly
