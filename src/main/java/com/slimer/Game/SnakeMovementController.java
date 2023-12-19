package com.slimer.Game;

import com.slimer.Main;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.util.Vector;

public class SnakeMovementController {
    private final SnakeLifecycleManager snakeLifecycleManager;
    private final double SPEED_BPS;

    public SnakeMovementController(SnakeLifecycleManager snakeLifecycleManager, Main main) {
        this.snakeLifecycleManager = snakeLifecycleManager;
        this.SPEED_BPS = main.getSnakeSpeed();
    }

    public void moveSnake(Player player) {
        Sheep snake = snakeLifecycleManager.getSnakeForPlayer(player);
        if (snake != null) {
            Vector direction = getCardinalDirectionVector(player.getLocation().getYaw());
            Vector velocity = direction.multiply(SPEED_BPS / 20.0); // Convert BPS to blocks per tick
            snake.setVelocity(velocity);
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