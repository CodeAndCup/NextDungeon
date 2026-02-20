---
description: Integrating NextDungeon with MMOCore for RPG-style level requirements.
icon: star
---

# MMOCore Integration

MMOCore by MagicGlens (Indyuce) is an advanced RPG core plugin for Minecraft. NextDungeon integrates with MMOCore to enforce **player level requirements** for entering dungeon floors.

## Dependency Type

MMOCore is declared as a **hard dependency** in `plugin.yml`:

```
depend: [ MMOCore, packetevents ]
```

This means NextDungeon will **not load** if MMOCore is not present on the server.

---

## How It Works

When a player attempts to enter a floor, `Floor.isRequirementsValid(player)` is called. The MMOCore integration is in this check:

```java
PlayerData playerData = PlayerData.get(player);
if (this.getRequirements().getMinLevel() > 0) {
    if (playerData.getLevel() < this.getRequirements().getMinLevel()) {
        return false;
    }
}
```

`PlayerData.get(player)` retrieves the MMOCore player data, and `.getLevel()` returns the player's current MMOCore level. If the player's level is below the floor's `minimum_level`, entry is denied.

---

## Configuring Level Requirements

In your floor's YAML configuration:

```yaml
requirements:
  minimum_level: 10    # Minimum MMOCore level required to enter this floor
```

| Value | Behaviour |
|-------|-----------|
| `0` | No level requirement (any player can enter) |
| `> 0` | Player must have at least this MMOCore level |

---

## Prerequisites

1. Install [MMOCore](https://www.spigotmc.org/resources/mmocore.87699/) on all game servers.
2. Place `MMOCore.jar` in the `plugins/` folder.
3. Configure MMOCore to track player levels.

> MMOCore must be installed even if you set `minimum_level: 0` on all floors, because it is a hard dependency.

---

## Completed Floor Tracking

NextDungeon stores completed floor IDs in `ProfileData.completedFloors` (persisted via `ProfileService`). This is used for:

* The `required_floor` requirement check (prerequisite floors)
* The completion screen stat display

This data is independent of MMOCore — it is managed by NextDungeon's own `ProfileService`.

---

## Future Plans

A developer API for exposing dungeon completion events to MMOCore quests, rewards, and progression systems is planned for a future release.
