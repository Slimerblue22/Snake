# Snake V2 - Minecraft Spigot Plugin

## Table of Contents

- [Introduction](#introduction)
- [Dependencies](#dependencies)
- [Key Changes from V1 to V2](#key-changes-from-v1-to-v2)
- [Music Configuration](#music-configuration)
- [Region Management](#region-management)
- [Permissions](#permissions)
- [User Commands](#user-commands)
- [Debugging](#debugging-and-logging)
- [Data Collection via bStats](#data-collection-via-bstats)
- [License](#license)

---

## Introduction

Welcome to Snake V2, a Minecraft Paper Spigot plugin that uniquely recreates the classic Snake game using sheep.

New in Snake V2:
- A complete rewrite, introducing major gameplay enhancements and new features.
- Watch the new movement system in action: [here](https://www.youtube.com/watch?v=c6Ihh8p-vGk)

Made for Minecraft 1.20.X.

---

## Dependencies

### Required Dependencies

- WorldGuard
- ProtocolLib
- Paper Spigot (Paper Spigot is essential due to Adventure API integration. Other server software with Adventure API should also work but hasn't been tested.)

### Soft Dependencies

- NoteBlockAPI (For more details on setting up music, see the [Music Configuration](#music-configuration) section)

---

## Key Changes from V1 to V2

- **Snake Movement**: Transitioned from teleporting to a velocity-based system for a smoother user experience.
- **Configuration**: V1 config files are incompatible with V2. Delete the old configs, allowing the plugin to generate new ones.
- **Data Management**: Three primary data files: `PlayerData.db`, `Regions.db`, and `config.yml` cater to player specifics, region info, and general config values respectively.
- **GUI support**: The game now includes a GUI instead of only text-based commands. Text-based commands can still be used.

---

## Music Configuration

Snake V2 includes support for game music using the NoteBlockAPI. Music settings are configurable within the `config.yml` file. The plugin expects music tracks to be in the .nbs (Note Block Studio) format.

**Important Note**:
- The Snake V2 plugin does not include any music files by default. Users need to supply their own .nbs files to add music to the game. Once you've added your .nbs files, adjust the file paths in the `config.yml` to point to your music files.

---

## Region Management


### Preliminary Steps

Before using the region-related commands in this plugin, ensure that the regions are first created using WorldGuard. The commands listed below are for registering these pre-existing WorldGuard regions with the Snake V2 plugin. The plugin itself does not create, modify, or remove WorldGuard regions.

**Important Note**:
- This plugin does not incorporate protections, modifications, or restrictions to the game assets, arena, or other properties located within the WorldGuard region. Users are responsible for setting up these restrictions themselves directly through WorldGuard. It is recommended to disable PvP, block changes, and damage to players or entities within the designated gameplay regions for an optimal experience.
- The Snake game is designed for flat, level ground. It does not support height variations, so ensure your designated play area is uniformly level for the best gameplay experience.

---

### Registering Regions

V2 allows for multiple game regions. To register a pre-existing WorldGuard region, use:

```
/snakeregion register [region type; game or lobby] [region name] [world region is in]
```

**Example**:

```
/snakeregion register lobby LobbyTest1 world
/snakeregion register game GameTest1 world
```

### Linking Regions

After registration, regions need to be linked together for gameplay:

```
/snakeregion link [region1 name] [region2 name]
```

**Example**:

```
/snakeregion link LobbyTest1 GameTest1
```

### Setting Teleport Coordinates

Set or update teleport coordinates within a region. Use the player's current location or specify coordinates:

```
/snakeregion addtp [region name]
OR
/snakeregion addtp [region name] [x] [y] [z]
```

**Example**:

```
/snakeregion addtp LobbyTest1
OR
/snakeregion addtp LobbyTest1 1 2 3

/snakeregion addtp GameTest1
OR
/snakeregion addtp GameTest1 4 5 6
```

### Viewing Region Information

To list all registered and linked regions:

```
/snakeregion view [game | lobby | links | search]
```

**Example**:

```
/snakeregion view game
/snakeregion view lobby
/snakeregion view links
/snakeregion view search GameTest1
```

### Unlinking and Unregistering Regions

To unlink:

```
/snakeregion unlink [region1 name] [region2 name]
```

**Example**:

```
/snakeregion unlink LobbyTest1 GameTest1
```

To unregister:

```
/snakeregion unregister [region name]
```

**Example**:

```
/snakeregion unregister LobbyTest1
/snakeregion unregister GameTest1
```

---

## Permissions

- `snake.admin`: Grants access to all administrative commands.
- `snake.play`: Allows players to engage in the game.

---

## User Commands

Players with `snake.play` permission can utilize:

- **Game Controls**:
  - `/snakegame start`: Initiates the game in a linked lobby region.
  - `/snakegame stop`: Concludes the game, transporting the player back to the lobby.
  - `/snakegame gui`: Unveils the game's GUI for setting adjustments and statistics.
  - `/snakegame help`: A guide on gameplay, controls, and objectives.

- **Personalization & Stats**:
  - `/snakegame color`: Choose and save your snake's color.
  - `/snakegame highscore`: Reveals your highest score.
  - `/snakegame leaderboard`: Compares your score with other players on the leaderboard.
  - `/snakegame music`: Toggles your music on or off.

---

## Debugging and Logging

Snake V2 incorporates an extensive debugging system that aids in tracking and troubleshooting the internal operations of the plugin. This debugging system is modular, allowing for fine-tuned control over which aspects of the plugin's operations are logged.

### Debug Commands

Administrators with the `snake.admin` permission can utilize the following commands to control the debugging system:

- **Toggle Debug Categories**:
  - `/snakedebug category [Category]`: Toggles the debug mode for a specific category.

- **Set Debug Destination**:
  - `/snakedebug destination [Destination]`: Sets the global destination for debug messages.
  - The available destinations are `PLAYER`, `CONSOLE`, and `BOTH`.

- **View Debug Status**:
  - `/snakedebug status`: Displays the currently enabled debug categories and the global debug message destination.

- **Help**:
  - `/snakedebug help`: Provides a guide on how to use the debugging commands.

### Colored Logging

For better clarity and distinction, debug messages sent to players are color-coded. The different segments of a debug message are highlighted in different colors to make them stand out and improve readability.

### Note on Debugging

The debugging system is primarily intended for developers, administrators, and those familiar with the internal workings of the Snake plugin. It provides a detailed look into the plugin's operations for troubleshooting and development purposes.

---

## Data Collection via bStats

This plugin uses bStats to collect anonymous data about its usage. This data assists in understanding how the plugin is being used and aids in its future development. By default, data collection is enabled, but it can be disabled by modifying the bStats global configuration.

However, it's appreciated if users keep this feature enabled as it provides valuable insights and helps improve the plugin.

To view the collected statistics, visit: [Bstats](https://bstats.org/plugin/bukkit/Snake%20V2/19729)

---

## License

This project is licensed under the MIT License. For more details and the full license text, please visit: [MIT License](https://github.com/Slimerblue22/Snake/blob/2.0/LICENSE)