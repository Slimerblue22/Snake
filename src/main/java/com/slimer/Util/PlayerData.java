package com.slimer.Util;

import com.slimer.Main.Main;
import org.bukkit.DyeColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton class responsible for managing player-specific data such as high scores.
 * This class uses an SQLite database for data storage.
 */
public class PlayerData {
    private static PlayerData instance;
    private Logger logger;
    private Connection connection;

    /**
     * Private constructor for the singleton pattern.
     * Initializes file and configuration for player data.
     *
     * @param plugin The JavaPlugin instance
     */
    private PlayerData(JavaPlugin plugin) {
        initializeDatabase(plugin);
    }

    /**
     * Returns the singleton instance of PlayerData, creating it if necessary.
     * This method should be called the first time to initialize the instance with a JavaPlugin object.
     *
     * @param plugin The JavaPlugin instance used for initialization.
     * @return The singleton instance of PlayerData.
     * @throws IllegalStateException If the method is called before initialization with a JavaPlugin instance.
     */
    public static synchronized PlayerData getInstance(JavaPlugin plugin) {
        if (instance == null && plugin != null) {
            instance = new PlayerData(plugin);
        }
        if (instance == null) {
            throw new IllegalStateException("PlayerData must be initialized with a JavaPlugin instance before use.");
        }
        return instance;
    }

