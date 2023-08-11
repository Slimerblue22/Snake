package com.slimer;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.*;

import java.util.LinkedList;
import java.util.List;

// Represents a player-controlled snake in the game.
public class Snake {
    private Apple apple; // Represents the apple the snake eats.
    private ArmorStand armorStand; // The invisible armor stand the player will be mounted on
    private final GameManager gameManager; // Manages the game instances.
    private final LinkedList<Sheep> body; // The snake's body, represented by a list of sheep entities.
    public LinkedList<Sheep> getBody() {
        return this.body;
    }
    private Direction currentDirection; // The current direction the snake is moving in.
    private final DyeColor color; // The color of the snake.
    private final Player player; // The player controlling the snake.
    private final ProtocolManager protocolManager; // Manages protocol interactions.
    private final PacketAdapter packetAdapter; // Adapter for receiving packet events.
    public PacketAdapter getPacketAdapter() {
        return this.packetAdapter;
    }
    private int score = 0; // The current score of the snake.

    public DyeColor getColor() { // Receive user's snake color.
        return this.color;
    }

    public ArmorStand getArmorStand() {
        return armorStand;
    }

    public enum Direction { NORTH, EAST, SOUTH, WEST } // Enum representing the possible directions for the snake.

    // Constructor initializes the snake.
    public Snake(Player player, Sheep initialSegment, GameManager gameManager, ProtocolManager protocolManager, PlayerData playerData, ArmorStand armorStand) {
        initialSegment.setSilent(true);
        this.player = player;
        this.body = new LinkedList<>();
        this.armorStand = armorStand;
        String colorName = playerData.getConfig().getString(player.getUniqueId() + ".color", "WHITE");
        this.color = DyeColor.valueOf(colorName);
        initialSegment.setColor(this.color);
        initialSegment.setAI(false);
        this.body.add(initialSegment);
        this.gameManager = gameManager;
        this.protocolManager = protocolManager;
        float yaw = player.getEyeLocation().getYaw();
        yaw = (yaw % 360 + 360) % 360;
        if (yaw < 45 || yaw > 315) {
            this.currentDirection = Direction.SOUTH;
        } else if (yaw < 135) {
            this.currentDirection = Direction.WEST;
        } else if (yaw < 225) {
            this.currentDirection = Direction.NORTH;
        } else {
            this.currentDirection = Direction.EAST;
        }

        this.packetAdapter = new PacketAdapter(gameManager.getPlugin(), PacketType.Play.Client.STEER_VEHICLE) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (event.getPlayer() == player) {
                    handleSteerVehiclePacket(event.getPacket());
                }
            }
        };

        protocolManager.addPacketListener(this.packetAdapter);
    }
    public Apple getApple() { // Returns the apple associated with the snake.
        return this.apple;
    }
    public void setApple(Apple apple) { // Sets the apple for the snake.
        this.apple = apple;
    }
    public Player getPlayer() { // Returns the player controlling the snake.
        return this.player;
    }
    public void stop() {
        // Remove the armor stand first
        if (armorStand != null) {
            armorStand.remove();
            armorStand = null;  // Set to null to ensure we don't try to remove it again
        }

        // Then remove all segments of the snake
        for (Sheep segment : body) {
            segment.remove();
        }

        apple.clearApple();
        body.clear();
        protocolManager.removePacketListener(this.packetAdapter);
    }
    public int getScore() { // Returns the current score of the snake.
        return score;
    }
    public void move() { // Moves the snake in the current direction.
        move(currentDirection);
    }
    public void eatApple() { // Consumes the apple, increasing the snake's size and score.
        apple.clearApple();
        apple = new Apple(gameManager, player, gameManager.getPlugin(), player.getWorld(), player.getLocation(), this, gameManager.getWorldGuardManager());
        score++;
        addSegment();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
    }
    public void move(Direction direction) { // Moves the snake in a given direction.
        Sheep head = body.getFirst();
        final Location[] currentLocation = {head.getLocation().clone()};
        Location newLocation = switch (currentDirection) {
            case NORTH -> currentLocation[0].add(0, 0, -1);
            case EAST -> currentLocation[0].add(1, 0, 0);
            case SOUTH -> currentLocation[0].add(0, 0, 1);
            case WEST -> currentLocation[0].add(-1, 0, 0);
        };
        Location appleLocation = null;
        if (apple != null && apple.getBlock() != null) {
            appleLocation = apple.getBlock().getLocation();
        }

        // Check if appleLocation is null, and if so, exit the method early
        if (appleLocation == null) {
            return;
        }

        Location[] lastSegmentLocation = { head.getLocation().clone() };

        if (newLocation.getBlockX() == appleLocation.getBlockX() && newLocation.getBlockY() == appleLocation.getBlockY() && newLocation.getBlockZ() == appleLocation.getBlockZ()) {
            eatApple();
        }
        newLocation.setX(newLocation.getBlockX() + 0.5);
        newLocation.setY(newLocation.getBlockY());
        newLocation.setZ(newLocation.getBlockZ() + 0.5);

        // Get the passengers of the armor stand.
        List<Entity> armorStandPassengers = armorStand.getPassengers();
        if (!armorStandPassengers.isEmpty() && armorStandPassengers.get(0) instanceof Player playerRiding) {

            // Dismount the player from the armor stand.
            armorStand.eject();

            // Set the head's yaw based on direction.
            float newYaw = switch (currentDirection) {
                case NORTH -> 180.0F;
                case EAST  -> -90.0F;
                case SOUTH -> 0.0F;
                case WEST  -> 90.0F;
            };
            newLocation.setYaw(newYaw);

            // Teleport the sheep and armor stand.
            head.teleport(newLocation);

            Location armorStandNewLocation = newLocation.clone().add(0, 1, 0);  // Armor stand 1 level above sheep.
            armorStand.teleport(armorStandNewLocation);

            // Remount the player to the armor stand.
            armorStand.addPassenger(playerRiding);

            player.playSound(newLocation, Sound.BLOCK_WOOL_STEP, 1.0F, 1.0F);
        } else {
            head.teleport(newLocation);
        }
        gameManager.getPlugin().getServer().getScheduler().runTaskLater(gameManager.getPlugin(), () -> {
            for (int i = 1; i < body.size(); i++) {
                Sheep currentSegment = body.get(i);
                Location currentSegmentLocation = currentSegment.getLocation().clone();
                currentSegment.teleport(lastSegmentLocation[0]);
                lastSegmentLocation[0] = currentSegmentLocation;
            }
            for (int i = 1; i < body.size(); i++) {
                Sheep currentSegment = body.get(i);
                Sheep segmentInFront = body.get(i - 1);
                Location locationOfSegmentInFront = segmentInFront.getLocation();

                currentLocation[0] = currentSegment.getLocation();
                double dx = locationOfSegmentInFront.getX() - currentLocation[0].getX();
                double dz = locationOfSegmentInFront.getZ() - currentLocation[0].getZ();

                float newYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                currentLocation[0].setYaw(newYaw);
                currentSegment.teleport(currentLocation[0]);
            }
        }, 1L);
    }

    // Handles the player's vehicle steer packet to control the snake's direction.
    private void handleSteerVehiclePacket(PacketContainer packet) {
        float forward = packet.getFloat().read(1);

        float playerYaw = player.getLocation().getYaw();
        playerYaw = (playerYaw % 360 + 360) % 360;

        if (forward > 0) {
            if (playerYaw < 45 || playerYaw > 315) {
                currentDirection = Direction.SOUTH;
            } else if (playerYaw < 135) {
                currentDirection = Direction.WEST;
            } else if (playerYaw < 225) {
                currentDirection = Direction.NORTH;
            } else {
                currentDirection = Direction.EAST;
            }
        }
    }
    public void addSegment() {
        Location newSegmentLocation = body.getLast().getLocation().clone();
        Sheep newSegment = (Sheep) player.getWorld().spawnEntity(newSegmentLocation, EntityType.SHEEP);
        newSegment.setAI(false);
        newSegment.setColor(this.color);
        body.add(newSegment);
        newSegment.setSilent(true);
    }
    public boolean isAlive() {
        Sheep head = body.getFirst();
        Location headLocation = head.getLocation();
        for (Sheep segment : body) {
            if (segment != head && segment.getLocation().getBlock().equals(headLocation.getBlock())) {
                return false;
            }
        }
        return true;
    }
}