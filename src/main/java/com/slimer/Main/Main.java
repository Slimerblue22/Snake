package com.slimer.Main;

import com.slimer.Game.*;
import com.slimer.Region.RegionCommandHandler;
import com.slimer.Region.RegionFileHandler;
import com.slimer.Region.RegionService;
import com.slimer.Util.DebugManager;
import com.slimer.Util.MusicManager;
import com.slimer.Util.PlayerData;
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
        songFilePath = config.getString("song-file-path", "songs/song.nbs");

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

        // Initialize BStats (Disabled until full release)
        //int pluginId = ID_HERE;
        //new Metrics(this, pluginId);

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
        new Apple();

        // Initialize GameManager and other game-related classes
        gameManager = new GameManager(playerSnakes, playerLobbyLocations, this, isMusicEnabled);
        SnakeMovement snakeMovement = new SnakeMovement(gameManager, null);
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
}
