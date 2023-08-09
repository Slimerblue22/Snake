package com.slimer;
import java.util.Objects;
public class BlockLocation {
    private final int x;
    private final int y;
    private final int z;
    // BlockLocation constructor setting the x, y, and z coordinates of the block location.
    public BlockLocation(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }
    public int getZ() {
        return z;
    }
    // This method checks if the provided object is equal to this BlockLocation. It checks if they are the same object, if the other object is not null and is a BlockLocation, and if their x, y, and z coordinates match.
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BlockLocation that = (BlockLocation) o;
        return x == that.x && y == that.y && z == that.z;
    }
    // This method returns a hash code value for this BlockLocation, which is a numerical representation used for searching objects in a collection. The hash code is based on the x, y, and z coordinates of this BlockLocation.
    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}
