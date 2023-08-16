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
    // Various class fields to manage the apple's state, snake game, world, etc.
    private final Object lock = new Object();
    private final WorldGuardManager worldGuardManager;
    private final Snake snake;
    private final JavaPlugin plugin;
    private final GameManager gameManager;
    private final Player player;
    private Block block;
    private ArmorStand armorStandLabel;

    public Apple(GameManager gameManager, Player player, SnakePlugin plugin, World world, Location playerLocation, Snake snake, WorldGuardManager worldGuardManager) {
        this.gameManager = gameManager;
        this.player = player;
        this.plugin = plugin;
        this.snake = snake;
        this.worldGuardManager = worldGuardManager;
        spawnApple(world, playerLocation);
    }

    // Returns a random location within the game zone's boundaries.
    public Location getRandomLocation(World world, Location snakeLocation) {
        // Retrieve the game zone name directly
        String gameZoneName = worldGuardManager.getPlayerGameZone(player);
        if (gameZoneName == null) {
            Bukkit.getLogger().severe("Unable to get game zone for player: " + player.getName());
            return null;
        }

        Location[] bounds = worldGuardManager.getGameZoneBounds(gameZoneName);
        if (bounds == null) {
            Bukkit.getLogger().severe("Unable to get game zone boundaries for: " + gameZoneName);
            return null;
        }

        Location min = bounds[0];
        Location max = bounds[1];

        int x = getRandomNumberInRange(min.getBlockX(), max.getBlockX());
        int z = getRandomNumberInRange(min.getBlockZ(), max.getBlockZ());
        int y = snakeLocation.getBlockY(); // Keep this unchanged

        return new Location(world, x, y, z);
    }

    // Helper method to get a random number within the specified range.
    private int getRandomNumberInRange(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("Max must be greater than min");
        }
        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    // Generates a random location in the world that is unoccupied by the snake.
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

    // Method to spawn the apple in the world, ensuring it is placed in a valid location.
    private void spawnApple(World world, Location location) {
        // Retrieve the game zone name directly
        String gameZoneName = worldGuardManager.getPlayerGameZone(player);
        if (gameZoneName == null) {
            Bukkit.getLogger().severe("Unable to get game zone for player: " + player.getName());
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Synchronized block to handle concurrent modifications safely.
            synchronized (lock) {
                clearApple();
                Set<BlockLocation> attemptedLocations = new HashSet<>();
                // Multiple attempts to find a valid location for the apple.
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
                        // Various checks to ensure the apple is placed in a suitable location.
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
                    // Set the apple's block and display a label above it.
                    this.block.setType(Material.PLAYER_HEAD);
                    Skull skull = (Skull) block.getState();
                    UUID ownerUUID = Bukkit.getOfflinePlayer("MHF_Apple").getUniqueId();
                    skull.setOwningPlayer(Bukkit.getOfflinePlayer(ownerUUID));
                    skull.update();
                    Location armorStandLoc = block.getLocation().add(0.5, 1, 0.5);
                    this.armorStandLabel = world.spawn(armorStandLoc, ArmorStand.class, as -> {
                        DyeColor sheepColor = snake.getColor();
                        TextColor textColor = dyeColorToTextColor(sheepColor);
                        as.customName(Component.text(snake.getPlayer().getName() + "'s Apple", textColor));
                        as.setCustomNameVisible(true);
                        as.setVisible(false);
                        as.setMarker(true);
                        as.setGravity(false);
                        as.setSmall(true);
                    });
                    // DEBUG Bukkit.getLogger().info("Apple successfully placed at X: " + newBlockLocation.getX() + ", Y: " + newBlockLocation.getY() + ", Z: " + newBlockLocation.getZ() + " on attempt " + attempts);
                    return;
                }
                // If failed to find a suitable location, end the game and notify the player.
                player.sendMessage(Component.text("Failed to find a new location for the apple after multiple attempts.", NamedTextColor.RED));
                player.sendMessage(Component.text("Please contact the server administrator.", NamedTextColor.RED));
                gameManager.endGame(player);
                Bukkit.getLogger().severe("Failed to find a suitable location for the apple after multiple attempts.");
            }
        }, 1L);
    }

    // Method to clear the apple from the world, reverting its block to air.
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

    // Helper method to convert a DyeColor to a corresponding TextColor.
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