package com.slimer;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class PlayerData {
    private final File playerDataFile;
    private final YamlConfiguration playerDataConfig;
    // Constructor for the PlayerData class.
    // It creates a new YamlConfiguration file or loads the existing one.
    public PlayerData(JavaPlugin plugin) {
        playerDataFile = new File(plugin.getDataFolder(), "PlayerData.yml");
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
    }
    // This method returns the YamlConfiguration object that was created or loaded in the constructor.
    public YamlConfiguration getConfig() {
        return playerDataConfig;
    }
    public int getHighScore(Player player) {
        return playerDataConfig.getInt(player.getUniqueId() + ".score", 0);
    }

    public void setHighScore(Player player, int score) {
        int currentHighScore = getHighScore(player);
        if (score > currentHighScore) {
            playerDataConfig.set(player.getUniqueId() + ".score", score);
            saveConfig();
        }
    }

    // This method saves any changes made to the YamlConfiguration object back to the file.
    public void saveConfig() {
        try {
            playerDataConfig.save(playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
