# Dungeons Plugin

A powerful Minecraft dungeon plugin that provides instanced dungeons with CloudNet service integration and Redis synchronization.

## Table of Contents
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Configuration](#configuration)
- [Usage](#usage)
- [Commands](#commands)
- [Development](#development)
- [API](#api)
- [Contributing](#contributing)
- [License](#license)

## Features

### Core Features
- **Instanced Dungeons**: Each dungeon instance runs on its own server
- **CloudNet Integration**: Seamless server management and scaling
- **Redis Synchronization**: Real-time data synchronization across servers
- **Dynamic Template System**: Automated dungeon template management
- **Party System Integration**: With Parties API

### Dungeon Features
- **Multi-Floor System**: Support for multiple floors per dungeon
- **Step-Based Progression**: Configurable progression system
- **Requirements System**: Set entry requirements for dungeons
- **Custom Rules**: Configurable rules per floor
- **World Management**: Automatic world creation and cleanup

## Requirements

- Minecraft Server 1.20.4
- CloudNet v4.0.0-RC13
- Redis Server
- Java 21
- Dependencies:
    - CloudNet Driver API
    - Redisson Client
    - CupCodeAPI
    - Parties API

## Installation

1. Download the latest release from the releases page
2. Place the JAR file in your plugins folder
3. Configure your CloudNet setup
4. Start your server
5. Configure the plugin (config.yml)

## Configuration

### Main Configuration (config.yml)
```yaml
ServerConfiguration:
  isLobby: false

RedisConfiguration:
  host: "localhost"
  port: 6379
  password: "your_password"
  topic: "dungeons"
```

### Dungeon Configuration (dungeons/example.yml)
```yaml
dungeon:
  id: "example"
  name: "Example Dungeon"
  floors:
    - id: "floor1"
      name: "First Floor"
      world:
        spawn:
          x: 0
          y: 100
          z: 0
        difficulty: "NORMAL"
      requirements:
        retry_cooldown: "30m"
        required_dungeons: []
        required_items: []
        forbidden_items: []
        party:
          min_size: 1
          max_size: 5
      rules:
        death_ban: "1h"
        gamemode: "ADVENTURE"
        allow_flight: false
      steps:
        - id: "step1"
          name: "Beginning"
          region:
            pos1:
              x: -10
              y: 100
              z: -10
            pos2:
              x: 10
              y: 120
              z: 10
```

## Usage

### Basic Setup

*Will be added later*

### Creating a Dungeon Instance

```bash
/dungeon admin create <dungeonId> <floorId>
```

### Testing a Dungeon

```bash
/dungeon admin test <dungeonId> <floorId>
```

## Commands

### Admin Commands
- `/dungeon admin create <dungeonId> <floorId>` - Create a new dungeon
- `/dungeon admin test <dungeonId> <floorId>` - Test a dungeon floor

### Player Commands
- `/dungeon join <dungeonId>` - Join a dungeon
- `/dungeon leave` - Leave current dungeon
- `/dungeon list` - List available dungeons

### Debug Commands
- `/dungeon debug toggle` - Toggle debug mode
- `/dungeon debug info` - Show debug information

> *Still in progress*

## Development

### Building from Source

1. Clone the repository
```bash
git clone https://github.com/SAOFR-DEV/Dungeons.git
```

2. Build with Maven
```bash
mvn clean package
```

### API Usage

No API available yet

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Authors

- Perrier - Initial work and maintenance

## Support

For support:
1. Check the [Wiki](https://github.com/SAOFR-DEV/Dungeons/wiki)
2. Open an [Issue](https://github.com/SAOFR-DEV/Dungeons/issues)
3. Join our Discord server

Last updated: 2025-08-11 20:11:15 UTC