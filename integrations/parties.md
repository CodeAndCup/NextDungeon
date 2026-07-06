---
description: Integrating NextDungeon with the AlessioDP Parties plugin for group dungeon play.
icon: people-group
---

# Parties Integration

NextDungeon supports group-based dungeon entry via a party system. Parties allow multiple players to queue together, enter the same instance, and share dungeon progress.

---

## Supported Party Backends

NextDungeon uses `PartyService` to abstract the party backend:

| Backend | Class | Description |
|---------|-------|-------------|
| **AlessioDP Parties** | `AlessioDPPartyProvider` | Uses the popular [Parties plugin](https://www.spigotmc.org/resources/parties.3709/) by AlessioDP |
| **Internal** | `InternalPartyProvider` | Built-in lightweight party system (no external plugin required) |

---

## Configuration

In `plugins/NextDungeon/config.yml`:

```yaml
PartyProvider:
  type: "AUTO"     # AUTO | AlessioDPParties | Internal
```

| Value | Behaviour |
|-------|-----------|
| `AUTO` | Detects available providers at startup; uses AlessioDP if available, falls back to Internal |
| `AlessioDPParties` | Forces AlessioDP Parties backend; plugin must be installed |
| `Internal` | Forces the internal party system regardless of installed plugins |

---

## AlessioDP Parties Setup

### Prerequisites

1. Install [AlessioDP Parties](https://www.spigotmc.org/resources/parties.3709/) on all servers.
2. Ensure `Parties.jar` is in the `plugins/` folder.
3. NextDungeon lists `Parties` as a `softdepend`, so it loads after the Parties plugin.

The `AlessioDPPartyProvider` calls `Parties.getApi()` at startup to obtain the `PartiesAPI` reference.

<!-- INSERT HERE: screenshot of the party builder menu -->

---

## Internal Party System

When no external party plugin is available (or `type: "Internal"` is set), NextDungeon uses its own party implementation:

* `InternalParty` — lightweight party data object (leader + members)
* A solo party is created automatically when a player launches a floor without being in a party — subject to the floor's minimum party size (see [Launching a Dungeon](#launching-a-dungeon))
* Party data is **not** persisted to Redis or the database — parties are in-memory only

---

## Party Requirements on Floors

Each floor can specify minimum and maximum party sizes:

```yaml
requirements:
  party:
    min_size: 2    # Minimum players in the party (1 = solo allowed)
    max_size: 10   # Maximum players in the party
```

These are checked in `Floor.isRequirementsValid(player)` (via `Requirements.getParty()`).

---

## DungeonParty

`DungeonPartyImpl` (`fr.perrier.dungeons.spigot.parties.impl.DungeonPartyImpl`) is a wrapper that associates a standard party with dungeon-specific data:

* `dungeonId` — which dungeon the party is queuing for
* `floorId` — which floor they want to enter
* `description` — optional party description for the party finder menu
* `minLevel` — minimum level required to join this party

The `PartyBuilderMenu`, `PartyFinderMenu`, and `PartyFilterMenu` provide in-game GUI screens for creating, browsing, and joining dungeon parties.

<!-- INSERT HERE: screenshot of the party finder menu -->

---

## Launching a Dungeon

Players start a dungeon from the **Dungeon Gate menu** — the GUI that lists a dungeon's floors (opened from the lobby, e.g. via an NPC). Clicking a floor attempts to launch it for the player's party. The following rules apply:

* **Only the party leader can start a dungeon.** If a member who is not the leader clicks a floor, they are told that only their leader may launch it. Leadership is read from the active party backend (AlessioDP or Internal).
* **You can launch directly from any existing party** — there is no need to build a party through the Party Finder first. If you are already grouped (for example, an AlessioDP party formed with `/party create`, or an internal party), the leader clicks a floor and the whole party is taken in. A `DungeonPartyImpl` is created on the fly to wrap the existing party.
* **Solo play** is allowed only when the floor's `min_size` is `1`. A player with no party counts as a party of one; if the floor requires two or more players, the launch is rejected with a "party too small" message.
* **No double launches.** A dungeon cannot be started while a launch is already in progress for your party, or while an instance for your party is being prepared or is already active. Rapid double-clicks and duplicate requests are rejected.

The **Party Finder** and **Party Builder** menus remain available for players who want to advertise a listed party and recruit members before starting — they are now optional rather than required.

---

## Party Queue Flow

1. The party leader launches a floor from the Dungeon Gate menu (or an admin uses `/dungeon admin run <floorId>`). If the leader has no party, a solo `DungeonPartyImpl` is created automatically.
2. `QueueManager.requestInstance(player, floor)` is called.
3. The instance is created for all party members.
4. `FloorInstance.sendToServer(IDungeonParty party)` checks `party.areAllMembersOnline()` before transferring every member.
5. If a party member is offline, the leader receives a warning message.

---

## GlobalPartyListener

The `GlobalPartyListener` (`spigot/src/main/java/fr/perrier/dungeons/spigot/listener/global/GlobalPartyListener.java`) listens for party events from the active provider and updates `DungeonPartyImpl` state accordingly (e.g. removing disbanded parties, updating membership).
