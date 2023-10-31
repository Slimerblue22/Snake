package com.slimer.Game;

import com.slimer.Main.Main;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;
/*
  ==============================================================================
                               IMPORTANT NOTICE
  ==============================================================================

  The SnakeMovement class contains complex logic for managing the movement of
  snakes in the game. While the current implementation works, it's held together
  with hopes and dreams and is a prime candidate for refactoring.

  Reasons for Refactoring:
  - Mixed Responsibilities: The class currently manages both waypoints and actual entity movement.
  - Complex Methods: Some methods contain complex conditions and calculations that could be simplified.
  - Magic Numbers: The code contains magic numbers that could be replaced with named constants.
  - Null Checks: There are multiple null checks for list and map values that could be simplified.
  - Differing Speed Issues: Different speeds often have unexpected results. Some work, some don't.

  Suggestions for Refactoring:
  - Separate waypoint management from movement logic to improve code clarity.
  - Simplify complex methods by breaking them into smaller, more focused functions.
  - Replace magic numbers with named constants for improved readability.
  - Streamline null checks and error handling for more concise code.

  Before any refactoring efforts:
  - Review the existing logic to understand the "why" behind each implementation detail.
  - Consider the fact that this is a core component of the game and edits will likely break other classes.
  - Thoroughly test the refactored code to avoid regressions.

  ==============================================================================
 */

/**
 * Manages the movement mechanics of the snakes in the game.
 * This class is responsible for updating and moving both the head entity and the segments of each snake.
 * Movement is determined based on player inputs and target positions.
 * The class uses waypoints to store intermediate positions for smooth and accurate snake movement.
 * <p>
 * Last updated: V2.1.0
 * @author Slimerblue22
 */
public class SnakeMovement {
    private final GameManager gameManager;
    private final double desiredSpeedInBlocksPerSecond;
    private final double forceTeleportDistance;
    private final double targetCloseEnoughDistance;
    private final Map<Player, Vector> playerTargetPositions = new HashMap<>();
    private final Map<Player, Deque<Vector>> playerWaypoints = new HashMap<>();
    private final Map<Entity, Vector> lastPositions = new HashMap<>();
    private PlayerInputHandler playerInputHandler;

    /**
     * Constructs a new SnakeMovement object.
     *
     * @param gameManager        The GameManager instance responsible for overall game management.
     * @param playerInputHandler The PlayerInputHandler instance responsible for handling player inputs.
     * @param plugin             The main plugin instance, used to access configuration values.
     */
    public SnakeMovement(GameManager gameManager, PlayerInputHandler playerInputHandler, JavaPlugin plugin) {
        this.gameManager = gameManager;
        this.playerInputHandler = playerInputHandler;
        Main mainPlugin = (Main) plugin;
        this.desiredSpeedInBlocksPerSecond = mainPlugin.getSnakeSpeed();
        this.forceTeleportDistance = mainPlugin.getForceTeleportDistance();
        this.targetCloseEnoughDistance = mainPlugin.getTargetCloseEnoughDistance();
    }

    /**
     * Gets the last position of either the last segment or the head entity of a given player's snake.
     *
     * @param player The player whose snake's last position is to be retrieved.
     * @return The Vector representing the last position.
     */
    public Vector getLastPositionOfLastSegmentOrHead(Player player) {
        // Retrieve the list of segments for the player's snake
        List<Entity> segments = gameManager.getSegmentsForPlayer(player);
        // Get the last segment, if available
        Entity lastSegment = (segments != null && !segments.isEmpty()) ? segments.get(segments.size() - 1) : null;
        // Return the last position of either the last segment or the snake's head
        return (lastSegment != null) ? lastPositions.get(lastSegment) : lastPositions.get(gameManager.getSnakeForPlayer(player).getSheepEntity());
    }

    /**
     * Sets the PlayerInputHandler for handling player inputs.
     *
     * @param playerInputHandler The PlayerInputHandler to be used.
     */
    public void setPlayerInputHandler(PlayerInputHandler playerInputHandler) {
        this.playerInputHandler = playerInputHandler;
    }

    /**
     * Clears the target position and waypoints for a given player's snake.
     *
     * @param player The player whose snake's target position and waypoints should be cleared.
     */
    public void clearTargetPosition(Player player) {
        // Remove the player's snake target position
        playerTargetPositions.remove(player);
        // Clear the waypoints for the player's snake
        Deque<Vector> waypoints = playerWaypoints.get(player);
        if (waypoints != null) {
            waypoints.clear();
        }
    }

    /**
     * Initializes the target position for a player's snake based on its initial position and direction.
     *
     * @param player          The player whose snake's target position is to be initialized.
     * @param initialPosition The initial position of the snake.
     */
    public void initializeTargetPositionForPlayer(Player player, Vector initialPosition) {
        // Calculate and store the initial target position based on the snake's initial position and direction
        Vector initialTargetPosition = initialPosition.clone().add(playerInputHandler.getCurrentDirection(player));
        playerTargetPositions.put(player, initialTargetPosition);
    }

