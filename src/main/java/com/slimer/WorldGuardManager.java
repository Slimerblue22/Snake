package com.slimer;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Objects;

// Manages interactions with WorldGuard for region queries, player checks, and location updates.
public class WorldGuardManager {
    private final RegionQuery query; // Handles queries to the regions in WorldGuard.
    private final String lobbyRegionName; // Name of the lobby region in WorldGuard.
    private final String gameZoneRegionName; // Name of the game zone region in WorldGuard.
    private final RegionContainer container; // Container for WorldGuard regions.
    private final SnakePlugin plugin; // Main plugin instance.

    // Initializes WorldGuard regions and configurations.
    public WorldGuardManager(SnakePlugin plugin) {
        this.plugin = plugin;
        updateLocations();
        this.container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        this.query = container.createQuery();
        this.lobbyRegionName = plugin.getConfig().getString("Lobby", "lobby");
        this.gameZoneRegionName = plugin.getConfig().getString("Gamezone", "gamezone");
    }

    // Checks if player is in the lobby region.
    public boolean isPlayerInLobby(Player player) {
        Location loc = player.getLocation();
        ApplicableRegionSet set = getApplicableRegions(loc);
        for (ProtectedRegion region : set.getRegions()) {
            if (region.getId().equalsIgnoreCase(lobbyRegionName)) {
                return true;
            }
        }
        return false;
    }
    // Teleports player to game location, sends error if location isn't set correctly.
    public boolean teleportToGame(Player player, boolean validateOnly) {
        Location gameLocation = plugin.getGameLocation();
        if (gameLocation.equals(new Location(player.getWorld(), 0, 0, 0))) {
            return false;
        }

        // Set the YAW and PITCH of the gameLocation to match the player's current YAW and PITCH.
        if (!validateOnly) {
            gameLocation.setYaw(player.getLocation().getYaw());
            gameLocation.setPitch(player.getLocation().getPitch());
            player.teleport(gameLocation);
        }

        return true;
    }

    // Teleports player to the lobby, sends error if location isn't set correctly.
    public boolean teleportToLobby(Player player, boolean validateOnly) {
        Location lobbyLocation = plugin.getLobbyLocation();
        if (lobbyLocation.equals(new Location(player.getWorld(), 0, 0, 0))) {
            return false;
        }

        // Set the YAW and PITCH of the lobbyLocation to match the player's current YAW and PITCH.
        if (!validateOnly) {
            lobbyLocation.setYaw(player.getLocation().getYaw());
            lobbyLocation.setPitch(player.getLocation().getPitch());
            player.teleport(lobbyLocation);
        }

        return true;
    }

    // Updates game and lobby locations based on configurations.
    public void updateLocations() {
        if (plugin == null) {
            Bukkit.getLogger().warning("Plugin instance is null in WorldGuardManager!");
            return;
        }
        Location gameLocation = new Location(Bukkit.getWorld(Objects.requireNonNull(plugin.getConfig().getString("world"))), plugin.getConfig().getDouble("teleportLocations.game.x"), plugin.getConfig().getDouble("teleportLocations.game.y"), plugin.getConfig().getDouble("teleportLocations.game.z"));
        Location lobbyLocation = new Location(Bukkit.getWorld(Objects.requireNonNull(plugin.getConfig().getString("world"))), plugin.getConfig().getDouble("teleportLocations.lobby.x"), plugin.getConfig().getDouble("teleportLocations.lobby.y"), plugin.getConfig().getDouble("teleportLocations.lobby.z"));

        plugin.setGameLocation(gameLocation);
        plugin.setLobbyLocation(lobbyLocation);
    }

    // Gets regions applicable to a location.
    private ApplicableRegionSet getApplicableRegions(Location location) {
        return query.getApplicableRegions(BukkitAdapter.adapt(location));
    }

    // Gets minimum point of game zone region.
    public Location getGameZoneMinimum() {
        ProtectedRegion gameZone = getRegion(gameZoneRegionName);
        if (gameZone != null) {
            return BukkitAdapter.adapt(Objects.requireNonNull(plugin.getServer().getWorld("world")), gameZone.getMinimumPoint());
        }
        return null;
    }

    // Gets maximum point of game zone region.
    public Location getGameZoneMaximum() {
        ProtectedRegion gameZone = getRegion(gameZoneRegionName);
        if (gameZone != null) {
            return BukkitAdapter.adapt(Objects.requireNonNull(plugin.getServer().getWorld("world")), gameZone.getMaximumPoint());
        }
        return null;
    }

    // Fetches a region by its name.
    private ProtectedRegion getRegion(String regionName) {
        return Objects.requireNonNull(container.get(BukkitAdapter.adapt(Objects.requireNonNull(plugin.getServer().getWorld("world"))))).getRegion(regionName);
    }
    public boolean isLocationInRegion(Location location, String regionName) {
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(location.getWorld()));

        if (!Objects.requireNonNull(regionManager).hasRegion(regionName)) {
            return false;
        }

        ProtectedRegion region = regionManager.getRegion(regionName);
        BlockVector3 locVector = BukkitAdapter.asBlockVector(location);

        return !Objects.requireNonNull(region).contains(locVector);
    }
}
