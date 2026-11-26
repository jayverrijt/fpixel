package net.fpixel.e2e;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.rank.Rank;
import net.fpixel.core.rank.RankManager;
import net.fpixel.game.Arena;
import net.fpixel.game.GameManager;
import net.fpixel.game.GameState;
import net.fpixel.game.kits.CustomKitManager;
import net.fpixel.game.scuffed.ScuffedModeManager;
import net.fpixel.games.skywars.SkyWarsGame;
import net.fpixel.lobby.listeners.LobbyListener;
import net.fpixel.test.BukkitTestHarness;
import net.fpixel.worlds.PlayerWorld;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End test suite covering complete minigame lifecycles, custom kits,
 * scuffed mode, spectator isolation, and rank/world access control.
 */
class FPixelEndToEndTest {

    private FPixelPlugin plugin;
    private GameManager gameManager;
    private ScuffedModeManager scuffedModeManager;
    private CustomKitManager customKitManager;
    private RankManager rankManager;

    @BeforeEach
    void setUp() throws Exception {
        plugin = BukkitTestHarness.createStubPlugin();
        plugin.getDataFolder().mkdirs();

        gameManager = new GameManager(plugin);
        scuffedModeManager = new ScuffedModeManager(plugin);
        customKitManager = new CustomKitManager(plugin);
        rankManager = new RankManager(plugin);

        setPluginField("gameManager", gameManager);
        setPluginField("scuffedModeManager", scuffedModeManager);
        setPluginField("customKitManager", customKitManager);
        setPluginField("rankManager", rankManager);
    }

    private void setPluginField(String fieldName, Object value) throws Exception {
        Field f = FPixelPlugin.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(plugin, value);
    }

    @Test
    void fullSkyWarsMatchWithScuffedModeSpectatorIsolationAndReplay() throws Exception {
        Inventory chestInv = BukkitTestHarness.createInventory(27);
        Block[] chestBlockRef = new Block[1];
        World world = BukkitTestHarness.proxy(World.class, (m, args) -> switch (m.getName()) {
            case "getName" -> "map_build_e2e_sw";
            case "isChunkLoaded" -> true;
            case "loadChunk" -> m.getReturnType() == boolean.class ? true : null;
            case "getLoadedChunks" -> new org.bukkit.Chunk[0];
            case "getEntities" -> Collections.emptyList();
            case "getBlockAt" -> {
                int x = args.length == 1 ? ((Location) args[0]).getBlockX() : (int) args[0];
                int y = args.length == 1 ? ((Location) args[0]).getBlockY() : (int) args[1];
                int z = args.length == 1 ? ((Location) args[0]).getBlockZ() : (int) args[2];
                if (x == 30 && y == 64 && z == 0) yield chestBlockRef[0];
                yield BukkitTestHarness.createBlock(BukkitTestHarness.createWorld("tmp"), x, y, z, Material.AIR);
            }
            default -> null;
        });

        Chest chestState = BukkitTestHarness.createChest(world, 30, 64, 0, chestInv);
        chestBlockRef[0] = BukkitTestHarness.proxy(Block.class, (m, args) -> switch (m.getName()) {
            case "getWorld" -> world;
            case "getX" -> 30;
            case "getY" -> 64;
            case "getZ" -> 0;
            case "getLocation" -> new Location(world, 30, 64, 0);
            case "getType" -> Material.CHEST;
            case "getState" -> (BlockState) chestState;
            default -> null;
        });

        SkyWarsGame sw = new SkyWarsGame(plugin, "e2e-sw", "E2E SkyWars", 2, 8, 40);
        sw.setLobbySpawn(new Location(world, 0, 100, 0));
        sw.addIslandChest(new Location(world, 30, 64, 0));
        gameManager.registerArena(sw);

        Player p1 = BukkitTestHarness.createPlayer("PlayerOne", UUID.randomUUID(), world);
        Player p2 = BukkitTestHarness.createPlayer("PlayerTwo", UUID.randomUUID(), world);

        // Add players to arena and start match with Scuffed Mode ON
        scuffedModeManager.setEnabled(true);
        addAlivePlayer(sw, p1);
        addAlivePlayer(sw, p2);
        setArenaState(sw, GameState.IN_GAME);

        sw.populateChests();
        scuffedModeManager.onArenaStart(sw);
        assertFalse(chestInv.isEmpty(), "Chests must populate at start of match");
        assertFalse(scuffedModeManager.getActiveModifiers(sw).isEmpty(), "Scuffed modifiers must be active");

        // Loot chest & verify LobbyListener does not refill it
        chestInv.clear();
        sw.fillChestDynamically(chestState);
        assertTrue(chestInv.isEmpty(), "Looted chest must stay empty");

        // Simulate p2 eliminated -> becomes spectator, cannot hit p1
        moveToSpectator(sw, p2);
        var ds = BukkitTestHarness.proxy(org.bukkit.damage.DamageSource.class, (m, args) -> null);
        EntityDamageByEntityEvent hit = new EntityDamageByEntityEvent(
                p2, p1, EntityDamageEvent.DamageCause.ENTITY_ATTACK, ds, 2.0);
        gameManager.onDamage(hit);
        assertTrue(hit.isCancelled(), "Eliminated spectator must not hit alive player");

        // Reset arena and replay
        sw.onArenaReset();
        scuffedModeManager.onArenaReset(sw);
        assertTrue(scuffedModeManager.getActiveModifiers(sw).isEmpty(), "Scuffed modifiers must reset after game");

        sw.populateChests();
        assertFalse(chestInv.isEmpty(), "Chests must repopulate on Play Again!");
    }

