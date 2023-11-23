package com.slimer.GUI.Menus;

import com.slimer.GUI.Menus.Holders.LeaderboardMenuHolder;
import com.slimer.Util.PlayerData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

import static com.slimer.GUI.GuiUtils.createMenuItem;
import static com.slimer.GUI.GuiUtils.createPlayerHead;

/**
 * This class is responsible for creating and managing the leaderboard menu in the GUI.
 * <p>
 * Last updated: V2.1.0
 *
 * @author Slimerblue22
 */
public class LeaderboardMenu {

    private static final int INVENTORY_ROWS = 4;
    private static final int INVENTORY_SIZE = INVENTORY_ROWS * 9; // Number of slots in the inventory
    private static final int ITEMS_PER_PAGE = 10; // Number of leaderboard items per page
    private static final int START_SLOT = 10; // Starting slot for leaderboard entries in the inventory
    private static final int PREV_PAGE_SLOT = 19; // Slot for the "Previous Page" button
    private static final int NEXT_PAGE_SLOT = 25; // Slot for the "Next Page" button
    private static final String PREV_PAGE_TEXT = "Previous Page"; // Text for the previous page button
    private static final String NEXT_PAGE_TEXT = "Next Page"; // Text for the next page button
    private static final String LEADERBOARD_TITLE_FORMAT = "Leaderboard (Page %d)"; // Format for the inventory title

    /**
     * Generates the inventory for a specific page of the leaderboard.
     *
     * @param page The page number of the leaderboard to be displayed.
     * @return The inventory for the specified page of the leaderboard.
     */
    public Inventory getInventory(int page) {
        Inventory leaderboardMenu = Bukkit.createInventory(new LeaderboardMenuHolder(null), INVENTORY_SIZE,
                Component.text(String.format(LEADERBOARD_TITLE_FORMAT, page)));

        List<Map.Entry<String, Integer>> allEntries = PlayerData.getInstance().getLeaderboard();
        int startEntry = (page - 1) * ITEMS_PER_PAGE;
        int endEntry = Math.min(startEntry + ITEMS_PER_PAGE, allEntries.size());
        List<Map.Entry<String, Integer>> pageEntries = allEntries.subList(startEntry, endEntry);

        addLeaderboardEntries(leaderboardMenu, pageEntries);
        addNavigationButtons(leaderboardMenu, page, allEntries.size());

        return leaderboardMenu;
    }

    /**
     * Adds navigation buttons to the inventory for navigating between pages.
     *
     * @param inventory    The inventory where the buttons will be added.
     * @param page         The current page number.
     * @param totalEntries The total number of entries in the leaderboard.
     */
    private void addNavigationButtons(Inventory inventory, int page, int totalEntries) {
        if (page > 1) {
            inventory.setItem(PREV_PAGE_SLOT, createMenuItem(Material.ARROW, PREV_PAGE_TEXT));
        }
        if (totalEntries > page * ITEMS_PER_PAGE) {
            inventory.setItem(NEXT_PAGE_SLOT, createMenuItem(Material.ARROW, NEXT_PAGE_TEXT));
        }
    }

    /**
     * Adds leaderboard entries to the inventory.
     *
     * @param inventory The inventory to which the leaderboard entries will be added.
     * @param entries   The leaderboard entries to be added.
     */
    private void addLeaderboardEntries(Inventory inventory, List<Map.Entry<String, Integer>> entries) {
        int slot = START_SLOT;
        for (Map.Entry<String, Integer> entry : entries) {
            ItemStack playerHead = createPlayerHead(entry.getKey());
            ItemMeta meta = playerHead.getItemMeta();
            meta.displayName(Component.text(entry.getKey() + ": " + entry.getValue()));
            playerHead.setItemMeta(meta);
            inventory.setItem(slot, playerHead);
            slot++;
        }
    }

    /**
     * Extracts the current page number from the inventory title.
     *
     * @param title The title of the inventory, which contains the page number.
     * @return The current page number or -1 if the page number is not found.
     */
    public static int getCurrentPage(Component title) {
        String titleString = title.toString();
        int startIndex = titleString.indexOf("(Page ");
        int endIndex = titleString.indexOf(")");

        if (startIndex == -1 || endIndex == -1) {
            return -1; // Invalid page number indicates an error
        }

        String pageString = titleString.substring(startIndex + 6, endIndex);
        return Integer.parseInt(pageString);
    }
}