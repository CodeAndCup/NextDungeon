---
description: Here is an example configuration for a dungeon with multiple floors and steps.
icon: gears
---

# Dungeon Config

Dungeon configuration files define the structure, requirements, rules, and progression of your dungeons. Each dungeon is configured in a separate YAML file located in `plugins/NextDungeon/dungeons/`.

## Configuration Overview

A dungeon configuration includes:
* **Dungeon Metadata** - ID, name, and basic information
* **Floors** - Multiple progressive stages/levels
* **Requirements** - Entry prerequisites and restrictions
* **Rules** - Gameplay rules and mechanics
* **Steps** - Progression areas within each floor
* **World Settings** - Spawn points and difficulty

## File Structure

Create individual files for each dungeon in `plugins/NextDungeon/dungeons/`:
* `starter_dungeon.yml`
* `castle_raid.yml`
* `mystic_tower.yml`

## Complete Configuration Example

```yaml
# ╔════════════════════════════════════════════════════════════════════════╗
# ║                       NextDungeon Configuration                        ║
# ║         This file defines one dungeon and its floors configuration.    ║
# ╚════════════════════════════════════════════════════════════════════════╝

dungeon:
  id: "example_dungeon"
  name: "Example Dungeon"
  floors:
    - id: "floor1"
      name: "The Forgotten Crypts"
      description: "Explore the ancient crypts\nfilled with mysteries and dangers."

      world:
        difficulty: "normal"
        spawn: { x: 0, y: 100, z: 0 }

      requirements:
        retry_cooldown: "15m" # Time before retrying
        required_floor: [] # List of required floor(s) (id)
        minimum_level: 0
        party:
          min_size: 2
          max_size: 25
        required_items:
          - "Old Key"
        forbidden_items:
          - "Magic Wand"

      rules:
        death_ban: "15m"
        gamemode: "SURVIVAL"
        allow_flight: false

      steps:
        - id: "entrance"
          name: "Catacombs Entrance"
          region:
            pos1: { x: -10, y: 60, z: -10 }
            pos2: { x: 10, y: 80, z: 10 }

        - id: "dark_gallery"
          name: "Dark Gallery"
          region:
            pos1: { x: 10, y: 60, z: -10 }
            pos2: { x: 30, y: 80, z: 10 }

        - id: "trapped_room"
          name: "Trapped Hall"
          region:
            pos1: { x: 30, y: 60, z: -10 }
            pos2: { x: 50, y: 80, z: 10 }

        - id: "mini_boss"
          name: "Guardian Chamber"
          region:
            pos1: { x: 50, y: 60, z: -15 }
            pos2: { x: 70, y: 85, z: 15 }

        - id: "final_boss"
          name: "Phantom Lord's Chamber"
          region:
            pos1: { x: 70, y: 60, z: -20 }
            pos2: { x: 100, y: 90, z: 20 }
    
    - id: "floor2"
      name: "The Shadow Labyrinth"
      description: "Navigate through a labyrinth\nfilled with traps and creatures."

      world:
        difficulty: "hard"
        spawn: { x: 0, y: 100, z: 0 }

      requirements:
        retry_cooldown: "30m"
        required_floor: ["example_dungeon_floor1"]
        minimum_level: 5
        party:
          min_size: 3
          max_size: 20
        required_items:
          - "Silver Key"
        forbidden_items:
          - "Fire Sword"

      rules:
        death_ban: "30m"
        gamemode: "SURVIVAL"
        allow_flight: false

      steps:
        - id: "labyrinth_entrance"
          name: "Labyrinth Entrance"
          region:
            pos1: { x: -15, y: 60, z: -15 }
            pos2: { x: 15, y: 80, z: 15 }

        - id: "winding_corridors"
          name: "Winding Corridors"
          region:
            pos1: { x: 15, y: 60, z: -15 }
            pos2: { x: 45, y: 80, z: 15 }

        - id: "illusion_room"
          name: "Hall of Illusions"
          region:
            pos1: { x: 45, y: 60, z: -20 }
            pos2: { x: 75, y: 85, z: 20 }

        - id: "labyrinth_boss"
          name: "Labyrinth Guardian"
          region:
            pos1: { x: 75, y: 60, z: -25 }
            pos2: { x: 110, y: 95, z: 25 }
```

## Configuration Fields Explained

### Dungeon Level

The root level defines the dungeon identity:

```yaml
dungeon:
  id: "my_dungeon"      # Unique identifier (lowercase, no spaces)
  name: "My Dungeon"    # Display name shown to players
  floors: []            # Array of floor configurations
```

**Important Notes:**
* `id` must be unique across all dungeons
* Use descriptive IDs: `castle_raid`, `mystic_tower`, not `dungeon1`
* `name` supports color codes: `&c&lEpic Dungeon`

