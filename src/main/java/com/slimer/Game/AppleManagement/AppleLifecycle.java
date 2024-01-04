package com.slimer.Game.AppleManagement;

import com.slimer.Util.DebugManager;
import com.slimer.Util.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AppleLifecycle {
    private final HashMap<UUID, ArmorStand> playerApples;
    private final AppleLocationFinder appleLocationFinder;
    private static final String NO_VALID_LOCATION_LOG = "No valid location found for apple creation for player: %s";
    private static final String APPLE_SUCCESSFULLY_CREATED_LOG = "Apple successfully created for player: %s";
    private static final String APPLE_SUCCESSFULLY_REMOVED_LOG = "Apple successfully removed for player: %s";

    public AppleLifecycle(AppleLocationFinder appleLocationFinder) {
        this.appleLocationFinder = appleLocationFinder;
        playerApples = new HashMap<>();
    }

    public Map<UUID, ArmorStand> getPlayerApples() {
        return Collections.unmodifiableMap(playerApples);
    }

    public void createAppleForPlayer(Player player, String world, String gameRegion) {
        // Extract the player's Y-coordinate
        int playerY = player.getLocation().getBlockY();

        // Find a random location to place the apple on the same Y-level as the player
        Location appleLocation = appleLocationFinder.findRandomLocationInRegion(world, gameRegion, playerY);
        if (appleLocation == null) {
            DebugManager.log(DebugManager.Category.APPLE_LIFECYCLE, String.format(NO_VALID_LOCATION_LOG, player.getName()));
            return; // No valid location found
        }

        // Adjust the apple location by -1.4 Y, and +0.5 X and Z
        appleLocation.subtract(0, 1.4, 0);
        appleLocation.add(0.5, 0, 0.5);

        // Set armor stand properties
        ArmorStand apple = (ArmorStand) appleLocation.getWorld().spawnEntity(appleLocation, EntityType.ARMOR_STAND);
        apple.setInvisible(true);
        apple.setGravity(false);
        apple.setInvulnerable(true);
        apple.setBasePlate(false);

        // Rename the armor stand to "{player}'s apple" and set the name color to the snake color
        String playerName = player.getName();
        DyeColor sheepColor = PlayerData.getInstance().getSheepColor(player);
        NamedTextColor color = convertDyeColorToTextColor(sheepColor);
        Component customName = Component.text(playerName + "'s apple").color(color);
        apple.customName(customName);
        apple.setCustomNameVisible(true);

        // Set ArmorStand's helmet to an apple head
        ItemStack appleHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) appleHead.getItemMeta();
        UUID ownerUUID = Bukkit.getOfflinePlayer("MHF_Apple").getUniqueId();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(ownerUUID));
        appleHead.setItemMeta(meta);
        apple.getEquipment().setHelmet(appleHead);

        // Store the armor stand in memory
        playerApples.put(player.getUniqueId(), apple);
        DebugManager.log(DebugManager.Category.APPLE_LIFECYCLE, String.format(APPLE_SUCCESSFULLY_CREATED_LOG, player.getName()));
    }

    public void removeAppleForPlayer(Player player) {
        if (playerApples.containsKey(player.getUniqueId())) {
            // Remove the armor stand
            ArmorStand apple = playerApples.get(player.getUniqueId());
            apple.remove();

            // Remove the apple from memory
            playerApples.remove(player.getUniqueId());
            DebugManager.log(DebugManager.Category.APPLE_LIFECYCLE, String.format(APPLE_SUCCESSFULLY_REMOVED_LOG, player.getName()));
        }
    }

    private NamedTextColor convertDyeColorToTextColor(DyeColor dyeColor) {
        return switch (dyeColor) {
            // Some dye colors do not have an equivalent in chat color
            // Attempting to use similar colors
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
            case BROWN -> NamedTextColor.DARK_RED;
            case GREEN -> NamedTextColor.DARK_GREEN;
            case RED -> NamedTextColor.RED;
            case BLACK -> NamedTextColor.BLACK;
            default -> NamedTextColor.WHITE; // Default to white if something goes wrong
        };
    }
}