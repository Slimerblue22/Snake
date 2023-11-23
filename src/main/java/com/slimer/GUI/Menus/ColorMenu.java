package com.slimer.GUI.Menus;

import com.slimer.GUI.Menus.Holders.ColorMenuHolder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

import static com.slimer.GUI.GuiUtils.createMenuItem;

/**
 * Singleton class representing the color selection menu in the GUI.
 * This class provides a method to create and retrieve the inventory for selecting colors.
 * <p>
 * Last updated: V2.1.0
 *
 * @author Slimerblue22
 */
public class ColorMenu {
    private static ColorMenu instance;
    private final Material[] woolColors = {
            // Array initialization with various wool colors
            Material.WHITE_WOOL, Material.LIGHT_GRAY_WOOL, Material.GRAY_WOOL, Material.BLACK_WOOL,
            Material.BROWN_WOOL, Material.RED_WOOL, Material.ORANGE_WOOL, Material.YELLOW_WOOL,
            Material.LIME_WOOL, Material.GREEN_WOOL, Material.CYAN_WOOL, Material.LIGHT_BLUE_WOOL,
            Material.BLUE_WOOL, Material.PURPLE_WOOL, Material.MAGENTA_WOOL, Material.PINK_WOOL
    };
    private static final int MAX_ROWS = 5;
    private static final int SLOTS_PER_ROW = 9;
    private static final int STARTING_SLOT = 10;

    /**
     * Private constructor to prevent direct instantiation.
     */
    private ColorMenu() {
    }

    /**
     * Gets the singleton instance of the ColorMenu.
     * If the instance doesn't exist, it creates a new one.
     *
     * @return The singleton instance of ColorMenu.
     */
    public static ColorMenu getInstance() {
        if (instance == null) {
            instance = new ColorMenu();
        }
        return instance;
    }

    /**
     * Creates and returns the inventory for the color menu.
     * This inventory allows users to select different colors.
     *
     * @return The inventory for the color menu.
     */
    public Inventory getInventory() {
        Inventory colorMenu = Bukkit.createInventory(new ColorMenuHolder(null), MAX_ROWS * SLOTS_PER_ROW,
                Component.text("Select Snake Color"));

        int slot = STARTING_SLOT;

        for (Material color : woolColors) {
            while (slot % SLOTS_PER_ROW == 0 || slot % SLOTS_PER_ROW == 8 || slot / SLOTS_PER_ROW == MAX_ROWS - 1) {
                slot++;
            }
            colorMenu.setItem(slot, createMenuItem(color, color.name().replace("_WOOL", "").replace("_", " ")));
            slot++;
        }

        return colorMenu;
    }
}