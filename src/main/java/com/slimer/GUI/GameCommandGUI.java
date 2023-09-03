package com.slimer.GUI;

import com.slimer.Util.PlayerData;
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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;

/**
 * A class to handle the graphical user interface (GUI) for the Snake game commands.
 */
public class GameCommandGUI implements Listener {
    private final Inventory menu;
    // Rainbow colors for the border
    private final Material[] colors = {
            Material.RED_STAINED_GLASS_PANE,
            Material.ORANGE_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE,
            Material.LIME_STAINED_GLASS_PANE,
            Material.BLUE_STAINED_GLASS_PANE,
            Material.PURPLE_STAINED_GLASS_PANE,
            Material.MAGENTA_STAINED_GLASS_PANE
    };
    // Wool colors for sheep color selection
    private final Material[] woolColors = {
            Material.WHITE_WOOL, Material.LIGHT_GRAY_WOOL, Material.GRAY_WOOL, Material.BLACK_WOOL,
            Material.BROWN_WOOL, Material.RED_WOOL, Material.ORANGE_WOOL, Material.YELLOW_WOOL,
            Material.LIME_WOOL, Material.GREEN_WOOL, Material.CYAN_WOOL, Material.LIGHT_BLUE_WOOL,
            Material.BLUE_WOOL, Material.PURPLE_WOOL, Material.MAGENTA_WOOL, Material.PINK_WOOL
    };
    private final Plugin plugin;
    private int rotationState = 0;

    /**
     * Constructs a new GameCommandGUI object.
     *
     * @param plugin The Plugin instance for the Snake game.
     */
    public GameCommandGUI(Plugin plugin) {
        this.plugin = plugin;
        menu = createMenu();
        startAnimation(menu, 3);
    }

    /**
     * Creates the main menu for the Snake game.
     *
     * @return An Inventory object representing the main menu.
     */
    private Inventory createMenu() {
        Inventory menu = Bukkit.createInventory(null, 9 * 3, Component.text("Snake Main Menu"));

        menu.setItem(10, createMenuItem(Material.GREEN_WOOL, "Start"));
        menu.setItem(11, createMenuItem(Material.RED_WOOL, "Stop"));
        menu.setItem(12, createMenuItem(Material.BOOK, "Help"));
        menu.setItem(13, createMenuItem(Material.PAINTING, "Set Color"));
        menu.setItem(14, createMenuItem(Material.DIAMOND, "Leaderboard"));
        // Should probably make this just display the score in menu at some point.
        // Can't really pass the player because I'd have to also pass it to the event listener which can't accept it.
        // So, I'll figure that out later.
        menu.setItem(15, createMenuItem(Material.EMERALD, "High Score"));

        return menu;
    }

    /**
     * Returns the main menu Inventory object.
     *
     * @return An Inventory object representing the main menu.
     */
    public Inventory getMenu() {
        return menu;
    }

    /**
     * Creates a color selection menu for the Snake game.
     *
     * @return An Inventory object representing the color selection menu.
     */
    public Inventory createColorMenu() {
        int maxRows = 5; // Including the border rows
        Inventory colorMenu = Bukkit.createInventory(null, maxRows * 9, Component.text("Select Snake Color"));

        int slot = 10; // Start from the second row and second column to accommodate the border

        for (Material color : woolColors) {
            while (slot % 9 == 0 || slot % 9 == 8 || slot / 9 == maxRows - 1) {
                slot++; // Skip the slots reserved for the border
            }

            ItemStack colorItem = new ItemStack(color);
            ItemMeta colorMeta = colorItem.getItemMeta();
            colorMeta.displayName(Component.text(color.name().replace("_WOOL", "").replace("_", " ")));
            colorItem.setItemMeta(colorMeta);
            colorMenu.setItem(slot, colorItem);
            slot++;
        }

        startAnimation(colorMenu, maxRows);
        return colorMenu;
    }

