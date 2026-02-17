# Workflow System Debug Guide

## Issues Fixed

### 1. ✅ Missing Import
**File**: `ActionFactory.java`
**Problem**: `ConditionalAction` used but not imported
**Fix**: Added `import fr.perrier.dungeons.spigot.workflow.action.conditional.ConditionalAction;`

### 2. ✅ Malformed Implements Clauses
**Files**: 7 condition classes
**Problem**: `implements BlocklyAction , , ConditionalAction` (double comma with spaces)
**Fix**: Changed to `implements BlocklyAction, ConditionalAction`

## Main Issue: EntityDeathTrigger Not Firing

### Symptoms
```
[21:16:44] Triggers cache refresh complete: 1 triggers
[21:17:15] Processing event: EntityDeathEvent with 0 triggers
```

Trigger is loaded but not found when zombie is killed.

### Diagnostic Logs Added

The code now includes extensive debug logging in `TriggersRegistry.java`:

#### During Handler Registration (startup)
```
Registered handlers:
  - EntityDeathTriggerHandler for EntityDeathEvent (supports: [entity_death_trigger])
  - ...
```

#### During Cache Refresh
```
Processing trigger: Kill Zombie (type: entity_death_trigger, class: EntityDeathTrigger)
  Checking handler: EntityDeathTriggerHandler (event: EntityDeathEvent, supports: [entity_death_trigger])
  -> Added trigger to event cache for EntityDeathEvent
```

#### After Cache Refresh
```
Cache contents - triggersByType: [entity_death_trigger]
Cache contents - triggersByEventType: [EntityDeathEvent, PlayerMoveEvent, ...]
```

#### If Problem Detected
```
WARNING: Trigger Kill Zombie (type: entity_death_trigger) was not added to any event cache!
```

### How to Use Debug Logs

1. **Enable debug mode** on the server:
   ```
   /nd debug toggle
   ```

2. **Check startup logs** for "Registered handlers" - verify EntityDeathTriggerHandler is listed

3. **Load/Edit floor** to trigger cache refresh

4. **Look for these patterns**:
   
   **If trigger type is wrong:**
   ```
   Processing trigger: Kill Zombie (type: wrong_type, class: EntityDeathTrigger)
   ```
   
   **If handler not registered:**
   ```
   Registered handlers: (EntityDeathTriggerHandler missing from list)
   ```
   
   **If types don't match:**
   ```
   Checking handler: EntityDeathTriggerHandler (event: EntityDeathEvent, supports: [different_type])
   ```
   
   **If trigger is disabled:**
   ```
   Skipping disabled trigger: Kill Zombie
   ```

5. **Check cache contents** - EntityDeathEvent should be in triggersByEventType

### Expected Correct Output

```
[DEBUG] Registered handlers:
[DEBUG]   - RegionTriggerHandler for PlayerMoveEvent (supports: [region_trigger])
[DEBUG]   - EntityDeathTriggerHandler for EntityDeathEvent (supports: [entity_death_trigger])
[DEBUG]   - BlockClickTriggerHandler for PlayerInteractEvent (supports: [block_click_trigger])
[DEBUG]   - PlayerDamageTriggerHandler for EntityDamageEvent (supports: [player_damage_trigger])
[DEBUG]   - ItemPickupTriggerHandler for EntityPickupItemEvent (supports: [item_pickup_trigger])
[DEBUG]   - ChatMessageTriggerHandler for AsyncPlayerChatEvent (supports: [chat_message_trigger])
[DEBUG]   - PlayerJumpTriggerHandler for PlayerMoveEvent (supports: [player_jump_trigger])

[INFO] GlobalTriggerManager initialized with 6 handlers

[DEBUG] Processing trigger: Kill Zombie (type: entity_death_trigger, class: EntityDeathTrigger)
[DEBUG]   Checking handler: RegionTriggerHandler (event: PlayerMoveEvent, supports: [region_trigger])
[DEBUG]   Checking handler: PlayerJumpTriggerHandler (event: PlayerMoveEvent, supports: [player_jump_trigger])
[DEBUG]   Checking handler: EntityDeathTriggerHandler (event: EntityDeathEvent, supports: [entity_death_trigger])
[DEBUG]   -> Added trigger to event cache for EntityDeathEvent

[INFO] Triggers cache refresh complete: 1 triggers
[DEBUG] Cache contents - triggersByType: [entity_death_trigger]
[DEBUG] Cache contents - triggersByEventType: [EntityDeathEvent]

[INFO] Processing event: EntityDeathEvent with 1 triggers
[DEBUG] Invoking handler: EntityDeathTriggerHandler for event: EntityDeathEvent
```

### Code Verification

All code is correct:
- ✅ `EntityDeathTrigger.getType()` returns `"entity_death_trigger"`
- ✅ `EntityDeathTriggerHandler.getSupportedTriggerTypes()` returns `["entity_death_trigger"]`
- ✅ `TriggerFactory` registers `"entity_death_trigger"` → `EntityDeathTrigger.class`
- ✅ `registerDefaultHandlers()` includes `EntityDeathTriggerHandler`

### Troubleshooting Steps

1. **Verify trigger in database/Redis**
   - Check that trigger is actually saved
   - Check that trigger has correct type field

2. **Check trigger enabled state**
   - Trigger must have `enabled: true`

3. **Verify floor is loaded**
   - Current floor must contain the trigger
   - Cache refresh must be called after floor load

4. **Check for exceptions**
   - Any exception during cache refresh will be logged with stack trace

### Why 6 Handlers for 7 Handler Classes?

This is **NORMAL**:
- `handlers` is a `Map<EventType, List<Handler>>`
- Multiple handlers can handle the same event type
- `RegionTriggerHandler` and `PlayerJumpTriggerHandler` both handle `PlayerMoveEvent`
- Therefore: 7 handler instances, 6 event types, `handlers.size() == 6` ✅
