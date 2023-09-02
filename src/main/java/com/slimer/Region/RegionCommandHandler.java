package com.slimer.Region;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * The RegionCommandHandler class is responsible for handling the various commands related to regions
 * within the snake game. This includes registering, linking, setting teleport locations, listing,
 * unregistering, and unlinking regions.
 * <p>
 * It acts as a CommandExecutor, implementing the interface's method to handle the command logic.
 */
public class RegionCommandHandler implements CommandExecutor, TabCompleter {
    private final RegionService regionService;

    /**
     * Constructs a new RegionCommandHandler with the specified RegionService.
     *
     * @param regionService The RegionService responsible for managing regions.
     */
    public RegionCommandHandler(RegionService regionService) {
        this.regionService = regionService;
    }

    /**
     * Handles the /snakeadmin command and its subcommands. This method dispatches the command
     * to the appropriate handler based on the given arguments.
     * <p>
     * Supported subcommands:
     * - register: Register a new region.
     * - link: Link two regions together.
     * - addteleport: Set teleport locations for regions.
     * - list: List registered regions and links.
     * - unregister: Unregister a region.
     * - unlink: Unlink two regions.
     *
     * @param sender  The sender of the command.
     * @param command The command being executed.
     * @param label   The command label.
     * @param args    The arguments provided with the command.
     * @return true if the command was handled successfully; false otherwise.
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
            player.sendMessage(Component.text("Usage: /snakeadmin <register|unregister|link|unlink|addteleport|list>", NamedTextColor.RED));
            return false;
        }
        String subCommand = args[0].toLowerCase();
        return switch (subCommand) {
            case "register" -> handleRegisterRegionCommand(player, args);
            case "link" -> handleLinkRegionsCommand(player, args);
            case "addteleport" -> handleSetTeleportCommand(player, args);
            case "list" -> handleListRegionsCommand(player, args);
            case "unregister" -> handleUnregisterRegionCommand(player, args);
            case "unlink" -> handleUnlinkRegionsCommand(player, args);
            default -> {
                player.sendMessage(Component.text("Unknown subcommand. Use /snakeadmin <register|unregister|link|unlink|addteleport|list>.", NamedTextColor.RED));
                yield false;
            }
        };
    }

    /**
     * Handles the tab completion for the Snake admin commands.
     * This method provides auto-complete suggestions for the admin commands.
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
            if ("register".startsWith(args[0].toLowerCase())) {
                completions.add("register");
            }
            if ("unregister".startsWith(args[0].toLowerCase())) {
                completions.add("unregister");
            }
            if ("link".startsWith(args[0].toLowerCase())) {
                completions.add("link");
            }
            if ("unlink".startsWith(args[0].toLowerCase())) {
                completions.add("unlink");
            }
            if ("addteleport".startsWith(args[0].toLowerCase())) {
                completions.add("addteleport");
            }
            if ("list".startsWith(args[0].toLowerCase())) {
                completions.add("list");
            }
        }
        return completions;
    }

    /**
     * Handles the 'register' subcommand of the /snakeadmin command. This method is responsible for registering
     * a new region within the game. It takes four arguments:
     * - Region type (either 'game' or 'lobby')
     * - Region name
     * - World where the region is located
     * <p>
     * The method validates the provided arguments, ensuring the correct usage and that the region type is valid,
     * the world exists, and the region is not already registered. If all conditions are met, the region is registered
     * using the provided details.
     *
     * @param player The player who executed the command.
     * @param args   The arguments provided with the subcommand.
     * @return true if the region was registered successfully; false otherwise, including if the command usage was incorrect.
     */
    private boolean handleRegisterRegionCommand(Player player, String[] args) {
        // Validate the number of arguments and correct usage
        if (args.length != 4) {
            player.sendMessage(Component.text("Incorrect usage. Usage: /snakeadmin register [region type; game or lobby] [region name] [world region is in]", NamedTextColor.RED));
            return false;
        }

        // Determine the region type
        String regionTypeStr = args[1].toLowerCase();
        Region.RegionType regionType;
        if (regionTypeStr.equals("game")) {
            regionType = Region.RegionType.GAME;
        } else if (regionTypeStr.equals("lobby")) {
            regionType = Region.RegionType.LOBBY;
        } else {
            player.sendMessage(Component.text("Invalid region type. Must be 'game' or 'lobby'.", NamedTextColor.RED));
            return false;
        }

        // Retrieve and validate region details
        String regionName = args[2];
        String worldName = args[3];
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(Component.text("World '" + worldName + "' not found.", NamedTextColor.RED));
            return false;
        }
        if (regionService.getRegion(regionName) != null) {
            player.sendMessage(Component.text("Region '" + regionName + "' is already registered.", NamedTextColor.RED));
            return false;
        }

