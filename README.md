## Welcome to the 2.0 Beta Branch

### ⚠️ Important Notes

- Version 2.0 is **incompatible** with 1.0.

- Existing configuration files from 1.0 should **not** be reused; they should be regenerated during startup.

- Currently, there is no `config.yml` file.

- Many features are either incomplete or don't exist yet.

- This version should **NOT** be used in a production environment.

### Introduction

The previous version of this plugin had reached a point where it was beyond repair. Hence, I've completely overhauled the codebase with a well-thought-out plan. This version brings more internal changes than I can currently detail, but I'll cover the essentials required for testing. Specifically, I'll go over the new process for setting up zones to play the game.

### Prerequisites

- You will need WorldGuard for setup.

- WorldEdit is also required as WorldGuard depends on it.

- ProtocolLib is also required.

### Permissions

To proceed, make sure you have the `snake.admin` permission, along with the permissions to create WorldGuard regions.

---

### Region Setup

1. **Create Two WorldGuard Regions**:
   - **Explanation**: Create one region for the game itself and another for the lobby.
   - **Example Regions**: `GameZone` for the game and `LobbyZone` for the lobby.

2. **Register Regions**:
   - **Syntax**: `/snakeadmin register [region type; game or lobby] [region name] [world region is in]`
   - **Example**: To register a game region named `GameZone` in the world called `world`, run the command: `/snakeadmin register game GameZone world`.

3. **Link Regions**:
   - **Syntax**: `/snakeadmin link [region type; game or lobby] [region name] [region type; game or lobby] [region name]`
   - **Example**: To link the lobby zone `LobbyZone` with the game zone `GameZone`, run the command: `/snakeadmin link lobby LobbyZone game GameZone`.

4. **Set Teleport Locations**:
   - **Syntax**: `/snakeadmin addteleport [region type; game or lobby] [region name]`
   - **Example**: To set the teleport location for the lobby named `LobbyZone`, run the command: `/snakeadmin addteleport lobby LobbyZone`.

### Gameplay

- To start the game, users with the `snake.play` permission can run `/snakegame start` (or `/sg start`).

- To stop, use `/snakegame stop` (or `/sg stop`).

### New Features

- The movement system now relies on velocity-based movements instead of teleportation.

- To change directions, hold the 'W' key, similar to version 1.

### Other Administrative Commands

- To unregister and unlink regions: `/snakeadmin unregister` and `/snakeadmin unlink`.

- To list region information: `/snakeadmin list [lobby, game, links, or all]`