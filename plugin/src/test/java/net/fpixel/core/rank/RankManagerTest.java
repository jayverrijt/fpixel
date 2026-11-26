package net.fpixel.core.rank;

import net.fpixel.FPixelPlugin;
import net.fpixel.test.BukkitTestHarness;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RankManagerTest {

    @Test
    void demotingOwnerToMemberOverridesOldUuidRank() {
        FPixelPlugin plugin = BukkitTestHarness.createStubPlugin();
        RankManager manager = new RankManager(plugin);
        World world = BukkitTestHarness.createWorld("world");
        UUID uuid = UUID.randomUUID();
        Player player = BukkitTestHarness.createPlayer("SmileyGaming27", uuid, world);

        // Simulate player previously having OWNER on their UUID
        manager.setRank(uuid, Rank.OWNER);
        assertEquals(Rank.OWNER, manager.getRank(player));

        // Demote by name to MEMBER
        manager.setRankByName("SmileyGaming27", Rank.MEMBER);
        assertEquals(Rank.MEMBER, manager.getRank(player),
                "Demoting by name to MEMBER must override previous OWNER UUID rank");
    }
}
