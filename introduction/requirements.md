---
description: Before installing NextDungeon, make sure your server meets these requirements.
icon: shield-check
---

# Requirements

## Server Platform

* **Minecraft server:** Spigot, Paper, or a compatible fork — version **1.21.4**
* **Java:** Java **21** or newer

## Required Plugins

These must be installed before NextDungeon will start:

| Plugin | Why it's needed |
|--------|-----------------|
| **Redis** | Keeps dungeons, instances, queues, and profiles in sync across your network |
| **MMOCore** | Player level checks for floor requirements |
| **PacketEvents** | Powers the corpse/ghost display used by the revive system |

> If MMOCore or PacketEvents is missing, the plugin will not load.

## Optional Plugins

| Plugin | Adds |
|--------|------|
| **CloudNet** (4.0.0-RC13+) | Runs each floor as its own dedicated server (see [CloudNet](../integrations/cloudnet.md)) |
| **AlessioDP Parties** | Party play (otherwise the built-in party system is used) |
| **MythicMobs** | Custom mobs and bosses inside dungeons |
| **WorldEdit / FAWE** | The WorldEdit region actions (via the WorldEdit module) |

> Without CloudNet, the plugin still runs on a single server but cannot create isolated floor instances.

## Database

You need **one** of these for saved player stats and trigger storage:

* **MySQL** (recommended)
* **MongoDB**

Configure your choice under `DatabaseConfiguration` in [`config.yml`](../configuration/main-config-file.md).

> Full MongoDB support for triggers is planned; MySQL is the recommended choice for now.

## Proxy (for the Editor & Dashboard)

To use the visual editor and the web dashboard, install the matching proxy plugin:

* **Velocity** — `NextDungeon-Velocity.jar`
* **BungeeCord** — `NextDungeon-BungeeCord.jar`

The proxy needs access to the same Redis as your game servers.

## Web Editor Access

The editor is served on the port set by `WebEditor.proxy-port` (default `7734`). Make sure that port is reachable from your admins' browsers and open in your firewall.

## Hardware

* **RAM:** ~4 GB for the network, plus ~512 MB per simultaneous dungeon instance
* **Storage:** SSD recommended for faster world loading
* **CPU:** a modern multi-core processor

## Permissions

Admin commands require server `OP` or the `nextdungeon.admin` permission — see [Commands & Permissions](commands-and-permissions.md) for the full list.

***

Ready to install? Continue to the [Installation Guide](../getting-started/installation.md).
