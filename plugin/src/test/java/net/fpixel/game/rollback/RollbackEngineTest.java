package net.fpixel.game.rollback;

import net.fpixel.test.BukkitTestHarness;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RollbackEngineTest {

    @Test
    void rollbackRemovesPlacedBlocksAndRestoresBrokenBlocks() {
        World world = BukkitTestHarness.createWorld("rollback_test");
        Block placedBlock = world.getBlockAt(5, 64, 5);
        placedBlock.setType(Material.OAK_PLANKS);

        Block brokenBlock = world.getBlockAt(10, 64, 10);
        brokenBlock.setType(Material.STONE);

        RollbackEngine engine = new RollbackEngine();
        engine.recordPlace(placedBlock);
        engine.recordBreak(brokenBlock);

        // Simulate block being broken in-game
        brokenBlock.setType(Material.AIR);
        assertEquals(Material.AIR, brokenBlock.getType());

        engine.rollback();

        assertEquals(Material.AIR, placedBlock.getType(), "Player-placed block must be removed on rollback");
        assertEquals(Material.STONE, brokenBlock.getType(), "Broken map block must be restored on rollback");
    }
}
