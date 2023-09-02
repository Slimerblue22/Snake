package com.slimer.Util;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.*;

public class AStar {
    /**
     * Calculates the heuristic cost between two locations using the Manhattan distance.
     *
     * @param a The starting location.
     * @param b The ending location.
     * @return The Manhattan distance between the two locations.
     */
    public double heuristic(Location a, Location b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getZ() - b.getZ());
    }

    /**
     * Returns the valid neighbors of a location, considering those not blocked by solid blocks.
     *
     * @param location The location to find neighbors for.
     * @return A list of valid neighbor locations.
     */
    public List<Location> getNeighbors(Location location) {
        List<Location> neighbors = new ArrayList<>();
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        for (int[] direction : directions) {
            Location neighbor = location.clone().add(direction[0], 0, direction[1]);
            Location belowNeighbor = neighbor.clone().subtract(0, 1, 0);

            if (!neighbor.getBlock().getType().isSolid() && belowNeighbor.getBlock().getType().isSolid()) {
                neighbors.add(neighbor);
            }
        }

        Bukkit.getLogger().info("{Snake 2.0.0 Beta-1} [AStar.java] Found " + neighbors.size() + " valid neighbors for location (" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")");
        return neighbors;
    }

    /**
     * Checks if a given location has any neighboring blocks that are solid.
     * The method considers eight neighboring locations around the specified location:
     * four cardinal directions (North, South, East, West) and four diagonal directions.
     *
     * @param location The Bukkit Location object representing the central point for the check.
     * @return true if any of the neighboring blocks are solid, false otherwise.
     */
    public boolean hasSolidNeighbors(Location location) {
        int[][] directions = {
                {1, 0}, {-1, 0}, {0, 1}, {0, -1},  // Cardinal directions
                {1, 1}, {-1, -1}, {1, -1}, {-1, 1} // Diagonal directions
        };

        for (int[] direction : directions) {
            Location neighbor = location.clone().add(direction[0], 0, direction[1]);
            if (neighbor.getBlock().getType().isSolid()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a path exists between two locations.
     *
     * @param start The starting location.
     * @param goal  The goal location.
     * @return True if a path exists, false otherwise.
     */
    public boolean pathExists(Location start, Location goal) {
        List<Location> path = findPath(start, goal);
        boolean exists = path != null && !path.isEmpty();
        Bukkit.getLogger().info("{Snake 2.0.0 Beta-1} [AStar.java] Path exists between start (" + start.getBlockX() + ", " + start.getBlockY() + ", " + start.getBlockZ() + ") and goal (" + goal.getBlockX() + ", " + goal.getBlockY() + ", " + goal.getBlockZ() + "): " + exists);
        return exists;
    }

    /**
     * Finds the shortest path between two locations using the A* algorithm.
     *
     * @param start The starting location.
     * @param goal  The goal location.
     * @return A list representing the path, or null if no path is found.
     */
    public List<Location> findPath(Location start, Location goal) {
        Bukkit.getLogger().info("{Snake 2.0.0 Beta-1} [AStar.java] Finding path between start (" + start.getBlockX() + ", " + start.getBlockY() + ", " + start.getBlockZ() + ") and goal (" + goal.getBlockX() + ", " + goal.getBlockY() + ", " + goal.getBlockZ() + ")");

        Set<Location> openSet = new HashSet<>();
        openSet.add(start);
        Map<Location, Location> cameFrom = new HashMap<>();
        Map<Location, Double> gScore = new HashMap<>();
        gScore.put(start, 0.0);
        Map<Location, Double> fScore = new HashMap<>();
        fScore.put(start, heuristic(start, goal));

        while (!openSet.isEmpty()) {
            Location current = getLowestFScoreNode(openSet, fScore);
            if (isSameBlock(current, goal)) {
                List<Location> path = reconstructPath(cameFrom, current);
                Bukkit.getLogger().info("{Snake 2.0.0 Beta-1} [AStar.java] Path found with length: " + path.size());
                return path;
            }

            openSet.remove(current);
            List<Location> neighbors = getNeighbors(current);

            for (Location neighbor : neighbors) {
                double tentativeGScore = gScore.getOrDefault(current, Double.MAX_VALUE) + 1;
                if (tentativeGScore < gScore.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeGScore);
                    fScore.put(neighbor, tentativeGScore + heuristic(neighbor, goal));
                    openSet.add(neighbor);
                }
            }
        }

        Bukkit.getLogger().info("{Snake 2.0.0 Beta-1} [AStar.java] No path found");
        return null; // Return the path or null if not found
    }

    /**
     * Compares two locations to determine if they represent the same block.
     *
     * @param loc1 The first location.
     * @param loc2 The second location.
     * @return True if the locations represent the same block, false otherwise.
     */
    public boolean isSameBlock(Location loc1, Location loc2) {
        return loc1.getBlockX() == loc2.getBlockX() &&
                loc1.getBlockY() == loc2.getBlockY() &&
                loc1.getBlockZ() == loc2.getBlockZ();
    }

    /**
     * Returns the node from the open set with the lowest f-score.
     *
     * @param openSet The set of locations being considered.
     * @param fScore  The map of f-scores for each location.
     * @return The location with the lowest f-score.
     */
    private Location getLowestFScoreNode(Set<Location> openSet, Map<Location, Double> fScore) {
        Location lowestNode = null;
        double lowestScore = Double.MAX_VALUE;
        for (Location node : openSet) {
            double score = fScore.getOrDefault(node, Double.MAX_VALUE);
            if (score < lowestScore) {
                lowestScore = score;
                lowestNode = node;
            }
        }
        return lowestNode;
    }

    /**
     * Reconstructs the path from the start to the goal by following the cameFrom pointers.
     *
     * @param cameFrom A map representing the parent of each node in the path.
     * @param current  The current location being considered.
     * @return A list representing the reconstructed path.
     */
    private List<Location> reconstructPath(Map<Location, Location> cameFrom, Location current) {
        List<Location> path = new ArrayList<>();
        while (current != null) {
            path.add(0, current);
            current = cameFrom.get(current);
        }
        return path;
    }
}
