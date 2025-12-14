---
description: Learn how to integrate NextDungeon with MMOCore for RPG elements and progression systems.
icon: dragon
---

# MMOCore

MMOCore integration brings comprehensive RPG mechanics to your dungeons, including classes, skills, level requirements, experience rewards, and stat-based gameplay. This integration transforms dungeons into true RPG experiences.

## What is MMOCore?

[MMOCore](https://www.spigotmc.org/resources/mmocore.87699/) is a complete RPG plugin for Minecraft that provides:
* **Class System**: Multiple playable classes with unique abilities
* **Skill Trees**: Customizable progression paths
* **Attributes**: Strength, dexterity, intelligence, and more
* **Experience System**: Level-based progression
* **Quest System**: Story-driven content
* **Custom Resources**: Mana, stamina, and other resource systems

### Benefits for NextDungeon

* **Level Requirements**: Restrict dungeons by player level
* **Class-Based Challenges**: Design dungeons for specific roles
* **RPG Progression**: Reward dungeon completion with experience and skills
* **Stat Scaling**: Dungeon difficulty adapts to player stats
* **Skill Usage**: Players use class abilities in dungeons
* **Attribute Checks**: Gate content behind stat requirements

## Requirements

### Plugin Dependencies
* **MMOCore** version 1.10.0 or newer
* **MMOItems** (recommended for custom dungeon items)
* **MythicLib** (required by MMOCore)

### NextDungeon Support
* MMOCore integration is built-in to NextDungeon
* Automatic detection when MMOCore is present

## Installation

### Step 1: Install MMOCore

1. **Install MythicLib** (required dependency)
   * Download from [SpigotMC](https://www.spigotmc.org/resources/mythiclib.90306/)
   * Place in `plugins` folder

2. **Download MMOCore** from [SpigotMC](https://www.spigotmc.org/resources/mmocore.87699/)
3. **Place the JAR** in your `plugins` folder
4. **Restart your server** to generate default configuration

### Step 2: Configure MMOCore Classes

Set up your class system in `plugins/MMOCore/classes/`:

Example class configuration:
```yaml
# warrior.yml
display:
  name: "Warrior"
  lore:
    - "A strong melee fighter"
  icon: IRON_SWORD

attributes:
  max-health: 20
  max-mana: 20
  
stats:
  health: 
    base: 20
    per-level: 2
  
skills:
  - skill: "POWER_STRIKE"
    unlock-level: 1
  - skill: "CHARGE"
    unlock-level: 5
```

### Step 3: Configure Dungeon Level Requirements

Edit your dungeon configurations to include level requirements:

```yaml
dungeon:
  id: "rpg_dungeon"
  floors:
    - id: "beginner"
      requirements:
        minimum_level: 1      # MMOCore level requirement
        party:
          min_size: 1
          max_size: 5
    
    - id: "intermediate"
      requirements:
        minimum_level: 10     # Higher level requirement
        required_floor: ["rpg_dungeon_beginner"]
    
    - id: "expert"
      requirements:
        minimum_level: 25     # Expert level
        required_floor: ["rpg_dungeon_intermediate"]
```

### Step 4: Verify Integration

1. **Check MMOCore is loaded**: `/mmocore`
2. **Verify player levels**: `/mmocore level`
3. **Test dungeon entry** with level requirements
4. **Check console** for MMOCore detection message

## How It Works

### Level-Based Requirements

When a player attempts to enter a dungeon:

1. **Level Check**
   * NextDungeon queries player's MMOCore level
   * Compares to `minimum_level` requirement
   * Denies entry if below requirement

2. **Party Level Checks**
   * Each party member checked individually
   * All members must meet minimum level
   * Entry denied if any member under-leveled

3. **Entry Granted**
   * All requirements met
   * Players enter dungeon
   * Class abilities available during dungeon

### Experience Rewards

Configure experience rewards for dungeon completion:

**Via Web Editor or Custom Scripts:**
* Grant MMOCore experience on boss kills
* Reward experience for completing steps
* Bonus experience for full dungeon completion
* Party-wide experience distribution

### Class-Based Gameplay

**In Dungeons:**
* Players use their class skills and abilities
* Mana/stamina regenerate normally
* Cooldowns apply as configured
* Class stats affect combat

## Configuration Options

### Level Requirements

#### Basic Level Requirement
```yaml
requirements:
  minimum_level: 10    # Player must be at least level 10
```

#### Tiered Progression
```yaml
dungeon:
  floors:
    - id: "floor1"
      requirements:
        minimum_level: 1     # Beginner
    - id: "floor2"
      requirements:
        minimum_level: 5     # Intermediate
    - id: "floor3"
      requirements:
        minimum_level: 10    # Advanced
    - id: "floor4"
      requirements:
        minimum_level: 20    # Expert
```

### Class-Specific Requirements

Using custom logic or web editor, you can implement:

```yaml
# Example of desired behavior (implementation via web editor)
requirements:
  allowed_classes:
    - "Warrior"
    - "Paladin"
  forbidden_classes:
    - "Mage"
```

### Attribute Requirements

Check player stats before entry:

```yaml
# Via web editor or custom triggers
requirements:
  minimum_attributes:
    strength: 15
    vitality: 10
```

## MMOCore Features in Dungeons

### Experience Gain

**Configure via triggers/web editor:**

```javascript
// Example: Grant 500 XP on boss kill
player.giveExperience(500);

// Example: Grant XP per step completion
party.members.forEach(p => p.giveExperience(100));
```

### Skill Usage

Players can use their MMOCore skills in dungeons:

* Cast spells and abilities
* Consume mana/stamina as normal
* Skill cooldowns apply
* Passive abilities remain active

**Important Considerations:**
* Some skills may need restrictions (teleports, escapes)
* Balance dungeon difficulty around skill availability
* Consider cooldown lengths for dungeon duration

### Stat Scaling

Player stats affect dungeon performance:

* **Strength**: Melee damage in combat
* **Dexterity**: Attack speed, critical chance
* **Intelligence**: Magic damage, mana pool
* **Vitality**: Health pool, survivability
* **Defense**: Damage reduction

### Resource Management

* **Mana**: Required for spell casting
* **Stamina**: Used by physical skills
* **Cooldowns**: Skill reuse timers

Configure regeneration or provide resource restore mechanics.

## Advanced Integration

### Class-Balanced Dungeons

Design dungeons requiring class diversity:

#### Tank-Required Dungeons
* High damage enemies requiring tanking
* Taunt mechanics using MMOCore skills
* Damage mitigation essential

#### Healer-Required Dungeons
* High incoming damage
* No natural regeneration
* Healing abilities crucial

#### DPS-Focused Dungeons
* Enrage timers
* High health enemies
* Damage requirements

### Dynamic Difficulty Scaling

Scale based on party composition and levels:

```javascript
// Pseudo-code example via web editor
let partyAverageLevel = calculateAverageLevel(party);
let mobDifficulty = baseDifficulty * (partyAverageLevel / 10);

spawnMobs(mobDifficulty);
```

### Skill-Based Puzzles

Create puzzles requiring specific class skills:

* **Mage**: Unlock magical barriers
* **Warrior**: Break through physical obstacles
* **Rogue**: Detect and disarm traps
* **Healer**: Purify corrupted areas

## Reward Systems

### Experience Rewards

Structure experience rewards:

```yaml
# Via web editor configuration
rewards:
  step_completion: 50    # XP per step
  boss_kill: 500         # XP for boss
  floor_complete: 1000   # XP for full floor
  time_bonus: 200        # Bonus for fast completion
```

### Skill Points

Grant skill points as rare rewards:

```javascript
// Via custom trigger
player.giveSkillPoints(1);
```

### Class Items

Use MMOItems integration for class-specific loot:

```yaml
# Drop class-appropriate items
warrior_weapon:
  type: "IRON_SWORD"
  class-requirement: "Warrior"
  
mage_staff:
  type: "STICK"
  class-requirement: "Mage"
```

### Attribute Scrolls

Reward permanent stat increases:

```javascript
// Grant attribute points
player.giveAttributePoint("strength", 1);
```

## Troubleshooting

### Level Requirement Not Working

**Symptoms:** Players can enter without meeting level requirement

**Solutions:**
1. Verify MMOCore is loaded: `/plugins`
2. Check player actual level: `/mmocore level <player>`
3. Confirm dungeon config has `minimum_level` set:
   ```yaml
   requirements:
     minimum_level: 10
   ```
4. Check console for MMOCore integration errors
5. Restart server if integration not detected

### Skills Not Working in Dungeon

**Symptoms:** Can't use MMOCore skills inside dungeon

**Solutions:**
1. Check gamemode is correct (skills disabled in Creative):
   ```yaml
   rules:
     gamemode: "SURVIVAL"  # Or ADVENTURE
   ```
2. Verify skill permissions not revoked
3. Check mana/stamina availability
4. Ensure no conflicting plugins blocking skills
5. Test skills outside dungeon to verify they work

### Experience Not Awarded

**Symptoms:** Players don't receive XP for dungeon completion

**Solutions:**
1. Verify experience grant triggers are configured
2. Check web editor actions for XP rewards
3. Test manual XP grant: `/mmocore admin experience give <player> <amount>`
4. Review console for errors during reward distribution
5. Ensure players are still in dungeon when rewards given

### Class Restrictions Not Applied

**Symptoms:** Wrong classes can enter restricted dungeons

**Solutions:**
1. Verify custom class checks are implemented (via web editor)
2. Class restrictions require custom logic - not built-in
3. Check trigger execution on dungeon entry
4. Test class detection: check player's current class
5. Review web editor configuration for class checks

## Best Practices

### Level Design
* **Progressive Difficulty**: Match dungeon difficulty to level requirements
* **Clear Scaling**: Level 20 dungeon should feel significantly harder than level 10
* **Fair Requirements**: Don't gate too much content behind high levels
* **Alternative Paths**: Offer dungeons at various level ranges

### Balance
* **Test with Classes**: Test each class can complete the dungeon
* **Skill Accessibility**: Ensure required skills are learnable before dungeon level
* **Resource Management**: Balance mana/stamina consumption vs regeneration
* **Time Limits**: Account for skill cooldowns in timer challenges

### Rewards
* **Appropriate XP**: Don't trivialize leveling with excessive XP
* **Scaling Rewards**: Higher level dungeons = better rewards
* **Class-Appropriate**: Drop items suitable for classes present
* **Fair Distribution**: Ensure party members equally rewarded

### Design Philosophy
* **Encourage Parties**: Design for mixed class parties
* **Unique Roles**: Give each class something valuable to do
* **Skill Expression**: Allow skilled play to matter
* **Progression Feel**: Make players feel stronger as they level

## Integration Examples

### Beginner Dungeon (Level 1-10)

```yaml
dungeon:
  id: "training_grounds"
  floors:
    - id: "tutorial"
      name: "Training Grounds"
      requirements:
        minimum_level: 1
        party:
          min_size: 1
          max_size: 5
      # Simple mechanics, low difficulty
      # Teaches basic dungeon concepts
```

### Mid-Level Dungeon (Level 15-25)

```yaml
dungeon:
  id: "cursed_temple"
  floors:
    - id: "entrance"
      requirements:
        minimum_level: 15
        party:
          min_size: 2
          max_size: 5
      # Requires basic skill usage
      # Encourages party play
```

### End-Game Raid (Level 40+)

```yaml
dungeon:
  id: "demon_fortress"
  floors:
    - id: "main_raid"
      requirements:
        minimum_level: 40
        party:
          min_size: 5
          max_size: 10
      # Complex mechanics
      # Requires coordinated skill usage
      # High-level class abilities essential
```

## Class-Specific Strategies

### Warrior
* **High survivability** for frontline combat
* **Taunt abilities** to protect allies
* **AoE damage** for mob groups
* **Tank role** in party composition

### Mage
* **High magic damage** for boss DPS
* **Crowd control** to manage adds
* **AoE clearing** for mob waves
* **Glass cannon** requiring protection

### Rogue
* **High single-target damage**
* **Mobility** for mechanic execution
* **Trap detection** (if implemented)
* **Evasion** for survival

### Healer/Cleric
* **Party sustain** through healing
* **Support buffs** for allies
* **Resurrection** for downed players
* **Essential for difficult content**

## Additional Resources

* [MMOCore Documentation](https://gitlab.com/phoenix-devt/mmocore/-/wikis/home)
* [MMOCore Discord](https://discord.gg/Mk7CgAJ)
* [MMOItems Wiki](https://gitlab.com/phoenix-devt/mmoitems/-/wikis/home)
* [Class Configuration Guide](https://gitlab.com/phoenix-devt/mmocore/-/wikis/Class-Configuration)

## Advanced Topics

### Custom XP Scaling

Implement diminishing returns for over-leveled players:

```javascript
// Pseudo-code
let levelDifference = player.level - dungeon.recommendedLevel;
let xpMultiplier = Math.max(0.1, 1 - (levelDifference * 0.1));
let finalXP = baseXP * xpMultiplier;
```

### Skill Unlock Quests

Gate dungeon access behind skill unlocks:

```yaml
# Require specific skill to be learned
requirements:
  required_skills:
    - "FIREBALL"
    - "HEAL"
```

### Stat Check Mechanics

Create challenges requiring minimum stats:

```javascript
// Strength check to break door
if (player.getAttribute("STRENGTH") >= 20) {
  breakDoor();
} else {
  sendMessage("You're not strong enough!");
}
```

***

MMOCore integration creates deep RPG experiences in your dungeons!

