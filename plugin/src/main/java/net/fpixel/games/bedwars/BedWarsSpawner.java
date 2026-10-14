package net.fpixel.games.bedwars;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class BedWarsSpawner {
    public enum SpawnerType {
        IRON(Material.IRON_INGOT, 3, 48),
        GOLD(Material.GOLD_INGOT, 8, 16),
        DIAMOND(Material.DIAMOND, 35, 6),
        EMERALD(Material.EMERALD, 50, 4);

        private final Material material;
        private final int intervalSeconds;
        private final int maxCap;

        SpawnerType(Material material, int intervalSeconds, int maxCap) {
            this.material = material;
            this.intervalSeconds = intervalSeconds;
            this.maxCap = maxCap;
        }

        public Material getMaterial() { return material; }
        public int getIntervalSeconds() { return intervalSeconds; }
        public int getMaxCap() { return maxCap; }
    }

    private final Location location;
    private final SpawnerType type;
    private int timer = 0;

    public BedWarsSpawner(Location location, SpawnerType type) {
        this.location = location;
        this.type = type;
    }

    public void tick() {
        timer++;
        if (timer >= type.getIntervalSeconds()) {
            timer = 0;
            spawn();
        }
    }

    public void reset() {
        this.timer = 0;
        if (location != null && location.getWorld() != null) {
            for (Entity e : location.getWorld().getNearbyEntities(location, 4.0, 4.0, 4.0)) {
                if (e instanceof Item item && item.getItemStack().getType() == type.getMaterial()) {
                    item.remove();
                }
            }
        }
    }

    private void spawn() {
        if (location == null || location.getWorld() == null) return;

        // Generator max cap check: prevents excessive item spam
        int nearbyCount = 0;
        for (Entity e : location.getWorld().getNearbyEntities(location, 2.5, 2.5, 2.5)) {
            if (e instanceof Item item && item.getItemStack().getType() == type.getMaterial()) {
                nearbyCount += item.getItemStack().getAmount();
            }
        }
        if (nearbyCount >= type.getMaxCap()) {
            return;
        }

        Item item = location.getWorld().dropItem(location.clone().add(0, 0.5, 0), new ItemStack(type.getMaterial(), 1));
        item.setVelocity(new Vector(0, 0.1, 0));

        if (type == SpawnerType.DIAMOND || type == SpawnerType.EMERALD) {
            location.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, location.clone().add(0, 0.8, 0), 4, 0.2, 0.2, 0.2, 0.05);
        }
    }

    public Location getLocation() { return location; }
    public SpawnerType getType() { return type; }
}
