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
    private final ProtocolManager protocolManager;
    private final WorldGuardManager worldGuardManager;
    private final Map<Player, Apple> activeApples; // Active apples, mapped by player
    private final SnakePlugin plugin;
    private final PlayerData playerData;
    private final GameLoopManager gameLoopManager; // Controls the main game loop

    // Constructor initializes the game manager and starts the game loop
    public GameManager(SnakePlugin plugin, ProtocolManager protocolManager, PlayerData playerData) {
        this.activeGames = new HashMap<>();
        this.activeApples = new HashMap<>();
        this.plugin = plugin;
        this.worldGuardManager = new WorldGuardManager(plugin);
        this.protocolManager = protocolManager;
        this.playerData = playerData;
        this.gameLoopManager = new GameLoopManager(this);
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
        String colorName = playerData.getConfig().getString(player.getUniqueId() + ".color", "WHITE");
        DyeColor color = DyeColor.valueOf(colorName);
        Sheep sheep = (Sheep) player.getWorld().spawnEntity(player.getLocation(), EntityType.SHEEP);
        sheep.setColor(color);
        Location armorStandLocation = sheep.getLocation().clone().add(0, 1, 0);
        final ArmorStand[] armorStand = new ArmorStand[1];
        armorStand[0] = (ArmorStand) player.getWorld().spawnEntity(armorStandLocation, EntityType.ARMOR_STAND);
        armorStand[0].setInvisible(true);
        armorStand[0].setGravity(false);
        armorStand[0].setBasePlate(false);
        armorStand[0].setMarker(true);
        armorStand[0].addPassenger(player);
        Snake snake = new Snake(player, sheep, this, protocolManager, playerData, armorStand[0]);
        this.activeGames.put(player, snake);
        Apple apple = new Apple(this, player, plugin, player.getWorld(), player.getLocation(), snake, worldGuardManager);
        this.activeApples.put(player, apple);
        snake.setApple(apple);
        protocolManager.addPacketListener(snake.getPacketAdapter());
        if (plugin.getMusicManager() != null) {
            plugin.getMusicManager().startMusic(player, plugin.getConfig().getString("song-file-path"));
        }
        int highScore = playerData.getHighScore(player);
        int score = snake.getScore();
        if (score > highScore) {
            playerData.setHighScore(player, score);
        }
    }

    // Ends a player's game, stops the snake, clears the apple, stops the music, etc.
    public void endGame(Player player) {
        Snake snake = this.activeGames.remove(player);
        if (snake != null) {
            snake.stop();
            player.sendMessage(Component.text("Your snake game has ended. Your score was: ", NamedTextColor.RED).append(Component.text(String.valueOf(snake.getScore()), NamedTextColor.GREEN)));
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.0F);
            if (plugin.getMusicManager() != null) {
                plugin.getMusicManager().stopMusic(player);
            }
            protocolManager.removePacketListener(snake.getPacketAdapter());
        }
        Apple apple = this.activeApples.remove(player);
        if (apple != null) {
            apple.clearApple();
        }
        int highScore = playerData.getHighScore(player);
        assert snake != null;
        int score = snake.getScore();
        if (score > highScore) {
            playerData.setHighScore(player, score);
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            assert plugin.getWorldGuardManager() != null;
            plugin.getWorldGuardManager().teleportToLobby(player, false);
        }, 20L);
    }

    public WorldGuardManager getWorldGuardManager() {
        return worldGuardManager;
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
}
