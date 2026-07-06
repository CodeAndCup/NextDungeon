---
description: Run each dungeon floor as its own isolated server with CloudNet.
icon: cloud
---

# CloudNet Integration

CloudNet is what lets NextDungeon give every dungeon run its own dedicated server. When players enter a floor, a fresh server is created just for them; when the run ends, it shuts down. This keeps runs isolated and lag-free.

## Requirements

* **CloudNet** 4.0.0-RC13 or newer
* **CloudNet Bridge module** — needed to move players between servers
* CloudNet's own runtime requirements (see the CloudNet documentation); this is independent of your Minecraft servers, which run on Java 21

---

## How It Works (in brief)

1. A party launches a floor.
2. NextDungeon creates a new server from that floor's **CloudNet task** and world template.
3. When the new server is ready, players are sent to it automatically.
4. When the run ends (or the server empties out), it shuts down and cleans itself up.

You don't manage any of this by hand — you just need to set up one task and template per floor, described below.

---

## Setting Up a Floor's Task

Every floor needs a **CloudNet task** named exactly `<dungeonId>_<floorId>`.

### Step 1 — Create the task

In CloudNet (panel or CLI):

```
task create my_dungeon_floor1
```

Set:

* **Name**: exactly `<dungeonId>_<floorId>` (e.g. `my_dungeon_floor1`)
* **Type**: Minecraft server
* **Nodes**: your Minecraft nodes
* **Memory**: at least 512 MB per instance (1 GB+ recommended)
* **Auto-start count**: `0` — NextDungeon starts instances on demand, CloudNet shouldn't

### Step 2 — Upload the world template

Place your dungeon world in the task's template folder:

```
CloudNet/local/templates/my_dungeon_floor1/default/
```

Put your Minecraft world folder (e.g. `world/`) inside it. When you save from edit mode, NextDungeon updates this template for you automatically.

### Step 3 — Add the plugins to the template

The instance server needs NextDungeon and its dependencies, plus a config pointing at your Redis:

```
CloudNet/local/templates/my_dungeon_floor1/default/plugins/NextDungeon.jar
CloudNet/local/templates/my_dungeon_floor1/default/plugins/MMOCore.jar
CloudNet/local/templates/my_dungeon_floor1/default/plugins/packetevents.jar
CloudNet/local/templates/my_dungeon_floor1/default/plugins/NextDungeon/config.yml
```

<!-- INSERT HERE: screenshot of a CloudNet task configuration screen -->

---

## Enabling CloudNet in NextDungeon

In `plugins/NextDungeon/config.yml`:

```yaml
InstanceProvider:
  type: "CLOUDNET"
```

Nothing else is required — the CloudNet connection is handled automatically.

---

## Instance Timeouts

* If an instance takes longer than `InstanceSettings.loadingTimeout` seconds (default 120) to become ready, the launch is cancelled and the player is notified.
* An instance with no players shuts down after `InstanceSettings.emptyShutdownTimeout` seconds (default 120) to free resources.

---

## Troubleshooting

| Problem | Likely cause | Fix |
|---------|--------------|-----|
| Launching a floor fails immediately | No CloudNet task for the floor | Create a task named exactly `<dungeonId>_<floorId>` |
| Instance never becomes ready | Template is missing plugins or config | Make sure the plugin JARs and `config.yml` are in the task template |
| Players aren't transferred | CloudNet Bridge module missing | Install and enable the Bridge module on the instance node |
| Instances fail to start at all | CloudNet not available when the plugin loads | Ensure CloudNet and its Bridge module are running before the server starts |
