# Memory Labyrinth — Room tags & assigning rooms to floors

Focused guide: how the **tags** in a room's config work, and how to
**assign rooms to a floor** (= a difficulty).

---

## 1. The model: a shared pool + a per-floor filter

Unlike a classic dungeon, rooms are **not** defined per floor. They live in
**a single shared pool at the dungeon level**:

```
Dungeon (LABYRINTH)
└─ LabyrinthDungeonConfig
   ├─ worldId
   ├─ dungeonSpawn
   └─ rooms[]            ← THE shared pool (every room)
        ├─ room { id, type, region, tags[], ... }
        ├─ room { ... }
        └─ ...

Floor "easy"     → LabyrinthFloorConfig { tagFilter: "easy",     maxRooms: 30, ... }
Floor "hard"     → LabyrinthFloorConfig { tagFilter: "hard",     maxRooms: 70, ... }
Floor "infinite" → LabyrinthFloorConfig { tagFilter: "infinite", maxRooms: 0,  ... }
```

Each floor has exactly **one** `tagFilter` (a string). At runtime the picker
starts from the dungeon's full pool and **keeps only the rooms whose `tags`
list contains that `tagFilter`**. That is what "assigning a room to a floor"
means: put on the room the tag that floor expects.

Code references:
- Pool: `LabyrinthDungeonConfig.rooms` (`common`)
- Room tags: `LabyrinthRoom.tags` (`List<String>`)
- Floor filter: `LabyrinthFloorConfig.tagFilter` (`String`)
- Filtering: `RoomPicker.filterByTag(...)` (`module-memory-labyrinth`)

---

## 2. A room's tags

In **Dungeon Editor → 🌀 Labyrinth tab → Pool de rooms → (a room)**, the
**Tags** field is a **comma-separated** list, e.g.:

```
easy, normal
```

Key properties:

- **Multiple tags per room**: the same room can serve several floors. A combat
  room tagged `easy, normal, hard` shows up in all three difficulties.
- **Exact, case-sensitive match**: filtering does
  `room.tags.contains(tagFilter)`. `Easy` ≠ `easy`, `easy ` (trailing space) ≠
  `easy`. Keep the casing consistent between the room tags and the floor's
  `tagFilter`.
- **Filtering is per room type**: LOBBY, COMBAT and BOSS are filtered
  independently. A floor needs, *for its tag*, at least:
  - 1 **LOBBY** room,
  - enough **COMBAT** rooms (8+ recommended for variety),
  - 1 **BOSS** room.

---

## 3. Assigning rooms to a floor — step by step

1. **Tag the rooms** in the dungeon pool. Recommended convention: the tag =
   the **floor id**.
   - `lobby_main`   → tags `easy, normal, hard, infinite`
   - `combat_skel`  → tags `easy, normal`
   - `combat_golem` → tags `hard, infinite`
   - `boss_lich`    → tags `hard, infinite`
2. **Create/edit the floor** (🏗️ Floors tab), type `LABYRINTH`.
3. In the floor's **🌀 Labyrinth** sub-tab, set **Tag filter** to the desired
   tag (e.g. `hard`).
4. Save. When a run starts on that floor, the picker only draws from rooms
   tagged `hard`.

> Tip: tag combat rooms broadly (`easy, normal, hard, …`) so one pool serves
> every difficulty, and reserve boss rooms for the floors they're balanced for.

---

## 4. Fallback behavior (must know)

Filtering is intentionally lenient — which can **hide mistakes**:

- **Empty or null `tagFilter`** → no filter: the **whole pool** is eligible for
  that floor.
- **No room carries the tag** → the picker **falls back to the full pool** (of
  that room type) instead of failing.
  *Consequence:* a **typo** in `tagFilter` (or a misspelled tag on the rooms)
  raises no visible error — the floor will silently use **every** room. If a
  difficulty seems to "mix in" rooms from other tiers, check the tags' casing
  and spaces first.

The startup warnings (`sanityCheck`) are about the dungeon's **global** pool
(0 lobby / 0 boss / <8 combat), **not** about a floor's tagged subset. So a
floor can pass the global sanity check while having, after filtering, no room
for its tag (→ fallback to the whole pool).

---

## 5. Notes / current limitations

- **One tag per floor.** `tagFilter` is a single string, not a list. (The
  registry does expose a multi-tag AND `getByTypeAndTags(...)`, but the picker
  only uses the simple `filterByTag`.)
- **Tags only affect room selection.** Difficulty scaling (HP/DMG per tier)
  comes from `LabyrinthFloorConfig` (`hpScalingPerTier` / `dmgScalingPerTier`),
  not from tags.
- **No per-tag weighting.** All eligible rooms are equally likely (with
  avoidance of the last `RoomPicker.RECENT_AVOID_WINDOW = 3` visited rooms when
  the pool allows it).

---

## 6. Full example

Pool of dungeon `tower`:

| Room id        | Type   | Tags                          |
|----------------|--------|-------------------------------|
| `lobby_main`   | LOBBY  | `easy, normal, hard, infinite`|
| `combat_a`     | COMBAT | `easy, normal`                |
| `combat_b`     | COMBAT | `easy, normal`                |
| `combat_c`     | COMBAT | `normal, hard, infinite`      |
| `combat_d`     | COMBAT | `hard, infinite`              |
| `boss_easy`    | BOSS   | `easy, normal`                |
| `boss_hard`    | BOSS   | `hard, infinite`              |

Floors:

| Floor id   | Tag filter | maxRooms | Effective rooms                           |
|------------|------------|----------|-------------------------------------------|
| `easy`     | `easy`     | 30       | lobby_main, combat_a, combat_b, boss_easy |
| `hard`     | `hard`     | 70       | lobby_main, combat_c, combat_d, boss_hard |
| `infinite` | `infinite` | 0        | lobby_main, combat_c, combat_d, boss_hard |
