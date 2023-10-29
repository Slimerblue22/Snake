package com.slimer.Region;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides helper methods for managing Snake game regions.
 */
public class RegionHelpers {
    private static RegionHelpers instance;
    private final Connection connection;
    private final Logger logger;
    private final WGHelpers wgHelpers = WGHelpers.getInstance();

    private RegionHelpers(Connection connection, Logger logger) {
        this.connection = connection;
        this.logger = logger;
    }

    /**
     * Initializes the helper instance with a given SQL Connection and Logger.
     *
     * @param connection The SQL Connection used for initialization.
     * @param logger     The Logger used for initialization.
     */
    public static synchronized void initializeInstance(Connection connection, Logger logger) {
        if (instance == null) {
            instance = new RegionHelpers(connection, logger);
        }
    }

    /**
     * Retrieves the current instance of the RegionHelpers.
     *
     * @return The current instance of the RegionHelpers.
     * @throws IllegalStateException if the helpers have not been initialized.
     */
    public static synchronized RegionHelpers getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RegionHelpers must be initialized before use.");
        }
        return instance;
    }

    /**
     * Checks if a given region is registered.
     *
     * @param regionName The name of the region to check.
     * @return true if the region is registered, false otherwise.
     */
    public boolean isRegionRegistered(String regionName) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM region_data WHERE regionName = ?");
            statement.setString(1, regionName.toLowerCase());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                return count > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "An error occurred while checking if the region is registered.", e);
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
            PreparedStatement statement = connection.prepareStatement("SELECT regionType FROM region_data WHERE regionName = ?");
            statement.setString(1, regionName.toLowerCase());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("regionType");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "An error occurred while getting the region type.", e);
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
            PreparedStatement statement = connection.prepareStatement("SELECT linkID FROM region_data WHERE regionName = ?");
            statement.setString(1, regionName.toLowerCase());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getObject("linkID") != null;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "An error occurred while checking if the region is linked.", e);
        }
        return false;
    }

    /**
     * Generates a unique link ID for linking regions.
     *
     * @return A unique link ID.
     * @throws SQLException if an SQL error occurs.
     */
    public int generateUniqueLinkID() throws SQLException {
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
            PreparedStatement statement = connection.prepareStatement("SELECT linkID FROM region_data WHERE regionName = ?");
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
            logger.log(Level.SEVERE, "An error occurred while fetching the link ID.", e);
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
            PreparedStatement statement = connection.prepareStatement("SELECT regionName FROM region_data WHERE linkID = ? AND regionName != ?");
            statement.setInt(1, linkID);
            statement.setString(2, regionName.toLowerCase());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("regionName");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "An error occurred while fetching the linked region.", e);
        }
        return null;
    }

    /**
     * Retrieves the teleport location for a given region.
     *
     * @param regionName The name of the region.
     * @return A Location object representing the teleport location, or null if not found.
     */
    public Location getRegionTeleportLocation(String regionName, World world) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT x, y, z FROM region_data WHERE regionName = ?");
            statement.setString(1, regionName.toLowerCase());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int x = resultSet.getInt("x");
                int y = resultSet.getInt("y");
                int z = resultSet.getInt("z");
                return new Location(world, x, y, z);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "An error occurred while fetching the teleport location.", e);
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
            PreparedStatement statement = connection.prepareStatement("SELECT worldName FROM region_data WHERE regionName = ?");
            statement.setString(1, regionName.toLowerCase());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String worldName = resultSet.getString("worldName");
                return Bukkit.getWorld(worldName);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "An error occurred while fetching the world information.", e);
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
            logger.log(Level.SEVERE, "An error occurred while fetching all registered region names.", e);
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
    public String fetchFormattedData(String option, String... searchTerm) {
        StringBuilder builder = new StringBuilder();
        switch (option) {
            case "game":
                builder.append(fetchRegionData("game"));
                break;
            case "lobby":
                builder.append(fetchRegionData("lobby"));
                break;
            case "links":
                builder.append(fetchLinksData());
                break;
            case "search":
                if (searchTerm.length > 0) {
                    builder.append(fetchSearchData(searchTerm[0]));
                } else {
                    builder.append("Please provide a region name to search.");
                }
                break;
            default:
                builder.append("Invalid option.");
                break;
        }
        return builder.toString();
    }

    /**
     * Fetches and formats data for regions that match a given search term.
     *
     * @param searchTerm The term to search for in the region names.
     * @return Formatted data string for regions matching the search term.
     */
    private String fetchSearchData(String searchTerm) {
        StringBuilder builder = new StringBuilder();
        builder.append("Search results for '").append(searchTerm).append("':\n");
        builder.append("-------------------\n");
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM region_data WHERE regionName = ?");
            statement.setString(1, searchTerm);
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.isBeforeFirst()) {
                builder.append("No regions found with the name '").append(searchTerm).append("'.");
                return builder.toString();
            }
            while (resultSet.next()) {
                String regionName = resultSet.getString("regionName");
                String regionType = resultSet.getString("regionType");
                String worldName = resultSet.getString("worldName");
                int x = resultSet.getInt("x");
                int y = resultSet.getInt("y");
                int z = resultSet.getInt("z");
                int linkID = resultSet.getInt("linkID");
                builder.append("Name: ").append(regionName).append("\n");
                builder.append("Type: ").append(regionType).append("\n");
                builder.append("World: ").append(worldName).append("\n");
                if (resultSet.wasNull()) {
                    builder.append("TP Location: Not set\n");
                } else {
                    builder.append("TP Location: ").append(String.format("(%d, %d, %d)", x, y, z)).append("\n");
                }
                if (resultSet.wasNull()) {
                    builder.append("Link ID: Not linked\n");
                } else {
                    builder.append("Link ID: ").append(linkID).append("\n");
                }
                String boundaries = fetchBoundariesFromWG(worldName, regionName);
                builder.append(boundaries).append("\n");
                builder.append("-------------------\n");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "An error occurred while fetching the search data.", e);
        }
        return builder.toString();
    }

    /**
     * Fetches and formats data for regions of a specific type ("game" or "lobby").
     *
     * @param regionType The type of regions to fetch data for ("game" or "lobby").
     * @return Formatted data string for regions of the given type.
     */
    private String fetchRegionData(String regionType) {
        StringBuilder builder = new StringBuilder();
        builder.append("Data for '").append(regionType).append("' option:\n");
        builder.append("-------------------\n");
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM region_data WHERE regionType = ?");
            statement.setString(1, regionType);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String regionName = resultSet.getString("regionName");
                String retrievedRegionType = resultSet.getString("regionType");
                String worldName = resultSet.getString("worldName");
                int x = resultSet.getInt("x");
                int y = resultSet.getInt("y");
                int z = resultSet.getInt("z");
                int linkID = resultSet.getInt("linkID");
                builder.append("Name: ").append(regionName).append("\n");
                builder.append("Type: ").append(retrievedRegionType).append("\n");
                builder.append("World: ").append(worldName).append("\n");
                if (resultSet.wasNull()) {
                    builder.append("TP Location: Not set\n");
                } else {
                    builder.append("TP Location: ").append(String.format("(%d, %d, %d)", x, y, z)).append("\n");
                }
                if (resultSet.wasNull()) {
                    builder.append("Link ID: Not linked\n");
                } else {
                    builder.append("Link ID: ").append(linkID).append("\n");
                }
                String boundaries = fetchBoundariesFromWG(worldName, regionName);
                builder.append(boundaries).append("\n");
                builder.append("-------------------\n");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "An error occurred while fetching the " + regionType + " data.", e);
        }
        return builder.toString();
    }

    /**
     * Fetches the boundaries of a given region from WorldGuard.
     *
     * @param worldName  The name of the world the region is in.
     * @param regionName The name of the region.
     * @return A string representing the boundaries of the region, or "Not available" if boundaries could not be fetched.
     */
    private String fetchBoundariesFromWG(String worldName, String regionName) {
        String boundaries = wgHelpers.getBoundariesOfRegion(worldName, regionName);
        if (boundaries == null) {
            return "Boundaries: Not available";
        } else {
            return "Boundaries: " + boundaries;
        }
    }

    /**
     * Fetches and formats data for linked regions.
     *
     * @return Formatted data string showing which regions are linked together.
     */
    private String fetchLinksData() {
        StringBuilder builder = new StringBuilder();
        builder.append("Data for 'links' option:\n");
        builder.append("-------------------\n");
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM region_data WHERE linkID IS NOT NULL ORDER BY linkID");
            ResultSet resultSet = statement.executeQuery();
            Integer currentLinkID = null;
            String firstRegion = null;
            while (resultSet.next()) {
                int linkID = resultSet.getInt("linkID");
                String regionName = resultSet.getString("regionName");
                if (currentLinkID == null || currentLinkID != linkID) {
                    if (firstRegion != null) {
                        builder.append("Warning: Region '").append(firstRegion).append("' with Link ID: ").append(currentLinkID).append(" is not linked to any other region.\n");
                        builder.append("-------------------\n");
                    }
                    currentLinkID = linkID;
                    firstRegion = regionName;
                } else {
                    builder.append("Regions '").append(firstRegion).append("' and '").append(regionName).append("' are linked with Link ID: ").append(linkID).append("\n");
                    builder.append("-------------------\n");
                    firstRegion = null;
                }
            }
            if (firstRegion != null) {
                builder.append("Warning: Region '").append(firstRegion).append("' with Link ID: ").append(currentLinkID).append(" is not linked to any other region.\n");
                builder.append("-------------------\n");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "An error occurred while fetching the links data.", e);
        }
        return builder.toString();
    }
}