package com.slimer.Game;

import com.slimer.Region.WGHelpers;
import com.slimer.Util.AStar;
import com.slimer.Util.DebugManager;
import com.slimer.Util.PlayerData;
import com.slimer.Region.RegionHelpers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an apple entity in the Snake game.
 * Responsible for the spawning and removal of apple instances in the game world.
 * Note: The actual logic for apple collection is managed by the {@code GameManager} class,
 * which serves as the primary control point for coordinating game-related activities.
 */
public class Apple {
    private final JavaPlugin plugin;
    private final GameManager gameManager;
    private ArmorStand armorStand;

    /**
     * Constructs an Apple object.
     *
     * @param plugin      The JavaPlugin instance associated with the game.
     * @param gameManager The GameManager managing the game.
     */
    public Apple(JavaPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    /**
     * Spawns an invisible ArmorStand at a given location to represent an apple.
     *
     * @param location Location where the ArmorStand should be spawned.
     * @return The spawned ArmorStand.
     */
    private ArmorStand spawnArmorStand(Location location) {
        ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);
        armorStand.setInvisible(true);
        armorStand.setGravity(false);
        armorStand.setInvulnerable(true);
        armorStand.setBasePlate(false);

        // Set ArmorStand's helmet to an apple head
        ItemStack appleHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) appleHead.getItemMeta();
        UUID ownerUUID = Bukkit.getOfflinePlayer("MHF_Apple").getUniqueId();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(ownerUUID));
        appleHead.setItemMeta(meta);
        armorStand.getEquipment().setHelmet(appleHead);
        DebugManager.log(DebugManager.Category.APPLE, "Apple ArmorStand spawned at " + location);
        return armorStand;
    }

    /**
     * Finds a suitable location for spawning an apple within the game zone.
     *
     * @param world         The world in which the game is occurring.
     * @param snakeYLevel   The Y-level of the snake.
     * @param snakeLocation The location of the snake.
     * @return A suitable Location for apple spawn, or null if not found.
     */
    private Location findSuitableLocation(World world, int snakeYLevel, String regionName, Location snakeLocation) {
        AStar aStar = new AStar();
        int attempts = 0;
        int maxAttempts = 1000;
        Location location;

        do {
            location = getRandomLocationWithinGameZone(world, snakeYLevel, regionName);
            DebugManager.log(DebugManager.Category.APPLE, "Checking suitability of random location: " + location);
            if (location == null || !isLocationValid(location, snakeLocation, aStar)) {
                location = null;
            }
            attempts++;
            if (attempts >= maxAttempts) {
                DebugManager.log(DebugManager.Category.APPLE, "Exceeded maximum apple spawn attempts (" + maxAttempts + "). No suitable location found.");
                return null;
            }
        } while (location == null);
        DebugManager.log(DebugManager.Category.APPLE, "Suitable apple spawn location found at " + location);
        return location;
    }

    /**
     * Checks if a given location is valid for apple spawn.
     *
     * @param location      The location to check.
     * @param snakeLocation The current location of the snake.
     * @param aStar         An instance of AStar for pathfinding.
     * @return true if location is valid, false otherwise.
     */
    private boolean isLocationValid(Location location, Location snakeLocation, AStar aStar) {
        DebugManager.log(DebugManager.Category.APPLE, "Validating apple spawn location: " + location);
        if (aStar.hasSolidNeighbors(location) || aStar.isSameBlock(location, snakeLocation)) {
            return false;
        }
        boolean pathExists = aStar.pathExists(snakeLocation, location);
        DebugManager.log(DebugManager.Category.APPLE, "Path from snake to apple exists: " + pathExists);
        return aStar.pathExists(snakeLocation, location);
    }

    /**
     * Spawns an apple with a custom name.
     *
     * @param snakeLocation The current location of the snake.
     * @param snakeYLevel   The Y-level of the snake.
     * @param playerName    The name of the player.
     */
    public void spawnWithName(Location snakeLocation, int snakeYLevel, String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        RegionHelpers regionHelpers = RegionHelpers.getInstance();
        WGHelpers wgHelpers = WGHelpers.getInstance();

        for (String regionName : regionHelpers.getAllRegisteredRegionNames()) {
            String regionType = regionHelpers.getRegionType(regionName);

            if ("game".equals(regionType)) {
                World world = regionHelpers.getRegionWorld(regionName);

                if (world != null && wgHelpers.areCoordinatesInWGRegion(world.getName(), regionName, snakeLocation.getBlockX(), snakeLocation.getBlockY(), snakeLocation.getBlockZ())) {
                    CompletableFuture<Location> future = CompletableFuture.supplyAsync(() -> findSuitableLocation(world, snakeYLevel, regionName, snakeLocation));
                    future.thenAccept(loc -> Bukkit.getScheduler().runTask(plugin, () -> {
                        if (loc == null) {
                            return;
                        }
                        Player playerObj = Bukkit.getPlayer(playerName);
                        if (playerObj != null && gameManager.getSnakeForPlayer(playerObj) == null) {
                            return;
                        }
                        loc.setX(loc.getBlockX() + 0.5);
                        loc.setZ(loc.getBlockZ() + 0.5);
                        Location adjustedLocation = loc.clone().subtract(0, 1.4, 0);
                        this.armorStand = spawnArmorStand(adjustedLocation);
                        DyeColor sheepColor = PlayerData.getInstance().getSheepColor(Objects.requireNonNull(player));
                        NamedTextColor color = convertDyeColorToTextColor(sheepColor);
                        Component customName = Component.text(playerName + "'s apple").color(color);
                        armorStand.customName(customName);
                        armorStand.setCustomNameVisible(true);
                        DebugManager.log(DebugManager.Category.APPLE, "Apple named after player: " + playerName);
                    }));
                }
            }
        }
    }

    /**
     * Gets the location of the ArmorStand representing the apple.
     *
     * @return The Location of the ArmorStand, or null if not spawned.
     */
    public Location getLocation() {
        return (armorStand != null) ? armorStand.getLocation() : null;
    }

    /**
     * Converts a Bukkit DyeColor to a NamedTextColor.
     *
     * @param dyeColor The Bukkit DyeColor.
     * @return The corresponding NamedTextColor.
     */
    private NamedTextColor convertDyeColorToTextColor(DyeColor dyeColor) {
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

    /**
     * Clears the ArmorStand representing the apple.
     */
    public void clear() {
        if (this.armorStand != null) {
            armorStand.remove();
            DebugManager.log(DebugManager.Category.APPLE, "Apple ArmorStand cleared at " + (this.armorStand != null ? this.armorStand.getLocation() : "unknown location"));
            this.armorStand = null;
        }
    }

    /**
     * Gets a random location within a specified game zone.
     *
     * @param world        The world in which the game zone is located.
     * @param yLevel       The Y-level for the location.
     * @param gameZoneName The name of the game zone.
     * @return A random Location within the game zone, or null if region not found.
     */
    private Location getRandomLocationWithinGameZone(World world, int yLevel, String gameZoneName) {
        WGHelpers wgHelpers = WGHelpers.getInstance();
        String boundaries = wgHelpers.getBoundariesOfRegion(world.getName(), gameZoneName);

        if (boundaries != null && !boundaries.isEmpty()) {
            String[] parts = boundaries.split(" - ");
            String[] minCoords = parts[0].substring(parts[0].indexOf('(') + 1, parts[0].indexOf(')')).split(", ");
            String[] maxCoords = parts[1].substring(parts[1].indexOf('(') + 1, parts[1].indexOf(')')).split(", ");

            int minX = Integer.parseInt(minCoords[0]);
            int maxX = Integer.parseInt(maxCoords[0]);
            int minZ = Integer.parseInt(minCoords[2]);
            int maxZ = Integer.parseInt(maxCoords[2]);

            Random random = new Random();
            int x = random.nextInt(maxX - minX + 1) + minX;
            int z = random.nextInt(maxZ - minZ + 1) + minZ;

            Location location = new Location(world, x, yLevel, z);
            DebugManager.log(DebugManager.Category.APPLE, "Generated random apple spawn location at " + location);
            return location;
        }

        DebugManager.log(DebugManager.Category.APPLE, "Failed to find a WorldGuard region for game zone: " + gameZoneName);
        return null;
    }
}
