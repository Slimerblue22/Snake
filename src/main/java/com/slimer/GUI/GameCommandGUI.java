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
 * The GameCommandGUI class represents a graphical user interface for the Snake game.
 * It provides menus and interfaces for players to interact with various game-related commands and settings.
 * This class implements the Bukkit Listener interface to handle inventory click events.
 * <p>
 * The GameCommandGUI includes the following functionality:
 * - Main menu with options to start, stop, get help, set colors, view the leaderboard, and toggle music.
 * - Color selection menu for choosing the snake's color.
 * - Leaderboard menu for displaying the top players and their scores.
 * <p>
 * This class also manages the animation of glass panes on the menu border.
 * <p>
 * Last updated: V2.1.0
 * @author Slimerblue22
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
     * Constructs a new GameCommandGUI object and sets it as the current instance.
     * Ensures that only one GUI event listener instance is active at a time.
     *
     * @param plugin The Plugin instance for the Snake game.
     * @param player The player for whom the GUI is being created.
     */
    public GameCommandGUI(Plugin plugin, Player player) {
        this.plugin = plugin;
        menu = createMenu(player);
        startAnimation(menu, 3);
        currentInstance = this;
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
     * Creates the main menu for the Snake game, including options like Start, Stop, Help, etc.
     * Additionally, it displays the player's high score and allows them to toggle music.
     *
     * @param player The player for whom the menu is being created.
     * @return An Inventory object representing the main menu.
     */
    private Inventory createMenu(Player player) {
        // Initialize main menu
        Inventory menu = Bukkit.createInventory(null, 9 * 3, Component.text("Snake Main Menu"));

        // Add regular menu items
        menu.setItem(10, createMenuItem(Material.GREEN_WOOL, "Start"));
        menu.setItem(11, createMenuItem(Material.RED_WOOL, "Stop"));
        menu.setItem(12, createMenuItem(Material.BOOK, "Help"));
        menu.setItem(13, createMenuItem(Material.PAINTING, "Set Color"));
        menu.setItem(14, createMenuItem(Material.DIAMOND, "Leaderboard"));
        menu.setItem(16, createMenuItem(Material.JUKEBOX, "Toggle Music"));

        // Add player-specific item with high score
        ItemStack playerHead = createPlayerHead(player.getName());
        SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
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
     * Creates a color selection menu for the Snake game, allowing players to choose their snake's color.
     *
     * @return An Inventory object representing the color selection menu.
     */
    public Inventory createColorMenu() {
        // Constants
        final int maxRows = 5;
        final int slotsPerRow = 9;

        // Initialize color menu
        Inventory colorMenu = Bukkit.createInventory(null, maxRows * slotsPerRow, Component.text("Select Snake Color"));

        // Initialize slot index; start at second row and second column
        int slot = 10;

        // Populate the menu with wool color options
        for (Material color : woolColors) {
            // Skip slots reserved for the border
            while (slot % slotsPerRow == 0 || slot % slotsPerRow == 8 || slot / slotsPerRow == maxRows - 1) {
                slot++;
            }

            // Create and set color item
            ItemStack colorItem = new ItemStack(color);
            ItemMeta colorMeta = colorItem.getItemMeta();
            colorMeta.displayName(Component.text(color.name().replace("_WOOL", "").replace("_", " ")));
            colorItem.setItemMeta(colorMeta);
            colorMenu.setItem(slot, colorItem);

            slot++;
        }

        // Start border animation
        startAnimation(colorMenu, maxRows);
        return colorMenu;
    }

    /**
     * Creates a leaderboard menu for the Snake game.
     *
     * @param page The page number for the leaderboard.
     * @return An Inventory object representing the leaderboard menu.
     */
    public Inventory createLeaderboardMenu(int page) {
        // Constants
        final int maxRows = 4;
        final int maxEntries = 10;
        final int slotsPerRow = 9;

        // Initialize leaderboard menu and get leaderboard entries
        Inventory leaderboardMenu = Bukkit.createInventory(null, maxRows * slotsPerRow,
                Component.text("Leaderboard (Page " + page + ")"));
        List<Map.Entry<String, Integer>> allEntries = PlayerData.getInstance((JavaPlugin) plugin).getLeaderboard();

        // Calculate page-specific details
        int startEntry = (page - 1) * maxEntries;
        int endEntry = Math.min(startEntry + maxEntries, allEntries.size());
        List<Map.Entry<String, Integer>> pageEntries = allEntries.subList(startEntry, endEntry);

        // Initialize slot index; start at second row and second column
        int slot = 10;

        // Populate leaderboard entries into the menu
        for (int i = 0; i < pageEntries.size(); i++) {
            Map.Entry<String, Integer> entry = pageEntries.get(i);

            // Special cases for last 3 entries
            if (i >= 7) {
                slot = 2 * slotsPerRow + 3 + (i - 7);
            }

            // Skip border slots
            while (slot % slotsPerRow == 0 || slot % slotsPerRow == 8 || slot / slotsPerRow == maxRows - 1) {
                slot++;
            }

            // Create and set player head
            ItemStack playerHead = createPlayerHead(entry.getKey());
            ItemMeta meta = playerHead.getItemMeta();
            meta.displayName(Component.text(entry.getKey() + ": " + entry.getValue()));
            playerHead.setItemMeta(meta);
            leaderboardMenu.setItem(slot, playerHead);

            // Increment slot index
            if (i < 7) {
                slot++;
            }
        }

        // Add navigation buttons
        int prevPageSlot = 1 + slotsPerRow * 2;
        int nextPageSlot = 7 + slotsPerRow * 2;
        if (page > 1) {
            leaderboardMenu.setItem(prevPageSlot, createMenuItem(Material.ARROW, "Previous Page"));
        }
        if (allEntries.size() > page * maxEntries) {
            leaderboardMenu.setItem(nextPageSlot, createMenuItem(Material.ARROW, "Next Page"));
        }

        // Start the border animation
        startAnimation(leaderboardMenu, maxRows);

        return leaderboardMenu;
    }

    /**
     * Extracts the current page number from the given inventory title.
     * The inventory title should contain a section like "(Page x)", where x is the page number.
     *
     * @param title The Component representing the inventory title.
     * @return The extracted page number, or -1 if the page number cannot be extracted.
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

                // Determine the next color based on the rotation state and index
                Material color = colors[(rotationState + index) % colors.length];

                // Set the new color for the slot
                menu.setItem(index, new ItemStack(color));
            }
        }

        // Update rotation state for the next cycle
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

        // Basic null checks
        if (clickedInventory == null || clickedItem == null) return;

        Component title = event.getView().title();
        event.setCancelled(true);  // Cancel the event by default

        // Route to the specific handler based on the title
        if (title.equals(Component.text("Snake Main Menu"))) {
            handleMainMenuClick(player, clickedItem);
        } else if (title.equals(Component.text("Select Snake Color"))) {
            handleColorMenuClick(player, clickedItem);
        } else if (title.toString().contains("Leaderboard")) {
            handleLeaderboardMenuClick(player, clickedItem, title);
        } else {
            // If none of the conditions are met, un-cancel the event
            event.setCancelled(false);
        }
    }

    /**
     * Handles click events in the Snake Main Menu.
     *
     * @param player      The player who clicked.
     * @param clickedItem The ItemStack that was clicked.
     */
    private void handleMainMenuClick(Player player, ItemStack clickedItem) {
        switch (clickedItem.getType()) {
            case GREEN_WOOL -> player.performCommand("snakegame start");
            case RED_WOOL -> player.performCommand("snakegame stop");
            case BOOK -> player.performCommand("snakegame help");
            case JUKEBOX -> player.performCommand("snakegame music");
            case PAINTING -> {
                player.openInventory(createColorMenu());
                return;
            }
            case DIAMOND -> {
                player.openInventory(createLeaderboardMenu(1));
                return;
            }
        }
        player.closeInventory();
    }

    /**
     * Handles click events in the Snake Color Menu.
     *
     * @param player       The player who clicked.
     * @param clickedItem  The ItemStack that was clicked.
     */
    private void handleColorMenuClick(Player player, ItemStack clickedItem) {
        Material clickedMaterial = clickedItem.getType();
        if (clickedMaterial.toString().endsWith("_WOOL")) {
            String color = clickedMaterial.name().replace("_WOOL", "").toLowerCase();
            player.performCommand("snakegame color " + color);
            player.closeInventory();
        }
    }

    /**
     * Handles click events in the Snake Leaderboard Menu.
     *
     * @param player      The player who clicked.
     * @param clickedItem The ItemStack that was clicked.
     * @param title       The title of the leaderboard menu.
     */
    private void handleLeaderboardMenuClick(Player player, ItemStack clickedItem, Component title) {
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta != null && meta.displayName() != null) {
            if (Objects.equals(meta.displayName(), Component.text("Next Page"))) {
                int nextPage = getCurrentPage(title) + 1;
                player.openInventory(createLeaderboardMenu(nextPage));
            } else if (Objects.equals(meta.displayName(), Component.text("Previous Page"))) {
                int prevPage = getCurrentPage(title) - 1;
                player.openInventory(createLeaderboardMenu(prevPage));
            }
        }
    }
}