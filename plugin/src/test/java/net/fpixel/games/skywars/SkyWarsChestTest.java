package net.fpixel.games.skywars;

import net.fpixel.FPixelPlugin;
import net.fpixel.game.GameManager;
import net.fpixel.game.GameState;
import net.fpixel.lobby.listeners.LobbyListener;
import net.fpixel.test.BukkitTestHarness;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SkyWarsChestTest {

    private FPixelPlugin plugin;
    private GameManager gameManager;
    private World world;
    private Inventory islandInv;
    private Inventory midInv;
    private Block islandBlock;
    private Block midBlock;

    @BeforeEach
    void setUp() throws Exception {
        plugin = BukkitTestHarness.createStubPlugin();
        gameManager = new GameManager(plugin);
        Field gmField = FPixelPlugin.class.getDeclaredField("gameManager");
        gmField.setAccessible(true);
        gmField.set(plugin, gameManager);

        islandInv = BukkitTestHarness.createInventory(27);
        midInv = BukkitTestHarness.createInventory(27);

        // World whose blocks at (30,64,0) and (0,64,0) return live Chest states
        world = BukkitTestHarness.proxy(World.class, (m, args) -> switch (m.getName()) {
            case "getName" -> "map_build_skywars_test";
            case "isChunkLoaded" -> true;
            case "loadChunk" -> m.getReturnType() == boolean.class ? true : null;
            case "getLoadedChunks" -> new org.bukkit.Chunk[0];
            case "getEntities" -> java.util.Collections.emptyList();
            case "getBlockAt" -> {
                int x = args.length == 1 ? ((Location) args[0]).getBlockX() : (int) args[0];
                int y = args.length == 1 ? ((Location) args[0]).getBlockY() : (int) args[1];
                int z = args.length == 1 ? ((Location) args[0]).getBlockZ() : (int) args[2];
                if (x == 30 && y == 64 && z == 0) yield islandBlock;
                if (x == 0 && y == 64 && z == 0) yield midBlock;
                yield BukkitTestHarness.createBlock(world, x, y, z, Material.AIR);
            }
            default -> null;
        });

        islandBlock = chestBlock(world, 30, 64, 0, islandInv);
        midBlock = chestBlock(world, 0, 64, 0, midInv);
    }

    private Block chestBlock(World w, int x, int y, int z, Inventory inv) {
        Chest chestState = BukkitTestHarness.createChest(w, x, y, z, inv);
        return BukkitTestHarness.proxy(Block.class, (m, args) -> switch (m.getName()) {
            case "getWorld" -> w;
            case "getX" -> x;
            case "getY" -> y;
            case "getZ" -> z;
            case "getLocation" -> new Location(w, x, y, z);
            case "getType" -> Material.CHEST;
            case "getState" -> (BlockState) chestState;
            default -> null;
        });
    }

    @Test
    void chestsPopulateOnFirstAndSecondGameAfterPlayAgain() {
        SkyWarsGame game = new SkyWarsGame(plugin, "sw-test", "SkyWars Test", 2, 8, 40);
        game.setLobbySpawn(new Location(world, 0, 100, 0));
        game.addIslandChest(new Location(world, 30, 64, 0));
        game.addMidChest(new Location(world, 0, 64, 0));
        gameManager.registerArena(game);

        // First game starts
        game.populateChests();
        assertFalse(islandInv.isEmpty(), "Island chest should be populated in first game");
        assertFalse(midInv.isEmpty(), "Mid chest should be populated in first game");

        // Player loots chests completely
        islandInv.clear();
        midInv.clear();
        assertTrue(islandInv.isEmpty());

        // Reopening a looted chest in the same round must NOT refill it
        game.fillChestDynamically((Chest) islandBlock.getState());
        assertTrue(islandInv.isEmpty(), "Already-looted chest must not refill on reopen");

        // Arena resets after game ends, then second game starts ("Play Again")
        game.onArenaReset();
        game.populateChests();
        assertFalse(islandInv.isEmpty(), "Island chest must be populated again on replay");
        assertFalse(midInv.isEmpty(), "Mid chest must be populated again on replay");
    }

    @Test
    void lobbyListenerDoesNotRefillEmptyChestsDuringActiveSkyWarsMatch() {
        SkyWarsGame game = new SkyWarsGame(plugin, "sw-active", "SkyWars Active", 2, 8, 40);
        game.setLobbySpawn(new Location(world, 0, 100, 0));
        gameManager.registerArena(game);

        Player player = BukkitTestHarness.createPlayer("Tester", UUID.randomUUID(), world);
        try {
            Field playersField = net.fpixel.game.Arena.class.getDeclaredField("players");
            playersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Set<UUID> players = (java.util.Set<UUID>) playersField.get(game);
            players.add(player.getUniqueId());

            Field stateField = net.fpixel.game.Arena.class.getDeclaredField("state");
            stateField.setAccessible(true);
            stateField.set(game, GameState.IN_GAME);
        } catch (Exception e) {
            fail(e);
        }

        LobbyListener lobbyListener = new LobbyListener(plugin);

        // Simulate player right-clicking an emptied chest during an active game
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(
                player, Action.RIGHT_CLICK_BLOCK, null, islandBlock, org.bukkit.block.BlockFace.UP);
        lobbyListener.onChestOpen(interactEvent);
        assertTrue(islandInv.isEmpty(), "LobbyListener.onChestOpen must not refill chests during an active match");

        // Simulate InventoryOpenEvent for an emptied chest
        InventoryView view = BukkitTestHarness.proxy(InventoryView.class, (m, args) -> switch (m.getName()) {
            case "getTopInventory" -> islandInv;
            case "getPlayer" -> player;
            default -> null;
        });
        InventoryOpenEvent openEvent = new InventoryOpenEvent(view);
        lobbyListener.onInventoryOpen(openEvent);
        assertTrue(islandInv.isEmpty(), "LobbyListener.onInventoryOpen must not refill chests during an active match");
    }
}
