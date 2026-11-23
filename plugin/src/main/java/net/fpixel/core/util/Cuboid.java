package net.fpixel.core.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public class Cuboid {
    private final String worldName;
    private final int xMin, xMax;
    private final int yMin, yMax;
    private final int zMin, zMax;

    public Cuboid(Location point1, Location point2) {
        this.worldName = point1.getWorld().getName();
        this.xMin = Math.min(point1.getBlockX(), point2.getBlockX());
        this.xMax = Math.max(point1.getBlockX(), point2.getBlockX());
        this.yMin = Math.min(point1.getBlockY(), point2.getBlockY());
        this.yMax = Math.max(point1.getBlockY(), point2.getBlockY());
        this.zMin = Math.min(point1.getBlockZ(), point2.getBlockZ());
        this.zMax = Math.max(point1.getBlockZ(), point2.getBlockZ());
    }

    public Cuboid(String worldName, int xMin, int xMax, int yMin, int yMax, int zMin, int zMax) {
        this.worldName = worldName;
        this.xMin = Math.min(xMin, xMax);
        this.xMax = Math.max(xMin, xMax);
        this.yMin = Math.min(yMin, yMax);
        this.yMax = Math.max(yMin, yMax);
        this.zMin = Math.min(zMin, zMax);
        this.zMax = Math.max(zMin, zMax);
    }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equals(worldName)) return false;
        return loc.getBlockX() >= xMin && loc.getBlockX() <= xMax
                && loc.getBlockY() >= yMin && loc.getBlockY() <= yMax
                && loc.getBlockZ() >= zMin && loc.getBlockZ() <= zMax;
    }

    public Location getCenter() {
        World world = getWorld();
        return new Location(world,
                xMin + (xMax - xMin) / 2.0,
                yMin + (yMax - yMin) / 2.0,
                zMin + (zMax - zMin) / 2.0);
    }

    public List<Block> getBlocks() {
        List<Block> blocks = new ArrayList<>();
        World world = getWorld();
        if (world == null) return blocks;
        for (int x = xMin; x <= xMax; x++) {
            for (int y = yMin; y <= yMax; y++) {
                for (int z = zMin; z <= zMax; z++) {
                    blocks.add(world.getBlockAt(x, y, z));
                }
            }
        }
        return blocks;
    }

    public int getXMin() { return xMin; }
    public int getXMax() { return xMax; }
    public int getYMin() { return yMin; }
    public int getYMax() { return yMax; }
    public int getZMin() { return zMin; }
    public int getZMax() { return zMax; }
    public String getWorldName() { return worldName; }
}
