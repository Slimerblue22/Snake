package com.slimer.Region;

import com.slimer.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides services for managing regions in the Snake game, including database initialization, region migration,
 * region registration, linking/unlinking regions, and setting region coordinates.
 * <p>
 * Last updated: V2.1.0
 *
 * @author Slimerblue22
 */
public class RegionService {
    private static RegionService instance;
    private Logger logger;
    private Connection connection;

    private RegionService(JavaPlugin plugin) {
        initializeDatabase(plugin);
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
            int newLinkID = generateUniqueLinkID();
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
    /**
     * Checks if a given region is registered.
     *
     * @param regionName The name of the region to check.
     * @return true if the region is registered, false otherwise.
     */
    public boolean isRegionRegistered(String regionName) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM region_data WHERE regionName = ?");
            statement.setString(1, regionName.toLowerCase());

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[Regions] An error occurred while checking if the region is registered.", e);
        }
        return false;
    }

    /**
     * Retrieves the type of a given region.
     *
     * @param regionName The name of the region.
     * @return The type of the region, or null if not found.
     */
    public String getRegionType(String regionName) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT regionType FROM region_data WHERE regionName = ?");
            statement.setString(1, regionName.toLowerCase());

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("regionType");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[Regions] An error occurred while getting the region type.", e);
        }
        return null;
    }

    /**
     * Checks if a given region is linked to another region.
     *
     * @param regionName The name of the region to check.
     * @return true if the region is linked, false otherwise.
     */
    public boolean isRegionLinked(String regionName) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT linkID FROM region_data WHERE regionName = ?");
            statement.setString(1, regionName.toLowerCase());

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getObject("linkID") != null;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[Regions] An error occurred while checking if the region is linked.", e);
        }
        return false;
    }

    /**
     * Generates a unique link ID for linking regions.
     *
     * @return A unique link ID.
     * @throws SQLException if an SQL error occurs.
     */
    private int generateUniqueLinkID() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT MAX(linkID) as maxID FROM region_data");
        if (resultSet.next()) {
            return resultSet.getInt("maxID") + 1;
        } else {
            return 1;
        }
    }

    /**
     * Retrieves the link ID associated with a given region.
     *
     * @param regionName The name of the region.
     * @return The link ID, or null if not linked.
     */
    public Integer getLinkID(String regionName) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT linkID FROM region_data WHERE regionName = ?");
            statement.setString(1, regionName.toLowerCase());

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int linkID = resultSet.getInt("linkID");
                if (resultSet.wasNull()) {
                    return null;
                }
                return linkID;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[Regions] An error occurred while fetching the link ID.", e);
        }
        return null;
    }

    /**
     * Retrieves the name of the region that is linked to the given region.
     *
     * @param regionName The name of the region whose linked region is to be found.
     * @return The name of the linked region, or null if no linked region is found or an error occurs.
     */
    public String getLinkedRegion(String regionName) {
        try {
            Integer linkID = getLinkID(regionName);
            if (linkID == null) {
                return null;
            }

            PreparedStatement statement = connection.prepareStatement(
                    "SELECT regionName FROM region_data WHERE linkID = ? AND regionName != ?");
            statement.setInt(1, linkID);
            statement.setString(2, regionName.toLowerCase());

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("regionName");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[Regions] An error occurred while fetching the linked region.", e);
        }
        return null;
    }

    /**
     * Retrieves the teleport location for a given region.
     *
     * @param regionName The name of the region.
     * @param world      The Bukkit world where the region is located.
     * @return A Location object representing the teleport location, or null if not found.
     */
    public Location getRegionTeleportLocation(String regionName, World world) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT x, y, z FROM region_data WHERE regionName = ?");
            statement.setString(1, regionName.toLowerCase());

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int x = resultSet.getInt("x");
                if (resultSet.wasNull()) {
                    x = Integer.MIN_VALUE;
                }
                int y = resultSet.getInt("y");
                if (resultSet.wasNull()) {
                    y = Integer.MIN_VALUE;
                }
                int z = resultSet.getInt("z");
                if (resultSet.wasNull()) {
                    z = Integer.MIN_VALUE;
                }
                if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE || z == Integer.MIN_VALUE) {
                    logger.log(Level.WARNING, "[Regions] At least one coordinate value was null for region '" + regionName + "'.");
                    return null;
                } else {
                    return new Location(world, x, y, z);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[Regions] An error occurred while fetching the teleport location for region '" + regionName + "'.", e);
        }
        return null;
    }

    /**
     * Retrieves the Bukkit World object associated with a given region name.
     *
     * <p>This method queries the database to find the world name associated with the
     * specified region name. It then uses this world name to get the corresponding
     * Bukkit World object.</p>
     *
     * @param regionName The name of the region for which the world is to be retrieved.
     * @return The Bukkit World object associated with the given region name, or null if the region or world does not exist.
     */
    public World getRegionWorld(String regionName) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT worldName FROM region_data WHERE regionName = ?");
            statement.setString(1, regionName.toLowerCase());

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String worldName = resultSet.getString("worldName");
                return Bukkit.getWorld(worldName);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[Regions] An error occurred while fetching the world information.", e);
        }
        return null;
    }

    public List<String> getAllRegisteredRegionNames() {
        List<String> regionNames = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT regionName FROM region_data");
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                regionNames.add(resultSet.getString("regionName"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[Regions] An error occurred while fetching all registered region names.", e);
        }
        return regionNames;
    }

    /**
     * Fetches formatted data based on the given option and search term.
     *
     * @param option     The type of data to fetch ("game", "lobby", "links", "search").
     * @param searchTerm The search term for the "search" option.
     * @return Formatted data string based on the given option.
     */
    public Component fetchFormattedData(String option, String... searchTerm) {
        Component message;

        switch (option) {
            case "game":
                message = (fetchRegionData("game"));
                break;
            case "lobby":
                message = (fetchRegionData("lobby"));
                break;
            case "links":
                message = (fetchLinksData());
                break;
            case "search":
                if (searchTerm.length > 0) {
                    message = fetchSearchData(searchTerm[0]);
                } else {
                    message = Component.text("Please provide a region name to search.", NamedTextColor.RED);
                }
                break;
            default:
                message = Component.text("Invalid option.", NamedTextColor.RED);
                break;
        }

        return message;
    }

    /**
     * Fetches and formats data for regions that match a given search term.
     *
     * @param searchTerm The term to search for in the region names.
     * @return Formatted data string for regions matching the search term.
     */
    private Component fetchSearchData(String searchTerm) {
        Component message = Component.text("Search results for '", NamedTextColor.GRAY)
                .append(Component.text(searchTerm, NamedTextColor.GRAY))
                .append(Component.text("':\n", NamedTextColor.GRAY))
                .append(Component.text("-------------------\n", NamedTextColor.GOLD));

        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM region_data WHERE regionName = ?");
            statement.setString(1, searchTerm.toLowerCase());
            ResultSet resultSet = statement.executeQuery();

            if (!resultSet.isBeforeFirst()) {
                return message.append(Component.text("No regions found with the name '", NamedTextColor.GRAY)
                        .append(Component.text(searchTerm, NamedTextColor.GRAY))
                        .append(Component.text("'.", NamedTextColor.GRAY)));
            }

            while (resultSet.next()) {
                String regionName = resultSet.getString("regionName");
                String regionType = resultSet.getString("regionType");
                String worldName = resultSet.getString("worldName");
                int x = resultSet.getInt("x");
                int y = resultSet.getInt("y");
                int z = resultSet.getInt("z");
                int linkID = resultSet.getInt("linkID");

                message = message.append(Component.text("Name: " + regionName + "\n", NamedTextColor.GRAY))
                        .append(Component.text("Type: " + regionType + "\n", NamedTextColor.GRAY))
                        .append(Component.text("World: " + worldName + "\n", NamedTextColor.GRAY))
                        .append(Component.text("TP Location: " + (resultSet.wasNull() ? "Not set" : String.format("(%d, %d, %d)", x, y, z)) + "\n", NamedTextColor.GRAY))
                        .append(Component.text("Link ID: " + (resultSet.wasNull() ? "Not linked" : String.valueOf(linkID)) + "\n", NamedTextColor.GRAY))
                        .append(Component.text(fetchBoundariesFromWG(worldName, regionName), NamedTextColor.GRAY))
                        .append(Component.text("\n-------------------\n", NamedTextColor.GOLD));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[Regions] An error occurred while fetching the search data.", e);
        }

        return message;
    }

    /**
     * Fetches and formats data for regions of a specific type ("game" or "lobby").
     *
     * @param regionType The type of regions to fetch data for ("game" or "lobby").
     * @return Formatted data string for regions of the given type.
     */
    private Component fetchRegionData(String regionType) {
        Component message = Component.text("Data for '", NamedTextColor.GRAY)
                .append(Component.text(regionType, NamedTextColor.GRAY))
                .append(Component.text("' option:\n", NamedTextColor.GRAY))
                .append(Component.text("-------------------\n", NamedTextColor.GOLD));

        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM region_data WHERE regionType = ?");
            statement.setString(1, regionType);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String regionName = resultSet.getString("regionName");
                String worldName = resultSet.getString("worldName");
                int x = resultSet.getInt("x");
                int y = resultSet.getInt("y");
                int z = resultSet.getInt("z");
                int linkID = resultSet.getInt("linkID");

                message = message.append(Component.text("Name: " + regionName + "\n", NamedTextColor.GRAY))
                        .append(Component.text("Type: " + regionType + "\n", NamedTextColor.GRAY))
                        .append(Component.text("World: " + worldName + "\n", NamedTextColor.GRAY))
                        .append(Component.text("TP Location: " + (resultSet.wasNull() ? "Not set" : String.format("(%d, %d, %d)", x, y, z)) + "\n", NamedTextColor.GRAY))
                        .append(Component.text("Link ID: " + (resultSet.wasNull() ? "Not linked" : String.valueOf(linkID)) + "\n", NamedTextColor.GRAY))
                        .append(Component.text(fetchBoundariesFromWG(worldName, regionName), NamedTextColor.GRAY))
                        .append(Component.text("\n-------------------\n", NamedTextColor.GOLD));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[Regions] An error occurred while fetching the " + regionType + " data.", e);
        }

        return message;
    }

    /**
     * Fetches the boundaries of a given region from WorldGuard.
     *
     * @param worldName  The name of the world the region is in.
     * @param regionName The name of the region.
     * @return A string representing the boundaries of the region, or "Not available" if boundaries could not be fetched.
     */
    private String fetchBoundariesFromWG(String worldName, String regionName) {
        String boundaries = WGHelpers.getBoundariesOfRegion(worldName, regionName);
        return boundaries != null ? "Boundaries: " + boundaries : "Boundaries: Not available";
    }

    /**
     * Fetches and formats data for linked regions.
     *
     * <p>This method processes regions with non-null link IDs and identifies which regions are linked together.
     * It also includes a check to handle scenarios where the last region in the result set has an unpaired link ID,
     * reporting it as a warning. This is essential for catching data inconsistencies, especially for the last processed region.</p>
     *
     * @return Formatted data string showing which regions are linked together.
     */
    private Component fetchLinksData() {
        Component message = Component.text("Data for 'links' option:\n", NamedTextColor.GRAY)
                .append(Component.text("-------------------\n", NamedTextColor.GOLD));

        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM region_data WHERE linkID IS NOT NULL ORDER BY linkID");
            ResultSet resultSet = statement.executeQuery();

            Integer currentLinkID = null;
            String firstRegion = null;

            while (resultSet.next()) {
                int linkID = resultSet.getInt("linkID");
                String regionName = resultSet.getString("regionName");

                if (currentLinkID == null || !currentLinkID.equals(linkID)) {
                    if (firstRegion != null) {
                        message = message.append(Component.text("Warning: Region '", NamedTextColor.RED)
                                .append(Component.text(firstRegion, NamedTextColor.GRAY))
                                .append(Component.text("' with Link ID: ", NamedTextColor.RED))
                                .append(Component.text(currentLinkID, NamedTextColor.GRAY))
                                .append(Component.text(" is not linked to any other region.\n", NamedTextColor.RED))
                                .append(Component.text("-------------------\n", NamedTextColor.GOLD)));
                    }
                    currentLinkID = linkID;
                    firstRegion = regionName;
                } else {
                    if (firstRegion != null) {
                        message = message.append(Component.text("Regions '", NamedTextColor.GRAY)
                                .append(Component.text(firstRegion, NamedTextColor.GRAY))
                                .append(Component.text("' and '", NamedTextColor.GRAY))
                                .append(Component.text(regionName, NamedTextColor.GRAY))
                                .append(Component.text("' are linked with Link ID: ", NamedTextColor.GRAY))
                                .append(Component.text(linkID, NamedTextColor.GRAY))
                                .append(Component.text("\n", NamedTextColor.GRAY))
                                .append(Component.text("-------------------\n", NamedTextColor.GOLD)));
                    }
                    firstRegion = null;
                }
            }
            // Not redundant code, see javadoc for more info
            if (firstRegion != null) {
                message = message.append(Component.text("Warning: Region '", NamedTextColor.RED)
                        .append(Component.text(firstRegion, NamedTextColor.GRAY))
                        .append(Component.text("' with Link ID: ", NamedTextColor.RED))
                        .append(Component.text(currentLinkID, NamedTextColor.GRAY))
                        .append(Component.text(" is not linked to any other region.\n", NamedTextColor.RED))
                        .append(Component.text("-------------------\n", NamedTextColor.GOLD)));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[Regions] An error occurred while fetching the links data.", e);
        }

        return message;
    }
}