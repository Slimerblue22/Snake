package com.slimer;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class PlayerData {
    private final File playerDataFile; // File to store player data
    private final YamlConfiguration playerDataConfig; // YAML configuration to manage player data

    public PlayerData(JavaPlugin plugin) {
        playerDataFile = new File(plugin.getDataFolder(), "PlayerData.yml"); // Initialize file
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.createNewFile(); // Create file if not exists
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile); // Load configuration
    }

    public YamlConfiguration getConfig() {
        return playerDataConfig; // Get the YAML configuration
    }

    // Get the high score of a player
    public int getHighScore(Player player) {
        return playerDataConfig.getInt(player.getUniqueId() + ".score", 0);
    }

    // Set the high score of a player if it's greater than the current high score
    public void setHighScore(Player player, int score) {
        int currentHighScore = getHighScore(player);
        if (score > currentHighScore) {
            playerDataConfig.set(player.getUniqueId() + ".score", score); // Set new score
            saveConfig(); // Save the configuration
        }
    }

    // Save the YAML configuration to file
    public void saveConfig() {
        try {
            playerDataConfig.save(playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
