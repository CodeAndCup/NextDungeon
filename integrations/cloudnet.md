---
description: Learn how to integrate NextDungeon with CloudNet for dynamic instance management.
icon: cloud
---

# CloudNet

CloudNet integration enables NextDungeon to dynamically create and manage dungeon instances across your server network. This provides scalability, resource optimization, and seamless multi-server dungeon experiences.

## What is CloudNet?

[CloudNet](https://cloudnetservice.eu/) is a modern cloud system for Minecraft server networks. It allows automatic creation, management, and deletion of server instances based on demand.

### Benefits for NextDungeon

* **Dynamic Scaling**: Automatically create dungeon instances as needed
* **Resource Optimization**: Instances are created and destroyed on demand
* **Network Integration**: Seamless player transfer between lobby and dungeon servers
* **Multi-Instance Support**: Run multiple dungeon instances simultaneously
* **High Performance**: Distribute load across multiple nodes

## Requirements

### CloudNet Version
* **CloudNet 4.0.0-RC13 or newer** (4.0.0-RC13+ recommended)
* **Java 24 or 25** for CloudNet (Note: Different from Minecraft server Java requirements)

### NextDungeon Configuration
* Instance provider must be set to `CLOUDNET`
* Redis must be configured for cross-server communication
* CloudNet Bridge/Driver modules installed on Minecraft servers

## Installation

### Step 1: Install CloudNet

Follow the [official CloudNet documentation](https://cloudnetservice.eu/docs) to:

1. Download and install CloudNet on your master/wrapper nodes
2. Configure basic CloudNet settings
3. Set up your network structure

### Step 2: Create Dungeon Service Templates

CloudNet uses templates to define server configurations. Create templates for your dungeon instances:

#### Create Template Directory

```bash
# On your CloudNet master/wrapper node
cd local/templates
mkdir Dungeon
cd Dungeon
```

#### Configure Template

Create or copy a Minecraft server setup in this directory:
* Server JAR file (Spigot/Paper)
* Essential plugins folder with NextDungeon and dependencies
* Basic server configuration files

### Step 3: Create CloudNet Task

Define a task for dungeon instances:

```bash
# Using CloudNet console
tasks create
```

Follow the prompts or create a task configuration:

```json
{
  "name": "Dungeon",
  "runtime": "jvm",
  "hostAddress": "0.0.0.0",
  "javaCommand": "java",
  "memory": 2048,
  "maintenance": false,
  "autoDeleteOnStop": true,
  "staticServices": false,
  "groups": ["Dungeon"],
  "associatedNodes": [],
  "deletedFilesAfterStop": [],
  "processConfiguration": {
    "environment": "MINECRAFT_SERVER",
    "maxHeapMemorySize": 2048,
    "jvmOptions": [
      "-XX:+UseG1GC",
      "-XX:+ParallelRefProcEnabled",
      "-XX:MaxGCPauseMillis=200"
    ]
  },
  "startPort": 30000,
  "minServiceCount": 0,
  "templates": [
    {
      "prefix": "Dungeon",
      "name": "default",
      "storage": "local"
    }
  ]
}
```

**Key Configuration Options:**
* **memory**: RAM allocated per instance (2048 MB recommended minimum)
* **autoDeleteOnStop**: Automatically remove instances when empty
* **minServiceCount**: Keep at 0 for on-demand creation
* **startPort**: Starting port for instances

### Step 4: Configure NextDungeon for CloudNet

Edit `plugins/NextDungeon/config.yml`:

```yaml
# Instance Provider Configuration
InstanceProvider:
  type: "CLOUDNET"

# Redis Configuration (required for CloudNet)
RedisConfiguration:
  host: "127.0.0.1"
  port: 6379
  username: "default"
  password: ""
  topic: "dungeons:packets"
```

### Step 5: Install CloudNet Modules

Ensure these plugins/modules are installed:

**On CloudNet Master:**
* CloudNet Bridge/Driver modules

**On Lobby/Proxy Servers:**
* CloudNet Bridge plugin

**On Dungeon Template:**
* CloudNet Bridge plugin
* NextDungeon plugin
* All required dependencies (Parties, MMOCore, MythicMobs)

### Step 6: Verify Installation

1. **Start CloudNet** master and wrapper nodes
2. **Check task creation**:
   ```
   tasks list
   ```
3. **Start your lobby server**
4. **Test instance creation** by entering a dungeon

## How It Works

### Instance Lifecycle

1. **Player Requests Dungeon**
   * Player or party attempts to enter a dungeon
   * NextDungeon checks requirements

2. **Instance Creation**
   * NextDungeon requests CloudNet to create a new service
   * CloudNet spins up a server from the Dungeon template
   * World data is loaded for the specific dungeon floor

3. **Player Transfer**
   * Once ready, players are transferred to the new instance
   * Redis coordinates the transfer across servers

4. **Active Instance**
   * Players complete the dungeon
   * Progress is tracked and saved to database

5. **Instance Cleanup**
   * When all players leave or time expires
   * Instance is stopped and removed
   * Resources are freed for new instances

### Cross-Server Communication

NextDungeon uses Redis for real-time communication:
* Instance status updates
* Player transfer coordination
* Progress synchronization
* Party management across servers

## Configuration Options

### CloudNet-Specific Settings

While NextDungeon doesn't expose many CloudNet-specific settings directly, you can customize through:

#### Task Configuration
Modify CloudNet task settings for:
* Memory allocation
* Max concurrent instances
* Node distribution
* Port ranges

#### Template Customization
Customize the Dungeon template with:
* Performance-optimized server settings
* Pre-loaded chunks
* Custom plugins per dungeon type

### Advanced Configuration

#### Multiple Dungeon Types

Create different CloudNet tasks for different dungeon categories:

```json
// Easy dungeons - lower resources
{
  "name": "Dungeon-Easy",
  "memory": 1024,
  "templates": [{"prefix": "Dungeon-Easy", "name": "default"}]
}

// Hard dungeons - higher resources
{
  "name": "Dungeon-Hard",
  "memory": 4096,
  "templates": [{"prefix": "Dungeon-Hard", "name": "default"}]
}
```

#### Node-Specific Instances

Distribute dungeons across specific nodes:

```json
{
  "name": "Dungeon",
  "associatedNodes": ["Node-1", "Node-2"],
  // Other configurations...
}
```

## Troubleshooting

### Instance Won't Start

**Symptoms:** Players can't enter dungeon, timeout errors

**Solutions:**
1. Check CloudNet task is configured correctly:
   ```
   tasks list
   tasks reload
   ```
2. Verify template exists and has all required files
3. Check node has sufficient resources (RAM, CPU)
4. Review CloudNet console for error messages

### Redis Connection Failed

**Symptoms:** "Redis connection error" in console

**Solutions:**
1. Verify Redis is running:
   ```bash
   redis-cli ping
   # Should return: PONG
   ```
2. Check Redis host/port in NextDungeon config
3. Verify firewall allows Redis connections
4. Test Redis authentication if password is set

### Players Stuck in Transfer

**Symptoms:** Players disconnect or get stuck during transfer

**Solutions:**
1. Verify CloudNet Bridge is installed on all servers
2. Check Redis connectivity across servers
3. Ensure proxy (Velocity/BungeeCord) allows connections
4. Review console logs on both sending and receiving servers

### Instances Not Cleaning Up

**Symptoms:** Many idle dungeon servers remain running

**Solutions:**
1. Verify `autoDeleteOnStop: true` in CloudNet task
2. Check NextDungeon instance timeout settings
3. Ensure no errors preventing proper shutdown
4. Manually stop instances: `/cloudnet stop Dungeon-1`

### World Data Not Loading

**Symptoms:** Instance starts but world is empty/default

**Solutions:**
1. Verify world files exist in template
2. Check ASP/Vanilla instance provider settings
3. Ensure proper world loading in server.properties
4. Review dungeon configuration for correct world name

## Performance Optimization

### CloudNet Settings

```json
{
  "memory": 2048,  // Adjust based on dungeon complexity
  "processConfiguration": {
    "maxHeapMemorySize": 2048,
    "jvmOptions": [
      "-XX:+UseG1GC",
      "-XX:+ParallelRefProcEnabled",
      "-XX:MaxGCPauseMillis=200",
      "-XX:+DisableExplicitGC",
      "-XX:+AlwaysPreTouch",
      "-XX:G1NewSizePercent=30",
      "-XX:G1MaxNewSizePercent=40",
      "-XX:G1HeapRegionSize=8M",
      "-XX:G1ReservePercent=20",
      "-XX:G1HeapWastePercent=5",
      "-XX:G1MixedGCCountTarget=4",
      "-XX:InitiatingHeapOccupancyPercent=15",
      "-XX:G1MixedGCLiveThresholdPercent=90",
      "-XX:SurvivorRatio=32",
      "-XX:+PerfDisableSharedMem",
      "-XX:MaxTenuringThreshold=1",
      "-Dusing.aikars.flags=https://mcflags.emc.gs",
      "-Daikars.new.flags=true"
    ]
  }
}
```

### Resource Allocation

**Recommended per instance:**
* **Small dungeons** (1-5 players): 1-2 GB RAM
* **Medium dungeons** (5-10 players): 2-3 GB RAM
* **Large raids** (10+ players): 4+ GB RAM

### Instance Limits

Set maximum concurrent instances to prevent resource exhaustion:

```json
{
  "maxServiceCount": 10  // Maximum 10 concurrent dungeon instances
}
```

## Best Practices

### Template Management
* Keep templates minimal - only essential plugins
* Pre-configure settings to avoid runtime changes
* Use symlinks for shared resources
* Version your templates

### Network Architecture
* Use Redis on a dedicated server for large networks
* Distribute dungeon instances across multiple nodes
* Keep lobby separate from dungeon instances
* Monitor network bandwidth

### Monitoring
* Watch CloudNet memory usage
* Monitor instance creation/deletion rates
* Track player transfer success rates
* Log unusual patterns or errors

### Security
* Restrict CloudNet console access
* Use Redis authentication
* Isolate dungeon instances from main network
* Validate player permissions before transfer

## Integration with Other Systems

### With Velocity/BungeeCord
CloudNet handles proxy integration automatically via Bridge modules.

### With Parties Plugin
Party members are transferred together to the same instance.

### With MMOCore
Player data syncs via database, progression tracked across instances.

## Migration from Vanilla/ASP

If switching from Vanilla or ASP to CloudNet:

1. **Backup all data** - configurations, databases, worlds
2. **Set up CloudNet** as described above
3. **Update config.yml**:
   ```yaml
   InstanceProvider:
     type: "CLOUDNET"  # Changed from VANILLA or ASP
   ```
4. **Test thoroughly** before production use
5. **Monitor performance** during transition

## Additional Resources

* [CloudNet Official Documentation](https://cloudnetservice.eu/docs)
* [CloudNet Discord](https://discord.cloudnetservice.eu/)
* [CloudNet GitHub](https://github.com/CloudNetService/CloudNet-v3)

***

CloudNet integration provides powerful scaling capabilities for your dungeon server!

