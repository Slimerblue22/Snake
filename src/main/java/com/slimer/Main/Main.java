package com.slimer.Main;

import com.slimer.GUI.InventoryClickListener;
import com.slimer.Game.GameCommandHandler;
import com.slimer.Region.RegionCommandHandler;
import com.slimer.Region.RegionService;
import com.slimer.Region.WGHelpers;
import com.slimer.Util.DebugManager;
import com.slimer.Util.PlayerData;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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

    /**
     * Called when the plugin is enabled. This method initializes various plugin components
     * and services.
     */
    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        pluginVersion = this.getDescription().getVersion();
        initRegionServices();
        initPlayerData();
        //initMetrics();
        registerCommands();
        checkForUpdates();
        registerEvents();
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
        Objects.requireNonNull(getCommand("snakegame")).setExecutor(new GameCommandHandler());
        Objects.requireNonNull(getCommand("snakeregion")).setExecutor(new RegionCommandHandler());
    }

    /**
     * Checks for updates by querying the latest release from the GitHub repository.
     * It compares the current plugin version with the latest available version on GitHub
     * and logs information about the update status.
     */
    private void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL("https://api.github.com/repos/Slimerblue22/Snake/releases/latest");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    getLogger().warning("Update checker received non-OK response from GitHub: " + responseCode);
                    return;
                }

                // Read response into string
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = reader.readLine()) != null) {
                        response.append(inputLine);
                    }

                    // Remove the `V` in the GitHub version then compare
                    JSONParser parser = new JSONParser();
                    JSONObject jsonResponse = (JSONObject) parser.parse(response.toString());
                    String latestVersion = ((String) jsonResponse.get("tag_name")).replaceFirst("^V", "");

                    // Log and compare versions
                    getLogger().info("Current plugin version: " + pluginVersion); // DEV BUILD ONLY
                    getLogger().info("Latest available version: " + latestVersion); // DEV BUILD ONLY

                    if (!pluginVersion.equalsIgnoreCase(latestVersion)) {
                        getLogger().info("An update is available for the plugin. Current: " + pluginVersion + " Latest: " + latestVersion);
                    } else {
                        getLogger().info("Plugin is up to date.");
                    }
                }
            } catch (Exception e) {
                getLogger().severe("An error occurred while checking for updates: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    /**
     * Registers event listeners.
     */
    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new InventoryClickListener(), this);
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
