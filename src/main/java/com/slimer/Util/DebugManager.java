package com.slimer.Util;

import com.slimer.Main.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * Manager responsible for debugging functionalities within the Snake plugin.
 */
public class DebugManager {

    private static final EnumSet<Category> activeCategories = EnumSet.noneOf(Category.class);
    private static DebugDestination globalDebugDestination = DebugDestination.BOTH;

    /**
     * Enables a specific debug category.
     *
     * @param category The debug category to enable.
     */
    public static void enableCategory(Category category) {
        activeCategories.add(category);
    }

    /**
     * Disables a specific debug category.
     *
     * @param category The debug category to disable.
     */
    public static void disableCategory(Category category) {
        activeCategories.remove(category);
    }

    /**
     * Checks if a specific debug category is enabled.
     *
     * @param category The debug category to check.
     * @return True if the category is enabled, false otherwise.
     */
    public static boolean isCategoryEnabled(Category category) {
        return activeCategories.contains(category);
    }

    /**
     * Logs a debug message to the appropriate destinations based on the active settings.
     * <p>
     * For players with the appropriate permission, the message is formatted with colors and sent in-game.
     * For the console, a plain text version of the message is logged.
     * </p>
     *
     * @param category The category of the debug message.
     * @param message  The actual debug message to be logged.
     */
    public static void log(Category category, String message) {
        if (isCategoryEnabled(category)) {
            String version = Main.getPluginVersion();

            // For console
            String plainMessage = "{Snake " + version + " DEBUG} [" + category.name() + "] " + message;

            // For players
            TextComponent formattedMessage = Component.text("{Snake " + version + " DEBUG} [", NamedTextColor.GOLD)
                    .append(Component.text(category.name(), NamedTextColor.YELLOW))
                    .append(Component.text("] ", NamedTextColor.GOLD))
                    .append(Component.text(message, NamedTextColor.GREEN));

            if (globalDebugDestination == DebugDestination.PLAYER || globalDebugDestination == DebugDestination.BOTH) {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (onlinePlayer.hasPermission("snake.admin")) {
                        onlinePlayer.sendMessage(formattedMessage);
                    }
                }
            }

            if (globalDebugDestination == DebugDestination.CONSOLE || globalDebugDestination == DebugDestination.BOTH) {
                Bukkit.getLogger().info(plainMessage);
            }
        }
    }

    /**
     * Enum representing the various debug categories available for the Snake plugin.
     */
    public enum Category { // Examples to be removed when debug logs are added
        EXAMPLE_ONE,
        EXAMPLE_TWO,
        EXAMPLE_THREE,
    }

    /**
     * Enum representing the various destinations where debug messages can be sent.
     */
    public enum DebugDestination {
        PLAYER, CONSOLE, BOTH
    }

    /**
     * Command executor class for the Snake plugin's debugging system.
     */
    public static class ToggleDebugCommand implements CommandExecutor, TabCompleter {

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("This command can only be run by a player.", NamedTextColor.RED));
                return false;
            }
            if (!player.hasPermission("snake.admin")) {
                player.sendMessage(Component.text("You don't have permission to run this command.", NamedTextColor.RED));
                return false;
            }
            if (args.length == 0) {
                handleHelpCommand(player);
                return false;
            }
            String subCommand = args[0].toLowerCase();
            return switch (subCommand) {
                case "category" -> handleToggleCategoryCommand(player, args);
                case "destination" -> handleSetDestinationCommand(player, args);
                case "help" -> handleHelpCommand(player);
                case "status" -> handleDebugStatusCommand(player);
                default -> {
                    player.sendMessage(Component.text("Unknown subcommand. Use one of the following:", NamedTextColor.RED));
                    player.sendMessage(Component.text("/snakedebug category", NamedTextColor.GRAY));
                    player.sendMessage(Component.text("/snakedebug destination", NamedTextColor.GRAY));
                    player.sendMessage(Component.text("/snakedebug help", NamedTextColor.GRAY));
                    player.sendMessage(Component.text("/snakedebug status", NamedTextColor.GRAY));
                    yield false;
                }
            };
        }

        /**
         * Provides tab completion suggestions for the debug command based on the current input.
         * This method helps streamline command usage by offering context-specific completions.
         *
         * @param sender  The CommandSender who is tab-completing (typically a player).
         * @param command The Command being completed.
         * @param alias   The alias used to execute the command.
         * @param args    The arguments provided so far in the command.
         * @return A list of suggested completions based on the provided input.
         */
        @Override
        public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
            List<String> completions = new ArrayList<>();

            if (args.length == 1) {
                List<String> subCommands = Arrays.asList("category", "destination", "help", "status");
                for (String subCmd : subCommands) {
                    if (subCmd.startsWith(args[0].toLowerCase())) {
                        completions.add(subCmd);
                    }
                }
            } else if (args.length == 2) {
                if ("category".equalsIgnoreCase(args[0])) {
                    for (Category category : Category.values()) {
                        if (category.name().toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(category.name().toLowerCase());
                        }
                    }
                } else if ("destination".equalsIgnoreCase(args[0])) {
                    for (DebugDestination dest : DebugDestination.values()) {
                        if (dest.name().toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(dest.name().toLowerCase());
                        }
                    }
                }
            }

            return completions;
        }

        /**
         * Handles the "category" subcommand to toggle the status of a specific debug category.
         *
         * @param player The player executing the command.
         * @param args   The arguments passed to the command.
         * @return True if the command was handled successfully, false otherwise.
         */
        private boolean handleToggleCategoryCommand(Player player, String[] args) {
            if (args.length != 2) {
                player.sendMessage(Component.text("Usage: /snakedebug category [Category]", NamedTextColor.RED));
                return false;
            }
            try {
                Category category = Category.valueOf(args[1].toUpperCase());

                if (isCategoryEnabled(category)) {
                    disableCategory(category);
                    player.sendMessage(Component.text(category.name() + " debug mode disabled", NamedTextColor.GREEN));
                } else {
                    enableCategory(category);
                    player.sendMessage(Component.text(category.name() + " debug mode enabled", NamedTextColor.GREEN));
                }
            } catch (IllegalArgumentException e) {
                player.sendMessage(Component.text("Invalid category name.", NamedTextColor.RED));
                return false;
            }
            return true;
        }

        /**
         * Handles the "destination" subcommand to set the global destination for debug messages.
         *
         * @param player The player executing the command.
         * @param args   The arguments passed to the command.
         * @return True if the command was handled successfully, false otherwise.
         */
        private boolean handleSetDestinationCommand(Player player, String[] args) {
            if (args.length != 2) {
                player.sendMessage(Component.text("Usage: /snakedebug destination [Destination]", NamedTextColor.RED));
                return false;
            }
            try {
                DebugDestination destination = DebugDestination.valueOf(args[1].toUpperCase());
                globalDebugDestination = destination;
                player.sendMessage(Component.text("Debug message destination set to " + destination.name(), NamedTextColor.GREEN));
            } catch (IllegalArgumentException e) {
                player.sendMessage(Component.text("Invalid debug destination name.", NamedTextColor.RED));
                return false;
            }
            return true;
        }

        /**
         * Displays the current status of the debug system, including enabled categories and the global debug message destination.
         *
         * @param player The player to whom the status will be displayed.
         * @return True indicating the command was handled successfully.
         */
        private boolean handleDebugStatusCommand(Player player) {
            player.sendMessage(Component.text("Currently enabled debug categories:", NamedTextColor.GOLD));
            for (Category category : activeCategories) {
                player.sendMessage(Component.text("- " + category.name(), NamedTextColor.GRAY));
            }
            if (activeCategories.isEmpty()) {
                player.sendMessage(Component.text("No categories currently enabled.", NamedTextColor.RED));
            }
            player.sendMessage(Component.text("Debug messages are being sent to: " + globalDebugDestination.name(), NamedTextColor.GOLD));
            return true;
        }

        /**
         * Displays the help message to the player, explaining the available debug commands.
         *
         * @param player The player to whom the help message will be displayed.
         * @return True indicating the command was handled successfully.
         */
        private boolean handleHelpCommand(Player player) {
            player.sendMessage(Component.text("The Snake Debugging system provides insights into the internal operations of the Snake plugin. Here's how to use it:", NamedTextColor.GOLD));

            TextComponent linkMessage = Component.text("For more details, click here to visit the debugging documentation.", NamedTextColor.AQUA)
                    // This URL is a placeholder
                    .clickEvent(ClickEvent.openUrl("http://www.example.com"))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to open the documentation website!")));
            player.sendMessage(linkMessage);

            player.sendMessage(Component.text("/snakedebug category [Category]", NamedTextColor.YELLOW));
            player.sendMessage(Component.text(" - Toggle the debug mode for a specific category.", NamedTextColor.GREEN));
            player.sendMessage(Component.text("Available categories: " + Arrays.toString(Category.values()), NamedTextColor.GOLD));

            player.sendMessage(Component.text("/snakedebug destination [Destination]", NamedTextColor.YELLOW));
            player.sendMessage(Component.text(" - Set the global destination for debug messages.", NamedTextColor.GREEN));
            player.sendMessage(Component.text("Available destinations: " + Arrays.toString(DebugDestination.values()), NamedTextColor.GOLD));

            player.sendMessage(Component.text("/snakedebug status", NamedTextColor.YELLOW));
            player.sendMessage(Component.text(" - View the currently enabled debug categories and the global debug message destination.", NamedTextColor.GREEN));

            player.sendMessage(Component.text("/snakedebug help", NamedTextColor.YELLOW));
            player.sendMessage(Component.text(" - Display this help message.", NamedTextColor.GREEN));

            return true;
        }
    }
}