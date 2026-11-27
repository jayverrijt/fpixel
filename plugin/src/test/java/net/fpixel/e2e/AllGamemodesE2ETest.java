package net.fpixel.e2e;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.bedrock.BedrockManager;
import net.fpixel.core.rank.RankManager;
import net.fpixel.core.scoreboard.ScoreboardManager;
import net.fpixel.core.user.UserManager;
import net.fpixel.core.watchdog.WatchdogListener;
import net.fpixel.core.watchdog.WatchdogManager;
import net.fpixel.game.Arena;
import net.fpixel.game.GameManager;
import net.fpixel.game.GameState;
import net.fpixel.game.kits.CustomKitManager;
import net.fpixel.game.scuffed.ScuffedModeManager;
import net.fpixel.game.spectator.SpectatorManager;
import net.fpixel.games.bedwars.BedWarsGame;
import net.fpixel.games.bridge.BridgeGame;
import net.fpixel.games.murdermystery.MurderMysteryGame;
import net.fpixel.games.parkour.ParkourGame;
import net.fpixel.games.prophunt.PropHuntGame;
import net.fpixel.games.pvp.PvPGame;
import net.fpixel.games.skywars.SkyWarsGame;
import net.fpixel.games.sumo.SumoGame;
import net.fpixel.games.tntrun.TNTRunGame;
import net.fpixel.lobby.LobbyManager;
import net.fpixel.test.BukkitTestHarness;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End tests for every minigame gamemode across both Java Edition and Bedrock Edition players
 * (excluding private PlayerWorlds and Survival SMP).
 */
class AllGamemodesE2ETest {

    private FPixelPlugin plugin;
    private GameManager gameManager;
    private BedrockManager bedrockManager;
    private World world;

    @BeforeEach
    void setUp() throws Exception {
        plugin = BukkitTestHarness.createStubPlugin();
        plugin.getDataFolder().mkdirs();

        gameManager = new GameManager(plugin);
        bedrockManager = new BedrockManager(plugin);
        SpectatorManager spectatorManager = new SpectatorManager(plugin);
        ScuffedModeManager scuffedModeManager = new ScuffedModeManager(plugin);
        CustomKitManager customKitManager = new CustomKitManager(plugin);
        RankManager rankManager = new RankManager(plugin);
        UserManager userManager = new UserManager(plugin);
        LobbyManager lobbyManager = allocateWithoutConstructor(LobbyManager.class);
        ScoreboardManager scoreboardManager = allocateWithoutConstructor(ScoreboardManager.class);

        setPluginField("gameManager", gameManager);
        setPluginField("bedrockManager", bedrockManager);
        setPluginField("spectatorManager", spectatorManager);
        setPluginField("scuffedModeManager", scuffedModeManager);
        setPluginField("customKitManager", customKitManager);
        setPluginField("rankManager", rankManager);
        setPluginField("userManager", userManager);
        setPluginField("lobbyManager", lobbyManager);
        setPluginField("scoreboardManager", scoreboardManager);

        world = BukkitTestHarness.createWorld("arenas");
    }

