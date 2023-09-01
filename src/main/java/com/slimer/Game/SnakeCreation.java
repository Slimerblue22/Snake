package com.slimer.Game;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Sheep;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for creating and managing a snake in the game.
 * The snake is represented by a lead sheep entity followed by zero or more segment entities.
 */
public class SnakeCreation {
    private final Sheep sheep;  // The lead entity of the snake
    private final List<Entity> segments;  // The segment entities that follow the sheep

    /**
     * Constructs a SnakeCreation object and spawns the initial sheep entity at the given location.
     *
     * @param location The spawn location for the lead sheep entity.
     */
    public SnakeCreation(Location location) {
        this.sheep = (Sheep) location.getWorld().spawnEntity(location, EntityType.SHEEP, CreatureSpawnEvent.SpawnReason.CUSTOM);
        this.sheep.setSilent(true);
        this.sheep.setAware(false);
        this.sheep.setCollidable(false);
        this.segments = new ArrayList<>();
    }

    /**
     * Adds a segment to the snake at the specified waypoint.
     *
     * @param lastWaypoint The location for the new segment.
     */
    public void addSegment(Vector lastWaypoint) {
        World world = sheep.getWorld();
        Location newSegmentLocation = new Location(world, lastWaypoint.getX(), lastWaypoint.getY(), lastWaypoint.getZ());
        Entity segment = world.spawnEntity(newSegmentLocation, EntityType.SHEEP, CreatureSpawnEvent.SpawnReason.CUSTOM);
        segments.add(segment);

        // Set segment properties if it's a Sheep entity
        if (segment instanceof Sheep) {
            segment.setSilent(true);
            ((Sheep) segment).setAware(false);
            ((Sheep) segment).setCollidable(false);
        }
    }

    /**
     * Destroys the snake by removing all its entities.
     */
    public void destroy() {
        this.sheep.remove();
        for (Entity segment : segments) {
            segment.remove();
        }
    }

    /**
     * Returns the list of segments in the snake.
     *
     * @return A List of Entity objects representing the segments.
     */
    public List<Entity> getSegments() {
        return this.segments;
    }

    /**
     * Returns the lead sheep entity of the snake.
     *
     * @return The Sheep entity that leads the snake.
     */
    public Entity getSheepEntity() {
        return this.sheep;
    }
}
