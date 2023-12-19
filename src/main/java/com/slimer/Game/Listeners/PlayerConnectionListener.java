package com.slimer.Game.Listeners;

import com.slimer.Region.RegionHelpers;
import com.slimer.Region.WGHelpers;
import com.slimer.Util.DebugManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Listens to player connect events in a multiplayer game environment.
 * This class is responsible for handling the actions to be taken when a player connects to the server.
 * It ensures that player's inside linked game zones are teleported back into the lobby zone.
 * This acts as a fail-safe when teleportation for disconnected players in the middle of a game fails.
 * <p>
 * Last updated: V2.1.0
 *
 * @author Slimerblue22
 */
public class PlayerConnectionListener implements Listener {

    /**
     * Handles the player join event.
     * This method serves as an event listener for when a player joins the game.
     *
     * @param event The PlayerJoinEvent triggered when a player joins the game.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        handlePlayerJoin(event.getPlayer());
    }

    /**
     * Checks if the player is in a game region on join and teleports them to the linked lobby region.
     *
     * @param player The player who joined the game.
     */
    private void handlePlayerJoin(Player player) {
        // Get the region helpers
        RegionHelpers regionHelpers = RegionHelpers.getInstance();

        // Check the current region of the player
        String currentRegion = WGHelpers.getPlayerCurrentRegion(player);
        if (currentRegion == null || !regionHelpers.isRegionRegistered(currentRegion)) {
            return; // Early exit if the current region is null or not registered
        }

        String regionType = regionHelpers.getRegionType(currentRegion);
        if (!"game".equals(regionType)) {
            return; // Early exit if the player is not in a game region
        }

        // Find the linked lobby region
        String linkedLobbyRegion = regionHelpers.getLinkedRegion(currentRegion);
        if (linkedLobbyRegion == null) {
            return; // Early exit if there's no linked lobby region
        }

        World lobbyWorld = regionHelpers.getRegionWorld(linkedLobbyRegion);
        Location lobbyTeleportLocation = regionHelpers.getRegionTeleportLocation(linkedLobbyRegion, lobbyWorld);

        // Teleport the player if the location is valid
        if (lobbyTeleportLocation != null) {
            player.teleport(lobbyTeleportLocation);
            DebugManager.log(DebugManager.Category.DEBUG, "Player: " + player.getName() + " with UUID of " + player.getUniqueId() + " logged in inside of a game zone. Teleporting them into the linked lobby.");
        }
    }
}