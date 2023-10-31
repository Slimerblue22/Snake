package com.slimer.Game;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.slimer.Util.DebugManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

/**
 * This class is responsible for handling player inputs for controlling snake movement.
 * <p>
 * Last updated: V2.1.0
 * @author Slimerblue22
 */
public class PlayerInputHandler {
    private final Plugin plugin;
    private final Map<Player, Vector> playerDirections = new HashMap<>();
    private final Random random = new Random();
    private final GameManager gameManager;
    private final Map<Player, LinkedList<Vector>> lastTwoDirections = new HashMap<>();

    /**
     * Constructs a PlayerInputHandler and registers the packet listener for key presses.
     *
     * @param plugin The Bukkit plugin.
     * @param gameManager The game manager instance responsible for handling game logic.
     */
    public PlayerInputHandler(Plugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        registerPacketListener();
        this.gameManager = gameManager;
    }

    /**
     * Starts monitoring a player's direction based on input and initializes it to a random direction.
     *
     * @param player The player to be monitored.
     */
    public void startMonitoring(Player player) {
        DebugManager.log(DebugManager.Category.PLAYER_INPUT, "Starting to monitor player: " + player.getName());

        // Possible directions: North, South, East, West
        Vector[] possibleDirections = {
                new Vector(0, 0, -1),  // North
                new Vector(0, 0, 1),   // South
                new Vector(1, 0, 0),   // East
                new Vector(-1, 0, 0)   // West
        };

        // Initialize to a random direction
        Vector randomDirection = possibleDirections[random.nextInt(possibleDirections.length)];
        playerDirections.put(player, randomDirection);
    }

    /**
     * Stops monitoring a player's direction by removing them from the playerDirections map.
     *
     * @param player The player to stop monitoring.
     */
    public void stopMonitoring(Player player) {
        DebugManager.log(DebugManager.Category.PLAYER_INPUT, "Stopping monitoring of player: " + player.getName());
        playerDirections.remove(player);
    }

    /**
     * Retrieves the current direction of a player.
     *
     * @param player The player whose current direction is to be retrieved.
     * @return The current direction Vector, or a zero Vector if not found.
     */
    public Vector getCurrentDirection(Player player) {
        return playerDirections.getOrDefault(player, new Vector());
    }

    /**
     * Registers a packet listener to handle incoming packets related to vehicle steering,
     * which is used to detect the player's input direction for snake movement.
     */
    private void registerPacketListener() {
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(plugin, PacketType.Play.Client.STEER_VEHICLE) {
                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        handleKeyPress(event);
                    }
                });
    }

    /**
     * Handles the key press events to update the direction of the player.
     * Specifically, it listens for the W key to determine the direction.
     *
     * @param event The PacketEvent containing the packet data.
     */
    private void handleKeyPress(PacketEvent event) {
        // Check if the packet is related to vehicle steering
        if (event.getPacketType() != PacketType.Play.Client.STEER_VEHICLE) {
            return;
        }

        Player player = event.getPlayer();
        // Check if the player is in a game, if not, return early
        if (gameManager.getSnakeForPlayer(player) == null) {
            return;
        }

        float forward = event.getPacket().getFloat().read(1);
        // Only proceed if the W key is pressed
        if (forward <= 0) {
            return;
        }

        float yaw = player.getLocation().getYaw();
        yaw = (yaw % 360 + 360) % 360;  // Normalize yaw to [0, 360)

        Vector newDirection;
        String facingDirection;
        DebugManager.log(DebugManager.Category.PLAYER_INPUT, "Handling key press for player: " + event.getPlayer().getName());

        // Determine the new direction based on player's yaw
        if (yaw >= 315 || yaw < 45) {
            newDirection = new Vector(0, 0, 1);  // South
            facingDirection = "South";
        } else if (yaw >= 45 && yaw < 135) {
            newDirection = new Vector(-1, 0, 0);  // East
            facingDirection = "East";
        } else if (yaw >= 135 && yaw < 225) {
            newDirection = new Vector(0, 0, -1);  // North
            facingDirection = "North";
        } else {  // yaw >= 225 && yaw < 315
            newDirection = new Vector(1, 0, 0);  // West
            facingDirection = "West";
        }

        playerDirections.put(player, newDirection);
        DebugManager.log(DebugManager.Category.PLAYER_INPUT, "Player: " + player.getName() + " is facing: " + facingDirection);

        // Update direction history
        LinkedList<Vector> directions = lastTwoDirections.getOrDefault(player, new LinkedList<>());
        directions.addLast(newDirection);
        if (directions.size() > 2) {
            directions.removeFirst();
        }
        lastTwoDirections.put(player, directions);

        // Check for U-turns
        if (isUTurn(player)) {
            gameManager.notifyUTurn(player);
        }
    }

    /**
     * Checks if the player made a U-turn based on the last two directions.
     *
     * @param player The player to check.
     * @return True if a U-turn is detected, false otherwise.
     */
    private boolean isUTurn(Player player) {
        LinkedList<Vector> directions = lastTwoDirections.get(player);
        if (directions == null || directions.size() < 2) {
            return false;
        }
        Vector last = directions.getLast();
        Vector secondLast = directions.get(directions.size() - 2);

        // Check if the last direction is the opposite of the second last
        return last.clone().multiply(-1).equals(secondLast);
    }
}