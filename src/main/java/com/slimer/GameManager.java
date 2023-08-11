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

// Manages all active snake games and provides game-related utilities.
public class GameManager {
    private final Map<Player, Snake> activeGames; // Active games mapped by the player controlling the snake.
    private final ProtocolManager protocolManager; // Manages protocol interactions.
    private final WorldGuardManager worldGuardManager; // Manages WorldGuard interactions.
    private final Map<Player, Apple> activeApples; // Active apples in the game mapped by the player.
    private final SnakePlugin plugin; // Main plugin instance.
    private final PlayerData playerData; // Manages player data such as scores.
    private final GameLoopManager gameLoopManager;

    // Returns the snake game associated with a player.
    public Snake getGame(Player player) {
        return this.activeGames.get(player);
    }

    // Returns the main plugin instance.
    public SnakePlugin getPlugin() {
        return this.plugin;
    }

    // Constructor initializes the game manager.
    public GameManager(SnakePlugin plugin, ProtocolManager protocolManager, PlayerData playerData) {
        this.activeGames = new HashMap<>();
        this.activeApples = new HashMap<>();
        this.plugin = plugin;
        this.worldGuardManager = new WorldGuardManager(plugin);
        this.protocolManager = protocolManager;
        this.playerData = playerData;
        this.gameLoopManager = new GameLoopManager(this);
    }

    // Returns all active games.
    public Map<Player, Snake> getActiveGames() {
        return activeGames;
    }

    // Starts a new snake game for the given player.
    public void addGame(Player player) {
        String colorName = playerData.getConfig().getString(player.getUniqueId() + ".color", "WHITE");
        DyeColor color = DyeColor.valueOf(colorName);
        Sheep sheep = (Sheep) player.getWorld().spawnEntity(player.getLocation(), EntityType.SHEEP);
        sheep.setColor(color);
        // Prepare the armor stand but do not spawn it yet
        Location armorStandLocation = sheep.getLocation().clone().add(0, 1, 0);
        final ArmorStand[] armorStand = new ArmorStand[1];

        // Spawn the armor stand
        armorStand[0] = (ArmorStand) player.getWorld().spawnEntity(armorStandLocation, EntityType.ARMOR_STAND);

        // Set properties to make the armor stand invisible and not affected by gravity
        armorStand[0].setInvisible(true);
        armorStand[0].setGravity(false);
        armorStand[0].setBasePlate(false);
        armorStand[0].setMarker(true);

        // Mount the player to the armor stand
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

        // check if the score of this game is higher than the player's high score
        int highScore = playerData.getHighScore(player);
        int score = snake.getScore();
        if (score > highScore) {
            playerData.setHighScore(player, score);
        }
    }

    // Ends a snake game for the given player.
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
        // check if the score of this game is higher than the player's high score
        int highScore = playerData.getHighScore(player);
        assert snake != null;
        int score = snake.getScore();
        if (score > highScore) {
            playerData.setHighScore(player, score);
        }

        // Delay teleporting player back to the lobby to allow time for the sound effect to play
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            assert plugin.getWorldGuardManager() != null;
            plugin.getWorldGuardManager().teleportToLobby(player, false);
        }, 20L); // 20 ticks is roughly 1 second
    }

    // Returns the WorldGuard manager.
    public WorldGuardManager getWorldGuardManager() {
        return worldGuardManager;
    }

    // Removes a snake game associated with the given player.
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

    // Returns a collection of all active snake games.
    public Collection<Snake> getAllGames() {
        return activeGames.values();
    }
    public GameLoopManager getGameLoopManager() {
        return gameLoopManager;
    }
}
