package net.fpixel.core.selection;

import net.fpixel.game.rollback.BlockSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SelectionManager {
    private final Map<UUID, Location> pos1Map = new HashMap<>();
    private final Map<UUID, Location> pos2Map = new HashMap<>();
    private final Map<UUID, List<BlockSnapshot>> undoMap = new HashMap<>();

    public void setPos1(UUID uuid, Location loc) {
        pos1Map.put(uuid, loc);
    }

    public void setPos2(UUID uuid, Location loc) {
        pos2Map.put(uuid, loc);
    }

    public Location getPos1(UUID uuid) {
        return pos1Map.get(uuid);
    }

    public Location getPos2(UUID uuid) {
        return pos2Map.get(uuid);
    }

    public boolean hasSelection(UUID uuid) {
        Location p1 = pos1Map.get(uuid);
        Location p2 = pos2Map.get(uuid);
        return p1 != null && p2 != null && p1.getWorld() != null && p1.getWorld().equals(p2.getWorld());
    }

    public int getSelectionBlockCount(UUID uuid) {
        Location p1 = pos1Map.get(uuid);
        Location p2 = pos2Map.get(uuid);
        if (p1 == null || p2 == null || !Objects.equals(p1.getWorld(), p2.getWorld())) {
            return 0;
        }
        int dx = Math.abs(p1.getBlockX() - p2.getBlockX()) + 1;
        int dy = Math.abs(p1.getBlockY() - p2.getBlockY()) + 1;
        int dz = Math.abs(p1.getBlockZ() - p2.getBlockZ()) + 1;
        return dx * dy * dz;
    }

    public void saveUndo(UUID uuid, List<BlockSnapshot> snapshots) {
        undoMap.put(uuid, snapshots);
    }

    public List<BlockSnapshot> getUndo(UUID uuid) {
        return undoMap.remove(uuid);
    }

    public ItemStack createWandItem() {
        ItemStack wand = new ItemStack(Material.WOODEN_AXE);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lSelectie Bijl §7(WorldEdit Wand)");
            List<String> lore = new ArrayList<>();
            lore.add("§7Gebruik deze bijl om een gebied te selecteren:");
            lore.add("§e» Linksklik: §fPositie 1 instellen");
            lore.add("§e» Rechtsklik: §fPositie 2 instellen");
            lore.add("");
            lore.add("§aTyp daarna §6//set <blok> [safe] §aom te vullen!");
            meta.setLore(lore);
            wand.setItemMeta(meta);
        }
        return wand;
    }

    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.WOODEN_AXE) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        return meta.getDisplayName().contains("Selectie Bijl");
    }
}
