---
description: Group up for dungeons with the built-in party system or the AlessioDP Parties plugin.
icon: people-group
---

# Parties

Dungeons are group content. Players form a party, enter the same instance together, and share progress. NextDungeon supports two party systems, and picks one automatically.

---

## Choosing a Party System

| System | When it's used |
|--------|----------------|
| **AlessioDP Parties** | Used automatically if the [Parties plugin](https://www.spigotmc.org/resources/parties.3709/) is installed |
| **Built-in** | Used when no party plugin is present — no extra install required |

Set which one to use in `config.yml`:

```yaml
PartyProvider:
  type: "AUTO"     # AUTO | AlessioDPParties | Internal
```

| Value | Behaviour |
|-------|-----------|
| `AUTO` | Use AlessioDP Parties if installed, otherwise the built-in system |
| `AlessioDPParties` | Force AlessioDP Parties (the plugin must be installed) |
| `Internal` | Force the built-in system, even if a party plugin is installed |

If you use AlessioDP Parties, install `Parties.jar` on every server. It loads before NextDungeon automatically.

---

## Party Commands (built-in system)

When you're using the built-in party system, players manage parties with these commands (no permission required):

| Command | Description |
|---------|-------------|
| `/dungeon party create [name]` | Create a party — you become the leader |
| `/dungeon party invite <player>` | Invite a player |
| `/dungeon party accept` / `deny` | Respond to an invite |
| `/dungeon party leave` | Leave your party |
| `/dungeon party promote <player>` | Hand leadership to another member |
| `/dungeon party kick <player>` | Remove a member (leader only) |
| `/dungeon party disband` | Disband the party (leader only) |
| `/dungeon party info` | Show your party's members |

If you use **AlessioDP Parties** instead, form parties with that plugin's own commands (e.g. `/party create`, `/party invite`).

---

## Floor Party Size

Each floor sets a minimum and maximum party size in its Requirements:

* **Minimum size** — how many players are needed. Set it to `1` to allow solo play.
* **Maximum size** — the largest party that can enter.

A party that's too small or too large for a floor is turned away with a message.

---

## Launching a Dungeon

Players start a dungeon from the in-game **Dungeon Gate menu** (opened from the lobby, e.g. via an NPC). Clicking a floor tries to launch it for the player's party. The rules:

* **Only the party leader can start a dungeon.** If a non-leader clicks a floor, they're told only the leader can launch it.
* **You can launch directly from any existing party** — there's no need to build a party through the Party Finder first. If you're already grouped (through the built-in system or AlessioDP Parties), the leader just clicks a floor and the whole party is taken in.
* **Solo play** works only when the floor's minimum size is `1`. A player with no party counts as a party of one.
* **No double launches.** A dungeon can't be started while one is already launching for your party, or while an instance for your party is being prepared or is already active.

The **Party Finder** and **Party Builder** menus are still available for players who want to advertise a party and recruit strangers before starting — they're optional, not required.

---

## What Happens on Launch

1. The leader launches a floor (or an admin uses `/dungeon admin run <floorId>`).
2. The floor's requirements are checked for every party member.
3. An instance is created and every online member is sent to it together.
4. If a member is offline, the leader is warned.

When the run ends, the party stays together so everyone returns to the lobby as a group.
