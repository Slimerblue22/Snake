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

// TODO: Hook this up with A* to ensure spots can be reached
public class AppleLocationFinder {
    private static final String LOCATION_FOUND_LOG = "Location found after %d attempts: %s";
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

            Block block = randomLocation.getBlock();
            if (block.getType() == Material.AIR) {
                DebugManager.log(DebugManager.Category.DEBUG, String.format(LOCATION_FOUND_LOG, attemptCount, randomLocation));
                return randomLocation; // Found a suitable location
            }
        }
        return null; // No suitable location found after maximum attempts
    }
}