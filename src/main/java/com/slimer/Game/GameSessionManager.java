package com.slimer.Game;

import com.slimer.Util.DebugManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages game sessions within the plugin.
 * This class provides functionality to start, stop, and manage game sessions for players.
 * It follows the Singleton design pattern to ensure only one instance of this manager exists.
 * <p>
 * Usage:
 * <ul>
 *   <li>To obtain an instance of this manager: {@code GameSessionManager.getInstance()}.</li>
 *   <li>To check if a player has an active game: {@code isGameActive(Player player)}.</li>
 *   <li>To start a new game for a player: {@code startGame(Player player, Location gameTeleportLocation, Location lobbyTeleportLocation)}.</li>
 *   <li>To stop an active game for a player: {@code stopGame(Player player)}.</li>
 * </ul>
 * <p>
 * Last updated: V2.1.0
 *
 * @author Slimerblue22
 */
public class GameSessionManager {
    private static GameSessionManager instance;
    private final HashMap<UUID, Map<String, Location>> activeGames;

    /**
     * Private constructor to enforce the Singleton pattern.
     * Initializes the map that holds active game sessions.
     */
    private GameSessionManager() {
        activeGames = new HashMap<>();
    }

    /**
     * Retrieves the singleton instance of the GameSessionManager.
     * If the instance does not exist, it is created.
     *
     * @return The singleton instance of the GameSessionManager.
     */
    public static GameSessionManager getInstance() {
        if (instance == null) {
            instance = new GameSessionManager();
        }
        return instance;
    }

    /**
     * Checks if the specified player has an active game session.
     *
     * @param player The player whose game session status is to be checked.
     * @return {@code true} if the player has an active game, {@code false} otherwise.
     * Returns {@code false} if the player parameter is null.
     */
    public boolean isGameActive(Player player) {
        if (player == null) {
            DebugManager.log(DebugManager.Category.SESSION_MANAGER, "Player object is null in isGameActive method.");
            return false;
        }
        return activeGames.containsKey(player.getUniqueId());
    }

    /**
     * Starts a new game session for the specified player.
     * If the player already has an active game, a message is sent to them and the game is not started.
     *
     * @param player                The player for whom the game session is to be started.
     * @param gameTeleportLocation  The location to teleport the player for the game.
     * @param lobbyTeleportLocation The location to teleport the player back to the lobby.
     */
    public void startGame(Player player, Location gameTeleportLocation, Location lobbyTeleportLocation) {
        if (player == null || gameTeleportLocation == null || lobbyTeleportLocation == null) {
            DebugManager.log(DebugManager.Category.SESSION_MANAGER, "Null parameter(s) provided in startGame method.");
            return;
        }

        if (isGameActive(player)) {
            player.sendMessage(Component.text("You already have an active game!", NamedTextColor.RED));
            DebugManager.log(DebugManager.Category.SESSION_MANAGER, "Attempted to start a game for " + player.getName() + " with UUID of " + player.getUniqueId() + " but a game was already active.");
            return;
        }

        // Adding player to the active games list
        HashMap<String, Location> locations = new HashMap<>();
        locations.put("game", gameTeleportLocation);
        locations.put("lobby", lobbyTeleportLocation);
        activeGames.put(player.getUniqueId(), locations);

        // Teleporting the player to the game location
        if (!player.teleport(gameTeleportLocation)) {
            DebugManager.log(DebugManager.Category.SESSION_MANAGER, "Failed to teleport player " + player.getName() + " to game location.");
        }

        // Informing the player that the game has started
        player.sendMessage(Component.text("Your game has started!", NamedTextColor.GREEN));
        DebugManager.log(DebugManager.Category.SESSION_MANAGER, "Game started for player: " + player.getName() + " with UUID of " + player.getUniqueId());
    }

    /**
     * Stops the active game session for the specified player.
     * If the player does not have an active game, a message is sent to them.
     *
     * @param player The player whose game session is to be stopped.
     */
    public void stopGame(Player player) {
        if (player == null) {
            DebugManager.log(DebugManager.Category.SESSION_MANAGER, "Player object is null in stopGame method.");
            return;
        }

        if (!isGameActive(player)) {
            player.sendMessage(Component.text("You don't have an active game to stop!", NamedTextColor.RED));
            DebugManager.log(DebugManager.Category.SESSION_MANAGER, "Attempted to stop a game for " + player.getName() + " with UUID of " + player.getUniqueId() + " but no game was active.");
            return;
        }

        // Retrieving game locations and teleporting player to the lobby
        Map<String, Location> locations = activeGames.get(player.getUniqueId());
        if (locations != null && locations.get("lobby") != null) {
            player.teleport(locations.get("lobby"));
        } else {
            DebugManager.log(DebugManager.Category.SESSION_MANAGER, "Lobby location is null or locations map is null for player " + player.getName() + " with UUID of " + player.getUniqueId());
        }

        // Removing player from the active games list
        activeGames.remove(player.getUniqueId());

        // Informing the player that their game has been stopped
        player.sendMessage(Component.text("Your game has been stopped!", NamedTextColor.GREEN));
        DebugManager.log(DebugManager.Category.SESSION_MANAGER, "Game stopped for player: " + player.getName() + " with UUID of " + player.getUniqueId());
    }
}