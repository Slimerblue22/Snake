package com.slimer.Game;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.slimer.GUI.GameCommandGUI;
import com.slimer.Region.Region;
import com.slimer.Region.RegionLink;
import com.slimer.Region.RegionService;
import com.slimer.Util.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles game-related commands.
 */
public class GameCommandHandler implements CommandExecutor, TabCompleter {
    private final GameManager gameManager;
    private final JavaPlugin plugin;

    /**
     * Constructs a new GameCommandHandler.
     *
     * @param gameManager The GameManager instance for managing game states.
     */
    public GameCommandHandler(GameManager gameManager, JavaPlugin plugin) {
        this.gameManager = gameManager;
        this.plugin = plugin;
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
            player.sendMessage(Component.text("Usage: /snakegame <start|stop|gui|help|highscore|leaderboard>", NamedTextColor.RED));
            return false;
        }
        String subCommand = args[0].toLowerCase();
        return switch (subCommand) {
            case "start" -> handleStartGameCommand(player);
            case "stop" -> handleStopGameCommand(player);
            case "gui" -> handleGUICommand(player);
            case "help" -> handleHelpCommand(player);
            case "color" -> args.length > 1 && handleSetColorCommand(player, args[1], plugin);
            case "highscore" -> handleHighScoreCommand(player);
            case "leaderboard" -> handleLeaderboardCommand(player);
            default -> {
                player.sendMessage(Component.text("Unknown subcommand. Use /snakegame <start|stop|gui|help|color|highscore|leaderboard>.", NamedTextColor.RED));
                yield false;
            }
        };
    }

    /**
     * Handles the tab completion for the Snake game's commands.
     * This method provides auto-complete suggestions for the game commands.
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
            if ("color".startsWith(args[0].toLowerCase())) {
                completions.add("color");
            }
            if ("highscore".startsWith(args[0].toLowerCase())) {
                completions.add("highscore");
            }
            if ("leaderboard".startsWith(args[0].toLowerCase())) {
                completions.add("leaderboard");
            } // This bit below adds tab support for the different color options.
        } else if (args.length == 2 && "color".equalsIgnoreCase(args[0])) {
            for (DyeColor dyeColor : DyeColor.values()) {
                if (dyeColor.name().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(dyeColor.name().toLowerCase());
                }
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
     * @return Always returns true to indicate successful execution.
     */
    private boolean handleGUICommand(Player player) {
        player.openInventory(GameCommandGUI.createMainMenu(player));
        return true;
    }

    /**
     * Sends a list of instructions for the snake game to the player.
     *
     * @param player The Player to whom the instructions will be sent.
     * @return True, indicating that the command was handled successfully.
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

    /**
     * Handles the command for setting the snake color for a player.
     * Validates the color input and updates the player's snake color if valid.
     *
     * @param player    The Player whose snake color is to be set.
     * @param colorName The name of the color to be set.
     * @param plugin    The JavaPlugin instance for accessing plugin-specific features.
     * @return True if the color was set successfully, false otherwise.
     */
    private boolean handleSetColorCommand(Player player, String colorName, JavaPlugin plugin) {
        // Make sure the player is not in a game
        if (gameManager.getSnakeForPlayer(player) != null) {
            player.sendMessage(Component.text("You must not be in a game to change your sheep's color.", NamedTextColor.RED));
            return false;
        }

        // Validate and set the color
        try {
            DyeColor dyeColor = DyeColor.valueOf(colorName.toUpperCase());
            PlayerData.getInstance(plugin).setSheepColor(player, dyeColor);
            player.sendMessage(Component.text("Successfully changed sheep color to " + colorName, NamedTextColor.GREEN));
            return true;
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Invalid color name. Use /snakegame color <color_name>", NamedTextColor.RED));
            return false;
        }
    }

    /**
     * Sends the player's high score to them as a chat message.
     *
     * @param player The Player whose high score is to be fetched and displayed.
     * @return True, indicating that the command was handled successfully.
     */
    private boolean handleHighScoreCommand(Player player) {
        // Fetch high score for the player.
        int highScore = PlayerData.getInstance(plugin).getHighScore(player);

        // Send the high score to the player
        player.sendMessage(Component.text("Your high score is: " + highScore, NamedTextColor.GOLD));

        return true;
    }

    /**
     * Sends the leaderboard data to the player as a series of chat messages.
     * The leaderboard shows the top 10 player names and their scores.
     *
     * @param player The Player to whom the leaderboard will be sent.
     * @return True, indicating that the command was handled successfully.
     */
    private boolean handleLeaderboardCommand(Player player) {
        // Fetch leaderboard data
        List<Map.Entry<String, Integer>> leaderboard = PlayerData.getInstance(plugin).getLeaderboard();

        // Send the leaderboard to the player
        player.sendMessage(Component.text("---- Leaderboard ----", NamedTextColor.GOLD));
        for (int i = 0; i < leaderboard.size(); i++) {
            Map.Entry<String, Integer> entry = leaderboard.get(i);
            player.sendMessage(Component.text((i + 1) + ". " + entry.getKey() + ": " + entry.getValue(), NamedTextColor.WHITE));
        }

        return true;
    }
}
