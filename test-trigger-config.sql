-- Test EntityDeathTrigger Configuration
-- This creates a working EntityDeathTrigger that should fire when a ZOMBIE is killed

-- Expected JSON structure for floor_triggers table:
-- The trigger should be in InstanceSerializer format with className/data wrapper

-- Example working trigger structure:
[
  {
    "className": "fr.perrier.dungeons.spigot.workflow.trigger.impl.EntityDeathTrigger",
    "data": {
      "triggerId": "test-zombie-trigger-001",
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
]

-- SQL to update floor_triggers table:
-- UPDATE floor_triggers 
-- SET triggers_data = '[...]' 
-- WHERE floor_id = 'example_floor1';

-- Key points to check:
-- 1. "enabled": true  <- MUST be true!
-- 2. "type": "entity_death_trigger"  <- Must match exactly
-- 3. "entityType": "ZOMBIE"  <- Entity type to match
-- 4. actions array with EndDungeonAction
-- 5. className fields present for serialization

-- Common issues:
-- - "enabled": false  ← Trigger won't load
-- - "type" mismatch  ← Won't match handler
-- - Missing className wrapper  ← Deserialization fails
-- - Wrong table/floor_id  ← Trigger not loaded for current floor
