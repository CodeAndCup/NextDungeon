---
description: The Cinematic Module adds camera cutscenes, titles, sounds, and screen effects to dungeon workflows.
icon: film
---

# Cinematic Module

The **Cinematic Module** lets you build smooth in-dungeon cutscenes right in the workflow editor — camera moves, titles, sounds, and screen effects — no coding required.

**Module ID:** `cinematic`

<!-- INSERT HERE: video demonstration of a cinematic sequence inside a dungeon floor -->

---

## Features

* **Camera paths** with smooth interpolation between waypoints
* **Timeline events** — fire messages, sounds, and titles at exact moments
* **Screen effects** — screen-black fades, on-screen titles, and sound cues

---

## Installation

1. Place `module-cinematic.jar` in `plugins/NextDungeon/modules/`.
2. Restart the server, **or** run `/dungeon admin module load module-cinematic.jar`.
3. Open the editor — a new **Cinematic** category appears in the toolbox.

---

## Blocks

### Actions

| Block | What it does |
|-------|--------------|
| **Start Cinematic** | Starts a named cinematic for the player (or everyone) and takes over their view |
| **Stop Cinematic** | Stops the current cinematic and returns control to the player |
| **Clear Cinematic** | Clears a cinematic's saved waypoints — use it before redefining them on replay |
| **Add Camera Waypoint** | Adds a camera point (position, look angle, timing, interpolation) to a cinematic |
| **Camera Move** | Smoothly moves the camera along a path over a range of frames |
| **Cinematic Title** | Shows a title/subtitle during a segment |
| **Cinematic Sound** | Plays a sound at a given moment |
| **Cinematic Message** | Sends a chat or action-bar message at a given moment |
| **Screen Black** | Fades the screen to black for a segment (great for scene changes) |
| **Move NPC** *(experimental)* | Intended to move an NPC actor along the timeline — currently a work in progress |
| **Timeline Event** *(experimental)* | Intended to schedule generic timeline events — currently a work in progress |

### Triggers

| Block | What it does |
|-------|--------------|
| **When Cinematic Ends** | Fires when a cinematic finishes — use it to resume gameplay or chain another scene |

### Conditions

| Block | What it does |
|-------|--------------|
| **Is Cinematic Playing?** | Passes while a cinematic is playing for the player |

---

## Example: Intro Cutscene on Floor Entry

```
Region Enter/Exit (spawn room, Enter)
  ├─ Start Cinematic ("floor1_intro")
  ├─ Add Camera Waypoint (overlooking the entrance)
  ├─ Add Camera Waypoint (panning toward the first room)
  ├─ Cinematic Title ("Welcome to the Dungeon!")
  └─ Cinematic Sound (dragon growl)

When Cinematic Ends ("floor1_intro")
  ├─ Send Message ("The dungeon awaits...")
  └─ Teleport (to the first room)
```

<!-- INSERT HERE: screenshot of the Cinematic category in the editor -->

---

## Notes

* Smooth curved camera paths need at least **3 waypoints**. For a simple straight move, use the **Camera Move** block.
* A cinematic's data stays saved with the floor. Editing or deleting the floor doesn't automatically wipe it — use **Clear Cinematic** to reset it.
* **Move NPC** and **Timeline Event** are placeholders for now; their full behaviour is still in development.
