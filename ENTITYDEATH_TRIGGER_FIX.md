# EntityDeathTrigger Diagnostic and Repair Guide

## Problem Summary

**Symptom**: EntityDeathTrigger loaded (1 trigger) but not firing when zombie is killed (0 triggers found for EntityDeathEvent)

**Logs**:
```
[21:43:59] Triggers cache refresh complete: 1 triggers  ← Trigger loaded
[21:44:33] Processing event: EntityDeathEvent with 0 triggers  ← Not found!
```

## Root Cause Analysis

The trigger is loaded from database but NOT added to `triggersByEventType` cache during `refreshTriggerCache()`.

### Possible Causes (in order of likelihood):

1. **`enabled: false` in database** ← Most likely!
2. **Type mismatch**: Trigger type != "entity_death_trigger"
3. **Deserialization failure**: Trigger not properly loaded
4. **Handler not registered**: Missing EntityDeathTriggerHandler

## Diagnostic Steps

### Step 1: Enable Debug Mode

**In-game:**
```
/nd debug toggle
/nd debug setlogbroadcast BOTH
```

**Or config.yml:**
```yaml
DebugMode:
    activated: true
    logType: "BOTH"
```

### Step 2: Refresh Trigger Cache

Trigger a cache refresh by editing the floor in web editor or restarting.

### Step 3: Check Debug Output

**Look for these logs:**

```
Registered handlers:
  - EntityDeathTriggerHandler for EntityDeathEvent (supports: [entity_death_trigger])

Processing trigger: <name> (type: entity_death_trigger, class: EntityDeathTrigger)
  Checking handler: EntityDeathTriggerHandler (event: EntityDeathEvent, supports: [entity_death_trigger])
  -> Added trigger to event cache for EntityDeathEvent
```

**If you see:**
```
Skipping disabled trigger: <name>
```
→ **Problem**: Trigger is disabled in database!

**If you see:**
```
WARNING: Trigger <name> (type: <type>) was not added to any event cache!
```
→ **Problem**: Type mismatch or handler not registered

### Step 4: Check Database

**Query the database:**
```sql
SELECT triggers_data FROM floor_triggers WHERE floor_id = 'example_floor1';
```

**Check JSON structure:**
```json
[
  {
    "className": "fr.perrier.dungeons.spigot.workflow.trigger.impl.EntityDeathTrigger",
    "data": {
      "triggerId": "...",
      "name": "Kill Zombie",
      "type": "entity_death_trigger",  ← Must be exactly this
      "enabled": true,  ← MUST BE TRUE!
      "entityType": "ZOMBIE",
      "actions": [...]
    }
  }
]
```

## Fix Solutions

### Solution 1: Enable Trigger in Database (Most Common)

**If `enabled: false` or missing:**

```sql
-- Get current triggers
SELECT triggers_data FROM floor_triggers WHERE floor_id = 'example_floor1';

-- Edit the JSON to set "enabled": true
-- Then update:
UPDATE floor_triggers 
SET triggers_data = '[your updated JSON]'
WHERE floor_id = 'example_floor1';
```

### Solution 2: Correct JSON Structure

**Required structure:**
```json
[
  {
    "className": "fr.perrier.dungeons.spigot.workflow.trigger.impl.EntityDeathTrigger",
    "data": {
      "triggerId": "unique-id-here",
      "name": "Kill Zombie Trigger",
      "type": "entity_death_trigger",
      "enabled": true,
      "entityType": "ZOMBIE",
      "actions": [
        {
          "className": "fr.perrier.dungeons.spigot.workflow.action.impl.EndDungeonAction",
          "data": {
            "type": "end_dungeon_action"
          }
        }
      ]
    }
  }
]
```

### Solution 3: Create New Working Trigger

**Via Web Editor:**
1. Open floor in web editor
2. Add EntityDeathTrigger
3. Set entityType to "ZOMBIE"
4. Add EndDungeonAction
5. **Ensure trigger is ENABLED** (check toggle)
6. Save

**Via SQL (manual):**
```sql
UPDATE floor_triggers 
SET triggers_data = '[
  {
    "className": "fr.perrier.dungeons.spigot.workflow.trigger.impl.EntityDeathTrigger",
    "data": {
      "triggerId": "test-zombie-001",
      "name": "Kill Zombie Test",
      "type": "entity_death_trigger",
      "enabled": true,
      "entityType": "ZOMBIE",
      "actions": [
        {
          "className": "fr.perrier.dungeons.spigot.workflow.action.impl.EndDungeonAction",
          "data": {
            "type": "end_dungeon_action"
          }
        }
      ]
    }
  }
]'
WHERE floor_id = 'example_floor1';
```

## Verification

After applying fix:

1. **Restart server** or reload floor
2. **Enable debug** if not already
3. **Check logs** for:
   ```
   Processing trigger: Kill Zombie Test (type: entity_death_trigger, class: EntityDeathTrigger)
   -> Added trigger to event cache for EntityDeathEvent
   Cache contents - triggersByEventType: [EntityDeathEvent, ...]
   ```
4. **Kill a zombie** and verify:
   ```
   Processing event: EntityDeathEvent with 1 triggers  ← Should be 1, not 0!
   ```

## Common Mistakes

1. **`enabled: false`** ← Most common! Always check this first
2. **Wrong `type` value** ← Must be exactly "entity_death_trigger"
3. **Missing `className` wrapper** ← Serialization format requirement
4. **Wrong `floor_id`** ← Trigger saved for different floor
5. **Wrong `entityType`** ← Won't match if killing different entity

## Testing

**Quick test:**
```
/nd debug toggle
/nd debug setlogbroadcast BOTH
/summon zombie ~ ~ ~
/kill @e[type=zombie,limit=1]
```

**Expected result**: EndDungeonAction executes, dungeon ends.

## Support

If issue persists after following this guide:
1. Share full debug logs from cache refresh
2. Share database `triggers_data` JSON
3. Share handler registration logs
