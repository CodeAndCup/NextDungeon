---
description: >-
  A detailed look at every feature NextDungeon brings to your Minecraft server.
icon: magnifying-glass
---

# Feature Overview

## 1. Multi-Floor Dungeon System

Each **dungeon** is a container for one or more **floors**. Floors are the playable levels, and each one is configured on its own with:

* **World settings** — difficulty and spawn point
* **Requirements** — minimum level, prerequisite floors, required/forbidden items, party size
* **Rules** — max lives, death penalty, game mode, flight, max concurrent instances
* **Steps** — named regions that mark progress through the floor
* **Triggers and actions** — the automation attached to the floor

Dungeon and floor definitions are shared across all your servers automatically, so every lobby shows the same up-to-date content.

## 2. Trigger-Action Workflow Engine

The workflow system lets you script rich dungeon behaviour with no coding — you build everything visually in the web editor.

* **Triggers** react to what happens in the dungeon (a player enters a region, clicks a block, kills a mob, sends a chat message…). See the [Triggers reference](../workflow/triggers.md).
* **Actions** are what happens next (send a message, teleport, spawn mobs, give items, play sounds, run commands, end the run…). See the [Actions reference](../workflow/actions.md).
* **Conditions & loops** add logic — run actions only when your rules are met, or repeat them — plus **variables** to remember values like scores and counters. See [Conditions & Variables](../workflow/conditions-and-variables.md).

<!-- INSERT HERE: screenshot of a trigger with attached actions in the web editor -->

## 3. Visual Web Editor

When you open a floor in edit mode, you get a drag-and-drop editor in your browser. Snap triggers, actions, and conditions together, fill in their fields, and save — the workflow goes live immediately. See [Editing Dungeons](../dungeon-management/editing-dungeons.md).

<!-- INSERT HERE: video demonstration of the editor in action -->

## 4. Isolated Instances

Every floor run happens on its own dedicated server, created on demand and cleaned up when the run ends. Players are routed to it automatically once it's ready. This keeps runs isolated from each other and from the lobby. See the [CloudNet integration](../integrations/cloudnet.md).

## 5. Queue System

When a floor is full, players wait in a queue and receive position updates. As instances free up, the next players are pulled in automatically and sent straight to a fresh instance.

## 6. Revive & Ghost System

On death inside a dungeon, a player becomes a ghost for a configurable time and is placed at their fallen body (the corpse) rather than being sent to spectate. Teammates can use the **revive item** to bring them back at that spot. If the timer runs out first, the player loses a life; running out of lives triggers a configurable penalty command. Tune it all under `ReviveSystem` in the [config](../configuration/main-config-file.md).

## 7. Party Play

Players tackle dungeons in groups. NextDungeon works with the **AlessioDP Parties** plugin or its own **built-in party system** (chosen with `PartyProvider.type`). Only the **party leader** starts a dungeon, and a leader can launch **directly from any existing party** without using the Party Finder first. See [Parties Integration](../integrations/parties.md).

## 8. Player Profiles & Statistics

Each player's progress is saved: which floors they've completed, plus per-run stats like completion time, enemies killed, deaths, and success/failure. This data drives floor prerequisites and leaderboards.

## 9. Web Dashboard

A browser dashboard (served by your proxy) lets you create and edit dungeons and floors visually. Changes sync to every lobby server automatically.

<!-- INSERT HERE: screenshot of the web dashboard -->

## 10. Admin & Debug Tools

A full set of in-game commands covers floor editing, testing, queue management, instance inspection, and module management — see [Commands & Permissions](commands-and-permissions.md).

## 11. Add-on Modules

NextDungeon can load extra **modules** that add new triggers, actions, and conditions to the editor. Two ship with the plugin:

| Module | Adds |
|--------|------|
| **Cinematic** | Scripted cinematics — camera paths, titles, sounds, screen effects |
| **WorldEdit** | Region operations — set, cut, replace, and paste schematics |
| **Memory Labyrinth** | A roguelike dungeon mode — random rooms, door choices, bosses, and endless runs |

Module blocks appear in the editor automatically once the module is loaded. See the [Modules Overview](../modules/overview.md).

***

Ready to build? Head to [Getting Started](../getting-started/installation.md).
