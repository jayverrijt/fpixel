package net.fpixel.game.rollback;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.*;

public class RollbackEngine {
    private final Map<Location, BlockSnapshot> originalBlocks = new HashMap<>();
    private final Set<Location> placedBlocks = new HashSet<>();

    public void logBlockBreak(Block block) {
        Location loc = block.getLocation();
        if (placedBlocks.contains(loc)) {
            // Block was placed by a player during the game, no original state to restore
            placedBlocks.remove(loc);
            return;
        }
        // Save the original block state if not already logged
        originalBlocks.putIfAbsent(loc, new BlockSnapshot(loc, block.getBlockData().clone()));
    }

    public void logBlockPlace(Block block) {
        Location loc = block.getLocation();
        placedBlocks.add(loc);
    }

    public void logCustomChange(Block block) {
        Location loc = block.getLocation();
        originalBlocks.putIfAbsent(loc, new BlockSnapshot(loc, block.getBlockData().clone()));
    }

    public boolean isPlayerPlaced(Location loc) {
        return placedBlocks.contains(loc);
    }

    public void rollback() {
        // Remove all player placed blocks
        for (Location loc : placedBlocks) {
            loc.getBlock().setType(Material.AIR, false);
        }
        placedBlocks.clear();

        // Restore all broken/modified map blocks
        for (BlockSnapshot snapshot : originalBlocks.values()) {
            snapshot.restore();
        }
        originalBlocks.clear();
    }
}
