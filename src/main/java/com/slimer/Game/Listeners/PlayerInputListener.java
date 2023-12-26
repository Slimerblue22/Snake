package com.slimer.Game.Listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.slimer.Util.DebugManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;

public class PlayerInputListener {
    private final JavaPlugin plugin;
    private final Set<UUID> monitoredPlayers;
    private final Map<UUID, Vector> playerDirections;
    private final Random random = new Random();

    public PlayerInputListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.monitoredPlayers = new HashSet<>();
        this.playerDirections = new HashMap<>();
        setupPacketListener();
    }

    public void addPlayer(Player player) {
        monitoredPlayers.add(player.getUniqueId());

        // Set a random direction as default
        Vector[] possibleDirections = {
                new Vector(0, 0, -1),  // North
                new Vector(0, 0, 1),   // South
                new Vector(1, 0, 0),   // East
                new Vector(-1, 0, 0)   // West
        };

        Vector randomDirection = possibleDirections[random.nextInt(possibleDirections.length)];
        playerDirections.put(player.getUniqueId(), randomDirection);
        DebugManager.log(DebugManager.Category.DEBUG, "Player: " + player.getName() + " with UUID of " + player.getUniqueId() + " is now being monitored for inputs with default direction: " + randomDirection);
    }

    public void removePlayer(Player player) {
        monitoredPlayers.remove(player.getUniqueId());
        playerDirections.remove(player.getUniqueId());
        DebugManager.log(DebugManager.Category.DEBUG, "Player: " + player.getName() + " with UUID of " + player.getUniqueId() + " is no longer being monitored for inputs and their direction has been removed");
    }

    public Map<UUID, Vector> getPlayerDirections() {
        return playerDirections;
    }

    private void setupPacketListener() {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.STEER_VEHICLE) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                if (player != null && monitoredPlayers.contains(player.getUniqueId())) {
                    if (event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE) {
                        if (isPlayerHoldingForward(event)) {
                            Vector cardinalDirection = getYawAsCardinalDirectionVector(player.getLocation().getYaw());
                            playerDirections.put(player.getUniqueId(), cardinalDirection);
                            DebugManager.log(DebugManager.Category.DEBUG, "Player: " + player.getName() + " with UUID of " + player.getUniqueId() + " is facing vector: " + cardinalDirection);
                        }
                    }
                }
            }
        });
    }

    private Vector getYawAsCardinalDirectionVector(float yaw) {
        // Normalize the yaw value
        yaw = (yaw % 360 + 360) % 360;

        if (yaw >= 315 || yaw < 45) {
            return new Vector(0, 0, 1);  // South
        } else if (yaw >= 45 && yaw < 135) {
            return new Vector(-1, 0, 0);  // East
        } else if (yaw >= 135 && yaw < 225) {
            return new Vector(0, 0, -1);  // North
        } else { // yaw >= 225 && yaw < 315
            return new Vector(1, 0, 0);  // West
        }
    }

    private boolean isPlayerHoldingForward(PacketEvent event) {
        // Extract forward value from packet and return whether it's positive (indicating forward movement)
        float forward = event.getPacket().getFloat().read(1);
        return forward > 0;
    }
}