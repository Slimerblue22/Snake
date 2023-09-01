package com.slimer.Game;

import com.slimer.Main.Main;
import com.slimer.Util.MusicManager;
import com.slimer.Util.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Manages the game state, player interactions, and the game logic for the Snake game.
 * Serves as the central hub for coordinating game actions, acting as a gateway between
 * various game components. For instance, it receives segment information from {@code SnakeCreation}
 * and passes it to {@code SnakeMovement} for executing snake movements.
 */

public class GameManager {
    private final Map<Player, SnakeCreation> playerSnakes;
    private final Map<Player, Location> playerLobbyLocations;
    private final Map<Player, Integer> playerScores = new HashMap<>();
    private final Map<Player, Apple> playerApples = new HashMap<>();
    private final Map<Player, BukkitRunnable> collisionDetectionTasks = new HashMap<>();
    private final Plugin plugin;
    private final Map<Player, BukkitRunnable> movementTasks = new HashMap<>();
    private final Map<Player, BukkitRunnable> appleCollectionTasks = new HashMap<>();
    private PlayerInputHandler playerInputHandler;
    private SnakeMovement snakeMovement;
    private final MusicManager musicManager;
    private final boolean isMusicEnabled;

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
     * Starts the game for a player, initializing snake, tasks, and apples.
     *
     * @param player        The player for whom the game is to be started.
     * @param gameLocation  The location in the game world.
     * @param lobbyLocation The location in the lobby world.
     */
    public void startGame(Player player, Location gameLocation, Location lobbyLocation) {
        // Initialize game start with sound
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);

        // Initialize the snake for the player
        SnakeCreation snake = new SnakeCreation(gameLocation);
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

        // Initialize collision detection
        CollisionHandler collisionHandler = new CollisionHandler(this, player);
        BukkitRunnable collisionTask = new BukkitRunnable() {
            @Override
            public void run() {
                collisionHandler.runCollisionChecks();
            }
        };
        collisionTask.runTaskTimer(plugin, 0L, 0L);
        collisionDetectionTasks.put(player, collisionTask);

        // Initialize the apple for the player
        Apple apple = new Apple();
        apple.spawnWithName(gameLocation, gameLocation.getBlockY(), player.getName());
        playerApples.put(player, apple);

