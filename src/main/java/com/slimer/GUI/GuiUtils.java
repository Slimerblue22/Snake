package com.slimer.GUI;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * Utility class providing static methods to create specific types of items for GUI interfaces in a Minecraft server environment.
 * This class includes methods for creating menu items and player heads.
 * <p>
 * Last updated: V2.1.0
 *
 * @author Slimerblue22
 */
public class GuiUtils {

    /**
     * Creates an ItemStack with the specified material and display name.
     * This method is used to create custom items for GUI menus.
     *
     * @param material    The material of the item to be created.
     * @param displayName The display name to be set on the item.
     * @return An ItemStack with the specified material and display name.
     */
    public static ItemStack createMenuItem(Material material, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(displayName));
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates a player head ItemStack with the skin of the specified player.
     * This method is commonly used to represent players in GUIs.
     *
     * @param playerName The name of the player whose head is to be created.
     * @return An ItemStack representing the player's head.
     */
    public static ItemStack createPlayerHead(String playerName) {
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
            playerHead.setItemMeta(meta);
        }
        return playerHead;
    }
}