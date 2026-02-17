# Workflow System Architecture

## Overview

The NextDungeon workflow system has been refactored to follow SOLID principles, enabling extensibility, proper serialization/deserialization, and web editor integration.

## Architecture Components

### 1. Context System (Dependency Inversion)

**WorkflowContext Interface**
- Provides access to workflow services (registries, logging)
- Enables dependency injection
- Breaks direct dependency on `Main.getInstance()`
- Location: `fr.perrier.dungeons.spigot.workflow.context.WorkflowContext`

**DefaultWorkflowContext**
- Adapter implementation for gradual migration
- Delegates to `Main.getInstance()` internally
- Location: `fr.perrier.dungeons.spigot.workflow.context.DefaultWorkflowContext`

### 2. Registry System (Open/Closed Principle)

#### Action Registry

**ActionTypeFactory Interface**
- Functional interface for creating actions from JSON
- Signature: `Action createFromJson(JsonObject jsonData)`
- Location: `fr.perrier.dungeons.spigot.workflow.action.registry.ActionTypeFactory`

**ActionTypeRegistry**
- Runtime registry for action types
- Allows plugins to register custom actions
- No code modification needed to add new action types
- Methods:
  - `register(type, factory)` - Register a new action type
  - `createAction(jsonData)` - Create action from JSON
  - `isRegistered(type)` - Check if type is registered
  - `getRegisteredTypes()` - Get all registered types
- Location: `fr.perrier.dungeons.spigot.workflow.action.registry.ActionTypeRegistry`

#### Trigger Registry

**TriggerTypeFactory Interface**
- Functional interface for creating triggers from JSON
- Signature: `Trigger createFromJson(JsonObject jsonData)`
- Location: `fr.perrier.dungeons.spigot.workflow.trigger.registry.TriggerTypeFactory`

**TriggerTypeRegistry**
- Runtime registry for trigger types
- Supports Gson-based automatic deserialization
- Methods:
  - `register(type, factory)` - Register a new trigger type
  - `registerGsonBased(type, class)` - Register with auto-deserialization
  - `createTrigger(jsonData)` - Create trigger from JSON
  - `isRegistered(type)` - Check if type is registered
  - `getRegisteredTypes()` - Get all registered types
- Location: `fr.perrier.dungeons.spigot.workflow.trigger.registry.TriggerTypeRegistry`

### 3. Factory Pattern (Single Responsibility)

**ActionFactory**
- Refactored to use registry pattern
- Each action type has dedicated factory method
- Supports both registry and legacy deserialization
- Lazy initialization of registry
- Methods:
  - `getRegistry()` - Get action type registry
  - `createActionFromJson(jsonData)` - Create action
  - Individual factory methods for each action type
- Location: `fr.perrier.dungeons.spigot.workflow.action.factory.ActionFactory`

**TriggerFactory**
- Refactored to use registry pattern
- Uses Gson-based deserialization for all standard triggers
- Lazy initialization of registry
- Methods:
  - `getRegistry()` - Get trigger type registry
  - `createTriggerFromJson(jsonData)` - Create trigger
- Location: `fr.perrier.dungeons.spigot.workflow.trigger.factory.TriggerFactory`

### 4. Validation System (Error Prevention)

**JsonValidator Utility**
- Safe JSON field access with defaults
- Prevents NullPointerException
- Clear error messages
- Methods:
  - `hasField(json, field)` - Check field existence
  - `hasFields(json, fields...)` - Check multiple fields
  - Type-safe getters: `getString()`, `getInt()`, `getBoolean()`, etc.
  - `requireFields(json, type, fields...)` - Validate with exception
  - `ValidationException` - Custom validation exception
- Location: `fr.perrier.dungeons.spigot.workflow.validation.JsonValidator`

### 5. Serialization (Consistency)

**EditorSerializer**
- Handles web editor JSON format
- Uses Gson automatic serialization
- Serializes triggers and actions with full properties
- Location: `fr.perrier.dungeons.spigot.workflow.serializer.EditorSerializer`

**InstanceSerializer**
- Handles persistence serialization
- Supports className/data wrapper format
- Backward compatible with legacy formats
- Location: `fr.perrier.dungeons.spigot.workflow.serializer.InstanceSerializer`

## SOLID Principles Applied

