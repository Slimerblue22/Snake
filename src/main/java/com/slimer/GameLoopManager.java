package com.slimer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameLoopManager {
    private final GameManager gameManager;
    private long moveInterval;  // Interval at which the game loop executes

    public GameLoopManager(GameManager gameManager) {
        this.gameManager = gameManager;
        long configuredSpeed = gameManager.getPlugin().getConfig().getLong("snake-speed", 10L);
        this.moveInterval = Math.max(configuredSpeed, 5L);
        this.initializeGameLoop(); // Start the game loop
    }

    // Sets the speed of the game loop
    public void setSpeed(int speed) {
        this.moveInterval = speed;
    }

    // Initializes the game loop with the configured speed
    private void initializeGameLoop() {
        gameManager.getPlugin().getServer().getScheduler().runTaskTimer(gameManager.getPlugin(), this::gameLoop, 0L, moveInterval);
    }

    // Main game loop logic
    private void gameLoop() {
        List<Player> playersToEndGame = new ArrayList<>();
        for (Map.Entry<Player, Snake> entry : gameManager.getActiveGames().entrySet()) {
            Snake snake = entry.getValue();
            Player player = entry.getKey();
            snake.move();   // Move the snake
            ArmorStand playerArmorStand = snake.getArmorStand();
            // Check for conditions that should end the game
            // Conditions include player not riding armor stand, snake outside the game zone, snake colliding with solid blocks, etc.
            if (!playerArmorStand.getPassengers().contains(player)) {
                playersToEndGame.add(player);
                continue;
            }
            Location snakeHeadLocation = snake.getBody().getFirst().getLocation();
            if (gameManager.getWorldGuardManager().isLocationOutsideRegion(snakeHeadLocation, gameManager.getPlugin().getConfig().getString("Gamezone"))) {
                playersToEndGame.add(player);
                continue;
            }
            Location newLocation = snake.getBody().getFirst().getLocation();
            if (!gameManager.getPlugin().getNonSolidBlocks().contains(newLocation.getBlock().getType())) {
                playersToEndGame.add(player);
            } else {
                Location blockUnderneath = newLocation.clone().subtract(0, 1, 0);
                Material blockType = blockUnderneath.getBlock().getType();
                if (blockType == Material.AIR || blockType == Material.WATER || blockType == Material.LAVA) {
                    playersToEndGame.add(player);
                } else if (!snake.isAlive()) {
                    playersToEndGame.add(player);
                }
            }
        }
        // End the game for players who met the game-ending conditions
        for (Player player : playersToEndGame) {
            gameManager.endGame(player);
        }
    }
}