package net.fpixel.games.bedwars;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class BedWarsSpawner {
    public enum SpawnerType {
        IRON(Material.IRON_INGOT, 1),
        GOLD(Material.GOLD_INGOT, 4),
        DIAMOND(Material.DIAMOND, 30),
        EMERALD(Material.EMERALD, 45);

        private final Material material;
        private final int intervalSeconds;

        SpawnerType(Material material, int intervalSeconds) {
            this.material = material;
            this.intervalSeconds = intervalSeconds;
        }

        public Material getMaterial() { return material; }
        public int getIntervalSeconds() { return intervalSeconds; }
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

    private void spawn() {
        if (location == null || location.getWorld() == null) return;
        Item item = location.getWorld().dropItem(location.clone().add(0, 0.5, 0), new ItemStack(type.getMaterial(), 1));
        item.setVelocity(new Vector(0, 0.1, 0));
    }

    public Location getLocation() { return location; }
    public SpawnerType getType() { return type; }
}
