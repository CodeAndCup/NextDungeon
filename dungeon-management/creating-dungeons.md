---
description: Learn how to create custom dungeons from scratch with NextDungeon.
icon: layer-plus
---

# Creating Dungeons

Creating dungeons in NextDungeon involves building the physical world, defining the dungeon structure in configuration files, and optionally adding custom logic with the visual web editor. This guide walks you through the entire process.

## Overview

The dungeon creation process consists of several steps:

1. **Building the Dungeon World** - Create the physical structure in Minecraft
2. **Configuring the Dungeon** - Define dungeon properties, floors, and steps
3. **Setting up Regions** - Mark important areas and boundaries
4. **Adding Logic** - Configure triggers, events, and actions
5. **Testing** - Verify everything works as expected

## Step 1: Building Your Dungeon World

### Planning Your Dungeon

Before building, plan out:

* **Dungeon theme** - Medieval, fantasy, sci-fi, etc.
* **Number of floors** - How many progressive stages
* **Difficulty progression** - Easy to hard, or themed difficulty
* **Step structure** - Linear, branching paths, or open world
* **Boss encounters** - Location and type of boss fights
* **Rewards** - What players earn for completion

### Building the World

1. **Create a new world** or use an existing one
2. **Build your dungeon structure**:
   * Entrance area
   * Corridors and rooms
   * Challenge areas
   * Boss arenas
   * Exit/completion area
3. **Add decorations and details**
4. **Place spawn points** for players and mobs
5. **Mark coordinates** of important locations

> **Tip:** Use WorldEdit to speed up building and to easily get region coordinates.

### World Requirements

* Ensure the spawn point is safe and accessible
* Leave space for mob spawns (if using MythicMobs)
* Consider performance - avoid excessive redstone or entities
* Test lighting and visibility
* Ensure proper boundaries to prevent players from escaping

## Step 2: Creating the Dungeon Configuration

### Basic Structure

Create a new YAML file in `plugins/NextDungeon/dungeons/`. Example: `my_dungeon.yml`

```yaml
dungeon:
  id: "my_dungeon"
  name: "My Epic Dungeon"
  floors:
    - id: "floor1"
      name: "Floor 1: The Beginning"
      description: "The first challenge awaits..."
      
      world:
        difficulty: "normal"
        spawn: { x: 0, y: 64, z: 0 }
      
      requirements:
        retry_cooldown: "15m"
        required_floor: []
        minimum_level: 1
        party:
          min_size: 1
          max_size: 5
        required_items: []
        forbidden_items: []
      
      rules:
        death_ban: "15m"
        gamemode: "SURVIVAL"
        allow_flight: false
      
      steps:
        - id: "entrance"
          name: "Entrance Hall"
          region:
            pos1: { x: -20, y: 60, z: -20 }
            pos2: { x: 20, y: 80, z: 20 }
        
        - id: "main_hall"
          name: "Main Hall"
          region:
            pos1: { x: 20, y: 60, z: -20 }
            pos2: { x: 60, y: 80, z: 20 }
        
        - id: "boss_room"
          name: "Boss Chamber"
          region:
            pos1: { x: 60, y: 60, z: -30 }
            pos2: { x: 100, y: 90, z: 30 }
```

### Configuration Breakdown

#### Dungeon Level
* **id**: Unique identifier for your dungeon (use lowercase, no spaces)
* **name**: Display name shown to players
* **floors**: Array of floor configurations

#### Floor Level
* **id**: Unique floor identifier within this dungeon
* **name**: Display name for this floor
* **description**: Multi-line description (use `\n` for line breaks)
* **world**: World-specific settings
* **requirements**: Entry requirements for players
* **rules**: Gameplay rules for this floor
* **steps**: Progression stages within the floor

#### World Settings
* **difficulty**: Minecraft difficulty (`peaceful`, `easy`, `normal`, `hard`)
* **spawn**: Coordinates where players spawn `{ x, y, z }`

#### Requirements
* **retry_cooldown**: Time before player can retry after failure (e.g., `15m`, `1h`, `30s`)
* **required_floor**: Array of floor IDs that must be completed first
* **minimum_level**: Minimum player level (requires MMOCore)
* **party.min_size**: Minimum party members required
* **party.max_size**: Maximum party size allowed
* **required_items**: Items players must have (exact names)
* **forbidden_items**: Items players cannot bring in

#### Rules
* **death_ban**: How long players are banned after death (uses ban command from config)
* **gamemode**: Game mode for players (`SURVIVAL`, `ADVENTURE`, `CREATIVE`)
* **allow_flight**: Whether flight is permitted (`true`/`false`)

#### Steps
Steps define progression areas within a floor:
* **id**: Unique step identifier
* **name**: Display name for the step
* **region**: Cuboid region defining the step area
  * **pos1**: First corner `{ x, y, z }`
  * **pos2**: Opposite corner `{ x, y, z }`

## Step 3: Adding Multiple Floors

For dungeons with multiple floors, add additional floor configurations:

