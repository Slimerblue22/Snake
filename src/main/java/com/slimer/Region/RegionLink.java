package com.slimer.Region;

import org.bukkit.Location;

/**
 * Represents a link between two regions in the game, typically a lobby and a game region.
 * The class encapsulates the names of the linked regions and their associated teleport locations.
 * It provides getters and setters to access and modify the teleport locations.
 */
public class RegionLink {
    private final String lobbyRegionName;
    private final String gameRegionName;
    private Location lobbyTeleportLocation;
    private Location gameTeleportLocation;

    /**
     * Constructs a new RegionLink object with the given lobby and game region names and teleport locations.
     *
     * @param lobbyRegionName       Name of the lobby region.
     * @param gameRegionName        Name of the game region.
     * @param lobbyTeleportLocation Teleport location for the lobby region.
     * @param gameTeleportLocation  Teleport location for the game region.
     */
    public RegionLink(String lobbyRegionName, String gameRegionName, Location lobbyTeleportLocation, Location gameTeleportLocation) {
        this.lobbyRegionName = lobbyRegionName;
        this.gameRegionName = gameRegionName;
        this.lobbyTeleportLocation = lobbyTeleportLocation;
        this.gameTeleportLocation = gameTeleportLocation;
    }

    public String getLobbyRegionName() {
        return lobbyRegionName;
    }

    public String getGameRegionName() {
        return gameRegionName;
    }

    public Location getLobbyTeleportLocation() {
        return lobbyTeleportLocation;
    }

    /**
     * Sets the teleport location for the lobby region.
     *
     * @param lobbyTeleportLocation New teleport location for the lobby region.
     */
    public void setLobbyTeleportLocation(Location lobbyTeleportLocation) {
        this.lobbyTeleportLocation = lobbyTeleportLocation;
    }

    public Location getGameTeleportLocation() {
        return gameTeleportLocation;
    }

    /**
     * Sets the teleport location for the game region.
     *
     * @param gameTeleportLocation New teleport location for the game region.
     */
    public void setGameTeleportLocation(Location gameTeleportLocation) {
        this.gameTeleportLocation = gameTeleportLocation;
    }
}
