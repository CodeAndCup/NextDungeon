---
description: Gate dungeon floors behind MMOCore player levels.
icon: star
---

# MMOCore Integration

NextDungeon uses **MMOCore** (by Indyuce) for RPG-style **level requirements** on dungeon floors.

## Required Plugin

MMOCore is a **hard dependency** — NextDungeon will not start without it. Install it even if you don't use level requirements on any floor.

1. Install [MMOCore](https://www.spigotmc.org/resources/mmocore.87699/) on all game servers (including your floor instance templates).
2. Configure MMOCore so it tracks player levels as you want.

---

## Setting a Level Requirement

In a floor's **Requirements** (in the web dashboard), set the **minimum level**:

| Minimum level | Behaviour |
|---------------|-----------|
| `0` | No level requirement — anyone can enter |
| `> 0` | The player must be at least this MMOCore level |

A player below the required level is turned away when they try to launch the floor, and the requirement shows as unmet on the floor's icon.

---

## Prerequisite Floors

Floors can also require that another floor be **completed first**. NextDungeon records each player's completed floors automatically and checks them alongside the level requirement. This is separate from MMOCore — it's tracked by NextDungeon itself.

---

## Future Plans

An API to expose dungeon completion to MMOCore quests, rewards, and progression is planned for a future release.
