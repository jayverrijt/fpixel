package net.fpixel.lobby.listeners;

import net.fpixel.FPixelPlugin;
import net.fpixel.lobby.LobbyManager;
import net.fpixel.test.BukkitTestHarness;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LaunchpadListenerTest {

    @Test
    void allEightHubLaunchpadsLaunchOutward() throws Exception {
        FPixelPlugin plugin = BukkitTestHarness.createStubPlugin();
        World hubWorld = BukkitTestHarness.createWorld("world");

        // LobbyManager is a class, so allocate via Unsafe and set spawnLocation
        var unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        LobbyManager lm = (LobbyManager) unsafe.allocateInstance(LobbyManager.class);
        Field spawnField = LobbyManager.class.getDeclaredField("spawnLocation");
        spawnField.setAccessible(true);
        spawnField.set(lm, new Location(hubWorld, 0, 100, 0));

        Field lmField = FPixelPlugin.class.getDeclaredField("lobbyManager");
        lmField.setAccessible(true);
        lmField.set(plugin, lm);

        LaunchpadListener listener = new LaunchpadListener(plugin);

        int[][] launchpadCoords = {
                {0, -15}, {11, -11}, {15, 0}, {11, 11},
                {0, 15}, {-11, 11}, {-15, 0}, {-11, -11}
        };

        for (int[] coords : launchpadCoords) {
            int lx = coords[0];
            int lz = coords[1];

            Block base = hubWorld.getBlockAt(lx, 100, lz);
            base.setType(Material.EMERALD_BLOCK);
            Block plate = hubWorld.getBlockAt(lx, 101, lz);
            plate.setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE);

            final Vector[] velocity = {new Vector(0, 0, 0)};
            Player player = BukkitTestHarness.proxy(Player.class, (m, args) -> switch (m.getName()) {
                case "getName" -> "HubTester";
                case "getUniqueId" -> UUID.randomUUID();
                case "getWorld" -> hubWorld;
                case "getLocation" -> new Location(hubWorld, lx + 0.5, 101, lz + 0.5);
                case "setVelocity" -> {
                    velocity[0] = (Vector) args[0];
                    yield null;
                }
                default -> null;
            });

            Block plateProxy = BukkitTestHarness.proxy(Block.class, (m, args) -> switch (m.getName()) {
                case "getType" -> Material.HEAVY_WEIGHTED_PRESSURE_PLATE;
                case "getX" -> lx;
                case "getY" -> 101;
                case "getZ" -> lz;
                case "getWorld" -> hubWorld;
                case "getRelative" -> base;
                default -> null;
            });

            PlayerInteractEvent event = new PlayerInteractEvent(
                    player, Action.PHYSICAL, null, plateProxy, BlockFace.UP);
            listener.onLaunchpad(event);

            assertTrue(event.isCancelled(), "Launchpad at (" + lx + "," + lz + ") should cancel physical interact");
            assertTrue(velocity[0].getY() > 1.0, "Launchpad at (" + lx + "," + lz + ") should launch upward");
            // Dot product with outward vector (lx, lz) must be positive
            assertTrue(velocity[0].getX() * lx + velocity[0].getZ() * lz > 0,
                    "Launchpad at (" + lx + "," + lz + ") should launch outward from plaza");
        }
    }
}
