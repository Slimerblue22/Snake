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
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Objects;

public class WorldGuardManager {
    private final RegionQuery query; // Query object for region lookup
    private final String lobbyRegionName; // Name of the lobby region
    private final String gameZoneRegionName; // Name of the game zone region
    private final RegionContainer container; // Container for regions
    private final SnakePlugin plugin; // Reference to the main plugin class

    public WorldGuardManager(SnakePlugin plugin) {
        this.plugin = plugin;
        updateLocations(); // Initialize and update locations
        this.container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        this.query = container.createQuery(); // Create query object
        // Read lobby and game zone region names from the configuration
        this.lobbyRegionName = plugin.getConfig().getString("Lobby", "lobby");
        this.gameZoneRegionName = plugin.getConfig().getString("Gamezone", "gamezone");
    }

    // Check if the player is in the lobby region
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

    // Teleport player to the game location, with optional validation
    public boolean teleportToGame(Player player, boolean validateOnly) {
        Location gameLocation = plugin.getGameLocation();
        if (gameLocation.equals(new Location(player.getWorld(), 0, 0, 0))) {
            return false;
        }
        if (!validateOnly) {
            gameLocation.setYaw(player.getLocation().getYaw());
            gameLocation.setPitch(player.getLocation().getPitch());
            player.teleport(gameLocation);
        }
        return true;
    }

    // Teleport player to the lobby location, with optional validation
    public boolean teleportToLobby(Player player, boolean validateOnly) {
        Location lobbyLocation = plugin.getLobbyLocation();
        if (lobbyLocation.equals(new Location(player.getWorld(), 0, 0, 0))) {
            return false;
        }
        if (!validateOnly) {
            lobbyLocation.setYaw(player.getLocation().getYaw());
            lobbyLocation.setPitch(player.getLocation().getPitch());
            player.teleport(lobbyLocation);
        }
        return true;
    }

    // Update locations for the game and lobby from the configuration
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

    private ApplicableRegionSet getApplicableRegions(Location location) {
        return query.getApplicableRegions(BukkitAdapter.adapt(location));
    }

    // Get the minimum point of the game zone region
    public Location getGameZoneMinimum() {
        String worldName = plugin.getConfig().getString("world");
        World world = Bukkit.getWorld(Objects.requireNonNull(worldName));
        ProtectedRegion gameZone = getRegion(gameZoneRegionName);
        if (gameZone != null && world != null) {
            return BukkitAdapter.adapt(world, gameZone.getMinimumPoint());
        }
        return null;
    }

    // Get the maximum point of the game zone region
    public Location getGameZoneMaximum() {
        String worldName = plugin.getConfig().getString("world");
        World world = Bukkit.getWorld(Objects.requireNonNull(worldName));
        ProtectedRegion gameZone = getRegion(gameZoneRegionName);
        if (gameZone != null && world != null) {
            return BukkitAdapter.adapt(world, gameZone.getMaximumPoint());
        }
        return null;
    }

    // Retrieve a specific region by name
    private ProtectedRegion getRegion(String regionName) {
        String worldName = plugin.getConfig().getString("world");
        World world = Bukkit.getWorld(Objects.requireNonNull(worldName));
        // Return null and log error if world or region is not found
        if (world == null) {
            Bukkit.getLogger().severe("World not found: " + worldName);
            return null;
        }
        RegionManager regions = container.get(BukkitAdapter.adapt(world));
        if (regions == null) {
            Bukkit.getLogger().severe("RegionManager is null for " + worldName);
            return null;
        }
        ProtectedRegion region = regions.getRegion(regionName);
        if (region == null) {
            Bukkit.getLogger().severe("Region not found: " + regionName);
        }
        return region;
    }

    // Check if a location is outside a specific region
    public boolean isLocationOutsideRegion(Location location, String regionName) {
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(location.getWorld()));
        if (!Objects.requireNonNull(regionManager).hasRegion(regionName)) {
            return false;
        }
        ProtectedRegion region = regionManager.getRegion(regionName);
        BlockVector3 locVector = BukkitAdapter.asBlockVector(location);
        return !Objects.requireNonNull(region).contains(locVector);
    }
}
