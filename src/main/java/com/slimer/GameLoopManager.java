package com.slimer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Manages the primary game loop for all snake games.
public class GameLoopManager {
    private final GameManager gameManager;
    private long moveInterval; // Variable to store the move interval.

    // Constructor initializes the game loop manager and starts the game loop.
    public GameLoopManager(GameManager gameManager) {
        this.gameManager = gameManager;

        // Fetch the move interval from the configuration only once.
        long configuredSpeed = gameManager.getPlugin().getConfig().getLong("snake-speed", 10L);
        this.moveInterval = Math.max(configuredSpeed, 5L); // Ensure it's not too fast.
        this.initializeGameLoop();
    }

    // Setter for speed.
    public void setSpeed(int speed) {
        this.moveInterval = speed; // Update moveInterval when speed changes.
    }

    // Initializes and starts the primary game loop.
    private void initializeGameLoop() {
        gameManager.getPlugin().getServer().getScheduler().runTaskTimer(gameManager.getPlugin(), this::gameLoop, 0L, moveInterval);
    }

    private void gameLoop() {
        // List of players whose game should end.
        List<Player> playersToEndGame = new ArrayList<>();

        // Iterate through all active games.
        for (Map.Entry<Player, Snake> entry : gameManager.getActiveGames().entrySet()) {
            Snake snake = entry.getValue();
            Player player = entry.getKey();

            // Move the snake.
            snake.move();

            // Reference to the armor stand the player should be riding.
            ArmorStand playerArmorStand = snake.getArmorStand();

            // End game if player isn't riding the armor stand.
            if (!playerArmorStand.getPassengers().contains(player)) {
                playersToEndGame.add(player);
                continue;
            }

            // Check if snake's head is outside the game zone:
            Location snakeHeadLocation = snake.getBody().getFirst().getLocation();
            if (gameManager.getWorldGuardManager().isLocationInRegion(snakeHeadLocation, gameManager.getPlugin().getConfig().getString("Gamezone"))) {
                playersToEndGame.add(player);
                continue;
            }

            // Check conditions to end game.
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

        // End game for all players in the list.
        for (Player player : playersToEndGame) {
            gameManager.endGame(player);
        }
    }
}