---
description: >-
  Before installing NextDungeon, ensure your server meets all requirements.
icon: shield-check
---

# Requirements

## Server Platform

* **Minecraft Server**: Spigot, Paper, or compatible fork — version **1.21.4** (API-version set to `1.21` in `plugin.yml`)
* **Java Version**: Java **21** or newer (Maven compiler source and target set to `21`)

## Required Dependencies

The following must be present before NextDungeon can start:

| Dependency | Purpose | Notes |
|-----------|---------|-------|
| **Redis** | Cross-server data sync, dungeon state, queue management, player profiles | Accessed via Redisson (single-server mode) |
| **MMOCore** | Player level checks for floor requirements | Listed as a hard `depend` in `plugin.yml` |
| **PacketEvents** | NPC library (NPC-Lib / ghost system) | Listed as a hard `depend` in `plugin.yml` |

> **Important:** If MMOCore or packetevents are absent the plugin will refuse to load.

## Optional / Integration Dependencies

| Plugin | Feature | Declaration |
|--------|---------|-------------|
| **CloudNet v4.0.0-RC13+** | Dynamic dungeon instance creation and routing | `softdepend` |
| **Parties (AlessioDP)** | Group play, party requirements | `softdepend` |
| **MythicMobs** | Custom mobs and bosses inside dungeons | `softdepend` |
| **WorldEdit / FAWE** | `WorldEdit*Action` workflow actions | Used at runtime if present |

> CloudNet is the only supported `InstanceProvider` type in `1.0.4-SNAPSHOT`. Without it the plugin can still run on a single server but cannot spin up isolated floor instances.

## Database

NextDungeon requires **one** of the following for persistent player stats and trigger storage:

* **MySQL** (default) — configured under `DatabaseConfiguration.mysql` in `config.yml`
* **MongoDB** — configured under `DatabaseConfiguration.mongodb` in `config.yml`

> MongoDB support for triggers is planned for future releases. Currently MySQL is the recommended choice.

## Proxy (optional)

To use the **Blockly web editor** and the **dashboard**, install the appropriate proxy module:

* **Velocity** — place `NextDungeon-Velocity.jar` in Velocity's `plugins/` folder
* **BungeeCord** — place `NextDungeon-BungeeCord.jar` in BungeeCord's `plugins/` folder

Both proxy modules require access to the same Redis instance as the Spigot servers.

## Web Editor Access

The web editor HTTP server binds to the port defined by `WebEditor.proxy-port` (default `7734`). Make sure:

* The port is accessible from your admin's browser
* Your firewall allows TCP connections on that port

## Hardware Recommendations

* **RAM**: Minimum 4 GB for the network; add ~512 MB per simultaneous dungeon instance
* **Storage**: SSD recommended for faster world template loading via CloudNet
* **CPU**: Modern multi-core processor; instance creation involves asynchronous tasks

## Permissions

* Server `OP` or the `nextdungeons.admin` permission node is required for admin commands
* See [Commands and Permissions](commands-and-permissions.md) for the full permission list

***

Ready to install? Continue to the [Installation Guide](../getting-started/installation.md).
