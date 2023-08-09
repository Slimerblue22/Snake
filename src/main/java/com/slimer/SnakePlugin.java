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
    private GameManager gameManager; // Manages game sessions for players.
    private MusicManager musicManager; // Handles music-related functionalities.
    private Set<Material> nonSolidBlocks; // Set of blocks that aren't solid (used in the game mechanics).
    private WorldGuardManager worldGuardManager; // Handles interactions with the WorldGuard plugin.
    private Location gameLocation; // The starting location of the game.
    private Location lobbyLocation; // The location where players wait before starting a game.

    @Override
    public void onEnable() {
        // Initialization Checks
        saveDefaultConfig();
        if (getServer().getPluginManager().getPlugin("NoteBlockAPI") != null) {
            musicManager = new MusicManager();
            getLogger().info("NoteBlockAPI detected, music will be enabled.");
        } else {
            getLogger().info("NoteBlockAPI not found, music will be disabled.");
        }
        if (getServer().getPluginManager().getPlugin("WorldGuard") == null) {
            getLogger().severe("WorldGuard not found! Disabling SnakePlugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Location Initialization
        gameLocation = new Location(getServer().getWorld("world"), getConfig().getDouble("teleportLocations.game.x"), getConfig().getDouble("teleportLocations.game.y"), getConfig().getDouble("teleportLocations.game.z"));

        lobbyLocation = new Location(getServer().getWorld("world"), getConfig().getDouble("teleportLocations.lobby.x"), getConfig().getDouble("teleportLocations.lobby.y"), getConfig().getDouble("teleportLocations.lobby.z"));

        // Managers and Data Initialization
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        PlayerData playerData = new PlayerData(this);
        gameManager = new GameManager(this, protocolManager, playerData);
        GameLoopManager gameLoopManager = gameManager.getGameLoopManager();
        worldGuardManager = new WorldGuardManager(this);
        worldGuardManager.updateLocations();
        int snakeSpeed = this.getConfig().getInt("snake-speed", 10); // Default to 10 if not found
        gameLoopManager.setSpeed(snakeSpeed);

        // Command Executor Initialization
        SnakeCommandExecutor commandExecutor = new SnakeCommandExecutor(gameManager, playerData, worldGuardManager, this);
        Objects.requireNonNull(this.getCommand("snake")).setExecutor(commandExecutor);

        // Non-Solid Blocks Initialization
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

        // Listeners Registration
        this.getServer().getPluginManager().registerEvents(new AppleCollectionListener(this, gameManager, worldGuardManager), this);
    }

    public Set<Material> getNonSolidBlocks() {
        return nonSolidBlocks; // Returns the set of non-solid blocks used in the game.
    }

    public MusicManager getMusicManager() {
        return musicManager; // Returns the manager handling music functionalities.
    }

    public Location getGameLocation() {
        return gameLocation; // Returns the starting location of the game.
    }

    public Location getLobbyLocation() {
        return lobbyLocation; // Returns the location where players wait before starting a game.
    }

    public WorldGuardManager getWorldGuardManager() {
        return this.worldGuardManager; // Returns the manager for WorldGuard interactions.
    }
    public void setGameLocation(Location location) {
        this.gameLocation = location;
    }

    public void setLobbyLocation(Location location) {
        this.lobbyLocation = location;
    }

    @Override
    public void onDisable() {
        // Ensure that all ongoing game sessions are terminated when the plugin is disabled.
        if (gameManager != null) {
            for (Snake snake : gameManager.getAllGames()) {
                gameManager.removeGame(snake.getPlayer());
                snake.stop();
            }
        }
    }
}