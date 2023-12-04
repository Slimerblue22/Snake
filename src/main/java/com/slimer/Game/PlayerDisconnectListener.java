package com.slimer.Game;

import com.slimer.Util.DebugManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerDisconnectListener implements Listener {
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GameSessionManager sessionManager = GameSessionManager.getInstance();

        if (sessionManager.isGameActive(player)) {
            // If the player is in a game, handle their removal
            DebugManager.log(DebugManager.Category.SESSION_MANAGER, "Game stopped for disconnected player: " + player.getName() + " with UUID of " + player.getUniqueId());
            sessionManager.stopGame(player);
        }
    }
}