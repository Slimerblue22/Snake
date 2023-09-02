# Snake 2.0.0 Beta-1

## Table of Contents

- [Introduction](#introduction)
- [Dependencies](#dependencies)
   - [Required Dependencies](#required-dependencies)
   - [Soft Dependencies](#soft-dependencies)
- [Key Changes from V1 to V2](#key-changes-from-v1-to-v2)
- [Region Management](#region-management)
   - [Preliminary Steps](#preliminary-steps)
   - [Registering Regions](#registering-regions)
   - [Linking Regions](#linking-regions)
   - [Setting Teleport Coordinates](#setting-teleport-coordinates)
   - [Viewing Region Information](#viewing-region-information)
   - [Unlinking and Unregistering Regions](#unlinking-and-unregistering-regions)
- [Permissions](#permissions)
- [User Commands](#user-commands)

---

## Introduction

Welcome to the Beta-1 release of Snake 2.0.0! This is a complete rewrite, introducing significant changes in gameplay, new features, and much more. This is a beta version and should not be used in production environments. Debug logs will be printed to the console during this phase for testing purposes. Some features in this beta version are either incomplete or do not yet exist. Further updates will aim to complete these features.

New movement system shown here
https://www.youtube.com/watch?v=c6Ihh8p-vGk
---

## Dependencies

### Required Dependencies
- WorldGuard
- ProtocolLib
- Paper Spigot or one of its forks

### Soft Dependencies
- NoteBlockAPI

---

## Key Changes from V1 to V2

- **Snake Movement**: Movement has changed from teleporting to a velocity-based system, providing a smoother user experience.
- **Config Files**: V1 config files are not compatible with V2. Delete them and let the plugin regenerate new ones. Note that `config.yml` does not currently exist.

- **Data Files**: Two main data files exist:
   - `PlayerData.yml`: Stores player-specific data like snake color preference, high score, username, and UUID.
   - `Regions.yml`: Information about multiple game regions.

---

## Region Management

### Preliminary Steps

Before using the region-related commands in this plugin, ensure that the regions are first created using WorldGuard. The commands listed below are for registering these pre-existing WorldGuard regions with the Snake 2.0.0 plugin. The plugin itself does not create, modify, or remove WorldGuard regions.

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

- `snake.admin`: Required for using all the administrative commands listed above.
- `snake.play`: Required for normal users to play the game.

---

## User Commands

For players with the `snake.play` permission, the following commands are available:

### Starting and Stopping the Game

- `/snakegame start`: Starts the game. Make sure you are in a lobby region linked to a game region.
- `/snakegame stop`: Stops the game. You will be returned to the lobby region.

### Game Interface and Help

- `/snakegame gui`: Opens the game's graphical user interface where you can manage various settings and view statistics.
- `/snakegame help`: Explains how to play the game, including controls and objectives.

### Personalization and Statistics

- `/snakegame color`: Opens a menu to select your snake's color. Your choice will be saved for future games.
- `/snakegame highscore`: Displays your personal highscore.

### Leaderboard

- `/snakegame leaderboard`: View the leaderboard to see how you rank against other players.