        // Initialize apple collection monitoring
        BukkitRunnable appleCollectionTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkAndCollectApple(snake.getSheepEntity(), player);
            }
        };
        appleCollectionTask.runTaskTimer(plugin, 0L, 0L);
        appleCollectionTasks.put(player, appleCollectionTask);

        // Start music for the player if enabled
        if (isMusicEnabled) {
            Objects.requireNonNull(musicManager).startMusic(player, "song.nbs");
        }
    }

    /**
     * Stops the game for a given player, saves their score, and cleans up resources.
     *
     * @param player The player for whom the game is to be stopped.
     */
    public void stopGame(Player player) {
        // Destroy the snake and remove player's snake entry
        SnakeCreation snake = playerSnakes.get(player);
        if (snake != null) {
            snake.destroy();
        }

        // Update and save the player's high score
        int score = playerScores.getOrDefault(player, 0);
        PlayerData playerData = PlayerData.getInstance((JavaPlugin) plugin);
        playerData.setHighScore(player, score);
        playerScores.remove(player);

        // Play sound effect for game stop
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        // Teleport the player back to the lobby after a delay
        Location lobbyLocation = playerLobbyLocations.get(player);
        if (lobbyLocation != null) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> player.teleport(lobbyLocation), 20L); // 20 ticks = 1 second
        }

        // Clear other game-related data
        playerSnakes.remove(player);
        playerLobbyLocations.remove(player);
        playerInputHandler.stopMonitoring(player);
        snakeMovement.clearTargetPosition(player);

        // Cancel and remove scheduled tasks
        BukkitRunnable movementTask = movementTasks.get(player);
        if (movementTask != null) {
            movementTask.cancel();
        }
        movementTasks.remove(player);

        // Cancel and remove collision detection task
        BukkitRunnable collisionTask = collisionDetectionTasks.get(player);
        if (collisionTask != null) {
            collisionTask.cancel();
        }
        collisionDetectionTasks.remove(player);

        // Clear apple data
        Apple apple = playerApples.get(player);
        if (apple != null) {
            apple.clear();
            playerApples.remove(player);
        }

        // Cancel and remove apple collection task
        BukkitRunnable appleCollectionTask = appleCollectionTasks.get(player);
        if (appleCollectionTask != null) {
            appleCollectionTask.cancel();
            appleCollectionTasks.remove(player);

        }
        // Stop music for the player if enabled
        if (isMusicEnabled) {
            Objects.requireNonNull(musicManager).stopMusic(player);
        }
    }

    /**
     * Stops all ongoing games and clears all game-related data.
     */
    public void stopAllGames() {
        // Clear snakes and teleport players back to lobby
        playerSnakes.clear();
        playerLobbyLocations.clear();

        // Cancel all scheduled movement tasks
        for (BukkitRunnable task : movementTasks.values()) {
            task.cancel();
        }
        movementTasks.clear();

        // Cancel all scheduled apple collection tasks
        for (BukkitRunnable task : appleCollectionTasks.values()) {
            task.cancel();
        }
        appleCollectionTasks.clear();

        // Clear all apples
        for (Apple apple : playerApples.values()) {
            apple.clear();
        }
        playerApples.clear();
    }

    // Below this point are helper methods.
    // These are used to pass information between classes connected via game manager.
    // While not the cleanest looking thing, it allows me to simplify connections between classes.

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
        SnakeCreation snake = playerSnakes.get(player);
        if (snake != null) {
            return snake.getSegments();
        }
        return null;
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
     * Checks and handles apple collection logic.
     *
     * @param sheepEntity The entity representing the snake's head.
     * @param player      The player controlling the snake.
     */
    public void checkAndCollectApple(Entity sheepEntity, Player player) {
        Apple apple = playerApples.get(player);
        Location appleLocation = apple.getLocation();
        Location sheepLocation = sheepEntity.getLocation();
        if (appleLocation != null &&
                appleLocation.getBlockX() == sheepLocation.getBlockX() &&
                appleLocation.getBlockZ() == sheepLocation.getBlockZ()) {
            handleAppleLogic(apple, sheepEntity, player);
            updatePlayerScore(player);
            addSnakeSegment(player);

            // Play level up sound
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
        }
    }

    /**
     * Handles the logic for when a snake collides with an apple.
     *
     * @param apple       The Apple object.
     * @param sheepEntity The entity representing the snake's head.
     * @param player      The player controlling the snake.
     */
    public void handleAppleLogic(Apple apple, Entity sheepEntity, Player player) {
        apple.clear();
        apple.spawnWithName(sheepEntity.getLocation(), sheepEntity.getLocation().getBlockY(), player.getName());
    }

    /**
     * Updates the score for a player.
     *
     * @param player The player whose score needs to be updated.
     */
    public void updatePlayerScore(Player player) {
        playerScores.put(player, playerScores.getOrDefault(player, 0) + 1);
    }

    /**
     * Adds a segment to the snake controlled by the given player.
     *
     * @param player The player controlling the snake.
     */
    public void addSnakeSegment(Player player) {
        SnakeCreation snake = getSnakeForPlayer(player);
        if (snake != null) {
            Vector lastPosition = snakeMovement.getLastPositionOfLastSegmentOrHead(player);
            if (lastPosition != null) {
                snake.addSegment(lastPosition);
            }
        }
    }

    /**
     * Sets the SnakeMovement handler for the GameManager.
     *
     * @param snakeMovement The SnakeMovement object to handle snake movements.
     */
    public void setSnakeMovement(SnakeMovement snakeMovement) {
        this.snakeMovement = snakeMovement;
    }
}

