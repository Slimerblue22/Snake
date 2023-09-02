package com.slimer.Util;

import org.bukkit.DyeColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
                    logger.log(Level.WARNING, "{Snake 2.0.0 Beta-1} [PlayerData.java] Failed to create PlayerData.yml file. It may already exist.");
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "{Snake 2.0.0 Beta-1} [PlayerData.java] An error occurred while creating PlayerData.yml", e);
            }
        }

        playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
    }

    /**
     * Singleton instance getter.
     *
     * @param plugin The JavaPlugin instance
     * @return The single instance of PlayerData
     */
    public static synchronized PlayerData getInstance(JavaPlugin plugin) {
        if (instance == null) {
            instance = new PlayerData(plugin);
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
     * Retrieves the top 10 high scores from the player data configuration.
     * The leaderboard is sorted in descending order based on the scores.
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
                .limit(10)
                .collect(Collectors.toList());
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
     * Saves the YAML configuration to disk.
     */
    public void saveConfig() {
        try {
            playerDataConfig.save(playerDataFile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "{Snake 2.0.0 Beta-1} [PlayerData.java] An error occurred while saving PlayerData.yml", e);
        }
    }
}
