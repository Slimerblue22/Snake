package com.slimer;

import java.util.Objects;

public class BlockLocation {
    // Coordinates of the block.
    private final int x;
    private final int y;
    private final int z;

    // Constructor to initialize the block location with given coordinates.
    public BlockLocation(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // Getter methods to retrieve the coordinates.
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    // Overrides the equals method to compare block locations based on their coordinates.
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BlockLocation that = (BlockLocation) o;
        return x == that.x && y == that.y && z == that.z;
    }

    // Overrides the hashCode method to generate a hash code based on the coordinates.
    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}
