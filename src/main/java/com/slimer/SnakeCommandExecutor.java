package com.slimer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SnakeCommandExecutor implements CommandExecutor, TabCompleter {
    private final GameManager gameManager;
    private final PlayerData playerData;
    private final WorldGuardManager worldGuardManager;
    private final SnakePlugin plugin;

    public SnakeCommandExecutor(GameManager gameManager, PlayerData playerData, WorldGuardManager worldGuardManager, SnakePlugin plugin) {
        this.gameManager = gameManager;
        this.playerData = playerData;
        this.worldGuardManager = worldGuardManager;
        this.plugin = plugin;
    }

    // Registering the commands
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        // If no arguments are provided, open the GUI
        if (args.length == 0) {
            openGUI(sender, player);
            return true;
        }

        // Handle other command logic based on the provided arguments
        switch (args[0].toLowerCase()) {
            case "start" -> startGame(sender, player);
            case "stop" -> stopGame(sender, player);
            case "setspeed" -> setSpeed(sender, args);
            case "lobbycords" -> setLobbyCoordinates(sender);
            case "instructions", "help" -> sendInstructions(sender);
            case "gamecords" -> setGameCoordinates(sender);
            case "reload" -> reloadConfig(sender);
            case "color" -> setColor(sender, args, player);
            case "leaderboard" -> showLeaderboard(sender, player);
            case "highscore" -> showHighScore(sender, player);
            case "gui" -> openGUI(sender, player);
            default -> showInvalidCommandMessage(sender);
        }
        return true;
    }

    // Tab completion stuff
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> availableCommands = new ArrayList<>();
        if (args.length == 1) {
            // Commands available for all players with 'snake.play' permission
            if (sender.hasPermission("snake.play")) {
                availableCommands.addAll(Arrays.asList("color", "gui", "help", "highscore", "leaderboard", "start", "stop"));
            }

            // Commands available for administrators with 'snake.admin' permission
            if (sender.hasPermission("snake.admin")) {
                availableCommands.addAll(Arrays.asList("gamecords", "lobbycords", "setspeed", "reload"));
            }
        }
        return availableCommands;
    }

    // This bit is related to getting the color options for the snake.
    private boolean isValidColor(String colorName) {
        try {
            DyeColor.valueOf(colorName);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // Starting from here is the logic for the commands
    private void startGame(CommandSender sender, Player player) {
        if (player == null) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return;
        }
        if (gameManager.getGame(player) != null) {
            sender.sendMessage(Component.text("You already have a snake game running.", NamedTextColor.RED));
            return;
        }
        if (!worldGuardManager.isPlayerInLobby(player)) {
            sender.sendMessage(Component.text("You can only start the game in the lobby!", NamedTextColor.RED));
            return;
        }
        if (!worldGuardManager.teleportToGame(player, true)) {
            sender.sendMessage(Component.text("The game coordinates are not specified correctly in the config. The game will not start.", NamedTextColor.RED));
            return;
        }
        if (!worldGuardManager.teleportToLobby(player, true)) {
            sender.sendMessage(Component.text("The lobby coordinates are not specified correctly in the config. The game will not start.", NamedTextColor.RED));
            return;
        }
        worldGuardManager.teleportToGame(player, false);
        if (!sender.hasPermission("snake.play")) {
            sender.sendMessage(Component.text("You do not have permission to start the snake game.", NamedTextColor.RED));
        } else {
            sender.sendMessage(Component.text("Starting the snake game...", NamedTextColor.GREEN));
            gameManager.addGame(player);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0F, 1.0F);
        }
    }

    private void stopGame(CommandSender sender, Player player) {
        if (player == null) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return;
        }
        if (!sender.hasPermission("snake.play")) {
            sender.sendMessage(Component.text("You do not have permission to stop the snake game.", NamedTextColor.RED));
            return;
        }
        Snake snake = gameManager.getGame(player);
        if (snake != null) {
            gameManager.endGame(player);
        } else {
            sender.sendMessage(Component.text("You don't have a snake game running.", NamedTextColor.RED));
        }
    }

    private void openGUI(CommandSender sender, Player player) {
        if (player == null) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return;
        }
        if (!sender.hasPermission("snake.play")) {
            sender.sendMessage(Component.text("You don't have permission to run this command!", NamedTextColor.RED));
            return;
        }

        Inventory menu = SnakeGUI.createMainMenu(player);
        player.openInventory(menu);
        sender.sendMessage(Component.text("Opening the snake game GUI...", NamedTextColor.GREEN));
    }

    private void setSpeed(CommandSender sender, String[] args) {
        if (!sender.hasPermission("snake.admin")) {
            sender.sendMessage(Component.text("You don't have permission to run this command!", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Specify a speed value.", NamedTextColor.RED));
            return;
        }
        int speed;
        try {
            speed = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid speed value.", NamedTextColor.RED));
            return;
        }
        plugin.getConfig().set("snake-speed", speed);
        plugin.saveConfig();
        gameManager.getGameLoopManager().setSpeed(speed);
        sender.sendMessage(Component.text("Snake speed set to: " + speed, NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Please restart your server for speed to take effect.", NamedTextColor.GREEN));

    }

    private void sendInstructions(CommandSender sender) {
        if (sender instanceof Player && !sender.hasPermission("snake.play")) {
            sender.sendMessage(Component.text("You don't have permission to run this command!", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text("------ Snake Game Instructions ------", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("- The objective is to eat as many apples as possible without running into yourself or the walls.", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("- Hold forward to move in the direction you are looking.", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("- Eating an apple will make your snake longer.", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("- The game ends if you run into yourself or the game boundaries.", NamedTextColor.WHITE));
        sender.sendMessage(Component.text("- Your score increases with each apple you eat.", NamedTextColor.WHITE));
    }

    private void reloadConfig(CommandSender sender) {
        if (sender.hasPermission("snake.admin")) {
            plugin.reloadConfig();
            worldGuardManager.updateLocations();
            sender.sendMessage(Component.text("Snake plugin config reloaded.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("You don't have permission to run this command!", NamedTextColor.RED));
        }
    }

    private void showInvalidCommandMessage(CommandSender sender) {
        sender.sendMessage(Component.text("Invalid command.", NamedTextColor.RED));
    }

    private void showHighScore(CommandSender sender, Player player) {
        if (player == null) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return;
        }
        if (!player.hasPermission("snake.play")) {
            sender.sendMessage(Component.text("You don't have permission to run this command!", NamedTextColor.RED));
            return;
        }
        int highScore = playerData.getHighScore(player);
        sender.sendMessage(Component.text("Your high score is: ", NamedTextColor.GREEN).append(Component.text(String.valueOf(highScore), NamedTextColor.GREEN)));
    }

    private void setColor(CommandSender sender, String[] args, Player player) {
        if (player == null) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return;
        }
        if (!player.hasPermission("snake.play")) {
            sender.sendMessage(Component.text("You don't have permission to run this command!", NamedTextColor.RED));
            return;
        }
        if (args.length != 2) {
            sender.sendMessage(Component.text("You need to specify a color.", NamedTextColor.RED));
            return;
        }
        String colorName = args[1].toUpperCase();
        if (!isValidColor(colorName)) {
            sender.sendMessage(Component.text("Invalid color selected.", NamedTextColor.RED));
            return;
        }
        playerData.getConfig().set(player.getUniqueId() + ".color", colorName);
        playerData.saveConfig();
        sender.sendMessage(Component.text("Color has been set to: ", NamedTextColor.GREEN).append(Component.text(colorName)));
    }

    private void showLeaderboard(CommandSender sender, Player player) {
        if (player == null) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return;
        }
        if (!player.hasPermission("snake.play")) {
            sender.sendMessage(Component.text("You don't have permission to run this command!", NamedTextColor.RED));
            return;
        }
        ConfigurationSection configSection = playerData.getConfig().getConfigurationSection("");
        if (configSection == null) {
            player.sendMessage(Component.text("No player data found.", NamedTextColor.RED));
            return;
        }
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

    private void setLobbyCoordinates(CommandSender sender) {
        if (sender instanceof Player player) {
            if (!player.hasPermission("snake.admin")) {
                player.sendMessage(Component.text("You don't have permission to run this command!", NamedTextColor.RED));
                return;
            }
            Location location = player.getLocation();
            String lobbyRegionName = plugin.getConfig().getString("Lobby");
            World gameWorld = Bukkit.getWorld(Objects.requireNonNull(plugin.getConfig().getString("world")));
            if (plugin.getWorldGuardManager().isLocationOutsideRegion(location, lobbyRegionName, gameWorld)) {
                player.sendMessage(Component.text("The selected location is not inside the lobby region!", NamedTextColor.RED));
                return;
            }
            plugin.getConfig().set("teleportLocations.lobby.x", location.getX());
            plugin.getConfig().set("teleportLocations.lobby.y", location.getY());
            plugin.getConfig().set("teleportLocations.lobby.z", location.getZ());
            plugin.saveConfig();
            plugin.getWorldGuardManager().updateLocations();
            player.sendMessage(Component.text("Lobby coordinates set to your current location.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
        }
    }

    private void setGameCoordinates(CommandSender sender) {
        if (sender instanceof Player player) {
            if (!player.hasPermission("snake.admin")) {
                player.sendMessage(Component.text("You don't have permission to run this command!", NamedTextColor.RED));
                return;
            }
            Location location = player.getLocation();
            String gameZoneRegionName = plugin.getConfig().getString("Gamezone");

            World gameWorld = Bukkit.getWorld(Objects.requireNonNull(plugin.getConfig().getString("world")));
            if (plugin.getWorldGuardManager().isLocationOutsideRegion(location, gameZoneRegionName, gameWorld)) {
                player.sendMessage(Component.text("The selected location is not inside the game region!", NamedTextColor.RED));
                return;
            }
            plugin.getConfig().set("teleportLocations.game.x", location.getX());
            plugin.getConfig().set("teleportLocations.game.y", location.getY());
            plugin.getConfig().set("teleportLocations.game.z", location.getZ());
            plugin.saveConfig();
            plugin.getWorldGuardManager().updateLocations();
            player.sendMessage(Component.text("Game coordinates set to your current location.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
        }
    }
}