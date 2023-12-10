package com.slimer.Game;

import com.slimer.GUI.GuiManager;
import com.slimer.Region.RegionHelpers;
import com.slimer.Region.WGHelpers;
import com.slimer.Util.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.DyeColor;
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
import java.util.Map;

/**
 * Handles player commands related to the Snake game, including starting and stopping games, setting snake colors,
 * displaying the game GUI, and providing game instructions.
 * <p>
 * Last updated: V2.1.0
 * @author Slimerblue22
 */
public class GameCommandHandler implements CommandExecutor, TabCompleter {
    private final GameManager gameManager;

    /**
     * Constructs a new GameCommandHandler.
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

        // Open the GUI if no arguments provided
        if (args.length == 0) {
            handleGUICommand(player);
            return false;
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "start" -> handleStartGameCommand(player);
            case "stop" -> handleStopGameCommand(player);
            case "gui" -> handleGUICommand(player);
            case "help" -> handleHelpCommand(player);
            case "color" -> handleSetColorCommand(player, args);
            case "highscore" -> handleHighScoreCommand(player);
            case "leaderboard" -> handleLeaderboardCommand(player, args);
            case "music" -> handleMusicToggleCommand(player);
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
        String[] commands = {"start", "stop", "gui", "help", "color", "highscore", "leaderboard", "music"};
        for (String cmd : commands) {
            player.sendMessage(Component.text("/snakegame " + cmd, NamedTextColor.GRAY));
        }
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
            String[] subCommands = {"start", "stop", "gui", "help", "color", "highscore", "leaderboard", "music"};
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
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
        WGHelpers wgHelpers = WGHelpers.getInstance();
        RegionHelpers regionHelpers = RegionHelpers.getInstance();

        String currentLobbyRegion = wgHelpers.getPlayerCurrentRegion(player);
        boolean isRegistered = (currentLobbyRegion != null) && regionHelpers.isRegionRegistered(currentLobbyRegion);
        String regionType = isRegistered ? regionHelpers.getRegionType(currentLobbyRegion) : null;

        if (!"lobby".equals(regionType)) {
            player.sendMessage(Component.text("You must be within a lobby region to start the game.", NamedTextColor.RED));
            return false;
        }

        boolean isLinked = regionHelpers.isRegionLinked(currentLobbyRegion);
        String currentGameRegion = regionHelpers.getLinkedRegion(currentLobbyRegion);

        if (!isLinked || currentGameRegion == null) {
            player.sendMessage(Component.text("The lobby you are in is not properly linked to a game region. You cannot start the game.", NamedTextColor.RED));
            return false;
        }

        World gameWorld = regionHelpers.getRegionWorld(currentGameRegion);
        World lobbyWorld = regionHelpers.getRegionWorld(currentLobbyRegion);

        Location gameTeleportLocation = regionHelpers.getRegionTeleportLocation(currentGameRegion, gameWorld);
        Location lobbyTeleportLocation = regionHelpers.getRegionTeleportLocation(currentLobbyRegion, lobbyWorld);

        if (gameTeleportLocation == null || lobbyTeleportLocation == null) {
            player.sendMessage(Component.text("Could not find the teleport location for the game or lobby region.", NamedTextColor.RED));
            return false;
        }

        gameManager.startGame(player, gameTeleportLocation, lobbyTeleportLocation);
        return true;
    }

    /**
     * Handles the "stop" subcommand.
     *
     * @param player The player issuing the command.
     * @return true if the game or queue was stopped for the player, false otherwise.
     */
    private boolean handleStopGameCommand(Player player) {
        gameManager.stopGame(player);
        return true;
    }

    /**
     * Handles the "gui" command by displaying the main menu GUI for the Snake game to the player.
     *
     * @param player The player executing the "gui" command.
     * @return Always returns true, indicating the command's successful execution.
     */
    private boolean handleGUICommand(Player player) {
        GuiManager.getInstance().openMainMenu(player);
        return true;
    }

