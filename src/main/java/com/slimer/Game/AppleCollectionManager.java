package com.slimer.Game;

import com.slimer.Main.Main;
import com.slimer.Util.DebugManager;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class AppleCollectionManager {
    private final GameManager gameManager;

    public AppleCollectionManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Monitors and manages the apple collection process for the player's snake.
     *
     * @param sheepEntity The entity representing the snake's head.
     * @param player      The player controlling the snake.
     * @param plugin      The JavaPlugin instance for accessing game configurations.
     */
    public void checkAndCollectApple(Entity sheepEntity, Player player, JavaPlugin plugin) {
        List<Apple> applesToHandle = detectAppleCollision(sheepEntity, player);
        handleCollidedApplesAndActions(applesToHandle, player, plugin);
        spawnNewApples(sheepEntity, player, plugin);
    }

    /**
     * Detects collisions between the snake's head and apples.
     *
     * @param sheepEntity The entity representing the snake's head.
     * @param player      The player controlling the snake.
     * @return List of collided apples.
     */
    private List<Apple> detectAppleCollision(Entity sheepEntity, Player player) {
        List<Apple> collidedApples = new ArrayList<>();
        List<Apple> apples = gameManager.getPlayerApples().getOrDefault(player, new ArrayList<>());

        for (Apple apple : apples) {
            if (isAppleCollisionDetected(apple, sheepEntity)) {
                DebugManager.log(DebugManager.Category.APPLE_COLLECTION, "Detected apple collision for player: " + player.getName() + " at location: " + apple.getLocation());
                collidedApples.add(apple);
            }
        }
        return collidedApples;
    }

    /**
     * Checks if a collision has occurred between an apple and the snake's head.
     *
     * @param apple       The apple entity.
     * @param sheepEntity The entity representing the snake's head.
     * @return true if a collision is detected, false otherwise.
     */
    private boolean isAppleCollisionDetected(Apple apple, Entity sheepEntity) {
        Location appleLocation = apple.getLocation();
        Location sheepLocation = sheepEntity.getLocation();
        return appleLocation != null &&
                appleLocation.getBlockX() == sheepLocation.getBlockX() &&
                appleLocation.getBlockZ() == sheepLocation.getBlockZ();
    }

    /**
     * Handles the apples that have collided with the snake's head and performs all related actions.
     *
     * @param collidedApples List of apples that have collided.
     * @param player         The player controlling the snake.
     * @param plugin         The JavaPlugin instance for accessing game configurations.
     */
    private void handleCollidedApplesAndActions(List<Apple> collidedApples, Player player, JavaPlugin plugin) {
        List<Apple> apples = gameManager.getPlayerApples().getOrDefault(player, new ArrayList<>());

        for (Apple apple : collidedApples) {
            DebugManager.log(DebugManager.Category.APPLE_COLLECTION, "Handling collided apple for player: " + player.getName());
            // Clear the apple from the game
            apple.clear();

            // Update the player's score
            gameManager.updatePlayerScore(player);

            // Add a new segment to the snake
            gameManager.addSnakeSegment(player, plugin);

            // Play the level-up sound
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);

            // Remove the apple from the list
            apples.remove(apple);
        }

        // Update the playerApples map
        gameManager.getPlayerApples().put(player, apples);
    }

    /**
     * Spawns new apples based on the number of apples collected.
     *
     * @param sheepEntity The entity representing the snake's head.
     * @param player      The player controlling the snake.
     * @param plugin      The JavaPlugin instance for accessing game configurations.
     */
    private void spawnNewApples(Entity sheepEntity, Player player, JavaPlugin plugin) {
        Main mainPlugin = (Main) plugin;
        int maxApples = mainPlugin.getMaxApplesPerGame();

        List<Apple> apples = gameManager.getPlayerApples().getOrDefault(player, new ArrayList<>());

        // Calculate the number of apples to spawn based on the current list
        int applesToSpawn = maxApples - apples.size();

        for (int i = 0; i < applesToSpawn; i++) {
            DebugManager.log(DebugManager.Category.APPLE_COLLECTION, "Attempting to spawn " + applesToSpawn + " new apples for player: " + player.getName());
            Apple newApple = new Apple(plugin, gameManager);
            newApple.spawnWithName(sheepEntity.getLocation(), sheepEntity.getLocation().getBlockY(), player.getName());
            apples.add(newApple);
        }

        // Update the playerApples map
        gameManager.getPlayerApples().put(player, apples);
    }
}