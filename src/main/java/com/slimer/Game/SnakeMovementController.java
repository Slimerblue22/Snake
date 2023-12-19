package com.slimer.Game;

import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.util.Vector;

public class SnakeMovementController {
    private final SnakeLifecycleManager snakeLifecycleManager;

    public SnakeMovementController(SnakeLifecycleManager snakeLifecycleManager) {
        this.snakeLifecycleManager = snakeLifecycleManager;
    }

    public void moveSnake(Player player) {
        Sheep snake = snakeLifecycleManager.getSnakeForPlayer(player);
        if (snake != null) {
            Vector direction = getCardinalDirectionVector(player.getLocation().getYaw());
            snake.setVelocity(direction);
        }
    }

    private Vector getCardinalDirectionVector(float yaw) {
        yaw = (yaw % 360 + 360) % 360; // Normalize the yaw

        if (yaw >= 315 || yaw < 45) {
            return new Vector(0, 0, 1);  // South
        } else if (yaw >= 45 && yaw < 135) {
            return new Vector(-1, 0, 0);  // East
        } else if (yaw >= 135 && yaw < 225) {
            return new Vector(0, 0, -1);  // North
        } else { // yaw >= 225 && yaw < 315
            return new Vector(1, 0, 0);  // West
        }
    }
}