    /**
     * Sends a list of instructions for the snake game to the player.
     *
     * @param player The Player to whom the instructions will be sent.
     * @return True, indicating that the command was handled successfully.
     */
    private boolean handleHelpCommand(Player player) {
        Component instructions = Component.text()
                .append(Component.text("------ Snake Game Instructions ------", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("- The objective is to eat as many apples as possible without running into yourself or the walls.", NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.text("- Hold forward to move in the direction you are looking.", NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.text("- Eating an apple will make your snake longer.", NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.text("- The game ends if you run into yourself or the game boundaries.", NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.text("- Your score increases with each apple you eat.", NamedTextColor.WHITE))
                .build();

        player.sendMessage(instructions);
        return true;
    }

    /**
     * Handles the command for setting the snake color for a player.
     * Validates the color input and updates the player's snake color if valid.
     *
     * @param player The Player whose snake color is to be set.
     * @param args   The name of the color to be set.
     * @return True if the color was set successfully, false otherwise.
     */
    private boolean handleSetColorCommand(Player player, String[] args) {
        // Validate the number of arguments and correct usage
        if (args.length != 2) {
            player.sendMessage(Component.text("Please specify a color name. Use /snakegame color <color_name>.", NamedTextColor.RED));
            return false;
        }
        String colorName = args[1];

        // Validate and set the color
        try {
            DyeColor dyeColor = DyeColor.valueOf(colorName.toUpperCase());
            PlayerData.getInstance().setSheepColor(player, dyeColor);
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
        int highScore = PlayerData.getInstance().getHighScore(player);
        player.sendMessage(Component.text("Your high score is: " + highScore, NamedTextColor.GOLD));
        return true;
    }

    /**
     * Sends the leaderboard data to the player as a series of chat messages.
     * The leaderboard shows player names and their scores based on the provided page number.
     *
     * @param player The Player to whom the leaderboard will be sent.
     * @param args   The arguments provided with the command.
     * @return True, indicating that the command was handled successfully.
     */
    private boolean handleLeaderboardCommand(Player player, String[] args) {
        int page = 1;  // Default to first page
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
                if (page < 1) {
                    player.sendMessage(Component.text("Invalid page number. Please provide a positive page number.", NamedTextColor.RED));
                    return false;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid page number. Please provide a numeric page number.", NamedTextColor.RED));
                return false;
            }
        }

        // Retrieve leaderboard data for the specified page
        List<Map.Entry<String, Integer>> leaderboard = PlayerData.getInstance().getPaginatedLeaderboard(page);

        // Inform the player if there's no data for the given page
        if (leaderboard.isEmpty()) {
            player.sendMessage(Component.text("There are no entries for this page.", NamedTextColor.RED));
            return true;
        }

        // Send the leaderboard data to the player
        player.sendMessage(Component.text("---- Leaderboard (Page " + page + ") ----", NamedTextColor.GOLD));
        for (int i = 0; i < leaderboard.size(); i++) {
            Map.Entry<String, Integer> entry = leaderboard.get(i);
            player.sendMessage(Component.text(((page - 1) * 10 + i + 1) + ". " + entry.getKey() + ": " + entry.getValue(), NamedTextColor.GRAY));
        }
        return true;
    }

    /**
     * Handles the "music" subcommand.
     *
     * @param player The player issuing the command.
     * @return true if the music preference is successfully toggled, otherwise false.
     */
    private boolean handleMusicToggleCommand(Player player) {
        // Retrieve the player's current music preference
        boolean currentPreference = PlayerData.getInstance().getMusicToggleState(player);

        // Update the music preference
        PlayerData.getInstance().setMusicToggleState(player, !currentPreference);

        // Notify the player about the change
        String message = currentPreference ? "Music has been disabled for your sessions." : "Music has been enabled for your sessions.";
        NamedTextColor color = currentPreference ? NamedTextColor.RED : NamedTextColor.GREEN;
        player.sendMessage(Component.text(message, color));

        return true;
    }
}
