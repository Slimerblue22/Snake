package com.slimer.Game;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages game sessions and their associated data within the plugin.
 * It follows the Singleton design pattern to ensure only one instance of this manager exists.
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
     *         Returns {@code false} if the player parameter is null.
     */
    public boolean isGameActive(Player player) {
        return player != null && activeGames.containsKey(player.getUniqueId());
    }

    /**
     * Retrieves a set of UUIDs representing all players with active game sessions.
     * This method is used to obtain a collection of all players who are currently
     * participating in a game, typically for purposes of iteration or batch processing.
     * <p>
     * The method does not perform any modifications to the active game sessions; it
     * merely provides a read-only view of the current active session identifiers.
     *
     * @return A Set of UUIDs for all players with active game sessions.
     */
    public Set<UUID> getActiveGameIds() {
        return activeGames.keySet();
    }

    /**
     * Sets or updates the game session data for the specified player.
     *
     * @param player   The player whose game session data is to be set.
     * @param gameData A map containing game session data such as locations.
     */
    public void setActiveGame(Player player, Map<String, Location> gameData) {
        if (player != null) {
            activeGames.put(player.getUniqueId(), gameData);
        }
    }

    /**
     * Removes the active game session for the specified player.
     *
     * @param player The player whose active game session is to be removed.
     */
    public void removeActiveGame(Player player) {
        if (player != null) {
            activeGames.remove(player.getUniqueId());
        }
    }

    /**
     * Retrieves the game session data for the specified player.
     *
     * @param player The player whose game session data is requested.
     * @return A map containing the player's game session data, or {@code null} if the player is not found or is null.
     */
    public Map<String, Location> getActiveGameData(Player player) {
        return player != null ? activeGames.get(player.getUniqueId()) : null;
    }
}