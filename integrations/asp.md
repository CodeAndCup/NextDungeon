---
description: Using Advanced Slime Paper (ASP) as an alternative world management backend.
icon: layer-group
---

# Advanced Slime Paper (ASP) Integration

Advanced Slime Paper (ASP), developed by InfernalSuite, is an optimised world management solution for Minecraft servers. It loads worlds from compressed `.slime` format files, drastically reducing memory usage and world load times compared to standard vanilla worlds.

## Status in 1.0.4-SNAPSHOT

> **ASP support is listed as a planned feature in the Feature Overview** (referenced in the Blockly web editor as an alternative world provider), but a dedicated `ASPProvider` implementation is **not present in the `1.0.4-SNAPSHOT` codebase**. The only `InstanceProvider` currently implemented is `CloudNetProvider`.

If you are looking to use ASP, watch for a future release that includes an `ASPInstanceProvider`. This page documents the intended integration model based on the `InstanceProvider` interface.

---

## InstanceProvider Interface

All instance providers implement `fr.perrier.dungeons.spigot.instance.InstanceProvider`:

```java
public interface InstanceProvider {
    CompletableFuture<Boolean> initialize();
    CompletableFuture<UUID> createInstance(Floor floor, boolean editMode);
    CompletableFuture<Boolean> saveEditWorldToTemplate(Floor floor);
    void shutdown();
    ProviderType getType();
}
```

A future ASP provider would implement this interface, loading worlds from `.slime` files instead of CloudNet templates.

---

## Planned Configuration

When an ASP provider is available, the configuration will likely look like:

```yaml
InstanceProvider:
  type: "ASP"   # Placeholder — not available in 1.0.4-SNAPSHOT
```

---

## Why Use ASP?

| Feature | Vanilla | CloudNet | ASP |
|---------|---------|----------|-----|
| World load time | Slow | Medium | Fast |
| Memory usage | High | Medium | Low |
| External service required | No | Yes (CloudNet) | No |
| Recommended for production | No | Yes | Yes (when available) |

---

## Resources

* [Advanced Slime Paper GitHub](https://github.com/InfernalSuite/AdvancedSlimePaper)
* [InfernalSuite Documentation](https://infernalsuite.com/docs/asp/)
