package com.slimer.Game;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.slimer.Region.RegionService;
import com.slimer.Util.AStar;
import com.slimer.Util.DebugManager;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SpawnLocationFinder {
    public SpawnLocationFinder() {
    }

    /**
     * Gets a random spawn location within a specified game zone for a player.
     *
     * @param world        The world in which the game zone is located.
     * @param yLevel       The Y-level for the location.
     * @param gameZoneName The name of the game zone.
     * @return A random Location within the game zone suitable for player spawn, or null if region not found.
     */
    private Location getRandomPlayerSpawnWithinGameZone(World world, int yLevel, String gameZoneName) {
        DebugManager.log(DebugManager.Category.SPAWN_LOCATION_FINDER, "Trying to get a random spawn location within game zone: " + gameZoneName);
        RegionService regionService = RegionService.getInstance();
        ProtectedRegion worldGuardRegion = regionService.getWorldGuardRegion(gameZoneName, world);

        if (worldGuardRegion != null) {
            int minX = worldGuardRegion.getMinimumPoint().getBlockX();
            int maxX = worldGuardRegion.getMaximumPoint().getBlockX();
            int minZ = worldGuardRegion.getMinimumPoint().getBlockZ();
            int maxZ = worldGuardRegion.getMaximumPoint().getBlockZ();

            Random random = new Random();
            Location newLoc;
            int x = random.nextInt(maxX - minX + 1) + minX;
            int z = random.nextInt(maxZ - minZ + 1) + minZ;
            newLoc = new Location(world, x, yLevel, z);

            return newLoc;
        }
        return null;
    }

    /**
     * Retrieves two valid spawn points within a specified game zone. This method attempts to identify
     * up to 10 valid spawn points and then randomly selects two from the list. A valid spawn point
     * is determined by its accessibility and suitability for player spawning.
     *
     * @param world        The world in which the game zone is located.
     * @param yLevel       The Y-level for the location.
     * @param gameZoneName The name of the game zone.
     * @return A Pair containing two Locations representing the spawn points. If two valid spawn points
     * cannot be found, both Locations in the Pair will be null.
     */
    public Pair<Location, Location> getValidPlayerSpawnPoints(World world, int yLevel, String gameZoneName) {
        DebugManager.log(DebugManager.Category.SPAWN_LOCATION_FINDER, "Attempting to find valid spawn points within game zone" + gameZoneName);
        List<Location> validSpawns = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = 1000;
        new AStar();

        while (attempts < maxAttempts && validSpawns.size() < 10) {
            Location spawn = getRandomPlayerSpawnWithinGameZone(world, yLevel, gameZoneName);
            if (spawn != null && isLocationValid(spawn)) {
                validSpawns.add(spawn);
            }
            attempts++;
        }

        if (validSpawns.size() < 2) {
            DebugManager.log(DebugManager.Category.SPAWN_LOCATION_FINDER, "Insufficient valid spawn points found for game zone: " + gameZoneName);
            return Pair.of(null, null);
        }

        DebugManager.log(DebugManager.Category.SPAWN_LOCATION_FINDER, "Found valid spawn points: " + validSpawns.size() + " for game zone: " + gameZoneName);
        Collections.shuffle(validSpawns);
        return Pair.of(validSpawns.get(0), validSpawns.get(1));
    }

    /**
     * Checks if a given location is valid for player spawning. A location is considered valid if it doesn't have
     * solid neighbors and if there's a 3x3 grid of solid blocks directly beneath it.
     *
     * @param location The location to validate.
     * @return True if the location is valid for player spawning, false otherwise.
     */
    private boolean isLocationValid(Location location) {
        DebugManager.log(DebugManager.Category.SPAWN_LOCATION_FINDER, "Validating location: " + location.toString());
        AStar aStar = new AStar();
        return !aStar.hasSolidNeighbors(location) &&
                aStar.isSolid3x3Below(location);
    }
}