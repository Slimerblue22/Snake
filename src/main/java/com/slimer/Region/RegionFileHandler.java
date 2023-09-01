package com.slimer.Region;

import com.slimer.Main.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.getPluginManager;

/**
 * The RegionFileHandler class is responsible for managing the regions and region links within the game. It provides
 * methods to handle file operations such as loading, saving, and manipulating regions and their corresponding links
 * in the game. The class interacts with the regions.yml file, facilitating actions like registering, unregistering,
 * linking, and unlinking regions, as well as converting between location objects and their string representations.
 */
public class RegionFileHandler {
    private final File regionsFile;
    private final RegionService regionService;
    private final Main plugin;
    private FileConfiguration regionsConfig;

    /**
     * Constructs a RegionFileHandler with the specified regions file, region service, and main plugin.
     *
     * @param regionsFile   The file object representing the regions.yml file.
     * @param regionService The service responsible for managing regions within the game.
     * @param plugin        The main plugin object.
     */
    public RegionFileHandler(File regionsFile, RegionService regionService, Main plugin) {
        this.regionsFile = regionsFile;
        this.regionService = regionService;
        this.plugin = plugin;
        loadRegionsFile();
    }

    /**
     * Loads the regions.yml file from disk. If the file does not exist, it creates a new file.
     * If an error occurs during file creation, appropriate warnings and errors are logged.
     */
    public void loadRegionsFile() {
        if (!regionsFile.exists()) {
            try {
                if (!regionsFile.createNewFile()) {
                    getLogger().warning("Regions.yml file already exists!");
                }
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Could not create Regions.yml file!", e);
            }
        }
        regionsConfig = YamlConfiguration.loadConfiguration(regionsFile);
    }

