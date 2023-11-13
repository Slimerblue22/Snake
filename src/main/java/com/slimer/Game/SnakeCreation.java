package com.slimer.Game;

import com.slimer.Util.DebugManager;
import com.slimer.Util.PlayerData;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for creating and managing a snake in the game.
 * The snake is represented by a lead sheep entity followed by zero or more segment entities.
 * <p>
 * Last updated: V2.1.0
 * @author Slimerblue22
 */
public class SnakeCreation {
    private final Sheep sheep;
    private final List<Entity> segments;
    private static final DyeColor DEFAULT_SHEEP_COLOR = DyeColor.WHITE;

    /**
     * Constructs a SnakeCreation object and spawns the initial sheep entity at the given location.
     *
     * @param location The spawn location for the lead sheep entity.
     */
    public SnakeCreation(Location location, Player player) {
        this.sheep = spawnSheep(location, player);
        this.segments = new ArrayList<>();
        DebugManager.log(DebugManager.Category.SNAKE_CREATION, "New snake created for player: " + player.getName() + " at location: " + location);
    }

    /**
     * Adds a segment to the snake at the specified waypoint.
     *
     * @param lastWaypoint The location for the new segment.
     */
    public void addSegment(Vector lastWaypoint, Player player) {
        World world = sheep.getWorld();
        Location newSegmentLocation = new Location(world, lastWaypoint.getX(), lastWaypoint.getY(), lastWaypoint.getZ());
        Entity segment = spawnSheep(newSegmentLocation, player);
        segments.add(segment);
        DebugManager.log(DebugManager.Category.SNAKE_CREATION, "Segment added for player: " + player.getName() + " at waypoint: " + lastWaypoint);
    }

    /**
     * Spawns a custom Sheep entity at the specified location with specific attributes.
     *
     * @param location The Location where the Sheep should be spawned.
     * @param player The Player associated with the spawned Sheep.
     * @return The newly spawned Sheep entity.
     */
    private Sheep spawnSheep(Location location, Player player) {
        Sheep newSheep = (Sheep) location.getWorld().spawnEntity(location, EntityType.SHEEP, CreatureSpawnEvent.SpawnReason.CUSTOM);
        newSheep.setSilent(true);
        newSheep.setAware(false);
        newSheep.setCollidable(false);
        DyeColor color = PlayerData.getInstance().getSheepColor(player);
        newSheep.setColor(color == null ? DEFAULT_SHEEP_COLOR : color);
        return newSheep;
    }

    /**
     * Destroys the snake by removing all its entities.
     */
    public void destroy() {
        this.sheep.remove();
        DebugManager.log(DebugManager.Category.SNAKE_CREATION, "Snake destroyed for player with lead sheep at: " + this.sheep.getLocation());
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
