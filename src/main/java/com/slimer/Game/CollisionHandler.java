package com.slimer.Game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Manages collision detection logic for the snake game.
 * This class is responsible for detecting collisions with walls and checking the solidity of the block below the snake.
 */
public class CollisionHandler {
    private final GameManager gameManager;
    private final Player player;
    private Location lastKnownLocation;

    /**
     * Constructs a new CollisionHandler.
     *
     * @param gameManager The GameManager instance to manage game logic.
     * @param player      The player for whom collision checks are to be performed.
     */
    public CollisionHandler(GameManager gameManager, Player player) {
        this.gameManager = gameManager;
        this.player = player;
    }

    /**
     * Runs collision checks for the snake.
     * If a collision is detected, the game for the player is stopped.
     */
    public void runCollisionChecks() {
        if (checkWallCollision() || checkSolidBlockBelow() || checkSelfCollision()) {
            gameManager.stopGame(player);  // End the game for the player
        }
    }

    /**
     * Checks for wall collisions by comparing the current and last known locations of the snake's head.
     *
     * @return True if a wall collision is detected, false otherwise.
     */
    private boolean checkWallCollision() {
        SnakeCreation snake = gameManager.getSnakeForPlayer(player);
        if (snake == null) {
            return false;
        }

        Entity sheepEntity = snake.getSheepEntity();
        Location currentLocation = sheepEntity.getLocation();

        // Round off the coordinates to a few decimal places
        double roundTo = 1e-3; // Consider up to the third decimal place
        double x = Math.round(currentLocation.getX() / roundTo) * roundTo;
        double y = Math.round(currentLocation.getY() / roundTo) * roundTo;
        double z = Math.round(currentLocation.getZ() / roundTo) * roundTo;
        Location roundedLocation = new Location(currentLocation.getWorld(), x, y, z);

        if (lastKnownLocation != null && lastKnownLocation.equals(roundedLocation)) {
            Bukkit.getLogger().info("[CollisionHandler.java] Wall collision detected!");  // Debug line
            return true; // Collision detected
        } else {
            lastKnownLocation = roundedLocation; // No collision detected
            return false;
        }
    }

    /**
     * Checks if the block below the snake's head is solid.
     *
     * @return True if the block below is not solid, false otherwise.
     */
    private boolean checkSolidBlockBelow() {
        SnakeCreation snake = gameManager.getSnakeForPlayer(player);
        if (snake == null) {
            return false;
        }

        Entity sheepEntity = snake.getSheepEntity();
        Location currentLocation = sheepEntity.getLocation();
        Block blockBelow = currentLocation.getWorld().getBlockAt(currentLocation.add(0, -1, 0));

        if (!blockBelow.getType().isSolid()) {
            Bukkit.getLogger().info("[CollisionHandler.java] Non-solid block below detected!");  // Debug line
            return true; // True if the block below is not solid
        }

        return false; // False if the block below is solid
    }

    /**
     * Checks for a collision between the snake's head and its segments.
     *
     * @return True if a self-collision is detected, false otherwise.
     */
    // NOTICE: There is a rare chance collision will not always flag when only the head and 1 segment exist.
    // This bug is fairly rare and seemingly random.
    // Until further information is obtained, this bug is to be ignored as it's very rare and does not cause major issues.
    private boolean checkSelfCollision() {
        SnakeCreation snake = gameManager.getSnakeForPlayer(player);
        if (snake == null) {
            return false;
        }

        Entity sheepEntity = snake.getSheepEntity();  // This is the head of the snake
        Vector headLocation = sheepEntity.getLocation().toVector();

        // Get all the segments of the snake
        List<Entity> segments = gameManager.getSegmentsForPlayer(player);

        // If there are no segments, ignore collision checks
        if (segments.isEmpty()) {
            return false;
        }

        // Iterate through the segments, starting from the first one
        for (Entity segment : segments) {
            Vector segmentLocation = segment.getLocation().toVector();
            if (headLocation.distance(segmentLocation) < 0.1) {  // Tolerance of 0.1 blocks
                Bukkit.getLogger().info("[CollisionHandler.java] Self-collision detected!");  // Debug line
                return true;  // Self-collision detected
            }
        }


        return false;  // No self-collision detected
    }
}
