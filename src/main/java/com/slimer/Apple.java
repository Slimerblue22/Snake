package com.slimer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class Apple {
    private final Object lock = new Object();
    private final WorldGuardManager worldGuardManager;
    private Block block;
    private ArmorStand armorStandLabel;
    private final Snake snake;
    private final JavaPlugin plugin;
    private final GameManager gameManager;
    private final Player player;
    // The Apple constructor setting the plugin, world, location, and snake. It also spawns an apple in the game.
    public Apple(GameManager gameManager, Player player, SnakePlugin plugin, World world, Location playerLocation, Snake snake, WorldGuardManager worldGuardManager) {
        this.gameManager = gameManager;
        this.player = player;
        this.plugin = plugin;
        this.snake = snake;
        this.worldGuardManager = worldGuardManager;
        spawnApple(world, playerLocation);
    }

    // This method generates a random location for an apple within a certain range around the player.
    public Location getRandomLocation(World world, Location snakeLocation) {
        Location min = worldGuardManager.getGameZoneMinimum();
        Location max = worldGuardManager.getGameZoneMaximum();

        if (min == null || max == null) {
            Bukkit.getLogger().severe("Unable to get game zone boundaries!");
            return null;
        }

        int x = getRandomNumberInRange(min.getBlockX(), max.getBlockX());
        int z = getRandomNumberInRange(min.getBlockZ(), max.getBlockZ());
        int y = snakeLocation.getBlockY(); // Y-coordinate of the snake's head

        return new Location(world, x, y, z);
    }

    private int getRandomNumberInRange(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("Max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }
    // This method generates a random location for an apple that is not in the same block as any part of the snake's body.
    public Location randomLocation(World world, Location playerLocation) {
        Location location;
        do {
            Location newLocation = getRandomLocation(world, playerLocation);
            if (snake.getBody().stream().noneMatch(sheep -> sheep.getLocation().getBlock().equals(newLocation.getBlock()))) {
                location = newLocation;
            } else {
                location = null;
            }
        } while (location == null);
        return location;
    }
    // This method spawns an apple at a random location within a certain range around the player, ensuring that it doesn't spawn in the same block as any part of the snake's body, or in a block that isn't air, or with a non-air block directly above it.
    private void spawnApple(World world, Location location) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            synchronized (lock) {
                clearApple();
                Set<BlockLocation> attemptedLocations = new HashSet<>();
                for (int attempts = 0; attempts < 400; attempts++) {
                    BlockLocation newBlockLocation;
                    int innerAttempts = 0;
                    do {
                        Location newLocation = randomLocation(world, location);
                        newBlockLocation = new BlockLocation(newLocation.getBlockX(), newLocation.getBlockY(), newLocation.getBlockZ());
                        innerAttempts++;
                        if (innerAttempts > 400) {
                            return;
                        }
                    } while (attemptedLocations.contains(newBlockLocation));
                    attemptedLocations.add(newBlockLocation);
                    Location newLocation = new Location(world, newBlockLocation.getX(), newBlockLocation.getY(), newBlockLocation.getZ());
                    this.block = world.getBlockAt(newLocation);
                    if (!this.block.getType().equals(Material.AIR)) {
                        // DEBUG Bukkit.getLogger().info("Attempt " + attempts + ": Block at X: " + newBlockLocation.getX() + ", Y: " + newBlockLocation.getY() + ", Z: " + newBlockLocation.getZ() + " is not air. Trying another location...");
                        continue;
                    }
                    Block blockBelow = world.getBlockAt(newLocation.subtract(0, 1, 0));
                    if (!blockBelow.getType().isSolid()) {
                        // DEBUG Bukkit.getLogger().info("Attempt " + attempts + ": Block below X: " + newBlockLocation.getX() + ", Y: " + newBlockLocation.getY() + ", Z: " + newBlockLocation.getZ() + " is not solid. Trying another location...");
                        continue;
                    }
                    boolean obstructed = false;
                    for (int i = 1; i <= 3; i++) {
                        Block blockAbove = world.getBlockAt(newLocation.add(0, i, 0));
                        if (!blockAbove.getType().equals(Material.AIR)) {
                            obstructed = true;
                            // DEBUG Bukkit.getLogger().info("Attempt " + attempts + ": Block above X: " + newBlockLocation.getX() + ", Y: " + newBlockLocation.getY() + ", Z: " + newBlockLocation.getZ() + " is not air. Trying another location...");
                            break;
                        }
                    }
                    if (obstructed) {
                        continue;
                    }
                    this.block.setType(Material.PLAYER_HEAD);
                    Skull skull = (Skull) block.getState();
                    UUID ownerUUID = Bukkit.getOfflinePlayer("MHF_Apple").getUniqueId(); // Spawn the apple
                    skull.setOwningPlayer(Bukkit.getOfflinePlayer(ownerUUID));
                    skull.update();
                    Location armorStandLoc = block.getLocation().add(0.5, 1, 0.5);
                    this.armorStandLabel = world.spawn(armorStandLoc, ArmorStand.class, as -> { // Spawn an invis armor stand
                        DyeColor sheepColor = snake.getColor();
                        TextColor textColor = dyeColorToTextColor(sheepColor);
                        as.customName(Component.text(snake.getPlayer().getName() + "'s Apple", textColor)); // Set the name to {player}'s apple in the color of their sheep
                        as.setCustomNameVisible(true);
                        as.setVisible(false);
                        as.setMarker(true);
                        as.setGravity(false);
                        as.setSmall(true);
                    });
                    // DEBUG Bukkit.getLogger().info("Apple successfully placed at X: " + newBlockLocation.getX() + ", Y: " + newBlockLocation.getY() + ", Z: " + newBlockLocation.getZ() + " on attempt " + attempts);
                    return;
                }
                player.sendMessage(Component.text("Failed to find a new location for the apple after multiple attempts.", NamedTextColor.RED));
                player.sendMessage(Component.text("Please contact the server administrator.", NamedTextColor.RED));
                gameManager.endGame(player);
                Bukkit.getLogger().severe("Failed to find a suitable location for the apple after multiple attempts.");
            }
        }, 1L);
    }

    public void clearApple() {
        if (this.block != null && this.block.getType().equals(Material.PLAYER_HEAD)) {
            this.block.setType(Material.AIR);
            this.block = null;
            if (this.armorStandLabel != null) {
                this.armorStandLabel.remove();
                this.armorStandLabel = null;
            }
        }
    }
    public TextColor dyeColorToTextColor(DyeColor dyeColor) {
        return switch (dyeColor) {
            case ORANGE -> NamedTextColor.GOLD;
            case MAGENTA, PINK -> NamedTextColor.LIGHT_PURPLE;
            case LIGHT_BLUE -> NamedTextColor.AQUA;
            case YELLOW -> NamedTextColor.YELLOW;
            case LIME -> NamedTextColor.GREEN;
            case GRAY -> NamedTextColor.DARK_GRAY;
            case LIGHT_GRAY -> NamedTextColor.GRAY;
            case CYAN -> NamedTextColor.DARK_AQUA;
            case PURPLE -> NamedTextColor.DARK_PURPLE;
            case BLUE -> NamedTextColor.BLUE;
            case BROWN -> NamedTextColor.DARK_RED; // No direct brown color in TextColor, using a similar shade
            case GREEN -> NamedTextColor.DARK_GREEN;
            case RED -> NamedTextColor.RED;
            case BLACK -> NamedTextColor.BLACK;
            default -> NamedTextColor.WHITE; // Default to white if something goes wrong
        };
    }

    public Block getBlock() {
        return block;
    }
}