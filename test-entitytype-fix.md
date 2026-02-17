# EntityType Not Persisting - Debug Guide

## Problem
When saving EntityDeathTrigger with `entityType = "TEST"` from web editor, the value reverts to default "ZOMBIE" when reloaded.

## Quick Debug Steps

### 1. Enable Debug Mode
```
/nd debug toggle
/nd debug setlogbroadcast BOTH
```

### 2. Save a Trigger from Web Editor
- Create EntityDeathTrigger
- Set entityType to "TEST" (not ZOMBIE)
- Save
- Check logs for: "Deserializing trigger type 'entity_death_trigger' from JSON"

### 3. Reload the Floor in Web Editor
- Check logs for: "Serialized trigger to JSON"
- Look for `entityType` field in JSON output

## What to Look For

**Good (Working):**
```json
{
  "type": "entity_death_trigger",
  "entityType": "TEST",  ← Should be "TEST", not "ZOMBIE"
  "name": "Kill Test Entity"
}
```

**Bad (Not Working):**
```json
{
  "type": "entity_death_trigger",
  "entityType": "ZOMBIE",  ← Wrong! Should be "TEST"
  "name": "Kill Test Entity"
}
```

## Possible Issues

### Issue 1: Field Not Serialized
If `entityType` is missing from JSON when loading, the Gson configuration might not be accessing private fields.

**Check:** Look for `entityType` in the "Serialized trigger to JSON" log.

### Issue 2: Field Not Deserialized
If `entityType` is in the save JSON but not preserved, Gson might not be populating the field.

**Check:** Look at "Deserialized trigger" log and compare input/output JSON.

### Issue 3: Constructor Overwriting
The no-arg constructor sets `entityType = "ZOMBIE"` which might run after Gson populates fields.

**Check:** This is the most likely issue!

## Solution

The problem is likely in how Gson creates the object:
1. Gson calls `new EntityDeathTrigger()` (no-arg constructor)
2. Constructor sets `entityType = "ZOMBIE"`
3. Gson then tries to populate fields from JSON
4. If something goes wrong, default value persists

**Fix options:**
- Ensure Gson can access private fields (already configured)
- Don't set default in constructor, let Gson handle it
- Use @SerializedName annotation if field name mismatches
- Check if web editor sends field with correct name

## Next Steps

1. Check debug logs to see if `entityType` is in JSON
2. Verify field name matches exactly
3. Test with the provided test class
4. Apply fix based on findings
