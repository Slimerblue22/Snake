package com.slimer.Game;

import com.slimer.Region.RegionHelpers;
import com.slimer.Region.WGHelpers;
import com.slimer.Util.DebugManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
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
     * @param player The player for whom the game session is to be started.
     */
    public void startGame(Player player) {
        // Checking if player already has an active game
        if (sessionManager.isGameActive(player)) {
            player.sendMessage(Component.text("You already have an active game!", NamedTextColor.RED));
            DebugManager.log(DebugManager.Category.GAME_MANAGER, "Attempted to start a game for " + player.getName() + " with UUID of " + player.getUniqueId() + " but a game was already active.");
            return;
        }

        // Check if player is inside a lobby region
        WGHelpers wgHelpers = WGHelpers.getInstance();
        RegionHelpers regionHelpers = RegionHelpers.getInstance();

        String currentLobbyRegion = wgHelpers.getPlayerCurrentRegion(player);
        boolean isRegistered = (currentLobbyRegion != null) && regionHelpers.isRegionRegistered(currentLobbyRegion);
        String regionType = isRegistered ? regionHelpers.getRegionType(currentLobbyRegion) : null;

        if (!"lobby".equals(regionType)) {
            player.sendMessage(Component.text("You must be within a lobby region to start the game.", NamedTextColor.RED));
            return;
        }

        // Check if lobby region is linked
        boolean isLinked = regionHelpers.isRegionLinked(currentLobbyRegion);
        String currentGameRegion = regionHelpers.getLinkedRegion(currentLobbyRegion);

        if (!isLinked || currentGameRegion == null) {
            player.sendMessage(Component.text("The lobby you are in is not properly linked to a game region. You cannot start the game.", NamedTextColor.RED));
            return;
        }

        // Check for unexpected null locations
        World gameWorld = regionHelpers.getRegionWorld(currentGameRegion);
        World lobbyWorld = regionHelpers.getRegionWorld(currentLobbyRegion);

        Location gameTeleportLocation = regionHelpers.getRegionTeleportLocation(currentGameRegion, gameWorld);
        Location lobbyTeleportLocation = regionHelpers.getRegionTeleportLocation(currentLobbyRegion, lobbyWorld);

        if (gameTeleportLocation == null || lobbyTeleportLocation == null) {
            player.sendMessage(Component.text("Could not find the teleport location for the game or lobby region.", NamedTextColor.RED));
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