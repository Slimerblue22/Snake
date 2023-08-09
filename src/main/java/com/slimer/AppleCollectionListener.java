package com.slimer;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
public class AppleCollectionListener implements Listener {
    private final SnakePlugin plugin;
    private final GameManager gameManager;
    private final WorldGuardManager worldGuardManager;

    // The constructor of AppleCollectionListener, initializing the plugin and gameManager.
    public AppleCollectionListener(SnakePlugin plugin, GameManager gameManager, WorldGuardManager worldGuardManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.worldGuardManager = worldGuardManager;
    }
    // This method handles the event when an entity changes a block. It checks if the entity is a sheep (representing a part of the snake) that a player is riding. If the block being changed is the block containing the apple, a new apple is spawned and the event is cancelled (to prevent the sheep from actually changing the block).
    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof Sheep sheep)) {
            return;
        }
        if (!(sheep.getPassengers() instanceof Player player)) {
            return;
        }
        Snake snake = gameManager.getGame(player);
        if (snake == null || snake.getApple() == null) {
            return;
        }
        if (event.getBlock().getLocation().equals(snake.getApple().getBlock().getLocation())) {
            Apple newApple = new Apple(gameManager, player, plugin, player.getWorld(), player.getLocation(), snake, worldGuardManager);
            snake.setApple(newApple);
            event.setCancelled(true);
        }
    }
}
