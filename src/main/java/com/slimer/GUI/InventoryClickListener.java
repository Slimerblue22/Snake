package com.slimer.GUI;

import com.slimer.GUI.Menus.Holders.ColorMenuHolder;
import com.slimer.GUI.Menus.Holders.LeaderboardMenuHolder;
import com.slimer.GUI.Menus.Holders.MainMenuHolder;
import com.slimer.GUI.Menus.LeaderboardMenu;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

/**
 * Listener class for handling inventory click events in the game's GUI.
 * It determines the type of inventory clicked and processes the event accordingly.
 * <p>
 * Last updated: V2.1.0
 *
 * @author Slimerblue22
 */
public class InventoryClickListener implements Listener {

    /**
     * Handles inventory click events. It checks the type of inventory and delegates
     * the event to the appropriate handler method.
     *
     * @param event The InventoryClickEvent to handle.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;

        Inventory inventory = event.getInventory();
        if (inventory.getHolder() instanceof MainMenuHolder) {
            event.setCancelled(true);
            handleMainMenuClick(event);
        } else if (inventory.getHolder() instanceof ColorMenuHolder) {
            event.setCancelled(true);
            handleColorMenuClick(event);
        } else if (inventory.getHolder() instanceof LeaderboardMenuHolder) {
            event.setCancelled(true);
            handleLeaderboardMenuClick(event);
        }
    }

    /**
     * Handles click events within the Main Menu inventory.
     *
     * @param event The InventoryClickEvent within the Main Menu.
     */
    private void handleMainMenuClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        switch (Objects.requireNonNull(clickedItem).getType()) {
            case GREEN_WOOL -> player.performCommand("snakegame start");
            case RED_WOOL -> player.performCommand("snakegame stop");
            case BOOK -> player.performCommand("snakegame help");
            case JUKEBOX -> player.performCommand("snakegame music");
            case PAINTING -> {
                GuiManager.getInstance().openColorMenu(player);
                return;
            }
            case DIAMOND -> {
                GuiManager.getInstance().openLeaderboardMenu(player, 1);
                return;
            }
        }
        player.closeInventory();
    }

    /**
     * Handles click events within the Color Menu inventory.
     *
     * @param event The InventoryClickEvent within the Color Menu.
     */
    private void handleColorMenuClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        Material clickedMaterial = Objects.requireNonNull(clickedItem).getType();

        // Check if the clicked item is a wool block
        if (clickedMaterial.name().endsWith("_WOOL")) {
            String colorName = clickedMaterial.name().replace("_WOOL", "").toLowerCase().replace("_", " ");
            player.performCommand("snakegame color " + colorName);
        }
        player.closeInventory();
    }

    /**
     * Handles click events within the Leaderboard Menu inventory.
     *
     * @param event The InventoryClickEvent within the Leaderboard Menu.
     */
    private void handleLeaderboardMenuClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        Component title = event.getView().title();

        if (clickedItem != null && clickedItem.hasItemMeta()) {
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta.displayName() != null) {
                if (Objects.equals(meta.displayName(), Component.text("Next Page"))) {
                    int nextPage = LeaderboardMenu.getCurrentPage(title) + 1;
                    GuiManager.getInstance().openLeaderboardMenu(player, nextPage);
                } else if (Objects.equals(meta.displayName(), Component.text("Previous Page"))) {
                    int prevPage = LeaderboardMenu.getCurrentPage(title) - 1;
                    GuiManager.getInstance().openLeaderboardMenu(player, prevPage);
                }
            }
        }
    }
}