package com.slimer.Game.SnakeManagement;

import com.slimer.Util.DebugManager;
import com.slimer.Util.PlayerData;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;

import java.util.HashMap;
import java.util.UUID;

public class SnakeLifecycle {
    private final HashMap<UUID, Sheep> playerSnakes;
    private static final String SNAKE_CREATED_LOG = "Snake created for player: %s";
    private static final String SNAKE_DESTROYED_LOG = "Snake destroyed for player: %s";

    public SnakeLifecycle() {
        this.playerSnakes = new HashMap<>();
    }

    public void spawnSnakeForPlayer(Player player, Location location) {
        // Adjust the location to center the sheep on the block
        Location spawnLocation = location.clone().add(0.5, 0, 0.5);

        // Spawn a sheep and set necessary properties
        Sheep sheep = player.getWorld().spawn(spawnLocation, Sheep.class);
        DyeColor color = PlayerData.getInstance().getSheepColor(player);

        sheep.setSilent(true);
        sheep.setCollidable(false);
        sheep.setAware(false);
        sheep.setColor(color == null ? DyeColor.WHITE : color);

        // Mount the player on the sheep
        sheep.addPassenger(player);

        // Store the association between the player and the sheep
        playerSnakes.put(player.getUniqueId(), sheep);
        DebugManager.log(DebugManager.Category.DEBUG, String.format(SNAKE_CREATED_LOG, player.getName()));
    }

    public void removeSnakeForPlayer(Player player) {
        Sheep sheep = playerSnakes.remove(player.getUniqueId());
        if (sheep != null) {
            sheep.remove();
            DebugManager.log(DebugManager.Category.DEBUG, String.format(SNAKE_DESTROYED_LOG, player.getName()));
        }
    }

    public Sheep getSnakeForPlayer(Player player) {
        return playerSnakes.get(player.getUniqueId());
    }
}
