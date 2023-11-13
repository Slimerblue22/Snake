package com.slimer.Main;

import com.slimer.Game.*;
import com.slimer.Region.RegionCommandHandler;
import com.slimer.Region.RegionService;
import com.slimer.Region.WGHelpers;
import com.slimer.Util.DebugManager;
import com.slimer.Util.MusicManager;
import com.slimer.Util.PlayerData;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
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
    private String songFilePath;
    private double snakeSpeed;
    private int maxPlayersPerGame;
    private int maxApplesPerGame;
    private double forceTeleportDistance;
    private double targetCloseEnoughDistance;
    private GameManager gameManager;
    private boolean isMusicEnabled = false;

    /**
     * Called when the plugin is enabled. This method initializes various plugin components
     * and services.
     */
    @Override
    public void onEnable() {
        initConfig();
        initMusic();
        initGameComponents();
        initRegionServices();
        initPlayerData();
        initMetrics();
        registerCommands();
        checkForUpdates();
    }

    /**
     * Initializes the plugin configuration by loading or setting default values.
     */
    private void initConfig() {
        this.saveDefaultConfig();
        FileConfiguration config = this.getConfig();
        songFilePath = config.getString("song-file-path", "songs/song.nbs");
        snakeSpeed = config.getDouble("snake-speed", 5.0);
        maxPlayersPerGame = config.getInt("max-players-per-game", 1);
        maxApplesPerGame = config.getInt("max-apples-per-game", 1);
        forceTeleportDistance = config.getDouble("force-teleport-distance", 1.2);
        targetCloseEnoughDistance = config.getDouble("target-close-enough-distance", 0.1);
        pluginVersion = this.getDescription().getVersion();
    }

    /**
     * Initializes music functionality, if enabled and NoteBlockAPI is available.
     */
    private void initMusic() {
        if (Bukkit.getPluginManager().isPluginEnabled("NoteBlockAPI") &&
                getConfig().getBoolean("enable-music", true)) {
            isMusicEnabled = true;
            new MusicManager(this);
        } else {
            isMusicEnabled = false;
        }
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
     * Initializes game-related components.
     */
    private void initGameComponents() {
        Map<Player, SnakeCreation> playerSnakes = new HashMap<>();
        Map<Player, Location> playerLobbyLocations = new HashMap<>();
        gameManager = new GameManager(playerSnakes, playerLobbyLocations, this, isMusicEnabled);
        SnakeMovement snakeMovement = new SnakeMovement(gameManager, null, this);
        PlayerInputHandler playerInputHandler = new PlayerInputHandler(this, gameManager);
        snakeMovement.setPlayerInputHandler(playerInputHandler);
        gameManager.setPlayerInputHandler(playerInputHandler);
        gameManager.setSnakeMovement(snakeMovement);
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
        Objects.requireNonNull(getCommand("snakegame")).setExecutor(new GameCommandHandler(gameManager, this));
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
     * Called when the plugin is disabled. Stops all active games and closes active SQL connections.
     */
    @Override
    public void onDisable() {
        gameManager.stopAllGames();
        RegionService.getInstance().closeDatabase();
        PlayerData.getInstance().closeDatabase();
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
     * Gets the speed of the snake in blocks per second.
     *
     * @return The speed of the snake in blocks per second.
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
