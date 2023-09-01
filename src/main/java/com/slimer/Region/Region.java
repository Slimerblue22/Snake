package com.slimer.Region;

import org.bukkit.Location;

/**
 * Represents a region in the Snake game, including its name, type, location,
 * and the name of the world it belongs to. A region can be of type LOBBY or GAME.
 */
public class Region {
    private final String name;
    private final RegionType type;
    private final Location location;
    private final String worldName;

    /**
     * Constructs a new Region with the specified name, type, location, and world name.
     *
     * @param name      The name of the region.
     * @param type      The type of the region, either LOBBY or GAME.
     * @param location  The location of the region.
     * @param worldName The name of the world the region is in.
     */
    public Region(String name, RegionType type, Location location, String worldName) {
        this.name = name;
        this.type = type;
        this.location = location;
        this.worldName = worldName;
    }

    /**
     * Returns the name of the region.
     *
     * @return The name of the region.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the type of the region.
     *
     * @return The type of the region.
     */
    public RegionType getType() {
        return type;
    }

    /**
     * Returns the name of the world the region is in.
     *
     * @return The name of the world the region is in.
     */
    public String getWorldName() {
        return worldName;
    }

    /**
     * Returns a string representation of the Region object.
     *
     * @return A string representation of the Region object.
     */
    @Override
    public String toString() {
        return "Region{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", location=" + location +
                '}';
    }

    /**
     * Represents the type of region in the game.
     */
    public enum RegionType {
        LOBBY, // Represents a lobby region
        GAME   // Represents a game region
    }
}
