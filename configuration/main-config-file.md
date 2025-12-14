---
description: Here is the main config file to set up on your spigot/paper server.
icon: gears
---

# Main Config File

The main configuration file (`config.yml`) controls all aspects of the NextDungeon plugin. This file is located in `plugins/NextDungeon/config.yml` after the plugin has been loaded for the first time.

## Configuration Overview

The configuration file includes the following sections:
* **Redis Configuration** - Cross-server communication
* **Web Editor Configuration** - Visual editor settings
* **Database Configuration** - Data storage settings
* **Instance Provider** - Dungeon instance management (CloudNet/ASP/Vanilla)
* **Revive System** - Player death and revival mechanics

## Full Configuration Example

```yaml
# =====================================================
# NextDungeon Configuration File
# =====================================================
# Welcome to the NextDungeon configuration!
# This configuration controls all aspects of the plugin.
# =====================================================

# ─────────────────────────────────────────────────────
# 🔴 REDIS CONFIGURATION
# ─────────────────────────────────────────────────────
# Redis is used for cross-server communication
# and real-time data storage.
RedisConfiguration:
    host: "127.0.0.1"
    port: 6379
    username: "default"
    password: ""
    topic: "dungeons:packets" # ⚠️ Don't modify unless you know what you're doing

# ─────────────────────────────────────────────────────
# 🌐 WEB EDITOR CONFIGURATION
# ─────────────────────────────────────────────────────
# Configuration for the web proxy server for the editor
webeditor:
    proxy-port: 7734 # Web proxy server port (Velocity/BungeeCord)

# ─────────────────────────────────────────────────────
# 💾 DATABASE CONFIGURATION
# ─────────────────────────────────────────────────────
# Available types: mongodb, mysql
# Choose the database type that matches
# your infrastructure.
DatabaseConfiguration:
    type: "mysql"

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

# ─────────────────────────────────────────────────────
# ☁️  INSTANCE PROVIDER CONFIGURATION
# ─────────────────────────────────────────────────────
# Manages the instance creation and management system.
#
# Available types:
#   • CLOUDNET   - Uses CloudNet (requires CloudNet installed)
#   • ASP        - Advanced Slime World Manager (lightweight & performant)
#   • VANILLA    - Native Minecraft system (always available)
#
InstanceProvider:
    type: "CLOUDNET"

    # ASP Configuration (Advanced Slime World Manager)
    ASP:
        loaderType: "FILE" # FILE, MYSQL, or MONGODB

        mysql:
            url: "jdbc:mysql://localhost:3306/asm"
            host: "localhost"
            port: 3306
            database: "asm"
            useSSL: false
            username: "root"
            password: "root"

        mongodb:
            database: "asm"
            collection: "worlds"
            username: ""
            password: ""
            authSource: ""
            host: "localhost"
            port: 27017
            uri: ""

# ─────────────────────────────────────────────────────
# ⚰️  REVIVE SYSTEM
# ─────────────────────────────────────────────────────
# Manages the ghost and resurrection system when
# a player dies. Teammates can resurrect them
# using a special item before the timer runs out.
#
ReviveSystem:

    # 📦 REVIVE ITEM
    # The item used to resurrect a deceased player
    ReviveItem:
        type: "BEETROOT_SOUP"           # Minecraft item type
        displayName: "&c&lRevive Item"  # Display name (supports color codes)
        lore:                           # Item description
            - "&7Use this to revive a fallen teammate"

    # ⏱️  GHOST MODE DURATION
    # Time in seconds before the player respawns
    # (and consumes a life, or gets banned if they have none left)
    ghostDuration: 15

    # 📢 REVIVE MESSAGE
    # Message shown to all players during a resurrection
    # {player} will be replaced by the player's name
    reviveMessage: "&a{player} has been revived!"

    # 🚫 PERMANENT BAN
    # Used when a player has no remaining lives
    banCommand: "litebans:ban {player} {time} {reason}"
    banReason: "You have died permanently in the dungeon."

# =====================================================
# 💡 CONFIGURATION TIPS
# =====================================================
# • Color codes: use Minecraft codes
#   &0=black &1=blue &2=green &3=cyan &4=red &5=magenta
#   &6=yellow &7=gray &8=dark gray &9=light blue &a=light green
#   &b=light cyan &c=light red &d=light magenta &e=light yellow &f=white
#   &l=bold &o=italic &n=underline &m=strikethrough
#   Or use hex codes: &#RRGGBB
#
# • Test your configuration: restart the server and check the logs
# • Need help? Check the documentation: https://cupcode-1.gitbook.io/nextdungeon/
#
# =====================================================

```

