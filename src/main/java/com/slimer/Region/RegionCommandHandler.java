package com.slimer.Region;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles commands related to managing regions in the Snake game. This class serves as the executor for region-related
 * commands and directs them to the appropriate sub-command logic. It allows for registering, unregistering, linking,
 * unlinking, adding teleport coordinates, and viewing data about game regions and their links.
 * <p>
 * Last updated: V2.1.0
 *
 * @author Slimerblue22
 */
public class RegionCommandHandler implements CommandExecutor, TabCompleter {
    private final WGHelpers wgHelpers = WGHelpers.getInstance();
    private final RegionService service = RegionService.getInstance();
    private final RegionHelpers regionHelpers = RegionHelpers.getInstance();

    /**
     * Processes the command input and directs to appropriate sub-command logic.
     *
     * @param sender  The command sender.
     * @param command The command being executed.
     * @param label   The command alias.
     * @param args    The arguments passed with the command.
     * @return true if the command was processed successfully, false otherwise.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("snake.admin")) {
            sender.sendMessage(Component.text("You don't have permission to run this command.", NamedTextColor.RED));
            return false;
        }

        if (args.length == 0) {
            handleUnknownCommand(sender);
            return false;
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "register" -> registerRegion(sender, args);
            case "unregister" -> unregisterRegion(sender, args);
            case "link" -> linkRegions(sender, args);
            case "unlink" -> unlinkRegions(sender, args);
            case "addtp" -> addTP(sender, args);
            case "view" -> viewData(sender, args);
            default -> {
                handleUnknownCommand(sender);
                yield false;
            }
        };
    }

    /**
     * Displays an unknown command message to the specified player.
     *
     * @param player The player to whom the message should be displayed.
     */
    private void handleUnknownCommand(@NotNull CommandSender player) {
        player.sendMessage(Component.text("Unknown subcommand. Use one of the following:", NamedTextColor.RED));
        String[] commands = {"register", "unregister", "link", "unlink", "addtp", "view"};
        for (String cmd : commands) {
            player.sendMessage(Component.text("/snakeregion " + cmd, NamedTextColor.GRAY));
        }
    }

