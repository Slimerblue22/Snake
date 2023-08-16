package com.slimer;

import com.comphenix.protocol.ProtocolManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GameManager {
    private final Map<Player, Snake> activeGames; // Active games, mapped by player
    private final Map<Player, String> gameRegions; // Game regions, mapped by player
    private final Map<Player, String> lobbyRegions; // Lobby regions, mapped by player
    private final Map<Player, WolfChase> activeWolves = new HashMap<>();
    private final ProtocolManager protocolManager;
    private final WorldGuardManager worldGuardManager;
    private final Map<Player, Apple> activeApples; // Active apples, mapped by player
    private final SnakePlugin plugin;
    private final PlayerData playerData;
    private final GameLoopManager gameLoopManager; // Controls the main game loop

    // Constructor initializes the game manager and starts the game loop
    public GameManager(SnakePlugin plugin, ProtocolManager protocolManager, PlayerData playerData) {
        this.activeGames = new HashMap<>();
        this.gameRegions = new HashMap<>();
        this.lobbyRegions = new HashMap<>();
        this.activeApples = new HashMap<>();
        this.plugin = plugin;
        this.worldGuardManager = new WorldGuardManager(plugin);
        this.protocolManager = protocolManager;
        this.playerData = playerData;
        this.gameLoopManager = new GameLoopManager(this, plugin, worldGuardManager);
    }

    // Retrieves an active game for a specific player
    public Snake getGame(Player player) {
        return this.activeGames.get(player);
    }

    public SnakePlugin getPlugin() {
        return this.plugin;
    }

    public Map<Player, Snake> getActiveGames() {
        return activeGames;
    }

    // Adds a new game for a player, sets up the snake, apple, music, etc.
    public void addGame(Player player) {
        // Retrieve and store the game and lobby regions for the player
        String gameZoneName = worldGuardManager.getPlayerGameZone(player);
        String lobbyZoneName = worldGuardManager.getPlayerLobbyZone(player);
        gameRegions.put(player, gameZoneName);
        lobbyRegions.put(player, lobbyZoneName);

        // Retrieve the player's preferred color and spawn the sheep
        String colorName = playerData.getConfig().getString(player.getUniqueId() + ".color", "WHITE");
        DyeColor color = DyeColor.valueOf(colorName);
        Sheep sheep = (Sheep) player.getWorld().spawnEntity(player.getLocation(), EntityType.SHEEP);
        sheep.setColor(color);

        // Spawn an invisible armor stand above the sheep and make the player a passenger
        Location armorStandLocation = sheep.getLocation().clone().add(0, 1, 0);
        ArmorStand armorStand = (ArmorStand) player.getWorld().spawnEntity(armorStandLocation, EntityType.ARMOR_STAND);
        armorStand.setInvisible(true);
        armorStand.setGravity(false);
        armorStand.setBasePlate(false);
        armorStand.setMarker(true);
        armorStand.addPassenger(player);

        // Create and store the Snake instance for the player
        Snake snake = new Snake(player, sheep, this, protocolManager, playerData, armorStand);
        this.activeGames.put(player, snake);

        // Create and store the Apple instance for the player
        Apple apple = new Apple(this, player, plugin, player.getWorld(), player.getLocation(), snake, worldGuardManager);
        this.activeApples.put(player, apple);
        snake.setApple(apple);

        // Register the packet listener for the snake
        protocolManager.addPacketListener(snake.getPacketAdapter());

        // Start the music if the NoteBlockAPI is available
        if (plugin.getMusicManager() != null) {
            plugin.getMusicManager().startMusic(player, plugin.getConfig().getString("song-file-path"));
        }

        // Check and update the high score
        int highScore = playerData.getHighScore(player);
        int score = snake.getScore();
        if (score > highScore) {
            playerData.setHighScore(player, score);
        }

        // If WolfChase mode is enabled, spawn a wolf and create a WolfChase instance
        if (plugin.isWolfChaseEnabled()) {
            WolfChase wolfChase = new WolfChase(this, worldGuardManager);
            Location wolfSpawnLocation = wolfChase.randomWolfLocation(player.getWorld(), player);
            wolfChase.spawnWolf(wolfSpawnLocation);
            this.activeWolves.put(player, wolfChase); // Store the WolfChase instance
        }
    }

    // Ends a player's game, stops the snake, clears the apple, stops the music, etc.
    public void endGame(Player player) {
        // Retrieve and stop the snake game, notify the player, and play a sound
        Snake snake = this.activeGames.remove(player);
        if (snake != null) {
            snake.stop();
            player.sendMessage(Component.text("Your snake game has ended. Your score was: ", NamedTextColor.RED)
                    .append(Component.text(String.valueOf(snake.getScore()), NamedTextColor.GREEN)));
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.0F);

            // Stop the music if the NoteBlockAPI is available
            if (plugin.getMusicManager() != null) {
                plugin.getMusicManager().stopMusic(player);
            }

            // Remove the packet listener for the snake
            protocolManager.removePacketListener(snake.getPacketAdapter());

            // Check and update the high score
            int highScore = playerData.getHighScore(player);
            int score = snake.getScore();
            if (score > highScore) {
                playerData.setHighScore(player, score);
            }
        }

        // Retrieve and clear the apple
        Apple apple = this.activeApples.remove(player);
        if (apple != null) {
            apple.clearApple();
        }

        // If WolfChase mode is enabled, retrieve and remove the wolf
        if (plugin.isWolfChaseEnabled()) {
            WolfChase wolfChase = this.activeWolves.remove(player);
            if (wolfChase != null) {
                wolfChase.removeWolf();
            }
        }

        // Get the stored game region for the player
        String gameZoneName = gameRegions.get(player);

        // Schedule the teleportation to the lobby using the stored game region
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> worldGuardManager.teleportToLobby(player, gameZoneName), 20L);

        // Remove the game and lobby regions for the player
        gameRegions.remove(player);
        lobbyRegions.remove(player);
    }

    public WorldGuardManager getWorldGuardManager() {
        return worldGuardManager;
    }

    // Methods to retrieve the game region and lobby region for a player
    public String getGameZoneForPlayer(Player player) {
        return gameRegions.get(player);
    }

    // Removes a game without ending it (used for cleanup)
    public void removeGame(Player player) {
        Snake snake = this.activeGames.remove(player);
        if (snake != null) {
            snake.stop();
            if (plugin.getMusicManager() != null) {
                plugin.getMusicManager().stopMusic(player);
            }
            protocolManager.removePacketListener(snake.getPacketAdapter());
        }
        Apple apple = this.activeApples.remove(player);
        if (apple != null) {
            apple.clearApple();
        }
    }

    // Retrieves all active games
    public Collection<Snake> getAllGames() {
        return activeGames.values();
    }

    // Retrieves the game loop manager
    public GameLoopManager getGameLoopManager() {
        return gameLoopManager;
    }

    public WolfChase getWolfChase(Player player) {
        return activeWolves.get(player);
    }
}
