---
description: Follow these steps to learn how to edit your dungeon floors and configure custom mechanics.
icon: pen-to-square
---

# Editing Dungeons

Editing dungeons in NextDungeon involves modifying the physical world structure and configuring custom logic using the visual web editor. This guide covers both aspects of dungeon editing.

## Overview

Dungeon editing consists of two main components:

1. **World Editing** - Modifying the physical dungeon structure (blocks, decorations, layout)
2. **Logic Editing** - Configuring triggers, events, mob spawns, and custom mechanics using the web editor

## Starting an Edit Session

### Step 1: Load a Floor in Edit Mode

To begin editing a dungeon floor, use the admin edit command:

```
/nd admin edit <dungeon> <floor>
```

**Parameters:**
* `<dungeon>` - The ID of the dungeon that contains the floor you want to edit
* `<floor>` - The ID of the specific floor to edit

**Example:**
```
/nd admin edit castle_dungeon floor1
```

> **Note:** Like normal dungeon loading, entering edit mode can take a few minutes depending on your instance provider (CloudNet, ASP, or Vanilla).

### Step 2: Wait for Instance Creation

The plugin will:
1. Create a temporary edit instance of the dungeon floor
2. Load the world data
3. Teleport you to the edit server

**Console Output:**
Watch for messages indicating:
* Instance creation started
* World loading progress
* Successful teleportation

## World Editing

Once you're in edit mode, you have full control over the dungeon world.

### What You Can Do

* **Build and destroy blocks** - Modify the dungeon structure
* **Place decorations** - Add details, furniture, obstacles
* **Configure spawn points** - Set mob spawn locations
* **Create arenas** - Design boss fight areas
* **Add obstacles and puzzles** - Place traps, parkour, challenges
* **Test navigation** - Walk through to verify flow

### Tools and Plugins

Use these tools to enhance your editing workflow:

#### WorldEdit
* Select regions: `//wand`
* Copy/paste sections: `//copy`, `//paste`
* Replace blocks: `//replace`
* Generate shapes: `//sphere`, `//cyl`

#### WorldGuard
* Define protected regions
* Set spawn points
* Configure region flags

#### Recommended Workflow

1. **Plan your layout** - Sketch or visualize the design
2. **Build the structure** - Create walls, rooms, corridors
3. **Add details** - Decorations, lighting, atmosphere
4. **Mark coordinates** - Note important locations (spawns, objectives)
5. **Test pathing** - Walk through to ensure smooth progression
6. **Save coordinates** - Update dungeon config with accurate regions

### Important Considerations

* **Performance** - Avoid excessive redstone, entities, or complex builds
* **Accessibility** - Ensure all areas are reachable
* **Lighting** - Balance atmosphere with player visibility
* **Safety** - No void falls, lava without warning, etc.
* **Scaling** - Design for your target party size

## Web Editor for Logic Configuration

After building the physical structure, use the web editor to configure dungeon mechanics.

### Step 1: Start the Web Editor

While in edit mode, start the web editor:

```
/nd admin webeditor start
```

This command will:
* Start a web server proxy
* Generate a unique session URL
* Display the URL in chat

**Example Output:**
```
[NextDungeon] Web Editor started!
[NextDungeon] Access at: http://your-server:7734/editor?session=abc123
```

### Step 2: Access the Editor

1. **Copy the URL** provided in the chat
2. **Open it in your web browser** (Chrome, Firefox, Edge recommended)
3. **The Blockly editor will load** with the current floor's configuration

> **Note:** Ensure the web editor port (default: 7734) is accessible from your computer. Check firewall settings if you can't connect.

### Step 3: Configure Logic with Blockly

The Blockly editor provides a visual, drag-and-drop interface for configuring:

#### Triggers (When)
* **Step Entry** - When players enter a step region
* **Step Exit** - When players leave a step region
* **Timer Events** - After a specific time
* **Mob Death** - When a specific mob dies
* **All Mobs Dead** - When all mobs in an area are defeated
* **Player Action** - When players interact with objects
* **Objective Complete** - When a custom objective is met

#### Actions (Then)
* **Spawn Mobs** - Summon MythicMobs at locations
* **Open/Close Doors** - Control access to areas
* **Display Messages** - Show text to players
* **Grant Rewards** - Give items, experience, currency
* **Teleport Players** - Move players to specific locations
* **Play Effects** - Particles, sounds, visual effects
* **Modify World** - Place/remove blocks
* **Complete Step** - Progress to next step
* **End Dungeon** - Complete the floor

#### Conditions (If)
* **Check Player Count** - Number of players in area
* **Check Health** - Boss or player health thresholds
* **Check Items** - Player has specific items
* **Check Time** - Current time or elapsed time
* **Random Chance** - Probability-based events

### Web Editor Features

#### Blockly Interface

The editor uses Blockly, a visual programming language:

**Blocks Categories:**
* **Events** - Trigger blocks (when something happens)
* **Actions** - What to do (spawn, teleport, message)
* **Logic** - If/else, loops, comparisons
* **Variables** - Store and use values
* **Math** - Calculations and numbers
* **Mobs** - MythicMobs integration
* **Players** - Player-related actions
* **World** - World manipulation

**How to Use:**
1. **Drag blocks** from the toolbox on the left
2. **Connect blocks** by snapping them together
3. **Configure parameters** by clicking on block fields
4. **Test logic** by saving and running the dungeon

#### Save and Apply

* **Save** - Stores your configuration
* **Apply** - Updates the dungeon floor with new logic
* **Revert** - Discards changes and restores previous version

### Step 4: Test Your Configuration

