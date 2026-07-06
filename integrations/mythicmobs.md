---
description: >-
  Using MythicMobs for custom mobs and bosses inside dungeon floors.
icon: dragon
---

# MythicMobs Integration

**MythicMobs** (by Lumine) lets you create custom mobs, bosses, and drops. NextDungeon can spawn them straight from your trigger workflows.

## Optional Plugin

MythicMobs is **optional**. NextDungeon runs fine without it — but the mob-spawning actions can only spawn MythicMobs creatures when MythicMobs is installed. Install it on every **instance server** (including your floor templates) if you want to use custom mobs.

---

## Spawning Custom Mobs

Two actions can spawn both vanilla entities and MythicMobs creatures — just type the MythicMobs mob name (e.g. `SkeletonKing`) in the mob-type field:

* **💠 Summon Mob** — spawns one mob at a position.
* **👾 Summon Mobs in Region** — spawns several mobs at random spots inside a region.

See the [Actions reference](../workflow/actions.md#summon-mob) for their full fields.

<!-- INSERT HERE: screenshot of the Summon Mob action with a MythicMobs mob name -->

---

## Setting Up MythicMobs

1. Install [MythicMobs](https://www.spigotmc.org/resources/mythicmobs.5702/) on all instance servers.
2. Create your mob templates in `plugins/MythicMobs/mobs/`.
3. Note each mob's exact **name** (e.g. `BoneKing`) — that's what you type into the Summon Mob action.

---

## Example: Boss Spawn on Region Entry

In the editor:

1. Add a **Region Enter/Exit** trigger (region: the boss room, event: *Enter*).
2. Attach **Send Title** ("The Bone King Awakens!").
3. Attach **Summon Mob** (mob type: `BoneKing`, at the boss spawn point).
4. Attach **Play Sound** (e.g. ender dragon growl).

<!-- INSERT HERE: video of a MythicMobs boss spawn triggered by region entry -->

---

## Detecting Boss Death

Use the **Entity Death** trigger with the mob's name in the *Entity* field (e.g. `BoneKing`) to run actions when the boss dies — open a gate, drop rewards, or end the run:

1. Add an **Entity Death** trigger (entity: `BoneKing`).
2. Attach **Send Title** ("The Bone King is slain!") and any rewards.
3. Attach **End Dungeon** to finish the run.

---

## Kill Tracking

Every mob killed inside a dungeon is counted automatically and shown on the completion screen — no setup needed.

---

## Notes

* MythicMobs drops and skills work exactly as configured in your mob templates; NextDungeon doesn't change them.
* Custom MythicMobs items can be used as required or forbidden items in a floor's requirements (matched by display name).