    @Test
    void customKitSavedFromWebJsonLoadsAndAppliesInGame() throws Exception {
        File kitsFile = new File(plugin.getDataFolder(), "custom_kits.json");
        String json = """
                {
                  "PVP": [
                    {
                      "id": "e2e_warrior",
                      "name": "E2E Warrior",
                      "icon": "DIAMOND_SWORD",
                      "helmet": "IRON_HELMET",
                      "chestplate": "IRON_CHESTPLATE",
                      "leggings": "IRON_LEGGINGS",
                      "boots": "IRON_BOOTS",
                      "items": [
                        { "material": "DIAMOND_SWORD", "amount": 1 },
                        { "material": "GOLDEN_APPLE", "amount": 3 }
                      ]
                    }
                  ]
                }
                """;
        try (FileWriter fw = new FileWriter(kitsFile, StandardCharsets.UTF_8)) {
            fw.write(json);
        }

        customKitManager.loadKits();
        var kits = customKitManager.getKitsForGameType("PVP");
        assertEquals(1, kits.size());
        assertEquals("E2E Warrior", kits.get(0).name);

        World world = BukkitTestHarness.createWorld("arenas");
        Player p = BukkitTestHarness.createPlayer("KitTester", UUID.randomUUID(), world);
        customKitManager.selectKit(p.getUniqueId(), "PVP", "e2e_warrior");

        assertTrue(customKitManager.applySelectedKit(p, "PVP"), "Selected custom kit must apply");
    }

    @Test
    void demotedMemberCannotSeeOrJoinUnsharedPersonalWorld() {
        World world = BukkitTestHarness.createWorld("world");
        UUID smileyUuid = UUID.randomUUID();
        Player smiley = BukkitTestHarness.createPlayer("SmileyGaming27", smileyUuid, world);

        rankManager.setRank(smileyUuid, Rank.OWNER);
        rankManager.setRankByName("SmileyGaming27", Rank.MEMBER);
        assertEquals(Rank.MEMBER, rankManager.getRank(smiley));

        PlayerWorld jayCreative = new PlayerWorld(
                "creative1", "Creative world", UUID.randomUUID(), "Jay_V0", 42L, true, "CREATIVE", "NORMAL", false);
        assertFalse(jayCreative.canAccess(smiley), "Demoted unshared member must not access private world");
    }

    @SuppressWarnings("unchecked")
    private void addAlivePlayer(Arena arena, Player p) throws Exception {
        Field f = Arena.class.getDeclaredField("players");
        f.setAccessible(true);
        ((Set<UUID>) f.get(arena)).add(p.getUniqueId());
    }

    @SuppressWarnings("unchecked")
    private void moveToSpectator(Arena arena, Player p) throws Exception {
        Field pf = Arena.class.getDeclaredField("players");
        pf.setAccessible(true);
        ((Set<UUID>) pf.get(arena)).remove(p.getUniqueId());

        Field sf = Arena.class.getDeclaredField("spectators");
        sf.setAccessible(true);
        ((Set<UUID>) sf.get(arena)).add(p.getUniqueId());
    }

    private void setArenaState(Arena arena, GameState state) throws Exception {
        Field f = Arena.class.getDeclaredField("state");
        f.setAccessible(true);
        f.set(arena, state);
    }
}