    /**
     * Moves the snake based on the given player and direction.
     * This method updates the head and segments' positions.
     *
     * @param player    The player controlling the snake.
     * @param direction The direction in which the snake should move.
     */
    public void moveSnake(Player player, Vector direction) {
        SnakeCreation snake = gameManager.getSnakeForPlayer(player);
        if (snake != null) {
            List<Entity> segments = gameManager.getSegmentsForPlayer(player);
            int numSegments = (segments != null) ? segments.size() : 0;

            // Ensure waypoints list is initialized for the player
            playerWaypoints.computeIfAbsent(player, k -> new LinkedList<>());

            // Adjust waypoints list size to match segment count
            while (playerWaypoints.get(player).size() > numSegments + 1) {
                playerWaypoints.get(player).removeFirst();
            }

            // Cache current positions
            Entity sheepEntity = snake.getSheepEntity();
            lastPositions.put(sheepEntity, sheepEntity.getLocation().toVector());

            if (segments != null) {
                for (Entity segment : segments) {
                    lastPositions.put(segment, segment.getLocation().toVector());
                }
            }

            // Update target and move entities
            Vector currentPosition = sheepEntity.getLocation().toVector();
            initializeOrUpdateTargetPosition(player, currentPosition, direction);
            moveHead(sheepEntity, currentPosition, direction, player);
            moveSegments(player);
        }
    }

    /**
     * Initializes or updates the target position for a player's snake.
     *
     * @param player           The player whose snake's target position is to be initialized or updated.
     * @param currentPosition  The current position of the snake's head.
     * @param currentDirection The current direction of the snake.
     */
    private void initializeOrUpdateTargetPosition(Player player, Vector currentPosition, Vector currentDirection) {
        Vector targetPosition = playerTargetPositions.get(player);
        // Initialize waypoints list if not present for the player
        playerWaypoints.computeIfAbsent(player, k -> new LinkedList<>());

        // Initialize target position if null
        if (targetPosition == null) {
            targetPosition = new Vector(
                    Math.floor(currentPosition.getX()) + 0.5,
                    currentPosition.getY(),
                    Math.floor(currentPosition.getZ()) + 0.5
            );
            playerWaypoints.get(player).addLast(targetPosition.clone());
        }

        // Round to one decimal place for precision
        double roundedCurrentX = Math.round(currentPosition.getX() * 10) / 10.0;
        double roundedCurrentZ = Math.round(currentPosition.getZ() * 10) / 10.0;
        double roundedTargetX = Math.round(targetPosition.getX() * 10) / 10.0;
        double roundedTargetZ = Math.round(targetPosition.getZ() * 10) / 10.0;

        // Check if the current position is close enough to the target
        boolean isCloseToTargetX = Math.abs(roundedCurrentX - roundedTargetX) <= targetCloseEnoughDistance;
        boolean isCloseToTargetZ = Math.abs(roundedCurrentZ - roundedTargetZ) <= targetCloseEnoughDistance;

        if (isCloseToTargetX && isCloseToTargetZ) {
            // Update target position
            targetPosition.add(currentDirection);

            // Update waypoints and target positions only if they are different
            Deque<Vector> currentWaypoints = playerWaypoints.get(player);
            if (currentWaypoints.isEmpty() || !Objects.equals(currentWaypoints.peekLast(), targetPosition)) {
                currentWaypoints.addLast(targetPosition.clone());
                playerTargetPositions.put(player, targetPosition);
            }
        }
    }

    /**
     * Moves the head entity of the snake to a new position.
     *
     * @param entity           The head entity of the snake.
     * @param currentPosition  The current position of the head entity.
     * @param currentDirection The current direction in which the head should move.
     * @param player           The player controlling the snake.
     */
    private void moveHead(Entity entity, Vector currentPosition, Vector currentDirection, Player player) {
        Vector targetPosition = playerTargetPositions.get(player);

        // Calculate new velocity
        Vector velocity = targetPosition.clone().subtract(currentPosition).normalize()
                .multiply(desiredSpeedInBlocksPerSecond / 20.0);
        entity.setVelocity(velocity);

        // Update entity rotation based on current direction
        float yaw = (float) Math.toDegrees(Math.atan2(-currentDirection.getX(), currentDirection.getZ()));
        entity.setRotation(yaw, entity.getLocation().getPitch());
    }

    /**
     * Moves the segments of a given player's snake.
     *
     * @param player The player whose snake's segments are to be moved.
     */
    private void moveSegments(Player player) {
        List<Entity> segments = gameManager.getSegmentsForPlayer(player);
        if (segments == null || segments.isEmpty()) {
            return;
        }

        Deque<Vector> waypoints = new LinkedList<>(playerWaypoints.get(player));
        Collections.reverse((List<?>) waypoints);
        Iterator<Vector> waypointIterator = waypoints.iterator();

        // Skip the first waypoint as it is for the head
        if (waypointIterator.hasNext()) {
            waypointIterator.next();
        }

        for (Entity segment : segments) {
            if (!waypointIterator.hasNext()) {
                break;
            }

            Vector waypoint = waypointIterator.next();
            Vector currentPosition = segment.getLocation().toVector();

            // Calculate new velocity
            Vector velocity = waypoint.clone().subtract(currentPosition).normalize()
                    .multiply(desiredSpeedInBlocksPerSecond / 20.0);

            // Calculate distance to next waypoint
            double distanceToWaypoint = currentPosition.distance(waypoint);

            // Teleport or move the segment based on the distance to waypoint
            if (distanceToWaypoint > forceTeleportDistance) {
                segment.teleport(waypoint.toLocation(segment.getWorld(), segment.getLocation().getYaw(), segment.getLocation().getPitch()));
            } else {
                if (Double.isFinite(velocity.getX()) && Double.isFinite(velocity.getY()) && Double.isFinite(velocity.getZ())) {
                    segment.setVelocity(velocity);
                    float yaw = (float) Math.toDegrees(Math.atan2(-velocity.getX(), velocity.getZ()));
                    segment.setRotation(yaw, segment.getLocation().getPitch());
                }
            }
        }
    }
}