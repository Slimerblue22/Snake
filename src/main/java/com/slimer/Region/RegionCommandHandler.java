package com.slimer.Region;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Handles the command logic for the Snake game regions.
 */
public class RegionCommandHandler implements CommandExecutor {
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be run by a player.", NamedTextColor.RED));
            return false;
        }
        if (!player.hasPermission("snake.admin")) {
            player.sendMessage(Component.text("You don't have permission to run this command.", NamedTextColor.RED));
            return false;
        }
        if (args.length == 0) {
            sender.sendMessage("Please enter a sub command.");
            return false;
        }
        String subCommand = args[0].toLowerCase();
        return switch (subCommand) {
            case "register" -> registerRegion(player, args);
            case "unregister" -> unregisterRegion(player, args);
            case "link" -> linkRegions(player, args);
            case "unlink" -> unlinkRegions(player, args);
            case "addtp" -> addTP(player, args);
            case "view" -> viewData(player, args);
            default -> {
                player.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
                yield false;
            }
        };
    }

    /**
     * Registers a new region in the game.
     *
     * @param player The player executing the command.
     * @param args   The arguments passed with the command.
     * @return true if the region was registered successfully, false otherwise.
     */
    private boolean registerRegion(Player player, String[] args) {
        if (args.length != 4) {
            player.sendMessage("Incorrect usage. Example: /snakeregion register [region type; game or lobby] [region name] [world region is in]");
            return false;
        }
        String regionType = args[1].toLowerCase();
        String regionName = args[2].toLowerCase();
        String worldName = args[3].toLowerCase();
        if (!"lobby".equals(regionType) && !"game".equals(regionType)) {
            player.sendMessage("Region type must be either 'lobby' or 'game'.");
            return false;
        }
        if (!wgHelpers.doesWGRegionExist(worldName, regionName)) {
            player.sendMessage(String.format("Region %s is not registered in WorldGuard for world %s.", regionName, worldName));
            return false;
        }
        if (regionHelpers.isRegionRegistered(regionName)) {
            player.sendMessage("Region is already registered.");
            return false;
        }
        if (service.registerNewRegion(regionType, regionName, worldName)) {
            player.sendMessage("Region registered successfully.");
            return true;
        } else {
            player.sendMessage("Failed to register region.");
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
    private boolean unregisterRegion(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage("Incorrect usage. Example: /snakeregion unregister [region name].");
            return false;
        }
        String regionName = args[1].toLowerCase();
        if (!regionHelpers.isRegionRegistered(regionName)) {
            player.sendMessage("Region is not registered.");
            return false;
        }
        if (regionHelpers.getLinkID(regionName) != null) {
            player.sendMessage("The region is linked to another region. Unlink it first before unregistering.");
            return false;
        }
        if (service.unregisterRegion(regionName)) {
            player.sendMessage("Region unregistered successfully.");
            return true;
        } else {
            player.sendMessage("Failed to unregister region.");
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
    private boolean linkRegions(Player player, String[] args) {
        if (args.length != 3) {
            player.sendMessage("Incorrect usage. Example: /snakeregion link [region1 name] [region2 name].");
            return false;
        }
        String regionName1 = args[1].toLowerCase();
        String regionName2 = args[2].toLowerCase();
        if (!regionHelpers.isRegionRegistered(regionName1) || !regionHelpers.isRegionRegistered(regionName2)) {
            player.sendMessage("One or both regions are not registered.");
            return false;
        }
        String type1 = regionHelpers.getRegionType(regionName1);
        String type2 = regionHelpers.getRegionType(regionName2);
        if (!((type1.equals("game") && type2.equals("lobby")) || (type1.equals("lobby") && type2.equals("game")))) {
            player.sendMessage("You must link a game region with a lobby region.");
            return false;
        }
        if (regionHelpers.isRegionLinked(regionName1) || regionHelpers.isRegionLinked(regionName2)) {
            player.sendMessage("One or both regions are already linked to another region.");
            return false;
        }
        if (service.linkRegions(regionName1, regionName2)) {
            player.sendMessage("Regions linked successfully.");
            return true;
        } else {
            player.sendMessage("Failed to link regions.");
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
    private boolean unlinkRegions(Player player, String[] args) {
        if (args.length != 3) {
            player.sendMessage("Incorrect usage. Example: /snakeregion unlink [region1 name] [region2 name].");
            return false;
        }
        String regionName1 = args[1].toLowerCase();
        String regionName2 = args[2].toLowerCase();
        if (regionName1.equals(regionName2)) {
            player.sendMessage("Cannot unlink the same region.");
            return false;
        }
        if (!regionHelpers.isRegionRegistered(regionName1) || !regionHelpers.isRegionRegistered(regionName2)) {
            player.sendMessage("One or both regions are not registered.");
            return false;
        }
        Integer linkID1 = regionHelpers.getLinkID(regionName1);
        Integer linkID2 = regionHelpers.getLinkID(regionName2);
        if (linkID1 == null || linkID2 == null) {
            player.sendMessage("One or both regions are not linked.");
            return false;
        }
        if (!linkID1.equals(linkID2)) {
            player.sendMessage("The provided regions are not linked together.");
            return false;
        }
        if (service.unlinkRegions(regionName1, regionName2)) {
            player.sendMessage("Regions unlinked successfully.");
            return true;
        } else {
            player.sendMessage("Failed to unlink regions.");
            return false;
        }
    }

    /**
     * Adds or updates the TP (teleport) coordinates of a region.
     *
     * @param player The player executing the command.
     * @param args   The arguments passed with the command.
     * @return true if the TP coordinates were set successfully, false otherwise.
     */
    private boolean addTP(Player player, String[] args) {
        if (args.length != 2 && args.length != 5) {
            player.sendMessage("Incorrect usage. Example: /snakeregion addtp [region name] or /snakedev addtp [region name] [x] [y] [z].");
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
                player.sendMessage("Invalid coordinates format. Please ensure x, y, and z are valid numbers.");
                return false;
            }
        } else {
            Location loc = player.getLocation();
            x = loc.getBlockX();
            y = loc.getBlockY();
            z = loc.getBlockZ();
        }
        if (!wgHelpers.areCoordinatesInWGRegion(player.getWorld().getName(), regionName, x, y, z)) {
            player.sendMessage("The coordinates are not within the specified WorldGuard region.");
            return false;
        }
        if (service.setRegionCoordinates(regionName, x, y, z)) {
            player.sendMessage(String.format("Coordinates set successfully for the region at (%d, %d, %d).", x, y, z));
            return true;
        } else {
            player.sendMessage("Failed to set coordinates for the region.");
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
    private boolean viewData(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("Incorrect usage. Example: /snakeregion view [game | lobby | links | search].");
            return false;
        }
        String option = args[1].toLowerCase();
        String message = (option.equals("search") && args.length > 2) ?
                regionHelpers.fetchFormattedData(option, args[2]) :
                regionHelpers.fetchFormattedData(option);
        player.sendMessage(message);
        return true;
    }
}
