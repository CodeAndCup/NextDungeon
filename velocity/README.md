# NextDungeon Velocity Plugin

This is the Velocity proxy implementation of the NextDungeon web editor plugin, ported from the BungeeCord version.

## Features

- **Centralized Web Editor Server**: Runs a centralized web server on port 8080 for the Blockly editor interface
- **Session Management**: Manages editor sessions across multiple Spigot servers  
- **Proxy Commands**: Provides `/webeditor-proxy` commands for session management
- **Spigot Communication**: Communicates with Spigot servers via HTTP requests for trigger management

## Commands

- `/webeditor-proxy create <dungeon> <floor> <server>` - Create new editor session
- `/webeditor-proxy stop` - Stop current editor session
- `/webeditor-proxy list` - List all active sessions
- `/webeditor-proxy info` - Show current session information

Permission: `nextdungeons.proxy.webeditor`

## Architecture

### Core Components

1. **NextDungeonVelocity** - Main plugin class handling lifecycle
2. **WebEditorProxyCommand** - Command handler for web editor operations  
3. **ProxyWebEditorServer** - HTTP server serving the Blockly interface
4. **EditorSessionManager** - Manages active editing sessions
5. **SpigotCommunicationService** - Handles communication with backend Spigot servers
6. **WebEditorMessage** - Message format for inter-server communication

### Key Differences from BungeeCord Version

- **Plugin Lifecycle**: Uses Velocity's `@Subscribe` event system instead of `onEnable()/onDisable()`
- **Commands**: Implements `SimpleCommand` instead of extending `Command`
- **Text Components**: Uses Adventure text components instead of legacy chat formatting
- **Logging**: Uses SLF4J logger instead of Java util logging
- **Dependencies**: Uses Velocity API and injection system

## URL Structure

When a session is created, the web editor is accessible at:
```
http://localhost:8080/{sessionId}/editor/
```

Where `{sessionId}` follows the format: `{floorId}-{uuid8}`

## Communication

The plugin communicates with Spigot servers via HTTP requests to port 8081, handling:
- Loading dungeon triggers
- Saving trigger configurations  
- Getting trigger type definitions
- Generating Blockly JavaScript blocks
- Retrieving floor information

## Build Requirements

- Java 17+
- Velocity API 3.4.0-SNAPSHOT
- Maven 3.6+

Built using `mvn clean package` from the velocity directory.