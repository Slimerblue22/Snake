# Snake 2.0.0 - Minecraft Spigot Plugin

## Table of Contents

- [Introduction](#introduction)
- [Dependencies](#dependencies)
- [Key Changes from V1 to V2](#key-changes-from-v1-to-v2)
- [Music Configuration](#music-configuration)
- [Region Management](#region-management)
- [Permissions](#permissions)
- [User Commands](#user-commands)
- [Data Collection via bStats](#data-collection-via-bstats)
- [License](#license)

---

## Introduction

Welcome to Snake 2.0.0, a Minecraft Spigot plugin that uniquely recreates the classic Snake game using sheep.

New in Snake 2.0.0:
- A complete rewrite, introducing major gameplay enhancements and new features.
- Watch the new movement system in action: [here](https://www.youtube.com/watch?v=c6Ihh8p-vGk)

NOTICE: Some features from V1 are not present in V2. These may be reintroduced in future versions. Made for Minecraft 1.20.X.

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
- **Data Management**: Three primary data files: `PlayerData.yml`, `Regions.yml`, and `config.yml` cater to player specifics, region info, and music paths respectively.
- **GUI support**: The game now includes a GUI instead of only text-based commands. Text-based commands can still be used.

---

## Music Configuration

Snake 2.0.0 includes support for game music using the NoteBlockAPI. Music settings are configurable within the `config.yml` file. The plugin expects music tracks to be in the .nbs (Note Block Studio) format.

**Important Note**:
- The Snake 2.0.0 plugin does not include any music files by default. Users need to supply their own .nbs files to add music to the game. Once you've added your .nbs files, adjust the file paths in the `config.yml` to point to your music files.

---

## Region Management


### Preliminary Steps

Before using the region-related commands in this plugin, ensure that the regions are first created using WorldGuard. The commands listed below are for registering these pre-existing WorldGuard regions with the Snake 2.0.0 plugin. The plugin itself does not create, modify, or remove WorldGuard regions.

**Important Note**:
- This plugin does not incorporate protections, modifications, or restrictions to the game assets, arena, or other properties located within the WorldGuard region. Users are responsible for setting up these restrictions themselves directly through WorldGuard. It is recommended to disable PvP, block changes, and damage to players or entities within the designated gameplay regions for an optimal experience.
- The Snake game is designed for flat, level ground. It does not support height variations, so ensure your designated play area is uniformly level for the best gameplay experience.

---

### Registering Regions

V2 allows for multiple game regions. To register a pre-existing WorldGuard region, use:

```
/snakeadmin register [region type; game or lobby] [region name] [world region is in]
```

**Example**:

```
/snakeadmin register lobby LobbyTest1 world
/snakeadmin register game GameTest1 world
```

### Linking Regions

After registration, regions need to be linked together for gameplay:

```
/snakeadmin link [region type; game or lobby] [region name] [region type; game or lobby] [region name]
```

**Example**:

```
/snakeadmin link lobby LobbyTest1 game GameTest1
```

### Setting Teleport Coordinates

To set the teleport coordinates within a region:

```
/snakeadmin addteleport [region type; game or lobby] [region name]
```

**Example**:

```
/snakeadmin addteleport lobby LobbyTest1
/snakeadmin addteleport game GameTest1
```

### Viewing Region Information

To list all registered and linked regions:

```
/snakeadmin list [lobby, game, links, or all]
```

### Unlinking and Unregistering Regions

To unlink:

```
/snakeadmin unlink [region type; game or lobby] [region name] [region type; game or lobby] [region name]
```

**Example**:

```
/snakeadmin unlink lobby LobbyTest1 game GameTest1
```

To unregister:

```
/snakeadmin unregister [region name]
```

**Example**:

```
/snakeadmin unregister LobbyTest1
/snakeadmin unregister GameTest1
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

## Data Collection via bStats

This plugin uses bStats to collect anonymous data about its usage. This data assists in understanding how the plugin is being used and aids in its future development. By default, data collection is enabled, but it can be disabled by modifying the bStats global configuration.

However, it's appreciated if users keep this feature enabled as it provides valuable insights and helps improve the plugin.

To view the collected statistics, visit: [Bstats](https://bstats.org/plugin/bukkit/Snake%20V2/19729)

---

## License

This project is licensed under the MIT License. For more details and the full license text, please visit: [MIT License](https://github.com/Slimerblue22/Snake/blob/2.0/LICENSE)