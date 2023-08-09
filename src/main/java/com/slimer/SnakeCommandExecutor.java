package com.slimer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

// Handles the execution of commands related to the Snake game.
public class SnakeCommandExecutor implements CommandExecutor, TabCompleter {
    private final GameManager gameManager; // Core game manager instance.
    private final PlayerData playerData; // Stores data associated with each player.
    private final WorldGuardManager worldGuardManager; // Manages interactions with WorldGuard.
    private final SnakePlugin plugin; // Main plugin instance.

    // Initializes the Snake command executor.
    public SnakeCommandExecutor(GameManager gameManager, PlayerData playerData, WorldGuardManager worldGuardManager, SnakePlugin plugin) {
        this.gameManager = gameManager;
        this.playerData = playerData;
        this.worldGuardManager = worldGuardManager;
        this.plugin = plugin;
    }

    // Processes and executes the Snake game commands.
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length > 0) {
            Player player = null;
            if (sender instanceof Player) {
                player = (Player) sender;
            }
            switch (args[0].toLowerCase()) {
                case "start" -> {
                    if (player == null) {
                        sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
                        return true;
                    }
                    if (!worldGuardManager.isPlayerInLobby(player)) {
                        sender.sendMessage(Component.text("You can only start the game in the lobby!", NamedTextColor.RED));
                        return true;
                    }

                    // Validate game and lobby coordinates
                    if (!worldGuardManager.teleportToGame(player, true)) {
                        sender.sendMessage(Component.text("The game coordinates are not specified correctly in the config. The game will not start.", NamedTextColor.RED));
                        return true;
                    }
                    if (!worldGuardManager.teleportToLobby(player, true)) {
                        sender.sendMessage(Component.text("The lobby coordinates are not specified correctly in the config. The game will not start.", NamedTextColor.RED));
                        return true;
                    }

                    // If validations passed, teleport to the game
                    worldGuardManager.teleportToGame(player, false);

                    if (!sender.hasPermission("snake.play")) {
                        sender.sendMessage(Component.text("You do not have permission to start the snake game.", NamedTextColor.RED));
                    } else if (gameManager.getGame(player) != null) {
                        sender.sendMessage(Component.text("You already have a snake game running.", NamedTextColor.RED));
                    } else {
                        sender.sendMessage(Component.text("Starting the snake game...", NamedTextColor.GREEN));
                        gameManager.addGame(player);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0F, 1.0F);
                    }
                }
                case "stop" -> {
                    if (player == null || !sender.hasPermission("snake.play")) {
                        sender.sendMessage(Component.text("You do not have permission to stop the snake game.", NamedTextColor.RED));
                    } else {
                        Snake snake = gameManager.getGame(player);
                        if (snake != null) {
                            gameManager.endGame(player);
                        } else {
                            sender.sendMessage(Component.text("You don't have a snake game running.", NamedTextColor.RED));
                        }
                    }
                }
                case "setspeed" -> {
                    if (!sender.hasPermission("snake.admin")) {
                        sender.sendMessage(Component.text("You don't have permission to run this command!", NamedTextColor.RED));
                        return true;
                    }
                    if (args.length < 2) {
                        sender.sendMessage(Component.text("Specify a speed value.", NamedTextColor.RED));
                        return true;
                    }
                    int speed;
                    try {
                        speed = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(Component.text("Invalid speed value.", NamedTextColor.RED));
                        return true;
                    }
                    plugin.getConfig().set("snake-speed", speed);
                    plugin.saveConfig();
                    // Inform the GameLoopManager of the speed change
                    gameManager.getGameLoopManager().setSpeed(speed);
                    sender.sendMessage(Component.text("Snake speed set to: " + speed, NamedTextColor.GREEN));
                }
                case "lobbycords" -> {
                    setLobbyCoordinates(sender);
                    return true;
                }
                case "instructions", "help" -> {
                    sender.sendMessage(Component.text("------ Snake Game Instructions ------", NamedTextColor.GOLD));
                    sender.sendMessage(Component.text("- The objective is to eat as many apples as possible without running into yourself or the walls.", NamedTextColor.WHITE));
                    sender.sendMessage(Component.text("- Hold forward to move in the direction you are looking.", NamedTextColor.WHITE));
                    sender.sendMessage(Component.text("- Eating an apple will make your snake longer.", NamedTextColor.WHITE));
                    sender.sendMessage(Component.text("- The game ends if you run into yourself or the game boundaries.", NamedTextColor.WHITE));
                    sender.sendMessage(Component.text("- Your score increases with each apple you eat.", NamedTextColor.WHITE));
                    return true;
                }
                case "gamecords" -> {
                    setGameCoordinates(sender);
                    return true;
                }
                case "reload" -> {
                    if (sender.hasPermission("snake.admin")) {
                        plugin.reloadConfig();
                        worldGuardManager.updateLocations();
                        sender.sendMessage(Component.text("Snake plugin config reloaded.", NamedTextColor.GREEN));
                    } else {
                        sender.sendMessage(Component.text("You don't have permission to reload the Snake plugin config.", NamedTextColor.RED));
                    }
                    return true;
                }
                case "color" -> {
                    if (args.length != 2) {
                        sender.sendMessage(Component.text("You need to specify a color.", NamedTextColor.RED));
                    } else {
                        String colorName = args[1].toUpperCase();
                        if (!isValidColor(colorName)) {
                            sender.sendMessage(Component.text("Invalid color. Use one of the following: ", NamedTextColor.RED).append(getValidColors()));
                        } else {
                            assert player != null;
                            playerData.getConfig().set(((Player) sender).getUniqueId() + ".color", colorName);
                            playerData.saveConfig();
                            sender.sendMessage(Component.text("Color has been set to: ", NamedTextColor.GREEN).append(Component.text(colorName, getColor(colorName))));
                        }
                    }
                }
                case "leaderboard" -> {
                    if (player == null) {
                        sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
                    } else {
                        ConfigurationSection configSection = playerData.getConfig().getConfigurationSection("");
                        if (configSection == null) {
                            player.sendMessage(Component.text("No player data found.", NamedTextColor.RED));
                        } else {
                            Set<String> keys = configSection.getKeys(false);
                            List<Map.Entry<String, Integer>> leaderboard = new ArrayList<>();
                            for (String key : keys) {
                                int score = playerData.getConfig().getInt(key + ".score", 0);
                                leaderboard.add(new AbstractMap.SimpleEntry<>(Bukkit.getOfflinePlayer(UUID.fromString(key)).getName(), score));
                            }
                            leaderboard.sort((a, b) -> b.getValue() - a.getValue());
                            player.sendMessage(Component.text("---- Leaderboard ----", NamedTextColor.GOLD));
                            for (int i = 0; i < leaderboard.size(); i++) {
                                Map.Entry<String, Integer> entry = leaderboard.get(i);
                                player.sendMessage(Component.text("#" + (i + 1) + ": " + entry.getKey() + " - " + entry.getValue(), NamedTextColor.GREEN));
                            }
                        }
                    }
                }
                case "highscore" -> {
                    if (player != null) {
                        int highScore = playerData.getHighScore(player);
                        sender.sendMessage(Component.text("Your high score is: ", NamedTextColor.GREEN).append(Component.text(String.valueOf(highScore), NamedTextColor.GREEN)));
                    }
                }
                default -> sender.sendMessage(Component.text("Invalid command.", NamedTextColor.RED));
            }
        }
        return true;
    }

    // Provides command suggestions for tab completion.
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("start", "stop", "color", "highscore", "setspeed", "leaderboard", "lobbycords", "gamecords","help");
        }
        return null;
    }

    // Checks if a given string corresponds to a valid color.
    private boolean isValidColor(String colorName) {
        try {
            DyeColor.valueOf(colorName);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // Retrieves a list of valid color names.
    private Component getValidColors() {
        List<Component> colorComponents = new ArrayList<>();
        for (DyeColor dyeColor : DyeColor.values()) {
            NamedTextColor textColor = getColor(dyeColor.name());
            if (textColor != null) {
                colorComponents.add(Component.text(dyeColor.name(), textColor));
            }
        }
        return Component.join(JoinConfiguration.separator(Component.text(", ", NamedTextColor.WHITE)), colorComponents);
    }

    // Sets the lobby coordinates to the player's current location.
    private void setLobbyCoordinates(CommandSender sender) {
        if (sender instanceof Player player) {
            if (!player.hasPermission("snake.admin")) {
                player.sendMessage(Component.text("You don't have permission to run this command!", NamedTextColor.RED));
                return;
            }
            Location location = player.getLocation();

            // Retrieve the lobby region name from the config
            String lobbyRegionName = plugin.getConfig().getString("Lobby");

            // Check if the location is inside the lobby region
            if (plugin.getWorldGuardManager().isLocationInRegion(location, lobbyRegionName)) {
                player.sendMessage(Component.text("The selected location is not inside the lobby region!", NamedTextColor.RED));
                return;
            }

            plugin.getConfig().set("teleportLocations.lobby.x", location.getX());
            plugin.getConfig().set("teleportLocations.lobby.y", location.getY());
            plugin.getConfig().set("teleportLocations.lobby.z", location.getZ());
            plugin.saveConfig();

            // Update the lobby coordinates without needing a full reload
            plugin.getWorldGuardManager().updateLocations();

            player.sendMessage(Component.text("Lobby coordinates set to your current location.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("This command can only be run by a player.", NamedTextColor.RED));
        }
    }

    // Sets the game coordinates to the player's current location.
    private void setGameCoordinates(CommandSender sender) {
        if (sender instanceof Player player) {
            if (!player.hasPermission("snake.admin")) {
                player.sendMessage(Component.text("You don't have permission to run this command!", NamedTextColor.RED));
                return;
            }
            Location location = player.getLocation();
            String gameZoneRegionName = plugin.getConfig().getString("Gamezone");

            if (plugin.getWorldGuardManager().isLocationInRegion(location, gameZoneRegionName)) {
                player.sendMessage(Component.text("The selected location is not inside the game region!", NamedTextColor.RED));
                return;
            }

            plugin.getConfig().set("teleportLocations.game.x", location.getX());
            plugin.getConfig().set("teleportLocations.game.y", location.getY());
            plugin.getConfig().set("teleportLocations.game.z", location.getZ());
            plugin.saveConfig();

            // Update the game coordinates without needing a full reload
            plugin.getWorldGuardManager().updateLocations();

            player.sendMessage(Component.text("Game coordinates set to your current location.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("This command can only be run by a player.", NamedTextColor.RED));
        }
    }

    // Converts a DyeColor name to its corresponding NamedTextColor.
    private NamedTextColor getColor(String dyeColorName) {
        try {
            DyeColor dyeColor = DyeColor.valueOf(dyeColorName);
            return switch (dyeColor) {
                case WHITE -> NamedTextColor.WHITE;
                case ORANGE -> NamedTextColor.GOLD;
                case MAGENTA, PINK -> NamedTextColor.LIGHT_PURPLE;
                case LIGHT_BLUE -> NamedTextColor.AQUA;
                case YELLOW -> NamedTextColor.YELLOW;
                case LIME -> NamedTextColor.GREEN;
                case GRAY -> NamedTextColor.DARK_GRAY;
                case LIGHT_GRAY -> NamedTextColor.GRAY;
                case CYAN -> NamedTextColor.DARK_AQUA;
                case PURPLE -> NamedTextColor.DARK_PURPLE;
                case BLUE -> NamedTextColor.DARK_BLUE;
                case BROWN -> NamedTextColor.DARK_RED;   // No direct brown color in TextColor, using a similar shade
                case GREEN -> NamedTextColor.DARK_GREEN;
                case RED -> NamedTextColor.RED;
                case BLACK -> NamedTextColor.BLACK;
            };
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
