# NextDungeon — Memory Labyrinth Module

Procedural dungeon module inspired by Hades and Archero. Provides a
labyrinth-style floor with branching room choices, visible reward icons
above doors, a boss every 10 rooms, and an infinite mode with
per-boss checkpoints.

See `MEMORY_LAB.md` at the repository root for the full CDC.

---

## Build

```
mvn -pl module-memory-labyrinth -am clean package
```

The shaded JAR ends up in `module-memory-labyrinth/target/`. Drop it in
`plugins/NextDungeon/modules/` and restart the server — the
`ModuleLoader` picks it up at boot.

## Run-time wiring

`MemoryLabyrinthModule.onEnable` instantiates :

- `RoomTemplateRegistry` + `LootTableRegistry` (loaded from MySQL/Mongo)
- `RoomPicker` + `IconRoller` (procedural)
- `LabyrinthRunManager` + `LabyrinthRoomLifecycle` + `MobSpawner`
- `DoorController` + `BossEncounterHandler`
- `LabyrinthSaveManager` (Infinite checkpoints)
- `LootCalculator` + `EndOfRunHandler`
- `LabyrinthTriggerBus` (Blockly hooks)
- `TierIndicatorTask` (action-bar feedback)
- `LabyrinthAdminCommandListener` (in-game admin CRUD)

All Bukkit listeners are registered against the host plugin
(`Bukkit.getPluginManager().getPlugin("NextDungeon")`) so they die with
it on shutdown.

## Database

Three tables / collections are auto-created on first boot (see
`MySQLManager.createTables` and `MongoManager.connect`) :

- `labyrinth_rooms` — room template pool (admin-curated)
- `labyrinth_saves` — Infinite mode checkpoints (auto-managed)
- `labyrinth_loot_tables` — per-floor loot configuration

## Admin commands

In-game commands intercept `/labyrinth admin <subcmd>` via
`PlayerCommandPreprocessEvent`. Same usage from the console as
`labyrinth admin <subcmd>`. Permission for players :
`nextdungeon.admin`.

| Command | Description |
|---|---|
| `list-rooms` | Dump the in-memory room pool |
| `list-loot-tables` | Dump the in-memory loot tables |
| `list-saves` | Query DB for every Infinite save (debug) |
| `import-rooms <directory>` | Upsert every `*.json` in `plugins/NextDungeon/labyrinth/<directory>/` |
| `import-loot <floorId> <file.json>` | Upsert a loot table |
| `reload` | Re-fetch room pool + loot tables from DB |
| `stats` | Active runs / pool size / loot table count |

## Sample data

`src/main/resources/samples/` ships starter JSON files :

- `rooms/lobby_basic.json` — single lobby (NONE icon, no mobs)
- `rooms/combat_t1_skeletons.json`, `combat_t1_zombies.json` — two
  combat rooms tagged `easy`/`normal`/`t1`
- `rooms/boss_t1.json` — wither-skeleton boss with fixed `GOLD` icon
- `loot-tables/easy.json` — flat reward table for the easy floor
- `loot-tables/infinite.json` — tier-gated rewards for infinite

Copy them into `plugins/NextDungeon/labyrinth/` and import :

```
labyrinth admin import-rooms rooms
labyrinth admin import-loot easy easy.json
labyrinth admin import-loot infinite infinite.json
labyrinth admin reload
```

## Blockly hooks (Option C)

The module exposes **no actions** — workflows hook into seven triggers
and three conditions to graft side effects (cinematics, sounds, gold
distribution via Vault/MMOCore, etc.).

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

## Soft dependencies

- **MythicMobs** — autodetected via reflection (`MythicMobsBridge`).
  When present, `RoomTemplate.MobSpawn.mobId` resolves Mythic ids
  first ; otherwise falls back to vanilla `EntityType.valueOf`.
- **Vault / MMOCore for gold** — not auto-distributed (CDC §6.5).
  The `goldEarned` value is exposed on the `on_run_ended` trigger ;
  admins wire it via Blockly to whichever currency plugin they use.

## Testing

```
mvn -pl module-memory-labyrinth test
```

Pure-logic test coverage (no Bukkit env required) :

- `DifficultyModifierTest` — tier scaling formulas
- `LabyrinthSaveTest` — partyHash determinism, checksum tamper-detection
- `IconRollerTest` — icon resolution rules + RNG determinism
- `LootCalculatorTest` — gold formula, item weight + minTier gate, per-player seed determinism

Bukkit-dependent classes (`RoomPicker`, `MobSpawner`, lifecycle, etc.)
are wired to `Main.getInstance().getLogger()` and currently require an
integration harness (MockBukkit) — left as a follow-up.

## Hors-scope v1

- `BLESSING` icon — enum reserved, not rolled in v1
- REST/panel endpoints (CDC §7) — pending the cinematic-pattern
  precedent in the host plugin
- Mythic/MMOCore item resolution at end-of-run — vanilla `Material`
  only for now
- Pending offline loot — offline players are skipped at distribution ;
  gold flows via Blockly so admins can queue it via their economy
- Voluntary exit detection from inside the labyrinth — relies on
  external `FloorInstance` lifecycle events that the v1 module does
  not subscribe to
