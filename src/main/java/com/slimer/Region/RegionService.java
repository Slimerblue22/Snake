package com.slimer.Region;

import com.slimer.Main.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides services for managing regions in the Snake game, including database initialization, region migration,
 * region registration, linking/unlinking regions, and setting region coordinates.
 * <p>
 * Last updated: V2.1.0
 * @author Slimerblue22
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
            logger.log(Level.SEVERE, "[Regions] An error occurred while initializing the SQLite database.", e);
        }
    }

    /**
     * Initiates the migration of region data from a YML file to an SQLite database.
     * The process includes verifying the existence of the YML file to ensure migration is necessary.
     * On successful verification, it triggers the migration of individual sections and linked regions,
     * and concludes by renaming the original YML file to indicate completion.
     *
     * @param main The main plugin instance, used to access the plugin's data folder.
     */
    public void migrateRegionsFromYmlToSql(Main main) {
        File dataFolder = main.getDataFolder();
        File ymlFile = new File(dataFolder, "Regions.yml");

        if (!initializeMigration(ymlFile)) {
            return;
        }

        YamlConfiguration ymlConfig = YamlConfiguration.loadConfiguration(ymlFile);

        migrateSection(ymlConfig, "lobbyzones", "lobby");
        migrateSection(ymlConfig, "gamezones", "game");
        migrateLinkedRegions(ymlConfig);

        finalizeMigration(ymlFile, dataFolder);
    }

    /**
     * Checks for the necessary conditions before beginning the migration process.
     * Verifies the existence of the YML file to avoid unnecessary migration actions.
     * Logs the process and returns a boolean indicating whether the migration should proceed.
     *
     * @param ymlFile The YML file containing region data.
     * @return true if the conditions are right for migration, false otherwise.
     */
    private boolean initializeMigration(File ymlFile) {
        String absolutePath = ymlFile.getAbsolutePath();
        logger.log(Level.INFO, "[Regions] Starting Region YML to SQL migration. Looking for YML file in: " + absolutePath);

        if (!ymlFile.exists()) {
            logger.log(Level.INFO, "[Regions] No Region YML file found at " + absolutePath + ". Migration skipped.");
            return false;
        }

        logger.log(Level.INFO, "[Regions] YML file detected. Proceeding with migration.");
        return true;
    }

    /**
     * Handles the migration of a specific section from the YML configuration to the database.
     * Iterates over each entry in the given section and performs the data transfer,
     * logging each migrated zone.
     *
     * @param ymlConfig   The loaded YML configuration object.
     * @param sectionName The section of the YML file to migrate ('lobbyzones' or 'gamezones').
     * @param regionType  The type of region to be registered during the migration ('lobby' or 'game').
     */
    private void migrateSection(YamlConfiguration ymlConfig, String sectionName, String regionType) {
        ConfigurationSection section = ymlConfig.getConfigurationSection(sectionName);
        if (section == null) {
            logger.log(Level.INFO, "[Regions] No " + sectionName + " section found. Skipping this section.");
            return;
        }

        for (String regionName : section.getKeys(false)) {
            String worldName = section.getString(regionName + ".world");
            registerNewRegion(regionType, regionName, Objects.requireNonNull(worldName));
            logger.log(Level.INFO, "[Regions] Migrated " + regionType + " zone: " + regionName);
        }
    }

    /**
     * Migrates linked region pairs and their associated teleportation coordinates from the YML configuration.
     * Processes each linked region set by creating or updating entries in the database to reflect the links.
     *
     * @param ymlConfig The loaded YML configuration object.
     */
    private void migrateLinkedRegions(YamlConfiguration ymlConfig) {
        ConfigurationSection linkedSection = ymlConfig.getConfigurationSection("Linked");
        if (linkedSection == null) {
            logger.log(Level.INFO, "[Regions] No Linked section found. Skipping linked regions migration.");
            return;
        }

        for (String linkID : linkedSection.getKeys(false)) {
            migrateLinkedRegion(linkedSection, linkID);
        }
    }

    /**
     * Migrates a single pair of linked regions based on a unique identifier from the YML configuration.
     * Updates the link between the specified lobby and game regions and migrates their teleportation coordinates.
     *
     * @param linkedSection The configuration section containing linked regions.
     * @param linkID        The identifier for the linked region set.
     */
    private void migrateLinkedRegion(ConfigurationSection linkedSection, String linkID) {
        String lobbyRegion = linkedSection.getString(linkID + ".LobbyRegion");
        String gameRegion = linkedSection.getString(linkID + ".GameRegion");
        linkRegions(Objects.requireNonNull(lobbyRegion), Objects.requireNonNull(gameRegion));
        logger.log(Level.INFO, "[Regions] Linked regions: " + lobbyRegion + " and " + gameRegion);

        migrateRegionCoordinates(linkedSection, lobbyRegion, linkID, "lobbyTP");
        migrateRegionCoordinates(linkedSection, gameRegion, linkID, "gameTP");
    }

    /**
     * Migrates the teleportation coordinates for a specified region.
     * Parses and transfers the X, Y, and Z coordinates from the YML format to the database.
     *
     * @param linkedSection The configuration section containing region coordinates.
     * @param region        The name of the region whose coordinates are being migrated.
     * @param linkID        The identifier for the linked region set.
     * @param coordinateKey The key used to retrieve the coordinate string from the configuration.
     */
    private void migrateRegionCoordinates(ConfigurationSection linkedSection, String region, String linkID, String coordinateKey) {
        String coordinates = linkedSection.getString(linkID + "." + coordinateKey);
        if (coordinates != null) {
            String[] tpCoordinates = coordinates.split(",");
            setRegionCoordinates(region, Integer.parseInt(tpCoordinates[0]), Integer.parseInt(tpCoordinates[1]), Integer.parseInt(tpCoordinates[2]));
            logger.log(Level.INFO, "[Regions] Migrated TP coordinates for " + region + ": " + Arrays.toString(tpCoordinates));
        }
    }

    /**
     * Finalizes the migration process by renaming the original YML file.
     * This prevents the migration from being run multiple times on the same data.
     * Logs the outcome of the file renaming operation.
     *
     * @param ymlFile    The original YML file that has been migrated.
     * @param dataFolder The folder containing the YML file, where the backup will be stored.
     */
    private void finalizeMigration(File ymlFile, File dataFolder) {
        File backupFile = new File(dataFolder, "MIGRATED_Regions.yml.bak");
        boolean isRenamed = ymlFile.renameTo(backupFile);
        String renameMessage = isRenamed ?
                "Successfully renamed Regions.yml to MIGRATED_Regions.yml.bak in folder: " :
                "Failed to rename Regions.yml to MIGRATED_Regions.yml.bak in folder: ";
        logger.log(isRenamed ? Level.INFO : Level.WARNING, "[Regions] " + renameMessage + dataFolder.getAbsolutePath());
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
            logger.log(Level.SEVERE, "[Regions] An error occurred while closing the SQLite database connection.", e);
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
            logger.log(Level.SEVERE, "[Regions] An error occurred while registering new region.", e);
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
            logger.log(Level.SEVERE, "[Regions] An error occurred while unregistering the region.", e);
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
                logger.log(Level.SEVERE, "[Regions] An error occurred during rollback.", rollbackEx);
            }
            logger.log(Level.SEVERE, "[Regions] An error occurred while linking regions.", e);
            return false;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "[Regions] An error occurred while resetting auto-commit setting.", e);
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
            logger.log(Level.SEVERE, "[Regions] An error occurred while unlinking regions.", e);
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
            logger.log(Level.SEVERE, "[Regions] An error occurred while setting the coordinates for the region.", e);
            return false;
        }
    }
}