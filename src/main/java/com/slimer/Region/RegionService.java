package com.slimer.Region;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Singleton service for managing regions and region links within the Snake game.
 * Provides methods for registering, unregistering, linking, and unlinking regions,
 * as well as accessing WorldGuard regions and related functionality.
 */
public class RegionService {
    private static RegionService instance;
    private final Map<String, Region> regions;
    private final Map<String, RegionLink> regionLinks;
    private RegionFileHandler regionFileHandler;

    /**
     * Private constructor to ensure only one instance of RegionService is created.
     */
    private RegionService() {
        regions = new HashMap<>();
        regionLinks = new HashMap<>();
    }

    /**
     * Returns the singleton instance of RegionService, creating it if necessary.
     *
     * @return The singleton instance of RegionService.
     */
    public static RegionService getInstance() {
        if (instance == null) {
            instance = new RegionService();
        }
        return instance;
    }

    /**
     * Returns an unmodifiable map of all registered regions.
     *
     * @return An unmodifiable map of all registered regions.
     */
    public Map<String, Region> getAllRegions() {
        return Collections.unmodifiableMap(regions);
    }

    /**
     * Returns an unmodifiable map of all registered region links.
     *
     * @return An unmodifiable map of all registered region links.
     */
    public Map<String, RegionLink> getAllRegionLinks() {
        return Collections.unmodifiableMap(regionLinks);
    }

    /**
     * Retrieves the WorldGuard region with the specified name and world.
     *
     * @param regionName The name of the region.
     * @param world      The world where the region resides.
     * @return The WorldGuard protected region.
     */
    public ProtectedRegion getWorldGuardRegion(String regionName, World world) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(world));
        return Objects.requireNonNull(regionManager).getRegion(regionName);
    }

    /**
     * Determines if the given location is within the specified region.
     *
     * @param location The location to check.
     * @param region   The region to check against.
     * @return True if the location is within the region, false otherwise.
     */
    public boolean isLocationInRegion(Location location, ProtectedRegion region) {
        return region.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * Sets the RegionFileHandler for this service.
     *
     * @param regionFileHandler The RegionFileHandler to set.
     */
    public void setRegionFileHandler(RegionFileHandler regionFileHandler) {
        this.regionFileHandler = regionFileHandler;
    }

    /**
     * Unlinks the specified lobby and game regions.
     *
     * @param lobbyRegionName The name of the lobby region to unlink.
     * @param gameRegionName  The name of the game region to unlink.
     */
    public void unlinkRegions(String lobbyRegionName, String gameRegionName) {
        String linkKey = lobbyRegionName + "-" + gameRegionName;
        regionLinks.remove(linkKey);
        regionFileHandler.removeLinkFromFile(lobbyRegionName, gameRegionName);
    }

    /**
     * Unregisters the region with the specified name.
     *
     * @param regionName The name of the region to unregister.
     */
    public void unregisterRegion(String regionName) {
        regions.remove(regionName);
        regionFileHandler.removeRegionFromFile(regionName);
    }

    /**
     * Registers a new region with the specified name, type, and world.
     *
     * @param name  The name of the region to register.
     * @param type  The type of the region, either LOBBY or GAME.
     * @param world The world where the region resides.
     */
    public void registerRegion(String name, Region.RegionType type, World world) {
        String worldName = world.getName();
        Region region = new Region(name, type, null, worldName);
        regions.put(name, region);
        regionFileHandler.saveRegionToFile(region);
    }

    /**
     * Creates and links regions between the specified lobby and game region names.
     * A new RegionLink is created and saved to the file by the RegionFileHandler.
     *
     * @param lobbyRegionName The name of the lobby region to link.
     * @param gameRegionName  The name of the game region to link.
     */
    public void linkRegions(String lobbyRegionName, String gameRegionName) {
        RegionLink link = new RegionLink(lobbyRegionName, gameRegionName, null, null);
        String linkKey = lobbyRegionName + "-" + gameRegionName;
        regionLinks.put(linkKey, link);
        regionFileHandler.saveRegionLinkToFile(link);
    }

    /**
     * Adds a pre-existing RegionLink to the internal map of region links.
     *
     * @param lobbyRegionName The name of the lobby region to link.
     * @param gameRegionName  The name of the game region to link.
     * @param link            The RegionLink object containing the linking information.
     */
    public void addRegionLink(String lobbyRegionName, String gameRegionName, RegionLink link) {
        String linkKey = lobbyRegionName + "-" + gameRegionName;
        regionLinks.put(linkKey, link);
    }

    /**
     * Retrieves the Region object with the specified name.
     *
     * @param name The name of the region to retrieve.
     * @return The Region object with the specified name, or null if not found.
     */
    public Region getRegion(String name) {
        return regions.get(name);
    }

    /**
     * Retrieves the RegionLink associated with the specified region name and type.
     *
     * @param regionName The name of the region for which to retrieve the link.
     * @param regionType The type of the region (either GAME or LOBBY).
     * @return The RegionLink object associated with the specified region name and type, or null if not found.
     */
    public RegionLink getRegionLink(String regionName, Region.RegionType regionType) {
        for (RegionLink link : regionLinks.values()) {
            if ((regionType == Region.RegionType.GAME && regionName.equals(link.getGameRegionName())) ||
                    (regionType == Region.RegionType.LOBBY && regionName.equals(link.getLobbyRegionName()))) {
                return link;
            }
        }
        return null;
    }

    /**
     * Saves the specified RegionLink to the file using the RegionFileHandler.
     *
     * @param link The RegionLink object to save.
     */
    public void saveRegionLinkToFile(RegionLink link) {
        regionFileHandler.saveRegionLinkToFile(link);
    }
}
