package com.slimer.Game.AppleManagement;

import com.sk89q.worldedit.math.BlockVector3;
import com.slimer.Region.WGHelpers;
import com.slimer.Util.DebugManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Random;

// TODO: Hook this up with A* to ensure spots can be reached (Or maybe BFS would be better?)
public class AppleLocationFinder {
    private static final String LOCATION_FOUND_LOG = "Location found after %d attempts: %s";
    private static final String FAILED_SOLID_BASE_CHECK_LOG = "Failed solid base check at %s - Block type: %s";
    private static final String FAILED_AIR_SURROUNDING_CHECK_LOG = "Failed air surrounding check at %s - Block type: %s";
    private static final String FAILED_AIR_ABOVE_CHECK_LOG = "Failed air above check at %s - Block type: %s";
    private static final int maxAttempts = 100;

    public AppleLocationFinder() {
    }

    public Location findRandomLocationInRegion(String worldName, String regionName, int fixedY) {
        BlockVector3[] boundaries = WGHelpers.getBoundariesOfRegion(worldName, regionName);
        if (boundaries == null || boundaries.length < 2) {
            return null;
        }

        BlockVector3 min = boundaries[0];
        BlockVector3 max = boundaries[1];

        Random random = new Random();
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        Location randomLocation;
        int attemptCount = 0;
        for (int i = 0; i < maxAttempts; i++) {
            attemptCount++;
            int x = min.getX() + random.nextInt(max.getX() - min.getX() + 1);
            int z = min.getZ() + random.nextInt(max.getZ() - min.getZ() + 1);
            randomLocation = new Location(world, x, fixedY, z);

            if (isSolidBase(randomLocation) && isAirSurrounding(randomLocation) && isAirAbove(randomLocation)) {
                DebugManager.log(DebugManager.Category.DEBUG, String.format(LOCATION_FOUND_LOG, attemptCount, randomLocation));
                return randomLocation; // Found a suitable location
            }
        }
        return null; // No suitable location found after maximum attempts
    }

    private boolean isSolidBase(Location location) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block block = location.clone().add(dx, -1, dz).getBlock();
                if (!block.getType().isSolid()) {
                    DebugManager.log(DebugManager.Category.DEBUG,
                            String.format(FAILED_SOLID_BASE_CHECK_LOG, block.getLocation(), block.getType()));
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isAirSurrounding(Location location) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block block = location.clone().add(dx, 0, dz).getBlock();
                if (block.getType() != Material.AIR) {
                    DebugManager.log(DebugManager.Category.DEBUG,
                            String.format(FAILED_AIR_SURROUNDING_CHECK_LOG, block.getLocation(), block.getType()));
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isAirAbove(Location location) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block block = location.clone().add(dx, 1, dz).getBlock();
                if (block.getType() != Material.AIR) {
                    DebugManager.log(DebugManager.Category.DEBUG,
                            String.format(FAILED_AIR_ABOVE_CHECK_LOG, block.getLocation(), block.getType()));
                    return false;
                }
            }
        }
        return true;
    }
}