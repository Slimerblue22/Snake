package com.slimer.Util;

import org.bukkit.DyeColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.slimer.Util.DebugManager.getDebugMessage;

/**
 * Singleton class responsible for managing player-specific data, such as high scores.
 */
public class PlayerData {
    private static PlayerData instance; // Singleton instance
    private final File playerDataFile; // File to store player data
    private final YamlConfiguration playerDataConfig; // Configuration to read/write YAML data
    private final Logger logger;  // Java's built-in logger

    /**
     * Private constructor for the singleton pattern.
     * Initializes file and configuration for player data.
     *
     * @param plugin The JavaPlugin instance
     */
    private PlayerData(JavaPlugin plugin) {
        logger = plugin.getLogger();  // Initialize the logger
        playerDataFile = new File(plugin.getDataFolder(), "PlayerData.yml");

        if (!playerDataFile.exists()) {
            try {
                if (!playerDataFile.createNewFile()) {
                    logger.log(Level.WARNING, getDebugMessage("[PlayerData.java] Failed to create PlayerData.yml file. It may already exist."));
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, getDebugMessage("[PlayerData.java] An error occurred while creating PlayerData.yml"), e);
            }
        }

        playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
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
     * Gets the configuration section corresponding to the given player.
     *
     * @param player The player whose configuration section is needed.
     * @return The ConfigurationSection for the player.
     */
    private ConfigurationSection getPlayerSection(Player player) {
        String path = player.getUniqueId().toString();
        if (playerDataConfig.getConfigurationSection(path) == null) {
            playerDataConfig.createSection(path); // Create section if it doesn't exist
        }
        return playerDataConfig.getConfigurationSection(path);
    }

    /**
     * Fetches the high score of the given player.
     *
     * @param player The player whose high score is to be fetched.
     * @return The high score.
     */
    public int getHighScore(Player player) {
        return getPlayerSection(player).getInt("score", 0);
    }

    /**
     * Sets the high score for a given player if the new score is greater than the current high score.
     *
     * @param player The player whose high score is to be set.
     * @param score  The new score.
     */
    public void setHighScore(Player player, int score) {
        int currentHighScore = getHighScore(player);
        if (score > currentHighScore) {
            String path = player.getUniqueId().toString();
            playerDataConfig.set(path + ".name", player.getName());
            playerDataConfig.set(path + ".score", score);
            saveConfig(); // Save the config after setting the score
        }
    }

    /**
     * Retrieves the high scores from the player data configuration for a specific page.
     * Each page contains 10 entries. The leaderboard is sorted in descending order based on the scores.
     *
     * @return A list of Map.Entry objects containing player names and their corresponding scores.
     */
    public List<Map.Entry<String, Integer>> getLeaderboard() {
        Map<String, Integer> scores = new HashMap<>();

        for (String uuid : playerDataConfig.getKeys(false)) {
            String name = playerDataConfig.getString(uuid + ".name");
            int score = playerDataConfig.getInt(uuid + ".score");
            if (name != null) {
                scores.put(name, score);
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a paginated leaderboard with entries corresponding to the given page number.
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
     * Fetches the sheep color of the given player.
     *
     * @param player The player whose sheep color is to be fetched.
     * @return The DyeColor value representing the sheep color.
     */
    public DyeColor getSheepColor(Player player) {
        String colorString = getPlayerSection(player).getString("sheepColor", "WHITE");
        return DyeColor.valueOf(colorString);
    }

    /**
     * Sets the sheep color for a given player.
     *
     * @param player The player whose sheep color is to be set.
     * @param color  The new DyeColor value for the sheep color.
     */
    public void setSheepColor(Player player, DyeColor color) {
        String path = player.getUniqueId().toString();
        playerDataConfig.set(path + ".sheepColor", color.name());
        saveConfig(); // Save the config after setting the color
    }

    /**
     * Retrieves the music toggle state of the given player.
     *
     * @param player The player whose music toggle state is to be fetched.
     * @return The music toggle state. True means music is enabled for the player, and false means it's disabled.
     */
    public boolean getMusicToggleState(Player player) {
        return getPlayerSection(player).getBoolean("musicToggle", true);
    }

    /**
     * Sets the music toggle state for a given player.
     *
     * @param player The player whose music toggle state is to be set.
     * @param state  The new state for the music toggle. True means music is enabled, and false means it's disabled.
     */
    public void setMusicToggleState(Player player, boolean state) {
        String path = player.getUniqueId().toString();
        playerDataConfig.set(path + ".musicToggle", state);
        saveConfig(); // Save the config after setting the music toggle state
    }

    /**
     * Saves the YAML configuration to disk.
     */
    public void saveConfig() {
        try {
            playerDataConfig.save(playerDataFile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, getDebugMessage("[PlayerData.java] An error occurred while saving PlayerData.yml"), e);
        }
    }
}
