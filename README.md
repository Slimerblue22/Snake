
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
- **Paper Spigot** (Or a fork of Paper)
- **ProtocolLib**
- **WorldGuard**

### Optional:
- **NoteblockAPI**: Required for game music. Without this, the game won't feature music.

---

## Snake Game Setup Guide

Follow these steps to set up the Snake Game arena using the Snake Plugin.

### Step 1: Configure the Config File

Locate your `config.yml` file.
Find the `world` section in the config file. By default, it will look like this:

   ```yaml
   # World that the game should be running in
   world: world
   ```

**Important**: Do not change `world:`. Instead, change the section after it to the name of the world you wish to use. Common world names include:
   - `world` (Overworld)
   - `world_nether` (Nether)
   - `world_the_end` (The End)

Under the `world` section, locate the `Lobby` and `Gamezone` sections:

   ```yaml
   # Region names
   Lobby: lobby
   Gamezone: gamezone
   ```

You can change the names of the regions if needed. Regions must fall under these names for the plugin to work.

### Step 2: Create WorldGuard Regions

Use the WorldGuard plugin to create two rectangular regions:
   - **Lobby Region**: This will be used for starting snake games.
   - **Game Zone Region**: This is where snake games will be played.

Players starting a snake game in the lobby will be teleported to the game zone, and after the game ends, they will be teleported back to the lobby.

### Step 3: Set Teleport Coordinates

By default, the plugin won't function until you set coordinates for both the lobby and game zone.
Use the following commands or modify the `config.yml` to set the coordinates (cords):

   - `/snake lobbycords`: Set teleport position for the lobby (within the lobby region).
   - `/snake gamecords`: Set teleport position for the game zone (within the game region).

Ensure game zone coordinates are in a flat, suitable location for the game. Incorrect locations may cause games to start and end immediately.

### Step 4: Set Permissions

Ensure players have the permission `snake.play` to participate in the snake game.
For setup, you will need `snake.admin` permission.

### Step 5: Start Playing

Your setup is complete! Players can now start and enjoy snake games within the configured regions.

---

## Configuration
Refer to the above section for game setup.

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
- `/snake gui`: Open the GUI menu.

**Permissions**:
- `snake.play`: Permission to play the game. 
- `snake.admin`: Administrative permissions for the plugin. 

Contributing

We appreciate community contributions to Snake! If you'd like to contribute, feel free to open an issue or submit a pull request.

This project is licensed under the MIT License
---
