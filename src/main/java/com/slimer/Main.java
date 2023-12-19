package com.slimer;

import com.slimer.GUI.InventoryClickListener;
import com.slimer.Game.GameCommandHandler;
import com.slimer.Game.GameManager;
import com.slimer.Game.Listeners.PlayerConnectionListener;
import com.slimer.Game.Listeners.PlayerDisconnectListener;
import com.slimer.Game.SnakeManager;
import com.slimer.Region.RegionCommandHandler;
import com.slimer.Region.RegionService;
import com.slimer.Util.DebugManager;
import com.slimer.Util.PlayerData;
import com.slimer.Util.UpdateChecker;
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

    /**
     * Called when the plugin is enabled. This method initializes various plugin components
     * and services.
     */
    @Override
    public void onEnable() {
        // Saves the default configuration
        this.saveDefaultConfig();

        // Retrieves and stores the plugin version
        pluginVersion = this.getDescription().getVersion();

        // Initializes the snake manager
        SnakeManager snakeManager = new SnakeManager();

        // Initializes the game manager
        gameManager = new GameManager(snakeManager);

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

        // Checks for plugin updates
        new UpdateChecker().checkForUpdates(this);

        // Registers event listeners
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
}
