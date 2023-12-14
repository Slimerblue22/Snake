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
     * Checks if the player is in a game region on join and teleports them to the linked lobby region.
     *
     * @param event The PlayerJoinEvent triggered when a player joins the game.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Get the region helpers
        WGHelpers wgHelpers = WGHelpers.getInstance();
        RegionHelpers regionHelpers = RegionHelpers.getInstance();

        // Check the current region of the player
        String currentRegion = wgHelpers.getPlayerCurrentRegion(player);
        if (currentRegion != null && regionHelpers.isRegionRegistered(currentRegion)) {
            String regionType = regionHelpers.getRegionType(currentRegion);

            // Check if the player is in a game region
            if ("game".equals(regionType)) {
                // Find the linked lobby region
                String linkedLobbyRegion = regionHelpers.getLinkedRegion(currentRegion);
                if (linkedLobbyRegion != null) {
                    World lobbyWorld = regionHelpers.getRegionWorld(linkedLobbyRegion);
                    Location lobbyTeleportLocation = regionHelpers.getRegionTeleportLocation(linkedLobbyRegion, lobbyWorld);

                    // Teleport the player if the location is valid
                    if (lobbyTeleportLocation != null) {
                        player.teleport(lobbyTeleportLocation);
                        DebugManager.log(DebugManager.Category.DEBUG, "Player: " + player.getName() + " with UUID of " + player.getUniqueId() + " logged in inside of a game zone. Teleporting them into the linked lobby.");
                    }
                }
            }
        }
    }
}