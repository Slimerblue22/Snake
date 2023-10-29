package com.slimer.Game;

import com.slimer.Main.Main;
import com.slimer.Region.RegionHelpers;
import com.slimer.Util.DebugManager;
import com.slimer.Util.MusicManager;
import com.slimer.Util.PlayerData;
import com.slimer.Region.WGHelpers;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
 * The `GameManager` class manages game-related logic and components for the Minecraft snake game.
 * It is responsible for handling player initialization, game start and stop, scoring, music, and various game events.
 * This class is a central component that connects various game-related subsystems and manages the game's lifecycle.
 * <p>
 * Last updated: V2.0.3
 * @author Slimerblue22
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
     * @param playerSnakes         A map associating players with their corresponding SnakeCreation objects.
     * @param playerLobbyLocations A map associating players with their lobby locations.
     * @param plugin               The JavaPlugin instance representing the game's main plugin.
     * @param isMusicEnabled       A boolean flag indicating whether music is enabled in the game.
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
    public void startGame(Player player, Location gameLocation, Location lobbyLocation) {
        DebugManager.log(DebugManager.Category.GAME_MANAGER, "Starting game for player " + player.getName());
        SnakeCreation snake = initializeGameAndPlayer(player, gameLocation, lobbyLocation);
        initializeBossBar(player);
        initializeMovement(player);
        initializeGameEndConditions(player);
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
        DebugManager.log(DebugManager.Category.GAME_MANAGER, "Initializing game and player " + player.getName());

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);

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
        DebugManager.log(DebugManager.Category.GAME_MANAGER, "Initializing boss bar for player " + player.getName());

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
        DebugManager.log(DebugManager.Category.GAME_MANAGER, "Initializing movement for player " + player.getName());

        playerInputHandler.startMonitoring(player);
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
     */
    private void initializeGameEndConditions(Player player) {
        DebugManager.log(DebugManager.Category.GAME_MANAGER, "Initializing game end conditions for player " + player.getName());

        GameEndConditionsHandler gameEndConditionsHandler = new GameEndConditionsHandler(this, player, plugin);
        BukkitRunnable gameEndEvents = new BukkitRunnable() {
            @Override
            public void run() {
                gameEndConditionsHandler.runGameEndEventsChecks();
            }
        };
        gameEndEvents.runTaskTimer(plugin, 0L, 0L);
        this.gameEndConditionsHandler.put(player, gameEndEvents);
    }

    /**
     * Initializes the apples in the game world for the given player.
     *
     * @param player       The player for whom to spawn the apples
     * @param gameLocation The location where apples are to be spawned
     * @param snake        The SnakeCreation object for the current game
     */
    private void initializeApples(Player player, Location gameLocation, SnakeCreation snake) {
        DebugManager.log(DebugManager.Category.GAME_MANAGER, "Initializing apples for player " + player.getName());

        Main mainPlugin = (Main) plugin;
        int maxApples = mainPlugin.getMaxApplesPerGame();
        List<Apple> applesForPlayer = playerApples.computeIfAbsent(player, k -> new ArrayList<>());
        int applesToSpawn = maxApples - applesForPlayer.size();

        for (int i = 0; i < applesToSpawn; i++) {
            Apple apple = new Apple((JavaPlugin) plugin, this);
            apple.spawnWithName(gameLocation, gameLocation.getBlockY(), player.getName());
            applesForPlayer.add(apple);
        }

        BukkitRunnable appleCollectionTask = new BukkitRunnable() {
            @Override
            public void run() {
                appleCollectionManager.checkAndCollectApple(snake.getSheepEntity(), player, (JavaPlugin) plugin);
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
        if (isMusicEnabled && isPlayerMusicToggledOn(player)) {
            DebugManager.log(DebugManager.Category.GAME_MANAGER, "Initializing music for player " + player.getName());
            Objects.requireNonNull(musicManager).startMusic(player);
        }
    }

    /**
     * Stops the ongoing game for the given player and performs cleanup operations.
     *
     * @param player The player for whom the game is to be stopped.
     */
    public void stopGame(Player player, String reason) {
        DebugManager.log(DebugManager.Category.GAME_MANAGER, "Stopping game for player " + player.getName());
        int score = updateAndSavePlayerScore(player);

        sendGameOverMessage(player, score, reason);
        hideAndRemoveBossBar(player);
        teleportPlayerToLobby(player);
        cancelScheduledTasks(player);
        clearAppleData(player);
        stopMusicForPlayer(player);
        destroySnakeAndClearData(player);
    }

    /**
     * Destroys the snake and clears game-related data for the given player.
     *
     * @param player The player whose game data is to be cleared.
     */
    private void destroySnakeAndClearData(Player player) {
        DebugManager.log(DebugManager.Category.GAME_MANAGER, "Destroying snake and clearing data for player " + player.getName());

        SnakeCreation snake = playerSnakes.get(player);
        if (snake != null) {
            snake.destroy();
        }

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
        DebugManager.log(DebugManager.Category.GAME_MANAGER, "Updating and saving score for player " + player.getName());

        int score = playerScores.getOrDefault(player, 0);
        PlayerData.getInstance((JavaPlugin) plugin).setHighScore(player, score);
        playerScores.remove(player);

        return score;
    }

    /**
     * Sends a "Game Over" message along with the player's score.
     *
     * @param player The player to whom the game over message is to be sent.
     * @param score  The score of the player.
     * @param reason The reason for the game being ended.
     */
    private void sendGameOverMessage(Player player, int score, String reason) {
        DebugManager.log(DebugManager.Category.GAME_MANAGER, "Sending game over message to player " + player.getName() + " with score: " + score);

        Component gameOverMessage = Component.text("Game Over!", NamedTextColor.RED)
                .append(Component.newline())
                .append(Component.text("Reason: ", NamedTextColor.RED))
                .append(Component.text(reason, NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Your score: ", NamedTextColor.RED))
                .append(Component.text(String.valueOf(score), NamedTextColor.GOLD));
        player.sendMessage(gameOverMessage);
    }

    /**
     * Hides and removes the boss bar for the given player.
     *
     * @param player The player for whom to hide and remove the boss bar.
     */
    private void hideAndRemoveBossBar(Player player) {
        DebugManager.log(DebugManager.Category.GAME_MANAGER, "Hiding and removing boss bar for player " + player.getName());

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
        DebugManager.log(DebugManager.Category.GAME_MANAGER, "Teleporting player " + player.getName() + " to lobby");

        Location lobbyLocation = playerLobbyLocations.get(player);
        if (lobbyLocation != null) {
            player.teleport(lobbyLocation);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
    }

    /**
     * Cancels any scheduled tasks related to the given player.
     *
     * @param player The player for whom to cancel scheduled tasks.
     */
    private void cancelScheduledTasks(Player player) {
        DebugManager.log(DebugManager.Category.GAME_MANAGER, "Cancelling scheduled tasks for player " + player.getName());

        BukkitRunnable movementTask = movementTasks.get(player);
        if (movementTask != null) {
            movementTask.cancel();
        }
        movementTasks.remove(player);

        BukkitRunnable gameEndEvents = gameEndConditionsHandler.get(player);
        if (gameEndEvents != null) {
            gameEndEvents.cancel();
        }
        gameEndConditionsHandler.remove(player);

        BukkitRunnable appleCollectionTask = appleCollectionTasks.get(player);
        if (appleCollectionTask != null) {
            appleCollectionTask.cancel();
        }
        appleCollectionTasks.remove(player);
    }

    /**
     * Clears apple-related data for the given player.
     *
     * @param player The player whose apple data is to be cleared.
     */
    private void clearAppleData(Player player) {
        DebugManager.log(DebugManager.Category.GAME_MANAGER, "Clearing apple data for player " + player.getName());

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
        if (isMusicEnabled) {
            DebugManager.log(DebugManager.Category.GAME_MANAGER, "Stopping music for player " + player.getName());
            Objects.requireNonNull(musicManager).stopMusic(player);
        }
    }

    /**
     * Stops all ongoing games and clears all game-related data. Typically used during server shutdown.
     */
    public void stopAllGames() {
        DebugManager.log(DebugManager.Category.GAME_MANAGER, "Stopping all ongoing games");

        destroyAllSnakesAndTeleportPlayers();
        clearAllLobbyLocations();
        cancelAllMovementTasks();
        cancelAllAppleCollectionTasks();
        clearAllApples();
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
        DebugManager.log(DebugManager.Category.GAME_MANAGER, "Adding segment to snake for player " + player.getName());
        SnakeCreation snake = getSnakeForPlayer(player);

        if (snake != null) {
            Vector lastPosition = snakeMovement.getLastPositionOfLastSegmentOrHead(player);

            if (lastPosition != null) {
                snake.addSegment(lastPosition, player, plugin);
                resetUTurnStatus(player);  // Reset the U-turn flag for this player
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
        DebugManager.log(DebugManager.Category.GAME_MANAGER, "Handling disconnect for player " + player.getName());

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
            DebugManager.log(DebugManager.Category.GAME_MANAGER, "Handling reconnect for player " + player.getName());
            handleTeleportToLobby(player, uuid);
        }
    }

    /**
     * Handles teleporting the player back to the lobby.
     *
     * @param player The player who has reconnected.
     * @param uuid   The UUID of the reconnected player.
     */
    private void handleTeleportToLobby(Player player, UUID uuid) {
        RegionHelpers regionHelpers = RegionHelpers.getInstance();
        World world = player.getWorld();
        Location loc = player.getLocation();
        String worldName = world.getName();

        for (String regionName : regionHelpers.getAllRegisteredRegionNames()) {
            if (checkAndTeleportPlayer(player, uuid, worldName, loc, regionName)) {
                break;
            }
        }
    }

    /**
     * Checks if the player is in a game region and teleports them to the lobby if so.
     *
     * @param player        The player.
     * @param uuid          The UUID of the player.
     * @param loc           The location of the player.
     * @param regionName    The name of the region.
     * @return true if the player was teleported, false otherwise.
     */
    private boolean checkAndTeleportPlayer(Player player, UUID uuid, String worldName, Location loc, String regionName) {
        RegionHelpers regionHelpers = RegionHelpers.getInstance();
        WGHelpers wgHelpers = WGHelpers.getInstance();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        if (wgHelpers.areCoordinatesInWGRegion(worldName, regionName, x, y, z)) {
            String regionType = regionHelpers.getRegionType(regionName);

            if ("game".equalsIgnoreCase(regionType)) {
                String linkedLobbyRegion = regionHelpers.getLinkedRegion(regionName);

                if (linkedLobbyRegion != null) {
                    World lobbyWorld = regionHelpers.getRegionWorld(linkedLobbyRegion);
                    Location lobbyTeleportLocation = regionHelpers.getRegionTeleportLocation(linkedLobbyRegion, lobbyWorld);

                    if (lobbyTeleportLocation != null) {
                        player.teleport(lobbyTeleportLocation);
                        disconnectedPlayerUUIDs.remove(uuid);
                        return true;
                    }
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
        DebugManager.log(DebugManager.Category.GAME_MANAGER, "Updating score for player " + player.getName());

        playerScores.put(player, playerScores.getOrDefault(player, 0) + 1);
        updateBossBarForPlayer(player);
    }

    /**
     * Updates the boss bar with the player's score for the given player.
     *
     * @param player The player whose boss bar should be updated.
     */
    private void updateBossBarForPlayer(Player player) {
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
}