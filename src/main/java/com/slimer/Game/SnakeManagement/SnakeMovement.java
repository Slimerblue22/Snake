package com.slimer.Game.SnakeManagement;

import com.slimer.Game.Listeners.PlayerInputListener;
import com.slimer.Main;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;

public class SnakeMovement {
    private final SnakeLifecycle snakeLifecycle;
    private final double SPEED_BPS;
    private final PlayerInputListener playerInputListener;

    public SnakeMovement(SnakeLifecycle snakeLifecycle, Main main, PlayerInputListener playerInputListener) {
        this.snakeLifecycle = snakeLifecycle;
        this.SPEED_BPS = main.getSnakeSpeed();
        this.playerInputListener = playerInputListener;
    }

    public void moveSnake(Player player) {
        Sheep snake = snakeLifecycle.getSnakeForPlayer(player);
        if (snake != null) {
            Map<UUID, Vector> playerDirections = playerInputListener.getPlayerDirections();
            Vector direction = playerDirections.get(player.getUniqueId());

            if (direction != null) {
                double speedPerTick = SPEED_BPS / 20.0; // Convert BPS to blocks per tick
                Vector movement = direction.normalize().multiply(speedPerTick);
                snake.setVelocity(movement);
            }
        }
    }
}