After configuring logic:

1. **Save your changes** in the web editor
2. **Stop the web editor**: `/nd admin webeditor stop`
3. **Test the dungeon** by running through it
4. **Verify all triggers work** as expected
5. **Adjust and iterate** as needed

### Example Configuration Scenarios

#### Basic Mob Spawn on Entry

```
When: Player enters step "boss_room"
Then: Spawn MythicMob "DungeonBoss" at location (100, 65, 100)
```

#### Progressive Waves

```
When: All mobs dead in "arena"
Then: If wave < 5:
        Increase wave by 1
        Wait 5 seconds
        Spawn 10 MythicMobs "DungeonZombie" in arena
      Else:
        Complete step
        Grant rewards
```

#### Boss Phase Transition

```
When: Mob "BossName" health below 50%
Then: Display message "Boss enters phase 2!"
      Spawn 5 MythicMobs "Minion" around boss
      Close doors
```

#### Secret Area Unlock

```
When: Player has item "Secret Key"
And:  Player enters region "secret_door"
Then: Remove blocks to open passage
      Display message "Secret area unlocked!"
      Remove item "Secret Key" from player
```

## Stopping the Edit Session

### Method 1: Using Web Editor

When you're done editing logic:
```
/nd admin webeditor stop
```

### Method 2: Leave the Server

Simply disconnect from the edit server. The instance will automatically clean up.

### Saving Changes

**World changes** are automatically saved by the instance provider.

**Logic changes** must be saved in the web editor before stopping.

> **Important:** Always save in the web editor before stopping it!

## Best Practices

### Planning
* **Design on paper first** - Sketch layouts and mechanics
* **Test iteratively** - Make small changes and test frequently
* **Document coordinates** - Keep notes of important locations
* **Version control** - Backup working configurations

### Building
* **Start simple** - Get basic structure working first
* **Think about flow** - Ensure smooth player progression
* **Add variety** - Mix combat, puzzles, and exploration
* **Consider party size** - Design for your target group size

### Logic Configuration
* **Keep it simple** - Start with basic triggers, add complexity gradually
* **Test each mechanic** - Verify triggers work before moving on
* **Clear feedback** - Use messages to indicate progress
* **Fail safely** - Handle edge cases (what if boss dies instantly?)

### Testing
* **Solo test first** - Walk through alone to check basics
* **Party test** - Verify with a group if party-based
* **Edge cases** - Try breaking things intentionally
* **Performance test** - Monitor TPS during complex encounters

## Troubleshooting

### Can't Enter Edit Mode

**Symptoms:** Command fails or doesn't teleport

**Solutions:**
1. Verify dungeon and floor IDs are correct
2. Check you have admin permissions
3. Ensure instance provider is working (CloudNet/ASP)
4. Check console for error messages
5. Verify dungeon is properly configured

### Web Editor Won't Load

**Symptoms:** URL doesn't open or times out

**Solutions:**
1. Check firewall allows port 7734
2. Verify `webeditor.proxy-port` in config
3. Try accessing from the same network as server
4. Check console for web editor errors
5. Ensure no other service is using the port

### Changes Not Saving

**Symptoms:** Logic changes disappear after reload

**Solutions:**
1. Click "Save" button in web editor before stopping
2. Wait for confirmation message
3. Verify no errors in browser console (F12)
4. Check server console for save errors
5. Ensure proper file permissions on server

### Triggers Not Working

**Symptoms:** Configured events don't fire

**Solutions:**
1. Verify trigger block is connected properly in Blockly
2. Check region coordinates are correct
3. Ensure MythicMobs names match exactly (case-sensitive)
4. Test with simple actions first (messages)
5. Check server console for trigger errors

### Mobs Not Spawning

**Symptoms:** Spawn commands in editor don't work

**Solutions:**
1. Verify MythicMobs is installed and loaded
2. Check mob names match MythicMobs config exactly
3. Ensure spawn coordinates are valid (not in walls)
4. Test manual spawn: `/mm mobs spawn <name> 1`
5. Check for conflicting plugins blocking spawns

## Advanced Topics

### Custom Variables

Use variables to create complex mechanics:
* Player-specific counters
* Global dungeon state
* Time tracking
* Score systems

### Procedural Content

Generate random elements:
* Random mob spawns
* Varied boss abilities
* Different path layouts
* Dynamic reward pools

### Multi-Step Quests

Create objectives spanning multiple steps:
* Collect items across floors
* Defeat specific bosses in order
* Unlock areas with keys
* Progressive difficulty

### Integration with Other Plugins

Combine with:
* **MMOCore** - Grant XP and skill points
* **MMOItems** - Drop custom items
* **MythicMobs** - Advanced boss mechanics
* **Economy plugins** - Monetary rewards

## Video Tutorial

Below is a video demonstration of the editing workflow:

{% embed url="https://files.gitbook.com/v0/b/gitbook-x-prod.appspot.com/o/spaces%2FcASiemNrQ83eXJNetWWR%2Fuploads%2FtvVu5C448VplHIAwzmPZ%2FNextDungeon_Editor.mp4?alt=media&token=24f5abfc-27b3-4d97-b2b8-624f788ccc25" %}

## Additional Resources

* [Creating Dungeons](creating-dungeons.md) - Start from scratch
* [Dungeon Config](../configuration/dungeon-config.md) - Configuration reference
* [MythicMobs Integration](../integrations/mythicmobs.md) - Custom mob setup
* [CloudNet Integration](../integrations/cloudnet.md) - Instance management

***

With the web editor and world editing tools, you can create complex, engaging dungeon experiences!