    /**
     * Creates a leaderboard menu for the Snake game.
     *
     * @return An Inventory object representing the leaderboard menu.
     */
    public Inventory createLeaderboardMenu() {
        int maxRows = 4; // Including the border rows
        int maxEntries = 10; // Maximum number of leaderboard entries to display
        Inventory leaderboardMenu = Bukkit.createInventory(null, maxRows * 9, Component.text("Leaderboard"));
        List<Map.Entry<String, Integer>> leaderboard = PlayerData.getInstance((JavaPlugin) plugin).getLeaderboard();

        int slot = 10; // Start from the second row and second column to accommodate the border

        for (int i = 0; i < Math.min(maxEntries, leaderboard.size()); i++) {
            Map.Entry<String, Integer> entry = leaderboard.get(i);

            // Special case to center the last 3 entries on the third row
            if (i == 7) {
                slot = 2 * 9 + 3; // Start at the third row and fourth column
            }
            if (i == 8) {
                slot = 2 * 9 + 4; // Start at the third row and fifth column
            }
            if (i == 9) {
                slot = 2 * 9 + 5; // Start at the third row and sixth column
            }

            while (slot % 9 == 0 || slot % 9 == 8 || slot / 9 == maxRows - 1) {
                slot++; // Skip the slots reserved for the border
            }

            ItemStack playerHead = createPlayerHead(entry.getKey());
            ItemMeta meta = playerHead.getItemMeta();
            meta.displayName(Component.text(entry.getKey() + ": " + entry.getValue()));
            playerHead.setItemMeta(meta);
            leaderboardMenu.setItem(slot, playerHead);

            // Only increment slot if not one of the last 3 special cases
            if (i < 7) {
                slot++;
            }
        }

        startAnimation(leaderboardMenu, maxRows);
        return leaderboardMenu;
    }

    /**
     * Creates a menu item with the specified material and display name.
     *
     * @param material    The material of the item.
     * @param displayName The display name of the item.
     * @return An ItemStack representing the menu item.
     */
    private ItemStack createMenuItem(Material material, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(displayName));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates a player head item with the specified player name.
     *
     * @param playerName The name of the player whose head to create.
     * @return An ItemStack representing the player head.
     */
    public ItemStack createPlayerHead(String playerName) {
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
        playerHead.setItemMeta(meta);
        return playerHead;
    }

    /**
     * Starts an animation for the border of the menu.
     *
     * @param menu    The Inventory object representing the menu.
     * @param maxRows The maximum number of rows in the menu.
     */
    private void startAnimation(Inventory menu, int maxRows) {
        new BukkitRunnable() {
            @Override
            public void run() {
                rotateGlassPanes(menu, maxRows);
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    /**
     * Rotates the glass panes in the border of the menu for animation.
     *
     * @param menu    The Inventory object representing the menu.
     * @param maxRows The maximum number of rows in the menu.
     */
    private void rotateGlassPanes(Inventory menu, int maxRows) {
        for (int row = 0; row < maxRows; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;

                // Skip the inner slots, only modify the border
                if (row > 0 && row < maxRows - 1 && col > 0 && col < 8) {
                    continue;
                }

                Material color = colors[(rotationState + index) % colors.length];
                menu.setItem(index, new ItemStack(color));
            }
        }

        rotationState = (rotationState + 1) % colors.length;
    }

    /**
     * Handles the InventoryClickEvent for the Snake game GUI menus.
     *
     * @param event The InventoryClickEvent object.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedInventory == null || clickedItem == null) return;

        Component title = event.getView().title();
        if (title.equals(Component.text("Snake Main Menu"))) {
            switch (clickedItem.getType()) {
                case GREEN_WOOL -> player.performCommand("snakegame start");
                case RED_WOOL -> player.performCommand("snakegame stop");
                case BOOK -> player.performCommand("snakegame help");
                case EMERALD -> player.performCommand("snakegame highscore");
                case PAINTING -> {
                    player.openInventory(createColorMenu()); // Open the color submenu
                    event.setCancelled(true);
                    return; // Return without closing the main menu
                }
                case DIAMOND -> {
                    player.openInventory(createLeaderboardMenu()); // Open the leaderboard submenu
                    event.setCancelled(true);
                    return; // Return without closing the main menu
                }
            }
            player.closeInventory();  // Close the main menu
            event.setCancelled(true);  // Cancel the event to prevent taking items

        } else if (title.equals(Component.text("Select Snake Color"))) {
            // Cancel the event to prevent taking items, including the glass panes
            event.setCancelled(true);

            Material clickedMaterial = clickedItem.getType();

            // Execute the color change command only if the clicked item is wool
            if (clickedMaterial.toString().endsWith("_WOOL")) {
                String color = clickedMaterial.name().replace("_WOOL", "").toLowerCase();
                player.performCommand("snakegame color " + color);
                player.closeInventory();
            }
        } else if (title.equals(Component.text("Leaderboard"))) {
            event.setCancelled(true);
        }
    }
}