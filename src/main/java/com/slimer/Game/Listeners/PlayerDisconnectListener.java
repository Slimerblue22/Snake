package com.slimer.Game.Listeners;

import com.slimer.Game.GameManager;
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
    private static final String GAME_STOPPED_FOR_DISCONNECTED_PLAYER_LOG = "Game stopped for disconnected player: %s";

    public PlayerDisconnectListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Handles the player quit event.
     * This method serves as an event listener for when a player leaves the game.
     *
     * @param event The PlayerQuitEvent triggered when a player leaves the game.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        handlePlayerQuit(event.getPlayer());
    }

    /**
     * Handles the logic when a player quits the game.
     * When a player disconnects, this method checks if they have an active game session and stops it.
     *
     * @param player The player who left the game.
     */
    private void handlePlayerQuit(Player player) {
        if (gameManager.hasActiveGame(player)) {
            // If the player is in a game, handle their removal
            DebugManager.log(DebugManager.Category.DEBUG, String.format(GAME_STOPPED_FOR_DISCONNECTED_PLAYER_LOG, player.getName()));
            gameManager.stopGame(player);
        }
    }
}