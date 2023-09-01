package com.slimer.GUI;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Class responsible for managing the graphical user interface (GUI) for the Snake game.
 * This class handles the creation of menus and responds to player interactions within the GUI.
 */
public class GameCommandGUI implements Listener {

    /**
     * Creates and returns the main menu inventory for the Snake game.
     * The main menu contains options to start and stop the game.
     *
     * @return The created Inventory object representing the main menu.
     */
    public static Inventory createMainMenu() {
        Inventory menu = Bukkit.createInventory(null, 9, Component.text("Snake Main Menu"));

        menu.setItem(0, createMenuItem(Material.GREEN_WOOL, "Start"));
        menu.setItem(1, createMenuItem(Material.RED_WOOL, "Stop"));

        return menu;
    }

    /**
     * Creates a menu item with the given material and display name.
     *
     * @param material     The material type for the item.
     * @param displayName  The display name for the item.
     * @return             An ItemStack representing the menu item.
     */
    private static ItemStack createMenuItem(Material material, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(displayName));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Event handler for inventory clicks.
     * This method handles player interactions within the Snake game's main menu.
     *
     * @param event  The InventoryClickEvent object containing event details.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedInventory == null || clickedItem == null)
            return;

        Component title = event.getView().title();
        if (title.equals(Component.text("Snake Main Menu"))) {
            switch (clickedItem.getType()) {
                case GREEN_WOOL -> player.performCommand("snakegame start");
                case RED_WOOL -> player.performCommand("snakegame stop");
            }
            player.closeInventory();  // Close the main menu
            event.setCancelled(true);  // Cancel the event to prevent taking items
        }
    }
}