### Floor Configuration

Each floor represents a complete dungeon level:

```yaml
- id: "floor1"                  # Unique floor ID within this dungeon
  name: "First Floor"           # Display name
  description: "Description"    # Multi-line description (use \n for line breaks)
```

**Floor ID Format:**
When referencing floors in `required_floor`, use format: `{dungeonId}_{floorId}`

Example: `example_dungeon_floor1`

### World Settings

Configure the dungeon world:

```yaml
world:
  difficulty: "normal"          # peaceful, easy, normal, hard
  spawn: { x: 0, y: 100, z: 0 } # Player spawn coordinates
```

**Difficulty Levels:**
* `peaceful` - No hostile mob spawning
* `easy` - Reduced mob damage
* `normal` - Standard difficulty
* `hard` - Increased mob damage and hunger

**Spawn Point:**
* Use exact coordinates where players should spawn
* Ensure the location is safe (not in walls, lava, etc.)
* Consider spawn direction (players spawn facing south by default)

### Requirements

Control who can enter the dungeon:

```yaml
requirements:
  retry_cooldown: "15m"           # Cooldown between attempts
  required_floor: []              # Prerequisites
  minimum_level: 0                # MMOCore level requirement
  party:
    min_size: 1                   # Minimum party size
    max_size: 5                   # Maximum party size
  required_items:                 # Items players must have
    - "Dungeon Key"
  forbidden_items:                # Items players cannot bring
    - "Teleport Scroll"
```

#### Retry Cooldown

Time format options:
* `s` - Seconds: `30s`
* `m` - Minutes: `15m`
* `h` - Hours: `2h`
* `d` - Days: `1d`

#### Required Floors

```yaml
required_floor: ["dungeon1_floor1", "dungeon2_floor3"]
```

All listed floors must be completed before entry.

#### Minimum Level

Requires MMOCore integration:
```yaml
minimum_level: 10  # Player must be level 10+
```

Set to `0` to disable level requirement.

#### Party Size

```yaml
party:
  min_size: 1   # Allow solo (1) or require party (2+)
  max_size: 10  # Maximum party members
```

**Common Configurations:**
* Solo play: `min_size: 1, max_size: 1`
* Small party: `min_size: 2, max_size: 5`
* Raid: `min_size: 10, max_size: 25`

#### Required/Forbidden Items

```yaml
required_items:
  - "Dungeon Key"         # Player must have this
  - "Torch"               # And this

forbidden_items:
  - "Recall Scroll"       # Cannot bring this
  - "Creative Wand"       # Or this
```

**Important Notes:**
* Items checked by display name (case-sensitive)
* Only checks inventory (not armor/offhand)
* Items are not consumed on entry

### Rules

Define gameplay mechanics:

```yaml
rules:
  death_ban: "15m"           # Ban duration on death
  gamemode: "SURVIVAL"       # SURVIVAL, ADVENTURE, CREATIVE
  allow_flight: false        # Allow flying
```

#### Death Ban

Duration player is banned from dungeon after death:
```yaml
death_ban: "15m"   # 15 minutes
death_ban: "1h"    # 1 hour
death_ban: "0s"    # No ban (respawn immediately)
```

Uses the ban command from main config (`ReviveSystem.banCommand`).

#### Gamemode

```yaml
gamemode: "SURVIVAL"    # Full survival mechanics
gamemode: "ADVENTURE"   # Cannot break/place blocks
gamemode: "CREATIVE"    # Creative mode (not recommended)
```

**Recommendation:** Use `ADVENTURE` to prevent griefing.

#### Allow Flight

```yaml
allow_flight: true   # Players can fly
allow_flight: false  # Flight disabled
```

### Steps

Define progression areas within the floor:

```yaml
steps:
  - id: "entrance"               # Unique step ID
    name: "Entrance Hall"        # Display name
    region:
      pos1: { x: 0, y: 60, z: 0 }    # First corner
      pos2: { x: 20, y: 80, z: 20 }  # Opposite corner
```

#### Step Regions

Regions define cuboid areas:
* `pos1` and `pos2` are opposite corners
* Forms a rectangular box
* All coordinates must be valid world positions

**Getting Coordinates:**
1. Stand at first corner, press F3, note coordinates
2. Stand at opposite corner, note coordinates
3. Use WorldEdit: `//pos1` and `//pos2`

**Region Tips:**
* Make regions slightly larger than needed
* Ensure regions don't overlap incorrectly
* Use consistent Y-level ranges (e.g., 60-80)

## Best Practices

