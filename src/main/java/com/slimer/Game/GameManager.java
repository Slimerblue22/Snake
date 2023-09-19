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
    private final Map<Player, SnakeCreation> playerSnakes;
    private final Map<Player, Location> playerLobbyLocations;
    private final Map<Player, Integer> playerScores = new HashMap<>();
    private final Map<Player, List<Apple>> playerApples = new HashMap<>();
    private final Map<Player, BukkitRunnable> GameEndConditionsHandler = new HashMap<>();
    private final Plugin plugin;
    private final Map<Player, BukkitRunnable> movementTasks = new HashMap<>();
    private final Map<Player, BukkitRunnable> appleCollectionTasks = new HashMap<>();
    private final MusicManager musicManager;
    private final boolean isMusicEnabled;
    private final Set<UUID> disconnectedPlayerUUIDs = new HashSet<>();
    private PlayerInputHandler playerInputHandler;
    private SnakeMovement snakeMovement;
    private final Map<Player, BossBar> playerScoreBars = new HashMap<>();


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

        // Initialization of boss bar
        BossBar bossBar = BossBar.bossBar(Component.text("Score: 0"), 1.0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
        player.showBossBar(bossBar);
        playerScoreBars.put(player, bossBar);
        updateBossBarForPlayer(player);

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

        // Initialize GameEndConditionsHandler
        GameEndConditionsHandler gameEndConditionsHandler = new GameEndConditionsHandler(this, player, plugin);
        BukkitRunnable GameEndEvents = new BukkitRunnable() {
            @Override
            public void run() {
                gameEndConditionsHandler.runGameEndEventsChecks();
            }
        };
        GameEndEvents.runTaskTimer(plugin, 0L, 0L);
        GameEndConditionsHandler.put(player, GameEndEvents);

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
                checkAndCollectApple(snake.getSheepEntity(), player, finalPlugin);  // Pass the plugin instance here
            }
        };
        appleCollectionTask.runTaskTimer(plugin, 0L, 0L);
        appleCollectionTasks.put(player, appleCollectionTask);

        // Start music for the player if music is globally enabled and player has toggled music on
        if (isMusicEnabled && isPlayerMusicToggledOn(player)) {
            Objects.requireNonNull(musicManager).startMusic(player);
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

        // Send the "Game Over" message along with the player's score
        Component gameOverMessage = Component.text("Game Over! Your score: ", NamedTextColor.RED)
                .append(Component.text(score, NamedTextColor.GOLD));
        player.sendMessage(gameOverMessage);

        // Remove and hide the boss bar
        BossBar bossBar = playerScoreBars.get(player);
        if (bossBar != null) {
            player.hideBossBar(bossBar);
            playerScoreBars.remove(player);
        }

        // Teleport the player back to the lobby
        Location lobbyLocation = playerLobbyLocations.get(player);
        if (lobbyLocation != null) {
            player.teleport(lobbyLocation);
        }

        // Play sound effect for game stop
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

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

        // Cancel and remove GameEndConditionsHandler information
        BukkitRunnable GameEndEvents = GameEndConditionsHandler.get(player);
        if (GameEndEvents != null) {
            GameEndEvents.cancel();
        }
        GameEndConditionsHandler.remove(player);

        // Clear apple data
        List<Apple> apples = playerApples.getOrDefault(player, new ArrayList<>());
        for (Apple apple : apples) {
            apple.clear();
        }
        playerApples.remove(player);


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
     * Used during server shutdown as cleanup.
     */
    public void stopAllGames() {
        // Destroy all snakes, teleport players back to lobby, and remove players' snake entries
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

        // Clear teleport locations back to lobby
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
        for (List<Apple> apples : playerApples.values()) {
            for (Apple apple : apples) {
                apple.clear();
            }
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
     * Handles the actions to be taken when a player disconnects from the server.
     * This is only called when a user leaves during a game.
     *
     * @param player The player who has disconnected.
     */
    public void handlePlayerDisconnect(Player player) {
        // Store the player's UUID
        disconnectedPlayerUUIDs.add(player.getUniqueId());
    }

    /**
     * Handles the actions to be taken when a player reconnects to the server.
     * Specifically, it teleports the reconnected player back to the lobby if they had previously disconnected.
     *
     * @param player The player who has reconnected.
     */
    public void handlePlayerReconnect(Player player) {
        UUID uuid = player.getUniqueId();
        if (disconnectedPlayerUUIDs.contains(uuid)) {
            if (DebugManager.isDebugEnabled) {
                Bukkit.getLogger().info(DebugManager.getDebugMessage("[GameManager.java] A previously disconnected player has been sent back to lobby!"));
            }
            // Fetch the game region the player is currently in
            World world = player.getWorld();
            Location loc = player.getLocation();
            RegionService regionService = RegionService.getInstance();

            for (Map.Entry<String, Region> entry : regionService.getAllRegions().entrySet()) {
                String regionName = entry.getKey();
                ProtectedRegion gameRegion = regionService.getWorldGuardRegion(regionName, world);

                if (gameRegion != null && regionService.isLocationInRegion(loc, gameRegion)) {
                    RegionLink link = regionService.getRegionLink(regionName, Region.RegionType.GAME);
                    if (link != null) {
                        Location lobbyTeleportLocation = link.getLobbyTeleportLocation();
                        if (lobbyTeleportLocation != null) {
                            player.teleport(lobbyTeleportLocation);
                            disconnectedPlayerUUIDs.remove(uuid);
                            break;
                        }
                    }
                }
            }
        }
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
     * Monitors and manages the apple collection process for the player's snake.
     * <p>
     * This method checks if the snake's head, represented by the sheep entity, collides with any apples.
     * If a collision is detected, the {@link #handleAppleLogic(Apple, Entity, Player)} method is invoked to
     * manage the apple collection logic. This includes increasing the player's score, adding a snake segment,
     * playing a level-up sound, and spawning new apples.
     * </p>
     * @param sheepEntity The entity representing the snake's head.
     * @param player      The player controlling the snake.
     * @param plugin      The JavaPlugin instance for accessing game configurations.
     */
    public void checkAndCollectApple(Entity sheepEntity, Player player, JavaPlugin plugin) {
        List<Apple> apples = playerApples.getOrDefault(player, new ArrayList<>());
        List<Apple> applesToHandle = new ArrayList<>();  // Temporary list to store apples for further processing

        for (Apple apple : apples) {
            Location appleLocation = apple.getLocation();
            Location sheepLocation = sheepEntity.getLocation();

            if (appleLocation != null &&
                    appleLocation.getBlockX() == sheepLocation.getBlockX() &&
                    appleLocation.getBlockZ() == sheepLocation.getBlockZ()) {

                applesToHandle.add(apple);
            }
        }

        // Now, handle apples outside the iteration loop
        for (Apple apple : applesToHandle) {
            handleAppleLogic(apple, sheepEntity, player);
            updatePlayerScore(player);
            addSnakeSegment(player, plugin);

            // Play level up sound
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);

            // Remove the apple from the list
            apples.remove(apple);
        }

        // Get the maximum number of apples allowed
        Main mainPlugin = (Main) plugin;
        int maxApples = mainPlugin.getMaxApplesPerGame();

        // Spawn new apples based on the difference between maxApples and currentAppleCount
        int currentAppleCount = apples.size();  // Update the current count
        int applesToSpawn = maxApples - currentAppleCount;

        for (int i = 0; i < applesToSpawn; i++) {
            Apple apple = new Apple(plugin, this);
            apple.spawnWithName(sheepEntity.getLocation(), sheepEntity.getLocation().getBlockY(), player.getName());
            apples.add(apple);
        }
    }


    /**
     * Handles the logic when a snake collides with an apple.
     * <p>
     * This method is called by {@link #checkAndCollectApple(Entity, Player, JavaPlugin)} when a snake head collision
     * with an apple is detected. It takes care of the apple collection process, including removing the apple from
     * the game and spawning new apples to maintain the maximum apple count as defined in the configuration.
     * </p>
     * @param apple       The Apple object.
     * @param sheepEntity The entity representing the snake's head.
     * @param player      The player controlling the snake.
     */
    public void handleAppleLogic(Apple apple, Entity sheepEntity, Player player) {
        apple.clear();

        Main mainPlugin = (Main) plugin;
        int maxApples = mainPlugin.getMaxApplesPerGame();
        List<Apple> playerAppleList = playerApples.getOrDefault(player, new ArrayList<>());
        int applesToSpawn = maxApples - playerAppleList.size(); // Calculate the number of apples needed to reach the limit

        for (int i = 0; i < applesToSpawn; i++) {
            Apple newApple = new Apple((JavaPlugin) plugin, this);
            newApple.spawnWithName(sheepEntity.getLocation(), sheepEntity.getLocation().getBlockY(), player.getName());
            playerAppleList.add(newApple);
        }
        playerApples.put(player, playerAppleList);
    }

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
}