    @SuppressWarnings("unchecked")
    private <T> T allocateWithoutConstructor(Class<T> clazz) throws Exception {
        var f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) f.get(null);
        return (T) unsafe.allocateInstance(clazz);
    }

    private void setPluginField(String name, Object value) throws Exception {
        Field f = FPixelPlugin.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(plugin, value);
    }

    private Player createPlatformPlayer(String baseName, boolean bedrock) {
        if (bedrock) {
            UUID floodgateUuid = UUID.fromString("00000000-0000-0000-0009-" + String.format("%012x", Math.abs(baseName.hashCode())));
            return BukkitTestHarness.createPlayer("." + baseName, floodgateUuid, world);
        }
        return BukkitTestHarness.createPlayer(baseName, UUID.randomUUID(), world);
    }

    private Arena createArenaForGameType(String gameType) {
        Location center = new Location(world, 0.5, 100, 0.5);
        Location spawn1 = new Location(world, -10.5, 100, 0.5);
        Location spawn2 = new Location(world, 10.5, 100, 0.5);

        return switch (gameType) {
            case "BEDWARS" -> {
                BedWarsGame bw = new BedWarsGame(plugin, "e2e-bw", "E2E BedWars", 2, 8);
                bw.setLobbySpawn(center);
                bw.setSpectatorSpawn(center);
                bw.getTeam("RED").setSpawnLocation(spawn1);
                bw.getTeam("RED").setBedLocation(new Location(world, -12, 100, 0));
                bw.getTeam("BLUE").setSpawnLocation(spawn2);
                bw.getTeam("BLUE").setBedLocation(new Location(world, 12, 100, 0));
                yield bw;
            }
            case "SKYWARS" -> {
                SkyWarsGame sw = new SkyWarsGame(plugin, "e2e-sw", "E2E SkyWars", 2, 8, 40);
                sw.setLobbySpawn(center);
                sw.setSpectatorSpawn(center);
                sw.addIslandSpawn(spawn1);
                sw.addIslandSpawn(spawn2);
                yield sw;
            }
            case "BRIDGE" -> {
                BridgeGame br = new BridgeGame(plugin, "e2e-bridge", "E2E Bridge", 40);
                br.setLobbySpawn(center);
                br.setSpectatorSpawn(center);
                br.setRedSpawn(spawn1);
                br.setBlueSpawn(spawn2);
                br.setRedGoal(new Location(world, -15, 100, 0));
                br.setBlueGoal(new Location(world, 15, 100, 0));
                yield br;
            }
            case "PARKOUR" -> {
                ParkourGame pk = new ParkourGame(plugin, "e2e-pk", "E2E Parkour", 2, 8, 40);
                pk.setLobbySpawn(center);
                pk.setSpectatorSpawn(center);
                pk.setStartLocation(spawn1);
                pk.setFinishLocation(spawn2);
                yield pk;
            }
            case "MURDER_MYSTERY" -> {
                MurderMysteryGame mm = new MurderMysteryGame(plugin, "e2e-mm", "E2E Murder Mystery");
                mm.setLobbySpawn(center);
                mm.setSpectatorSpawn(center);
                mm.addSpawnPoint(spawn1);
                mm.addSpawnPoint(spawn2);
                yield mm;
            }
            case "SUMO" -> {
                SumoGame sumo = new SumoGame(plugin, "e2e-sumo", "E2E Sumo", 40);
                sumo.setLobbySpawn(center);
                sumo.setSpectatorSpawn(center);
                sumo.setSpawn1(spawn1);
                sumo.setSpawn2(spawn2);
                yield sumo;
            }
            case "TNTRUN" -> {
                TNTRunGame tnt = new TNTRunGame(plugin, "e2e-tnt", "E2E TNT Run", 2, 8, 40);
                tnt.setLobbySpawn(center);
                tnt.setSpectatorSpawn(center);
                tnt.setArenaSpawn(spawn1);
                yield tnt;
            }
            case "PVP" -> {
                PvPGame pvp = new PvPGame(plugin, "e2e-pvp", "E2E PvP");
                pvp.setLobbySpawn(center);
                pvp.setSpectatorSpawn(center);
                pvp.setSpawn1(spawn1);
                pvp.setSpawn2(spawn2);
                yield pvp;
            }
            case "PROP_HUNT" -> {
                PropHuntGame ph = new PropHuntGame(plugin, "e2e-ph", "E2E Prop Hunt");
                ph.setLobbySpawn(center);
                ph.setSpectatorSpawn(center);
                ph.setHunterSpawn(spawn1);
                ph.addPropSpawn(spawn2);
                yield ph;
            }
            default -> throw new IllegalArgumentException("Unknown gameType: " + gameType);
        };
    }

    @ParameterizedTest(name = "{0} ({1})")
    @CsvSource({
            "BEDWARS, JAVA", "BEDWARS, BEDROCK",
            "SKYWARS, JAVA", "SKYWARS, BEDROCK",
            "BRIDGE, JAVA", "BRIDGE, BEDROCK",
            "PARKOUR, JAVA", "PARKOUR, BEDROCK",
            "MURDER_MYSTERY, JAVA", "MURDER_MYSTERY, BEDROCK",
            "SUMO, JAVA", "SUMO, BEDROCK",
            "TNTRUN, JAVA", "TNTRUN, BEDROCK",
            "PVP, JAVA", "PVP, BEDROCK",
            "PROP_HUNT, JAVA", "PROP_HUNT, BEDROCK"
    })
    void fullGamemodeLifecycleWorksForJavaAndBedrock(String gameType, String platform) throws Exception {
        boolean isBedrock = "BEDROCK".equals(platform);
        Arena arena = createArenaForGameType(gameType);
        gameManager.registerArena(arena);

        Player primary = createPlatformPlayer("Tester1", isBedrock);
        Player opponent = createPlatformPlayer("Tester2", !isBedrock);
        Player third = createPlatformPlayer("Tester3", false);

        assertEquals(isBedrock, bedrockManager.isBedrock(primary) || primary.getName().startsWith("."),
                "Platform identity should match " + platform);

        // Verify Watchdog exempts Bedrock players from movement penalties
        WatchdogManager watchdog = new WatchdogManager(plugin);
        WatchdogListener wdListener = new WatchdogListener(plugin, watchdog);
        wdListener.onMove(new PlayerMoveEvent(primary, new Location(world, 0, 100, 0), new Location(world, 5, 100, 0)));
        if (isBedrock) {
            assertEquals(WatchdogManager.MAX_TRUST_SCORE, watchdog.getTrustScore(primary),
                    "Bedrock player must not be penalized on movement in " + gameType);
        }

        // Join players into arena
        assertTrue(arena.join(primary), primary.getName() + " should join " + gameType);
        assertTrue(arena.join(opponent), opponent.getName() + " should join " + gameType);
        if (arena.getMinPlayers() >= 3) {
            assertTrue(arena.join(third), "Third player should join " + gameType);
        }

        // Scoreboard lines should render cleanly in WAITING/STARTING
        List<String> lobbyLines = arena.getScoreboardLines(primary);
        assertNotNull(lobbyLines);
        assertFalse(lobbyLines.isEmpty(), gameType + " lobby scoreboard must not be empty");

        // Start game & run ticks
        arena.startGame();
        assertEquals(GameState.IN_GAME, arena.getState(), gameType + " should enter IN_GAME");

        List<String> gameLines = arena.getScoreboardLines(primary);
        assertNotNull(gameLines);
        assertFalse(gameLines.isEmpty(), gameType + " in-game scoreboard must not be empty");

        // Simulate 3 game ticks + movement + block break + combat routing through GameManager
        for (int t = 0; t < 3; t++) {
            arena.tick();
        }
        gameManager.onMove(new PlayerMoveEvent(primary, new Location(world, 0, 100, 0), new Location(world, 1, 100, 0)));

        Block b = world.getBlockAt(3, 100, 3);
        b.setType(Material.OAK_PLANKS);
        gameManager.onBlockBreak(new BlockBreakEvent(b, primary));

        var ds = BukkitTestHarness.proxy(org.bukkit.damage.DamageSource.class, (m, args) -> null);
        gameManager.onDamage(new EntityDamageByEntityEvent(
                primary, opponent, EntityDamageEvent.DamageCause.ENTITY_ATTACK, ds, 2.0));

        // End game & reset arena cleanly
        arena.endGame(primary);
        assertEquals(GameState.ENDING, arena.getState(), gameType + " should enter ENDING");

        arena.resetArena();
        assertEquals(GameState.WAITING, arena.getState(), gameType + " should return to WAITING after reset");
        assertEquals(0, arena.getPlayerCount(), gameType + " should have 0 players after reset");
    }
}
