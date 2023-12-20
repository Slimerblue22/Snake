package com.slimer;

import com.slimer.GUI.InventoryClickListener;
import com.slimer.Game.GameCommandHandler;
import com.slimer.Game.GameManager;
import com.slimer.Game.IncrementScoreCommand;
import com.slimer.Game.Listeners.PlayerConnectionListener;
import com.slimer.Game.Listeners.PlayerDisconnectListener;
import com.slimer.Game.Listeners.PlayerInputListener;
import com.slimer.Game.ScoreManager;
import com.slimer.Game.SnakeManagement.SnakeLifecycle;
import com.slimer.Game.SnakeManagement.SnakeMovement;
import com.slimer.Region.RegionCommandHandler;
import com.slimer.Region.RegionService;
import com.slimer.Util.DebugManager;
import com.slimer.Util.PlayerData;
import com.slimer.Util.UpdateChecker;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * The Main class is the entry point for the Minecraft snake game plugin.
 * It initializes game components, manages configuration settings, and registers commands.
 * This class also handles plugin enable and disable events.
 * <p>
 * Last updated: V2.1.0
 *
 * @author Slimerblue22
 */
public final class Main extends JavaPlugin {
    private static String pluginVersion;
    private GameManager gameManager;
    private double snakeSpeed;

    /**
     * Called when the plugin is enabled. This method initializes various plugin components
     * and services.
     */
    @Override
    public void onEnable() {
        // Saves the default configuration
        this.saveDefaultConfig();
        FileConfiguration config = this.getConfig();
        snakeSpeed = config.getDouble("snake-speed", 5.0);

        // Retrieves and stores the plugin version
        pluginVersion = this.getDescription().getVersion();

        // Initializes the scoring manager
        ScoreManager scoreManager = new ScoreManager();

        // Initializes the snake lifecycle manager
        SnakeLifecycle snakeLifecycle = new SnakeLifecycle();

        // Initializes the snake movement controller
        SnakeMovement snakeMovement = new SnakeMovement(snakeLifecycle, this);

        // Initializes the ProtocolLib listener
        PlayerInputListener playerInputListener = new PlayerInputListener(this, snakeMovement);

        // Initializes the game manager
        gameManager = new GameManager(snakeLifecycle, playerInputListener, scoreManager);

        // Initializes the RegionService singleton instance
        RegionService.initializeInstance(this);

        // Initializes the PlayerData singleton instance
        PlayerData.initializeInstance(this);

        // Sets up plugin metrics (Disabled during testing)
        // new Metrics(this, 19729);

        // Configures command executors for the plugin
        Objects.requireNonNull(getCommand("snakedebug")).setExecutor(new DebugManager.ToggleDebugCommand());
        Objects.requireNonNull(getCommand("snakegame")).setExecutor(new GameCommandHandler(gameManager));
        Objects.requireNonNull(getCommand("snakeregion")).setExecutor(new RegionCommandHandler());
        // TODO: This is for debugging, remove it later!
        Objects.requireNonNull(getCommand("incscore")).setExecutor(new IncrementScoreCommand(scoreManager));

        // Checks for plugin updates
        new UpdateChecker().checkForUpdates(this);

        // Registers bukkit event listeners
        getServer().getPluginManager().registerEvents(new InventoryClickListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerDisconnectListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(), this);
    }

    /**
     * Called when the plugin is disabled. Stops all active games and closes active SQL connections.
     */
    @Override
    public void onDisable() {
        gameManager.stopAllGames();
        RegionService.getInstance().closeDatabase();
        PlayerData.getInstance().closeDatabase();
    }

    /**
     * Retrieves the version of the Snake plugin.
     *
     * @return The version string of the Snake plugin.
     */
    public static String getPluginVersion() {
        return pluginVersion;
    }

    /**
     * Gets the speed of the snake in blocks per second.
     *
     * @return The speed of the snake in blocks per second.
     */
    public double getSnakeSpeed() {
        return snakeSpeed;
    }
}