## Configuration Sections Explained

### Redis Configuration

Redis is required for cross-server communication, especially when using CloudNet for instance management.

```yaml
RedisConfiguration:
    host: "127.0.0.1"      # Redis server address
    port: 6379             # Redis server port (default: 6379)
    username: "default"    # Redis username (if authentication enabled)
    password: ""           # Redis password (leave empty if no auth)
    topic: "dungeons:packets"  # Redis pub/sub topic for communication
```

**Important Notes:**
* Redis must be running and accessible for the plugin to work with CloudNet
* The `topic` should remain unchanged unless you have multiple separate NextDungeon networks
* If you change the topic, ensure all servers in your network use the same topic

### Web Editor Configuration

Settings for the Blockly-based visual web editor.

```yaml
webeditor:
    proxy-port: 7734  # Port for the web editor proxy server
```

**Important Notes:**
* This port must be accessible from your web browser
* Ensure firewall allows connections on this port
* Default port is 7734, change if conflicts occur

### Database Configuration

NextDungeon supports MySQL and MongoDB for storing dungeon data, player progress, and statistics.

#### MySQL Configuration (Recommended)

```yaml
DatabaseConfiguration:
    type: "mysql"
    
    mysql:
        host: "localhost"    # Database server address
        port: 3306          # Database server port (default: 3306)
        database: "dungeons" # Database name
        username: "root"     # Database username
        password: "root"     # Database password
```

#### MongoDB Configuration

```yaml
DatabaseConfiguration:
    type: "mongodb"
    
    mongodb:
        host: "localhost"    # MongoDB server address
        port: 27017         # MongoDB server port (default: 27017)
        database: "dungeons" # Database name
```

**Important Notes:**
* Create the database before starting the plugin
* Ensure the database user has proper permissions
* Plugin will create necessary tables/collections automatically

### Instance Provider Configuration

Controls how dungeon instances are created and managed.

#### CloudNet Provider (Recommended for Networks)

```yaml
InstanceProvider:
    type: "CLOUDNET"
```

**Requirements:**
* CloudNet 4.0.0-RC13+ installed
* Redis configured and running
* CloudNet task configured for dungeon instances

See [CloudNet Integration](../integrations/cloudnet.md) for detailed setup.

#### ASP Provider (Optimized Performance)

```yaml
InstanceProvider:
    type: "ASP"
    
    ASP:
        loaderType: "FILE"  # Options: FILE, MYSQL, MONGODB
```

**ASP with File Storage:**
```yaml
ASP:
    loaderType: "FILE"
```

**ASP with MySQL Storage:**
```yaml
ASP:
    loaderType: "MYSQL"
    
    mysql:
        url: "jdbc:mysql://localhost:3306/asm"
        host: "localhost"
        port: 3306
        database: "asm"
        useSSL: false
        username: "root"
        password: "root"
```

**ASP with MongoDB Storage:**
```yaml
ASP:
    loaderType: "MONGODB"
    
    mongodb:
        database: "asm"
        collection: "worlds"
        username: ""
        password: ""
        authSource: "admin"
        host: "localhost"
        port: 27017
        uri: ""  # Or use full connection URI
```

See [ASP Integration](../integrations/asp.md) for detailed setup.

#### Vanilla Provider (Basic Setup)

```yaml
InstanceProvider:
    type: "VANILLA"
```

**Important Notes:**
* Uses standard Minecraft world loading
* Not recommended for production due to performance
* No additional configuration needed
* Best for testing or small servers

### Revive System Configuration

