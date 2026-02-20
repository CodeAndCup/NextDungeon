---
description: Follow these steps to install NextDungeon on your Minecraft server network.
icon: screwdriver-wrench
---

# Installation

## Prerequisites

Before you begin, make sure the following are in place:

| Requirement | Version | Notes |
|------------|---------|-------|
| Minecraft server (Spigot/Paper) | 1.21.4 | Any compatible fork works |
| Java | 21+ | Required for the plugin and CloudNet |
| Redis | 6.x or 7.x | Local or remote; accessible from all servers |
| MMOCore | Latest | Hard dependency |
| PacketEvents | Latest | Hard dependency |
| CloudNet | 4.0.0-RC13+ | For instance management |
| MySQL **or** MongoDB | Any modern version | For player profiles and trigger persistence |

Optional but recommended:
* **AlessioDP Parties** — group play
* **MythicMobs** — custom mobs in dungeons
* **WorldEdit / FAWE** — WorldEdit workflow actions

---

## Step 1: Set Up Redis

1. Install a Redis server (locally or on a dedicated host).
2. Note the host, port (default `6379`), username, and password.
3. Make sure all your Minecraft servers and proxy can reach Redis on that port.

## Step 2: Set Up CloudNet

1. Install CloudNet v4.0.0-RC13 or newer on your network machine.
2. Configure CloudNet to manage your Minecraft server nodes.
3. For each dungeon floor you create, you will need a **CloudNet task** named after the floor ID (e.g. `example_floor1`). See [Creating Dungeons](../dungeon-management/creating-dungeons.md) for details.
4. Refer to the [CloudNet integration page](../integrations/cloudnet.md) for full CloudNet-specific setup.

## Step 3: Install Required Plugins

Place the following JARs in the `plugins/` folder of every **game server** (lobby + instance servers):

* `NextDungeon.jar` — the main plugin
* `MMOCore.jar`
* `packetevents.jar`

Optional JARs (place in `plugins/` if you use these integrations):
* `Parties.jar`
* `MythicMobs.jar`

## Step 4: Install the Proxy Module

Choose the module matching your proxy software and place it in that proxy's `plugins/` folder:

* **Velocity** — `NextDungeon-Velocity.jar`
* **BungeeCord** — `NextDungeon-BungeeCord.jar`

## Step 5: First Start — Generate Default Config

Start your Minecraft server once to generate the default configuration file:

```
plugins/NextDungeon/config.yml
```

> **Note:** Dungeon configurations are no longer stored as YAML files. All dungeons and floors are created and managed exclusively through the web dashboard (served by the Velocity/BungeeCord proxy module). See [Creating Dungeons](../dungeon-management/creating-dungeons.md) for details.

Check the console for startup errors. The plugin will disable itself if Redis, MMOCore, or packetevents are unavailable.

## Step 6: Configure the Plugin

Edit `plugins/NextDungeon/config.yml`. At minimum, update the Redis and database sections:

```yaml
RedisConfiguration:
  host: "your-redis-host"
  port: 6379
  username: "default"
  password: "your-password"
  database: 0
  topic: "nextdungeon"

DatabaseConfiguration:
  type: "mysql"
  mysql:
    host: "localhost"
    port: 3306
    database: "dungeons"
    username: "root"
    password: "your-db-password"

InstanceProvider:
  type: "CLOUDNET"
```

See [Main Config File](../configuration/main-config-file.md) for all available options.

## Step 7: Configure the Proxy Module

**Velocity** (`plugins/NextDungeonVelocity/config.toml`):

```toml
[webeditor]
port = 7734

[redis]
host = "your-redis-host"
port = 6379
topic = "nextdungeon"
username = "default"
password = ""
database = 0
```

**BungeeCord** (`plugins/NextDungeonBungee/config.yml`):

```yaml
webeditor:
  port: 7734

redis:
  host: "your-redis-host"
  port: 6379
  topic: "nextdungeon"
  username: "default"
  password: ""
```

## Step 8: Verify Installation

Reload or restart the server. In-game, run:

```
/dungeon list
```

An empty list means the plugin is loaded correctly. Run `/dungeon debug list dungeons` to confirm Redis connectivity.

---

> **Troubleshooting:** If the plugin fails to start, check the console for errors about Redis connectivity, missing dependencies (MMOCore, packetevents), or database connection failures. Most startup failures are caught and logged with a descriptive message.

***

Ready? Continue to the [Quick Start Guide](quick-start-guide.md) to create your first dungeon.
