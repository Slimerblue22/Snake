package com.slimer.Util;

import org.bukkit.Location;

import java.util.*;

/**
 * The AStar class provides pathfinding functionality using the A* algorithm.
 * It calculates heuristic costs, finds valid neighbors, and determines the existence
 * of a path between locations. This class is useful for pathfinding in a grid-based environment,
 * considering block solidity and block location characteristics.
 * <p>
 * Last updated: V2.1.0
 * @author Slimerblue22
 */
public class AStar {

    /**
     * Calculates the Manhattan distance heuristic between two locations.
     *
     * @param startLocation The starting location.
     * @param endLocation The ending location.
     * @return The Manhattan distance heuristic between the two locations.
     */
    private double calculateManhattanHeuristic(Location startLocation, Location endLocation) {
        double hValue = Math.abs(startLocation.getX() - endLocation.getX()) +
                Math.abs(startLocation.getZ() - endLocation.getZ());

        DebugManager.log(DebugManager.Category.ASTAR, "Manhattan Heuristic value calculated as: " + hValue);
        return hValue;
    }

    /**
     * Returns the valid neighboring locations of a given starting location, excluding those blocked by solid blocks.
     *
     * @param startLocation The starting location to find neighboring locations for.
     * @return A list of valid neighboring locations.
     */
    private List<Location> getValidNeighbors(Location startLocation) {
        List<Location> neighbors = new ArrayList<>();
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] direction : directions) {
            Location neighbor = startLocation.clone().add(direction[0], 0, direction[1]);
            if (!neighbor.getBlock().getType().isSolid() && isSolid3x3Below(neighbor)) {
                DebugManager.log(DebugManager.Category.ASTAR, "Valid neighbor found at " + neighbor);
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
     * @return True if a path exists, false otherwise. It internally calls the "findPath" method.
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
    private List<Location> findPath(Location start, Location goal) {
        DebugManager.log(DebugManager.Category.ASTAR, "Starting pathfinding from " + start + " to " + goal);

        Set<Location> openSet = new HashSet<>();
        Set<Location> closedSet = new HashSet<>();
        openSet.add(start);

        Map<Location, Location> cameFrom = new HashMap<>();
        Map<Location, Double> gScore = new HashMap<>();
        gScore.put(start, 0.0);

        Map<Location, Double> fScore = new HashMap<>();
        fScore.put(start, calculateManhattanHeuristic(start, goal));

        int iterationCount = 0; // Used to limit the number of iterations
        while (!openSet.isEmpty()) {
            if (iterationCount++ > 1000) {
                DebugManager.log(DebugManager.Category.ASTAR, "Iteration count exceeded 1000. Breaking out of loop.");
                break;
            }

            Location current = getLowestFScoreNode(openSet, fScore);
            if (isSameBlock(current, goal)) {
                List<Location> path = reconstructPath(cameFrom, current);
                DebugManager.log(DebugManager.Category.ASTAR, "Path found with length: " + path.size());
                return path;
            }

            openSet.remove(current);
            closedSet.add(current);

            List<Location> neighbors = getValidNeighbors(current);
            for (Location neighbor : neighbors) {
                if (closedSet.contains(neighbor)) {
                    continue;
                }

                double tentativeGScore = gScore.getOrDefault(current, Double.MAX_VALUE) + 1;
                if (tentativeGScore < gScore.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    DebugManager.log(DebugManager.Category.ASTAR, "Updating gScore for neighbor " + neighbor + " with value: " + tentativeGScore);
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeGScore);
                    fScore.put(neighbor, tentativeGScore + calculateManhattanHeuristic(neighbor, goal));
                    openSet.add(neighbor);
                }
            }
        }

        DebugManager.log(DebugManager.Category.ASTAR, "No path found from " + start + " to " + goal);
        return null; // Return null if no path is found
    }

    /**
     * Compares two locations to determine if they represent the same block.
     *
     * @param location1 The first location.
     * @param location2 The second location.
     * @return True if the locations represent the same block, false otherwise.
     */
    public boolean isSameBlock(Location location1, Location location2) {
        return location1.getBlockX() == location2.getBlockX() &&
                location1.getBlockY() == location2.getBlockY() &&
                location1.getBlockZ() == location2.getBlockZ();
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
        DebugManager.log(DebugManager.Category.ASTAR, "Path reconstructed with length: " + path.size());
        return path;
    }
}
