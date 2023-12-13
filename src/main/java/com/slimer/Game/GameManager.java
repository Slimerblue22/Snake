package com.slimer.Game;

import com.slimer.Region.RegionHelpers;
import com.slimer.Region.WGHelpers;
import com.slimer.Util.DebugManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
     * Initiates a game session for the specified player.
     * This method first performs a series of pregame checks to ensure the player
     * meets all the criteria to start a game. If the checks pass, the player is
     * added to the active games list, teleported to the game location, and informed
     * that their game has started.
     *
     * @param player The player for whom the game session is to be initiated.
     */
    public void startGame(Player player) {
        // Run pregame checks
        HashMap<String, Location> locations = performPregameChecks(player);
        if (locations == null) {
            return; // Pregame checks failed, stop the method
        }

        // Adding player to the active games list
        sessionManager.setActiveGame(player, locations);

        // Teleporting the player to the game
        player.teleport(locations.get("game"));

        // Informing the player that the game has started
        player.sendMessage(Component.text("Your game has started!", NamedTextColor.GREEN));
        DebugManager.log(DebugManager.Category.GAME_MANAGER, "Game started for player: " + player.getName() + " with UUID of " + player.getUniqueId());
    }

    /**
     * Performs pregame checks for a player attempting to start a game.
     * This method validates several conditions, including whether the player
     * already has an active game, if they are within a registered lobby region,
     * whether the lobby region is linked to a game region, and the existence of
     * valid teleport locations. If any of these checks fail, the method returns
     * null, indicating that the player cannot start the game.
     *
     * @param player The player to perform pregame checks on.
     * @return A HashMap containing the 'game' and 'lobby' teleport locations
     * if all pregame checks pass, or null if any check fails.
     */
    private HashMap<String, Location> performPregameChecks(Player player) {
        // Checking if player already has an active game
        if (sessionManager.isGameActive(player)) {
            player.sendMessage(Component.text("You already have an active game!", NamedTextColor.RED));
            return null;
        }

        // Check if player is inside a lobby region
        WGHelpers wgHelpers = WGHelpers.getInstance();
        RegionHelpers regionHelpers = RegionHelpers.getInstance();

        String currentLobbyRegion = wgHelpers.getPlayerCurrentRegion(player);
        boolean isRegistered = (currentLobbyRegion != null) && regionHelpers.isRegionRegistered(currentLobbyRegion);
        String regionType = isRegistered ? regionHelpers.getRegionType(currentLobbyRegion) : null;

        if (!"lobby".equals(regionType)) {
            player.sendMessage(Component.text("You must be within a lobby region to start the game.", NamedTextColor.RED));
            return null;
        }

        // Check if lobby region is linked
        boolean isLinked = regionHelpers.isRegionLinked(currentLobbyRegion);
        String currentGameRegion = regionHelpers.getLinkedRegion(currentLobbyRegion);

        if (!isLinked || currentGameRegion == null) {
            player.sendMessage(Component.text("The lobby you are in is not properly linked to a game region. You cannot start the game.", NamedTextColor.RED));
            return null;
        }

        // Check for unexpected null locations
        World gameWorld = regionHelpers.getRegionWorld(currentGameRegion);
        World lobbyWorld = regionHelpers.getRegionWorld(currentLobbyRegion);

        Location gameTeleportLocation = regionHelpers.getRegionTeleportLocation(currentGameRegion, gameWorld);
        Location lobbyTeleportLocation = regionHelpers.getRegionTeleportLocation(currentLobbyRegion, lobbyWorld);

        if (gameTeleportLocation == null || lobbyTeleportLocation == null) {
            player.sendMessage(Component.text("Could not find the teleport location for the game or lobby region.", NamedTextColor.RED));
            return null;
        }

        HashMap<String, Location> locations = new HashMap<>();
        locations.put("game", gameTeleportLocation);
        locations.put("lobby", lobbyTeleportLocation);
        return locations;
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
            /*
             * Attempting to teleport an offline player to the lobby location.
             * Issue: WorldGuard's `exit-override` flag, if set to false, prevents
             * teleporting offline players into regions.
             * Possible Solutions:
             * 1. Modify WorldGuard's `exit-override` flag for the affected region to true.
             * This approach requires direct modification of WorldGuard regions, which was
             * previously avoided.
             * 2. Grant players permissions to bypass this check. This is NOT recommended due
             * to various other issues that arise because of that.
             * 3. Implement a system to store players' teleportation data and handle
             * teleportation upon reconnection. This ensures teleportation even after server
             * restarts but requires a persistent storage solution (e.g., file-based or
             * database) to track player locations across sessions.
             *
             * Current Implementation: Teleports player if online, but may fail for offline
             * players due to WorldGuard restrictions.
             */
            player.teleport(locations.get("lobby"));
        }

        // Removing player from the active games list
        sessionManager.removeActiveGame(player);

        // Informing the player that their game has been stopped
        player.sendMessage(Component.text("Your game has been stopped!", NamedTextColor.GREEN));
        DebugManager.log(DebugManager.Category.GAME_MANAGER, "Game stopped for player: " + player.getName() + " with UUID of " + player.getUniqueId());
    }

    /**
     * Stops all currently active game sessions.
     * Iterates through all active game sessions managed by the GameSessionManager,
     * and stops each session. This is typically used during server shutdown to ensure
     * all games are stopped gracefully.
     * <p>
     * This method retrieves each player's UUID from the active games, checks if the
     * player is currently online, and if so, stops their game session. Additionally,
     * it logs the action for each stopped game if the `GAME_MANAGER` debug category is enabled.
     */
    public void stopAllGames() {
        for (UUID playerId : sessionManager.getActiveGameIds()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                DebugManager.log(DebugManager.Category.GAME_MANAGER,"Detected active game for player: " + player.getName() + " with UUID of " + player.getUniqueId() + " during shutdown. Stopping game!");
                stopGame(player);
            }
        }
    }
}