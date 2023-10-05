package com.slimer.Game;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.slimer.Main.Main;
import com.slimer.Region.Region;
import com.slimer.Region.RegionLink;
import com.slimer.Region.RegionService;
import com.slimer.Util.DebugManager;
import com.slimer.Util.MusicManager;
import com.slimer.Util.PlayerData;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Manages the game state, player interactions, and the game logic for the Snake game.
 * Serves as the central hub for coordinating game actions, acting as a gateway between
 * various game components. For instance, it receives segment information from {@code SnakeCreation}
 * and passes it to {@code SnakeMovement} for executing snake movements.
 */
public class GameManager {

    // Player and game state mappings
    private final Map<Player, SnakeCreation> playerSnakes;
    private final Map<Player, Location> playerLobbyLocations;
    private final Map<Player, Integer> playerScores = new HashMap<>();
    private final Map<Player, List<Apple>> playerApples = new HashMap<>();
    private final Set<UUID> disconnectedPlayerUUIDs = new HashSet<>();
    private final Map<Player, BossBar> playerScoreBars = new HashMap<>();
    private final Map<Player, Boolean> playerUTurnStatus = new HashMap<>();
    private final Map<Player, String> selectedGameModes = new HashMap<>();

    // Scheduled task mappings
    private final Map<Player, BukkitRunnable> movementTasks = new HashMap<>();
    private final Map<Player, BukkitRunnable> appleCollectionTasks = new HashMap<>();
    private final Map<Player, BukkitRunnable> gameEndConditionsHandler = new HashMap<>();

    // Game settings and utilities
    private final Plugin plugin;
    private final MusicManager musicManager;
    private final boolean isMusicEnabled;
    private final AppleCollectionManager appleCollectionManager = new AppleCollectionManager(this);
    private PlayerInputHandler playerInputHandler;
    private SnakeMovement snakeMovement;

    /**
     * Constructs a new GameManager.
     *
     * @param playerSnakes         Map of players to their snakes.
     * @param playerLobbyLocations Map of players to their lobby locations.
     * @param plugin               The plugin instance.
     */
    public GameManager(Map<Player, SnakeCreation> playerSnakes, Map<Player, Location> playerLobbyLocations, JavaPlugin plugin, boolean isMusicEnabled) {
        this.playerSnakes = playerSnakes;
        this.playerLobbyLocations = playerLobbyLocations;
        this.plugin = plugin;
        if (isMusicEnabled) {
            this.musicManager = new MusicManager((Main) plugin);
        } else {
            this.musicManager = null;
        }
        this.isMusicEnabled = isMusicEnabled;
    }

    /**
     * Starts a new game for the given player by initializing various game components.
     *
     * @param player        The player for whom the game is to be started.
     * @param gameLocation  The starting location in the game world.
     * @param lobbyLocation The location in the lobby world.
     */
    public void startGame(Player player, Location gameLocation, Location lobbyLocation, String gameMode) {
        // If it's PvP mode, make some adjustments to gameLocation before initializing
        if (gameMode.equals("pvp")) {
            // This is just a simple mechanism to place the second player a few blocks away from the first player
            // Replace with something better later
            gameLocation.add(5, 0, 5);
        }
        SnakeCreation snake = initializeGameAndPlayer(player, gameLocation, lobbyLocation);
        setGameModeForPlayer(player, gameMode);
        initializeBossBar(player);
        initializeMovement(player);
        initializeGameEndConditions(player, gameMode);
        initializeApples(player, gameLocation, snake);
        initializeMusic(player);
    }

    /**
     * Initializes the game and prepares the player for a new game session.
     *
     * @param player        The player to initialize.
     * @param gameLocation  The starting location in the game world.
     * @param lobbyLocation The location in the lobby world.
     * @return Initialized SnakeCreation object.
     */
    private SnakeCreation initializeGameAndPlayer(Player player, Location gameLocation, Location lobbyLocation) {

        // Initialize game start with sound
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);

