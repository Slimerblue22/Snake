package com.slimer.GUI;

import com.slimer.GUI.Menus.ColorMenu;
import com.slimer.GUI.Menus.LeaderboardMenu;
import com.slimer.GUI.Menus.MainMenu;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Manages the Graphical User Interface (GUI) interactions for players on the server.
 * This class follows the Singleton pattern to ensure only one instance is used throughout the application.
 * It provides methods to open various menus like the Main Menu, Color Menu, and Leaderboard Menu.
 * <p>
 * Last updated: V2.1.0
 *
 * @author Slimerblue22
 */
public class GuiManager {
    private static GuiManager instance;

    /**
     * Private constructor to prevent instantiation from outside the class.
     */
    private GuiManager() {
    }

    /**
     * Retrieves the singleton instance of GuiManager.
     * If the instance does not exist, it creates and returns a new instance.
     *
     * @return The singleton instance of GuiManager.
     */
    public static GuiManager getInstance() {
        if (instance == null) {
            instance = new GuiManager();
        }
        return instance;
    }

    /**
     * Opens the main menu for the specified player.
     *
     * @param player The player for whom the main menu is to be opened.
     */
    public void openMainMenu(Player player) {
        Inventory mainMenu = new MainMenu().getInventory(player);
        player.openInventory(mainMenu);
    }

    /**
     * Opens the color menu for the specified player.
     *
     * @param player The player for whom the color menu is to be opened.
     */
    public void openColorMenu(Player player) {
        Inventory colorMenu = ColorMenu.getInstance().getInventory();
        player.openInventory(colorMenu);
    }

    /**
     * Opens the leaderboard menu for the specified player.
     *
     * @param player The player for whom the leaderboard menu is to be opened.
     * @param page   The page number of the leaderboard to display.
     */
    public void openLeaderboardMenu(Player player, int page) {
        Inventory leaderboardMenu = new LeaderboardMenu().getInventory(page);
        player.openInventory(leaderboardMenu);
    }
}