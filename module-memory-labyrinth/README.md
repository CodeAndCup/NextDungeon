# Memory Labyrinth — Admin User Guide

Procedural dungeon module inspired by **Hades** and **Archero**. Players
explore a labyrinth of pre-built rooms with branching door choices,
visible reward icons, a boss every 10 rooms, and an optional infinite
mode with per-boss checkpoints.

> Full design specification : `MEMORY_LAB.md` at the repository root.

---

## 1. Installation

1. Build the module (or use the JAR you were shipped) :
   ```
   mvn -pl module-memory-labyrinth -am clean package
   ```
2. Drop `module-memory-labyrinth-<version>.jar` into
   `plugins/NextDungeon/modules/`.
3. Restart the server. The host's `ModuleLoader` auto-discovers the
   JAR ; you should see this in the console :
   ```
   [ModuleLoader] Loaded module: Memory Labyrinth Module v0.1.0 (memory_labyrinth) ...
   ```

No DB migration required — the only labyrinth-specific table is
`labyrinth_saves`, created automatically on first boot.

---

## 2. Building a labyrinth dungeon (admin walkthrough)

Everything is configured from the **NextDungeon dashboard**. There are
no JSON files to import, no CLI commands required.

### 2.1 Pre-build the rooms in a world

Pick a world dedicated to your labyrinth pool (e.g. `labyrinth_pool`)
and physically build :

- **1 lobby room** — entry-side, no mobs, where players land for a fresh
  run.
- **N combat rooms** (8+ recommended for variety) — fights with mobs,
  with two visible exit doors.
- **1+ boss rooms** — single exit, contains a boss mob.

