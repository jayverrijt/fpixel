package net.fpixel.worlds;

import net.fpixel.test.BukkitTestHarness;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerWorldAccessTest {

    @Test
    void unsharedPersonalWorldIsPrivateByDefault() {
        World world = BukkitTestHarness.createWorld("world");
        UUID ownerId = UUID.randomUUID();
        Player owner = BukkitTestHarness.createPlayer("Jay_V0", ownerId, world);
        Player stranger = BukkitTestHarness.createPlayer("SmileyGaming", UUID.randomUUID(), world);

        PlayerWorld creativeWorld = new PlayerWorld(
                "creative1", "Creative world", ownerId, "Jay_V0", 12345L, true, "CREATIVE", "NORMAL", false);

        assertFalse(creativeWorld.isPublicWorld(), "Personal worlds must default to private");
        assertTrue(creativeWorld.canAccess(owner), "Owner must have access");
        assertFalse(creativeWorld.canAccess(stranger), "Unshared player must NOT have access");

        creativeWorld.getSharedPlayers().put("smileygaming", false);
        assertTrue(creativeWorld.canAccess(stranger), "Shared player should have access");
    }

    @Test
    void defaultNoArgConstructorDefaultsToPrivate() {
        World world = BukkitTestHarness.createWorld("world");
        Player stranger = BukkitTestHarness.createPlayer("SmileyGaming", UUID.randomUUID(), world);

        // Simulates Gson deserializing a world JSON that omits publicWorld
        PlayerWorld deserialized = new PlayerWorld();
        deserialized.setUid("creative2");
        deserialized.setCreatorName("Jay_V0");
        deserialized.setPrimarySmp(false);

        assertFalse(deserialized.isPublicWorld(), "Deserialized world without publicWorld must default to private");
        assertFalse(deserialized.canAccess(stranger), "Unshared player must NOT see or join deserialized private world");
    }
}
