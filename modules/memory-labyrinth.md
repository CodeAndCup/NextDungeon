---
description: A roguelike dungeon mode — random rooms, door choices, bosses, loot, and endless runs.
icon: dungeon
---

# Memory Labyrinth Module

The **Memory Labyrinth** turns a dungeon into a **roguelike run** (think Hades or Archero). Instead of a fixed, hand-built map, players push through a **generated sequence of rooms**, choosing a door at every step, fighting their way deeper, and banking loot at the end.

**Module ID:** `memory_labyrinth`

<!-- INSERT HERE: video of a labyrinth run — clearing a room and choosing a door -->

---

## How a Run Plays

1. The party spawns in a **lobby room** (safe, no mobs).
2. Two **doors** appear, each with a floating **reward icon** hinting at what's behind it.
3. A player **walks through a door** — the whole party is taken into the next room.
4. The party **clears the room** by killing every mob in it. A message shows the clear time, and two new doors open.
5. **Every 10th room is a boss room** (a single door). Beating the boss raises the difficulty.
6. The run continues until the party **wins**, **leaves** (endless mode), or **wipes**.

Runs go strictly **room → door → next room** — there's no backtracking, and each door commits you to that room's fight. An **action bar** always shows your current **Room** and **Tier** so you know how deep and how hard you are.

---

## Rooms, Doors & Tiers

**Room types**

| Type | Role |
|------|------|
| **Lobby** | The safe starting room (room 0), no mobs |
| **Combat** | The standard fight rooms — most of a run |
| **Boss** | Appears every 10th room; tougher, single exit door |

**Door icons.** A small floating icon above each door previews the next room's reward bias:

* **Gold icon** — a room that boosts your final gold payout.
* **Wither skeleton skull** — always shown on a **boss door**.
* **No icon** — a neutral room.

Players weigh the two previews and pick the door they want.

**Tiers (difficulty scaling).** Difficulty climbs in **tiers**. Tier starts at 1 and goes **up by one every time a boss is killed**. Higher tiers make mobs stronger — roughly **+30% health and +15% damage per tier** — and unlock better loot. Tiers control *how hard* the mobs are; tags (below) control *which* rooms appear. They're separate systems.

---

## Difficulties & Room Tags

A labyrinth dungeon can offer several **difficulties** (its "floors" — for example `easy`, `hard`, and an endless `infinite`). The clever part: **rooms aren't built per difficulty.** You build **one shared pool of rooms** for the whole dungeon, and each difficulty just **filters that pool by a tag**.

* Every room has a comma-separated **Tags** list, e.g. `easy, normal`.
* Every difficulty has one **Tag filter**, e.g. `hard`.
* When a run starts, only rooms whose tags include that filter are used. A room can carry several tags, so one combat room can serve several difficulties.

> **Watch the tags carefully.** Matching is **exact and case-sensitive** (`Easy` ≠ `easy`, and a stray space breaks it). Filtering is deliberately lenient: if a tag is misspelled or nothing matches, the game **silently uses the whole pool** instead of erroring. So if a difficulty seems to "mix in" the wrong rooms, a tag typo is almost always the cause.

A workable difficulty needs, **for its tag**, at least **1 lobby room, several combat rooms (8+ for variety), and 1 boss room**.

**Example**

| Room | Type | Tags |
|------|------|------|
| `lobby_main` | Lobby | `easy, normal, hard, infinite` |
| `combat_a` | Combat | `easy, normal` |
| `combat_c` | Combat | `normal, hard, infinite` |
| `boss_easy` | Boss | `easy, normal` |
| `boss_hard` | Boss | `hard, infinite` |

The `hard` difficulty (Tag filter `hard`) then draws only `lobby_main`, `combat_c`, and `boss_hard`.

---

## Loot & Rewards

Loot is handed out **at the end of the run**, not room by room. As the party plays, it **collects reward icons** (like gold-icon doors); those totals decide the payout.

Each difficulty has its own **loot table** with:

* a **base gold** amount, plus a **bonus per gold icon** collected,
* a number of **item rolls**, and
* a **weighted item list**, where items can require a **minimum tier** to appear.

At the end, **each player rolls independently** (fairly seeded), gold scales with the icons collected **and** the final tier, and items land straight in the player's inventory.

> **Rewards are only granted on a win.** A full party wipe means **no loot**. In endless mode, **leaving on purpose counts as a win** — you bank what you earned.

---

## Bosses

* A **boss room appears every 10 rooms**.
* Bosses and mobs come from each room's own **spawn list** (a mob id, a position, and a count).
* **MythicMobs is supported** — use custom Mythic bosses, or plain vanilla mobs if MythicMobs isn't installed. (Double-check boss mob ids: a room that spawns nothing auto-clears.)
* Every mob is **scaled to the current tier**, so encounters get deadlier as you descend.
* Killing a boss **raises the tier**, offers a one-time revive (below), and — in endless mode — **saves your progress**.

