package com.slimer.Game;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * This class is responsible for handling player inputs for controlling snake movement.
 */
public class PlayerInputHandler {
    private final Plugin plugin;
    private final Map<Player, Vector> playerDirections = new HashMap<>();
    private final Random random = new Random();

    /**
     * Constructs a PlayerInputHandler and registers the packet listener for key presses.
     *
     * @param plugin The Bukkit plugin.
     */
    public PlayerInputHandler(Plugin plugin) {
        this.plugin = plugin;
        new Vector();
        registerPacketListener();
    }

    /**
     * Starts monitoring a player's direction based on input.
     * Initializes the player's direction to a random direction.
     *
     * @param player The player to be monitored.
     */
    public void startMonitoring(Player player) {
        // Possible directions the snake can move
        Vector[] possibleDirections = {
                new Vector(0, 0, -1),  // North
                new Vector(0, 0, 1),   // South
                new Vector(1, 0, 0),   // East
                new Vector(-1, 0, 0)   // West
        };

        // Initialize player's direction to a random one
        Vector randomDirection = possibleDirections[random.nextInt(possibleDirections.length)];
        playerDirections.put(player, randomDirection);
    }

    /**
     * Stops monitoring a player's direction by removing them from the playerDirections map.
     *
     * @param player The player to stop monitoring.
     */
    public void stopMonitoring(Player player) {
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
     * which is used to detect the player's input direction.
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
        if (event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE) {
            Player player = event.getPlayer();
            float forward = event.getPacket().getFloat().read(1);

            // If W key is pressed, update the direction based on player's yaw
            if (forward > 0) {
                float yaw = player.getLocation().getYaw();
                yaw = (yaw % 360 + 360) % 360;
                Vector newDirection;

                if (yaw >= 315 || yaw < 45) {
                    newDirection = new Vector(0, 0, 1);  // South
                } else if (yaw >= 45 && yaw < 135) {
                    newDirection = new Vector(-1, 0, 0);  // East
                } else if (yaw >= 135 && yaw < 225) {
                    newDirection = new Vector(0, 0, -1);  // North
                } else {  // yaw >= 225 && yaw < 315
                    newDirection = new Vector(1, 0, 0);  // West
                }
                playerDirections.put(player, newDirection);
            }
        }
    }
}
