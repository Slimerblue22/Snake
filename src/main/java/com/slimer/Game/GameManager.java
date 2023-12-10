package com.slimer.Game;

import com.slimer.Util.DebugManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the logic for starting and stopping game sessions.
 * Utilizes GameSessionManager to store and retrieve session data.
 * <p>
 * Last updated: V2.1.0
 *
 * @author Slimerblue22
 */
public class GameManager {

    private final GameSessionManager sessionManager;

    /**
     * Constructor for GameManager.
     * Initializes a connection to the GameSessionManager singleton.
     */
    public GameManager() {
        this.sessionManager = GameSessionManager.getInstance();
    }

    /**
     * Starts a game session for the specified player.
     * Teleports the player to the game location and updates the session data.
     *
     * @param player                The player for whom the game session is to be started.
     * @param gameTeleportLocation  The location to teleport the player for the game.
     * @param lobbyTeleportLocation The location to teleport the player back to the lobby.
     */
    public void startGame(Player player, Location gameTeleportLocation, Location lobbyTeleportLocation) {
        // Checking if player already has an active game
        if (sessionManager.isGameActive(player)) {
            player.sendMessage(Component.text("You already have an active game!", NamedTextColor.RED));
            DebugManager.log(DebugManager.Category.GAME_MANAGER, "Attempted to start a game for " + player.getName() + " with UUID of " + player.getUniqueId() + " but a game was already active.");
            return;
        }

        // Adding player to the active games list
        HashMap<String, Location> locations = new HashMap<>();
        locations.put("game", gameTeleportLocation);
        locations.put("lobby", lobbyTeleportLocation);
        sessionManager.setActiveGame(player, locations);

        // Teleporting the player to the game
        player.teleport(gameTeleportLocation);

        // Informing the player that the game has started
        player.sendMessage(Component.text("Your game has started!", NamedTextColor.GREEN));
        DebugManager.log(DebugManager.Category.GAME_MANAGER, "Game started for player: " + player.getName() + " with UUID of " + player.getUniqueId());
    }

    /**
     * Stops the active game session for the specified player.
     * Teleports the player back to the lobby and clears their session data.
     *
     * @param player The player whose game session is to be stopped.
     */
    public void stopGame(Player player) {
        // Checking if player has a game to stop
        if (!sessionManager.isGameActive(player)) {
            DebugManager.log(DebugManager.Category.GAME_MANAGER, "Attempted to stop a game for " + player.getName() + " with UUID of " + player.getUniqueId() + " but no game was active.");
            return;
        }

        // Retrieving lobby location and teleporting player
        Map<String, Location> locations = sessionManager.getActiveGameData(player);
        if (locations != null && locations.get("lobby") != null) {
            player.teleport(locations.get("lobby"));
        }

        // Removing player from the active games list
        sessionManager.removeActiveGame(player);

        // Informing the player that their game has been stopped
        player.sendMessage(Component.text("Your game has been stopped!", NamedTextColor.GREEN));
        DebugManager.log(DebugManager.Category.GAME_MANAGER, "Game stopped for player: " + player.getName() + " with UUID of " + player.getUniqueId());
    }
}