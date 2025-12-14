---
description: Learn how to integrate NextDungeon with Advanced Slime Paper (ASP) for optimized world management.
icon: layer-group
---

# ASP

Advanced Slime Paper (ASP) is a lightweight, performance-focused world management system developed by InfernalSuite. When integrated with NextDungeon, ASP provides efficient world storage and loading for dungeon instances.

## What is ASP?

ASP (Advanced Slime Paper) is a world management plugin that:
* Stores worlds in a compact slime format
* Enables fast world loading and cloning
* Supports multiple storage backends (File, MySQL, MongoDB)
* Reduces disk I/O and memory usage
* Provides better performance than vanilla world handling

### Benefits for NextDungeon

* **Faster Instance Creation**: Worlds load significantly quicker than vanilla
* **Reduced Storage**: Slime format is more compact than vanilla world files
* **Multiple Storage Options**: Choose file, MySQL, or MongoDB storage
* **Better Performance**: Optimized for repeated world loading/unloading
* **Lower Memory Usage**: Efficient world data management

## Requirements

### Plugin Dependencies
* **Advanced Slime Paper** - Core plugin from InfernalSuite
* **Paper** or **Purpur** server (Spigot not fully supported)

### NextDungeon Configuration
* Instance provider set to `ASP`
* ASP loader type configured (FILE, MYSQL, or MONGODB)

## Installation

### Step 1: Install Advanced Slime Paper