### Dungeon Design
* **Progressive Difficulty**: Each floor should be harder than the last
* **Clear Progression**: Players should understand how to advance
* **Reasonable Requirements**: Don't make entry too restrictive
* **Balanced Cooldowns**: Match cooldown to difficulty and length

### Floor Structure
* **Logical Flow**: Steps should follow a logical progression
* **Varied Challenges**: Mix combat, puzzles, and exploration
* **Fair Checkpoints**: Don't make players repeat too much
* **Boss Placement**: Final step typically contains the boss

### Step Configuration
* **Unique IDs**: Use descriptive step IDs (not `step1`, `step2`)
* **Proper Sizing**: Make regions appropriate for their purpose
* **Boss Rooms**: Larger regions for boss encounters
* **Corridors**: Smaller regions for connecting areas

### Testing Checklist
- [ ] All coordinates are valid and correct
- [ ] Spawn point is safe and accessible
- [ ] Steps progress logically
- [ ] Regions don't overlap incorrectly
- [ ] Party size requirements are reasonable
- [ ] Level requirements match difficulty
- [ ] Cooldowns are balanced
- [ ] Death ban duration is fair

## Configuration Examples

### Solo Dungeon

```yaml
dungeon:
  id: "solo_challenge"
  name: "Solo Challenge Dungeon"
  floors:
    - id: "main"
      name: "The Trial"
      requirements:
        minimum_level: 15
        party:
          min_size: 1
          max_size: 1    # Solo only
        retry_cooldown: "30m"
```

### Party Dungeon

```yaml
dungeon:
  id: "party_adventure"
  name: "Party Adventure"
  floors:
    - id: "floor1"
      name: "Teamwork Trial"
      requirements:
        party:
          min_size: 3    # Requires party
          max_size: 5
        retry_cooldown: "1h"
```

### Progressive Multi-Floor

```yaml
dungeon:
  id: "tower"
  name: "The Tower of Ascension"
  floors:
    - id: "ground"
      name: "Ground Floor"
      requirements:
        minimum_level: 1
        party:
          min_size: 1
          max_size: 10
    
    - id: "second"
      name: "Second Floor"
      requirements:
        required_floor: ["tower_ground"]
        minimum_level: 5
    
    - id: "top"
      name: "Top Floor"
      requirements:
        required_floor: ["tower_second"]
        minimum_level: 10
```

### Raid Dungeon

```yaml
dungeon:
  id: "epic_raid"
  name: "Epic Raid Instance"
  floors:
    - id: "raid"
      name: "The Final Confrontation"
      requirements:
        minimum_level: 50
        party:
          min_size: 10
          max_size: 25
        retry_cooldown: "24h"
        required_items:
          - "Raid Key"
      rules:
        death_ban: "1h"
        gamemode: "ADVENTURE"
        allow_flight: false
```

## Common Issues

### Dungeon Won't Load

**Check:**
* YAML syntax is correct (proper indentation, spacing)
* Dungeon ID is unique
* All required fields are present
* File is in `plugins/NextDungeon/dungeons/`

**Test:** Use a YAML validator online.

### Players Can't Enter

**Check:**
* Party size meets min/max requirements
* Players meet level requirement
* Required floors are completed
* Required items are in inventory
* Cooldown has expired

### Steps Not Working

**Check:**
* Step IDs are unique within the floor
* Coordinates are correct (not all zeros)
* Regions are properly formed (pos1 and pos2 valid)
* Regions don't have inverted coordinates

### Required Floor Not Recognized

**Format must be:** `{dungeonId}_{floorId}`

**Example:**
```yaml
# Dungeon ID: tower
# Floor ID: ground
# Reference: tower_ground

required_floor: ["tower_ground"]
```

## Advanced Topics

### Dynamic Content

Use the web editor to add:
* Custom mob spawns per step
* Trigger-based events
* Conditional progression
* Custom rewards

### Multiple Paths

Create branching dungeons:
```yaml
steps:
  - id: "fork"
    name: "The Crossroads"
  - id: "path_left"
    name: "Left Path"
  - id: "path_right"
    name: "Right Path"
  - id: "convergence"
    name: "The Meeting Point"
```

Configure logic via web editor.

### Secret Areas

Create optional secret steps:
* Not required for completion
* Contain bonus rewards
* Hidden entrances

### Time Trials

Add time-based challenges:
* Configure via web editor
* Set time limits per step
* Reward fast completion

## Additional Resources

* [Creating Dungeons Guide](../dungeon-management/creating-dungeons.md)
* [Editing Dungeons](../dungeon-management/editing-dungeons.md)
* [Web Editor Documentation](../dungeon-management/editing-dungeons.md)
* [Quick Start Guide](../getting-started/quick-start-guide.md)

***

Proper dungeon configuration creates engaging and balanced experiences for players!


