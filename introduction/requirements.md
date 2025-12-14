---
description: >-
  Before installing and running the Dungeons Plugin, make sure your server meets
  the following requirements and dependencies.
icon: shield-check
---

# Requirements

### Server Platform

* **Minecraft Server**: Spigot, Paper, or compatible fork (1.21.4+ recommended)
* **Java Version**: Java 21 or newer

### Required Dependencies

The following are core requirements:

* **Redis Server** – Required for cross-server communication (especially with CloudNet)
* **Database** – MySQL or MongoDB for storing dungeon data and player progress

### Integration Plugins

These plugins provide additional functionality and are **optional** but recommended:

* [CloudNet](https://cloudnetservice.eu/) – For dynamic server and instance management _<mark style="color:orange;">(Requires Java 24/25 and version 4.0.0-RC13)</mark>_
* [Advanced Slime Paper (ASP)](https://github.com/InfernalSuite/AdvancedSlimePaper) – For optimized world management (alternative to CloudNet)
* [Parties](https://www.spigotmc.org/resources/parties.3709/) – For group play and party features
* [MMOCore](https://www.spigotmc.org/resources/mmocore.87699/) – For RPG elements (classes, skills, level requirements)
* [MythicMobs](https://www.spigotmc.org/resources/mythicmobs.5702/) – For custom mobs, bosses, and abilities

> **Note:** NextDungeon works with vanilla Minecraft world management if CloudNet or ASP are not installed, though this is not recommended for production due to performance considerations.

### Web Editor

To use the visual Blockly web editor for dungeon creation:

* Access the provided web editor URL (details in Dungeon Creation)
* Make sure your server and firewall allow connections between the editor and your Minecraft server

### Resources & Hardware

* Sufficient RAM and CPU to run multiple dungeon instances (recommend at least 4GB RAM for medium-sized servers)
* SSD storage recommended for faster world and instance loading

### Permissions

* Admin or OP access recommended for installation and setup
* Proper permissions must be granted for managing dungeons and using plugin commands (see Permissions)

### Supported Minecraft Versions

* The plugin is tested and supported on Minecraft 1.21.4 and above. Compatibility with future versions may depend on updates to dependencies.

***

Ready to install? Continue to the Installation Guide.