1. **Download ASP** from [InfernalSuite repository](https://github.com/InfernalSuite/AdvancedSlimePaper)
2. **Place the JAR** in your `plugins` folder
3. **Restart your server** to generate default configuration

### Step 2: Configure ASP Storage Backend

Choose your preferred storage method:

#### Option A: File Storage (Recommended for Small Servers)

Simplest setup - stores worlds as files on disk.

No additional configuration needed. Worlds are stored in:
```
plugins/AdvancedSlimePaper/worlds/
```

#### Option B: MySQL Storage (Recommended for Networks)

Best for multi-server setups or when using CloudNet.

**1. Create Database:**
```sql
CREATE DATABASE asm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'asm'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON asm.* TO 'asm'@'localhost';
FLUSH PRIVILEGES;
```

**2. Configure in NextDungeon:**
```yaml
InstanceProvider:
  type: "ASP"
  
  ASP:
    loaderType: "MYSQL"
    
    mysql:
      url: "jdbc:mysql://localhost:3306/asm"
      host: "localhost"
      port: 3306
      database: "asm"
      useSSL: false
      username: "asm"
      password: "your_password"
```

#### Option C: MongoDB Storage

For MongoDB-based infrastructures.

**1. Create Database:**
```javascript
// In MongoDB shell
use asm
db.createCollection("worlds")
```

**2. Configure in NextDungeon:**
```yaml
InstanceProvider:
  type: "ASP"
  
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
      uri: ""  # Or use connection URI instead of individual settings
```

### Step 3: Configure NextDungeon

Edit `plugins/NextDungeon/config.yml`:

```yaml
# ─────────────────────────────────────────────────────
# ☁️  CONFIGURATION DU PROVIDER D'INSTANCES
# ─────────────────────────────────────────────────────
InstanceProvider:
  type: "ASP"

  # Configuration ASP
  ASP:
    loaderType: "FILE"  # Options: FILE, MYSQL, MONGODB

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
```

### Step 4: Import Dungeon Worlds

Convert your existing dungeon worlds to slime format:

#### Using ASP Commands

```
# Import a world from the server's world folder
/aswm import <world-name> <data-source>

# Examples:
/aswm import dungeon_floor1 file
/aswm import dungeon_floor2 mysql
```

#### Manual Import

1. Place your world folder in the server directory
2. Start the server
3. Use the import command
4. The world is converted and stored in your chosen backend

### Step 5: Verify Installation

1. **Check ASP is loaded**:
   ```
   /aswm help
   ```
2. **List imported worlds**:
   ```
   /aswm list
   ```
3. **Test world loading**:
   ```
   /aswm load <world-name> <data-source>
   ```

## How It Works

### World Loading Process

1. **Dungeon Request**
   * Player/party requests dungeon entrance
   * NextDungeon determines which floor to load

2. **ASP World Clone**
   * ASP loads the slime world from storage
   * Creates a temporary instance clone
   * Loads the world on the server

3. **Instance Ready**
   * Players are teleported to the instance
   * Dungeon progression begins

4. **Cleanup**
   * When dungeon completes or players leave
   * Instance world is unloaded
   * Changes are discarded (unless saved)

### Storage Backends

#### File Storage
* **Pros**: Simple, no additional services needed
* **Cons**: Not shared across servers
* **Best for**: Single-server setups

#### MySQL Storage
* **Pros**: Shared across network, reliable, SQL management tools
* **Cons**: Requires MySQL server
* **Best for**: Multi-server networks, CloudNet integration

#### MongoDB Storage
* **Pros**: High performance, scalable, JSON-like structure
* **Cons**: Requires MongoDB server
* **Best for**: Large networks, high-volume operations

## Configuration Options

### Loader Type Selection

Choose based on your infrastructure:

```yaml
ASP:
  loaderType: "FILE"     # Single server, simple setup
  loaderType: "MYSQL"    # Network with CloudNet, shared storage
  loaderType: "MONGODB"  # Large scale, high performance needed
```

### MySQL Configuration

```yaml
mysql:
  url: "jdbc:mysql://localhost:3306/asm"  # Full JDBC URL
  host: "localhost"      # Database host
  port: 3306            # Database port
  database: "asm"       # Database name
  useSSL: false         # SSL connection
  username: "asm"       # Database user
  password: "password"  # Database password
```

**Advanced MySQL Options:**
* Add connection pool settings in JDBC URL
* Configure timeouts: `?connectTimeout=10000`
* Enable compression: `?useCompression=true`

### MongoDB Configuration

```yaml
mongodb:
  database: "asm"           # Database name
  collection: "worlds"      # Collection for world data
  username: "admin"         # MongoDB user (if auth enabled)
  password: "password"      # MongoDB password
  authSource: "admin"       # Authentication database
  host: "localhost"         # MongoDB host
  port: 27017              # MongoDB port
  uri: ""                  # Alternative: full connection string
```

**Using Connection URI:**
```yaml
mongodb:
  uri: "mongodb://username:password@localhost:27017/asm?authSource=admin"
```

## Managing Worlds

### Importing Worlds

#### From Server Directory
```
/aswm import <world-folder-name> <datasource>
```

Example:
```
/aswm import dungeon_castle file
/aswm import dungeon_tower mysql
```

### Listing Worlds

```
/aswm list              # List all slime worlds
/aswm list file         # List worlds in file storage
/aswm list mysql        # List worlds in MySQL storage
```

### Loading/Unloading Worlds

```
/aswm load <world> <datasource>      # Load a world
/aswm unload <world>                 # Unload a world
```

### Deleting Worlds

```
/aswm delete <world> <datasource>    # Delete from storage
```

> **Warning**: Be careful with delete commands. Always backup first!

## Troubleshooting

### World Won't Import

**Symptoms:** Import command fails or world corrupted

**Solutions:**
1. Ensure world is unloaded before importing
2. Check world folder is complete (region files, level.dat)
3. Verify sufficient disk space
4. Check console for specific errors
5. Try vanilla world format first, then import

### Instance Won't Load

**Symptoms:** Dungeon entrance fails, timeout

**Solutions:**
1. Verify world was imported successfully: `/aswm list`
2. Check storage backend is accessible (MySQL/MongoDB running)
3. Review ASP console logs for errors
4. Ensure correct loaderType in NextDungeon config
5. Test manual load: `/aswm load <world> <datasource>`

### Database Connection Failed

**Symptoms:** "Cannot connect to database" errors

**MySQL Solutions:**
1. Verify MySQL is running: `systemctl status mysql`
2. Test connection: `mysql -u asm -p -h localhost`
3. Check credentials in config
4. Ensure database exists
5. Verify firewall allows connection on port 3306

**MongoDB Solutions:**
1. Verify MongoDB is running: `systemctl status mongod`
2. Test connection: `mongo --host localhost --port 27017`
3. Check credentials in config
4. Ensure database/collection exists
5. Verify firewall allows connection on port 27017

### Performance Issues

**Symptoms:** Slow world loading, lag spikes

**Solutions:**
1. Check storage backend performance (disk I/O, database)
2. Optimize MySQL: increase `innodb_buffer_pool_size`
3. Optimize MongoDB: ensure indexes are created
4. Use SSD storage for file-based storage
5. Reduce world size (pre-generate only needed chunks)
6. Monitor server resources (RAM, CPU)

### World Data Corruption

**Symptoms:** World loads but is missing chunks or corrupted

**Solutions:**
1. Restore from backup
2. Re-import world from original source
3. Verify original world integrity
4. Check storage backend for issues
5. Review ASP logs during import

## Performance Optimization

### File Storage Optimization

```bash
# Use SSD for storage location
# Ensure proper permissions
chmod -R 755 plugins/AdvancedSlimePaper/worlds/

# Monitor disk I/O
iostat -x 1
```

### MySQL Optimization

Add to MySQL configuration (`my.cnf`):

```ini
[mysqld]
innodb_buffer_pool_size = 2G
innodb_log_file_size = 512M
innodb_flush_log_at_trx_commit = 2
innodb_flush_method = O_DIRECT
max_connections = 200
```

### MongoDB Optimization

```javascript
// Create indexes for better performance
db.worlds.createIndex({ "name": 1 })

// Enable compression
db.runCommand({
  collMod: "worlds",
  validationLevel: "moderate"
})
```

### World Size Optimization

* Pre-generate only necessary chunks
* Remove unused regions
* Minimize entity count
* Optimize redstone contraptions
* Use world border to limit size

## Best Practices

### World Management
* **Backup regularly** - especially before updates
* **Test imports** - verify world integrity after import
* **Use descriptive names** - `dungeon_castle_floor1` not `world1`
* **Clean unused worlds** - delete old/unused slime worlds

### Storage Backend Selection
* **Small servers** (<10 players): Use FILE storage
* **Medium networks** (10-50 players): Use MYSQL storage
* **Large networks** (50+ players): Use MONGODB storage
* **CloudNet integration**: Prefer MYSQL or MONGODB

### Performance
* **Monitor resources** - watch RAM and disk usage
* **Optimize worlds** - keep them as small as possible
* **Use caching** - if available in your database
* **Scale horizontally** - distribute load across nodes

### Security
* **Secure database access** - use strong passwords
* **Limit permissions** - grant only necessary privileges
* **Backup encryption** - encrypt sensitive backups
* **Regular updates** - keep ASP and database software updated

## Migration

### From Vanilla to ASP

1. **Backup current worlds**
2. **Install ASP** plugin
3. **Import worlds**:
   ```
   /aswm import dungeon_floor1 file
   ```
4. **Update NextDungeon config**:
   ```yaml
   InstanceProvider:
     type: "ASP"
   ```
5. **Test thoroughly**
6. **Remove old vanilla worlds** after verification

### Changing Storage Backend

From FILE to MYSQL:

1. **Set up MySQL** database
2. **Export from FILE**:
   ```
   /aswm export <world> file
   ```
3. **Import to MYSQL**:
   ```
   /aswm import <world> mysql
   ```
4. **Update config**:
   ```yaml
   ASP:
     loaderType: "MYSQL"
   ```
5. **Test and verify**

## Integration with CloudNet

ASP works seamlessly with CloudNet:

1. **Use MySQL or MongoDB** storage (not FILE)
2. **Configure same database** on all nodes
3. **Worlds are shared** across all CloudNet services
4. **Instances load quickly** from shared storage

Example configuration for CloudNet:

```yaml
InstanceProvider:
  type: "CLOUDNET"  # Or "ASP" depending on your setup

ASP:
  loaderType: "MYSQL"
  mysql:
    host: "shared.mysql.server"
    port: 3306
    database: "asm"
    username: "asm"
    password: "secure_password"
```

## Additional Resources

* [InfernalSuite GitHub](https://github.com/InfernalSuite/)
* [ASP Documentation](https://github.com/InfernalSuite/AdvancedSlimePaper/wiki)
* [Slime World Format](https://github.com/InfernalSuite/AdvancedSlimePaper/blob/master/SLIME_FORMAT.md)

***

ASP provides efficient and optimized world management for your dungeon instances!

