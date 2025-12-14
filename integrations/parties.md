---
description: Learn how to integrate NextDungeon with Parties plugin for group dungeon experiences.
icon: user-group
---

# Parties

The Parties plugin integration enables group-based dungeon gameplay, allowing players to team up, share progress, and tackle dungeons together. NextDungeon seamlessly integrates with the popular Parties plugin to provide coordinated party experiences.

## What is Parties?

[Parties](https://www.spigotmc.org/resources/parties.3709/) is a comprehensive party management plugin for Minecraft that allows players to:
* Create and manage player groups
* Chat within party channels
* Share experience and loot
* Teleport together
* Coordinate gameplay activities

### Benefits for NextDungeon

* **Coordinated Entry**: Entire party enters dungeons together
* **Shared Progress**: All party members progress through floors simultaneously
* **Group Requirements**: Enforce minimum/maximum party sizes
* **Party-Wide Rewards**: Distribute rewards to all members
* **Coordinated Teleportation**: All members transfer to instances together
* **Party-Based Cooldowns**: Manage retry timers per party

## Requirements

### Plugin Dependencies
* **Parties** plugin version 3.x or newer
* NextDungeon with Parties support enabled

### Configuration
* Parties plugin properly installed and configured
* Party size requirements set in dungeon configurations

## Installation

### Step 1: Install Parties Plugin

1. **Download Parties** from [SpigotMC](https://www.spigotmc.org/resources/parties.3709/)
2. **Place the JAR** in your `plugins` folder
3. **Restart your server** to generate configuration
4. **Configure Parties** according to your preferences

### Step 2: Configure Parties

Edit `plugins/Parties/config.yml` for basic setup:

```yaml
parties:
  general:
    # Enable party features
    enable: true
    
  join:
    # Require party invitation to join
    require-invite: true
    
  experience:
    # Share XP among party members
    enable: true
    share-percentage: 80
```

### Step 3: Verify NextDungeon Integration

NextDungeon automatically detects and integrates with Parties. Verify by:

1. **Start your server**
2. **Check console** for Parties detection message
3. **Test party creation**:
   ```
   /party create
   /party invite <player>
   ```

## How It Works

### Party Dungeon Entry

1. **Party Formation**
   * Party leader or member initiates dungeon entry
   * NextDungeon checks party size requirements

2. **Requirements Check**
   * Minimum party size met
   * Maximum party size not exceeded
   * All members meet individual requirements (level, items, cooldowns)

3. **Instance Creation**
   * Dungeon instance created for the party
   * All party members teleported together
   * Party structure maintained in dungeon

4. **Shared Progression**
   * Party progresses through steps together
   * Objectives completed as a group
   * Rewards distributed to all members

5. **Exit Handling**
   * Party can leave together
   * Or handle individual disconnects/deaths

### Party Size Requirements

Configure in your dungeon configuration:

```yaml
dungeon:
  floors:
    - id: "floor1"
      requirements:
        party:
          min_size: 2    # Minimum 2 players required
          max_size: 5    # Maximum 5 players allowed
```

**Solo Play:**
Set `min_size: 1` to allow solo entry.

**Raid-Style:**
Set higher limits like `min_size: 10, max_size: 25` for large group content.

## Configuration Options

### Dungeon-Level Party Requirements

Each dungeon floor can have different party requirements:

```yaml
dungeon:
  id: "party_dungeon"
  floors:
    - id: "easy_floor"
      name: "Training Grounds"
      requirements:
        party:
          min_size: 1    # Solo or party
          max_size: 10   # Up to 10 players
    
    - id: "hard_floor"
      name: "Challenge Mode"
      requirements:
        party:
          min_size: 3    # Requires party
          max_size: 5    # Small group
    
    - id: "raid_floor"
      name: "Epic Raid"
      requirements:
        party:
          min_size: 10   # Large group required
          max_size: 25   # Full raid size
```

### Party-Specific Rules

#### All Members Must Meet Requirements

Each party member must individually satisfy:
* Minimum level requirements
* Required items in inventory
* Not on cooldown for that dungeon
* Have completed prerequisite floors

Example configuration:
```yaml
requirements:
  minimum_level: 10          # All members must be level 10+
  required_floor: ["floor1"] # All must have completed floor1
  retry_cooldown: "30m"      # Individual cooldown per player
  required_items:
    - "Dungeon Key"          # Each member needs the key
```

## Party Commands Integration

### Basic Party Commands

Players use standard Parties commands:

```
/party create              # Create a party
/party invite <player>     # Invite a player
/party accept              # Accept invitation
/party leave               # Leave party
/party info                # View party info
/party chat <message>      # Party chat
```

### Dungeon-Specific Usage

```
# Party leader initiates dungeon entry
/dungeon enter <dungeon_id>

# All party members are:
# 1. Checked for requirements
# 2. Teleported together if all pass
# 3. Entered into the same instance
```

## Party Management in Dungeons

### During Dungeon Run

#### Party Chat
Party members can communicate using party chat:
```
/party chat Hello team!
# Or use prefix
@p Ready for boss fight?
```

#### Party Member Tracking
* View party member locations
* See member health/status (if configured)
* Coordinate positioning

#### Shared Objectives
* Step progression affects all members
* Boss defeats count for entire party
* Loot can be configured for party-wide drops

### Death and Revival

With the NextDungeon revival system:

```yaml
ReviveSystem:
  ReviveItem:
    type: "BEETROOT_SOUP"
    displayName: "&c&lRevive Item"
  ghostDuration: 15  # Seconds teammates have to revive
```

**Party members can revive each other:**
1. Player dies, enters ghost mode
2. Teammates have 15 seconds (configurable)
3. Use revive item near ghost to resurrect
4. Prevents life consumption

### Party Disbanding

**If party disbands during dungeon:**
* Configuration option determines behavior:
  * Kick all players from dungeon
  * Allow players to continue solo
  * End dungeon instance
  
**If party leader leaves:**
* Leadership can transfer to another member
* Or party continues with remaining members
* Configured via Parties plugin settings

## Troubleshooting

### Party Can't Enter Dungeon

**Symptoms:** "Party size requirements not met" or similar error

**Solutions:**
1. Check party size meets min/max requirements:
   ```
   /party info
   ```
2. Verify all members are online and in the party
3. Check that all members meet individual requirements:
   * Level requirements
   * Required items
   * Completed prerequisite floors
   * Not on cooldown
4. Ensure all members are in the same world/server (if multi-server)

### Some Party Members Not Teleported

**Symptoms:** Only some members enter dungeon

**Solutions:**
1. Verify all members meet requirements
2. Check console for specific rejection reasons
3. Ensure members are not in restricted areas
4. Verify members have teleport permissions
5. Check for conflicting plugins preventing teleport

### Party Requirements Not Enforced

**Symptoms:** Can enter with wrong party size

**Solutions:**
1. Verify dungeon configuration has party requirements set:
   ```yaml
   requirements:
     party:
       min_size: 2
       max_size: 5
   ```
2. Ensure Parties plugin is loaded (check `/plugins`)
3. Verify NextDungeon detected Parties (check console logs)
4. Restart server if integration not detected

### Cooldowns Not Working Correctly

**Symptoms:** Can retry too soon or cooldown incorrect

**Solutions:**
1. Cooldowns are per-player, not per-party
2. Check individual player cooldowns
3. Verify `retry_cooldown` is configured:
   ```yaml
   requirements:
     retry_cooldown: "15m"
   ```
4. Check database for stored cooldown data

## Advanced Features

### Party-Based Scaling

**Dynamic Difficulty:**
Adjust dungeon difficulty based on party size (via web editor or custom logic):

* More players = more/stronger mobs
* Fewer players = reduced challenge
* Scale rewards accordingly

### Role-Based Requirements

Using MMOCore integration with Parties:

```yaml
# Example: Require specific class distribution
# This would be configured via custom logic/web editor
requirements:
  party_composition:
    - min_tanks: 1
    - min_healers: 1
    - min_dps: 2
```

### Party Rewards Distribution

Configure how rewards are distributed:

**Options (via custom configuration or web editor):**
* **Equal Split**: Divide rewards equally
* **Contribution-Based**: Based on damage/healing done
* **Random Distribution**: Random member gets specific items
* **All Receive**: Everyone gets the same rewards

## Best Practices

### Dungeon Design
* **Balance for Groups**: Design encounters for coordinated play
* **Require Roles**: Encourage diverse party composition
* **Communication**: Design puzzles requiring teamwork
* **Fair Scaling**: Don't make solo impossible if allowed

### Configuration
* **Clear Requirements**: Make party size requirements obvious
* **Reasonable Sizes**: Don't require unrealistic party sizes
* **Flexible Entry**: Allow some flexibility in party composition
* **Test Thoroughly**: Test with minimum and maximum party sizes

### Party Management
* **Active Leadership**: Encourage active party leaders
* **Communication**: Promote party chat usage
* **Fair Loot**: Implement fair reward distribution
* **Handle Disconnects**: Plan for player disconnections

### Player Experience
* **Matchmaking**: Consider implementing party finder systems
* **Clear Messaging**: Show requirements before entry attempt
* **Grace Periods**: Allow time for party members to reconnect
* **Fair Cooldowns**: Don't punish entire party for one member

## Integration Examples

### Small Group Dungeon (2-4 Players)

```yaml
dungeon:
  id: "crypt_dungeon"
  floors:
    - id: "main_floor"
      requirements:
        party:
          min_size: 2
          max_size: 4
        minimum_level: 5
        retry_cooldown: "30m"
      rules:
        death_ban: "15m"
```

### Solo or Party Dungeon (1-5 Players)

```yaml
dungeon:
  id: "flexible_dungeon"
  floors:
    - id: "main_floor"
      requirements:
        party:
          min_size: 1    # Allow solo
          max_size: 5
        minimum_level: 1
        retry_cooldown: "10m"
```

### Raid Dungeon (10-20 Players)

```yaml
dungeon:
  id: "epic_raid"
  floors:
    - id: "raid_floor"
      requirements:
        party:
          min_size: 10
          max_size: 20
        minimum_level: 50
        retry_cooldown: "24h"
      rules:
        death_ban: "1h"
```

## Party Plugin Alternatives

While NextDungeon is designed for the Parties plugin, it may work with alternatives:

* **McMMO Parties**: Basic party system
* **Custom Party Systems**: Depending on implementation
* **Guild Plugins**: Some guild plugins with party features

> **Note:** Full compatibility is only guaranteed with the official Parties plugin. Other systems may have limited functionality.

## Additional Resources

* [Parties Plugin Documentation](https://alessiodp.com/docs/parties/)
* [Parties Wiki](https://github.com/AlessioDP/Parties/wiki)
* [Parties Discord](https://discord.alessiodp.com/)
* [SpigotMC Page](https://www.spigotmc.org/resources/parties.3709/)

## Migration and Compatibility

### From Non-Party Dungeons

If adding party support to existing dungeons:

1. **Update configurations** with party requirements
2. **Test party entry** with various sizes
3. **Adjust difficulty** if needed for groups
4. **Update documentation** for players
5. **Announce changes** to community

### With Other Plugins

Parties integration works alongside:
* **MMOCore**: Level and class requirements
* **MythicMobs**: Custom party-scaled mob encounters
* **CloudNet/ASP**: Instance management for parties
* **Economy Plugins**: Shared costs or rewards

***

Party integration brings cooperative gameplay to your dungeons!

