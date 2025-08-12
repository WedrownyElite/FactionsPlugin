package me.elite.Factions.warps;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public class FactionWarp {
    private final String name;
    private final String worldName;
    private final double x, y, z;
    private final float yaw, pitch;
    private final UUID createdBy;
    private final long createdTime;

    public FactionWarp(String name, Location location, UUID createdBy) {
        this.name = name;
        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.createdBy = createdBy;
        this.createdTime = System.currentTimeMillis();
    }

    // Constructor for loading from data
    public FactionWarp(String name, String worldName, double x, double y, double z,
                       float yaw, float pitch, UUID createdBy, long createdTime) {
        this.name = name;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.createdBy = createdBy;
        this.createdTime = createdTime;
    }

    public String getName() {
        return name;
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

    public UUID getCreatedBy() {
        return createdBy;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    @Override
    public String toString() {
        return "FactionWarp{" +
                "name='" + name + '\'' +
                ", worldName='" + worldName + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", yaw=" + yaw +
                ", pitch=" + pitch +
                ", createdBy=" + createdBy +
                ", createdTime=" + createdTime +
                '}';
    }
}