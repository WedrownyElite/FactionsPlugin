package me.elite.Factions.homes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public class FactionHome {
    private final String worldName;
    private final double x, y, z;
    private final float yaw, pitch;
    private final UUID setBy;
    private final long setTime;

    public FactionHome(Location location, UUID setBy) {
        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.setBy = setBy;
        this.setTime = System.currentTimeMillis();
    }

    // Constructor for loading from data
    public FactionHome(String worldName, double x, double y, double z,
                       float yaw, float pitch, UUID setBy, long setTime) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.setBy = setBy;
        this.setTime = setTime;
    }

    public Location getLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null; // World doesn't exist
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    public String getWorldName() {
        return worldName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public UUID getSetBy() {
        return setBy;
    }

    public long getSetTime() {
        return setTime;
    }

    @Override
    public String toString() {
        return "FactionHome{" +
                "worldName='" + worldName + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", yaw=" + yaw +
                ", pitch=" + pitch +
                ", setBy=" + setBy +
                ", setTime=" + setTime +
                '}';
    }
}