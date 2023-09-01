package com.slimer.Game;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.slimer.Region.Region;
import com.slimer.Region.RegionLink;
import com.slimer.Region.RegionService;
import com.slimer.GUI.GameCommandGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles game-related commands.
 */
public class GameCommandHandler implements CommandExecutor, TabCompleter {
    private final GameManager gameManager;

    /**
     * Constructs a new GameCommandHandler.
     *
     * @param gameManager The GameManager instance for managing game states.
     */
    public GameCommandHandler(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Executes the given command, returning its success.
     *
     * @param sender  Source of the command.
     * @param command Command which was executed.
     * @param label   Alias of the command which was used.
     * @param args    Passed command arguments.
     * @return true if a valid command, otherwise false.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be run by a player.", NamedTextColor.RED));
            return false;
        }
        if (!player.hasPermission("snake.play")) {
            player.sendMessage(Component.text("You don't have permission to run this command.", NamedTextColor.RED));
            return false;
        }
        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /snakegame <start|stop|gui|help>", NamedTextColor.RED));
            return false;
        }
        String subCommand = args[0].toLowerCase();
        return switch (subCommand) {
            case "start" -> handleStartGameCommand(player);
            case "stop" -> handleStopGameCommand(player);
            case "gui" -> handleGUICommand(player);
            case "help" -> handleHelpCommand(player);
            default -> {
                player.sendMessage(Component.text("Unknown subcommand. Use /snakegame <start|stop|gui|help>.", NamedTextColor.RED));
                yield false;
            }
        };
    }

    /**
     * Handles the tab completion for the Snake game's commands.
     * This method provides auto-complete suggestions for the game commands.
     *
     * @param sender   The sender of the command, can be a player or the console.
     * @param command  The command that was executed.
     * @param alias    The alias that the sender used to trigger the command.
     * @param args     The arguments that were provided with the command.
     * @return         A list of possible completions for a command argument.
     */
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if ("start".startsWith(args[0].toLowerCase())) {
                completions.add("start");
            }
            if ("stop".startsWith(args[0].toLowerCase())) {
                completions.add("stop");
            }
            if ("gui".startsWith(args[0].toLowerCase())) {
                completions.add("gui");
            }
            if ("help".startsWith(args[0].toLowerCase())) {
                completions.add("help");
            }
        }
        return completions;
    }

    /**
     * Handles the "start" subcommand.
     *
     * @param player The player issuing the command.
     * @return true if the game is successfully started, otherwise false.
     */
    private boolean handleStartGameCommand(Player player) {
        RegionService regionService = RegionService.getInstance();

        // Loop through all available lobby regions
        for (Region region : regionService.getAllRegions().values()) {
            if (region.getType() == Region.RegionType.LOBBY) {
                String worldName = region.getWorldName();
                World world = Bukkit.getWorld(worldName);

                // Check if player is inside a valid lobby region
                if (world != null) {
                    ProtectedRegion worldGuardRegion = regionService.getWorldGuardRegion(region.getName(), world);
                    if (worldGuardRegion != null && regionService.isLocationInRegion(player.getLocation(), worldGuardRegion)) {
                        RegionLink regionLink = regionService.getRegionLink(region.getName(), Region.RegionType.LOBBY);

                        // Check if the lobby has a linked game region
                        if (regionLink != null) {
                            Location gameTeleportLocation = regionLink.getGameTeleportLocation();
                            Location lobbyTeleportLocation = regionLink.getLobbyTeleportLocation();

                            // Start the game
                            if (gameTeleportLocation != null) {
                                player.teleport(gameTeleportLocation);
                                gameManager.startGame(player, gameTeleportLocation, lobbyTeleportLocation);
                                player.sendMessage(Component.text("Starting the snake game...", NamedTextColor.GREEN));
                                return true;
                            }
                        }
                        player.sendMessage(Component.text("No linked game region found. Unable to start the game.", NamedTextColor.RED));
                        return false;
                    }
                }
            }
        }
        player.sendMessage(Component.text("You must be within a lobby region to start the game.", NamedTextColor.RED));
        return false;
    }

    /**
     * Handles the "stop" subcommand.
     *
     * @param player The player issuing the command.
     * @return true indicating the game has been stopped.
     */
    private boolean handleStopGameCommand(Player player) {
        gameManager.stopGame(player);
        player.sendMessage(Component.text("Stopping the snake game...", NamedTextColor.RED));
        return true;
    }

    /**
     * Handles the "gui" command by opening the main menu of the Snake game.
     * This method is responsible for triggering the GUI that allows players to interact with the Snake game.
     *
     * @param player The player who issued the "gui" command.
     * @return       Always returns true to indicate successful execution.
     */
    private boolean handleGUICommand(Player player) {
        player.openInventory(GameCommandGUI.createMainMenu());
        return true;
    }

    /**
     * PLACEHOLDER
     */
    private boolean handleHelpCommand(Player player) {
        player.sendMessage(Component.text("------ Snake Game Instructions ------", NamedTextColor.GOLD));
        player.sendMessage(Component.text("- The objective is to eat as many apples as possible without running into yourself or the walls.", NamedTextColor.WHITE));
        player.sendMessage(Component.text("- Hold forward to move in the direction you are looking.", NamedTextColor.WHITE));
        player.sendMessage(Component.text("- Eating an apple will make your snake longer.", NamedTextColor.WHITE));
        player.sendMessage(Component.text("- The game ends if you run into yourself or the game boundaries.", NamedTextColor.WHITE));
        player.sendMessage(Component.text("- Your score increases with each apple you eat.", NamedTextColor.WHITE));
        return true;
    }
}
