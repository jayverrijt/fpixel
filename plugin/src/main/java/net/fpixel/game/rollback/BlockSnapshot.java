package net.fpixel.game.rollback;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

public class BlockSnapshot {
    private final Location location;
    private final BlockData blockData;

    public BlockSnapshot(Location location, BlockData blockData) {
        this.location = location;
        this.blockData = blockData;
    }

    public Location getLocation() {
        return location;
    }

    public BlockData getBlockData() {
        return blockData;
    }

    public void restore() {
        location.getBlock().setBlockData(blockData, false);
    }
}
