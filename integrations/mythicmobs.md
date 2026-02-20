---
description: >-
  Using MythicMobs for custom mobs and bosses inside dungeon floors.
icon: dragon
---

# MythicMobs Integration

MythicMobs by Lumine allows server administrators to create highly customised mobs, bosses, and item drops. NextDungeon integrates with MythicMobs through the workflow action system.

## Dependency Type

MythicMobs is a **soft dependency** (listed in `softdepend` in `plugin.yml`). NextDungeon loads without it, but MythicMob-specific actions only function when MythicMobs is installed.

---

## How It Works

The `SummonMobAction` and `SummonMobInRegionAction` workflow actions can spawn both vanilla Minecraft entities and MythicMobs creatures.

### SummonMobAction

Spawns a single entity at a specific location.

| Field | Type | Description |
|-------|------|-------------|
| Entity type | string | Vanilla type (e.g. `ZOMBIE`) or a MythicMobs mob name (e.g. `SkeletonKing`) |
| Location | coordinates | X, Y, Z spawn coordinates within the floor world |
| Count | integer | Number of entities to spawn |

### SummonMobInRegionAction

Spawns entities at random positions within a cuboid region.

| Field | Type | Description |
|-------|------|-------------|
| Entity type | string | Vanilla type or MythicMobs mob name |
| Region | cuboid | Min and max corner coordinates |
| Count | integer | Number of entities to spawn |

<!-- INSERT HERE: screenshot of the Blockly editor with SummonMobAction and a MythicMobs mob name -->

---

## Setting Up MythicMobs

1. Install [MythicMobs](https://www.spigotmc.org/resources/mythicmobs.5702/) on all instance servers.
2. Create custom mob templates in `plugins/MythicMobs/mobs/`.
3. Note the exact mob template **name** (e.g. `BoneKing`) — this is what you enter in `SummonMobAction`.
4. Ensure MythicMobs is in the `plugins/` folder on every instance server (including CloudNet templates).

---

## Example: Boss Spawn on Region Entry

In the Blockly editor:

1. Add a `RegionTrigger` (region: `boss_room`, event: `enter`)
2. Attach `SendTitleAction` (title: `The Bone King Awakens!`)
3. Attach `SummonMobAction` (type: `BoneKing`, coordinates of boss spawn)
4. Attach `PlaySoundAction` (sound: `ENTITY_ENDER_DRAGON_GROWL`)

<!-- INSERT HERE: video demonstration of a MythicMobs boss spawn triggered by region entry -->

---

## Detecting Boss Death

Use `EntityDeathTrigger` to detect when a MythicMobs boss dies:



---

## Kill Tracking

The `InstanceMobKillListener` records every entity death inside a dungeon instance. Kill counts are stored in `PlayerStats.enemiesKilled` and displayed on the dungeon completion screen.

---

## Notes

* MythicMobs drops and skills work as configured in the mob templates — NextDungeon does not override them.
* Custom MythicMobs items can be used as `required_items` or `forbidden_items` in floor requirements (matched by item display name).
