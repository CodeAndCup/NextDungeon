# NextDungeon

NextDungeon is a powerful and extensible plugin for Minecraft servers, designed to manage and automate custom dungeons with advanced workflow logic. Dungeons and floors are centrally managed and synchronized using Redis, with a flexible trigger-action system for custom mechanics. The plugin includes a web editor, admin/debug commands, and integrates with Parties for group management.

Main Documentation: [https://cupcode-1.gitbook.io/nextdungeon/](https://cupcode-1.gitbook.io/nextdungeon/)

## Features
- **Centralized Dungeon Management:** Dungeons and floors are managed as data objects and synchronized across servers using Redis.
- **Flexible Workflow System:** Triggers, actions, and conditions allow for custom dungeon automation and scripting.
- **Floor Configuration:** Each floor supports requirements, rules, steps, and triggers for granular control.
- **Web Editor:** Integrated web interface for editing dungeons, floors, and workflow logic.
- **Admin & Debug Tools:** Commands for editing, testing, importing, and managing dungeons and floors.
- **Parties Integration:** Supports group management via PartiesAPI.
- **Template System:** Asynchronous generation and management of floor templates.
- **No Per-Dungeon Server Instancing:** Dungeons are not run on separate servers; all data is synchronized and managed centrally.

## Requirements
- Minecraft server 1.21.4
- Java 21
- Redis server
- Dependencies:
    - CloudNet v4.0.0-RC13 or higher
    - Redis
    - Parties
    - MMOCore
- SoftDependencies (optional):
    - MythicLib / MythicMobs

## Installation
1. Place the NextDungeon plugin in your server's `plugins` folder.
2. Install and configure Redis.
3. Ensure all dependencies are present.
4. Restart your server.

## Configuration
- Main configuration: `src/resources/config.yml`
- Dungeons and floors: YAML files in `src/resources/dungeons/`
- Example configuration: `dungeon_exemple.yml`
- Redis and MySQL connection settings in the config file. `MongoDB is not supported for now but it's planned for future releases.`

## Usage
- Use the web editor to create and manage dungeons, floors, triggers, and actions.
- Admins can edit, test, import, and manage dungeons and floors using commands.
- Debug commands allow inspection of dungeons, floors, and instances.

## Commands
### Admin Commands
- `/dungeon admin help` — List admin commands
- `/dungeon admin edit start <dungeon> <floor>` — Start edit mode for a floor
- `/dungeon admin edit stop [--confirm]` — Stop edit mode
- `/dungeon admin webeditor start` — Start web editor
- `/dungeon admin webeditor stop` — Stop web editor
- `/dungeon admin test <dungeon> <floor>` — Test a dungeon floor
- `/dungeon admin import <world> <dungeon> <floor>` — Import a world as a dungeon floor
- `/dungeon admin load <config>` — Reload configuration
- `/dungeon admin status <dungeon> [floor]` — Check status
- `/dungeon admin goto <server>` — Teleport to server

### Debug Commands
- `/dungeon debug help` — List debug commands
- `/dungeon debug list dungeons` — List all dungeons
- `/dungeon debug list floors` — List all floors
- `/dungeon debug list instances` — List all instances
- `/dungeon debug openmenu` — Open example dungeon menu
- `/dungeon debug print <message>` — Print a message

### Player Commands
- `/dungeon` — Show available commands
- `/dungeon help` — Show help

## Workflow System
- **Triggers:** Define events that start workflows (e.g., region entry, entity death).
- **Actions:** Define effects (e.g., teleport, send message, set variable).
- **Conditions:** Use logic blocks (if/else) for advanced automation.
- All workflow logic is editable via the web editor.

## API & Development
- No API available for now.

## Contributing
Contributions are welcome! Please open an issue or pull request on the GitHub repository.

## License
This project is licensed under the MIT License. See `LICENSE.md` for more information.
