package com.slimer.Main;

import com.slimer.GUI.InventoryClickListener;
import com.slimer.Game.GameCommandHandler;
import com.slimer.Game.GameManager;
import com.slimer.Game.PlayerDisconnectListener;
import com.slimer.Region.RegionCommandHandler;
import com.slimer.Region.RegionService;
import com.slimer.Region.WGHelpers;
import com.slimer.Util.DebugManager;
import com.slimer.Util.PlayerData;
import com.slimer.Util.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * The Main class is the entry point for the Minecraft snake game plugin.
 * It initializes game components, manages configuration settings, and registers commands.
 * This class also handles plugin enable and disable events.
 * <p>
 * Last updated: V2.1.0
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
        this.saveDefaultConfig();
        pluginVersion = this.getDescription().getVersion();
        initGameComponents();
        initRegionServices();
        initPlayerData();
        //initMetrics();
        registerCommands();
        checkForUpdates();
        registerEvents();
    }

    private void initGameComponents() {
        gameManager = new GameManager();
    }

    /**
     * Initializes player data management and migration.
     */
    private void initPlayerData() {
        PlayerData.initializeInstance(this);
        PlayerData.getInstance().migrateFromYmlToSql(this);
    }

    /**
     * Initializes the bstats metrics collection for the plugin.
     */
    private void initMetrics() {
        new Metrics(this, 19729);
    }

    /**
     * Initializes region-related services and migration.
     */
    private void initRegionServices() {
        RegionService.initializeInstance(this);
        WGHelpers.getInstance();
        RegionService.getInstance().migrateRegionsFromYmlToSql(this);
    }

    /**
     * Registers plugin commands for debugging, game management, and region management.
     */
    private void registerCommands() {
        Objects.requireNonNull(getCommand("snakedebug")).setExecutor(new DebugManager.ToggleDebugCommand());
        Objects.requireNonNull(getCommand("snakegame")).setExecutor(new GameCommandHandler(gameManager));
        Objects.requireNonNull(getCommand("snakeregion")).setExecutor(new RegionCommandHandler());
    }

    /**
     * Runs the update checker.
     */
    private void checkForUpdates() {
        new UpdateChecker().checkForUpdates(this);
    }

    /**
     * Registers event listeners.
     */
    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new InventoryClickListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerDisconnectListener(gameManager), this);
    }

    /**
     * Called when the plugin is disabled. Stops all active games and closes active SQL connections.
     */
    @Override
    public void onDisable() {
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
