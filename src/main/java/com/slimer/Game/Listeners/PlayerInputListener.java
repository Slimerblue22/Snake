package com.slimer.Game.Listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.slimer.Game.SnakeManagement.SnakeMovement;
import com.slimer.Util.DebugManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerInputListener {
    private final JavaPlugin plugin;
    private final Set<UUID> monitoredPlayers;
    private final SnakeMovement snakeMovement;

    public PlayerInputListener(JavaPlugin plugin, SnakeMovement snakeMovement) {
        this.plugin = plugin;
        this.snakeMovement = snakeMovement;
        this.monitoredPlayers = new HashSet<>();
        setupPacketListener();
    }

    public void addPlayer(Player player) {
        monitoredPlayers.add(player.getUniqueId());
        DebugManager.log(DebugManager.Category.DEBUG, "Player: " + player.getName() + " with UUID of " + player.getUniqueId() + " is now being monitored for inputs");
    }

    public void removePlayer(Player player) {
        monitoredPlayers.remove(player.getUniqueId());
        DebugManager.log(DebugManager.Category.DEBUG, "Player: " + player.getName() + " with UUID of " + player.getUniqueId() + " is no longer being monitored for inputs");
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
                            float yaw = player.getLocation().getYaw();
                            String cardinalDirection = getYawAsCardinalDirection(yaw);
                            DebugManager.log(DebugManager.Category.DEBUG, "Player: " + player.getName() + " with UUID of " + player.getUniqueId() + " is facing " + cardinalDirection);
                        }
                    }
                }
            }
        });
    }

    private boolean isPlayerHoldingForward(PacketEvent event) {
        // Extract forward value from packet and return whether it's positive (indicating forward movement)
        float forward = event.getPacket().getFloat().read(1);
        if (forward > 0) {
            Player player = event.getPlayer();
            snakeMovement.moveSnake(player);
            return true;
        }
        return false;
    }

    private String getYawAsCardinalDirection(float yaw) {
        // Normalize the yaw value and convert it to a cardinal direction
        yaw = (yaw % 360 + 360) % 360;
        if (yaw >= 315 || yaw < 45) {
            return "South";
        } else if (yaw >= 45 && yaw < 135) {
            return "West";
        } else if (yaw >= 135 && yaw < 225) {
            return "North";
        } else {  // yaw >= 225 && yaw < 315
            return "East";
        }
    }
}