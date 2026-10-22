package net.fpixel.survival.waypoints;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;

public class Waypoint {
    private final String name;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final ChatColor color;
    private final Material icon;
    private final long createdAt;

    public Waypoint(String name, Location loc, ChatColor color, Material icon) {
        this.name = name;
        this.worldName = loc.getWorld() != null ? loc.getWorld().getName() : "survival";
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
        this.yaw = loc.getYaw();
        this.pitch = loc.getPitch();
        this.color = color != null ? color : ChatColor.AQUA;
        this.icon = icon != null ? icon : Material.COMPASS;
        this.createdAt = System.currentTimeMillis();
    }

    public Waypoint(String name, String worldName, double x, double y, double z, float yaw, float pitch, ChatColor color, Material icon, long createdAt) {
        this.name = name;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.color = color != null ? color : ChatColor.AQUA;
        this.icon = icon != null ? icon : Material.COMPASS;
        this.createdAt = createdAt;
    }

    public String getName() { return name; }
    public String getWorldName() { return worldName; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public ChatColor getColor() { return color; }
    public Material getIcon() { return icon; }
    public long getCreatedAt() { return createdAt; }

    public double distanceTo(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().getName().equalsIgnoreCase(worldName)) {
            return -1.0;
        }
        double dx = loc.getX() - x;
        double dy = loc.getY() - y;
        double dz = loc.getZ() - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