        // Initialize the snake for the player
        SnakeCreation snake = new SnakeCreation(gameLocation, player, (JavaPlugin) plugin);
        Entity sheepEntity = snake.getSheepEntity();
        if (sheepEntity != null) {
            sheepEntity.addPassenger(player);
        }
        Vector initialPosition = new Vector(
                Math.floor(gameLocation.getX()) + 0.5,
                gameLocation.getY(),
                Math.floor(gameLocation.getZ()) + 0.5
        );
        snakeMovement.initializeTargetPositionForPlayer(player, initialPosition);
        playerSnakes.put(player, snake);
        playerLobbyLocations.put(player, lobbyLocation);
        playerScores.put(player, 0);
        return snake;
    }

    /**
     * Initializes the boss bar for the given player.
     *
     * @param player The player for whom to initialize the boss bar.
     */
    private void initializeBossBar(Player player) {
        // Initialization of boss bar
        BossBar bossBar = BossBar.bossBar(Component.text("Score: 0"), 1.0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
        player.showBossBar(bossBar);
        playerScoreBars.put(player, bossBar);
        updateBossBarForPlayer(player);
    }

    /**
     * Initializes the snake movement for the given player.
     *
     * @param player The player for whom to initialize the movement.
     */
    private void initializeMovement(Player player) {
        // Initialize input and movement handling
        playerInputHandler.startMonitoring(player);

        // Movement task for the snake
        BukkitRunnable movementTask = new BukkitRunnable() {
            @Override
            public void run() {
                Vector direction = playerInputHandler.getCurrentDirection(player);
                snakeMovement.moveSnake(player, direction);
            }
        };
        movementTask.runTaskTimer(plugin, 0L, 0L);
        movementTasks.put(player, movementTask);
    }

    /**
     * Initializes the game end conditions for the given player.
     *
     * @param player   The player for whom to initialize the game end conditions.
     * @param gameMode The game mode type for the player.
     */
    private void initializeGameEndConditions(Player player, String gameMode) {
        // Initialize GameEndConditionsHandler
        GameEndConditionsHandler gameEndConditionsHandler = new GameEndConditionsHandler(this, player, plugin);
        BukkitRunnable GameEndEvents = new BukkitRunnable() {
            @Override
            public void run() {
                gameEndConditionsHandler.runGameEndEventsChecks(gameMode);
            }
        };
        GameEndEvents.runTaskTimer(plugin, 0L, 0L);
        this.gameEndConditionsHandler.put(player, GameEndEvents);
    }

    /**
     * Initializes the apples in the game world for the given player.
     *
     * @param player       The player for whom to spawn the apples.
     * @param gameLocation The location where apples are to be spawned.
     * @param snake        The SnakeCreation object for the current game.
     */
    private void initializeApples(Player player, Location gameLocation, SnakeCreation snake) {
        // Get the maximum number of apples allowed
        Main mainPlugin = (Main) plugin;
        int maxApples = mainPlugin.getMaxApplesPerGame();

        // Retrieve the apples list for the player, or create a new list if it doesn't exist
        List<Apple> applesForPlayer = playerApples.computeIfAbsent(player, k -> new ArrayList<>());

        int currentAppleCount = applesForPlayer.size();

        // Calculate the number of apples to spawn
        int applesToSpawn = maxApples - currentAppleCount;

        for (int i = 0; i < applesToSpawn; i++) {
            Apple apple = new Apple((JavaPlugin) plugin, this);
            apple.spawnWithName(gameLocation, gameLocation.getBlockY(), player.getName());
            applesForPlayer.add(apple);
        }

        // Initialize apple collection monitoring
        final JavaPlugin finalPlugin = (JavaPlugin) plugin;  // Make a final copy of the plugin instance
        BukkitRunnable appleCollectionTask = new BukkitRunnable() {
            @Override
            public void run() {
                appleCollectionManager.checkAndCollectApple(snake.getSheepEntity(), player, finalPlugin);  // Pass the plugin instance here
            }
        };
        appleCollectionTask.runTaskTimer(plugin, 0L, 0L);
        appleCollectionTasks.put(player, appleCollectionTask);
    }

    /**
     * Initializes the background music for the game if the player has it enabled.
     *
     * @param player The player for whom to start the music.
     */
    private void initializeMusic(Player player) {
        // Start music for the player if music is globally enabled and player has toggled music on
        if (isMusicEnabled && isPlayerMusicToggledOn(player)) {
            Objects.requireNonNull(musicManager).startMusic(player);
        }
    }

    /**
     * Stops the ongoing game for the given player and performs cleanup operations.
     *
     * @param player The player for whom the game is to be stopped.
     */
    public void stopGame(Player player) {
        int score = updateAndSavePlayerScore(player);
        updateAndSavePlayerScore(player);
        sendGameOverMessage(player, score);
        hideAndRemoveBossBar(player);
        teleportPlayerToLobby(player);
        cancelScheduledTasks(player);
        clearAppleData(player);
        stopMusicForPlayer(player);
        destroySnakeAndClearData(player);
        clearGameModeForPlayer(player);
    }

    /**
     * Destroys the snake and clears game-related data for the given player.
     *
     * @param player The player whose game data is to be cleared.
     */
    private void destroySnakeAndClearData(Player player) {
        // Destroy the snake and remove player's snake entry
        SnakeCreation snake = playerSnakes.get(player);
        if (snake != null) {
            snake.destroy();
        }
        // Clear other game-related data
        playerSnakes.remove(player);
        playerLobbyLocations.remove(player);
        playerInputHandler.stopMonitoring(player);
        snakeMovement.clearTargetPosition(player);
    }

    /**
     * Updates and saves the player's high score.
     *
     * @param player The player whose score is to be updated.
     * @return The score of the player.
     */
    private int updateAndSavePlayerScore(Player player) {
        // Update and save the player's high score
        int score = playerScores.getOrDefault(player, 0);
        PlayerData playerData = PlayerData.getInstance((JavaPlugin) plugin);
        playerData.setHighScore(player, score);
        playerScores.remove(player);
        return score;
    }

    /**
     * Sends a "Game Over" message along with the player's score.
     *
     * @param player The player to whom the game over message is to be sent.
     * @param score  The score of the player.
     */
    private void sendGameOverMessage(Player player, int score) {
        // Send the "Game Over" message along with the player's score
        Component gameOverMessage = Component.text("Game Over! Your score: ", NamedTextColor.RED)
                .append(Component.text(score, NamedTextColor.GOLD));
        player.sendMessage(gameOverMessage);
    }

    /**
     * Hides and removes the boss bar for the given player.
     *
     * @param player The player for whom to hide and remove the boss bar.
     */
    private void hideAndRemoveBossBar(Player player) {
        // Remove and hide the boss bar
        BossBar bossBar = playerScoreBars.get(player);
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
        playerScoreBars.remove(player);
    }

    /**
     * Teleports the given player back to the lobby.
     *
     * @param player The player to be teleported.
     */
    private void teleportPlayerToLobby(Player player) {
        // Teleport the player back to the lobby
        Location lobbyLocation = playerLobbyLocations.get(player);
        if (lobbyLocation != null) {
            player.teleport(lobbyLocation);
        }
        // Play sound effect for game stop directly after teleporting
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
    }

    /**
     * Cancels any scheduled tasks related to the given player.
     *
     * @param player The player for whom to cancel scheduled tasks.
     */
    private void cancelScheduledTasks(Player player) {
        // Cancel and remove scheduled movement tasks
        BukkitRunnable movementTask = movementTasks.get(player);
        if (movementTask != null) {
            movementTask.cancel();
        }
        movementTasks.remove(player);

        // Cancel and remove GameEndConditionsHandler information
        BukkitRunnable GameEndEvents = gameEndConditionsHandler.get(player);
        if (GameEndEvents != null) {
            GameEndEvents.cancel();
        }
        gameEndConditionsHandler.remove(player);

        // Cancel and remove apple collection task
        BukkitRunnable appleCollectionTask = appleCollectionTasks.get(player);
        if (appleCollectionTask != null) {
            appleCollectionTask.cancel();
            appleCollectionTasks.remove(player);
        }
    }

    /**
     * Clears apple-related data for the given player.
     *
     * @param player The player whose apple data is to be cleared.
     */
    private void clearAppleData(Player player) {
        // Clear apple data
        List<Apple> apples = playerApples.getOrDefault(player, new ArrayList<>());
        for (Apple apple : apples) {
            apple.clear();
        }
        playerApples.remove(player);
    }

    /**
     * Stops the background music for the given player if it is enabled.
     *
     * @param player The player for whom to stop the music.
     */
    private void stopMusicForPlayer(Player player) {
        // Stop music for the player if enabled
        if (isMusicEnabled) {
            Objects.requireNonNull(musicManager).stopMusic(player);
        }
    }

    /**
     * Stops all ongoing games and clears all game-related data. Typically used during server shutdown.
     */
    public void stopAllGames() {
        destroyAllSnakesAndTeleportPlayers();
        clearAllLobbyLocations();
        cancelAllMovementTasks();
        cancelAllAppleCollectionTasks();
        clearAllApples();
        clearAllGameModeInfo();
    }

    /**
     * Destroys all snakes and teleports all players back to the lobby.
     */
    private void destroyAllSnakesAndTeleportPlayers() {
        for (Map.Entry<Player, SnakeCreation> entry : playerSnakes.entrySet()) {
            Player player = entry.getKey();
            SnakeCreation snake = entry.getValue();

            if (snake != null) {
                snake.destroy();
            }

            // Teleport the player back to the lobby immediately
            Location lobbyLocation = playerLobbyLocations.get(player);
            if (lobbyLocation != null) {
                player.teleport(lobbyLocation);
            }
        }
        playerSnakes.clear();
    }

    /**
     * Clears all lobby locations stored for players.
     */
    private void clearAllLobbyLocations() {
        playerLobbyLocations.clear();
    }

    /**
     * Cancels all scheduled movement tasks for all players.
     */
    private void cancelAllMovementTasks() {
        for (BukkitRunnable task : movementTasks.values()) {
            task.cancel();
        }
        movementTasks.clear();
    }

    /**
     * Cancels all scheduled apple collection tasks for all players.
     */
    private void cancelAllAppleCollectionTasks() {
        for (BukkitRunnable task : appleCollectionTasks.values()) {
            task.cancel();
        }
        appleCollectionTasks.clear();
    }

    /**
     * Clears all apple data for all players.
     */
    private void clearAllApples() {
        for (List<Apple> apples : playerApples.values()) {
            for (Apple apple : apples) {
                apple.clear();
            }
        }
        playerApples.clear();
    }

    /**
     * Clears all stored game mode information for all players.
     */
    private void clearAllGameModeInfo() {
        selectedGameModes.clear();
    }

    // Helpers for getting and modifying snake segments

    /**
     * Retrieves the snake associated with a player.
     *
     * @param player The player.
     * @return The SnakeCreation object for the player, or null if none exists.
     */
    public SnakeCreation getSnakeForPlayer(Player player) {
        return playerSnakes.get(player);
    }

    /**
     * Retrieves the snake segments associated with a player.
     *
     * @param player The player.
     * @return A list of Entity objects representing the snake segments, or null if no snake exists.
     */
    public List<Entity> getSegmentsForPlayer(Player player) {
        SnakeCreation snake = getSnakeForPlayer(player);
        return snake != null ? snake.getSegments() : null;
    }

    /**
     * Adds a segment to the snake controlled by the given player.
     *
     * @param player The player controlling the snake.
     */
    public void addSnakeSegment(Player player, JavaPlugin plugin) {
        SnakeCreation snake = getSnakeForPlayer(player);
        if (snake != null) {
            Vector lastPosition = snakeMovement.getLastPositionOfLastSegmentOrHead(player);
            if (lastPosition != null) {
                snake.addSegment(lastPosition, player, plugin);

                // Reset the U-turn flag for this player
                resetUTurnStatus(player);
            }
        }
    }

    // Helpers for setting handlers, used for class connections

    /**
     * Sets the SnakeMovement handler for the GameManager.
     *
     * @param snakeMovement The SnakeMovement object to handle snake movements.
     */
    public void setSnakeMovement(SnakeMovement snakeMovement) {
        this.snakeMovement = snakeMovement;
    }

    /**
     * Sets the PlayerInputHandler for the GameManager.
     *
     * @param playerInputHandler The PlayerInputHandler to be used for handling player inputs.
     */
    public void setPlayerInputHandler(PlayerInputHandler playerInputHandler) {
        this.playerInputHandler = playerInputHandler;
    }

    /**
     * Retrieves the mapping of players to their corresponding list of apples.
     * This method provides access to the list of apples for each player,
     * which is used to track apple-related game events for individual players.
     *
     * @return A map where the key is a Player object and the value is a list of Apple objects associated with that player.
     */
    public Map<Player, List<Apple>> getPlayerApples() {
        return playerApples;
    }

    // Helpers for handling game disconnect and reconnect actions

    /**
     * Handles the actions to be taken when a player disconnects from the server.
     *
     * @param player The player who has disconnected.
     */
    public void handlePlayerDisconnect(Player player) {
        // Store the player's UUID
        disconnectedPlayerUUIDs.add(player.getUniqueId());
    }

    /**
     * Handles the actions to be taken when a player reconnects to the server.
     *
     * @param player The player who has reconnected.
     */
    public void handlePlayerReconnect(Player player) {
        UUID uuid = player.getUniqueId();
        if (disconnectedPlayerUUIDs.contains(uuid)) {
            logPlayerReconnection();
            handleTeleportToLobby(player, uuid);
        }
    }

    /**
     * Logs the reconnection of a player.
     */
    private void logPlayerReconnection() {
        if (DebugManager.isDebugEnabled) {
            Bukkit.getLogger().info(DebugManager.getDebugMessage("[GameManager.java] A previously disconnected player has been sent back to lobby!"));
        }
    }

    /**
     * Handles teleporting the player back to the lobby.
     *
     * @param player The player who has reconnected.
     * @param uuid   The UUID of the reconnected player.
     */
    private void handleTeleportToLobby(Player player, UUID uuid) {
        World world = player.getWorld();
        Location loc = player.getLocation();
        RegionService regionService = RegionService.getInstance();

        for (Map.Entry<String, Region> entry : regionService.getAllRegions().entrySet()) {
            if (checkAndTeleportPlayer(player, uuid, world, loc, regionService, entry.getKey())) {
                break;
            }
        }
    }

    /**
     * Checks if the player is in a game region and teleports them to the lobby if so.
     *
     * @param player        The player.
     * @param uuid          The UUID of the player.
     * @param world         The world the player is in.
     * @param loc           The location of the player.
     * @param regionService The RegionService instance.
     * @param regionName    The name of the region.
     * @return true if the player was teleported, false otherwise.
     */
    private boolean checkAndTeleportPlayer(Player player, UUID uuid, World world, Location loc, RegionService regionService, String regionName) {
        ProtectedRegion gameRegion = regionService.getWorldGuardRegion(regionName, world);

        if (gameRegion != null && regionService.isLocationInRegion(loc, gameRegion)) {
            RegionLink link = regionService.getRegionLink(regionName, Region.RegionType.GAME);
            if (link != null) {
                Location lobbyTeleportLocation = link.getLobbyTeleportLocation();
                if (lobbyTeleportLocation != null) {
                    player.teleport(lobbyTeleportLocation);
                    disconnectedPlayerUUIDs.remove(uuid);
                    return true;
                }
            }
        }
        return false;
    }

    // Helpers for updating scores

    /**
     * Updates the score for a player.
     *
     * @param player The player whose score needs to be updated.
     */
    public void updatePlayerScore(Player player) {
        playerScores.put(player, playerScores.getOrDefault(player, 0) + 1);
        updateBossBarForPlayer(player);
    }

    /**
     * Updates the boss bar with the player's score for the given player.
     *
     * @param player The player whose boss bar should be updated.
     */
    private void updateBossBarForPlayer(Player player) {
        // Retrieve the boss bar for the player
        BossBar bossBar = playerScoreBars.get(player);

        // If the boss bar exists, update its name with the player's score
        if (bossBar != null) {
            bossBar.name(Component.text("Score: ", NamedTextColor.GOLD).append(Component.text(playerScores.get(player), NamedTextColor.WHITE)));
        }
    }

    // Helpers for music toggles

    /**
     * Determines if the player has music toggled on.
     *
     * @param player The player whose music preference is checked.
     * @return true if the player has toggled music on, otherwise false.
     */
    private boolean isPlayerMusicToggledOn(Player player) {
        return PlayerData.getInstance((JavaPlugin) plugin).getMusicToggleState(player);
    }

    /**
     * Checks whether the music feature is globally enabled on the server.
     *
     * @return true if music is globally enabled, otherwise false.
     */
    public boolean isMusicEnabled() {
        return isMusicEnabled;
    }

    // Helpers for U-Turn check

    /**
     * Notifies the GameManager that a player has made a U-turn.
     *
     * @param player The player who made the U-turn.
     */
    public void notifyUTurn(Player player) {
        playerUTurnStatus.put(player, true);
    }

    /**
     * Resets the U-turn status for the given player.
     *
     * @param player The player whose U-turn status needs to be reset.
     */
    public void resetUTurnStatus(Player player) {
        playerUTurnStatus.put(player, false);
    }

    /**
     * Checks if the player made a U-turn.
     *
     * @param player The player to check.
     * @return True if a U-turn is detected, false otherwise.
     */
    public boolean isUTurnDetected(Player player) {
        return playerUTurnStatus.getOrDefault(player, false);
    }

    // Helpers for game mode checks

    /**
     * Sets the game mode for a specific player.
     *
     * @param player    The player for whom the game mode is being set.
     * @param gameMode  The game mode to set for the player. Valid values include "classic", "pvp", etc.
     */
    public void setGameModeForPlayer(Player player, String gameMode) {
        selectedGameModes.put(player, gameMode);
    }

    /**
     * Retrieves the game mode currently set for a specific player.
     *
     * @param player  The player whose game mode is to be retrieved.
     * @return        The game mode associated with the player, or null if no game mode has been set.
     */
    public String getGameModeForPlayer(Player player) {
        return selectedGameModes.get(player);
    }

    /**
     * Clears the game mode information for a specific player.
     * This can be used when a player leaves a game or during cleanup processes.
     *
     * @param player  The player for whom the game mode information is to be cleared.
     */
    public void clearGameModeForPlayer(Player player) {
        selectedGameModes.remove(player);
    }
}