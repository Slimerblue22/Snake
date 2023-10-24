package com.slimer.Main;

import com.slimer.Game.*;
import com.slimer.Region.RegionCommandHandler;
import com.slimer.Region.RegionFileHandler;
import com.slimer.Region.RegionService;
import com.slimer.Util.DebugManager;
import com.slimer.Util.MusicManager;
import com.slimer.Util.PlayerData;
import com.slimer.WIP.NewRegionCommandHandler;
import com.slimer.WIP.NewRegionService;
import com.slimer.WIP.NewWGHelpers;
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
 * Main entry point for the Snake game plugin.
 * Manages the game's initialization and various settings.
 */
public final class Main extends JavaPlugin {

    // Plugin Metadata
    private static String pluginVersion;

    // Configuration Settings
    private String songFilePath;
    private double snakeSpeed;
    private int maxPlayersPerGame;
    private int maxApplesPerGame;
    private double forceTeleportDistance;
    private double targetCloseEnoughDistance;

    // Game Management
    private GameManager gameManager;
    private boolean isMusicEnabled = false;

    /**
     * Called when the plugin is enabled.
     * Initializes all components and settings necessary for the game.
     */
    @Override
    public void onEnable() {
        // Initialize Configuration
        initConfig();

        // Initialize Music
        initMusic();

        // Initialize Game Components
        initializeGameComponents();


        // Initialize Region Services
        initializeRegionServices();

        NewRegionService.initializeInstance(this);
        NewWGHelpers.getInstance();
        //NewRegionService.getInstance().migrateRegionsFromYmlToSql(this);

        // Initialize Player Data
        initPlayerData();

        // Initialize Metrics
        initMetrics();

        // Register Commands
        registerCommands();
    }

    /**
     * Initializes the configuration settings from the config file.
     * Sets default values if the settings are not found.
     */
    private void initConfig() {
        this.saveDefaultConfig();
        FileConfiguration config = this.getConfig();
        songFilePath = config.getString("song-file-path", "songs/song.nbs"); // Default path of songs/song.nbs
        snakeSpeed = config.getDouble("snake-speed", 5.0);// Default value of 5
        maxPlayersPerGame = config.getInt("max-players-per-game", 1);  // Default value of 1
        maxApplesPerGame = config.getInt("max-apples-per-game", 1);  // Default value of 1
        forceTeleportDistance = config.getDouble("force-teleport-distance", 1.2); // Default value of 1.2
        targetCloseEnoughDistance = config.getDouble("target-close-enough-distance", 0.1); // Default value of 0.1
        pluginVersion = this.getDescription().getVersion(); // Deprecated yet no alternative, still works though
    }

    /**
     * Initializes the music settings and checks if NoteBlockAPI is present.
     * Sets the music state accordingly.
     */
    private void initMusic() {
        boolean enableMusic = getConfig().getBoolean("enable-music", true);
        if (Bukkit.getPluginManager().isPluginEnabled("NoteBlockAPI") && enableMusic) {
            isMusicEnabled = true;
            new MusicManager(this);
        } else {
            isMusicEnabled = false;
        }
    }

    /**
     * Initializes the player data and performs migration if necessary.
     */
    private void initPlayerData() {
        PlayerData.getInstance(this);
        // Run migration checks
        PlayerData.getInstance().migrateFromYmlToSql(this);
    }

    /**
     * Initializes the metrics for the plugin.
     */
    private void initMetrics() {
        int pluginId = 19729;
        new Metrics(this, pluginId);
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

        // Initialize PlayerInputHandler and pass GameManager to it
        PlayerInputHandler playerInputHandler = new PlayerInputHandler(this, gameManager);

        snakeMovement.setPlayerInputHandler(playerInputHandler);
        gameManager.setPlayerInputHandler(playerInputHandler);
        gameManager.setSnakeMovement(snakeMovement);

        // Initialize game command handler
        new GameCommandHandler(gameManager, this);
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
        new RegionCommandHandler(regionService);
    }

    /**
     * Registers all the commands used in the plugin.
     */
    private void registerCommands() {
        // Register the Debug Command
        Objects.requireNonNull(getCommand("snakedebug")).setExecutor(new DebugManager.ToggleDebugCommand());

        // Register the Game Command
        Objects.requireNonNull(getCommand("snakegame")).setExecutor(new GameCommandHandler(gameManager, this));

        // Register the Admin Command for Regions
        Objects.requireNonNull(getCommand("snakeadmin")).setExecutor(new RegionCommandHandler(RegionService.getInstance()));

        // Register the new Region Command
        Objects.requireNonNull(getCommand("snakedev")).setExecutor(new NewRegionCommandHandler());
    }

    /**
     * Called when the plugin is disabled.
     * This method stops all active games and performs any necessary cleanup logic.
     */
    @Override
    public void onDisable() {
        // Stop all active games
        gameManager.stopAllGames();
        // Close active SQL connections
        NewRegionService.getInstance().closeDatabase();
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

    /**
     * Retrieves the version of the Snake plugin.
     *
     * @return The version string of the Snake plugin.
     */
    public static String getPluginVersion() {
        return pluginVersion;
    }
}
