package com.slimer.GUI.Menus;

import com.slimer.GUI.Menus.Holders.MainMenuHolder;
import com.slimer.Util.PlayerData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import static com.slimer.GUI.GuiUtils.createMenuItem;
import static com.slimer.GUI.GuiUtils.createPlayerHead;

/**
 * Class representing the main menu in the GUI.
 * This class provides a method to create and retrieve the main menu inventory.
 * <p>
 * Last updated: V2.1.0
 *
 * @author Slimerblue22
 */
public class MainMenu {
    private static final int INVENTORY_SIZE = 9 * 3; // Size of the inventory

    /**
     * Creates and returns the inventory for the main menu specific to a player.
     * The menu includes various options like starting/stopping the game, accessing help, etc.
     *
     * @param player The player for whom the inventory is to be created.
     * @return The inventory for the main menu.
     */
    public Inventory getInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(new MainMenuHolder(null), INVENTORY_SIZE, Component.text("Snake Main Menu"));

        // Add regular menu items
        inventory.setItem(10, createMenuItem(Material.GREEN_WOOL, "Start"));
        inventory.setItem(11, createMenuItem(Material.RED_WOOL, "Stop"));
        inventory.setItem(12, createMenuItem(Material.BOOK, "Help"));
        inventory.setItem(13, createMenuItem(Material.PAINTING, "Set Color"));
        inventory.setItem(14, createMenuItem(Material.DIAMOND, "Leaderboard"));
        inventory.setItem(16, createMenuItem(Material.JUKEBOX, "Toggle Music"));

        // Add player-specific item with high score
        ItemStack playerHead = createPlayerHead(player.getName());
        SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
        int highScore = PlayerData.getInstance().getHighScore(player);
        meta.displayName(Component.text("High Score: " + highScore));
        playerHead.setItemMeta(meta);
        inventory.setItem(15, playerHead);

        return inventory;
    }
}