        // Register the region if it exists in WorldGuard
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(world));
        if (Objects.requireNonNull(regions).getRegion(regionName) != null) {
            regionService.registerRegion(regionName, regionType, world);
            player.sendMessage(Component.text("Region '" + regionName + "' of type '" + regionTypeStr + "' has been registered successfully in world '" + world.getName() + "'.", NamedTextColor.GREEN));
            return true;
        }

        // Handle case where region is not found
        player.sendMessage(Component.text("Region '" + regionName + "' not found in world '" + worldName + "'.", NamedTextColor.RED));
        return false;
    }

    /**
     * Handles the 'link' subcommand of the /snakeadmin command. This method is responsible for linking
     * a lobby region with a game region, allowing players to transition between these two regions.
     * <p>
     * The method expects five arguments:
     * - 'game' or 'lobby' indicating the type of the first region
     * - The name of the first region
     * - 'game' or 'lobby' indicating the type of the second region
     * - The name of the second region
     * <p>
     * The method validates that both a lobby and game region are specified, that both regions exist,
     * that their types match the specified types, and that neither region is already linked to another
     * region of the opposite type. If all conditions are met, the regions are linked.
     *
     * @param player The player who executed the command.
     * @param args   The arguments provided with the subcommand.
     * @return true if the regions were linked successfully; false otherwise, including if the command usage was incorrect.
     */
    private boolean handleLinkRegionsCommand(Player player, String[] args) {
        // Validate the number of arguments and correct usage
        if (args.length != 5) {
            player.sendMessage(Component.text("Incorrect usage. Usage: /snakeadmin link [region type; game or lobby] [region name] [region type; game or lobby] [region name]", NamedTextColor.RED));
            return false;
        }

        // Determine the lobby and game region names
        String lobbyRegionName = null, gameRegionName = null;
        for (int i = 1; i < 5; i += 2) {
            String regionTypeStr = args[i].toLowerCase();
            String regionName = args[i + 1];
            if (regionTypeStr.equals("game")) {
                gameRegionName = regionName;
            } else if (regionTypeStr.equals("lobby")) {
                lobbyRegionName = regionName;
            } else {
                player.sendMessage(Component.text("Invalid region type. Must be 'game' or 'lobby'.", NamedTextColor.RED));
                return false;
            }
        }

        // Validate that both lobby and game regions are specified
        if (lobbyRegionName == null || gameRegionName == null) {
            player.sendMessage(Component.text("Both lobby and game regions must be specified.", NamedTextColor.RED));
            return false;
        }

        // Validate that both regions exist and are of the correct type
        Region lobbyRegion = regionService.getRegion(lobbyRegionName);
        Region gameRegion = regionService.getRegion(gameRegionName);
        if (lobbyRegion == null || gameRegion == null) {
            player.sendMessage(Component.text("One or both regions not found.", NamedTextColor.RED));
            return false;
        }
        if (lobbyRegion.getType() != Region.RegionType.LOBBY || gameRegion.getType() != Region.RegionType.GAME) {
            player.sendMessage(Component.text("Region types do not match. Lobby region must be of type 'LOBBY' and game region must be of type 'GAME'.", NamedTextColor.RED));
            return false;
        }

        // Validate that neither region is already linked
        RegionLink existingLobbyLink = regionService.getRegionLink(lobbyRegionName, Region.RegionType.LOBBY);
        RegionLink existingGameLink = regionService.getRegionLink(gameRegionName, Region.RegionType.GAME);
        if (existingLobbyLink != null) {
            player.sendMessage(Component.text("Lobby region '" + lobbyRegionName + "' is already linked to game region '" + existingLobbyLink.getGameRegionName() + "'.", NamedTextColor.RED));
            return false;
        }
        if (existingGameLink != null) {
            player.sendMessage(Component.text("Game region '" + gameRegionName + "' is already linked to lobby region '" + existingGameLink.getLobbyRegionName() + "'.", NamedTextColor.RED));
            return false;
        }

        // Link the regions
        regionService.linkRegions(lobbyRegionName, gameRegionName);
        player.sendMessage(Component.text("Lobby region '" + lobbyRegionName + "' has been linked with game region '" + gameRegionName + "'.", NamedTextColor.GREEN));
        return true;
    }

    /**
     * Handles the 'addteleport' subcommand of the /snakeadmin command. This method is responsible for
     * setting a teleport location inside a specified game or lobby region. This teleport location
     * serves as a point where players can be teleported to within that region.
     * <p>
     * The method expects three arguments:
     * - 'game' or 'lobby' indicating the type of the region
     * - The name of the region
     * <p>
     * The command must be executed inside the specified region, and the player's current location will
     * be used as the teleport location. The method validates the existence of the region, the location
     * of the command execution, and the existence of a link for the region if needed. If all conditions
     * are met, the teleport location is set.
     *
     * @param player The player who executed the command.
     * @param args   The arguments provided with the subcommand.
     * @return true if the teleport location was set successfully; false otherwise, including if the command usage was incorrect.
     */
    private boolean handleSetTeleportCommand(Player player, String[] args) {
        // Validate the number of arguments and correct usage
        if (args.length != 3) {
            player.sendMessage(Component.text("Incorrect usage. Usage: /snakeadmin addteleport [region type; game or lobby] [region name]", NamedTextColor.RED));
            return false;
        }

        // Determine the region type
        String regionTypeStr = args[1].toLowerCase();
        boolean isGame = regionTypeStr.equals("game");
        if (!isGame && !regionTypeStr.equals("lobby")) {
            player.sendMessage(Component.text("Invalid region type. Must be 'game' or 'lobby'.", NamedTextColor.RED));
            return false;
        }

        // Validate the region name and existence
        String regionName = args[2];
        World world = player.getWorld();
        ProtectedRegion wgRegion = regionService.getWorldGuardRegion(regionName, world);
        if (wgRegion == null) {
            player.sendMessage(Component.text("WorldGuard region not found for '" + regionName + "'.", NamedTextColor.RED));
            return false;
        }

        // Determine the teleport location and validate that it's inside the specified region
        Location teleportLocation = player.getLocation();
        if (!regionService.isLocationInRegion(teleportLocation, wgRegion)) {
            player.sendMessage(Component.text("Teleport location must be inside the region '" + regionName + "'.", NamedTextColor.RED));
            return false;
        }

        // Get the region type and validate the existence of a link for the region
        Region.RegionType regionType = isGame ? Region.RegionType.GAME : Region.RegionType.LOBBY;
        RegionLink link = regionService.getRegionLink(regionName, regionType);
        if (link == null) {
            player.sendMessage(Component.text("Region link not found for region '" + regionName + "'.", NamedTextColor.RED));
            return false;
        }

        // Set the teleport location and save it
        if (isGame) {
            link.setGameTeleportLocation(teleportLocation);
        } else {
            link.setLobbyTeleportLocation(teleportLocation);
        }
        regionService.saveRegionLinkToFile(link);

        // Confirm success
        player.sendMessage(Component.text("Teleport location for '" + regionTypeStr + "' region '" + regionName + "' has been set successfully.", NamedTextColor.GREEN));
        return true;
    }

    /**
     * Handles the 'list' subcommand of the /snakeadmin command. This method is responsible for
     * displaying the list of registered game regions, lobby regions, linked regions, or all of them,
     * depending on the option provided as an argument.
     * <p>
     * The method expects two arguments:
     * - The option to list: 'lobby', 'game', 'links', or 'all'
     * <p>
     * The method fetches the regions and region links, sorts them, and builds the message using
     * helper methods to create the content for each type of list.
     *
     * @param player The player who executed the command.
     * @param args   The arguments provided with the subcommand.
     * @return true if the list was displayed successfully; false otherwise, including if the command usage was incorrect.
     */
    private boolean handleListRegionsCommand(Player player, String[] args) {
        // Check for correct argument count
        if (args.length != 2) {
            player.sendMessage(Component.text("Incorrect usage. Usage: /snakeadmin list [lobby, game, links, or all]", NamedTextColor.RED));
            return false;
        }

        // Extract option and validate
        String option = args[1].toLowerCase();
        if (!option.equals("lobby") && !option.equals("game") && !option.equals("links") && !option.equals("all")) {
            player.sendMessage(Component.text("Invalid option. Use lobby, game, links, or all.", NamedTextColor.RED));
            return false;
        }

        // Retrieve regions and links, and sort them
        Map<String, Region> regions = regionService.getAllRegions();
        Map<String, RegionLink> regionLinks = regionService.getAllRegionLinks();
        List<Region> sortedRegions = new ArrayList<>(regions.values());
        sortedRegions.sort(Comparator.comparing(Region::getName));
        List<RegionLink> sortedRegionLinks = new ArrayList<>(regionLinks.values());
        sortedRegionLinks.sort(Comparator.comparing(RegionLink::getLobbyRegionName));

        // Prepare components to be displayed
        Component separator = Component.text("\n");
        List<Component> allComponents = new ArrayList<>();
        if (option.equals("lobby") || option.equals("all")) {
            allComponents.add(Component.text("Registered Lobbies:", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));
            allComponents.addAll(buildRegionContent(sortedRegions, Region.RegionType.LOBBY));
        }
        if (option.equals("game") || option.equals("all")) {
            allComponents.add(Component.text("Registered Games:", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));
            allComponents.addAll(buildRegionContent(sortedRegions, Region.RegionType.GAME));
        }
        if (option.equals("links") || option.equals("all")) {
            allComponents.add(Component.text("Linked Regions:", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));
            allComponents.addAll(buildLinkContent(sortedRegionLinks));
        }

        // Join components and send the message to the player
        JoinConfiguration joinConfig = JoinConfiguration.builder()
                .separator(separator)
                .build();
        player.sendMessage(Component.join(joinConfig, allComponents));
        return true;
    }

    /**
     * Handles the command to unregister a region. The region must be specified and not linked to any other regions.
     *
     * @param player The player who executed the command.
     * @param args   The arguments passed with the command, expecting exactly one argument representing the region name.
     * @return true if the region is successfully unregistered, false otherwise.
     */
    private boolean handleUnregisterRegionCommand(Player player, String[] args) {
        // Check for correct argument count
        if (args.length != 2) {
            player.sendMessage(Component.text("Incorrect usage. Usage: /snakeadmin unregister [region name]", NamedTextColor.RED));
            return false;
        }

        // Retrieve the region name and region object
        String regionName = args[1];
        Region region = regionService.getRegion(regionName);

        // Check if the region is registered
        if (region == null) {
            player.sendMessage(Component.text("Region '" + regionName + "' is not registered.", NamedTextColor.RED));
            return false;
        }

        // Check if the region is linked to another region
        if (regionService.getRegionLink(regionName, Region.RegionType.GAME) != null ||
                regionService.getRegionLink(regionName, Region.RegionType.LOBBY) != null) {
            player.sendMessage(Component.text("Region '" + regionName + "' is linked to another region. Unlink the regions first.", NamedTextColor.RED));
            return false;
        }

        // Unregister the region and send a success message
        regionService.unregisterRegion(regionName);
        player.sendMessage(Component.text("Region '" + regionName + "' has been unregistered successfully.", NamedTextColor.GREEN));
        return true;
    }

    /**
     * Handles the command to unlink regions, specifically a game region and a lobby region.
     * Both regions must be specified, and they must be currently linked to each other.
     *
     * @param player The player who executed the command.
     * @param args   The arguments passed with the command, expecting exactly four arguments
     *               representing the region types and names in the pattern [type1, name1, type2, name2].
     * @return true if the regions are successfully unlinked, false otherwise.
     */
    private boolean handleUnlinkRegionsCommand(Player player, String[] args) {
        // Check for correct argument count
        if (args.length != 5) {
            player.sendMessage(Component.text("Incorrect usage. Usage: /snakeadmin unlink [region type; game or lobby] [region name] [region type; game or lobby] [region name]", NamedTextColor.RED));
            return false;
        }

        // Extract region names
        String lobbyRegionName = null, gameRegionName = null;
        for (int i = 1; i < 5; i += 2) {
            String regionTypeStr = args[i].toLowerCase();
            String regionName = args[i + 1];
            if (regionTypeStr.equals("game")) {
                gameRegionName = regionName;
            } else if (regionTypeStr.equals("lobby")) {
                lobbyRegionName = regionName;
            } else {
                player.sendMessage(Component.text("Invalid region type. Must be 'game' or 'lobby'.", NamedTextColor.RED));
                return false;
            }
        }

        // Validate both regions are specified
        if (lobbyRegionName == null || gameRegionName == null) {
            player.sendMessage(Component.text("Both lobby and game regions must be specified.", NamedTextColor.RED));
            return false;
        }

        // Retrieve link and check if regions are linked
        RegionLink link = regionService.getRegionLink(lobbyRegionName, Region.RegionType.LOBBY);
        if (link == null || !gameRegionName.equals(link.getGameRegionName())) {
            player.sendMessage(Component.text("Regions '" + lobbyRegionName + "' and '" + gameRegionName + "' are not linked.", NamedTextColor.RED));
            return false;
        }

        // Unlink the regions and send a success message
        regionService.unlinkRegions(lobbyRegionName, gameRegionName);
        player.sendMessage(Component.text("Lobby region '" + lobbyRegionName + "' has been unlinked from game region '" + gameRegionName + "'.", NamedTextColor.GREEN));
        return true;
    }

    /**
     * Builds a list of text components representing the specified type of regions (either game or lobby).
     * This method is used to create the content for the 'list' subcommand, specifically for listing
     * registered game or lobby regions.
     *
     * @param sortedRegions The sorted list of regions to be displayed.
     * @param type          The type of regions to be displayed (either Region.RegionType.GAME or Region.RegionType.LOBBY).
     * @return A list of text components representing the regions of the specified type.
     */
    private List<Component> buildRegionContent(List<Region> sortedRegions, Region.RegionType type) {
        List<Component> components = new ArrayList<>();
        for (Region region : sortedRegions) {
            if (region.getType() == type) {
                components.add(Component.text("- " + region.getName(), NamedTextColor.YELLOW)
                        .append(Component.text(" (" + region.getType() + ")", NamedTextColor.AQUA))
                        .append(Component.text(" in " + region.getWorldName(), NamedTextColor.GRAY)));
            }
        }
        return components;
    }

    /**
     * Builds a list of text components representing the linked regions.
     * This method is used to create the content for the 'list' subcommand, specifically for listing
     * the links between lobby and game regions.
     *
     * @param sortedRegionLinks The sorted list of region links to be displayed.
     * @return A list of text components representing the linked regions.
     */
    private List<Component> buildLinkContent(List<RegionLink> sortedRegionLinks) {
        List<Component> components = new ArrayList<>();
        for (RegionLink link : sortedRegionLinks) {
            components.add(Component.text("- " + link.getLobbyRegionName(), NamedTextColor.YELLOW)
                    .append(Component.text(" <-> ", NamedTextColor.GRAY))
                    .append(Component.text(link.getGameRegionName(), NamedTextColor.YELLOW))
                    .append(Component.text("\n[Lobby TP: " + locationToString(link.getLobbyTeleportLocation()) + ", Game TP: " + locationToString(link.getGameTeleportLocation()) + "]", NamedTextColor.GRAY)));
        }
        return components;
    }

    /**
     * Converts a Location object to a string representation. The string representation includes the
     * block coordinates (X, Y, Z) of the location. If the location is null, an empty string is returned.
     *
     * @param location The location to be converted to a string.
     * @return The string representation of the location or an empty string if the location is null.
     */
    private String locationToString(Location location) {
        if (location == null) {
            return "";
        }
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }
}