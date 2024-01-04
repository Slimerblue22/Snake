package com.slimer.Game.AppleManagement;

import com.slimer.Game.ScoreManager;
import com.slimer.Game.SnakeManagement.SnakeLifecycle;
import com.slimer.Util.DebugManager;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ArmorStand;

import java.util.Map;
import java.util.UUID;

public class AppleCollection {
    private final AppleLifecycle appleLifecycle;
    private final SnakeLifecycle snakeLifecycle;
    private final ScoreManager scoreManager;
    private static final String APPLE_COLLECTED_LOG = "Apple collected by player: %s, Apple Location: %s, Snake Location: %s";

    public AppleCollection(AppleLifecycle appleLifecycle, SnakeLifecycle snakeLifecycle, ScoreManager scoreManager) {
        this.appleLifecycle = appleLifecycle;
        this.snakeLifecycle = snakeLifecycle;
        this.scoreManager = scoreManager;
    }

    public void collectApple(Player player, String worldName, String gameRegionName) {
        Map<UUID, ArmorStand> apples = appleLifecycle.getPlayerApples();
        UUID playerId = player.getUniqueId();

        // Retrieve the snake entity for the player
        Entity snake = snakeLifecycle.getSnakeForPlayer(player);

        // Check if there is an apple for this player and if the snake exists
        if (apples.containsKey(playerId) && snake != null) {
            ArmorStand apple = apples.get(playerId);

            // Get the locations of the snake and the apple
            Location snakeLocation = snake.getLocation();
            Location appleLocation = apple.getLocation();

            // Check if the X and Z coordinates match
            if (snakeLocation.getBlockX() == appleLocation.getBlockX() && snakeLocation.getBlockZ() == appleLocation.getBlockZ()) {

                // Remove the existing apple and add a point
                DebugManager.log(DebugManager.Category.APPLE_COLLECTION, String.format(APPLE_COLLECTED_LOG, player.getName(), appleLocation, snakeLocation));
                appleLifecycle.removeAppleForPlayer(player);
                scoreManager.updateScore(player);

                // Add a new apple
                appleLifecycle.createAppleForPlayer(player, worldName, gameRegionName);
            }
        }
    }
}