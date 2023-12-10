package com.slimer.Game;

import com.slimer.Util.DebugManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens to player disconnect events in a multiplayer game environment.
 * This class is responsible for handling the actions to be taken when a player disconnects from the server.
 * It ensures that any active game sessions associated with the disconnecting player are properly terminated.
 * <p>
 * Last updated: V2.1.0
 *
 * @author Slimerblue22
 */
public class PlayerDisconnectListener implements Listener {

    private final GameManager gameManager;

    public PlayerDisconnectListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Handles the player quit event.
     * When a player disconnects, this method checks if they have an active game session and stops it.
     *
     * @param event The PlayerQuitEvent triggered when a player leaves the game.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GameSessionManager sessionManager = GameSessionManager.getInstance();

        if (sessionManager.isGameActive(player)) {
            // If the player is in a game, handle their removal
            DebugManager.log(DebugManager.Category.DISCONNECT_LISTENER, "Game stopped for disconnected player: " + player.getName() + " with UUID of " + player.getUniqueId());
            gameManager.stopGame(player);
        }
    }
}