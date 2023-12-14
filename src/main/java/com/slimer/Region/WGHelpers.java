package com.slimer.Region;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Provides helper methods for interacting with WorldGuard regions in the Snake game.
 * <p>
 * Last updated: V2.1.0
 *
 * @author Slimerblue22
 */
public class WGHelpers {
    private static WGHelpers instance;

    private WGHelpers() {
    }

    /**
     * Singleton instance retrieval method.
     *
     * @return The singleton instance of the WGHelpers class.
     */
    public static synchronized WGHelpers getInstance() {
        if (instance == null) {
            instance = new WGHelpers();
        }
        return instance;
    }

    /**
     * Retrieves the WorldGuard RegionManager for a given world.
     *
     * @param worldName The name of the Bukkit world for which the RegionManager is needed.
     * @return The RegionManager for the specified world, or null if the world does not exist or the RegionManager could not be retrieved.
     */
    private RegionManager getRegionManager(String worldName) {
        World bukkitWorld = Bukkit.getWorld(worldName);
        if (bukkitWorld == null) {
            return null;
        }
        com.sk89q.worldedit.world.World worldEditWorld = BukkitAdapter.adapt(bukkitWorld);
        return WorldGuard.getInstance().getPlatform().getRegionContainer().get(worldEditWorld);
    }

    /**
     * Checks if a WorldGuard region exists in a specified world.
     *
     * @param worldName  The name of the world to check.
     * @param regionName The name of the region to check for.
     * @return True if the region exists, false otherwise.
     */
    public boolean doesWGRegionExist(String worldName, String regionName) {
        RegionManager regionManager = getRegionManager(worldName);
        if (regionManager == null) {
            return false;
        }
        return regionManager.getRegion(regionName) != null;
    }

    /**
     * Checks if a set of coordinates lies within a specific WorldGuard region.
     *
     * @param worldName  The name of the world where the region resides.
     * @param regionName The name of the WorldGuard region.
     * @param x          The x-coordinate to check.
     * @param y          The y-coordinate to check.
     * @param z          The z-coordinate to check.
     * @return True if the coordinates are in the region, false otherwise.
     */
    public boolean areCoordinatesInWGRegion(String worldName, String regionName, int x, int y, int z) {
        RegionManager regionManager = getRegionManager(worldName);
        if (regionManager == null) {
            return false;
        }
        ProtectedRegion region = regionManager.getRegion(regionName);
        return region != null && region.contains(x, y, z);
    }

    /**
     * Retrieves the boundary points of a specific WorldGuard region.
     *
     * @param worldName  The name of the world where the region resides.
     * @param regionName The name of the WorldGuard region.
     * @return A string describing the minimum and maximum points of the region, or null if the region does not exist.
     */
    public String getBoundariesOfRegion(String worldName, String regionName) {
        RegionManager regionManager = getRegionManager(worldName);
        if (regionManager == null) {
            return null;
        }
        ProtectedRegion region = regionManager.getRegion(regionName);
        if (region == null) {
            return null;
        }
        com.sk89q.worldedit.math.BlockVector3 minPoint = region.getMinimumPoint();
        com.sk89q.worldedit.math.BlockVector3 maxPoint = region.getMaximumPoint();
        return String.format("Min(%d, %d, %d) - Max(%d, %d, %d)",
                minPoint.getX(), minPoint.getY(), minPoint.getZ(),
                maxPoint.getX(), maxPoint.getY(), maxPoint.getZ());
    }

    /**
     * Gets the WorldGuard region that a player is currently in.
     *
     * @param player The player whose location will be checked.
     * @return The name of the WorldGuard region the player is in, or null if the player is not in any region.
     */
    public String getPlayerCurrentRegion(Player player) {
        Location loc = player.getLocation();
        World bukkitWorld = loc.getWorld();
        if (bukkitWorld == null) {
            return null;
        }
        com.sk89q.worldedit.world.World worldEditWorld = BukkitAdapter.adapt(bukkitWorld);
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(worldEditWorld);
        if (regionManager == null) {
            return null;
        }
        com.sk89q.worldedit.math.BlockVector3 point = BukkitAdapter.asBlockVector(loc);
        ApplicableRegionSet applicableRegions = regionManager.getApplicableRegions(point);
        for (ProtectedRegion region : applicableRegions) {
            return region.getId();
        }
        return null;
    }
}