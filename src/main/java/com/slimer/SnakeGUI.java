package com.slimer;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class SnakeGUI implements Listener {
    private final SnakePlugin plugin;
    private final Set<UUID> awaitingSpeedInput = new HashSet<>();

    public SnakeGUI(SnakePlugin plugin) {
        this.plugin = plugin;
    }

    // Creates the main menu inventory
    public static Inventory createMainMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 9, Component.text("Snake Main Menu"));

        menu.setItem(0, createMenuItem(Material.GREEN_WOOL, "Start"));
        menu.setItem(1, createMenuItem(Material.RED_WOOL, "Stop"));
        menu.setItem(2, createMenuItem(Material.BOOK, "Help"));
        menu.setItem(3, createMenuItem(Material.PAINTING, "Set Color"));
        menu.setItem(4, createMenuItem(Material.GOLDEN_APPLE, "Leaderboard"));
        menu.setItem(5, createPlayerHeadMenuItem(player));

        if (player.hasPermission("snake.admin")) {
            menu.setItem(8, createMenuItem(Material.GOLD_BLOCK, "Admin Options"));
        }

        return menu;
    }

    // Creates the admin menu inventory
    public static Inventory createAdminMenu() {
        Inventory adminMenu = Bukkit.createInventory(null, 9, Component.text("Admin Options"));

        adminMenu.setItem(0, createSpeedPotionMenuItem());
        adminMenu.setItem(1, createMenuItem(Material.LIME_BANNER, "Set Lobby Coordinates"));
        adminMenu.setItem(2, createMenuItem(Material.RED_BANNER, "Set Game Coordinates"));
        adminMenu.setItem(3, createMenuItem(Material.EMERALD, "Reload"));

        return adminMenu;
    }

    // Creates the color selection menu inventory
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

    // Creates the speed selection menu inventory
    public static Inventory createSpeedMenu() {
        Inventory speedMenu = Bukkit.createInventory(null, 9, Component.text("Set Speed"));

        speedMenu.setItem(0, createMenuItem(Material.STONE_BUTTON, "5"));
        speedMenu.setItem(1, createMenuItem(Material.STONE_BUTTON, "10"));
        speedMenu.setItem(2, createMenuItem(Material.STONE_BUTTON, "15"));
        speedMenu.setItem(3, createMenuItem(Material.STONE_BUTTON, "20"));
        speedMenu.setItem(4, createMenuItem(Material.BARRIER, "Other"));

        return speedMenu;
    }

    // Creates a generic menu item with a given material and display name
    private static ItemStack createMenuItem(Material material, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(displayName));
        item.setItemMeta(meta);
        return item;
    }

    // Creates a speed potion menu item
    private static ItemStack createSpeedPotionMenuItem() {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setBasePotionData(new PotionData(PotionType.SPEED));
        meta.displayName(Component.text("Set Speed"));
        item.setItemMeta(meta);
        return item;
    }

    // Creates a player's head menu item
    private static ItemStack createPlayerHeadMenuItem(Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        meta.displayName(Component.text("High Score"));
        item.setItemMeta(meta);
        return item;
    }

    // Event handler for player chat, used to get custom speed input
    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (awaitingSpeedInput.remove(player.getUniqueId())) {
            String message = PlainTextComponentSerializer.plainText().serialize(event.originalMessage());
            if ("cancel".equalsIgnoreCase(message)) {
                // Inform the player that the speed change has been canceled
                player.sendMessage(Component.text("Speed change has been canceled.", NamedTextColor.YELLOW));
                event.setCancelled(true);
                return; // Exit the method
            }
            try {
                int speed = Integer.parseInt(message);
                if (speed > 0) {
                    // Schedule the command execution on the main thread
                    Bukkit.getScheduler().runTask(plugin, () -> player.performCommand("snake setspeed " + speed));
                } else {
                    // Inform the player that the speed is invalid
                    player.sendMessage(Component.text("Invalid speed. The speed change has been canceled.", NamedTextColor.RED));
                }
            } catch (NumberFormatException e) {
                // Inform the player that the input is not a valid number
                player.sendMessage(Component.text("Invalid input. The speed change has been canceled.", NamedTextColor.RED));
            }
            event.setCancelled(true); // Cancel the chat message
        }
    }

    // Event handler for inventory click, used to handle various GUI menus
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
                case GREEN_WOOL -> player.performCommand("snake start");
                case RED_WOOL -> player.performCommand("snake stop");
                case BOOK -> player.performCommand("snake help");
                case PAINTING -> {
                    player.openInventory(createColorMenu()); // Open color submenu
                    event.setCancelled(true);
                    return; // Return without closing the main menu
                }
                case GOLDEN_APPLE -> player.performCommand("snake leaderboard");
                case PLAYER_HEAD -> player.performCommand("snake highscore");
                case GOLD_BLOCK -> {
                    player.openInventory(createAdminMenu()); // Open admin submenu
                    event.setCancelled(true);
                    return; // Return without closing the main menu
                }
            }
            player.closeInventory(); // Close the main menu for all other items
            event.setCancelled(true);
        } else if (title.equals(Component.text("Admin Options"))) {
            switch (clickedItem.getType()) {
                case POTION -> {
                    player.openInventory(createSpeedMenu()); // Open speed submenu
                    event.setCancelled(true);
                    return; // Return without closing the admin menu
                }
                case LIME_BANNER -> player.performCommand("snake lobbycords");
                case RED_BANNER -> player.performCommand("snake gamecords");
                case EMERALD -> player.performCommand("snake reload");
            }
            player.closeInventory(); // Close the main menu for all other items
            event.setCancelled(true);
        } else if (title.equals(Component.text("Set Speed"))) {
            switch (clickedItem.getType()) {
                case STONE_BUTTON -> {
                    int speed = Integer.parseInt(PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(clickedItem.getItemMeta().displayName())));
                    player.performCommand("snake setspeed " + speed);
                }
                case BARRIER -> {
                    // Add the player to the awaitingSpeedInput set and prompt them for input
                    awaitingSpeedInput.add(player.getUniqueId());
                    player.sendMessage(Component.text("Please enter the desired speed value in chat or type cancel to cancel", NamedTextColor.GREEN));
                }
            }
            player.closeInventory(); // Close the speed menu for all items
            event.setCancelled(true);
        } else if (title.equals(Component.text("Select Snake Color"))) {
            Material clickedMaterial = clickedItem.getType();
            if (clickedMaterial.toString().endsWith("_WOOL")) {
                String color = clickedMaterial.name().replace("_WOOL", "").toLowerCase();
                player.performCommand("snake color " + color);
                player.closeInventory();
                event.setCancelled(true);
            }
        }
    }
}