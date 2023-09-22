package com.slimer.Main;

import com.slimer.Game.*;
import com.slimer.Region.RegionCommandHandler;
import com.slimer.Region.RegionFileHandler;
import com.slimer.Region.RegionService;
import com.slimer.Util.DebugManager;
import com.slimer.Util.MusicManager;
import com.slimer.Util.PlayerData;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Main class responsible for initializing and managing the Snake game plugin.
 * This class handles the enabling and disabling of the plugin, loading regions
 * from configuration, and setting up the required command handlers.
 */
public final class Main extends JavaPlugin {
    private GameManager gameManager; // Reference to the game manager
    private boolean isMusicEnabled = false; // Flag to indicate if music is enabled
    private String songFilePath;
    private double snakeSpeed;
    private int maxPlayersPerGame;
    private int maxApplesPerGame;
    private double forceTeleportDistance;
    private double targetCloseEnoughDistance;

    /**
     * Called when the plugin is enabled.
     * This method initializes the game components such as GameManager, SnakeMovement,
     * PlayerInputHandler, and game and region command handlers. It also loads regions
     * from configuration.
     */
    @Override
    public void onEnable() {
        // Load configuration
        this.saveDefaultConfig();
        FileConfiguration config = this.getConfig();
        songFilePath = config.getString("song-file-path", "songs/song.nbs"); // Default path of songs/song.nbs
        snakeSpeed = config.getDouble("snake-speed", 5.0);// Default value of 5
        maxPlayersPerGame = config.getInt("max-players-per-game", 1);  // Default value of 1
        maxApplesPerGame = config.getInt("max-apples-per-game", 1);  // Default value of 1
        forceTeleportDistance = config.getDouble("force-teleport-distance", 1.2); // Default value of 1.2
        targetCloseEnoughDistance = config.getDouble("target-close-enough-distance", 0.1); // Default value of 0.1

        // Read the 'enable-music' setting from config
        boolean enableMusic = config.getBoolean("enable-music", true); // Default to true if not set

        // Check for NoteblockAPI and the 'enable-music' setting
        if (Bukkit.getPluginManager().isPluginEnabled("NoteBlockAPI") && enableMusic) {
            isMusicEnabled = true;
            new MusicManager(this); // Initialize only if NoteblockAPI is present and music is enabled
        } else {
            isMusicEnabled = false;
        }
        // Initialize game components
        initializeGameComponents();

        // Initialize region services and handlers
        initializeRegionServices();

        // Initialize PlayerData singleton
        PlayerData.getInstance(this);

        // Initialize BStats
        int pluginId = 19729;
        new Metrics(this, pluginId);

        // Adds a command to toggle debugs on or off, disabled by default.
        Objects.requireNonNull(this.getCommand("snaketoggledebug")).setExecutor(new DebugManager.ToggleDebugCommand());

    }

    /**
     * Called when the plugin is disabled.
     * This method stops all active games and performs any necessary cleanup logic.
     */
    @Override
    public void onDisable() {
        // Stop all active games
        gameManager.stopAllGames();
    }

    /**
     * Initializes the game components including GameManager, SnakeMovement,
     * PlayerInputHandler, and the game command handler.
     */
    private void initializeGameComponents() {
        Map<Player, SnakeCreation> playerSnakes = new HashMap<>();
        Map<Player, Location> playerLobbyLocations = new HashMap<>();

        // Initialize GameManager first
        gameManager = new GameManager(playerSnakes, playerLobbyLocations, this, isMusicEnabled);

        // Now, pass the GameManager instance to Apple's constructor
        new Apple(this, gameManager);

        SnakeMovement snakeMovement = new SnakeMovement(gameManager, null, this);
        PlayerInputHandler playerInputHandler = new PlayerInputHandler(this);
        snakeMovement.setPlayerInputHandler(playerInputHandler);
        gameManager.setPlayerInputHandler(playerInputHandler);
        gameManager.setSnakeMovement(snakeMovement);

        // Initialize game command handler
        GameCommandHandler gameCommandHandler = new GameCommandHandler(gameManager, this);
        Objects.requireNonNull(getCommand("snakegame")).setExecutor(gameCommandHandler);
    }

    /**
     * Initializes the region services including the RegionService, RegionFileHandler,
     * and region command handler. It also loads regions from configuration.
     */
    private void initializeRegionServices() {
        // Initialize region services and handlers
        RegionService regionService = RegionService.getInstance();
        File regionsFile = new File(getDataFolder(), "Regions.yml");
        RegionFileHandler regionFileHandler = new RegionFileHandler(regionsFile, regionService, this);
        regionService.setRegionFileHandler(regionFileHandler);
        regionFileHandler.loadRegionsFromConfig();

        // Initialize region command handler
        RegionCommandHandler regionCommandHandler = new RegionCommandHandler(regionService);
        Objects.requireNonNull(getCommand("snakeadmin")).setExecutor(regionCommandHandler);
    }

    /**
     * Retrieves the file path of the song associated with this instance.
     * This information is used in {@code MusicManager} to retrieve the music file.
     *
     * @return A string representing the file path of the song.
     */
    public String getSongFilePath() {
        return songFilePath;
    }

    /**
     * Gets the maximum number of players allowed per game.
     *
     * @return The maximum number of players per game.
     */
    public int getMaxPlayersPerGame() {
        return maxPlayersPerGame;
    }

    /**
     * Gets the speed of the snake.
     *
     * @return The speed of the snake.
     */
    public double getSnakeSpeed() {
        return snakeSpeed;
    }

    /**
     * Retrieves the maximum number of apples allowed to be present in the game at any given time.
     *
     * @return The maximum number of apples permitted per game.
     */
    public int getMaxApplesPerGame() {
        return maxApplesPerGame;
    }

    /**
     * Retrieves the distance before a segment is forcefully teleported back to its waypoint.
     *
     * @return The distance before forceful teleportation of a segment, as a double.
     */
    public double getForceTeleportDistance() {
        return forceTeleportDistance;
    }

    /**
     * Retrieves the distance threshold for determining if the snake's segment is close enough to its target waypoint.
     *
     * @return The distance threshold for target closeness, as a double.
     */
    public double getTargetCloseEnoughDistance() {
        return targetCloseEnoughDistance;
    }
}
