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
            case "instructions", "help" -> sendInstructions(sender);
            case "reload" -> reloadConfig(sender);
            case "color" -> setColor(sender, args, player);
            case "gamezone" -> registerGameZone(sender, args);
            case "lobbyzone" -> registerLobbyZone(sender, args);
            case "linkzones" -> linkZones(sender, args);
            case "lobbytp" -> setLobbyTeleport(sender, args);
            case "gametp" -> setGameTeleport(sender, args);
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
                availableCommands.addAll(Arrays.asList("gamezone", "lobbyzone", "setspeed", "linkzones", "reload", "lobbytp", "gametp"));
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
        String lobbyZone = worldGuardManager.getPlayerLobbyZone(player);
        if (lobbyZone == null) {
            sender.sendMessage(Component.text("You must be in a lobby to start the game.", NamedTextColor.RED));
            return;
        }

        // Attempt to teleport to the game zone
        if (!worldGuardManager.teleportToGameZone(player, lobbyZone)) {
            sender.sendMessage(Component.text("Failed to teleport to the game zone.", NamedTextColor.RED));
            return;
        }

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

    private void setLobbyTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by a player.");
            return;
        }
        if (!sender.hasPermission("snake.admin")) {
            sender.sendMessage(Component.text("You don't have permission to run this command!", NamedTextColor.RED));
            return;
        }
        if (args.length != 2) {
            sender.sendMessage("Usage: /snake LobbyTP {lobby region name}");
            return;
        }
        String lobbyRegionName = args[1];

        // Find the link number based on the lobby region name
        ConfigurationSection linkedSection = worldGuardManager.getRegionLinksConfig().getConfigurationSection("Linked");
        if (linkedSection == null) {
            sender.sendMessage("No links found.");
            return;
        }
        String linkKey = null;
        for (String key : linkedSection.getKeys(false)) {
            if (Objects.equals(linkedSection.getString(key + ".LobbyRegion"), lobbyRegionName)) {
                linkKey = key;
                break;
            }
        }
        if (linkKey == null) {
            sender.sendMessage("Lobby region not found: " + lobbyRegionName);
            return;
        }

        // Retrieve the player's coordinates and round to the nearest full number
        Location playerLocation = player.getLocation();
        int x = (int) Math.round(playerLocation.getX());
        int y = (int) Math.round(playerLocation.getY());
        int z = (int) Math.round(playerLocation.getZ());

        // Save the coordinates to the RegionLinks.yml file for the lobby region
        worldGuardManager.getRegionLinksConfig().set("Linked." + linkKey + ".lobbyTP", x + "," + y + "," + z);
        worldGuardManager.saveRegionLinksFile();

        sender.sendMessage("Teleportation coordinates for lobby region " + lobbyRegionName + " have been set.");
    }

    private void setGameTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by a player.");
            return;
        }
        if (!sender.hasPermission("snake.admin")) {
            sender.sendMessage(Component.text("You don't have permission to run this command!", NamedTextColor.RED));
            return;
        }

        if (args.length != 2) {
            sender.sendMessage("Usage: /snake GameTP {game region name}");
            return;
        }
        String gameRegionName = args[1];

        // Find the link number based on the game region name
        ConfigurationSection linkedSection = worldGuardManager.getRegionLinksConfig().getConfigurationSection("Linked");
        if (linkedSection == null) {
            sender.sendMessage("No links found.");
            return;
        }
        String linkKey = null;
        for (String key : linkedSection.getKeys(false)) {
            if (Objects.equals(linkedSection.getString(key + ".GameRegion"), gameRegionName)) {
                linkKey = key;
                break;
            }
        }
        if (linkKey == null) {
            sender.sendMessage("Game region not found: " + gameRegionName);
            return;
        }

        // Retrieve the player's coordinates and round to the nearest full number
        Location playerLocation = player.getLocation();
        int x = (int) Math.round(playerLocation.getX());
        int y = (int) Math.round(playerLocation.getY());
        int z = (int) Math.round(playerLocation.getZ());

        // Save the coordinates to the RegionLinks.yml file for the game region
        worldGuardManager.getRegionLinksConfig().set("Linked." + linkKey + ".gameTP", x + "," + y + "," + z);
        worldGuardManager.saveRegionLinksFile();

        sender.sendMessage("Teleportation coordinates for game region " + gameRegionName + " have been set.");
    }

    private void registerLobbyZone(CommandSender sender, String[] args) {
        if (!sender.hasPermission("snake.admin")) {
            sender.sendMessage(Component.text("You don't have permission to run this command!", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Please specify the name of the WorldGuard region to register as a lobby zone.", NamedTextColor.RED));
            return;
        }
        String regionName = args[1];

        // Call into WorldGuardManager to register the region
        if (plugin.getWorldGuardManager().registerLobbyZone(regionName)) {
            sender.sendMessage(Component.text("Successfully registered region " + regionName + " as a lobby zone.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Failed to register region " + regionName + " as a lobby zone.", NamedTextColor.RED));
        }
    }

    private void linkZones(CommandSender sender, String[] args) {
        if (!sender.hasPermission("snake.admin")) {
            sender.sendMessage(Component.text("You don't have permission to run this command!", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text("Please specify the names of the lobby and game zones to link.", NamedTextColor.RED));
            return;
        }
        String lobbyZoneName = args[1];
        String gameZoneName = args[2];

        // Call into WorldGuardManager to link the zones
        WorldGuardManager.LinkResult result = plugin.getWorldGuardManager().addZoneLink(lobbyZoneName, gameZoneName);
        switch (result) {
            case SUCCESS ->
                    sender.sendMessage(Component.text("Successfully linked lobby zone " + lobbyZoneName + " with game zone " + gameZoneName + ".", NamedTextColor.GREEN));
            case ALREADY_LINKED ->
                    sender.sendMessage(Component.text("These zones are already linked!", NamedTextColor.RED));
            case ZONES_NOT_FOUND ->
                    sender.sendMessage(Component.text("One or both zones not found!", NamedTextColor.RED));
        }
    }

    private void registerGameZone(CommandSender sender, String[] args) {
        if (!sender.hasPermission("snake.admin")) {
            sender.sendMessage(Component.text("You don't have permission to run this command!", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Please specify the name of the WorldGuard region to register as a game zone.", NamedTextColor.RED));
            return;
        }
        String regionName = args[1];

        // Call into WorldGuardManager to register the region
        if (plugin.getWorldGuardManager().registerGameZone(regionName)) {
            sender.sendMessage(Component.text("Successfully registered region " + regionName + " as a game zone.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Failed to register region " + regionName + " as a game zone.", NamedTextColor.RED));
        }
    }
}