For each room, note down :
- The **cuboid region** (min and max coordinates).
- The **player spawn point** (where players are TP'd on entry).
- The **exit door anchor(s)** : 1 anchor for lobby/boss, 2 anchors for
  combat (left and right candidates).

You'll also pick a **dungeon spawn point** — a single location anywhere
in the world where players land at run start, before they're sent to
the lobby (or directly to a saved room in Infinite resume).

### 2.2 Create the dungeon in the dashboard

1. Open the **Dungeon Editor** in the web dashboard.
2. Click **+ Nouveau Donjon**.
3. Fill in id / name / description.
4. **Type** → select `LABYRINTH`.
5. Save. You're routed to the editor view.

A new **🌀 Labyrinth** tab appears next to "📋 Informations". Open it.

### 2.3 Configure the dungeon-level Labyrinth tab

- **World ID** — the Bukkit world your rooms live in (e.g.
  `labyrinth_pool`).
- **Dungeon spawn point** — the X/Y/Z where players TP at run start.
- **Pool de rooms** — click **+ Ajouter une room** to open the room
  editor sub-modal :
  - **ID** — unique within the dungeon (e.g. `combat_t1_skeletons`).
  - **Type** — `LOBBY` / `COMBAT` / `BOSS`.
  - **World ID** — leave empty to inherit the dungeon's world.
  - **Region** — min/max cuboid of the build.
  - **Player spawn** — where players appear on entry.
  - **Exit doors** — anchors that drive door icons + traversal
    detection. 1 for lobby/boss, 2 for combat (first = left, second =
    right).
  - **Mob spawns** — list of `(mobId, x, y, z, count)`. `mobId` is
    resolved against MythicMobs first, then a vanilla `EntityType`.
  - **Tags** — comma-separated. Used by the picker's tag filter
    (typically the floor name : `easy`, `normal`, `hard`,
    `infinite`).
  - **Fixed icon** — leave empty for COMBAT to roll an icon at
    door-proposal time ; required for BOSS ; auto-NONE for LOBBY.

Save the dungeon when you're done — the rooms are persisted in the
dungeon's payload (no separate DB table).

### 2.4 Add difficulties as floors

In the **🏗️ Floors** tab, click **+ Ajouter Floor**. Each floor of a
labyrinth dungeon represents one difficulty.

In the **Basique** tab :
- **ID** — typically `easy`, `normal`, `hard`, `infinite`.
- **Nom** — display name.
- **Type de floor** → `LABYRINTH` (auto-set when the dungeon is
  LABYRINTH).

Once `LABYRINTH` is selected, a **🌀 Labyrinth** sub-tab appears in
the floor modal. Open it to configure the difficulty :

- **Max rooms** — finite floor cap (`30`/`50`/`70`…). Set to `0` for
  the infinite floor.
- **Tag filter** — only rooms carrying this tag are eligible (typically
  the floor's id, e.g. `easy`).
- **HP scaling per tier** / **DMG scaling per tier** — overrides of the
  defaults (`0.30` HP / `0.15` DMG). Leave at `0` to use the module
  defaults.
- **Save activée** — check it for Infinite, leave unchecked for
  finite floors.
- **Loot table** :
  - **Base gold** — flat amount per player at run end.
  - **Gold per icon** — multiplier added per `GOLD` icon collected
    (e.g. `0.15` = +15 % per gold icon).
  - **Base item rolls** — number of weighted rolls per player.
  - **Items** — add entries `(itemId, weight, minTier)`. `minTier`
    gates the entry by the player's current difficulty tier.

Save the floor. Repeat for every difficulty you want to ship.

> **Tip — same room pool, different difficulties** : you typically
> tag combat rooms with multiple floor names (`easy`, `normal`, `hard`,
> `infinite`) so the same pool serves all difficulties, while boss
> rooms carry only the floors they're balanced for.

---

## 3. Player flow

Players reach your labyrinth like any other dungeon — the existing
dashboard menu / `/dungeon` flow still applies.

When they pick the labyrinth dungeon and a difficulty :

1. The host plugin creates a `FloorInstance` and TPs the players to
   the dungeon's server.
2. Once the instance is ready, the module's
   `LabyrinthInstanceReadyListener` takes over and TPs everyone to the
   **dungeon spawn point**.
3. **Infinite floor with an existing save** :
   - The module fetches the save by `partyHash` (sha256 of the sorted
     party UUIDs).
   - The party leader sees a chat-clickable prompt :
     `[Reprendre]` / `[Nouvelle partie]`.
   - **Reprendre** → players are TP'd directly into the next room
     (last cleared boss room + 1) with the saved tier and accumulated
     icons restored.
   - **Nouvelle partie** → the save is deleted ; players are TP'd to
     the lobby for a fresh run.
4. **No save / classic floor** → players are TP'd straight to the
   lobby.
5. The standard loop kicks in :
   - Spawn mobs, lock doors.
   - Players clear the room → doors unlock with their icons floating
     above as item displays.
   - Walk through a door → next room. Repeat.
   - Boss every 10 rooms → tier+1 ; in Infinite, save is upserted ;
     dead party members can be revived once via a chat prompt.
6. End of run (finite completion / total wipe / voluntary exit) :
   loot is calculated per player from the icon counts and the floor's
   loot table, and the `labyrinth.on_run_ended` Blockly trigger
   fires.

The **action bar** displays `🌀 Salle X · Palier Y` continuously
during the run.

---

## 4. Blockly hooks (Option C — events only)

The module exposes **no actions** — workflows hook into seven triggers
and three conditions to graft side effects (cinematics, sounds, gold
distribution via Vault/MMOCore, end-of-run titles, etc.).

| Block | Type | Variables |
|---|---|---|
| `labyrinth_on_room_entered` | TRIGGER | `roomIndex`, `roomType`, `rewardIcon`, `playerUuid` |
| `labyrinth_on_room_cleared` | TRIGGER | `roomIndex`, `rewardIcon`, `clearTimeMs` |
| `labyrinth_on_doors_proposed` | TRIGGER | `iconLeft`, `iconRight`, `nextIsBoss` |
| `labyrinth_on_boss_killed` | TRIGGER | `roomIndex`, `tier`, `playersAlive` |
| `labyrinth_on_run_ended` | TRIGGER | `success`, `goldEarned`, `itemsRolled`, `iconCounts`, `tier`, `finalRoomIndex` |
| `labyrinth_on_checkpoint_saved` | TRIGGER | `roomIndex`, `tier` |
| `labyrinth_on_save_invalidated` | TRIGGER | `reason` (`ALL_DEAD` / `VOLUNTARY_EXIT`) |
| `labyrinth_is_boss_room` | CONDITION | — |
| `labyrinth_is_infinite_floor` | CONDITION | — |
| `labyrinth_has_resumable_save` | CONDITION | — |

The most common hook is `labyrinth_on_run_ended` — wire it to your
economy plugin (`mmocore add-currency`, `eco give`, etc.) to actually
credit the `goldEarned` value.

---

## 5. Soft dependencies

| Plugin | Detected at | Behaviour without |
|---|---|---|
| **MythicMobs** | First mob spawn | Falls back to `EntityType.valueOf(mobId)` (vanilla mobs only). |
| **Vault / MMOCore for gold** | Never (CDC §6.5) | `goldEarned` is exposed on `on_run_ended` only — admins wire the actual currency credit via Blockly. |

---

## 6. Reward icons

| Icon | v1 status | Effect |
|---|---|---|
| 🪙 `GOLD` | implemented | Gold reward at end of run = `baseGold × (1 + goldPerIcon × iconCount) × (1 + 0.10 × (tier − 1))`. |
| ✨ `BLESSING` | reserved | Enum exists, not rolled in v1 (placeholder for a future "buff between rooms" mechanic). |

The icon shown above each door at choice time is rolled when the
previous room is cleared :
- LOBBY → always `NONE` (no icon displayed).
- BOSS → uses the room's `fixedIcon` (admin-defined).
- COMBAT → uses `fixedIcon` if set, otherwise rolled uniformly from
  the v1 rollable set (currently `[GOLD]`).

---

## 7. Save (Infinite)

- A save is **automatically upserted** at every boss kill on the
  Infinite floor.
- The save is keyed by `(partyHash, floorId)` where
  `partyHash = sha256(sorted(initialPlayerUuids))`. Only a party with
  the **exact same UUIDs as at run start** can resume it.
- A save is **deleted** when : the group wipes, a leader chooses
  « Nouvelle partie » at the resume prompt, or a player voluntarily
  leaves the Infinite run.
- Each save carries a `checksum` (sha256 of the canonical payload) ;
  the module refuses to resume a save whose checksum doesn't match
  (anti-tamper).
- Saves are stored in the `labyrinth_saves` MySQL table /
  `labyrinth_saves` Mongo collection. They are **cross-server** —
  the same party can resume from any server connected to the same DB.

---

## 8. Limitations & v1 hors-scope

- **No live preview** in the dashboard — admins build rooms in-world
  and reference them by coordinates. A "click to set position" pad is
  a v2 polish.
- **`BLESSING` icon** — reserved enum, not rolled in v1.
- **Mythic / MMOCore items at end-of-run** — vanilla `Material` ids
  only ; Mythic item resolution is a follow-up.
- **Pending offline loot** — players who are offline at run end are
  skipped for inventory drops. Gold flows via the Blockly trigger so
  admins can queue it via their economy of choice.
- **Voluntary exit detection from inside the labyrinth** — the v1
  module doesn't subscribe to a "player left instance" event ; if you
  need this, fire `endOfRunHandler.onVoluntaryExit(run)` from your own
  hook.

---

## 9. Testing

```
mvn -pl module-memory-labyrinth test
```

Pure-logic test coverage (no Bukkit runtime required) :

- `DifficultyModifierTest` — tier scaling formulas.
- `LabyrinthSaveTest` — `partyHash` determinism + checksum tamper
  detection.
- `IconRollerTest` — icon resolution rules + RNG determinism.
- `LootCalculatorTest` — gold formula, weighted item rolls, `minTier`
  gate, per-player seed determinism.

Bukkit-integration tests (room lifecycle, door controller, mob spawner,
run manager) are not in this suite — they require a MockBukkit harness
and are best validated in-game.

---

## 10. Troubleshooting

| Symptom | Likely cause |
|---|---|
| `[MemoryLabyrinth] Dungeon X has no labyrinthDungeonConfig` at run start | The dungeon was saved as `CLASSIC` or the `🌀 Labyrinth` tab was never filled. Re-edit and save. |
| `Instance X has no LOBBY room — run cannot start` | No room of type `LOBBY` in the pool, or none carries the floor's `tagFilter` tag. |
| Players walk through a door but nothing happens | The door anchor coordinates don't match the actual door position in-world. Open the room editor and align them (≤ 1.5 blocks of the player's location triggers traversal). |
| Boss kill doesn't bump the tier | The boss room is tagged with the wrong `RoomType`. It must be `BOSS` for `currentRoomIndex % 10 == 0` to trigger the boss handler. |
| Resume prompt never appears in Infinite | No save exists yet (first run) or the party composition has changed since the last save. |
| Rooms repeat heavily | Pool too thin — the picker logs a warning when fewer than 8 combat rooms are eligible for the current floor's tag filter. |
