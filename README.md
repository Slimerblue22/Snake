
---

# Snake - Minecraft Spigot Plugin

**Snake** is a unique Spigot plugin that recreates the classic Snake game in Minecraft using sheep, bringing a nostalgic arcade experience into the Minecraft universe.

## Features
- **Classic Gameplay with a Minecraft Twist**: Maneuver your snake, represented by a line of sheep, to consume apples and grow in length.
- **Multiplayer Compatibility**: Multiple players can play simultaneously in the same world without interference. Players can see others' games but cannot disrupt them.
- **Personalization**: Players can customize their snake's color.
- **Highscore System**: The plugin tracks players' scores, maintaining both individual records and a global leaderboard.
- **WorldGuard Integration**: Define specific zones for gameplay and lobby areas using WorldGuard.
  - **Lobby Zone**: Defines where the game start command can be run.
  - **Game Zone**: Determines the play area for games.
- **Teleportation Commands**: Set teleport points for both the lobby and game zones to smoothly transition players between game states.
- **Customizable Game Speed**: Adjust the speed of the snake. (Note: Requires server restart to take effect).
- **Dynamic Sound**: The game is enhanced with sound effects. Please note, the plugin does not come with built-in music. Users must supply their music in .NBS (NoteBlockStudio) format.
  
## Dependencies
### Required:
- **Paper Spigot** or **AdventureAPI**: (Note: AdventureAPI is only needed if Paper Spigot is not used)
- **ProtocolLib**
- **WorldGuard**

### Optional:
- **NoteblockAPI**: Required for game music. Without this, the game won't feature music.

## Installation

1. Download the latest release of Snake from the GitHub repository.
2. Place the `.jar` file into your `plugins` folder of your Spigot server.
3. Start the server to generate the default configuration file.
4. Optionally, adjust the settings in the generated `config.yml` file as per your requirements.

## Configuration
Refer to the provided `config.yml` for configuration details:

- **nonSolidBlocks**: Blocks that should be ignored when considering what counts as a collision. (Note: Do not remove `AIR` or `PLAYER_HEAD` for proper game functionality.)
- **song-file-path**: Define the path for the game music.
- **Region names**: Names of regions for Lobby and Gamezone.
- **teleportLocations**: Set teleportation coordinates for both game and lobby zones.
- **world**: Specify the world in which the game should operate.
- **snake-speed**: Adjust the speed of the snake in ticks.

## Gameplay Disclaimer
- The game is designed to be played on level ground and does not support height elevations.

## Commands & Permissions
**Commands**:
- `/snake play`: Start the game.
- `/snake stop`: Stop the game.
- `/snake color`: Set your snake color.
- `/snake highscore`: View your highscore.
- `/snake setspeed`: (Admin) Change the snake speed.
- `/snake leaderboard`: View the leaderboard.
- `/snake lobbycoords`: (Admin) Set the lobby coordinates.
- `/snake gamecoords`: (Admin) Set the game coordinates.
- `/snake help`: Access the help command for game instructions.

**Permissions**:
- `snake.play`: Permission to play the game. 
- `snake.admin`: Administrative permissions for the plugin. 

Contributing

We appreciate community contributions to Snake! If you'd like to contribute, feel free to open an issue or submit a pull request.

This project is licensed under the MIT License
---
