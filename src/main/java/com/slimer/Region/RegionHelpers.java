package com.slimer.Region;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides helper methods for managing regions in the Snake game.
 * This class interacts with the database for operations such as checking if a region is registered,
 * retrieving region types, managing region links, generating link IDs, and fetching region data.
 * <p>
 * Last updated: V2.1.0
 *
 * @author Slimerblue22
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
        String boundaries = wgHelpers.getBoundariesOfRegion(worldName, regionName);
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