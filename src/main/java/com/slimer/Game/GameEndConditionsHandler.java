package com.slimer.Game;

import com.slimer.Util.DebugManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Handles conditions that may result in the end of the snake game for a player.
 * This includes wall collisions, snake self-collisions, and player dismounting the snake's head.
 * The class also manages events related to the game-ending conditions.
 */
public class GameEndConditionsHandler implements Listener {
    private final GameManager gameManager;
    private final Player player;
    private Location lastKnownLocation;

    /**
     * Constructs a new GameEndConditionsHandler.
     *
     * @param gameManager The GameManager instance to manage game logic.
     * @param player      The player for whom collision checks are to be performed.
     */
    public GameEndConditionsHandler(GameManager gameManager, Player player, Plugin plugin) {
        this.gameManager = gameManager;
        this.player = player;

        // Register the event for player disconnections
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Runs collision checks for the snake.
     * If a collision is detected, the game for the player is stopped.
     */
    public void runGameEndEventsChecks() {
        if (checkWallCollision() || checkSolidBlockBelow() || checkSelfCollision() || checkPlayerDismounted()) {
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

        Entity sheepEntity = snake.getSheepEntity();  // This is the head of the snake
        Location currentLocation = sheepEntity.getLocation();

        // Round off the coordinates to a few decimal places
        double roundTo = 1e-3; // Consider up to the third decimal place
        double x = Math.round(currentLocation.getX() / roundTo) * roundTo;
        double y = Math.round(currentLocation.getY() / roundTo) * roundTo;
        double z = Math.round(currentLocation.getZ() / roundTo) * roundTo;
        Location roundedLocation = new Location(currentLocation.getWorld(), x, y, z);

        if (lastKnownLocation != null && lastKnownLocation.equals(roundedLocation)) {
            if (DebugManager.isDebugEnabled) {
                Bukkit.getLogger().info("{Snake 2.0.0 Beta-2} [GameEndConditionsHandler.java] Wall collision detected!");
            }
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

        Entity sheepEntity = snake.getSheepEntity();  // This is the head of the snake
        Location currentLocation = sheepEntity.getLocation();
        Block blockBelow = currentLocation.getWorld().getBlockAt(currentLocation.add(0, -1, 0));

        if (!blockBelow.getType().isSolid()) {
            if (DebugManager.isDebugEnabled) {
                Bukkit.getLogger().info("{Snake 2.0.0 Beta-2} [GameEndConditionsHandler.java] Non-solid block below detected!");
            }
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
                if (DebugManager.isDebugEnabled) {
                    Bukkit.getLogger().info("{Snake 2.0.0 Beta-2} [GameEndConditionsHandler.java] Self-collision detected!");
                }
                return true;  // Self-collision detected
            }
        }


        return false;  // No self-collision detected
    }

    /**
     * Checks if the player has dismounted the snake's head.
     *
     * @return True if the player has dismounted, false otherwise.
     */
    private boolean checkPlayerDismounted() {
        SnakeCreation snake = gameManager.getSnakeForPlayer(player);
        if (snake == null) {
            return false;
        }

        Entity sheepEntity = snake.getSheepEntity();  // This is the head of the snake

        // Check if the list of passengers is empty or not containing the player
        List<Entity> passengers = sheepEntity.getPassengers();
        if (passengers.isEmpty() || !passengers.contains(player)) {
            if (DebugManager.isDebugEnabled) {
                Bukkit.getLogger().info("{Snake 2.0.0 Beta-2} [GameEndConditionsHandler.java] Player dismounted!");
            }
            return true;  // Player has dismounted
        }

        return false;  // Player is still mounted
    }

    /**
     * Event handler for player quit events.
     * Stops the game if the quitting player is the same as the one managed by this GameEndConditionsHandler instance.
     *
     * @param event The PlayerQuitEvent object containing event data.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player quittingPlayer = event.getPlayer();
        Entity riddenEntity = quittingPlayer.getVehicle();

        // Check if the quitting player is the same as the one managed by this GameEndConditionsHandler instance
        if (quittingPlayer.equals(player)) {
            if (riddenEntity != null && isSnakeEntity(riddenEntity)) {
                if (DebugManager.isDebugEnabled) {
                    Bukkit.getLogger().info("{Snake 2.0.0 Beta-2} [GameEndConditionsHandler.java] A player has disconnected mid game!");
                }
                gameManager.stopGame(quittingPlayer);  // Perform cleanup
                gameManager.handlePlayerDisconnect(quittingPlayer); // Handle player disconnection
            }
        }
    }

    /**
     * Event handler for when a player joins the server.
     * Calls the handlePlayerReconnect method to manage reconnected players.
     *
     * @param event The PlayerJoinEvent object containing event data.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joiningPlayer = event.getPlayer();

        // Handle reconnection logic
        gameManager.handlePlayerReconnect(joiningPlayer);
    }

    /**
     * Determines if a given entity is a snake entity (in this case, a sheep).
     * Used by the player disconnection check to remove the snake the player was riding.
     *
     * @param entity The Entity object to check.
     * @return True if the entity is a sheep, false otherwise.
     */
    private boolean isSnakeEntity(Entity entity) {
        // Check if the entity is a sheep
        return entity.getType() == EntityType.SHEEP;
    }
}
