package com.slimer;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SnakePlugin extends JavaPlugin {
    private GameManager gameManager; // Manager for handling game logic
    private MusicManager musicManager; // Manager for handling music
    private Set<Material> nonSolidBlocks; // Set of non-solid blocks
    private WorldGuardManager worldGuardManager; // WorldGuard integration manager
    private Location gameLocation; // Location for the game
    private Location lobbyLocation; // Location for the lobby

    @Override
    public void onEnable() {
        saveDefaultConfig(); // Save default config if not exists
        // Check for NoteBlockAPI and initialize music manager if exists
        // Initialize game and lobby locations from the config
        if (getServer().getPluginManager().getPlugin("NoteBlockAPI") != null) {
            musicManager = new MusicManager();
            getLogger().info("NoteBlockAPI detected, music will be enabled.");
        } else {
            getLogger().info("NoteBlockAPI not found, music will be disabled.");
        }
        gameLocation = new Location(getServer().getWorld("world"), getConfig().getDouble("teleportLocations.game.x"), getConfig().getDouble("teleportLocations.game.y"), getConfig().getDouble("teleportLocations.game.z"));
        lobbyLocation = new Location(getServer().getWorld("world"), getConfig().getDouble("teleportLocations.lobby.x"), getConfig().getDouble("teleportLocations.lobby.y"), getConfig().getDouble("teleportLocations.lobby.z"));
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager(); // Get the protocol manager
        PlayerData playerData = new PlayerData(this); // Create PlayerData instance
        gameManager = new GameManager(this, protocolManager, playerData); // Initialize GameManager
        SnakeGUI snakeGUI = new SnakeGUI(this);
        getServer().getPluginManager().registerEvents(snakeGUI, this); // Initialize the GUI
        // Additional initialization for game loop, world guard, snake speed, command executor
        // Load non-solid blocks from the config
        // Register event listener for AppleCollection
        GameLoopManager gameLoopManager = gameManager.getGameLoopManager();
        worldGuardManager = new WorldGuardManager(this);
        worldGuardManager.updateLocations();
        int snakeSpeed = this.getConfig().getInt("snake-speed", 10);
        gameLoopManager.setSpeed(snakeSpeed);
        SnakeCommandExecutor commandExecutor = new SnakeCommandExecutor(gameManager, playerData, worldGuardManager, this);
        Objects.requireNonNull(this.getCommand("snake")).setExecutor(commandExecutor);
        this.nonSolidBlocks = new HashSet<>();
        List<String> nonSolidBlockNames = this.getConfig().getStringList("nonSolidBlocks");
        for (String blockName : nonSolidBlockNames) {
            try {
                Material material = Material.valueOf(blockName);
                nonSolidBlocks.add(material);
            } catch (IllegalArgumentException e) {
                this.getLogger().warning("Invalid block type in config: " + blockName);
            }
        }
        this.getServer().getPluginManager().registerEvents(new AppleCollectionListener(this, gameManager, worldGuardManager), this);
    }

    public Set<Material> getNonSolidBlocks() {
        return nonSolidBlocks;
    }

    public MusicManager getMusicManager() {
        return musicManager;
    }

    public Location getGameLocation() {
        return gameLocation;
    }

    public void setGameLocation(Location location) {
        this.gameLocation = location;
    }

    public Location getLobbyLocation() {
        return lobbyLocation;
    }

    public void setLobbyLocation(Location location) {
        this.lobbyLocation = location;
    }

    public WorldGuardManager getWorldGuardManager() {
        return this.worldGuardManager;
    }

    @Override
    public void onDisable() {
        // On plugin disable, remove and stop all active games
        if (gameManager != null) {
            for (Snake snake : gameManager.getAllGames()) {
                gameManager.removeGame(snake.getPlayer());
                snake.stop();
            }
        }
    }
}