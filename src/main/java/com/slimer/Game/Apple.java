package com.slimer.Game;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.slimer.Region.Region;
import com.slimer.Region.RegionService;
import com.slimer.Util.AStar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Random;
import java.util.UUID;

/**
 * Represents an apple entity in the Snake game.
 * Responsible for the spawning and removal of apple instances in the game world.
 * Note: The actual logic for apple collection is managed by the {@code GameManager} class,
 * which serves as the primary control point for coordinating game-related activities.
 */
public class Apple {
    private ArmorStand armorStand;

    /**
     * Default constructor for Apple.
     */
    public Apple() {
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

        return armorStand;
    }

    /**
     * Finds a suitable location for spawning an apple within the game zone.
     *
     * @param world         The world in which the game is occurring.
     * @param snakeYLevel   The Y-level of the snake.
     * @param region        The region representing the game zone.
     * @param snakeLocation The location of the snake.
     * @return A suitable Location for apple spawn, or null if not found.
     */
    private Location findSuitableLocation(World world, int snakeYLevel, Region region, Location snakeLocation) {
        AStar aStar = new AStar();
        int attempts = 0;
        int maxAttempts = 1000;
        Location location;

        do {
            location = getRandomLocationWithinGameZone(world, snakeYLevel, region.getName());
            if (location == null || !isLocationValid(location, snakeLocation, aStar)) {
                location = null;
            }
            attempts++;
            if (attempts >= maxAttempts) {
                return null;
            }
        } while (location == null);

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
        if (aStar.hasSolidNeighbors(location) || aStar.isSameBlock(location, snakeLocation)) {
            return false;
        }
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
        RegionService regionService = RegionService.getInstance();

        // Iterate through all game regions
        for (Region region : regionService.getAllRegions().values()) {
            if (region.getType() == Region.RegionType.GAME) {
                World world = Bukkit.getWorld(region.getWorldName());
                ProtectedRegion worldGuardRegion = regionService.getWorldGuardRegion(region.getName(), world);

                if (world != null && worldGuardRegion != null && regionService.isLocationInRegion(snakeLocation, worldGuardRegion)) {
                    Location location = findSuitableLocation(world, snakeYLevel, region, snakeLocation);
                    if (location == null) {
                        Bukkit.getLogger().severe("{Snake 2.0.0 Beta-1} [Apple.java] Could not find a valid location for apple spawn.");
                        return;
                    }
                    // Center the ArmorStand on the block
                    location.setX(location.getBlockX() + 0.5);
                    location.setZ(location.getBlockZ() + 0.5);
                    location = location.clone().subtract(0, 1.4, 0);

                    this.armorStand = spawnArmorStand(location);

                    // Set custom name for the ArmorStand
                    Component customName = Component.text(playerName + "'s apple");
                    armorStand.customName(customName);
                    armorStand.setCustomNameVisible(true);

                    return;
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
     * Clears the ArmorStand representing the apple.
     */
    public void clear() {
        if (this.armorStand != null) {
            armorStand.remove();
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
        RegionService regionService = RegionService.getInstance();
        ProtectedRegion worldGuardRegion = regionService.getWorldGuardRegion(gameZoneName, world);

        if (worldGuardRegion != null) {
            int minX = worldGuardRegion.getMinimumPoint().getBlockX();
            int maxX = worldGuardRegion.getMaximumPoint().getBlockX();
            int minZ = worldGuardRegion.getMinimumPoint().getBlockZ();
            int maxZ = worldGuardRegion.getMaximumPoint().getBlockZ();

            Random random = new Random();
            int x = random.nextInt(maxX - minX + 1) + minX;
            int z = random.nextInt(maxZ - minZ + 1) + minZ;

            return new Location(world, x, yLevel, z);
        }

        Bukkit.getLogger().severe("{Snake 2.0.0 Beta-1} [Apple.java] Region not found for game zone name: " + gameZoneName);
        return null;
    }
}
