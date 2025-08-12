package me.elite.Factions.territory;

import java.util.Objects;

public class ChunkCoord {
    public final int x, z;

    public ChunkCoord(int x, int z) {
        this.x = x;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChunkCoord)) return false;
        ChunkCoord that = (ChunkCoord) o;
        return x == that.x && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    @Override
    public String toString() {
        return "(" + x + "," + z + ")";
    }
}