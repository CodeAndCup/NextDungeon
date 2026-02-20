---
description: Complete reference for the main NextDungeon configuration file.
icon: gear
---

# Main Config File

The main configuration file is located at `plugins/NextDungeon/config.yml`. It is generated automatically on first start with default values. The sections below document every available option.

---

## DebugMode

Controls verbose logging for troubleshooting.

```yaml
DebugMode:
  activated: false       # Set to true to enable detailed debug logs
  logType: "CONSOLE"     # Where to show debug messages: CONSOLE | IN_GAME | BOTH
```

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `activated` | boolean | `false` | Enables or disables debug mode. **Do not enable in production.** |
| `logType` | string | `CONSOLE` | Output destination for debug messages: `CONSOLE` (server log), `IN_GAME` (admin chat), or `BOTH` |

---

## RedisConfiguration

Connection settings for the Redis server used for cross-server communication and data storage.

```yaml
RedisConfiguration:
  host: "127.0.0.1"
  port: 6379
  username: "default"
  password: ""
  database: 0
  topic: "nextdungeon"
```

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `host` | string | `127.0.0.1` | Redis server hostname or IP |
| `port` | integer | `6379` | Redis server port |
| `username` | string | `default` | Redis ACL username (use `default` for no authentication) |
| `password` | string | `""` | Redis password (leave empty if no password is set) |
| `database` | integer | `0` | Redis logical database index (0-15) |
| `topic` | string | `nextdungeon` | Namespace prefix for all Redis keys and pub/sub channels. Change this if running multiple NextDungeon networks on the same Redis instance. |

> **Warning:** Changing `topic` after initial setup requires migrating all stored Redis keys. Only change this if you know what you are doing.

---

## DungeonLoader

Specifies where dungeon definitions are loaded from at startup.

```yaml
DungeonLoader: "redis"
```

| Value | Description |
|-------|-------------|
| `redis` | Load all dungeons and floors from Redis. Recommended for production. Required after migrating configs with `/dungeon admin migrate-all`. |
| `yaml` | Load dungeons from YAML files in `plugins/NextDungeon/dungeons/`. Useful for initial setup or development. |

---

## WebEditor

Configuration for the Blockly web editor reverse-proxy endpoint (served by the Velocity/BungeeCord proxy module).

```yaml
WebEditor:
  proxy-port: 7734
```

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `proxy-port` | integer | `7734` | Port on which the proxy's web editor HTTP server listens. Make sure this matches the proxy module config. |

---

## NotificationConfiguration

Controls how queue and dungeon event notifications are delivered to players.

```yaml
NotificationConfiguration:
  type: "CHAT"
```

| Value | Description |
|-------|-------------|
| `CHAT` | Messages appear in the chat window |
| `ACTION_BAR` | Messages appear on the action bar (above the hotbar) |
| `TITLE` | Messages appear as screen titles |

---

## DatabaseConfiguration

Persistent storage for player profiles and dungeon trigger data.

```yaml
DatabaseConfiguration:
  type: "mysql"    # mysql | mongodb

  mysql:
    host: "localhost"
    port: 3306
    database: "dungeons"
    username: "root"
    password: "root"

  mongodb:
    host: "localhost"
    port: 27017
    database: "dungeons"
```

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `type` | string | `mysql` | Active database backend: `mysql` or `mongodb` |
| `mysql.host` | string | `localhost` | MySQL hostname |
| `mysql.port` | integer | `3306` | MySQL port |
| `mysql.database` | string | `dungeons` | MySQL database name |
| `mysql.username` | string | `root` | MySQL username |
| `mysql.password` | string | `root` | MySQL password |
| `mongodb.host` | string | `localhost` | MongoDB hostname |
| `mongodb.port` | integer | `27017` | MongoDB port |
| `mongodb.database` | string | `dungeons` | MongoDB database name |

---

## InstanceSettings

Parameters related to dungeon instance lifecycle.

```yaml
InstanceSettings:
  loadingTimeout: 120
```

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `loadingTimeout` | integer | `120` | Seconds to wait for an instance to become ready before timing out. If exceeded, `FloorInstance.cancelInstance()` is called and the player is notified. |

---

## InstanceProvider

Controls which backend is used to spin up dungeon instances.

```yaml
InstanceProvider:
  type: "CLOUDNET"
```

| Value | Description |
|-------|-------------|
| `CLOUDNET` | Uses CloudNet v4 to create isolated server instances per floor run. This is the only fully supported option in `1.0.4-SNAPSHOT`. |

---

## PartyProvider

Selects the party system backend.

```yaml
PartyProvider:
  type: "AUTO"
```

| Value | Description |
|-------|-------------|
| `AUTO` | Automatically selects the first available provider at startup (preferred) |
| `AlessioDPParties` | Forces use of the AlessioDP Parties plugin |
| `Internal` | Forces use of the built-in party system (no external plugin required) |

---

## ReviveSystem

Configures the death/ghost/revive mechanic.

```yaml
ReviveSystem:

  ReviveItem:
    type: "BEETROOT_SOUP"
    displayName: "&c&lRevive Item"
    lore:
      - "&7Use this to revive a fallen teammate"

  ghostDuration: 15

  reviveMessage: "&a{player} has been revived!"

  deathMessage: "&c{player} has died! They have {lives} lives remaining."

  banCommand: "litebans:ban {player} {time} {reason}"
  banReason: "You have died permanently in the dungeon."
```

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `ReviveItem.type` | string | `BEETROOT_SOUP` | Minecraft material name for the revive item |
| `ReviveItem.displayName` | string | `&c&lRevive Item` | Display name (supports `&` and `&#RRGGBB` colour codes) |
| `ReviveItem.lore` | list | see above | Item lore lines |
| `ghostDuration` | integer | `15` | Seconds a dead player remains as a ghost before losing a life |
| `reviveMessage` | string | see above | Broadcast message when a player is revived. `{player}` = revived player name |
| `deathMessage` | string | see above | Broadcast message on death. `{player}` = player name, `{lives}` = lives remaining |
| `banCommand` | string | see above | Console command executed when a player runs out of lives. `{player}`, `{time}`, `{reason}` are replaced at runtime |
| `banReason` | string | see above | Reason passed to `{reason}` in `banCommand` |
