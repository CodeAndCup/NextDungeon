---
description: Learn how to integrate NextDungeon with MythicMobs for custom bosses and challenging encounters.
icon: skull
---

# MythicMobs

MythicMobs integration allows you to populate your dungeons with custom-designed monsters, bosses, and challenging encounters. Create unique enemies with special abilities, mechanics, and loot tables specifically tailored for your dungeon experiences.

## What is MythicMobs?

[MythicMobs](https://www.spigotmc.org/resources/mythicmobs.5702/) is the premier custom mob plugin for Minecraft, providing:
* **Custom Mobs**: Create unique enemies with custom stats and behaviors
* **Boss Mechanics**: Complex attack patterns and phases
* **Custom Skills**: Hundreds of abilities and effects
* **AI Customization**: Control mob behavior and tactics
* **Loot Tables**: Define custom drops and rewards
* **Particle Effects**: Visual flair for abilities and attacks

### Benefits for NextDungeon

* **Unique Bosses**: Create memorable boss encounters
* **Progressive Difficulty**: Scale mob power by dungeon floor
* **Custom Mechanics**: Design complex encounter patterns
* **Themed Enemies**: Match mobs to dungeon aesthetics
* **Coordinated Spawns**: Trigger mob spawns based on dungeon events
* **Balanced Loot**: Control rewards from dungeon enemies

## Requirements

### Plugin Dependencies
* **MythicMobs** version 5.0.0 or newer (5.x recommended)
* **MMOCore** (optional, for enhanced integration)
* **MMOItems** (optional, for custom loot integration)

### NextDungeon Support
* MythicMobs integration is built-in
* Automatic detection when MythicMobs is present
* Compatible with web editor for spawn triggers

## Installation

### Step 1: Install MythicMobs

1. **Download MythicMobs** from [SpigotMC](https://www.spigotmc.org/resources/mythicmobs.5702/)
2. **Place the JAR** in your `plugins` folder
3. **Restart your server** to generate default configuration
4. **Verify installation**: `/mm help`

### Step 2: Create Custom Mobs

Navigate to `plugins/MythicMobs/Mobs/` and create mob configuration files.

#### Example: Dungeon Guardian Boss

Create `dungeon_guardian.yml`:

```yaml
DungeonGuardian:
  Type: ZOMBIE
  Display: '&4&lDungeon Guardian'
  Health: 500
  Damage: 15
  
  Skills:
  - skill{s=GuardianSlam} @target ~onTimer:100
  - skill{s=SummonMinions} @self ~onTimer:200
  - skill{s=Enrage} @self ~onHealthMod:<0.25
  
  Drops:
  - DungeonKey 1 1.0
  - diamond 5-10 0.5
  - experience 500 1.0
  
  Options:
    MovementSpeed: 0.3
    FollowRange: 32
    PreventItemPickup: true
    AlwaysShowName: true
```

#### Example: Trash Mob

Create `dungeon_zombie.yml`:

```yaml
DungeonZombie:
  Type: ZOMBIE
  Display: '&cCorrupted Zombie'
  Health: 50
  Damage: 5
  
  Skills:
  - skill{s=PoisonAttack} @target ~onAttack >0.3
  
  Drops:
  - rotten_flesh 1-3 0.8
  - experience 20 1.0
  
  Options:
    MovementSpeed: 0.25
    PreventItemPickup: true
```

### Step 3: Create Custom Skills

Navigate to `plugins/MythicMobs/Skills/` and create skill files.

#### Example Skills

Create `boss_skills.yml`:

```yaml
GuardianSlam:
  Skills:
  - effect:particles{particle=EXPLOSION_LARGE;amount=20;speed=0.1} @self
  - damage{amount=20;ignoreArmor=false} @PlayersInRadius{r=5}
  - throw{velocity=2;velocityY=1} @PlayersInRadius{r=5}
  - sound{s=ENTITY_GENERIC_EXPLODE;volume=2;pitch=0.5} @self

SummonMinions:
  Skills:
  - summon{type=DungeonZombie;amount=3;radius=5} @self
  - message{m="&cThe Guardian summons reinforcements!"} @PlayersInRadius{r=30}

Enrage:
  Skills:
  - modifyDamage{multiplier=2.0} @self
  - modifySpeed{multiplier=1.5} @self
  - effect:particles{particle=ANGRY_VILLAGER;amount=100;hS=1;vS=1;speed=0} @self
  - message{m="&4&lThe Guardian becomes enraged!"} @PlayersInRadius{r=50}

PoisonAttack:
  Skills:
  - potion{type=POISON;duration=100;level=1} @target
  - effect:particles{particle=SLIME;amount=10} @target
```

### Step 4: Configure Mob Spawns in Dungeons

Use the NextDungeon web editor to configure mob spawns:

1. **Enter edit mode**:
   ```
   /dungeon admin edit <dungeon> <floor>
   ```

2. **Start web editor**:
   ```
   /dungeon admin webeditor start
   ```

3. **Configure spawn triggers**:
   * Step entry → spawn mobs
   * Region activation → spawn boss
   * Timer events → spawn waves
   * Boss death → spawn loot chest

### Step 5: Test Your Mobs

Test mobs before adding to dungeons:

```
# Spawn a mob for testing
/mm mobs spawn DungeonGuardian 1

# Test skills
/mm test cast GuardianSlam

# Check mob stats
/mm mobs info DungeonGuardian
```

## How It Works

### Mob Spawning in Dungeons

1. **Player Enters Step**
   * Trigger activates via web editor
   * MythicMobs spawns configured mobs
   * Mobs engage players

2. **Boss Encounters**
   * Player enters boss room step
   * Boss spawns at designated location
   * Boss mechanics activate
   * Rewards given on defeat

3. **Wave Spawns**
   * Timer-based spawning
   * Objective-based spawning
   * Progressive difficulty waves

### Custom Mechanics

**Using Web Editor:**
* Configure spawn locations
* Set spawn conditions
* Define mob quantities
* Link to dungeon progression
* Trigger rewards on mob death

## Configuration Examples

### Basic Trash Mob Spawns

**In Web Editor:**
```javascript
// When players enter step "corridor"
onStepEnter("corridor", function() {
  spawnMythicMob("DungeonZombie", location1, 5);  // 5 zombies
  spawnMythicMob("DungeonSkeleton", location2, 3); // 3 skeletons
});
```

### Boss Encounter

**In Web Editor:**
```javascript
// When players enter boss room
onStepEnter("boss_room", function() {
  // Close doors
  closeDoors();
  
  // Spawn boss
  boss = spawnMythicMob("DungeonGuardian", bossSpawnLocation, 1);
  
  // Open doors when boss dies
  onMobDeath(boss, function() {
    openDoors();
    grantRewards();
    completeStep();
  });
});
```

### Progressive Waves

**In Web Editor:**
```javascript
// Survival arena with waves
onStepEnter("arena", function() {
  wave = 1;
  
  function spawnWave() {
    let mobCount = 5 + (wave * 2);  // Increasing difficulty
    spawnMythicMob("DungeonZombie", arenaCenter, mobCount);
    
    onAllMobsDead(function() {
      wave++;
      if (wave <= 5) {
        // Wait 10 seconds, spawn next wave
        scheduleTask(spawnWave, 200);  // 200 ticks = 10 seconds
      } else {
        // All waves complete
        completeStep();
      }
    });
  }
  
  spawnWave();
});
```

### Boss Phases

**In MythicMobs Config:**
```yaml
PhasedBoss:
  Type: WITHER_SKELETON
  Display: '&5&lPhased Boss'
  Health: 1000
  Damage: 20
  
  Skills:
  # Phase 1: 100% - 66% health
  - skill{s=Phase1Attack} @target ~onTimer:40 ~onHealthMod:>0.66
  
  # Phase 2: 66% - 33% health
  - skill{s=Phase2Attack} @target ~onTimer:30 ~onHealthMod:0.33to0.66
  - skill{s=SummonAdds} @self ~onHealthMod:0.66
  
  # Phase 3: Below 33% health
  - skill{s=Phase3Enrage} @target ~onTimer:20 ~onHealthMod:<0.33
  - skill{s=DespawnAdds} @MobsInRadius{r=50;t=DungeonZombie} ~onHealthMod:0.33
```

## Advanced Features

### Mob Scaling

Scale mob stats based on party size or dungeon difficulty:

```yaml
# Different mob variants for different difficulties
EasyDungeonBoss:
  Type: ZOMBIE
  Health: 200
  Damage: 8

NormalDungeonBoss:
  Type: ZOMBIE
  Health: 500
  Damage: 15

HardDungeonBoss:
  Type: ZOMBIE
  Health: 1000
  Damage: 25
```

**Select via Web Editor:**
```javascript
if (difficulty === "easy") {
  spawnMythicMob("EasyDungeonBoss", location, 1);
} else if (difficulty === "normal") {
  spawnMythicMob("NormalDungeonBoss", location, 1);
} else {
  spawnMythicMob("HardDungeonBoss", location, 1);
}
```

### Custom Boss Loot

```yaml
DungeonBoss:
  # ... other config ...
  
  Drops:
  # Guaranteed drops
  - DungeonCompletionToken 1 1.0
  - experience 1000 1.0
  
  # Rare drops
  - LegendarySword 1 0.05
  - EpicArmor 1 0.1
  
  # Common drops
  - diamond 5-10 0.5
  - gold_ingot 10-20 0.8
  
  DropTables:
  - BossLootTable
```

Create `plugins/MythicMobs/DropTables/BossLootTable.yml`:
```yaml
BossLootTable:
  Drops:
  - DungeonKey 1 0.3
  - RareResource 1-5 0.2
  - SkillScroll 1 0.1
```

### Environmental Mechanics

Combine MythicMobs with dungeon environment:

```javascript
// Boss creates lava pools during fight
onTimer(100, function() {
  if (bossAlive) {
    location = getRandomLocation(bossRoom);
    createLavaPool(location);
    spawnMythicMob("LavaElemental", location, 2);
  }
});
```

### Coordinated Encounters

Multiple bosses working together:

```javascript
// Twin boss fight
onStepEnter("twin_boss_room", function() {
  boss1 = spawnMythicMob("FireBoss", location1, 1);
  boss2 = spawnMythicMob("IceBoss", location2, 1);
  
  // If one dies, other enrages
  onMobDeath(boss1, function() {
    boss2.cast("Enrage");
  });
  
  onMobDeath(boss2, function() {
    boss1.cast("Enrage");
  });
  
  // Both must die to proceed
  onAllMobsDead(function() {
    openDoors();
    grantRewards();
  });
});
```

## Best Practices

### Mob Design
* **Clear Telegraphs**: Make boss abilities visually obvious
* **Fair Mechanics**: Avoid one-shot kills or unavoidable damage
* **Counterplay**: Allow skilled players to dodge or mitigate attacks
* **Progressive Difficulty**: Increase complexity gradually

### Performance
* **Limit Active Mobs**: Don't spawn too many at once
* **Optimize Skills**: Avoid excessive particles or effects
* **Cleanup**: Despawn mobs when players leave
* **Test Performance**: Monitor TPS during encounters

### Balance
* **Health Pools**: Match to expected party size and DPS
* **Damage Output**: Don't one-shot players
* **Ability Frequency**: Don't spam abilities too often
* **Loot Rewards**: Make them worth the challenge

### Player Experience
* **Visual Clarity**: Use particles to show boss abilities
* **Audio Cues**: Sounds alert players to mechanics
* **Text Warnings**: Messages warn of major abilities
* **Learning Curve**: Allow practice before punishing

## Troubleshooting

### Mobs Not Spawning

**Symptoms:** No mobs appear in dungeon

**Solutions:**
1. Verify MythicMobs is loaded: `/mm help`
2. Check mob exists: `/mm mobs list`
3. Test manual spawn: `/mm mobs spawn <mobname> 1`
4. Verify web editor spawn configuration
5. Check console for errors
6. Ensure spawn location is valid (not in walls)

### Boss Mechanics Not Working

**Symptoms:** Boss doesn't use skills

**Solutions:**
1. Check skill configuration syntax
2. Test skill manually: `/mm test cast <skillname>`
3. Verify target selectors are valid
4. Check health mod triggers: `~onHealthMod:<0.5`
5. Review console for skill errors
6. Ensure boss has line of sight to targets

### Loot Not Dropping

**Symptoms:** Boss dies but no loot

**Solutions:**
1. Verify drops section in mob config
2. Check drop chances (1.0 = 100%)
3. Ensure DropTables exist if referenced
4. Test with manual spawn and kill
5. Check for conflicting loot plugins
6. Verify death event not cancelled

### Performance Issues

**Symptoms:** Lag during boss fights

**Solutions:**
1. Reduce particle effects: lower amount values
2. Limit concurrent mob count
3. Optimize skill timers: don't spam abilities
4. Check pathfinding complexity
5. Monitor TPS: `/tps`
6. Reduce FollowRange if too high

## Integration Examples

### Beginner Dungeon Mobs

```yaml
# Simple enemies for new players
TrainingDummy:
  Type: ZOMBIE
  Display: '&aTraining Dummy'
  Health: 30
  Damage: 3
  
  Skills:
  - effect:particles{particle=HEART;amount=5} @self ~onDamaged
  
  Options:
    MovementSpeed: 0.15
    Silent: true
```

### Mid-Level Elite

```yaml
CryptGuardian:
  Type: WITHER_SKELETON
  Display: '&6&lCrypt Guardian'
  Health: 300
  Damage: 12
  
  Equipment:
  - IRON_SWORD:0 HAND
  - IRON_HELMET:0 HEAD
  - IRON_CHESTPLATE:0 CHEST
  
  Skills:
  - skill{s=ShieldBash} @target ~onTimer:60
  - skill{s=Fortify} @self ~onDamaged >0.2
  
  Drops:
  - GuardianKey 1 0.5
  - experience 200 1.0
```

### End-Game Raid Boss

```yaml
DemonLord:
  Type: WITHER
  Display: '&4&k|||&c &4&lDEMON LORD &c&4&k|||'
  Health: 5000
  Damage: 30
  
  Skills:
  - skill{s=Hellfire} @PlayersInRadius{r=20} ~onTimer:100
  - skill{s=SummonDemons} @self ~onTimer:200
  - skill{s=CurseOfWeakness} @PIR{r=30} ~onTimer:150
  - skill{s=FinalPhase} @self ~onHealthMod:0.1
  
  BossBar:
    Enabled: true
    Title: '&4&lDEMON LORD'
    Color: RED
    Style: SEGMENTED_20
  
  Drops:
  - LegendaryWeapon 1 1.0
  - DemonHeart 1 1.0
  - experience 5000 1.0
  
  DropTables:
  - RaidBossTable
  - RareMaterialsTable
```

## Additional Resources

* [MythicMobs Documentation](https://git.mythiccraft.io/mythiccraft/MythicMobs/-/wikis/home)
* [Skill Mechanics List](https://git.mythiccraft.io/mythiccraft/MythicMobs/-/wikis/Skills/mechanics)
* [Target Selectors](https://git.mythiccraft.io/mythiccraft/MythicMobs/-/wikis/Skills/targeters)
* [MythicMobs Discord](https://discord.gg/mythiccraft)
* [Example Mobs](https://git.mythiccraft.io/mythiccraft/MythicMobs/-/wikis/Examples)

## Advanced Topics

### Random Boss Selection

```javascript
// Web editor: randomly select one of multiple bosses
let bosses = ["FireDragon", "IceWyrm", "StormPhoenix"];
let randomBoss = bosses[Math.floor(Math.random() * bosses.length)];
spawnMythicMob(randomBoss, location, 1);
```

### Conditional Spawns

```javascript
// Different mobs based on time or conditions
if (isNightTime()) {
  spawnMythicMob("NightmareCreature", location, 3);
} else {
  spawnMythicMob("DaylightGuardian", location, 3);
}
```

### Achievement Integration

```javascript
// Track boss kills
onMobDeath("DemonLord", function(killer) {
  grantAchievement(killer, "DEMON_SLAYER");
  broadcastMessage(killer.name + " has defeated the Demon Lord!");
});
```

### Mob Resurrection

```yaml
# Boss that resurrects once
ResurrectingBoss:
  # ... config ...
  
  Skills:
  - skill{s=Resurrect} @self ~onDeath 1
```

```yaml
Resurrect:
  Skills:
  - heal{amount=999999} @self
  - effect:particles{particle=EXPLOSION_LARGE;amount=50} @self
  - message{m="&4&lThe boss rises again!"} @PIR{r=50}
  Cooldown: 999999  # Only once per fight
```

***

MythicMobs integration brings epic boss battles to your dungeons!

