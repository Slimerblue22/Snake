package com.slimer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GameLoopManager {
    private final GameManager gameManager;
    private final WorldGuardManager worldGuardManager;
    private long moveInterval;  // Interval at which the game loop executes
    private int wolfMoveCounter = 0;
    private final SnakePlugin plugin;

    public GameLoopManager(GameManager gameManager, SnakePlugin plugin, WorldGuardManager worldGuardManager) {
        this.gameManager = gameManager;
        this.worldGuardManager = worldGuardManager;
        this.plugin = plugin;
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
        // List to store players whose games need to be ended
        List<Player> playersToEndGame = new ArrayList<>();

        // Iterate through active games
        for (Map.Entry<Player, Snake> entry : gameManager.getActiveGames().entrySet()) {
            Player player = entry.getKey();
            Snake snake = entry.getValue();

            // Move the snake
            snake.move();

            // Handle wolf chase logic if enabled
            if (this.plugin.isWolfChaseEnabled()) {
                handleWolfChaseLogic(player, snake, playersToEndGame);
            }

            // Check for conditions that should end the game
            checkGameEndingConditions(player, snake, playersToEndGame);
        }

        // End the game for players who met the game-ending conditions
        for (Player player : playersToEndGame) {
            gameManager.endGame(player);
        }
    }

    private void handleWolfChaseLogic(Player player, Snake snake, List<Player> playersToEndGame) {
        // Wolf movement logic
        wolfMoveCounter++;
        WolfChase wolfChase = gameManager.getWolfChase(player);
        if (wolfChase != null) {
            wolfChase.setAngry(true); // Keep the wolf angry
            if (wolfMoveCounter >= 3) {
                Location snakeHeadLocation = snake.getBody().getFirst().getLocation();
                LinkedList<Sheep> snakeBody = snake.getBody();
                wolfChase.moveTowards(snakeHeadLocation, snakeBody); // Move the wolf towards the closest part of the snake
                wolfMoveCounter = 0; // Reset the counter
            }
        }

        // Check collision with wolf using exact coordinates
        if (wolfChase != null) {
            for (Sheep segment : snake.getBody()) {
                Location segmentLocation = segment.getLocation();
                if (wolfChase.isCollidingWithWolf(segmentLocation)) {
                    playersToEndGame.add(player);
                    break; // No need to check further segments if one collides
                }
            }
        }
    }

    private void checkGameEndingConditions(Player player, Snake snake, List<Player> playersToEndGame) {
        ArmorStand playerArmorStand = snake.getArmorStand();

        // Check if player is no longer riding the armor stand
        if (!playerArmorStand.getPassengers().contains(player)) {
            playersToEndGame.add(player);
            return;
        }

        // Check if the snake is outside the game zone
        String gameZoneName = gameManager.getGameZoneForPlayer(player);
        if (gameZoneName != null) {
            Location[] bounds = worldGuardManager.getGameZoneBounds(gameZoneName);
            if (bounds != null) {
                Location min = bounds[0];
                Location max = bounds[1];
                Location snakeHeadLocation = snake.getBody().getFirst().getLocation();
                if (!worldGuardManager.isLocationWithinBounds(snakeHeadLocation, min, max)) {
                    playersToEndGame.add(player);
                    return;
                }
            }
        }

        // Check if the snake is colliding with solid blocks or if it's over invalid terrain
        Location newLocation = snake.getBody().getFirst().getLocation();
        if (!gameManager.getPlugin().getNonSolidBlocks().contains(newLocation.getBlock().getType())) {
            playersToEndGame.add(player);
            return;
        }

        Location blockUnderneath = newLocation.clone().subtract(0, 1, 0);
        Material blockType = blockUnderneath.getBlock().getType();
        if (blockType == Material.AIR || blockType == Material.WATER || blockType == Material.LAVA || !snake.isAlive()) {
            playersToEndGame.add(player);
        }
    }
}