---
description: The Cinematic Module adds data-driven camera sequences, NPC actors, and timeline events to dungeon workflows.
icon: film
---

# Cinematic Module

The **Cinematic Module** (`module-cinematic`) extends NextDungeon with a full cinematic system. It enables server administrators to create smooth, data-driven cutscenes inside dungeon floors using the Blockly workflow editor — no coding required.

**Module ID:** `cinematic`
**Source:** `module-cinematic/src/main/java/fr/perrier/dungeons/module/cinematic/`

<!-- INSERT HERE: video demonstration of a cinematic sequence inside a dungeon floor -->

---

## Features

* **Camera paths** with Catmull-Rom spline interpolation for smooth movement
* **NPC actors** spawned and moved along predefined paths
* **Timeline events** — trigger messages, sounds, and titles at precise timestamps
* **Screen effects** — blind fade, on-screen titles, and sound cues
* **Real-time clock** running at 20 fps (one Bukkit tick per frame)
* All cinematic data stored in the database as JSON — no files on disk

---

## Installation

1. Place `module-cinematic.jar` in `plugins/NextDungeon/modules/`.
2. Restart the server, **or** run `/dungeon admin module load module-cinematic.jar`.
3. Open the Blockly editor — a new **Cinematic** category appears in the toolbox.

---

## Workflow Blocks

### Action Blocks

| Block | Description |
|-------|-------------|
| **Start Cinematic** | Starts a named cinematic sequence for the player. Freezes the player's view and begins playback. |
| **Stop Cinematic** | Immediately stops the currently playing cinematic and restores normal player control. |
| **Clear Cinematic** | Removes all cinematic data (waypoints, NPC paths, timeline events) for the current floor. |
| **Add Camera Waypoint** | Appends a camera waypoint (X, Y, Z, yaw, pitch, duration in ms) to the active cinematic path. |
| **Move NPC** | Moves a registered NPC actor along its defined path over a given duration. |
| **Add Timeline Event** | Schedules an event (message, sound, title, or effect) at a specific timestamp during playback. |
| **Camera Move (Segment)** | Adds a segment-based camera animation from one location to another with configurable duration. |
| **Title (Cinematic)** | Displays a title/subtitle at a given timestamp during the cinematic. |
| **Sound (Cinematic)** | Plays a sound effect at a given timestamp. |
| **Message (Cinematic)** | Sends a chat message at a given timestamp. |
| **Blind (Cinematic)** | Applies a screen-darkening effect for a specified duration (useful for scene transitions). |

### Trigger Blocks

| Block | Description |
|-------|-------------|
| **Cinematic End** | Fires when a cinematic sequence finishes playing. Use this to resume normal gameplay or start a new sequence. |

### Condition Blocks

| Block | Description |
|-------|-------------|
| **Is Cinematic Playing** | Returns `true` if a cinematic is currently playing for the triggering player. |

---

## Example: Intro Cutscene on Floor Entry

In the Blockly editor, attach to `RegionTrigger` (region: `spawn`, event: `enter`):

```
RegionTrigger (spawn, enter)
  -- Start Cinematic (name: "floor1_intro")
  -- Add Camera Waypoint (x: 100, y: 65, z: 200, yaw: 180, pitch: -20, duration: 2000ms)
  -- Add Camera Waypoint (x: 110, y: 68, z: 210, yaw: 160, pitch: -30, duration: 3000ms)
  -- Add Timeline Event (at: 1000ms, type: TITLE, value: "Welcome to the Dungeon!")
  -- Add Timeline Event (at: 1000ms, type: SOUND, value: "ENTITY_ENDER_DRAGON_GROWL")

Cinematic End (name: "floor1_intro")
  -- SendMessageAction (message: "The dungeon awaits...")
  -- TeleportLocationAction (to: boss_room_entrance)
```

<!-- INSERT HERE: screenshot of the Cinematic category in the Blockly editor -->

---

## Architecture

| Class | Description |
|-------|-------------|
| `CinematicModule` | Module entry point — registers all blocks and initialises the cinematic clock |
| `CinematicManager` | Manages active cinematics per player; dispatches timeline events |
| `CinematicClock` / `CinematicClockImpl` | Real-time 20 fps clock that ticks the active cinematic players |
| `CinematicPlayer` | Per-player state during playback (current time, active segments) |
| `CinematicData` | Data model containing waypoints, NPC paths, and timeline events for one cinematic |
| `CameraInterpolation` | Catmull-Rom spline interpolation between `CameraWaypoint` positions |
| `PositionInterpolator` | Computes interpolated `Location` values between two waypoints at a given time fraction |
| `CinematicExecutor` | Executes scheduled actions (title, sound, message, blind) from the timeline |
| `CinematicActionFactory` | Creates `CinematicAction` instances from raw segment data |
| `CinematicSegment` | Base class for a single playback segment (camera, title, sound, message, blind) |

---

## Notes

* The cinematic clock ticks asynchronously every Bukkit tick. Keep segment logic lightweight to avoid performance issues on large servers.
* Camera waypoints use Catmull-Rom interpolation, which requires at least **3 waypoints** for smooth curves. For straight camera moves, use the **Camera Move (Segment)** block instead.
* NPC actors are managed by the plugin's NPC library (NPC-Lib). Ensure `packetevents` is installed, as it is required by the NPC system.
* All cinematic data is stored in the database. Deleting or editing a floor does **not** automatically remove its cinematic data — use the **Clear Cinematic** block to clean up.
