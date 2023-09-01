package com.slimer.Game;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
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
     * @param player The player for whom collision checks are to be performed.
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
        if (checkWallCollision() || checkSolidBlockBelow()) {
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

        return !blockBelow.getType().isSolid(); // True if the block below is not solid
    }
}
