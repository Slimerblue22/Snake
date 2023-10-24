package com.slimer.WIP;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides helper methods for managing Snake game regions.
 */
public class NewRegionHelpers {
    private static NewRegionHelpers instance;
    private final Connection connection;
    private final Logger logger;
    private final NewWGHelpers wgHelpers = NewWGHelpers.getInstance();


    private NewRegionHelpers(Connection connection, Logger logger) {
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
            instance = new NewRegionHelpers(connection, logger);
        }
    }

    /**
     * Retrieves the current instance of the NewRegionHelpers.
     *
     * @return The current instance of the NewRegionHelpers.
     * @throws IllegalStateException if the helpers have not been initialized.
     */
    public static synchronized NewRegionHelpers getInstance() {
        if (instance == null) {
            throw new IllegalStateException("NewRegionHelpers must be initialized before use.");
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