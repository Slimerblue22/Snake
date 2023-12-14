package com.slimer.Util;

import com.slimer.Main.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
 * The DebugManager class provides a system for managing and logging debug messages within the Snake plugin.
 * It allows enabling and disabling specific debug categories, setting the global debug message destination,
 * and provides a command handler for players to manage debug settings.
 * <p>
 * Last updated: V2.1.0
 *
 * @author Slimerblue22
 */
public class DebugManager {
    private static final EnumSet<Category> activeCategories = EnumSet.noneOf(Category.class);
    private static DebugDestination globalDebugDestination = DebugDestination.BOTH;

    /**
     * Enables a specific debug category. When a debug category is enabled, debug messages related to that category
     * will be logged and displayed as appropriate.
     *
     * @param category The debug category to enable.
     */
    public static void enableCategory(Category category) {
        activeCategories.add(category);
    }

    /**
     * Disables a specific debug category. When a debug category is disabled, debug messages related to that category
     * will not be logged or displayed.
     *
     * @param category The debug category to disable.
     */
    public static void disableCategory(Category category) {
        activeCategories.remove(category);
    }

    /**
     * Checks if a specific debug category is enabled. If a debug category is enabled, debug messages related to that
     * category will be logged and displayed as appropriate.
     *
     * @param category The debug category to check.
     * @return True if the category is enabled, false otherwise.
     */
    public static boolean isCategoryEnabled(Category category) {
        return activeCategories.contains(category);
    }

    /**
     * Logs a debug message to the appropriate destinations based on the active settings.
     * For players with the appropriate permission, the message is formatted with colors and sent in-game.
     * For the console, a plain text version of the message is logged.
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

            if (globalDebugDestination == DebugDestination.BOTH || globalDebugDestination == DebugDestination.PLAYER) {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (onlinePlayer.hasPermission("snake.admin")) {
                        onlinePlayer.sendMessage(formattedMessage);
                    }
                }
            }

            if (globalDebugDestination == DebugDestination.BOTH || globalDebugDestination == DebugDestination.CONSOLE) {
                Bukkit.getLogger().info(plainMessage);
            }
        }
    }

    /**
     * Enum representing various debug categories available for the Snake plugin.
     * These categories help classify and organize debug messages related to different aspects of the plugin's functionality.
     */
    public enum Category {
        DEBUG
    }

    /**
     * Enum representing various destinations where debug messages can be sent.
     * These destinations determine where the debug messages are logged and displayed, including in-game for players
     * and in the console.
     */
    public enum DebugDestination {
        PLAYER, CONSOLE, BOTH
    }

    /**
     * The ToggleDebugCommand class serves as a nested command handler within the DebugManager class.
     * It provides commands for players to manage debug settings, including enabling/disabling categories,
     * setting the global debug message destination, checking debug status, and displaying help messages.
     * <p>
     * Last updated: V2.1.0
     *
     * @author Slimerblue22
     */
    public static class ToggleDebugCommand implements CommandExecutor, TabCompleter {

