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
import java.util.Objects;

/**
 * A class to handle the graphical user interface (GUI) for the Snake game commands.
 */
public class GameCommandGUI implements Listener {
    private static GameCommandGUI currentInstance;  // Single instance of GameCommandGUI to prevent multiple event listener registrations
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
     * Constructs a new GameCommandGUI object, setting this as the current instance.
     * Ensures that only one GUI event listener instance is active at a time.
     *
     * @param plugin The Plugin instance for the Snake game.
     * @param player The player for whom the GUI is being created.
     */
    public GameCommandGUI(Plugin plugin, Player player) {
        this.plugin = plugin;
        menu = createMenu(player);
        startAnimation(menu, 3);
        currentInstance = this;  // Set this object as the current active instance
    }

    /**
     * Gets the current active instance of the GameCommandGUI.
     *
     * @return The current instance of GameCommandGUI.
     */
    public static GameCommandGUI getCurrentInstance() {
        return currentInstance;
    }

    /**
     * Creates the main menu for the Snake game.
     *
     * @return An Inventory object representing the main menu.
     */
    private Inventory createMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 9 * 3, Component.text("Snake Main Menu"));

        menu.setItem(10, createMenuItem(Material.GREEN_WOOL, "Start"));
        menu.setItem(11, createMenuItem(Material.RED_WOOL, "Stop"));
        menu.setItem(12, createMenuItem(Material.BOOK, "Help"));
        menu.setItem(13, createMenuItem(Material.PAINTING, "Set Color"));
        menu.setItem(14, createMenuItem(Material.DIAMOND, "Leaderboard"));
        menu.setItem(16, createMenuItem(Material.JUKEBOX, "Toggle Music"));

        // Create player head with high score for the specific player
        ItemStack playerHead = createPlayerHead(player.getName());
        SkullMeta meta = (SkullMeta) playerHead.getItemMeta();

        // Fetch the high score for the player using the PlayerData class
        int highScore = PlayerData.getInstance().getHighScore(player);
        meta.displayName(Component.text("High Score: " + highScore));
        playerHead.setItemMeta(meta);

        menu.setItem(15, playerHead);

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
     * Creates a game mode selection submenu for the Snake game.
     *
     * @return An Inventory object representing the game mode selection submenu.
     */
    private Inventory createGameModeSubMenu() {
        int maxRows = 3;
        Inventory gameModeMenu = Bukkit.createInventory(null, maxRows * 9, Component.text("Select Game Mode"));

        // Adding game mode options in the middle of the inventory
        gameModeMenu.setItem(9 + 4, createMenuItem(Material.GRASS_BLOCK, "Classic Mode"));
        gameModeMenu.setItem(9 + 5, createMenuItem(Material.DIAMOND_SWORD, "PvP Mode"));

        startAnimation(gameModeMenu, maxRows);

        return gameModeMenu;
    }

    /**
     * Creates a leaderboard menu for the Snake game.
     *
     * @return An Inventory object representing the leaderboard menu.
     */
    public Inventory createLeaderboardMenu(int page) {
        int maxRows = 4;
        int maxEntries = 10;
        int startEntry = (page - 1) * maxEntries; // Calculate the starting entry for the current page
        Inventory leaderboardMenu = Bukkit.createInventory(null, maxRows * 9, Component.text("Leaderboard (Page " + page + ")"));

        List<Map.Entry<String, Integer>> allLeaderboardEntries = PlayerData.getInstance((JavaPlugin) plugin).getLeaderboard();
        List<Map.Entry<String, Integer>> pageLeaderboardEntries = allLeaderboardEntries.subList(startEntry, Math.min(startEntry + maxEntries, allLeaderboardEntries.size()));

        int slot = 10; // Start from the second row and second column to accommodate the border

        for (int i = 0; i < Math.min(maxEntries, pageLeaderboardEntries.size()); i++) {
            Map.Entry<String, Integer> entry = pageLeaderboardEntries.get(i);

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

        // Calculate slot positions for the 3rd row
        int previousPageSlot = 1 + 9 * 2; // Slot 2 of the 3rd row
        int nextPageSlot = 7 + 9 * 2; // Slot 8 of the 3rd row

        // Add next and previous page buttons
        if (page > 1) {
            // Add "Previous Page" button
            leaderboardMenu.setItem(previousPageSlot, createMenuItem(Material.ARROW, "Previous Page"));
        }
        if (allLeaderboardEntries.size() > page * maxEntries) {
            // Add "Next Page" button
            leaderboardMenu.setItem(nextPageSlot, createMenuItem(Material.ARROW, "Next Page"));
        }

        startAnimation(leaderboardMenu, maxRows);
        return leaderboardMenu;
    }

    /**
     * Extracts the current page number from the given inventory title.
     * The inventory title should contain a section like "(Page x)", where x is the page number.
     *
     * @param title The Component representing the inventory title.
     * @return The extracted page number. Returns -1 if the page number cannot be extracted.
     */
    private int getCurrentPage(Component title) {
        String titleString = title.toString();
        int startIndex = titleString.indexOf("(Page ");
        int endIndex = titleString.indexOf(")");

        // Check if both "(Page " and ")" are present in the title
        if (startIndex == -1 || endIndex == -1) {
            return -1; // Return an invalid page number to indicate an error
        }

        String pageString = titleString.substring(startIndex + 6, endIndex);
        return Integer.parseInt(pageString);
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
                case GREEN_WOOL -> {
                    player.openInventory(createGameModeSubMenu()); // Open the game mode submenu
                    event.setCancelled(true);
                    return; // Return without closing the main menu
                }
                case RED_WOOL -> player.performCommand("snakegame stop");
                case BOOK -> player.performCommand("snakegame help");
                case JUKEBOX -> player.performCommand("snakegame music");
                case PAINTING -> {
                    player.openInventory(createColorMenu()); // Open the color submenu
                    event.setCancelled(true);
                    return; // Return without closing the main menu
                }
                case DIAMOND -> {
                    player.openInventory(createLeaderboardMenu(1)); // Open the leaderboard submenu
                    event.setCancelled(true);
                    return; // Return without closing the main menu
                }
            }
            player.closeInventory();  // Close the main menu
            event.setCancelled(true);  // Cancel the event to prevent taking items
        } else if (title.equals(Component.text("Select Game Mode"))) {
            event.setCancelled(true); // Prevent taking items from the GUI
            if (clickedItem.getType() == Material.GRASS_BLOCK) {
                player.performCommand("snakegame start classic");
                player.closeInventory();
            } else if (clickedItem.getType() == Material.DIAMOND_SWORD) {
                player.performCommand("snakegame start pvp");
                player.closeInventory();
            }
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
        } else if (title.toString().contains("Leaderboard")) {
            event.setCancelled(true); // Prevent taking items from the GUI
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.displayName() != null) {
                if (Objects.equals(meta.displayName(), Component.text("Next Page"))) {
                    // Increment the page number and open the next page
                    int nextPage = getCurrentPage(title) + 1;
                    player.openInventory(createLeaderboardMenu(nextPage));
                } else if (Objects.equals(meta.displayName(), Component.text("Previous Page"))) {
                    // Decrement the page number and open the previous page
                    int prevPage = getCurrentPage(title) - 1;
                    player.openInventory(createLeaderboardMenu(prevPage));
                }
            }
        }
    }
}