    /**
     * Returns the singleton instance of PlayerData.
     *
     * @return The singleton instance of PlayerData.
     * @throws IllegalStateException If the method is called before initialization with a JavaPlugin instance.
     */
    public static synchronized PlayerData getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PlayerData must be initialized with a JavaPlugin instance before use.");
        }
        return instance;
    }

    /**
     * Migrates player data from a YML file to an SQLite database. This method is intended to be called
     * during the plugin's startup phase. It looks for a "PlayerData.yml" file in the plugin's data folder,
     * reads the existing player data, and inserts it into an SQLite database. After successful migration,
     * the YML file is renamed to "MIGRATED_PlayerData.yml.bak".
     *
     * @param main The main plugin instance, used to access the plugin's data folder.
     */
    public void migrateFromYmlToSql(Main main) {
        File dataFolder = main.getDataFolder();
        File ymlFile = new File(dataFolder, "PlayerData.yml");
        String absolutePath = ymlFile.getAbsolutePath();

        logger.log(Level.INFO, "[PlayerData.java] Starting YML to SQL migration. Looking for YML file in: " + absolutePath);

        if (!ymlFile.exists()) {
            logger.log(Level.INFO, "[PlayerData.java] No YML file found at " + absolutePath + ". Migration skipped.");
            return; // No YML file to migrate from
        }

        YamlConfiguration ymlConfig = YamlConfiguration.loadConfiguration(ymlFile);
        logger.log(Level.INFO, "[PlayerData.java] YML file loaded.");

        for (String uuid : ymlConfig.getKeys(false)) {
            logger.log(Level.INFO, "[PlayerData.java] Migrating data for UUID: " + uuid);

            String name = ymlConfig.getString(uuid + ".name");
            int score = ymlConfig.getInt(uuid + ".score");
            String sheepColor = ymlConfig.getString(uuid + ".sheepColor");
            boolean musicToggle = ymlConfig.getBoolean(uuid + ".musicToggle");

            // Insert into SQLite
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO player_data (uuid, name, score, sheepColor, musicToggle) VALUES (?, ?, ?, ?, ?)")) {
                statement.setString(1, uuid);
                statement.setString(2, name);
                statement.setInt(3, score);
                statement.setString(4, sheepColor);
                statement.setInt(5, musicToggle ? 1 : 0);
                statement.executeUpdate();

                logger.log(Level.INFO, "[PlayerData.java] Data migration successful for UUID: " + uuid);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[PlayerData.java] An error occurred while migrating data for UUID: " + uuid, e);
            }
        }

        // Rename YML file to mark it as migrated
        File backupFile = new File(dataFolder, "MIGRATED_PlayerData.yml.bak");
        if (!ymlFile.renameTo(backupFile)) {
            logger.log(Level.WARNING, "[PlayerData.java] Failed to rename PlayerData.yml to MIGRATED_PlayerData.yml.bak in folder: " + dataFolder.getAbsolutePath());
        } else {
            logger.log(Level.INFO, "[PlayerData.java] Successfully renamed PlayerData.yml to MIGRATED_PlayerData.yml.bak.in folder: " + dataFolder.getAbsolutePath());
        }
    }

    /**
     * Initializes the SQLite database used for storing player data.
     *
     * @param plugin The JavaPlugin instance.
     */
    private void initializeDatabase(JavaPlugin plugin) {
        logger = plugin.getLogger();
        try {
            // Create a database connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/PlayerData.db");

            // Create a table if it doesn't exist
            Statement statement = connection.createStatement();
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_data (uuid TEXT, name TEXT, score INTEGER, sheepColor TEXT, musicToggle INTEGER)");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[PlayerData.java] An error occurred while initializing the SQLite database", e);
        }
    }

    /**
     * Fetches the high score of the given player from the SQLite database.
     *
     * @param player The player whose high score is to be fetched.
     * @return The high score.
     */
    public int getHighScore(Player player) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT score FROM player_data WHERE uuid = ?");
            statement.setString(1, player.getUniqueId().toString());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("score");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[PlayerData.java] An error occurred while fetching the high score", e);
        }
        return 0;
    }

    /**
     * Sets the high score for a given player in the SQLite database
     * if the new score is greater than the current high score.
     *
     * @param player The player whose high score is to be set.
     * @param score  The new score.
     */
    public void setHighScore(Player player, int score) {
        try {
            int currentHighScore = getHighScore(player);
            if (score > currentHighScore) {
                PreparedStatement statement = connection.prepareStatement("REPLACE INTO player_data (uuid, name, score) VALUES (?, ?, ?)");
                statement.setString(1, player.getUniqueId().toString());
                statement.setString(2, player.getName());
                statement.setInt(3, score);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[PlayerData.java] An error occurred while setting the high score", e);
        }
    }

    /**
     * Retrieves the high scores from the SQLite database for a specific page.
     * Each page contains 10 entries. The leaderboard is sorted in descending order based on the scores.
     *
     * @return A list of Map.Entry objects containing player names and their corresponding scores.
     */
    public List<Map.Entry<String, Integer>> getLeaderboard() {
        Map<String, Integer> scores = new HashMap<>();
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT name, score FROM player_data ORDER BY score DESC");
            while (resultSet.next()) {
                scores.put(resultSet.getString("name"), resultSet.getInt("score"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[PlayerData.java] An error occurred while fetching the leaderboard", e);
        }
        return new ArrayList<>(scores.entrySet());
    }

    /**
     * Retrieves a paginated leaderboard from the SQLite database with entries corresponding to the given page number.
     * Each page contains up to 10 entries. The leaderboard is sorted in descending order based on the scores.
     *
     * @param page The desired page number, starting from 1.
     * @return A list of Map.Entry objects containing player names and their corresponding scores for the specified page.
     * If the page number exceeds available pages, an empty list is returned.
     */
    public List<Map.Entry<String, Integer>> getPaginatedLeaderboard(int page) {
        List<Map.Entry<String, Integer>> allEntries = getLeaderboard();

        int start = (page - 1) * 10;
        int end = Math.min(start + 10, allEntries.size());

        if (start >= allEntries.size()) {
            return new ArrayList<>();  // Return an empty list if the starting index is beyond the list size
        }

        return allEntries.subList(start, end);
    }

    /**
     * Fetches the sheep color of the given player from the SQLite database.
     *
     * @param player The player whose sheep color is to be fetched.
     * @return The DyeColor value representing the sheep color.
     */
    public DyeColor getSheepColor(Player player) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT sheepColor FROM player_data WHERE uuid = ?");
            statement.setString(1, player.getUniqueId().toString());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String color = resultSet.getString("sheepColor");
                return color != null ? DyeColor.valueOf(color) : DyeColor.WHITE;  // Default to WHITE if NULL
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[PlayerData.java] An error occurred while fetching the sheep color", e);
        }
        return DyeColor.WHITE; // Default value
    }

    /**
     * Sets the sheep color for a given player in the SQLite database.
     *
     * @param player The player whose sheep color is to be set.
     * @param color  The new DyeColor value for the sheep color.
     */
    public void setSheepColor(Player player, DyeColor color) {
        try {
            PreparedStatement statement = connection.prepareStatement("UPDATE player_data SET sheepColor = ? WHERE uuid = ?");
            statement.setString(1, color.name());
            statement.setString(2, player.getUniqueId().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[PlayerData.java] An error occurred while setting the sheep color", e);
        }
    }

    /**
     * Retrieves the music toggle state of the given player from the SQLite database.
     *
     * @param player The player whose music toggle state is to be fetched.
     * @return The music toggle state. True means music is enabled for the player, and false means it's disabled.
     */
    public boolean getMusicToggleState(Player player) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT musicToggle FROM player_data WHERE uuid = ?");
            statement.setString(1, player.getUniqueId().toString());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int musicState = resultSet.getInt("musicToggle");
                return resultSet.wasNull() || musicState != 0;  // Default to true if NULL
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[PlayerData.java] An error occurred while fetching the music toggle state", e);
        }
        return true; // Default value
    }

    /**
     * Sets the music toggle state for a given player in the SQLite database.
     *
     * @param player The player whose music toggle state is to be set.
     * @param state  The new state for the music toggle. True means music is enabled, and false means it's disabled.
     */
    public void setMusicToggleState(Player player, boolean state) {
        try {
            PreparedStatement statement = connection.prepareStatement("UPDATE player_data SET musicToggle = ? WHERE uuid = ?");
            statement.setBoolean(1, state);
            statement.setString(2, player.getUniqueId().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[PlayerData.java] An error occurred while setting the music toggle state", e);
        }
    }
}
