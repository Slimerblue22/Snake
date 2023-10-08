package com.slimer.Game;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.slimer.GUI.GameCommandGUI;
import com.slimer.Main.Main;
import com.slimer.Region.Region;
import com.slimer.Region.RegionLink;
import com.slimer.Region.RegionService;
import com.slimer.Util.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;
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
        if (args.length == 0) { // If the user only types `/snakegame` or `/sg` just open the GUI for them
            handleGUICommand(player, plugin);
            return false;
        }
        String subCommand = args[0].toLowerCase();
        return switch (subCommand) {
            case "start" -> handleStartGameCommand(player, args);
            case "stop" -> handleStopGameCommand(player);
            case "gui" -> handleGUICommand(player, plugin);
            case "help" -> handleHelpCommand(player);
            case "color" -> handleSetColorCommand(player, args, plugin);
            case "highscore" -> handleHighScoreCommand(player);
            case "leaderboard" -> handleLeaderboardCommand(player, args);
            case "music" -> handleMusicToggleCommand(player);
            default -> {
                player.sendMessage(Component.text("Unknown subcommand. Use one of the following:", NamedTextColor.RED));
                player.sendMessage(Component.text("/snakegame start", NamedTextColor.GRAY));
                player.sendMessage(Component.text("/snakegame stop", NamedTextColor.GRAY));
                player.sendMessage(Component.text("/snakegame gui", NamedTextColor.GRAY));
                player.sendMessage(Component.text("/snakegame help", NamedTextColor.GRAY));
                player.sendMessage(Component.text("/snakegame color", NamedTextColor.GRAY));
                player.sendMessage(Component.text("/snakegame highscore", NamedTextColor.GRAY));
                player.sendMessage(Component.text("/snakegame leaderboard", NamedTextColor.GRAY));
                player.sendMessage(Component.text("/snakegame music", NamedTextColor.GRAY));
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
            if ("music".startsWith(args[0].toLowerCase())) {
                completions.add("music");
            }
            if ("leaderboard".startsWith(args[0].toLowerCase())) {
                completions.add("leaderboard");
            } // This bit below adds tab support for the different color options
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
    private boolean handleStartGameCommand(Player player, String[] args) {
        // Check for game mode argument
        if (args.length < 2) {
            player.sendMessage(Component.text("Please specify a game mode. Use /snakegame start <game_mode>.", NamedTextColor.RED));
            return false;
        }

        String gameMode = args[1].toLowerCase();

        if (!gameMode.equals("classic") && !gameMode.equals("pvp")) {
            player.sendMessage(Component.text("Invalid game mode. Available modes: classic, pvp.", NamedTextColor.RED));
            return false;
        }

        player.sendMessage(Component.text("Selected game mode: " + gameMode, NamedTextColor.GREEN));

        Triple<Location, Location, String> locations = findGameAndLobbyLocations(player);
        if (locations == null) {
            player.sendMessage(Component.text("You must be within a lobby region to start the game.", NamedTextColor.RED));
            return false;
        }

        Location lobbyLocation = locations.getMiddle();
        Location gameLocation = locations.getLeft();
        String regionName = locations.getRight();
        World gameWorld = gameLocation.getWorld();

        if (gameMode.equals("pvp")) {
            // Get the instance of QueueManager and use its method to enqueue the player for PvP
            return QueueManager.getInstance().handlePvPQueue(player, lobbyLocation, gameLocation, regionName, gameWorld);
        } else {
            return handleClassicGameStart(player);
        }
    }

    /**
     * Handles the "stop" subcommand.
     *
     * @param player The player issuing the command.
     * @return true if the game or queue was stopped for the player, false otherwise.
     */
    private boolean handleStopGameCommand(Player player) {
        QueueManager queueManager = QueueManager.getInstance();
        if (gameManager.getSnakeForPlayer(player) != null) {  // Check if player is in a game
            gameManager.stopGame(player);
            player.sendMessage(Component.text("Stopping the snake game...", NamedTextColor.RED));
            return true;
        } else if (queueManager.isPlayerInPvPQueue(player)) {  // Check if player is in a PvP queue
            queueManager.removePlayerFromQueues(player);
            player.sendMessage(Component.text("You've been removed from the PvP queue.", NamedTextColor.RED));
            return true;
        } else {
            player.sendMessage(Component.text("You are not currently in a game or queue.", NamedTextColor.RED));
            return false;
        }
    }

    /**
     * Handles the "gui" command by displaying the main menu GUI for the Snake game to the player.
     * If there's an existing GUI instance (and associated event listener), it's unregistered to ensure
     * only the latest instance is active. This method then creates a new instance of the GUI,
     * registers the associated event listener, and opens the GUI for the player.
     *
     * @param player The player executing the "gui" command.
     * @param plugin The current plugin instance.
     * @return Always returns true, indicating the command's successful execution.
     */
    private boolean handleGUICommand(Player player, Plugin plugin) {
        // Unregister the old listener if it exists to avoid multiple GUI instances
        GameCommandGUI oldInstance = GameCommandGUI.getCurrentInstance();
        if (oldInstance != null) {
            InventoryClickEvent.getHandlerList().unregister(oldInstance);
        }

        // Create a new GUI instance and register its event listener
        GameCommandGUI gameCommandGUI = new GameCommandGUI(plugin, player);
        Bukkit.getPluginManager().registerEvents(gameCommandGUI, plugin);

        // Open the GUI for the player
        player.openInventory(gameCommandGUI.getMenu());
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
     * @param args The name of the color to be set.
     * @param plugin    The JavaPlugin instance for accessing plugin-specific features.
     * @return True if the color was set successfully, false otherwise.
     */
    private boolean handleSetColorCommand(Player player, String[] args, JavaPlugin plugin) {
        // Validate the number of arguments and correct usage
        if (args.length != 2) {
            player.sendMessage(Component.text("Please specify a color name. Use /snakegame color <color_name>.", NamedTextColor.RED));
            return false;
        }

        String colorName = args[1];

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
     * The leaderboard shows player names and their scores based on the provided page number.
     *
     * @param player The Player to whom the leaderboard will be sent.
     * @param args   The arguments provided with the command.
     * @return True, indicating that the command was handled successfully.
     */
    private boolean handleLeaderboardCommand(Player player, String[] args) {
        int page = 1; // default page

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

        // Fetch leaderboard data
        List<Map.Entry<String, Integer>> leaderboard = PlayerData.getInstance(plugin).getPaginatedLeaderboard(page);

        // If there's no data for the given page, inform the player
        if (leaderboard.isEmpty()) {
            player.sendMessage(Component.text("There are no entries for this page.", NamedTextColor.RED));
            return true;
        }

        // Send the leaderboard to the player
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
        if (!gameManager.isMusicEnabled()) {
            player.sendMessage(Component.text("Music is globally disabled on this server.", NamedTextColor.RED));
            return false;
        }

        // Fetch the current music preference for the player
        boolean currentPreference = PlayerData.getInstance(plugin).getMusicToggleState(player);

        // Toggle the preference
        PlayerData.getInstance(plugin).setMusicToggleState(player, !currentPreference);

        // Notify the player of the change
        if (!currentPreference) {
            player.sendMessage(Component.text("Music has been enabled for your sessions.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Music has been disabled for your sessions.", NamedTextColor.RED));
        }
        return true;
    }

    // Helper methods

    /**
     * Finds the game and lobby locations for a given player.
     *
     * @param player The player for whom to find locations.
     * @return A Pair containing the game teleport location and the lobby teleport location,
     * or null if no suitable locations are found.
     */
    private Triple<Location, Location, String> findGameAndLobbyLocations(Player player) {
        RegionService regionService = RegionService.getInstance();

        for (Region region : regionService.getAllRegions().values()) {
            if (region.getType() == Region.RegionType.LOBBY) {
                String worldName = region.getWorldName();
                World world = Bukkit.getWorld(worldName);

                if (world != null) {
                    ProtectedRegion worldGuardRegion = regionService.getWorldGuardRegion(region.getName(), world);
                    if (worldGuardRegion != null && regionService.isLocationInRegion(player.getLocation(), worldGuardRegion)) {
                        RegionLink regionLink = regionService.getRegionLink(region.getName(), Region.RegionType.LOBBY);

                        if (regionLink != null) {
                            Location gameTeleportLocation = regionLink.getGameTeleportLocation();
                            Location lobbyTeleportLocation = regionLink.getLobbyTeleportLocation();
                            if (gameTeleportLocation != null) {
                                return Triple.of(gameTeleportLocation, lobbyTeleportLocation, region.getName());
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Handles the initiation of a classic snake game for a player.
     *
     * @param player The player for whom to start the game.
     * @return True if the game was successfully started, false otherwise.
     */
    private boolean handleClassicGameStart(Player player) {
        Triple<Location, Location, String> locations = findGameAndLobbyLocations(player);
        if (locations == null) {
            player.sendMessage(Component.text("No linked game region found. Unable to start the game.", NamedTextColor.RED));
            return false;
        }

        Location gameTeleportLocation = locations.getLeft();
        Location lobbyLocation = locations.getMiddle();
        String regionName = locations.getRight();

        RegionService regionService = RegionService.getInstance();

        // Use the region's name to get the RegionLink
        RegionLink regionLink = regionService.getRegionLink(regionName, Region.RegionType.LOBBY);
        ProtectedRegion gameRegion = regionService.getWorldGuardRegion(regionLink.getGameRegionName(), gameTeleportLocation.getWorld());

        int maxPlayersPerGame = ((Main) plugin).getMaxPlayersPerGame();
        int playersInGameRegion = 0;
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (regionService.isLocationInRegion(onlinePlayer.getLocation(), gameRegion)) {
                SnakeCreation snake = gameManager.getSnakeForPlayer(onlinePlayer); // Checks if the player inside is in a game
                if (snake != null) {
                    playersInGameRegion++;
                }
            }
        }

        if (playersInGameRegion >= maxPlayersPerGame) {
            player.sendMessage(Component.text("The game region has reached its maximum number of players (" + maxPlayersPerGame + " players).", NamedTextColor.RED));
            return false;
        }

        // Start the game
        player.teleport(gameTeleportLocation);
        gameManager.startGame(player, gameTeleportLocation, lobbyLocation, "classic");
        player.sendMessage(Component.text("Starting the snake game...", NamedTextColor.GREEN));
        return true;
    }
}
