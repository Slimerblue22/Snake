package com.slimer;

import org.bukkit.Location;

import java.util.*;

public class AStarPathfinder {
    // Computes the heuristic cost between two locations, using the Manhattan distance
    public double heuristic(Location a, Location b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getZ() - b.getZ());
    }

    // Returns the valid neighbors of a location (those not blocked by solid blocks)
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

        return neighbors;
    }

    // Finds the shortest path between two locations using the A* algorithm
    public List<Location> findPath(Location start, Location goal) {
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
                return reconstructPath(cameFrom, current);
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

        return null; // Return the path or null if not found
    }

    // Compares two locations to see if they represent the same block
    public boolean isSameBlock(Location loc1, Location loc2) {
        return loc1.getBlockX() == loc2.getBlockX() &&
                loc1.getBlockY() == loc2.getBlockY() &&
                loc1.getBlockZ() == loc2.getBlockZ();
    }

    // Returns the node from the open set with the lowest f-score
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

    // Reconstructs the path from the start to the goal by following the cameFrom pointers
    private List<Location> reconstructPath(Map<Location, Location> cameFrom, Location current) {
        List<Location> path = new ArrayList<>();
        while (current != null) {
            path.add(0, current);
            current = cameFrom.get(current);
        }
        return path;
    }
}
