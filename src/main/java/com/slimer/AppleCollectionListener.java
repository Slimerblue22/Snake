package com.slimer;

import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

public class AppleCollectionListener implements Listener {
    // Class fields to manage the plugin, game, and world.
    private final SnakePlugin plugin;
    private final GameManager gameManager;
    private final WorldGuardManager worldGuardManager;

    // Constructor to initialize the listener with necessary dependencies.
    public AppleCollectionListener(SnakePlugin plugin, GameManager gameManager, WorldGuardManager worldGuardManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.worldGuardManager = worldGuardManager;
    }

    // Event handler for when an entity changes a block (e.g., a sheep eats grass).
    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        // Check if the entity involved is a sheep.
        if (!(event.getEntity() instanceof Sheep sheep)) {
            return;
        }
        // Check if the sheep has a player passenger (i.e., the player controlling the snake).
        if (!(sheep.getPassengers() instanceof Player player)) {
            return;
        }
        // Retrieve the Snake object corresponding to the player.
        Snake snake = gameManager.getGame(player);
        if (snake == null || snake.getApple() == null) {
            return;
        }
        // Check if the block being changed is the apple's location.
        if (event.getBlock().getLocation().equals(snake.getApple().getBlock().getLocation())) {
            // Spawn a new apple and update the snake's apple reference.
            Apple newApple = new Apple(gameManager, player, plugin, player.getWorld(), player.getLocation(), snake, worldGuardManager);
            snake.setApple(newApple);
            // Cancel the block change event to prevent the apple block from being altered.
            event.setCancelled(true);
        }
    }
}