### Single Responsibility Principle (SRP)
- ✅ ActionFactory: Delegates creation to individual methods
- ✅ Each factory method: Handles one action type
- ✅ JsonValidator: Only handles validation
- ✅ Registries: Only handle type registration and lookup

### Open/Closed Principle (OCP)
- ✅ New action types: Register without modifying ActionFactory
- ✅ New trigger types: Register without modifying TriggerFactory
- ✅ Plugin extensibility: External plugins can add types
- ✅ No switch statement modifications needed

### Liskov Substitution Principle (LSP)
- ⚠️ DelayAction special-casing: Still needs addressing
- ⚠️ Action execution: Needs polymorphic approach

### Interface Segregation Principle (ISP)
- ✅ ActionTypeFactory: Single method interface
- ✅ TriggerTypeFactory: Single method interface
- ✅ WorkflowContext: Focused interface

### Dependency Inversion Principle (DIP)
- ✅ WorkflowContext interface: Abstracts service access
- ⚠️ Actions: Still depend on Main.getInstance()
- 🔄 Migration in progress: To use WorkflowContext

## Usage Examples

### Registering a Custom Action Type

```java
// Get the action registry
ActionTypeRegistry registry = ActionFactory.getRegistry();

// Register a custom action
registry.register("my_custom_action", jsonData -> {
    String param = JsonValidator.getString(jsonData, "param", "default");
    return new MyCustomAction(param);
});
```

### Registering a Custom Trigger Type

```java
// Get the trigger registry
TriggerTypeRegistry registry = TriggerFactory.getRegistry();

// Register with Gson-based deserialization
registry.registerGsonBased("my_custom_trigger", MyCustomTrigger.class);

// Or with custom factory
registry.register("my_complex_trigger", jsonData -> {
    // Custom deserialization logic
    return new MyComplexTrigger(/* ... */);
});
```

### Using JsonValidator

```java
// Safe field access with defaults
String name = JsonValidator.getString(jsonData, "name", "default_name");
int count = JsonValidator.getInt(jsonData, "count", 0);
boolean enabled = JsonValidator.getBoolean(jsonData, "enabled", true);

// Validation with exception
try {
    JsonValidator.requireFields(jsonData, "MyAction", "name", "count");
    // Process validated data
} catch (JsonValidator.ValidationException e) {
    // Handle validation error
}
```

## Serialization Format

### JSON Format (Web Editor)
```json
{
  "triggers": [
    {
      "id": "uuid",
      "type": "region_trigger",
      "name": "Enter Zone",
      "enabled": true,
      "properties": { /* type-specific */ },
      "actions": [
        {
          "type": "send_message_action",
          "message": "Welcome!",
          "targetplayer": "player"
        }
      ]
    }
  ]
}
```

### Persistence Format (Database)
```json
[
  {
    "className": "fr.perrier.dungeons.spigot.workflow.trigger.impl.RegionTrigger",
    "data": {
      "triggerId": "uuid",
      "name": "Enter Zone",
      "enabled": true,
      "properties": { /* ... */ },
      "actions": [
        {
          "className": "fr.perrier.dungeons.spigot.workflow.action.impl.SendMessageAction",
          "data": { /* ... */ }
        }
      ]
    }
  }
]
```

## Migration Path

### Current State
- ✅ Registry pattern implemented
- ✅ Validation added
- ✅ Serialization consistency improved
- ⚠️ Actions still use Main.getInstance()
- ⚠️ DelayAction special-casing exists

### Next Steps
1. Update Action.execute() to accept WorkflowContext
2. Migrate all actions to use WorkflowContext
3. Remove DelayAction special-casing
4. Add comprehensive error handling
5. Write integration tests

## Benefits

1. **Extensibility**: Plugins can add custom triggers/actions without modifying core code
2. **Maintainability**: Clear separation of concerns, easier to understand and modify
3. **Testability**: Factory methods and validation can be tested individually
4. **Error Prevention**: Validation prevents crashes from malformed JSON
5. **Type Safety**: Compile-time checks for registered types
6. **Backward Compatibility**: Legacy formats still supported during migration

## Performance Considerations

- Registry lookups: O(1) HashMap access
- Lazy initialization: No overhead until first use
- Gson caching: Reuses configured Gson instance
- Validation: Minimal overhead, prevents expensive error recovery
