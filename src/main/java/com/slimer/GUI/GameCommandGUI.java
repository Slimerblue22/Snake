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
        menu.setItem(2, createMenuItem(Material.BOOK, "Help"));
        menu.setItem(3, createMenuItem(Material.PAINTING, "Set Color"));

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
     * Creates an inventory menu for selecting the snake color.
     * The menu will contain different wool items each representing a color option for the snake.
     *
     * @return The Inventory object representing the color selection menu.
     */
    public static Inventory createColorMenu() {
        Inventory colorMenu = Bukkit.createInventory(null, 18, Component.text("Select Snake Color"));
        Material[] colors = {Material.WHITE_WOOL, Material.LIGHT_GRAY_WOOL, Material.GRAY_WOOL, Material.BLACK_WOOL, Material.BROWN_WOOL, Material.RED_WOOL, Material.ORANGE_WOOL, Material.YELLOW_WOOL, Material.LIME_WOOL, Material.GREEN_WOOL, Material.CYAN_WOOL, Material.LIGHT_BLUE_WOOL, Material.BLUE_WOOL, Material.PURPLE_WOOL, Material.MAGENTA_WOOL, Material.PINK_WOOL};

        for (int i = 0; i < colors.length; i++) {
            ItemStack colorItem = new ItemStack(colors[i]);
            ItemMeta colorMeta = colorItem.getItemMeta();
            colorMeta.displayName(Component.text(colors[i].name().replace("_WOOL", "").replace("_", " ")));
            colorItem.setItemMeta(colorMeta);
            colorMenu.setItem(i, colorItem);
        }

        return colorMenu;
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
                case BOOK -> player.performCommand("snakegame help");
                case PAINTING -> {
                    player.openInventory(createColorMenu()); // Open the color submenu
                    event.setCancelled(true);
                    return; // Return without closing the main menu
                }
            }
            player.closeInventory();  // Close the main menu
            event.setCancelled(true);  // Cancel the event to prevent taking items

        } else if (title.equals(Component.text("Select Snake Color"))) {
            Material clickedMaterial = clickedItem.getType();
            if (clickedMaterial.toString().endsWith("_WOOL")) {
                String color = clickedMaterial.name().replace("_WOOL", "").toLowerCase();
                player.performCommand("snakegame color " + color);
                player.closeInventory();
                event.setCancelled(true);
            }
        }
    }
}

