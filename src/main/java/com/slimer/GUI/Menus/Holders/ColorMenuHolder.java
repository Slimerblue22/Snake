package com.slimer.GUI.Menus.Holders;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a holder for a color menu inventory in a GUI.
 * This class is responsible for holding and providing access to the color menu inventory.
 * <p>
 * Last updated: V2.1.0
 *
 * @author Slimerblue22
 */
public class ColorMenuHolder implements InventoryHolder {
    private final Inventory inventory;

    /**
     * Constructs a new ColorMenuHolder with the specified inventory.
     *
     * @param inventory The inventory associated with this holder.
     */
    public ColorMenuHolder(Inventory inventory) {
        this.inventory = inventory;
    }

    /**
     * Retrieves the inventory associated with this holder.
     *
     * @return The inventory held by this holder.
     */
    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}