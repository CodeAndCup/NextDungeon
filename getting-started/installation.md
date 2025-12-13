---
description: Follow these steps to install the Dungeons Plugin on your Minecraft server.
icon: screwdriver-wrench
---

# Installation

### Prerequisites

* **Minecraft Server:** Spigot, Paper, or a compatible fork (version 1.21.4 recommended)
* **Java:** Java 21
* **CloudNet:** v4.0.0-RC13
* **Redis Server:** Running and accessible
* **Required Dependencies:**
  * CloudNet Driver/Bridge modules
  * Parties
  * MMOCore
  * MythicMobs

> **Tip:** Make sure all dependencies are compatible with your server version.

### Step-by-Step Installation

#### 1. Download the Plugin

* Go to the [releases page](https://github.com/SAOFR-DEV/Dungeons/releases) of the Dungeons repository.
* Download the latest version of the plugin JAR file.

#### 2. Add the Plugin to Your Server

* Place the downloaded `NextDungeons.jar` file into the `plugins` folder of your Minecraft server.

#### 3. Install Dependencies

* Ensure the required dependency plugins (e.g., Parties) are also in the `plugins` folder.
* Set up CloudNet and Redis according to their documentation.

#### 4. Configure CloudNet

* Make sure CloudNet is installed and configured for your network.
* Refer to the [CloudNet documentation](https://cloudnetservice.eu/) for setup instructions.

#### 5. Start Your Server

* Start your Minecraft server to generate the default configuration files for Dungeons.
* Check the console logs for any errors or missing dependencies.

#### 6. Configure the Plugin

* Edit the main configuration file:\
  `plugins/Dungeons/config.yml`
*   Example configuration:

    ```yaml
    ServerConfiguration:
      isLobby: false

    RedisConfiguration:
      host: "localhost"
      port: 6379
      password: "your_password"
      topic: "dungeons"
    ```
* If needed, configure additional plugins and dependencies.

#### 7. Set Up Dungeons and Floors

* Use the provided configuration templates in the `dungeons/` folder to define your dungeons and floors.
* See the Dungeon Configuration page for more details.

#### 8. Verify Installation

* Use `/dungeon debug list` in-game to verify the plugin is loaded and dungeons are available.
* Check the logs for successful initialization and any warnings.

***

Need more help? See the Troubleshooting section or join our Discord for support.
