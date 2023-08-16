package com.slimer;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SnakePlugin extends JavaPlugin {
    private GameManager gameManager;             // Manages the game sessions
    private MusicManager musicManager;           // Manages the music playback
    private Set<Material> nonSolidBlocks;        // Set of non-solid blocks as defined in the configuration
    private WorldGuardManager worldGuardManager; // Manages WorldGuard-related functionalities
    private boolean wolfChaseEnabled;            // Flag to indicate if wolf chase is enabled

    @Override
    public void onEnable() {
        // Save default configuration and read settings
        saveDefaultConfig();
        wolfChaseEnabled = getConfig().getBoolean("wolfchase");
        int snakeSpeed = this.getConfig().getInt("snake-speed", 10);
        List<String> nonSolidBlockNames = this.getConfig().getStringList("nonSolidBlocks");

        // Initialize music if NoteBlockAPI is detected
        if (getServer().getPluginManager().getPlugin("NoteBlockAPI") != null) {
            musicManager = new MusicManager();
            getLogger().info("NoteBlockAPI detected, music will be enabled.");
        } else {
            getLogger().info("NoteBlockAPI not found, music will be disabled.");
        }

        // Initialize ProtocolLib, PlayerData, GameManager, and GUI
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        PlayerData playerData = new PlayerData(this);
        gameManager = new GameManager(this, protocolManager, playerData);
        SnakeGUI snakeGUI = new SnakeGUI(this);
        getServer().getPluginManager().registerEvents(snakeGUI, this);

        // Initialize WorldGuard and load region links
        worldGuardManager = new WorldGuardManager(this);
        worldGuardManager.loadRegionLinksFile();
        worldGuardManager.loadZoneLinks();

        // Set snake speed
        GameLoopManager gameLoopManager = gameManager.getGameLoopManager();
        gameLoopManager.setSpeed(snakeSpeed);

        // Register commands
        SnakeCommandExecutor commandExecutor = new SnakeCommandExecutor(gameManager, playerData, worldGuardManager, this);
        Objects.requireNonNull(this.getCommand("snake")).setExecutor(commandExecutor);

        // Load non-solid blocks from configuration
        this.nonSolidBlocks = new HashSet<>();
        for (String blockName : nonSolidBlockNames) {
            try {
                Material material = Material.valueOf(blockName);
                nonSolidBlocks.add(material);
            } catch (IllegalArgumentException e) {
                this.getLogger().warning("Invalid block type in config: " + blockName);
            }
        }

        // Register event listener for apple collection
        this.getServer().getPluginManager().registerEvents(new AppleCollectionListener(this, gameManager, worldGuardManager), this);
    }

    // Returns the set of non-solid blocks
    public Set<Material> getNonSolidBlocks() {
        return nonSolidBlocks;
    }

    // Returns the music manager
    public MusicManager getMusicManager() {
        return musicManager;
    }

    // Returns the WorldGuard manager
    public WorldGuardManager getWorldGuardManager() {
        return this.worldGuardManager;
    }

    // Returns the wolf chase enabled status
    public boolean isWolfChaseEnabled() {
        return wolfChaseEnabled;
    }

    // Called when the plugin is disabled
    // Stops all ongoing games and cleans up resources
    @Override
    public void onDisable() {
        if (gameManager != null) {
            for (Snake snake : gameManager.getAllGames()) {
                gameManager.removeGame(snake.getPlayer());
                snake.stop();
            }
        }
    }
}