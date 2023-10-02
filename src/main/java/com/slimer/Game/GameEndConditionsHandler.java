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
import org.bukkit.util.BoundingBox;
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
    public void runGameEndEventsChecks(String gameMode) {
        boolean isClassicMode = "classic".equalsIgnoreCase(gameMode);
        boolean isPvPMode = "pvp".equalsIgnoreCase(gameMode);

        if (isClassicMode || isPvPMode) {
            if (checkWallCollision() || checkSolidBlockBelow() || checkSelfCollision() || checkPlayerDismounted()) {
                gameManager.stopGame(player);  // End the game for the player
            }
        }

        if (isPvPMode) {
            checkPvPModeCollision();  // PvP mode specific logic
        }
    }

    /**
     * Checks for collisions between players in PvP mode using their bounding boxes.
     * <ul>
     *     <li>If there's a head-to-head collision between two players, both players' games are ended.</li>
     *     <li>If a player's head collides with another player's body segment, only the game of the player whose head collided is ended.</li>
     * </ul>
     */
    private void checkPvPModeCollision() {
        SnakeCreation checkingPlayerSnake = gameManager.getSnakeForPlayer(player);
        if (checkingPlayerSnake == null) {
            return;
        }

        String checkingPlayerGameMode = gameManager.getGameModeForPlayer(player);
        if (checkingPlayerGameMode == null || !checkingPlayerGameMode.equals("pvp")) {
            return;  // The player being checked is not in PvP mode
        }

        BoundingBox checkingPlayerHeadBox = checkingPlayerSnake.getSheepEntity().getBoundingBox();

        for (Player potentialCollidingPlayer : Bukkit.getOnlinePlayers()) {
            if (!potentialCollidingPlayer.equals(player)) {
                SnakeCreation collidingPlayerSnake = gameManager.getSnakeForPlayer(potentialCollidingPlayer);
                if (collidingPlayerSnake == null) {
                    continue;
                }

                String collidingPlayerGameMode = gameManager.getGameModeForPlayer(potentialCollidingPlayer);
                if (collidingPlayerGameMode == null || !collidingPlayerGameMode.equals("pvp")) {
                    continue;  // The potentially colliding player is not in PvP mode
                }

                BoundingBox collidingPlayerHeadBox = collidingPlayerSnake.getSheepEntity().getBoundingBox();

                if (checkingPlayerHeadBox.overlaps(collidingPlayerHeadBox)) {
                    // Head-to-head collision, end both games
                    gameManager.stopGame(player);
                    gameManager.stopGame(potentialCollidingPlayer);
                    return;
                }

                // Check if the head of 'checkingPlayer' collides with the body segments of 'potentialCollidingPlayer'
                for (Entity segment : gameManager.getSegmentsForPlayer(potentialCollidingPlayer)) {
                    if (checkingPlayerHeadBox.overlaps(segment.getBoundingBox())) {
                        // Head to body collision, end the game of the player being checked
                        gameManager.stopGame(player);
                        return;
                    }
                }
            }
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
                Bukkit.getLogger().info(DebugManager.getDebugMessage("[GameEndConditionsHandler.java] Wall collision detected!"));
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
                Bukkit.getLogger().info(DebugManager.getDebugMessage("[GameEndConditionsHandler.java] Non-solid block below detected!"));
            }
            return true; // True if the block below is not solid
        }

        return false; // False if the block below is solid
    }

    /**
     * Checks for a collision between the snake's head and its body segments.
     *
     * <p>This method performs two types of checks:</p>
     * <ol>
     *   <li>Special Case: When the snake has one or more segments, a U-turn check is conducted.
     *       This is to ensure that U-turns, which are considered self-collisions, are accurately detected.
     *       The U-turn status is reset after this check.</li>
     *   <li>General Case: For snakes with more than one segment, checks are performed to detect
     *       any collisions between the head and the segments, skipping the first segment.
     *       This is to avoid false positives that may arise due to the proximity of the first
     *       segment to the head.</li>
     * </ol>
     *
     * @return True if a self-collision is detected, either through a U-turn or a collision with another segment. False otherwise.
     */
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

        // Special case: check for U-turn if there is one or more segments
        if (gameManager.isUTurnDetected(player)) {
            if (DebugManager.isDebugEnabled) {
                Bukkit.getLogger().info(DebugManager.getDebugMessage("[GameEndConditionsHandler.java] U-turn self-collision detected!"));
            }
            gameManager.resetUTurnStatus(player);  // Reset the U-turn status
            return true;  // U-turn detected
        }

        // Iterate through the segments, skipping the first one
        for (int i = 1; i < segments.size(); i++) {
            Entity segment = segments.get(i);
            Vector segmentLocation = segment.getLocation().toVector();
            if (headLocation.distance(segmentLocation) < 0.1) {  // Tolerance of 0.1 blocks
                if (DebugManager.isDebugEnabled) {
                    Bukkit.getLogger().info(DebugManager.getDebugMessage("[GameEndConditionsHandler.java] Self-collision detected!"));
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
                Bukkit.getLogger().info(DebugManager.getDebugMessage("[GameEndConditionsHandler.java] Player dismounted!"));
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
                    Bukkit.getLogger().info(DebugManager.getDebugMessage("[GameEndConditionsHandler.java] A player has disconnected mid game!"));
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
