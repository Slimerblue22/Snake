package com.slimer.Game;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Manages the movement mechanics of the snakes in the game.
 * This class is responsible for updating and moving both the head entity and the segments of each snake.
 * Movement is determined based on player inputs and target positions.
 * The class uses waypoints to store intermediate positions for smooth and accurate snake movement.
 */
public class SnakeMovement {
    private final GameManager gameManager;
    private final double desiredSpeedInBlocksPerSecond = 5.0;
    private final Map<Player, Vector> playerTargetPositions = new HashMap<>();
    private final Map<Player, Deque<Vector>> playerWaypoints = new HashMap<>();
    private final Map<Entity, Vector> lastPositions = new HashMap<>();
    private PlayerInputHandler playerInputHandler;

    /**
     * Constructs a new SnakeMovement object.
     *
     * @param gameManager        The GameManager instance responsible for overall game management.
     * @param playerInputHandler The PlayerInputHandler instance responsible for handling player inputs.
     */
    public SnakeMovement(GameManager gameManager, PlayerInputHandler playerInputHandler) {
        this.gameManager = gameManager;
        this.playerInputHandler = playerInputHandler;
    }

    /**
     * Gets the last position of either the last segment or the head entity of a given player's snake.
     *
     * @param player The player whose snake's last position is to be retrieved.
     * @return The Vector representing the last position.
     */
    public Vector getLastPositionOfLastSegmentOrHead(Player player) {
        List<Entity> segments = gameManager.getSegmentsForPlayer(player);
        Entity lastSegment = (segments != null && !segments.isEmpty()) ? segments.get(segments.size() - 1) : null;
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
        playerTargetPositions.remove(player);
        Deque<Vector> waypoints = playerWaypoints.get(player);
        if (waypoints != null) {
            waypoints.clear();
        }
    }

    /**
     * Initializes the target position for a player's snake based on its initial position.
     *
     * @param player          The player whose snake's target position is to be initialized.
     * @param initialPosition The initial position of the snake.
     */
    public void initializeTargetPositionForPlayer(Player player, Vector initialPosition) {
        // Get the current direction of the player's snake
        Vector initialDirection = playerInputHandler.getCurrentDirection(player);

        // Calculate the initial target position by adding the direction to the initial position
        Vector initialTargetPosition = initialPosition.clone().add(initialDirection);

        // Store the initial target position for the player's snake
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
        // Get the SnakeCreation object for the given player
        SnakeCreation snake = gameManager.getSnakeForPlayer(player);

        // If the player has a snake
        if (snake != null) {
            // Get all the segments of the snake
            List<Entity> segments = gameManager.getSegmentsForPlayer(player);

            // Count the number of segments
            int numSegments = segments != null ? segments.size() : 0;

            // Initialize or get the existing waypoints queue for the snake
            playerWaypoints.computeIfAbsent(player, k -> new LinkedList<>());

            // Remove extra waypoints if any
            while (playerWaypoints.get(player).size() > numSegments + 1) {
                playerWaypoints.get(player).removeFirst();
            }

            // Get the 'head' entity of the snake (a sheep in this case)
            Entity sheepEntity = snake.getSheepEntity();

            // Update last position of the head.
            lastPositions.put(sheepEntity, sheepEntity.getLocation().toVector());

            // Update last positions of the segments.
            if (segments != null) {
                for (Entity segment : segments) {
                    lastPositions.put(segment, segment.getLocation().toVector());
                }
            }

            // Get the current position of the 'head' entity
            Vector currentPosition = sheepEntity.getLocation().toVector();

            // Initialize or update the target position for the snake
            initializeOrUpdateTargetPosition(player, currentPosition, direction);

            // Move the 'head' entity
            moveHead(sheepEntity, currentPosition, direction, player);

            // Move all segments of the snake
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
        // Fetch the current target position for the player's snake
        Vector targetPosition = playerTargetPositions.get(player);

        // Initialize or get existing waypoints for the player's snake
        playerWaypoints.computeIfAbsent(player, k -> new LinkedList<>());

        // Initialize the target position if not set
        if (targetPosition == null) {
            targetPosition = new Vector(
                    Math.floor(currentPosition.getX()) + 0.5,
                    currentPosition.getY(),
                    Math.floor(currentPosition.getZ()) + 0.5
            );
            playerWaypoints.get(player).addLast(targetPosition.clone());
        }

        // Update the target position if the snake is near it
        if (currentPosition.distance(targetPosition) < 0.1) {
            targetPosition.add(currentDirection);
            if (playerWaypoints.get(player).isEmpty() ||
                    !Objects.equals(playerWaypoints.get(player).peekLast(), targetPosition)) {
                playerWaypoints.get(player).addLast(targetPosition.clone());
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
        // Get the target position for the snake
        Vector targetPosition = playerTargetPositions.get(player);

        // Calculate the velocity needed to reach the target position
        Vector velocity = targetPosition.clone().subtract(currentPosition).normalize()
                .multiply(desiredSpeedInBlocksPerSecond / 20.0);

        // Set the calculated velocity
        entity.setVelocity(velocity);

        // Calculate and set the yaw rotation for the entity based on direction
        float yaw = (float) Math.toDegrees(Math.atan2(-currentDirection.getX(), currentDirection.getZ()));
        entity.setRotation(yaw, entity.getLocation().getPitch());
    }

    /**
     * Moves the segments of a given player's snake.
     *
     * @param player The player whose snake's segments are to be moved.
     */
    private void moveSegments(Player player) {
        // Get all the segments of the snake
        List<Entity> segments = gameManager.getSegmentsForPlayer(player);

        // Exit if there are no segments
        if (segments == null || segments.isEmpty()) {
            return;
        }

        // Get the waypoints for the segments
        Deque<Vector> waypoints = new LinkedList<>(playerWaypoints.get(player));

        // Reverse the waypoints so that the last waypoint comes first
        Collections.reverse((List<?>) waypoints);

        // Start from the second waypoint as the first is for the head
        Iterator<Vector> waypointIterator = waypoints.iterator();
        if (waypointIterator.hasNext()) {
            waypointIterator.next();
        }

        for (Entity segment : segments) {
            if (!waypointIterator.hasNext()) {
                break;
            }
            Vector waypoint = waypointIterator.next();
            Vector currentPosition = segment.getLocation().toVector();
            Vector velocity = waypoint.clone().subtract(currentPosition).normalize()
                    .multiply(desiredSpeedInBlocksPerSecond / 20.0);

            // Set the calculated velocity if it's finite
            if (Double.isFinite(velocity.getX()) && Double.isFinite(velocity.getY()) && Double.isFinite(velocity.getZ())) {
                segment.setVelocity(velocity);

                // Calculate and set the yaw rotation for the segment based on the velocity direction
                float yaw = (float) Math.toDegrees(Math.atan2(-velocity.getX(), velocity.getZ()));
                segment.setRotation(yaw, segment.getLocation().getPitch());
            }
        }
    }
}