        /**
         * Executes the "snake debug" command.
         *
         * @param sender  The sender of the command.
         * @param command The command being executed.
         * @param label   The label used to invoke the command.
         * @param args    The arguments provided with the command.
         * @return True if the command was handled successfully, false otherwise.
         */
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
                    handleUnknownCommand(player);
                    yield false;
                }
            };
        }

        /**
         * Displays an unknown command message to the specified player.
         *
         * @param player The player to whom the message should be displayed.
         */
        private void handleUnknownCommand(Player player) {
            player.sendMessage(Component.text("Unknown subcommand. Use one of the following:", NamedTextColor.RED));
            String[] commands = {"category", "destination", "help", "status"};
            for (String cmd : commands) {
                player.sendMessage(Component.text("/snakedebug " + cmd, NamedTextColor.GRAY));
            }
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
                // Provide first level tab completion for subcommands
                completions.addAll(Arrays.asList("category", "destination", "help", "status"));
            } else if (args.length == 2) {
                String subCommand = args[0].toLowerCase();

                if ("category".equalsIgnoreCase(subCommand)) {
                    // Tab complete for debug category names at the second level
                    completions.addAll(Arrays.stream(Category.values())
                            .map(Category::name)
                            .map(String::toLowerCase)
                            .filter(categoryName -> categoryName.startsWith(args[1].toLowerCase()))
                            .toList());
                } else if ("destination".equalsIgnoreCase(subCommand)) {
                    // Tab complete for debug destination names at the second level
                    completions.addAll(Arrays.stream(DebugDestination.values())
                            .map(DebugDestination::name)
                            .map(String::toLowerCase)
                            .filter(destName -> destName.startsWith(args[1].toLowerCase()))
                            .toList());
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

            String categoryName = args[1].toUpperCase();
            try {
                Category category = Category.valueOf(categoryName);

                // Toggle the category state and send appropriate feedback
                if (isCategoryEnabled(category)) {
                    disableCategory(category);
                    player.sendMessage(Component.text(category.name() + " debug mode disabled", NamedTextColor.GREEN));
                } else {
                    enableCategory(category);
                    player.sendMessage(Component.text(category.name() + " debug mode enabled", NamedTextColor.GREEN));
                }
            } catch (IllegalArgumentException e) {
                // Handle invalid category names
                player.sendMessage(Component.text("Invalid category name: " + categoryName, NamedTextColor.RED));
                player.sendMessage(Component.text("Available categories: " + Arrays.toString(Category.values()), NamedTextColor.GRAY));
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
                // Inform the player of the invalid input and provide valid options
                player.sendMessage(Component.text("Invalid debug destination name.", NamedTextColor.RED));
                player.sendMessage(Component.text("Available destinations: " + Arrays.toString(DebugDestination.values()), NamedTextColor.GRAY));
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
            TextComponent.Builder messageBuilder = Component.text();

            // Constructing header section
            messageBuilder.append(Component.text("Debug System Status", NamedTextColor.GOLD)
                            .decoration(TextDecoration.BOLD, true))
                    .append(Component.newline());

            // Listing enabled categories
            messageBuilder.append(Component.text("Enabled Categories:", NamedTextColor.GOLD))
                    .append(Component.newline());
            if (!activeCategories.isEmpty()) {
                for (Category category : activeCategories) {
                    messageBuilder.append(Component.text("- " + category.name(), NamedTextColor.GREEN))
                            .append(Component.newline());
                }
            } else {
                messageBuilder.append(Component.text("No categories currently enabled.", NamedTextColor.RED))
                        .append(Component.newline());
            }

            // Displaying global debug message destination
            messageBuilder.append(Component.text("Debug messages are being sent to: " + globalDebugDestination.name(), NamedTextColor.GOLD))
                    .append(Component.newline());

            // Sending the formatted status message to the player
            player.sendMessage(messageBuilder.build());
            return true;
        }

        /**
         * Displays the help message to the player, explaining the available debug commands.
         *
         * @param player The player to whom the help message will be displayed.
         * @return True indicating the command was handled successfully.
         */
        private boolean handleHelpCommand(Player player) {
            TextComponent.Builder messageBuilder = Component.text();

            // Constructing the header
            messageBuilder.append(Component.text("Snake Debugging System", NamedTextColor.GOLD)
                            .decoration(TextDecoration.BOLD, true))
                    .append(Component.newline());

            // Introduction section
            messageBuilder.append(Component.text("The Snake Debugging system provides insights into the internal operations of the Snake plugin. Here's how to use it:", NamedTextColor.YELLOW))
                    .append(Component.newline());

            // Adding the documentation link
            TextComponent linkMessage = Component.text("For more details, click here to visit the debugging documentation.", NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.openUrl("http://www.example.com"))  // Replace with the actual documentation link
                    .hoverEvent(HoverEvent.showText(Component.text("Click to open the documentation website!")));
            messageBuilder.append(linkMessage).append(Component.newline());

            // Listing available debug commands
            messageBuilder.append(Component.text("Available Debug Commands:", NamedTextColor.GOLD))
                    .append(Component.newline());

            // Detailing each command
            // Command: category
            messageBuilder.append(Component.text("/snakedebug category [Category]", NamedTextColor.YELLOW))
                    .append(Component.text(" - Toggle the debug mode for a specific category.", NamedTextColor.GREEN))
                    .append(Component.newline())
                    .append(Component.text("Available categories: " + Arrays.toString(Category.values()), NamedTextColor.GRAY))
                    .append(Component.newline());

            // Command: destination
            messageBuilder.append(Component.text("/snakedebug destination [Destination]", NamedTextColor.YELLOW))
                    .append(Component.text(" - Set the global destination for debug messages.", NamedTextColor.GREEN))
                    .append(Component.newline())
                    .append(Component.text("Available destinations: " + Arrays.toString(DebugDestination.values()), NamedTextColor.GRAY))
                    .append(Component.newline());

            // Command: status
            messageBuilder.append(Component.text("/snakedebug status", NamedTextColor.YELLOW))
                    .append(Component.text(" - View the currently enabled debug categories and the global debug message destination.", NamedTextColor.GREEN))
                    .append(Component.newline());

            // Command: help
            messageBuilder.append(Component.text("/snakedebug help", NamedTextColor.YELLOW))
                    .append(Component.text(" - Display this help message.", NamedTextColor.GREEN));

            // Sending the formatted help message to the player
            player.sendMessage(messageBuilder.build());
            return true;
        }
    }
}