    /**
     * Saves the regions.yml file to disk. If an error occurs during the saving process,
     * an error message is logged, and the stack trace is printed.
     */
    public void saveRegionsFile() {
        try {
            regionsConfig.save(regionsFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save Regions.yml file!", e);
        }
    }

    /**
     * Saves a specific region to the regions.yml file. The region details are organized under
     * a key prefix based on the region type (either "lobbyzones" or "gamezones").
     *
     * @param region The region object containing details to be saved to the file.
     */
    public void saveRegionToFile(Region region) {
        String keyPrefix = region.getType() == Region.RegionType.LOBBY ? "lobbyzones" : "gamezones";
        ConfigurationSection section = regionsConfig.getConfigurationSection(keyPrefix);
        if (section == null) {
            section = regionsConfig.createSection(keyPrefix);
        }
        ConfigurationSection regionSection = section.createSection(region.getName());
        regionSection.set("world", region.getWorldName());
        saveRegionsFile(); // Save changes to file
    }

    /**
     * Removes the specified region from the regions.yml file. The method iterates through both "lobbyzones" and "gamezones"
     * sections to find and remove the region with the given name.
     *
     * @param regionName The name of the region to be removed.
     */
    public void removeRegionFromFile(String regionName) {
        for (String keyPrefix : new String[]{"lobbyzones", "gamezones"}) { // Iterate through both lobby and game zones
            ConfigurationSection section = regionsConfig.getConfigurationSection(keyPrefix);
            if (section != null && section.contains(regionName)) {
                regionsConfig.set(keyPrefix + "." + regionName, null); // Remove region if found
                saveRegionsFile(); // Save changes to file
                return;
            }
        }
    }

    /**
     * Saves the details of a RegionLink to the regions.yml file. The information is stored
     * under the "Linked" section. If the region link already exists in the file, it updates
     * the existing entry; otherwise, a new entry is created.
     *
     * @param link The RegionLink object containing the details to be saved.
     */
    public void saveRegionLinkToFile(RegionLink link) {
        ConfigurationSection linkedSection = regionsConfig.getConfigurationSection("Linked");
        if (linkedSection == null) {
            linkedSection = regionsConfig.createSection("Linked"); // Create "Linked" section if not present
        }

        String keyToModify = findExistingLinkKey(linkedSection, link);

        if (keyToModify != null) {
            updateExistingLink(linkedSection, keyToModify, link); // Update existing link if found
        } else {
            createNewLink(linkedSection, link); // Create new link if not found
        }

        saveRegionsFile(); // Save changes to file
    }

    /**
     * Finds the key for an existing link in the linkedSection.
     *
     * @param linkedSection The ConfigurationSection containing linked regions.
     * @param link          The RegionLink to find.
     * @return The key of the existing link or null if not found.
     */
    private String findExistingLinkKey(ConfigurationSection linkedSection, RegionLink link) {
        for (String key : linkedSection.getKeys(false)) {
            if (Objects.equals(linkedSection.getString(key + ".LobbyRegion"), link.getLobbyRegionName()) &&
                    Objects.equals(linkedSection.getString(key + ".GameRegion"), link.getGameRegionName())) {
                return key; // Return key if link found
            }
        }
        return null; // Return null if link not found
    }

    /**
     * Updates the existing link in the linkedSection.
     *
     * @param linkedSection The ConfigurationSection containing linked regions.
     * @param key           The key of the link to be updated.
     * @param link          The RegionLink containing the new details.
     */
    private void updateExistingLink(ConfigurationSection linkedSection, String key, RegionLink link) {
        linkedSection.set(key + ".lobbyTP", locationToString(link.getLobbyTeleportLocation()));
        linkedSection.set(key + ".gameTP", locationToString(link.getGameTeleportLocation())); // Update existing link
    }

    /**
     * Retrieves the next available link number that can be used as a key in the linkedSection.
     * It iterates through the existing keys and finds the first available number.
     *
     * @param linkedSection The ConfigurationSection containing linked regions.
     * @return The next available link number.
     */
    private int getNextAvailableLinkNumber(ConfigurationSection linkedSection) {
        int linkNumber = 1;
        while (linkedSection.contains("Link" + linkNumber)) {
            linkNumber++;
        }
        return linkNumber;
    }

    /**
     * Creates a new link in the linkedSection.
     *
     * @param linkedSection The ConfigurationSection containing linked regions.
     * @param link          The RegionLink containing the details to be added.
     */
    private void createNewLink(ConfigurationSection linkedSection, RegionLink link) {
        int linkNumber = getNextAvailableLinkNumber(linkedSection);
        String keyPrefix = "Link" + linkNumber;
        linkedSection.set(keyPrefix + ".LobbyRegion", link.getLobbyRegionName());
        linkedSection.set(keyPrefix + ".GameRegion", link.getGameRegionName());
        linkedSection.set(keyPrefix + ".lobbyTP", locationToString(link.getLobbyTeleportLocation()));
        linkedSection.set(keyPrefix + ".gameTP", locationToString(link.getGameTeleportLocation()));
    }

    /**
     * Removes the link between the specified lobby and game regions from the regions.yml file.
     * The method searches the "Linked" section for the link with the given lobby and game region names and removes it.
     *
     * @param lobbyRegionName The name of the lobby region in the link to be removed.
     * @param gameRegionName  The name of the game region in the link to be removed.
     */
    public void removeLinkFromFile(String lobbyRegionName, String gameRegionName) {
        ConfigurationSection linkedSection = regionsConfig.getConfigurationSection("Linked");
        if (linkedSection == null) return; // Return if "Linked" section not present
        for (String key : linkedSection.getKeys(false)) {
            if (lobbyRegionName.equals(linkedSection.getString(key + ".LobbyRegion")) &&
                    gameRegionName.equals(linkedSection.getString(key + ".GameRegion"))) {
                regionsConfig.set("Linked." + key, null); // Remove link if found
                saveRegionsFile(); // Save changes to file
                return;
            }
        }
    }

    /**
     * Loads all regions and region links from the regions.yml file into the server.
     * Regions are loaded from both "lobbyzones" and "gamezones" sections and registered with the region service.
     * Linked regions are loaded from the "Linked" section, and each link is created and added to the region service.
     * If an unexpected error occurs during the process, a severe log is written, the error stack trace is printed,
     * and the plugin is disabled.
     */
    public void loadRegionsFromConfig() {
        try {
            // Load lobby and game regions
            for (String keyPrefix : new String[]{"lobbyzones", "gamezones"}) {
                ConfigurationSection section = regionsConfig.getConfigurationSection(keyPrefix);
                if (section != null) {
                    for (String regionName : section.getKeys(false)) {
                        Region.RegionType type = keyPrefix.equals("lobbyzones") ? Region.RegionType.LOBBY : Region.RegionType.GAME;
                        String worldName = section.getString(regionName + ".world");
                        regionService.registerRegion(regionName, type, Objects.requireNonNull(Bukkit.getWorld(Objects.requireNonNull(worldName)))); // Use the world name
                    }
                }
            }

            // Load linked regions
            ConfigurationSection linkedSection = regionsConfig.getConfigurationSection("Linked");
            if (linkedSection != null) {
                for (String key : linkedSection.getKeys(false)) {
                    String lobbyRegionName = linkedSection.getString(key + ".LobbyRegion");
                    String gameRegionName = linkedSection.getString(key + ".GameRegion");
                    String lobbyWorldName = regionService.getRegion(lobbyRegionName).getWorldName();
                    String gameWorldName = regionService.getRegion(gameRegionName).getWorldName();
                    World lobbyWorld = org.bukkit.Bukkit.getWorld(lobbyWorldName);
                    World gameWorld = org.bukkit.Bukkit.getWorld(gameWorldName);
                    Location lobbyTP = stringToLocation(lobbyWorld, linkedSection.getString(key + ".lobbyTP"));
                    Location gameTP = stringToLocation(gameWorld, linkedSection.getString(key + ".gameTP"));
                    RegionLink link = new RegionLink(lobbyRegionName, gameRegionName, lobbyTP, gameTP);
                    regionService.addRegionLink(lobbyRegionName, gameRegionName, link);
                }
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Unexpected error in Regions.yml. Please review the configuration.", e);
            getPluginManager().disablePlugin(plugin); // Disable the plugin if an error occurs
        }
    }

    /**
     * Converts a {@link Location} object into a string representation. The coordinates are separated by commas.
     * If the location is null, an empty string is returned.
     *
     * @param location The location to be converted.
     * @return A string representation of the location in the format "x,y,z", or an empty string if the location is null.
     */
    public String locationToString(Location location) {
        if (location == null) {
            return "";
        }
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    /**
     * Converts a string representation of a location into a {@link Location} object. The string must be in the format "x,y,z".
     * If the string is null or empty, null is returned.
     *
     * @param world          The world in which the location resides.
     * @param locationString The string representation of the location in the format "x,y,z".
     * @return A Location object representing the coordinates, or null if the string is invalid.
     */
    private Location stringToLocation(World world, String locationString) {
        if (locationString == null || locationString.isEmpty()) {
            return null;
        }
        String[] parts = locationString.split(",");
        int x = Integer.parseInt(parts[0]);
        int y = Integer.parseInt(parts[1]);
        int z = Integer.parseInt(parts[2]);
        return new Location(world, x, y, z);
    }
}