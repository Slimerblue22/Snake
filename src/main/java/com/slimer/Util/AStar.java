package com.slimer.Util;

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

            if (!neighbor.getBlock().getType().isSolid() && isSolid3x3Below(neighbor)) {
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }

    /**
     * Checks if the 3x3 area below the given location consists of solid blocks.
     *
     * @param center The central location above which the 3x3 grid is checked.
     * @return True if the 3x3 area below the center consists of solid blocks, false otherwise.
     */
    private boolean isSolid3x3Below(Location center) {
        int[][] offset = {
                {-1, -1}, {0, -1}, {1, -1},
                {-1, 0}, {0, 0}, {1, 0},
                {-1, 1}, {0, 1}, {1, 1}
        };

        for (int[] os : offset) {
            Location loc = center.clone().add(os[0], -1, os[1]); // Check 1 block below the center
            if (!loc.getBlock().getType().isSolid()) {
                return false;
            }
        }
        return true;
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
        return path != null && !path.isEmpty();
    }

    /**
     * Finds the shortest path between two locations using the A* algorithm.
     *
     * @param start The starting location.
     * @param goal  The goal location.
     * @return A list representing the path, or null if no path is found.
     */
    public List<Location> findPath(Location start, Location goal) {
        Set<Location> openSet = new HashSet<>();
        Set<Location> closedSet = new HashSet<>();
        openSet.add(start);
        Map<Location, Location> cameFrom = new HashMap<>();
        Map<Location, Double> gScore = new HashMap<>();
        gScore.put(start, 0.0);
        Map<Location, Double> fScore = new HashMap<>();
        fScore.put(start, heuristic(start, goal));

        int iterationCount = 0;
        while (!openSet.isEmpty()) {
            if (iterationCount++ > 1000) {
                break;
            }
            Location current = getLowestFScoreNode(openSet, fScore);
            if (isSameBlock(current, goal)) {
                return reconstructPath(cameFrom, current);
            }

            openSet.remove(current);
            closedSet.add(current);
            List<Location> neighbors = getNeighbors(current);

            for (Location neighbor : neighbors) {
                if (closedSet.contains(neighbor)) {
                    continue;
                }
                double tentativeGScore = gScore.getOrDefault(current, Double.MAX_VALUE) + 1;
                if (tentativeGScore < gScore.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeGScore);
                    fScore.put(neighbor, tentativeGScore + heuristic(neighbor, goal));
                    openSet.add(neighbor);
                }
            }
        }
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
