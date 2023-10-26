package com.slimer.Region;

import com.slimer.Main.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides services for managing Snake game regions.
 */
public class RegionService {
    private static RegionService instance;
    private Logger logger;
    private Connection connection;

    private RegionService(JavaPlugin plugin) {
        initializeDatabase(plugin);
        RegionHelpers.initializeInstance(connection, logger);
    }

    /**
     * Initializes the service instance with a given JavaPlugin.
     *
     * @param plugin The JavaPlugin instance used for initialization.
     */
    public static synchronized void initializeInstance(JavaPlugin plugin) {
        if (instance == null) {
            instance = new RegionService(plugin);
        }
    }

    /**
     * Retrieves the current instance of the RegionService.
     *
     * @return The current instance of the RegionService.
     * @throws IllegalStateException if the service has not been initialized.
     */
    public static synchronized RegionService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RegionService must be initialized with a JavaPlugin instance before use.");
        }
        return instance;
    }

    /**
     * Initializes the SQLite database for region data.
     *
     * @param plugin The JavaPlugin instance used for initialization.
     */
    private void initializeDatabase(JavaPlugin plugin) {
        logger = plugin.getLogger();
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/Regions.db");
            Statement statement = connection.createStatement();
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS region_data (regionType TEXT, regionName TEXT, worldName TEXT, linkID INTEGER DEFAULT NULL, x INTEGER DEFAULT NULL, y INTEGER DEFAULT NULL, z INTEGER DEFAULT NULL)");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "An error occurred while initializing the SQLite database.", e);
        }
    }

    /**
     * Migrates region data from a YML file to an SQLite database. The function reads from a 'Regions.yml' file and
     * populates the SQLite database with this data. After successful migration, the YML file is renamed to indicate
     * that migration has been completed. The function reports the status of each part of the migration through logging.
     *
     * @param main The main plugin instance, used to access the plugin's data folder.
     */
    public void migrateRegionsFromYmlToSql(Main main) {
        File dataFolder = main.getDataFolder();
        File ymlFile = new File(dataFolder, "Regions.yml");
        String absolutePath = ymlFile.getAbsolutePath();
        logger.log(Level.INFO, "[RegionService.java] Starting Region YML to SQL migration. Looking for YML file in: " + absolutePath);
        if (!ymlFile.exists()) {
            logger.log(Level.INFO, "[RegionService.java] No Region YML file found at " + absolutePath + ". Migration skipped.");
            return;
        }
        YamlConfiguration ymlConfig = YamlConfiguration.loadConfiguration(ymlFile);
        logger.log(Level.INFO, "[RegionService.java] YML file loaded. Starting lobby zones migration.");
        ConfigurationSection lobbySection = ymlConfig.getConfigurationSection("lobbyzones");
        for (String regionName : Objects.requireNonNull(lobbySection).getKeys(false)) {
            String worldName = lobbySection.getString(regionName + ".world");
            registerNewRegion("lobby", regionName, Objects.requireNonNull(worldName));
            logger.log(Level.INFO, "[RegionService.java] Migrated lobby zone: " + regionName);
        }
        logger.log(Level.INFO, "[RegionService.java] Starting game zones migration.");
        ConfigurationSection gameSection = ymlConfig.getConfigurationSection("gamezones");
        for (String regionName : Objects.requireNonNull(gameSection).getKeys(false)) {
            String worldName = gameSection.getString(regionName + ".world");
            registerNewRegion("game", regionName, Objects.requireNonNull(worldName));
            logger.log(Level.INFO, "[RegionService.java] Migrated game zone: " + regionName);
        }
        logger.log(Level.INFO, "[RegionService.java] Starting linked regions migration.");
        ConfigurationSection linkedSection = ymlConfig.getConfigurationSection("Linked");
        for (String linkID : Objects.requireNonNull(linkedSection).getKeys(false)) {
            String lobbyRegion = linkedSection.getString(linkID + ".LobbyRegion");
            String gameRegion = linkedSection.getString(linkID + ".GameRegion");
            linkRegions(Objects.requireNonNull(lobbyRegion), Objects.requireNonNull(gameRegion));
            logger.log(Level.INFO, "[RegionService.java] Linked regions: " + lobbyRegion + " and " + gameRegion);
            String[] lobbyTP = Objects.requireNonNull(linkedSection.getString(linkID + ".lobbyTP")).split(",");
            String[] gameTP = Objects.requireNonNull(linkedSection.getString(linkID + ".gameTP")).split(",");
            setRegionCoordinates(lobbyRegion, Integer.parseInt(lobbyTP[0]), Integer.parseInt(lobbyTP[1]), Integer.parseInt(lobbyTP[2]));
            setRegionCoordinates(gameRegion, Integer.parseInt(gameTP[0]), Integer.parseInt(gameTP[1]), Integer.parseInt(gameTP[2]));
            logger.log(Level.INFO, "[RegionService.java] Migrated TP coordinates for linked regions: " + lobbyRegion + " and " + gameRegion);
        }
        File backupFile = new File(dataFolder, "MIGRATED_Regions.yml.bak");
        if (!ymlFile.renameTo(backupFile)) {
            logger.log(Level.WARNING, "[RegionService.java] Failed to rename Regions.yml to MIGRATED_Regions.yml.bak in folder: " + dataFolder.getAbsolutePath());
        } else {
            logger.log(Level.INFO, "[RegionService.java] Successfully renamed Regions.yml to MIGRATED_Regions.yml.bak in folder: " + dataFolder.getAbsolutePath());
        }
    }

    /**
     * Closes the SQLite database connection.
     * Only used during server shutdown or reloads and is invoked in the `onDisable` method of the main class.
     * Should not be used during any other processes.
     */
    public void closeDatabase() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "An error occurred while closing the SQLite database connection.", e);
        }
    }

    /**
     * Registers a new region in the database.
     *
     * @param regionType The type of the region (e.g., game, lobby).
     * @param regionName The name of the region.
     * @param worldName  The world in which the region resides.
     * @return true if the region was registered successfully, false otherwise.
     */
    public boolean registerNewRegion(String regionType, String regionName, String worldName) {
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO region_data (regionType, regionName, worldName, linkID, x, y, z) VALUES (?, ?, ?, NULL, NULL, NULL, NULL)");
            statement.setString(1, regionType.toLowerCase());
            statement.setString(2, regionName.toLowerCase());
            statement.setString(3, worldName.toLowerCase());
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "An error occurred while registering new region.", e);
            return false;
        }
    }

    /**
     * Unregisters a region from the database.
     *
     * @param regionName The name of the region to be unregistered.
     * @return true if the region was unregistered successfully, false otherwise.
     */
    public boolean unregisterRegion(String regionName) {
        try {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM region_data WHERE regionName = ?");
            statement.setString(1, regionName.toLowerCase());
            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "An error occurred while unregistering the region.", e);
            return false;
        }
    }

    /**
     * Links two regions together in the database.
     * Uses a transaction to ensure atomicity.
     *
     * @param regionName1 The name of the first region.
     * @param regionName2 The name of the second region.
     * @return true if the regions were linked successfully, false otherwise.
     */
    public boolean linkRegions(String regionName1, String regionName2) {
        try {
            connection.setAutoCommit(false);
            int newLinkID = RegionHelpers.getInstance().generateUniqueLinkID();
            PreparedStatement statement = connection.prepareStatement("UPDATE region_data SET linkID = ? WHERE regionName = ?");
            statement.setInt(1, newLinkID);
            statement.setString(2, regionName1.toLowerCase());
            statement.executeUpdate();
            statement.setString(2, regionName2.toLowerCase());
            statement.executeUpdate();
            connection.commit();
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                logger.log(Level.SEVERE, "An error occurred during rollback.", rollbackEx);
            }
            logger.log(Level.SEVERE, "An error occurred while linking regions.", e);
            return false;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "An error occurred while resetting auto-commit setting.", e);
            }
        }
    }

    /**
     * Unlinks two regions that are currently linked in the database.
     *
     * @param regionName1 The name of the first region.
     * @param regionName2 The name of the second region.
     * @return true if the regions were unlinked successfully, false otherwise.
     */
    public boolean unlinkRegions(String regionName1, String regionName2) {
        try {
            PreparedStatement statement = connection.prepareStatement("UPDATE region_data SET linkID = NULL WHERE regionName = ? OR regionName = ?");
            statement.setString(1, regionName1.toLowerCase());
            statement.setString(2, regionName2.toLowerCase());
            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "An error occurred while unlinking regions.", e);
            return false;
        }
    }

    /**
     * Sets or updates the TP (teleport) coordinates of a region in the database.
     *
     * @param regionName The name of the region.
     * @param x          The x-coordinate.
     * @param y          The y-coordinate.
     * @param z          The z-coordinate.
     * @return true if the coordinates were set successfully, false otherwise.
     */
    public boolean setRegionCoordinates(String regionName, int x, int y, int z) {
        try {
            PreparedStatement statement = connection.prepareStatement("UPDATE region_data SET x = ?, y = ?, z = ? WHERE regionName = ?");
            statement.setInt(1, x);
            statement.setInt(2, y);
            statement.setInt(3, z);
            statement.setString(4, regionName.toLowerCase());
            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "An error occurred while setting the coordinates for the region.", e);
            return false;
        }
    }
}