---

## Death & Revive

The labyrinth has its **own revive rules**, different from normal dungeons:

* A player who dies becomes a **ghost** and joins the run's "down" list — they **can't be revived right away**.
* **Revives unlock at boss kills.** After a boss, if anyone is down, the living players get a **clickable chat prompt** listing dead teammates. Clicking a name revives them at the boss room.
* It's **one revive per boss**, and it **expires** once the party moves on — so the incentive is to push to the next boss to earn a rescue.
* **A total wipe ends the run** with no loot (and deletes any endless save).

---

## Endless Mode: Save & Resume

The endless difficulty lets a party **save and continue later**:

* **Progress saves automatically at every boss kill** ("Progress saved — checkpoint at room N").
* A save belongs to the **exact party** that made it — only that same group can resume it. Up to **3 saves per party** are kept.
* When starting an endless run, the party waits in the lobby (frozen) until the **leader** chooses from a **clickable prompt**: resume one of the saved runs (each shows its room, tier, and date) or start a **New run**.
  * **Resuming** drops the party into the room right after the saved boss, at the saved tier — skipping the lobby.
  * **New run** starts fresh without deleting existing saves.
* Saves are integrity-checked; a tampered save is refused.
* Leaving with `/nd memory leave` banks your loot and ends the session **but keeps the save**. A total wipe **deletes** it.

---

## Blessings

If the **SAO-Blessing** plugin is installed, the labyrinth grants character **blessings** (buffs/perks) during a run:

* Entering the lobby of a **fresh run** offers each player a blessing choice.
* **Blessing rooms** grant an extra blessing offer when cleared.
* Level-up style perk screens are shown **at room clear** (never mid-fight).
* Blessings are tied to the same party as the endless save, so **resuming a run also resumes its blessings**.

---

## Player Commands

Most interactions are **clickable chat buttons** (revive, resume/new) — players never type those. The one command players use directly:

| Command | Description |
|---------|-------------|
| `/nd memory leave` | (Endless mode) The party leader banks the current loot and leaves — counts as a win |

---

## Automation Blocks

When the module is loaded, the editor gains a **Memory Labyrinth** category with blocks you can use to script cinematics, sounds, titles, or reward-granting around a run. These are **react-only** — they're triggers and conditions, not actions.

**Triggers**

| Block | Fires when |
|-------|-----------|
| 🏛 **When room entered** | A player enters a room |
| ✅ **When room cleared** | Every mob in the current room is dead |
| 🚪 **When doors proposed** | The next door choice is offered |
| 👹 **When boss killed** | A boss room is cleared (every 10th room) |
| 🏁 **When run ended** | The run finishes (carries success/loot info) |
| 💾 **When checkpoint saved** | An endless run saves progress at a boss |
| 🗑 **When save invalidated** | An endless save is deleted (wipe or voluntary exit) |

**Conditions**

| Block | True when |
|-------|-----------|
| ❓ **Is boss room?** | The current room is a boss room |
| ♾ **Is infinite floor?** | The run is on the endless difficulty |
| 💾 **Has resumable save?** | The current party has a save to resume |

---

## Setting Up a Labyrinth (Admins)

Everything is configured in the **Dungeon Editor dashboard** under the **Labyrinth** tab — there are no separate config files.

1. **Create the dungeon and its floors as type `LABYRINTH`.** When a labyrinth floor's instance is ready, the module automatically takes over and starts a run.
2. **Build your rooms** in the dungeon world, then add them to the shared **room pool**. For each room, define:
   * its **type** (Lobby / Combat / Boss),
   * its **region** (the box copied into the run) and **player spawn**,
   * its **exit doors** (and optionally a separate icon position),
   * its **mob spawns** (mob id, position, count),
   * its **tags**, and — for boss rooms — a **fixed reward icon**.
3. **Configure each difficulty (floor):**
   * **Max rooms** (`0` = endless),
   * **Save enabled** (usually only for the endless difficulty),
   * the **Tag filter**,
   * optional **HP/damage scaling per tier**,
   * and a **loot table** (base gold, gold-per-icon, item rolls, weighted item list with per-item minimum tiers).

> **Tip:** tag combat rooms broadly (`easy, normal, hard, …`) so one pool serves every difficulty, and reserve boss rooms for the difficulties they're balanced for. On startup the server log warns if the whole pool has 0 lobby, 0 boss, or fewer than 8 combat rooms — but remember that check looks at the *whole* pool, not each tag's filtered subset.

> **Language note:** all player-facing labyrinth text is in English, like the rest of the plugin. The only French touch is the cosmetic **"Memory Laby"** chat prefix.
