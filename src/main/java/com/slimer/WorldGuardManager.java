package com.slimer;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class WorldGuardManager {
    private final RegionContainer container;
    private final SnakePlugin plugin;
    private final Map<String, String> zoneLinks;
    private File regionLinksFile;
    private FileConfiguration regionLinksConfig;

    public WorldGuardManager(SnakePlugin plugin) {
        this.plugin = plugin;
        this.container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        this.zoneLinks = new HashMap<>();
        loadRegionLinksFile();
        loadZoneLinks();
    }

    public FileConfiguration getRegionLinksConfig() {
        return regionLinksConfig;
    }

    public void loadRegionLinksFile() {
        regionLinksFile = new File(plugin.getDataFolder(), "RegionLinks.yml");
        if (!regionLinksFile.exists()) {
            try {
                regionLinksFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        regionLinksConfig = YamlConfiguration.loadConfiguration(regionLinksFile);
    }

    public void loadZoneLinks() {
        if (regionLinksConfig.contains("Linked")) {
            for (String linkKey : Objects.requireNonNull(regionLinksConfig.getConfigurationSection("Linked")).getKeys(false)) {
                String lobbyZone = regionLinksConfig.getString("Linked." + linkKey + ".LobbyRegion");
                String gameZone = regionLinksConfig.getString("Linked." + linkKey + ".GameRegion");
                if (lobbyZone != null && gameZone != null) {
                    zoneLinks.put(lobbyZone, gameZone);
                }
            }
        }
    }

    public enum LinkResult {
        SUCCESS, ALREADY_LINKED, ZONES_NOT_FOUND
    }

    public LinkResult addZoneLink(String lobbyZone, String gameZone) {
        // Check if zones are already linked
        if (zoneLinks.containsKey(lobbyZone) || zoneLinks.containsValue(gameZone)) {
            return LinkResult.ALREADY_LINKED;
        }

        // Verify that the game and lobby zones exist
        String worldName = plugin.getConfig().getString("world");
        World world = Bukkit.getWorld(Objects.requireNonNull(worldName));
        RegionManager regions = container.get(BukkitAdapter.adapt(Objects.requireNonNull(world)));
        if (regions == null || regions.getRegion(gameZone) == null || regions.getRegion(lobbyZone) == null) {
            Bukkit.getLogger().warning("One or both zones not found: " + gameZone + ", " + lobbyZone);
            return LinkResult.ZONES_NOT_FOUND;
        }

        // Determine the next link number
        ConfigurationSection linkedSection = regionLinksConfig.getConfigurationSection("Linked");
        int nextLinkNumber = (linkedSection != null ? linkedSection.getKeys(false).size() : 0) + 1;
        String linkName = "Link" + nextLinkNumber;

        // Link the zones
        zoneLinks.put(lobbyZone, gameZone);
        regionLinksConfig.set("Linked." + linkName + ".LobbyRegion", lobbyZone);
        regionLinksConfig.set("Linked." + linkName + ".GameRegion", gameZone);
        saveRegionLinksFile();

        return LinkResult.SUCCESS;
    }

    public void saveRegionLinksFile() {
        try {
            regionLinksConfig.save(regionLinksFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getPlayerLobbyZone(Player player) {
        String worldName = plugin.getConfig().getString("world");
        World world = Bukkit.getWorld(Objects.requireNonNull(worldName));
        RegionManager regions = container.get(BukkitAdapter.adapt(Objects.requireNonNull(world)));

        // Iterate through lobby zones
        List<String> lobbyZones = regionLinksConfig.getStringList("lobbyzones");
        for (String lobbyZoneName : lobbyZones) {
            if (Objects.requireNonNull(Objects.requireNonNull(regions).getRegion(lobbyZoneName)).contains(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ())) {
                return lobbyZoneName;
            }
        }

        return null; // Player is not in any lobby zone
    }

    public String getPlayerGameZone(Player player) {
        String worldName = plugin.getConfig().getString("world");
        World world = Bukkit.getWorld(Objects.requireNonNull(worldName));
        RegionManager regions = container.get(BukkitAdapter.adapt(Objects.requireNonNull(world)));

        // Iterate through game zones
        List<String> gameZones = regionLinksConfig.getStringList("gamezones");
        for (String gameZoneName : gameZones) {
            if (Objects.requireNonNull(Objects.requireNonNull(regions).getRegion(gameZoneName)).contains(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ())) {
                return gameZoneName;
            }
        }

        return null; // Player is not in any game zone
    }

    public Location[] getGameZoneBounds(String gameZoneName) {
        String worldName = plugin.getConfig().getString("world");
        World world = Bukkit.getWorld(Objects.requireNonNull(worldName));
        if (world == null) {
            Bukkit.getLogger().severe("World not found: " + worldName);
            return null;
        }
        RegionManager regions = container.get(BukkitAdapter.adapt(world));
        if (regions == null) {
            Bukkit.getLogger().severe("RegionManager not found for world: " + worldName);
            return null;
        }
        ProtectedRegion region = regions.getRegion(gameZoneName);
        if (region == null) {
            Bukkit.getLogger().severe("Region not found: " + gameZoneName);
            return null;
        }

        Location min = new Location(world, region.getMinimumPoint().getX(), region.getMinimumPoint().getY(), region.getMinimumPoint().getZ());
        Location max = new Location(world, region.getMaximumPoint().getX(), region.getMaximumPoint().getY(), region.getMaximumPoint().getZ());
        return new Location[] {min, max};
    }

    public String getLinkKeyByLobbyZone(String lobbyZoneName) {
        ConfigurationSection linkedSection = regionLinksConfig.getConfigurationSection("Linked");
        if (linkedSection != null) {
            for (String linkKey : linkedSection.getKeys(false)) {
                String lobbyRegion = regionLinksConfig.getString("Linked." + linkKey + ".LobbyRegion");
                if (lobbyZoneName.equals(lobbyRegion)) {
                    return linkKey;
                }
            }
        }
        return null; // Return null if no matching link key is found
    }

    public String getLinkKeyByGameZone(String gameZoneName) {
        ConfigurationSection linkedSection = regionLinksConfig.getConfigurationSection("Linked");
        if (linkedSection != null) {
            for (String linkKey : linkedSection.getKeys(false)) {
                String gameRegion = regionLinksConfig.getString("Linked." + linkKey + ".GameRegion");
                if (gameZoneName.equals(gameRegion)) {
                    return linkKey;
                }
            }
        }
        return null; // Return null if no matching link key is found
    }

    public boolean isLocationWithinBounds(Location location, Location min, Location max) {
        return location.getX() >= min.getX() && location.getX() <= max.getX()
                && location.getY() >= min.getY() && location.getY() <= max.getY()
                && location.getZ() >= min.getZ() && location.getZ() <= max.getZ();
    }

    public boolean teleportToGameZone(Player player, String lobbyZoneName) {
        String linkKey = getLinkKeyByLobbyZone(lobbyZoneName);
        if (linkKey == null) {
            Bukkit.getLogger().info("Link key not found for lobby zone: " + lobbyZoneName);
            return true;
        }

        String coordinates = regionLinksConfig.getString("Linked." + linkKey + ".gameTP");
        if (coordinates == null) {
            Bukkit.getLogger().info("Teleportation coordinates not found for game zone linked with lobby zone: " + lobbyZoneName);
            return true;
        }

        String[] parts = coordinates.split(",");
        Location tpLocation = new Location(player.getWorld(), Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        player.teleport(tpLocation);
        return true;
    }

    public void teleportToLobby(Player player, String gameZoneName) {
        String linkKey = getLinkKeyByGameZone(gameZoneName); // Call the new method
        if (linkKey == null) {
            Bukkit.getLogger().info("Link key not found for game zone: " + gameZoneName);
            return;
        }

        String coordinates = regionLinksConfig.getString("Linked." + linkKey + ".lobbyTP");
        if (coordinates == null) {
            Bukkit.getLogger().info("Teleportation coordinates not found for lobby zone linked with game zone: " + gameZoneName);
            return;
        }

        String[] parts = coordinates.split(",");
        Location tpLocation = new Location(player.getWorld(), Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));

        player.teleport(tpLocation);
    }

    public boolean registerGameZone(String gameZoneName) {
        // Fetch the world name from the configuration
        String worldName = plugin.getConfig().getString("world");
        World world = Bukkit.getWorld(Objects.requireNonNull(worldName));
        if (world == null) {
            Bukkit.getLogger().severe("World not found: " + worldName);
            return false;
        }

        RegionManager regions = container.get(BukkitAdapter.adapt(world));
        if (regions == null || regions.getRegion(gameZoneName) == null) {
            Bukkit.getLogger().severe("Region not found: " + gameZoneName);
            return false;
        }

        // Store the game zone name in the configuration
        List<String> gameZones = regionLinksConfig.getStringList("gamezones");
        gameZones.add(gameZoneName);
        regionLinksConfig.set("gamezones", gameZones);
        saveRegionLinksFile();

        return true;
    }

    public boolean registerLobbyZone(String lobbyZoneName) {
        // Fetch the world name from the configuration
        String worldName = plugin.getConfig().getString("world");
        World world = Bukkit.getWorld(Objects.requireNonNull(worldName));
        if (world == null) {
            Bukkit.getLogger().severe("World not found: " + worldName);
            return false;
        }

        RegionManager regions = container.get(BukkitAdapter.adapt(world));
        if (regions == null || regions.getRegion(lobbyZoneName) == null) {
            Bukkit.getLogger().severe("Region not found: " + lobbyZoneName);
            return false;
        }

        // Store the lobby zone name in the configuration
        List<String> lobbyZones = regionLinksConfig.getStringList("lobbyzones");
        lobbyZones.add(lobbyZoneName);
        regionLinksConfig.set("lobbyzones", lobbyZones);
        saveRegionLinksFile();

        return true;
    }
}