```yaml
dungeon:
  id: "tower_dungeon"
  name: "The Tower of Trials"
  floors:
    - id: "ground_floor"
      name: "Ground Floor"
      # ... configuration ...
    
    - id: "second_floor"
      name: "Second Floor"
      requirements:
        required_floor: ["tower_dungeon_ground_floor"]
        minimum_level: 5
        party:
          min_size: 2
          max_size: 5
      # ... rest of configuration ...
    
    - id: "top_floor"
      name: "Top Floor - Final Boss"
      requirements:
        required_floor: ["tower_dungeon_second_floor"]
        minimum_level: 10
        party:
          min_size: 3
          max_size: 5
      # ... rest of configuration ...
```

> **Note:** When referencing required floors, use the format `{dungeonId}_{floorId}`.

## Step 4: Advanced Configuration

### Custom Items

To use custom items in requirements:

```yaml
required_items:
  - "Dungeon Key"
  - "Magic Torch"

forbidden_items:
  - "Recall Scroll"
  - "Teleport Stone"
```

> Items are checked by display name. Ensure exact name matching.

### Time-based Cooldowns

Supported time formats:
* `s` - seconds (e.g., `30s`)
* `m` - minutes (e.g., `15m`)
* `h` - hours (e.g., `2h`)
* `d` - days (e.g., `1d`)

### Level Requirements

When using MMOCore integration:

```yaml
requirements:
  minimum_level: 10
```

Players below this level cannot enter.

## Step 5: Loading Your Dungeon

After creating the configuration file:

1. **Reload the plugin** or restart the server
2. **Load the dungeon** using:
   ```
   /dungeon admin load my_dungeon
   ```
3. **Verify it loaded**:
   ```
   /dungeon debug list dungeons
   ```

## Step 6: Adding Custom Logic with Web Editor

For advanced dungeon mechanics, use the visual web editor:

1. **Enter edit mode** for a floor:
   ```
   /dungeon admin edit my_dungeon floor1
   ```
2. **Start the web editor**:
   ```
   /dungeon admin webeditor start
   ```
3. **Open the provided URL** in your browser
4. **Configure triggers and actions** using the Blockly interface:
   * Step entry/exit events
   * Mob spawn triggers
   * Custom objective completion
   * Reward distribution
   * Boss fight mechanics

See [Editing Dungeons](editing-dungeons.md) for detailed web editor instructions.

## Step 7: Testing Your Dungeon

### Pre-Launch Checklist

- [ ] All coordinates are correct
- [ ] Spawn point is safe and accessible
- [ ] Steps progress logically
- [ ] Party size requirements are reasonable
- [ ] Death ban duration is appropriate
- [ ] Required/forbidden items are configured correctly
- [ ] Cooldowns are balanced

### Testing Process

1. **Solo test** (if min_size allows):
   * Enter the dungeon alone
   * Walk through each step
   * Verify progression works
   
2. **Party test**:
   * Form a party of appropriate size
   * Test party entry requirements
   * Verify all party members teleport correctly
   
3. **Failure scenarios**:
   * Test death and respawn
   * Verify ban system works
   * Check cooldown timers
   
4. **Performance test**:
   * Monitor server TPS
   * Check for lag or memory issues
   * Verify instance creation/deletion

## Best Practices

### Design
* **Start simple** - Get basic structure working before adding complexity
* **Clear progression** - Make it obvious where players should go next
* **Balanced difficulty** - Match difficulty to level requirements
* **Fair checkpoints** - Don't make players repeat too much on death

### Configuration
* **Consistent naming** - Use clear, descriptive IDs and names
* **Document changes** - Add comments to complex configurations
* **Version control** - Keep backups of working configurations
* **Test thoroughly** - Always test after making changes

### Performance
* **Optimize builds** - Avoid excessive entities or complex redstone
* **Reasonable regions** - Don't make step regions unnecessarily large
* **Instance management** - Configure proper cleanup and timeouts
* **Monitor resources** - Watch RAM and CPU usage during testing

## Troubleshooting

### Dungeon won't load
* Check YAML syntax (use a YAML validator)
* Verify dungeon ID is unique
* Check console for specific error messages

### Players can't enter
* Verify party size is within min/max range
* Check required_floor prerequisites are met
* Ensure required_items are correct (exact names)
* Verify cooldown has expired

### Steps not progressing
* Confirm region coordinates are correct
* Check that regions don't overlap incorrectly
* Verify step IDs are unique within the floor

### Instance creation fails
* Check CloudNet/ASP configuration
* Verify world files are accessible
* Ensure sufficient server resources
* Review instance provider logs

## Examples

### Simple Linear Dungeon
A basic dungeon with clear progression from start to end.

### Multi-Floor Tower
Progressive difficulty across multiple floors with prerequisites.

### Open World Dungeon
Non-linear exploration with multiple paths and objectives.

### Raid Dungeon
Large-scale dungeon requiring coordination and specific roles.

## Next Steps

* Learn about [Editing Dungeons](editing-dungeons.md) with the web editor
* Explore [MythicMobs Integration](../integrations/mythicmobs.md) for custom enemies
* Configure [Rewards and Progression](#) (if available)
* Set up [CloudNet Integration](../integrations/cloudnet.md) for scalability

***

Ready to create amazing dungeon experiences for your players!

