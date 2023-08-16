package com.slimer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Wolf;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class WolfChase {
    private final WorldGuardManager worldGuardManager; // Instance variable for WorldGuardManager
    private Wolf wolf; // Instance variable for the wolf
    private final GameManager gameManager;

    public WolfChase(GameManager gameManager, WorldGuardManager worldGuardManager) {
        this.gameManager = gameManager;
        this.worldGuardManager = worldGuardManager;
    }

    public void spawnWolf(Location spawnLocation) {
        // Spawn the wolf at the given location
        wolf = (Wolf) spawnLocation.getWorld().spawnEntity(spawnLocation, EntityType.WOLF); // Assign to instance variable

        // Disable the wolf's AI
        wolf.setAI(false);

        // Set the wolf to be angry
        wolf.setAngry(true);
    }

    public void removeWolf() {
        if (wolf != null) {
            wolf.remove();
            wolf = null;
        }
    }

    public Location findClosestTarget(Location wolfLocation, Location snakeHead, LinkedList<Sheep> snakeBody) {
        Location closestTarget = snakeHead;
        double minDistance = wolfLocation.distance(snakeHead);

        for (Sheep bodyPart : snakeBody) {
            Location bodyLocation = bodyPart.getLocation();
            double distance = wolfLocation.distance(bodyLocation);
            if (distance < minDistance) {
                minDistance = distance;
                closestTarget = bodyLocation;
            }
        }

        return closestTarget;
    }

    public void moveTowards(Location snakeHead, LinkedList<Sheep> snakeBody) {
        Location wolfLocation = wolf.getLocation();
        Location targetLocation = findClosestTarget(wolfLocation, snakeHead, snakeBody);

        // Then use the A* pathfinding algorithm as before to move the wolf towards the target location:
        AStarPathfinder pathfinder = new AStarPathfinder();
        List<Location> path = pathfinder.findPath(wolfLocation, targetLocation);

        if (path != null && path.size() >= 2) {
            Location nextLocation = path.get(1);
            double deltaX = nextLocation.getX() - wolfLocation.getX();
            double deltaZ = nextLocation.getZ() - wolfLocation.getZ();
            float yaw = (float) Math.toDegrees(Math.atan2(-deltaX, deltaZ));

            nextLocation.setYaw(yaw);
            wolf.teleport(nextLocation);
        }
    }

    public Location randomWolfLocation(World world, Player player) {
        Location playerLocation = player.getLocation();
        Location location;
        do {
            // Generate a random location within the game zone's boundaries
            Location newLocation = getRandomLocation(world, player);

            // Check if the location is suitable for spawning the wolf
            if (isLocationSuitable(newLocation) && newLocation.distance(playerLocation) >= 10) {
                location = newLocation;
            } else {
                location = null;
            }
        } while (location == null);
        return location;
    }

    public Location getWolfLocation() {
        return wolf.getLocation();
    }


    // Method to check if the given location is colliding with the wolf
    public boolean isCollidingWithWolf(Location location) {
        Location wolfLocation = getWolfLocation();
        return Math.abs(wolfLocation.getX() - location.getX()) < 0.5 &&
                Math.abs(wolfLocation.getY() - location.getY()) < 0.5 &&
                Math.abs(wolfLocation.getZ() - location.getZ()) < 0.5;
    }

    private boolean isLocationSuitable(Location location) {
        // Check if the block at the location is air
        if (!location.getBlock().getType().equals(Material.AIR)) {
            return false;
        }

        // Check if the block below the location is solid
        if (!location.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
            return false;
        }

        // Check if there are any obstructing blocks above the location
        for (int i = 1; i <= 2; i++) { // Check two blocks above the location
            if (!location.clone().add(0, i, 0).getBlock().getType().equals(Material.AIR)) {
                return false;
            }
        }

        return true;
    }

    // Returns a random location within the game zone's boundaries.
    public Location getRandomLocation(World world, Player player) {
        // Get the game zone for the player using the GameManager instance
        String gameZoneName = gameManager.getGameZoneForPlayer(player);
        if (gameZoneName != null) {
            Location[] bounds = worldGuardManager.getGameZoneBounds(gameZoneName);
            if (bounds != null) {
                Location min = bounds[0];
                Location max = bounds[1];
                int x = getRandomNumberInRange(min.getBlockX(), max.getBlockX());
                int z = getRandomNumberInRange(min.getBlockZ(), max.getBlockZ());
                int y = player.getLocation().getBlockY();
                return new Location(world, x + 0.5, y, z + 0.5); // Add 0.5 to x and z
            }
        }
        Bukkit.getLogger().severe("Unable to get game zone boundaries!");
        return null;
    }

    public void setAngry(boolean angry) {
        if (wolf != null) {
            wolf.setAngry(angry);
        }
    }

    // Helper method to get a random number within the specified range.
    private int getRandomNumberInRange(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("Max must be greater than min");
        }
        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }
}