    /**
     * Handles the tab completion for the Snake region system's commands.
     * This method provides auto-complete suggestions for the region commands.
     *
     * @param sender  The sender of the command, can be a player or the console.
     * @param command The command that was executed.
     * @param alias   The alias that the sender used to trigger the command.
     * @param args    The arguments that were provided with the command.
     * @return A list of possible completions for a command argument.
     */
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String[] subCommands = {"register", "unregister", "link", "unlink", "addtp", "view"};
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        }

        return completions;
    }

    /**
     * Registers a new region in the game.
     *
     * @param player The player executing the command.
     * @param args   The arguments passed with the command.
     * @return true if the region was registered successfully, false otherwise.
     */
    private boolean registerRegion(@NotNull CommandSender player, String[] args) {
        if (args.length != 4) {
            player.sendMessage(Component.text("Incorrect usage. Example: /snakeregion register [region type; game or lobby] [region name] [world region is in]", NamedTextColor.RED));
            return false;
        }

        String regionType = args[1].toLowerCase();
        String regionName = args[2].toLowerCase();
        String worldName = args[3].toLowerCase();

        if (!"lobby".equals(regionType) && !"game".equals(regionType)) {
            player.sendMessage(Component.text("Region type must be either 'lobby' or 'game'.", NamedTextColor.RED));
            return false;
        }

        if (!wgHelpers.doesWGRegionExist(worldName, regionName)) {
            player.sendMessage(Component.text(String.format("Region %s is not registered in WorldGuard for world %s.", regionName, worldName), NamedTextColor.RED));
            return false;
        }

        if (regionHelpers.isRegionRegistered(regionName)) {
            player.sendMessage(Component.text("Region is already registered.", NamedTextColor.RED));
            return false;
        }

        if (service.registerNewRegion(regionType, regionName, worldName)) {
            player.sendMessage(Component.text("Region registered successfully.", NamedTextColor.GREEN));
            return true;
        } else {
            player.sendMessage(Component.text("Failed to register region.", NamedTextColor.RED));
            return false;
        }
    }

    /**
     * Unregisters an existing region from the game.
     *
     * @param player The player executing the command.
     * @param args   The arguments passed with the command.
     * @return true if the region was unregistered successfully, false otherwise.
     */
    private boolean unregisterRegion(@NotNull CommandSender player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(Component.text("Incorrect usage. Example: /snakeregion unregister [region name].", NamedTextColor.RED));
            return false;
        }

        String regionName = args[1].toLowerCase();

        if (!regionHelpers.isRegionRegistered(regionName)) {
            player.sendMessage(Component.text("Region is not registered.", NamedTextColor.RED));
            return false;
        }

        if (regionHelpers.getLinkID(regionName) != null) {
            player.sendMessage(Component.text("The region is linked to another region. Unlink it first before unregistering.", NamedTextColor.RED));
            return false;
        }

        if (service.unregisterRegion(regionName)) {
            player.sendMessage(Component.text("Region unregistered successfully.", NamedTextColor.GREEN));
            return true;
        } else {
            player.sendMessage(Component.text("Failed to unregister region.", NamedTextColor.RED));
            return false;
        }
    }

    /**
     * Links two regions together.
     *
     * @param player The player executing the command.
     * @param args   The arguments passed with the command.
     * @return true if the regions were linked successfully, false otherwise.
     */
    private boolean linkRegions(@NotNull CommandSender player, String[] args) {
        if (args.length != 3) {
            player.sendMessage(Component.text("Incorrect usage. Example: /snakeregion link [region1 name] [region2 name].", NamedTextColor.RED));
            return false;
        }

        String regionName1 = args[1].toLowerCase();
        String regionName2 = args[2].toLowerCase();

        if (!regionHelpers.isRegionRegistered(regionName1) || !regionHelpers.isRegionRegistered(regionName2)) {
            player.sendMessage(Component.text("One or both regions are not registered.", NamedTextColor.RED));
            return false;
        }

        String type1 = regionHelpers.getRegionType(regionName1);
        String type2 = regionHelpers.getRegionType(regionName2);

        if (!((type1.equals("game") && type2.equals("lobby")) || (type1.equals("lobby") && type2.equals("game")))) {
            player.sendMessage(Component.text("You must link a game region with a lobby region.", NamedTextColor.RED));
            return false;
        }

        if (regionHelpers.isRegionLinked(regionName1) || regionHelpers.isRegionLinked(regionName2)) {
            player.sendMessage(Component.text("One or both regions are already linked to another region.", NamedTextColor.RED));
            return false;
        }

        if (service.linkRegions(regionName1, regionName2)) {
            player.sendMessage(Component.text("Regions linked successfully.", NamedTextColor.GREEN));
            return true;
        } else {
            player.sendMessage(Component.text("Failed to link regions.", NamedTextColor.RED));
            return false;
        }
    }

    /**
     * Unlinks two regions that are currently linked.
     *
     * @param player The player executing the command.
     * @param args   The arguments passed with the command.
     * @return true if the regions were unlinked successfully, false otherwise.
     */
    private boolean unlinkRegions(@NotNull CommandSender player, String[] args) {
        if (args.length != 3) {
            player.sendMessage(Component.text("Incorrect usage. Example: /snakeregion unlink [region1 name] [region2 name].", NamedTextColor.RED));
            return false;
        }

        String regionName1 = args[1].toLowerCase();
        String regionName2 = args[2].toLowerCase();

        if (regionName1.equals(regionName2)) {
            player.sendMessage(Component.text("Cannot unlink the same region.", NamedTextColor.RED));
            return false;
        }

        if (!regionHelpers.isRegionRegistered(regionName1) || !regionHelpers.isRegionRegistered(regionName2)) {
            player.sendMessage(Component.text("One or both regions are not registered.", NamedTextColor.RED));
            return false;
        }

        Integer linkID1 = regionHelpers.getLinkID(regionName1);
        Integer linkID2 = regionHelpers.getLinkID(regionName2);

        if (linkID1 == null || linkID2 == null) {
            player.sendMessage(Component.text("One or both regions are not linked.", NamedTextColor.RED));
            return false;
        }

        if (!linkID1.equals(linkID2)) {
            player.sendMessage(Component.text("The provided regions are not linked together.", NamedTextColor.RED));
            return false;
        }

        if (service.unlinkRegions(regionName1, regionName2)) {
            player.sendMessage(Component.text("Regions unlinked successfully.", NamedTextColor.GREEN));
            return true;
        } else {
            player.sendMessage(Component.text("Failed to unlink regions.", NamedTextColor.RED));
            return false;
        }
    }

    /**
     * Adds or updates the TP (teleport) coordinates of a region.
     *
     * @param sender The player executing the command.
     * @param args   The arguments passed with the command.
     * @return true if the TP coordinates were set successfully, false otherwise.
     */
    private boolean addTP(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Because of how the addtp command works, it can only be run by a player.", NamedTextColor.RED));
            return false;
        }

        if (args.length != 2 && args.length != 5) {
            player.sendMessage(Component.text("Incorrect usage. Example: /snakeregion addtp [region name] or /snakeregion addtp [region name] [x] [y] [z].", NamedTextColor.RED));
            return false;
        }

        String regionName = args[1].toLowerCase();
        int x, y, z;

        if (args.length == 5) {
            try {
                x = Math.round(Float.parseFloat(args[2]));
                y = Math.round(Float.parseFloat(args[3]));
                z = Math.round(Float.parseFloat(args[4]));
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid coordinates format. Please ensure x, y, and z are valid numbers.", NamedTextColor.RED));
                return false;
            }
        } else {
            Location loc = player.getLocation();
            x = loc.getBlockX();
            y = loc.getBlockY();
            z = loc.getBlockZ();
        }

        if (!wgHelpers.areCoordinatesInWGRegion(player.getWorld().getName(), regionName, x, y, z)) {
            player.sendMessage(Component.text("The coordinates are not within the specified WorldGuard region.", NamedTextColor.RED));
            return false;
        }

        if (service.setRegionCoordinates(regionName, x, y, z)) {
            player.sendMessage(Component.text(String.format("Coordinates set successfully for the region at (%d, %d, %d).", x, y, z), NamedTextColor.GREEN));
            return true;
        } else {
            player.sendMessage(Component.text("Failed to set coordinates for the region.", NamedTextColor.RED));
            return false;
        }
    }

    /**
     * Displays data about regions based on the specified option.
     *
     * @param player The player requesting the data.
     * @param args   The arguments passed with the command.
     * @return true if the data was fetched successfully, false otherwise.
     */
    private boolean viewData(@NotNull CommandSender player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Incorrect usage. Example: /snakeregion view [game | lobby | links | search].", NamedTextColor.RED));
            return false;
        }

        String option = args[1].toLowerCase();
        Component message = (option.equals("search") && args.length > 2) ?
                regionHelpers.fetchFormattedData(option, args[2]) :
                regionHelpers.fetchFormattedData(option);
        player.sendMessage(message);
        return true;
    }
}
