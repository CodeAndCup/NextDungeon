---
icon: question
---

# What is NextDungeon

**NextDungeon** is a powerful, extensible Minecraft server plugin (version 1.0.4-SNAPSHOT) built for Spigot/Paper servers running Minecraft **1.21.4** and Java **21**. It enables server administrators to design, deploy, and run fully customizable dungeon instances with multi-floor support, advanced trigger/action workflow automation, and deep integration with popular third-party plugins.

<!-- INSERT HERE: screenshot of the NextDungeon main menu or dungeon gate UI -->

Dungeons and their floors are centrally synchronized across servers via **Redis**, meaning multiple game servers in a network share the same dungeon data in real time. Each floor runs as an isolated dungeon instance (managed by **CloudNet**), giving players a dedicated, lag-free environment to explore.

### Key Highlights

* **Multi-Floor Dungeon System**: Each dungeon (`Dungeon`) contains one or more floors (`Floor`). Every floor is independently configured with its own world settings, player requirements, game rules, and progression steps.
* **Trigger-Action Workflow Engine**: An event-driven scripting system lets you attach behaviours to dungeons without writing code. Triggers (block click, region entry, entity death, etc.) fire sequences of actions (send message, teleport, summon mob, end dungeon, etc.) conditioned on optional logic blocks.
* **Blockly Web Editor**: A browser-based drag-and-drop editor backed by Google Blockly lets you create and modify dungeon workflows visually while in edit-mode on a live server.
* **Redis-Backed Synchronization**: All dungeon definitions, floor configurations, active instances, player queues, and profiles are stored in and broadcast through Redis, keeping the entire network consistent.
* **CloudNet Instance Management**: Dungeon floors spin up as dedicated CloudNet service instances. The plugin handles creation, readiness detection, player routing, and cleanup automatically.
* **Queue System**: Players (or parties) queue for a floor from a lobby server. The `QueueManager` monitors available instances and sends groups to a ready server when capacity opens up.
* **Revive & Ghost System**: When a player dies they become a ghost for a configurable duration. Teammates can revive them using a special item. Running out of lives can trigger a configurable ban command.
* **Party Integration**: Works with AlessioDP Parties or the built-in internal party system. Auto-detection mode picks the best available provider at startup.
* **MMOCore Level Requirements**: Floor entry can require a minimum MMOCore level and completion of prerequisite floors.
* **Persistent Player Profiles**: Per-player stats (completed floors, kills, deaths, completion time) are tracked and stored via Redis, then persisted to MySQL or MongoDB.

### Project Modules

| Module | Description |
|--------|-------------|
| `common` | Shared data models (`FloorData`, `FloorInstanceData`, `QueueEntry`, etc.) and the Pidgin messaging library |
| `spigot` | Main game-server plugin containing all dungeon logic, workflow engine, commands, and listeners |
| `velocity` | Velocity proxy plugin providing the web-editor reverse-proxy and dashboard integration |
| `bungeecord` | BungeeCord proxy plugin (equivalent of Velocity module) |
| `webserver` | Static HTML assets for the Blockly dungeon editor frontend |

### Supported Minecraft Platforms

NextDungeon targets **Spigot 1.21.4** and compatible forks (Paper, Purpur). The proxy module supports both **Velocity** and **BungeeCord**.

***

Continue to the [Feature Overview](introduction/feature-overview.md) to see what you can do with NextDungeon!
