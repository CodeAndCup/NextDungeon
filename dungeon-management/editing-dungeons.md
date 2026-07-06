---
description: How to modify existing dungeons — floor settings, world builds, and trigger workflows.
icon: pencil
---

# Editing Dungeons

This page covers changing a dungeon that already exists: its floor settings, its world, and its trigger workflows.

---

## Editing Floor Settings

Open the web dashboard, select the dungeon and floor, change any setting (name, requirements, rules, steps…), and click **Save**. Every lobby server picks up the change automatically — no restart needed.

---

## Editing the World

To change the actual build (add rooms, move structures, fix terrain):

**1. Enter edit mode** from a lobby server:

```
/dungeon admin edit start <floorId>
```

You're sent to a private edit server. No live players are affected.

**2. Make your changes** — build normally.

**3. Save and exit:**

```
/dungeon admin edit stop --confirm
```

This saves the world back to the floor's template and shuts the edit server down.

> Running `/dungeon admin edit stop` **without** `--confirm` first checks whether the floor already has triggers and asks you to confirm — a safety net against accidentally overwriting a floor.

---

## Editing Triggers & Actions

Trigger workflows are edited in the **visual editor** while you're in edit mode.

**Open it** (on the edit server):

```
/dungeon admin webeditor start
```

The plugin sends you a clickable link. Open it in your browser to see the workspace, organised into categories:

| Category | What's inside |
|----------|---------------|
| **Triggers** | Everything that can start a workflow — see [Triggers](../workflow/triggers.md) |
| **Actions** | Everything a trigger can do — see [Actions](../workflow/actions.md) |
| **Logic** | *If* and *For* blocks — see [Conditions & Variables](../workflow/conditions-and-variables.md) |
| **Conditions** | Checks like *Player has item*, *Player in region*… |
| **Variables** | Set, add, subtract, random, and math blocks |
| **Cinematic / WorldEdit** | Extra blocks added by [modules](../modules/overview.md), when installed |

**Build a workflow:**

1. Drag a trigger onto the workspace and set its fields (region corners, block type, etc.).
2. Attach action blocks inside the trigger.
3. Fill in each action (message text, target, coordinates…).
4. Optionally wrap actions in a condition so they only run when your rule is met.
5. Click **Save**. The workflow is saved immediately — you don't need to leave edit mode.

**Text placeholders.** In any message, title, or command you can use `{player}`, `{trigger}`, `{global.name}`, and `{player.name}` — see [Conditions & Variables](../workflow/conditions-and-variables.md#placeholders-in-text).

**Close the editor** when finished:

```
/dungeon admin webeditor stop
```

---

## Renaming or Deleting a Dungeon

Use the web dashboard to rename or delete a dungeon. There are no in-game commands for these actions.

---

## Verifying Your Changes

After editing, launch the floor and watch it run:

```
/dungeon admin run <floorId>
/dungeon admin status
```

Confirm that:

* The floor loads and appears in `/dungeon admin list`
* Triggers fire at the right moments during the run
* The floor completes correctly