Configure the death and revival mechanics for dungeons.

```yaml
ReviveSystem:
    ReviveItem:
        type: "BEETROOT_SOUP"           # Item used for reviving
        displayName: "&c&lRevive Item"  # Item display name
        lore:
            - "&7Use this to revive a fallen teammate"
    
    ghostDuration: 15  # Seconds before auto-respawn
    
    reviveMessage: "&a{player} has been revived!"
    
    banCommand: "litebans:ban {player} {time} {reason}"
    banReason: "You have died permanently in the dungeon."
```

**Configuration Options:**

* **ReviveItem.type**: Any valid Minecraft item type (e.g., `GOLDEN_APPLE`, `TOTEM_OF_UNDYING`)
* **ReviveItem.displayName**: Custom name with color codes (`&` or `&#RRGGBB`)
* **ReviveItem.lore**: Array of description lines
* **ghostDuration**: Time in seconds teammates have to revive (recommend: 10-30)
* **reviveMessage**: Message broadcast on successful revival (use `{player}` placeholder)
* **banCommand**: Command executed when player dies permanently (customize for your ban plugin)
* **banReason**: Reason shown to player when banned

**Important Notes:**
* Players in ghost mode are invisible and cannot interact
* Teammates must have the revive item in inventory to revive
* Ban command can be customized for different ban plugins (Vanilla, LiteBans, AdvancedBan, etc.)
* Set `ghostDuration: 0` to disable the revive system

## Color Codes Reference

NextDungeon supports standard Minecraft color codes and hex colors:

### Standard Colors
* `&0` - Black
* `&1` - Dark Blue
* `&2` - Dark Green
* `&3` - Dark Aqua
* `&4` - Dark Red
* `&5` - Dark Purple
* `&6` - Gold
* `&7` - Gray
* `&8` - Dark Gray
* `&9` - Blue
* `&a` - Green
* `&b` - Aqua
* `&c` - Red
* `&d` - Light Purple
* `&e` - Yellow
* `&f` - White

### Formatting Codes
* `&l` - Bold
* `&m` - Strikethrough
* `&n` - Underline
* `&o` - Italic
* `&r` - Reset

### Hex Colors (1.16+)
* `&#FF0000` - Custom red
* `&#00FF00` - Custom green
* `&#0000FF` - Custom blue

## Best Practices

### General
* **Backup before editing** - Always backup `config.yml` before making changes
* **Test changes** - Restart server after editing to verify configuration
* **Use comments** - Add notes to remember why settings were changed
* **Version control** - Keep old configs when updating

### Security
* **Secure Redis** - Use password authentication for Redis in production
* **Database security** - Use strong passwords for database access
* **Limit access** - Don't expose database/Redis ports publicly
* **Regular backups** - Backup database regularly

### Performance
* **Choose appropriate provider** - CloudNet for networks, ASP for optimization, Vanilla for testing
* **Monitor resources** - Watch RAM and CPU usage
* **Optimize database** - Regular maintenance and indexing
* **Use Redis wisely** - Don't overload with excessive messages

## Troubleshooting

### Plugin Won't Load

Check console for errors related to:
* Database connection issues
* Redis connection failures
* Invalid YAML syntax
* Missing dependencies

### Database Connection Failed

1. Verify database server is running
2. Check credentials (host, port, username, password)
3. Ensure database exists
4. Test connection: `mysql -u username -p -h host database`

### Redis Connection Failed

1. Verify Redis server is running: `redis-cli ping`
2. Check host and port settings
3. Test authentication if password is set
4. Ensure firewall allows connection

### Configuration Changes Not Applied

1. Ensure proper YAML syntax (spacing, indentation)
2. Restart server (not just reload)
3. Check console for configuration errors
4. Verify file saved properly

## Additional Resources

* [Installation Guide](../getting-started/installation.md)
* [Quick Start Guide](../getting-started/quick-start-guide.md)
* [CloudNet Integration](../integrations/cloudnet.md)
* [ASP Integration](../integrations/asp.md)

***

Properly configuring NextDungeon ensures optimal performance and functionality!


