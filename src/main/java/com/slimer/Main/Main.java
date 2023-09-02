package com.slimer.Main;

import com.slimer.GUI.GameCommandGUI;
import com.slimer.Game.*;
import com.slimer.Region.RegionCommandHandler;
import com.slimer.Region.RegionFileHandler;
import com.slimer.Region.RegionService;
import com.slimer.Util.MusicManager;
import com.slimer.Util.PlayerData;
//(Disabled until full release) import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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

    /**
     * Called when the plugin is enabled.
     * This method initializes the game components such as GameManager, SnakeMovement,
     * PlayerInputHandler, and game and region command handlers. It also loads regions
     * from configuration.
     */
    @Override
    public void onEnable() {
        // Check for NoteblockAPI
        if (Bukkit.getPluginManager().isPluginEnabled("NoteBlockAPI")) {
            isMusicEnabled = true;
            new MusicManager(this); // Initialize only if NoteblockAPI is present
        }
        // Initialize game components
        initializeGameComponents();

        // Initialize region services and handlers
        initializeRegionServices();

        // Initialize PlayerData singleton
        PlayerData.getInstance(this);

        // Register the GameCommandGUI as a listener
        Bukkit.getPluginManager().registerEvents(new GameCommandGUI(), this);

        // Initialize BStats (Disabled until full release)
        //int pluginId = ID_HERE;
        //new Metrics(this, pluginId);
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
}
