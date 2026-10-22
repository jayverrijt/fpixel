package net.fpixel.core.map;

import net.fpixel.FPixelPlugin;
import net.fpixel.games.bedwars.BedWarsGame;
import net.fpixel.games.bedwars.BedWarsSpawner;
import net.fpixel.games.bedwars.BedWarsTeam;
import net.fpixel.games.bridge.BridgeGame;
import net.fpixel.games.murdermystery.MurderMysteryGame;
import net.fpixel.games.parkour.ParkourGame;
import net.fpixel.games.prophunt.PropHuntGame;
import net.fpixel.games.pvp.PvPGame;
import net.fpixel.games.skywars.SkyWarsGame;
import net.fpixel.games.sumo.SumoGame;
import net.fpixel.games.tntrun.TNTRunGame;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import net.fpixel.core.util.ChatUtil;

import java.io.File;

public class WorldManager {
    private final FPixelPlugin plugin;

    public WorldManager(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    public void generateAllMaps(World defaultWorld) {
        plugin.getLogger().info("Loading maps, downloaded worlds, and configuring 1v1 duels & global lobbies...");

        // Ensure lobby world is peaceful and sunny
        defaultWorld.setDifficulty(Difficulty.PEACEFUL);
        defaultWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        defaultWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        defaultWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        defaultWorld.setTime(6000L);

        // 1. Grand Castle Hub in default world at (0, 100, 0)
        buildGrandHub(defaultWorld);

        // Dedicated pure void world for all minigames (Sumo, TNT Run, SkyWars, BedWars, Parkour, Bridge, Murder Mystery, Prop Hunt)
        World arenaWorld = getOrCreateArenaWorld(defaultWorld);

        // 2. Global Multiplayer BedWars (Min 3, Max 8-16) - Loads downloaded worlds if available
        setupGlobalBedWarsMaps(arenaWorld);

        // 3. Global Multiplayer SkyWars (Min 3, Max 12)
        setupGlobalSkyWarsMaps(arenaWorld);

        // 4. Global Multiplayer TNT Run (Min 3, Max 16)
        setupGlobalTNTRunMaps(arenaWorld);

        // 5. Global Multiplayer Parkour (Min 3, Max 16)
        setupGlobalParkourMaps(arenaWorld);

        // 6. Dedicated 1v1 Duel Arenas (Min 2, Max 2) for:
        //    SkyWars, BedWars, PvP, Parkour, TNT, Sumo, Bridge
        setupAllDuelArenas(arenaWorld);

        // 7. Murder Mystery (Min 3, Max 4)
        setupMurderMysteryMaps(arenaWorld);

        // 8. Prop Hunt (Min 2, Max 4)
        setupPropHuntMaps(arenaWorld);

        plugin.getLogger().info("Map setup complete! All global lobbies and 1v1 duel arenas are live in void world.");
    }

    private World getOrCreateArenaWorld(World fallback) {
        try {
            WorldCreator wc = new WorldCreator("arenas");
            wc.generator(new VoidChunkGenerator());
            World w = wc.createWorld();
            if (w != null) {
                w.setAutoSave(false);
                w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
                w.setGameRule(GameRule.DO_TILE_DROPS, true);
                w.setGameRule(GameRule.DO_ENTITY_DROPS, true);
                w.setTime(6000L);
                return w;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create void arena world: " + e.getMessage());
        }
        return fallback;
    }

    private World loadOrCreateWorld(String worldName, World fallback) {
        File dir = new File(plugin.getServer().getWorldContainer(), worldName);
        if (dir.exists() && dir.isDirectory()) {
            try {
                plugin.getLogger().info("Loading downloaded world map: " + worldName + "...");
                WorldCreator wc = new WorldCreator(worldName);
                World w = wc.createWorld();
                if (w != null) {
                    w.setAutoSave(false);
                    w.setKeepSpawnInMemory(false);
                    w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                    w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
                    w.setTime(6000L);
                    return w;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load world " + worldName + ": " + e.getMessage());
            }
        }
        return fallback;
    }

    // =========================================================================
    // 1. Grand Minigame Hub & Launch Spot (Survival-Inspired Architecture)
    // =========================================================================

    public void rebuildGrandHub(World world) {
        buildGrandHub(world);
        Location spawn = plugin.getLobbyManager().getSpawnLocation();
        if (spawn != null) {
            for (Player p : world.getPlayers()) {
                p.teleport(spawn);
                ChatUtil.sendMessage(p, "§aGrand Hub has been updated!");
            }
        }
    }

    private void buildGrandHub(World world) {
        int cy = 100;
        plugin.getLogger().info("Rebuilding Grand Hub Archipelago: 9 Expanded Floating Islands, Spaced Out Biomes, Zero Stairs, and Clear Launchpad Trajectories...");

        // 1. Purge all ground items and stray non-player entities in default world
        for (Entity e : world.getEntities()) {
            if (e instanceof Item || (e instanceof LivingEntity && !(e instanceof Player))) {
                e.remove();
            }
        }
        world.setGameRule(GameRule.DO_TILE_DROPS, false);
        world.setGameRule(GameRule.DO_ENTITY_DROPS, false);

        // 2. Clear all previous hub structures and vanilla generated terrain to open sky
        MapBuilder.clearVanillaTerrain(world, 0, 0, 180);
        for (int y = cy - 30; y <= cy + 60; y++) {
            MapBuilder.fillCircle(world, 0, y, 0, 130, Material.AIR);
        }

        // =========================================================================
        // 3. ISLAND 1: CENTRAL GRAND HARVEST PLAZA (Center: 0, cy, 0, Radius 18)
        // =========================================================================
        MapBuilder.buildFloatingIslandNatural(world, 0, cy, 0, 18, Material.POLISHED_DEEPSLATE, Material.STONE_BRICKS, Material.DEEPSLATE);

        // Concentric Plaza Rings
        MapBuilder.fillCircle(world, 0, cy, 0, 17, Material.POLISHED_DEEPSLATE);
        MapBuilder.fillRing(world, 0, cy, 0, 15, 16, Material.MUD_BRICKS);
        MapBuilder.fillRing(world, 0, cy, 0, 12, 14, Material.SPRUCE_PLANKS);
        MapBuilder.fillRing(world, 0, cy, 0, 8, 11, Material.SMOOTH_QUARTZ);
        MapBuilder.fillRing(world, 0, cy, 0, 6, 7, Material.POLISHED_ANDESITE);

        // Monumental Tiered Marble/Quartz Fountain with Skyward Beacon Spire (Using Slabs, Zero Stairs)
        MapBuilder.buildGrandFountain(world, 0, cy, 0, 5);
        world.getBlockAt(0, cy + 1, 0).setType(Material.BEACON, false);
        MapBuilder.buildCelestialRuneCrown(world, 0, cy, 0);

        // 4 Blooming Japanese Cherry Blossom Trees close to the fountain (Never obstructing outer launchpads!)
        MapBuilder.buildCherryTree(world, -7, cy + 1, -7, 5);
        MapBuilder.buildCherryTree(world, 7, cy + 1, -7, 5);
        MapBuilder.buildCherryTree(world, -7, cy + 1, 7, 5);
        MapBuilder.buildCherryTree(world, 7, cy + 1, 7, 5);

        // Azalea bushes, flowering azaleas and lanterns around plaza edge (leaving launchpads and bridge mouths open)
        for (int angle = 0; angle < 360; angle += 15) {
            if (angle % 45 == 0) continue; // Keep cardinal & diagonal exits 100% clear!
            double rad = Math.toRadians(angle);
            int bx = (int) Math.round(17 * Math.cos(rad));
            int bz = (int) Math.round(17 * Math.sin(rad));
            if (angle % 30 == 0) {
                world.getBlockAt(bx, cy + 1, bz).setType(Material.FLOWERING_AZALEA, false);
            } else {
                world.getBlockAt(bx, cy + 1, bz).setType(Material.STONE_BRICK_WALL, false);
                world.getBlockAt(bx, cy + 2, bz).setType(Material.LANTERN, false);
            }
        }

        // Directional Road Signposts around Plaza
        MapBuilder.placeDirectionalSign(world, 0, cy + 1, -13, "§a§l▲ NORTH ▲", "§2§lROYAL CITADEL", "§7Survival SMP Gateway");
        MapBuilder.placeDirectionalSign(world, -13, cy + 1, 0, "§c§l◀ WEST ◀", "§c§lMEDIEVAL VILLAGE", "§7BedWars & Bazaar");
        MapBuilder.placeDirectionalSign(world, 10, cy + 1, -10, "§6§l↗ NORTH-EAST", "§6§lROMAN COLOSSEUM", "§7TNT Run & Arena");
        MapBuilder.placeDirectionalSign(world, 10, cy + 1, 10, "§e§l↘ SOUTH-EAST", "§e§lAIRSHIP HARBOR", "§7Observatory & Zeppelins");
        MapBuilder.placeDirectionalSign(world, -10, cy + 1, 10, "§d§l↙ SOUTH-WEST", "§d§lTORII SHRINE", "§7Sumo 1v1 Dojo");
        MapBuilder.placeDirectionalSign(world, 0, cy + 1, 13, "§b§l▼ SOUTH ▼", "§b§lTOURNAMENT HALL", "§7Duels & PvP");
        MapBuilder.placeDirectionalSign(world, 13, cy + 1, 0, "§b§l▶ EAST ▶", "§b§lSKYWARS SANCTUARY", "§7Celestial Void Arena");
        MapBuilder.placeDirectionalSign(world, -10, cy + 1, -10, "§a§l↖ NORTH-WEST", "§a§lPARKOUR RUINS", "§7Agility Courses & Spire");

        // The 8 Super Launchpads Radiating Outwards from Central Plaza (Radius 15 - Completely Unobstructed Sky!)
        int[][] launchpadCoords = {
                {0, -15},   // North -> Royal Citadel (0, -68)
                {11, -11},  // North-East -> Roman Colosseum (60, -48)
                {15, 0},    // East -> SkyWars Sanctuary (72, 10)
                {11, 11},   // South-East -> Airship Harbor (55, 65)
                {0, 15},    // South -> Duels Citadel (0, 76)
                {-11, 11},  // South-West -> Sumo Torii ( -55, 65)
                {-15, 0},   // West -> Medieval Village (-70, 0)
                {-11, -11}  // North-West -> Parkour Ruins (-58, -54)
        };
        for (int[] lp : launchpadCoords) {
            int lx = lp[0], lz = lp[1];
            MapBuilder.fillRing(world, lx, cy, lz, 1, 2, Material.GOLD_BLOCK);
            world.getBlockAt(lx, cy, lz).setType(Material.EMERALD_BLOCK, false);
            world.getBlockAt(lx, cy + 1, lz).setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, false);
            world.getBlockAt(lx, cy - 1, lz).setType(Material.SEA_LANTERN, false);
        }

        // =========================================================================
        // 4. ISLAND 2: THE ROYAL CITADEL (North: 0, cy, -68, Radius 22, Gap to Plaza: 28 blocks)
        // =========================================================================
        // Grand Arched Stone Bridge connecting Central Plaza to Royal Citadel (Zero Stairs, Slabs & Walls)
        MapBuilder.buildArchedStoneBridge(world, 0, -18, 0, -46, cy, 5);

        // Floating Citadel Island Base
        MapBuilder.buildFloatingIslandNatural(world, 0, cy, -68, 22, Material.STONE_BRICKS, Material.COBBLESTONE, Material.DEEPSLATE);

        // Arrival Landing Platform & Return Launchpad to Plaza
        MapBuilder.fillRing(world, 0, cy, -46, 1, 2, Material.GOLD_BLOCK);
        world.getBlockAt(0, cy, -46).setType(Material.EMERALD_BLOCK, false);
        world.getBlockAt(0, cy + 1, -46).setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, false);
        world.getBlockAt(0, cy - 1, -46).setType(Material.SEA_LANTERN, false);

        // Twin Soaring Citadel Bastion Towers
        MapBuilder.buildCastleTowerRound(world, -12, cy, -68, 4, 26, Material.STONE_BRICKS, Material.DARK_OAK_PLANKS);
        MapBuilder.buildCastleTowerRound(world, 12, cy, -68, 4, 26, Material.STONE_BRICKS, Material.DARK_OAK_PLANKS);

        // Monumental Vaulted Gatehouse Arch connecting the twin towers
        MapBuilder.fillBox(world, -7, cy + 1, -69, 7, cy + 14, -67, Material.STONE_BRICKS);
        MapBuilder.fillBox(world, -4, cy + 1, -69, 4, cy + 8, -67, Material.AIR); // arch tunnel
        MapBuilder.fillBox(world, -4, cy + 7, -68, 4, cy + 7, -68, Material.IRON_BARS); // portcullis top
        MapBuilder.fillBox(world, -7, cy + 15, -70, 7, cy + 15, -66, Material.POLISHED_ANDESITE); // battlements
        for (int gx = -7; gx <= 7; gx += 2) {
            world.getBlockAt(gx, cy + 16, -70).setType(Material.STONE_BRICK_WALL, false);
            world.getBlockAt(gx, cy + 16, -66).setType(Material.STONE_BRICK_WALL, false);
        }
        world.getBlockAt(-7, cy + 17, -70).setType(Material.LANTERN, false);
        world.getBlockAt(7, cy + 17, -70).setType(Material.LANTERN, false);

        // Radiant Skyward Cyan Beacon shooting from the citadel gateway
        MapBuilder.buildColoredBeacon(world, 0, cy, -68, Material.CYAN_STAINED_GLASS);

        // Survival SMP Portal Courtyard (Behind Gatehouse at 0, -78)
        int sgZ = -78;
        MapBuilder.buildPillar(world, -4, cy + 1, sgZ, 7, Material.STRIPPED_OAK_LOG);
        MapBuilder.buildPillar(world, 4, cy + 1, sgZ, 7, Material.STRIPPED_OAK_LOG);
        for (int px = -5; px <= 5; px++) {
            world.getBlockAt(px, cy + 8, sgZ).setType(Material.SPRUCE_SLAB, false);
            world.getBlockAt(px, cy + 7, sgZ).setType(Material.DARK_OAK_PLANKS, false);
            world.getBlockAt(px, cy + 6, sgZ).setType(Material.STRIPPED_OAK_LOG, false);
        }
        world.getBlockAt(-4, cy + 5, sgZ).setType(Material.LANTERN, false);
        world.getBlockAt(4, cy + 5, sgZ).setType(Material.LANTERN, false);
        world.getBlockAt(0, cy + 1, sgZ).setType(Material.CAMPFIRE, false);
        world.getBlockAt(0, cy + 2, sgZ).setType(Material.COBBLESTONE_WALL, false);
        world.getBlockAt(-5, cy + 1, sgZ).setType(Material.BARREL, false);
        world.getBlockAt(5, cy + 1, sgZ).setType(Material.CHEST, false);
        MapBuilder.buildColoredBeacon(world, 0, cy, -84, Material.LIME_STAINED_GLASS);

        // =========================================================================
        // 5. ISLAND 3: MEDIEVAL VILLAGE & BAZAAR (West: -70, cy, 0, Radius 22, Gap to Plaza: 30 blocks)
        // =========================================================================
        // Arched Stone Bridge connecting Central Plaza to Medieval Village (Zero Stairs)
        MapBuilder.buildArchedStoneBridge(world, -18, 0, -48, 0, cy, 5);

        // Floating Village Island Base
        MapBuilder.buildFloatingIslandNatural(world, -70, cy, 0, 22, Material.GRASS_BLOCK, Material.DIRT, Material.DEEPSLATE);

        // Arrival Landing Platform & Return Launchpad to Plaza
        MapBuilder.fillRing(world, -48, cy, 0, 1, 2, Material.GOLD_BLOCK);
        world.getBlockAt(-48, cy, 0).setType(Material.EMERALD_BLOCK, false);
        world.getBlockAt(-48, cy + 1, 0).setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, false);
        world.getBlockAt(-48, cy - 1, 0).setType(Material.SEA_LANTERN, false);

        // Half-Timbered Houses (Using Slabs, Zero Stairs)
        MapBuilder.buildAutumnTownHouse(world, -76, cy, -10, 6, 6, Material.MUD_BRICKS, Material.SPRUCE_SLAB);
        MapBuilder.buildAutumnTownHouse(world, -76, cy, 10, 6, 6, Material.WHITE_TERRACOTTA, Material.DARK_OAK_SLAB);
        MapBuilder.buildBedWarsBaseForge(world, -64, cy, -12, Material.STONE_BRICKS, Material.DEEPSLATE_BRICKS, Material.RED_TERRACOTTA);

        // Medieval Bazaar Market Stalls
        MapBuilder.buildMarketStall(world, -66, cy, -3, Material.RED_WOOL, Material.WHITE_WOOL);
        MapBuilder.buildMarketStall(world, -66, cy, 4, Material.YELLOW_WOOL, Material.ORANGE_WOOL);

        // BedWars Pavilion & Keep
        MapBuilder.buildGamePavilion(world, -70, cy, 0, 5, Material.STONE_BRICKS, Material.DEEPSLATE_BRICK_WALL, Material.RED_TERRACOTTA, Material.RED_GLAZED_TERRACOTTA);
        world.getBlockAt(-70, cy + 1, -3).setType(Material.RED_BED, false);
        world.getBlockAt(-70, cy + 1, 3).setType(Material.BLUE_BED, false);
        MapBuilder.buildColoredBeacon(world, -80, cy, 0, Material.RED_STAINED_GLASS);

        // =========================================================================
        // 6. ISLAND 4: ROMAN COLOSSEUM & RUINS (North-East: 60, cy, -48, Radius 20, Gap: 38 blocks)
        // =========================================================================
        // Arched Stone Bridge connecting Central Plaza to Roman Colosseum
        MapBuilder.buildArchedStoneBridge(world, 13, -13, 45, -36, cy, 4);

        // Floating Colosseum Island Base
        MapBuilder.buildFloatingIslandNatural(world, 60, cy, -48, 20, Material.SANDSTONE, Material.CUT_SANDSTONE, Material.DEEPSLATE);

        // Arrival Landing Platform & Return Launchpad to Plaza
        MapBuilder.fillRing(world, 45, cy, -36, 1, 2, Material.GOLD_BLOCK);
        world.getBlockAt(45, cy, -36).setType(Material.EMERALD_BLOCK, false);
        world.getBlockAt(45, cy + 1, -36).setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, false);
        world.getBlockAt(45, cy - 1, -36).setType(Material.SEA_LANTERN, false);

        // 2-Tier Curved Roman Colosseum Arcade (Slabs, Zero Stairs)
        MapBuilder.buildRomanColosseumCurved(world, 60, cy + 1, -48, 14, -70, 110);

        // Colosseum Arena Interior
        MapBuilder.buildGamePavilion(world, 60, cy, -48, 5, Material.NETHER_BRICKS, Material.NETHER_BRICK_WALL, Material.RED_NETHER_BRICKS, Material.MAGMA_BLOCK);
        world.getBlockAt(63, cy + 1, -48).setType(Material.TNT, false);
        world.getBlockAt(60, cy + 1, -45).setType(Material.SOUL_CAMPFIRE, false);
        MapBuilder.buildColoredBeacon(world, 72, cy, -48, Material.ORANGE_STAINED_GLASS);

        // =========================================================================
        // 7. ISLAND 5: AIRSHIP HARBOR & OBSERVATORY (South-East: 55, cy, 65, Radius 20, Gap: 47 blocks)
        // =========================================================================
        // Arched Wooden Bridge connecting Central Plaza to Airship Harbor
        MapBuilder.buildArchedWoodenBridge(world, 13, 13, 40, 48, cy, 4);

        // Floating Harbor Island Base
        MapBuilder.buildFloatingIslandNatural(world, 55, cy, 65, 20, Material.GRASS_BLOCK, Material.DIRT, Material.DEEPSLATE);

        // Arrival Landing Platform & Return Launchpad to Plaza
        MapBuilder.fillRing(world, 40, cy, 48, 1, 2, Material.GOLD_BLOCK);
        world.getBlockAt(40, cy, 48).setType(Material.EMERALD_BLOCK, false);
        world.getBlockAt(40, cy + 1, 48).setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, false);
        world.getBlockAt(40, cy - 1, 48).setType(Material.SEA_LANTERN, false);

        // Celestial Glass-Domed Observatory
        MapBuilder.buildObservatory(world, 48, cy + 1, 56, 5);

        // Wooden Airship Docking Boardwalk Pier extending over the void
        for (int px = 50; px <= 68; px++) {
            for (int pz = 64; pz <= 67; pz++) {
                world.getBlockAt(px, cy, pz).setType(Material.SPRUCE_PLANKS, false);
                world.getBlockAt(px, cy - 1, pz).setType(Material.DARK_OAK_LOG, false);
            }
            world.getBlockAt(px, cy + 1, 64).setType(Material.SPRUCE_FENCE, false);
            if (px % 4 == 0) {
                world.getBlockAt(px, cy + 2, 64).setType(Material.LANTERN, false);
            }
        }

        // Moored Steampunk Zeppelin Airship moored alongside the pier
        MapBuilder.buildSteampunkAirship(world, 62, cy + 7, 72);

        // Gangway connecting pier to airship (Spruce slabs)
        for (int z = 67; z <= 71; z++) {
            world.getBlockAt(60, cy + 1, z).setType(Material.SPRUCE_SLAB, false);
            world.getBlockAt(61, cy + 1, z).setType(Material.SPRUCE_SLAB, false);
        }

        // =========================================================================
        // 8. ISLAND 6: SACRED JAPANESE TORII SHRINE (South-West: -55, cy, 65, Radius 20, Gap: 47 blocks)
        // =========================================================================
        // Arched Crimson Bridge connecting Central Plaza to Torii Island
        MapBuilder.buildArchedCrimsonBridge(world, -13, 13, -40, 48, cy, 4);

        // Floating Sacred Island Base
        MapBuilder.buildFloatingIslandNatural(world, -55, cy, 65, 20, Material.MOSS_BLOCK, Material.DIRT, Material.DEEPSLATE);

        // Arrival Landing Platform & Return Launchpad to Plaza
        MapBuilder.fillRing(world, -40, cy, 48, 1, 2, Material.GOLD_BLOCK);
        world.getBlockAt(-40, cy, 48).setType(Material.EMERALD_BLOCK, false);
        world.getBlockAt(-40, cy + 1, 48).setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, false);
        world.getBlockAt(-40, cy - 1, 48).setType(Material.SEA_LANTERN, false);

        // Crimson Torii Gate (Dark Oak Slabs, Zero Stairs)
        MapBuilder.buildToriiArch(world, -55, cy + 1, 58);

        // Blooming Cherry Blossom Trees with falling petals
        MapBuilder.buildCherryTree(world, -60, cy + 1, 70, 6);
        MapBuilder.buildCherryTree(world, -48, cy + 1, 72, 6);

        // Circular Golden Sumo Ring
        MapBuilder.fillCircle(world, -55, cy, 65, 5, Material.POLISHED_ANDESITE);
        MapBuilder.fillRing(world, -55, cy, 65, 4, 5, Material.GOLD_BLOCK);
        MapBuilder.buildColoredBeacon(world, -64, cy, 72, Material.WHITE_STAINED_GLASS);

        // =========================================================================
        // 9. ISLAND 7: SKYWARS CELESTIAL SANCTUARY (East: 72, cy, 10, Radius 20, Gap: 34 blocks)
        // =========================================================================
        MapBuilder.buildFloatingIslandNatural(world, 72, cy, 10, 20, Material.SMOOTH_QUARTZ, Material.QUARTZ_BRICKS, Material.DEEPSLATE);

        // Arched crystal walkway connecting from Central Plaza
        for (int x = 18; x <= 52; x++) {
            world.getBlockAt(x, cy, 10).setType(Material.SMOOTH_QUARTZ, false);
            world.getBlockAt(x, cy - 1, 10).setType(Material.SEA_LANTERN, false);
            if (x % 5 == 0) {
                world.getBlockAt(x, cy + 1, 9).setType(Material.STONE_BRICK_WALL, false);
                world.getBlockAt(x, cy + 1, 11).setType(Material.STONE_BRICK_WALL, false);
                world.getBlockAt(x, cy + 2, 9).setType(Material.LANTERN, false);
                world.getBlockAt(x, cy + 2, 11).setType(Material.LANTERN, false);
            }
        }

        // Arrival Landing Platform & Return Launchpad to Plaza
        MapBuilder.fillRing(world, 52, cy, 10, 1, 2, Material.GOLD_BLOCK);
        world.getBlockAt(52, cy, 10).setType(Material.EMERALD_BLOCK, false);
        world.getBlockAt(52, cy + 1, 10).setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, false);
        world.getBlockAt(52, cy - 1, 10).setType(Material.SEA_LANTERN, false);

        MapBuilder.buildGamePavilion(world, 72, cy, 10, 5, Material.SMOOTH_QUARTZ, Material.QUARTZ_PILLAR, Material.PURPUR_BLOCK, Material.AMETHYST_BLOCK);
        world.getBlockAt(72, cy + 1, 13).setType(Material.ENCHANTING_TABLE, false);
        world.getBlockAt(72, cy + 1, 7).setType(Material.ENDER_CHEST, false);
        MapBuilder.buildColoredBeacon(world, 80, cy, 10, Material.LIGHT_BLUE_STAINED_GLASS);

        // =========================================================================
        // 10. ISLAND 8: DUELS & PVP CITADEL (South: 0, cy, 76, Radius 20, Gap: 38 blocks)
        // =========================================================================
        MapBuilder.buildArchedStoneBridge(world, 0, 18, 0, 56, cy, 5);
        MapBuilder.buildFloatingIslandNatural(world, 0, cy, 76, 20, Material.POLISHED_ANDESITE, Material.STONE_BRICKS, Material.DEEPSLATE);

        // Arrival Landing Platform & Return Launchpad to Plaza
        MapBuilder.fillRing(world, 0, cy, 56, 1, 2, Material.GOLD_BLOCK);
        world.getBlockAt(0, cy, 56).setType(Material.EMERALD_BLOCK, false);
        world.getBlockAt(0, cy + 1, 56).setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, false);
        world.getBlockAt(0, cy - 1, 56).setType(Material.SEA_LANTERN, false);

        MapBuilder.buildGamePavilion(world, 0, cy, 76, 5, Material.POLISHED_ANDESITE, Material.STONE_BRICK_WALL, Material.DARK_OAK_PLANKS, Material.IRON_BLOCK);
        world.getBlockAt(-3, cy + 1, 76).setType(Material.HAY_BLOCK, false);
        world.getBlockAt(-3, cy + 2, 76).setType(Material.TARGET, false);
        world.getBlockAt(3, cy + 1, 76).setType(Material.SMITHING_TABLE, false);
        MapBuilder.buildColoredBeacon(world, 0, cy, 84, Material.MAGENTA_STAINED_GLASS);

        // =========================================================================
        // 11. ISLAND 9: PARKOUR AGILITY SPIRE & RUINS (North-West: -58, cy, -54, Radius 20, Gap: 41 blocks)
        // =========================================================================
        MapBuilder.buildArchedStoneBridge(world, -13, -13, -44, -40, cy, 4);
        MapBuilder.buildFloatingIslandNatural(world, -58, cy, -54, 20, Material.MOSS_BLOCK, Material.DIRT, Material.DEEPSLATE);

        // Arrival Landing Platform & Return Launchpad to Plaza
        MapBuilder.fillRing(world, -44, cy, -40, 1, 2, Material.GOLD_BLOCK);
        world.getBlockAt(-44, cy, -40).setType(Material.EMERALD_BLOCK, false);
        world.getBlockAt(-44, cy + 1, -40).setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, false);
        world.getBlockAt(-44, cy - 1, -40).setType(Material.SEA_LANTERN, false);

        // Parkour Pavilion (Spruce slabs, zero stairs)
        MapBuilder.buildGamePavilion(world, -58, cy, -54, 5, Material.OAK_PLANKS, Material.OAK_LOG, Material.SPRUCE_SLAB, Material.EMERALD_BLOCK);
        world.getBlockAt(-62, cy + 1, -54).setType(Material.SLIME_BLOCK, false);
        world.getBlockAt(-62, cy + 2, -54).setType(Material.GOLD_BLOCK, false);
        MapBuilder.buildColoredBeacon(world, -68, cy, -54, Material.GREEN_STAINED_GLASS);

        // Rustic Windmill on Parkour Island (Slabs, zero stairs)
        MapBuilder.buildWindmill(world, -66, cy + 1, -62);

        // 12. Furnish & Decorate All 9 Islands with Paths, Gardens, and Vistas
        decorateGrandPlaza(world, cy);
        decorateRoyalCitadel(world, cy);
        decorateMedievalVillage(world, cy);
        decorateRomanColosseum(world, cy);
        decorateAirshipHarbor(world, cy);
        decorateToriiShrine(world, cy);
        decorateSkyWarsSanctuary(world, cy);
        decorateDuelsCitadel(world, cy);
        decorateParkourRuins(world, cy);

        // 13. Spawn Point at Grand Central Plaza facing North (towards Fountain, Cherry Trees & Royal Citadel)
        Location hubSpawn = new Location(world, 0.5, cy + 1.5, 6.5, 0f, 0f);
        world.setSpawnLocation(0, cy + 2, 6);
        plugin.getLobbyManager().setSpawn(hubSpawn);

        // 13. Spawn All Interactive Gamemode & Lore NPCs with Floating Holograms!
        plugin.getHubNPCManager().spawnAllNPCs(world);

        // 14. Spawn All 7 Ancient Lore Relics & Secrets Displays!
        plugin.getHubLoreManager().spawnAllRelics(world);

        plugin.getLogger().info("Grand Hub Archipelago rebuild complete! 9 Sprawling Floating Islands, Slabs Only, Unobstructed Launchpads, and Scenic Vistas are live.");
    }

    // =========================================================================
    // Grand Hub Island Decoration & Furnishing Methods (Paths, Gardens, Vistas)
    // =========================================================================

    private void setPathBlock(World world, int x, int z, Material mat) {
        for (int y = 105; y >= 95; y--) {
            Material existing = world.getBlockAt(x, y, z).getType();
            if (existing.isSolid() && !existing.name().endsWith("_WALL") && !existing.name().endsWith("_FENCE")
                    && !existing.name().endsWith("_SLAB") && !existing.name().endsWith("_STAIRS")
                    && !existing.name().endsWith("_PRESSURE_PLATE")) {
                world.getBlockAt(x, y, z).setType(mat, false);
                return;
            }
        }
    }

    private void setSurfaceBlock(World world, int x, int z, Material mat) {
        for (int y = 105; y >= 95; y--) {
            Material existing = world.getBlockAt(x, y, z).getType();
            if (existing.isSolid() && !existing.name().endsWith("_WALL") && !existing.name().endsWith("_FENCE")
                    && !existing.name().endsWith("_PRESSURE_PLATE")) {
                world.getBlockAt(x, y + 1, z).setType(mat, false);
                return;
            }
        }
    }

    private int getGroundY(World world, int x, int z) {
        for (int y = 105; y >= 95; y--) {
            if (world.getBlockAt(x, y, z).getType().isSolid()) {
                return y;
            }
        }
        return 100;
    }

    private void decorateGrandPlaza(World world, int cy) {
        // Radial paved paths connecting fountain plaza rings to launchpads & bridges
        for (int z = -15; z <= -6; z++) {
            for (int x = -1; x <= 1; x++) setPathBlock(world, x, z, Material.POLISHED_ANDESITE);
        }
        for (int z = 6; z <= 15; z++) {
            for (int x = -1; x <= 1; x++) setPathBlock(world, x, z, Material.POLISHED_ANDESITE);
        }
        for (int x = -15; x <= -6; x++) {
            for (int z = -1; z <= 1; z++) setPathBlock(world, x, z, Material.POLISHED_ANDESITE);
        }
        for (int x = 6; x <= 15; x++) {
            for (int z = -1; z <= 1; z++) setPathBlock(world, x, z, Material.POLISHED_ANDESITE);
        }

        // 4 Cozy Plaza Garden Benches around Fountain (Slabs + Spruce Trapdoor backs)
        int[][] benchLocs = {{-4, -4}, {4, -4}, {-4, 4}, {4, 4}};
        for (int[] b : benchLocs) {
            int bx = b[0], bz = b[1];
            int gy = getGroundY(world, bx, bz);
            world.getBlockAt(bx, gy + 1, bz).setType(Material.SMOOTH_QUARTZ_SLAB, false);
            // Flower planter beside bench
            world.getBlockAt(bx + (bx > 0 ? 1 : -1), gy + 1, bz).setType(Material.COMPOSTER, false);
            world.getBlockAt(bx + (bx > 0 ? 1 : -1), gy + 2, bz).setType(Material.FLOWERING_AZALEA, false);
        }

        // Pink petals ground scatter around the 4 cherry trees
        int[][] cherryCenters = {{-7, -7}, {7, -7}, {-7, 7}, {7, 7}};
        for (int[] c : cherryCenters) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if ((dx + dz) % 2 == 0 && (dx != 0 || dz != 0)) {
                        setSurfaceBlock(world, c[0] + dx, c[1] + dz, Material.PINK_PETALS);
                    }
                }
            }
        }

        // 4 Grand Lampposts at path corners
        int[][] lampLocs = {{-8, -8}, {8, -8}, {-8, 8}, {8, 8}};
        for (int[] lp : lampLocs) {
            int lx = lp[0], lz = lp[1];
            int gy = getGroundY(world, lx, lz);
            world.getBlockAt(lx, gy + 1, lz).setType(Material.POLISHED_BLACKSTONE_WALL, false);
            world.getBlockAt(lx, gy + 2, lz).setType(Material.POLISHED_BLACKSTONE_WALL, false);
            world.getBlockAt(lx, gy + 3, lz).setType(Material.LANTERN, false);
        }
    }

    private void decorateRoyalCitadel(World world, int cy) {
        // Courtyard Cobblestone & Stone Brick Avenue from bridge landing (-46) to Gatehouse (-68) & SMP (-78)
        for (int z = -46; z >= -78; z--) {
            for (int x = -2; x <= 2; x++) {
                Material m = ((x + z) % 3 == 0) ? Material.COBBLESTONE : (((x + z) % 5 == 0) ? Material.CRACKED_STONE_BRICKS : Material.STONE_BRICKS);
                setPathBlock(world, x, z, m);
            }
        }
        // Lateral paths to Bastion Towers
        for (int x = -12; x <= 12; x++) {
            setPathBlock(world, x, -68, Material.COBBLESTONE);
        }

        // Royal Guard Outpost / Armory on west side of courtyard (-6, -74)
        int gy1 = getGroundY(world, -6, -74);
        world.getBlockAt(-6, gy1 + 1, -74).setType(Material.ANVIL, false);
        world.getBlockAt(-6, gy1 + 1, -75).setType(Material.SMITHING_TABLE, false);
        world.getBlockAt(-6, gy1 + 1, -73).setType(Material.GRINDSTONE, false);
        world.getBlockAt(-7, gy1 + 1, -74).setType(Material.BARREL, false);
        world.getBlockAt(-7, gy1 + 2, -74).setType(Material.BARREL, false);

        // Royal Weapon Storage on east side of courtyard (6, -74)
        int gy2 = getGroundY(world, 6, -74);
        world.getBlockAt(6, gy2 + 1, -74).setType(Material.SPRUCE_FENCE, false);
        world.getBlockAt(6, gy2 + 2, -74).setType(Material.IRON_BARS, false);
        world.getBlockAt(7, gy2 + 1, -74).setType(Material.CHEST, false);
        world.getBlockAt(6, gy2 + 1, -75).setType(Material.BARREL, false);

        // Soul Campfire Fire Bowls framing the inner fortress approach
        int[][] bowls = {{-5, -55}, {5, -55}};
        for (int[] b : bowls) {
            int bx = b[0], bz = b[1];
            int gy = getGroundY(world, bx, bz);
            world.getBlockAt(bx, gy + 1, bz).setType(Material.CHISELED_DEEPSLATE, false);
            world.getBlockAt(bx, gy + 2, bz).setType(Material.DEEPSLATE_TILE_WALL, false);
            world.getBlockAt(bx, gy + 3, bz).setType(Material.SOUL_LANTERN, false);
        }

        // 2 Natural Mountain Spruce Trees framing the courtyard
        int[][] spruces = {{-14, -58}, {14, -58}};
        for (int[] s : spruces) {
            int sx = s[0], sz = s[1];
            int gy = getGroundY(world, sx, sz);
            for (int y = 1; y <= 5; y++) world.getBlockAt(sx, gy + y, sz).setType(Material.SPRUCE_LOG, false);
            // Leaves cone
            for (int dy = 3; dy <= 6; dy++) {
                int r = (dy >= 5) ? 1 : 2;
                for (int lx = -r; lx <= r; lx++) {
                    for (int lz = -r; lz <= r; lz++) {
                        if (lx == 0 && lz == 0 && dy <= 5) continue;
                        if (Math.abs(lx) + Math.abs(lz) <= r + 1) {
                            world.getBlockAt(sx + lx, gy + dy, sz + lz).setType(Material.SPRUCE_LEAVES, false);
                        }
                    }
                }
            }
            world.getBlockAt(sx, gy + 7, sz).setType(Material.SPRUCE_LEAVES, false);
        }
    }

    private void decorateMedievalVillage(World world, int cy) {
        // Winding Organic Cobblestone & Packed Mud pathways connecting village structures
        for (int x = -48; x >= -72; x--) {
            for (int z = -2; z <= 2; z++) {
                Material m = ((x + z) % 2 == 0) ? Material.PACKED_MUD : (((x + z) % 3 == 0) ? Material.COBBLESTONE : Material.MUD_BRICKS);
                setPathBlock(world, x, z, m);
            }
        }
        // Lateral path to houses and forge
        for (int z = -12; z <= 12; z++) {
            setPathBlock(world, -72, z, Material.COBBLESTONE);
            setPathBlock(world, -71, z, Material.PACKED_MUD);
            setPathBlock(world, -70, z, Material.GRAVEL);
        }

        // 1. Village Water Well at (-58, 0) - 3x3 Stone Brick with Water & Roof
        int wellX = -58, wellZ = 0;
        int wellY = getGroundY(world, wellX, wellZ);
        // Stone brick rim
        for (int wx = -1; wx <= 1; wx++) {
            for (int wz = -1; wz <= 1; wz++) {
                if (wx == 0 && wz == 0) {
                    world.getBlockAt(wellX, wellY, wellZ).setType(Material.WATER, false);
                    world.getBlockAt(wellX, wellY - 1, wellZ).setType(Material.WATER, false);
                } else {
                    world.getBlockAt(wellX + wx, wellY + 1, wellZ + wz).setType(Material.STONE_BRICKS, false);
                }
            }
        }
        // 4 Corner Spruce Fences
        world.getBlockAt(wellX - 1, wellY + 2, wellZ - 1).setType(Material.SPRUCE_FENCE, false);
        world.getBlockAt(wellX + 1, wellY + 2, wellZ - 1).setType(Material.SPRUCE_FENCE, false);
        world.getBlockAt(wellX - 1, wellY + 2, wellZ + 1).setType(Material.SPRUCE_FENCE, false);
        world.getBlockAt(wellX + 1, wellY + 2, wellZ + 1).setType(Material.SPRUCE_FENCE, false);
        world.getBlockAt(wellX - 1, wellY + 3, wellZ - 1).setType(Material.SPRUCE_FENCE, false);
        world.getBlockAt(wellX + 1, wellY + 3, wellZ - 1).setType(Material.SPRUCE_FENCE, false);
        world.getBlockAt(wellX - 1, wellY + 3, wellZ + 1).setType(Material.SPRUCE_FENCE, false);
        world.getBlockAt(wellX + 1, wellY + 3, wellZ + 1).setType(Material.SPRUCE_FENCE, false);
        // Roof of spruce slabs
        for (int wx = -1; wx <= 1; wx++) {
            for (int wz = -1; wz <= 1; wz++) {
                world.getBlockAt(wellX + wx, wellY + 4, wellZ + wz).setType(Material.SPRUCE_SLAB, false);
            }
        }
        world.getBlockAt(wellX, wellY + 3, wellZ).setType(Material.IRON_CHAIN, false);

        // 2. Harvest Farm Cart #1 at (-56, -8) loaded with Hay & Pumpkins
        int cart1X = -56, cart1Z = -8;
        int c1Y = getGroundY(world, cart1X, cart1Z);
        world.getBlockAt(cart1X - 1, c1Y + 1, cart1Z).setType(Material.DARK_OAK_TRAPDOOR, false);
        world.getBlockAt(cart1X + 1, c1Y + 1, cart1Z).setType(Material.DARK_OAK_TRAPDOOR, false);
        world.getBlockAt(cart1X, c1Y + 1, cart1Z).setType(Material.SPRUCE_PLANKS, false);
        world.getBlockAt(cart1X, c1Y + 1, cart1Z - 1).setType(Material.SPRUCE_PLANKS, false);
        world.getBlockAt(cart1X, c1Y + 2, cart1Z).setType(Material.HAY_BLOCK, false);
        world.getBlockAt(cart1X, c1Y + 2, cart1Z - 1).setType(Material.CARVED_PUMPKIN, false);
        world.getBlockAt(cart1X, c1Y + 1, cart1Z - 2).setType(Material.SPRUCE_FENCE, false); // shaft

        // 3. Trade Wagon #2 at (-56, 8) loaded with Barrels & Melons
        int cart2X = -56, cart2Z = 8;
        int c2Y = getGroundY(world, cart2X, cart2Z);
        world.getBlockAt(cart2X - 1, c2Y + 1, cart2Z).setType(Material.DARK_OAK_TRAPDOOR, false);
        world.getBlockAt(cart2X + 1, c2Y + 1, cart2Z).setType(Material.DARK_OAK_TRAPDOOR, false);
        world.getBlockAt(cart2X, c2Y + 1, cart2Z).setType(Material.SPRUCE_PLANKS, false);
        world.getBlockAt(cart2X, c2Y + 1, cart2Z + 1).setType(Material.SPRUCE_PLANKS, false);
        world.getBlockAt(cart2X, c2Y + 2, cart2Z).setType(Material.BARREL, false);
        world.getBlockAt(cart2X, c2Y + 2, cart2Z + 1).setType(Material.MELON, false);
        world.getBlockAt(cart2X, c2Y + 1, cart2Z + 2).setType(Material.SPRUCE_FENCE, false); // shaft

        // 4. Outdoor Blacksmith Forge Annex at (-64, -16)
        int fX = -64, fZ = -16;
        int fY = getGroundY(world, fX, fZ);
        world.getBlockAt(fX, fY + 1, fZ).setType(Material.BLAST_FURNACE, false);
        world.getBlockAt(fX + 1, fY + 1, fZ).setType(Material.ANVIL, false);
        world.getBlockAt(fX - 1, fY + 1, fZ).setType(Material.CAULDRON, false);
        world.getBlockAt(fX, fY + 1, fZ + 1).setType(Material.GRINDSTONE, false);
        world.getBlockAt(fX + 1, fY + 1, fZ + 1).setType(Material.SMITHING_TABLE, false);

        // 5. Archery Range at (-82, -8)
        int aX = -82, aZ = -8;
        int aY = getGroundY(world, aX, aZ);
        world.getBlockAt(aX, aY + 1, aZ).setType(Material.HAY_BLOCK, false);
        world.getBlockAt(aX, aY + 2, aZ).setType(Material.TARGET, false);
        world.getBlockAt(aX, aY + 1, aZ + 1).setType(Material.HAY_BLOCK, false);
        world.getBlockAt(aX, aY + 2, aZ + 1).setType(Material.TARGET, false);
        world.getBlockAt(aX + 3, aY + 1, aZ).setType(Material.FLETCHING_TABLE, false);
        world.getBlockAt(aX + 3, aY + 1, aZ + 1).setType(Material.SPRUCE_FENCE, false);

        // 6. Streetlamps throughout Village
        int[][] vLamps = {{-52, -4}, {-52, 4}, {-64, 8}, {-64, -8}};
        for (int[] vl : vLamps) {
            int lx = vl[0], lz = vl[1];
            int gy = getGroundY(world, lx, lz);
            world.getBlockAt(lx, gy + 1, lz).setType(Material.SPRUCE_FENCE, false);
            world.getBlockAt(lx, gy + 2, lz).setType(Material.SPRUCE_FENCE, false);
            world.getBlockAt(lx, gy + 3, lz).setType(Material.LANTERN, false);
        }

        // 7. Wildflower Patches across Village lawns
        int[][] flowers = {{-62, -5, 0}, {-62, 5, 1}, {-52, -10, 2}, {-52, 10, 3}, {-78, -4, 0}, {-78, 4, 1}};
        Material[] flowerMats = {Material.POPPY, Material.CORNFLOWER, Material.DANDELION, Material.ALLIUM};
        for (int[] fl : flowers) {
            setSurfaceBlock(world, fl[0], fl[1], flowerMats[fl[2]]);
        }
    }

    private void decorateRomanColosseum(World world, int cy) {
        // Sandstone paved plaza from bridge (45, -36) to Colosseum (60, -48)
        for (int x = 46; x <= 58; x++) {
            for (int z = -44; z <= -38; z++) {
                Material m = ((x + z) % 2 == 0) ? Material.SMOOTH_SANDSTONE : Material.CUT_SANDSTONE;
                setPathBlock(world, x, z, m);
            }
        }

        // Fallen Roman Column #1 at (52, -40) - collapsed marble monolith on grass
        int c1Y = getGroundY(world, 52, -40);
        world.getBlockAt(52, c1Y + 1, -40).setType(Material.CHISELED_SANDSTONE, false);
        world.getBlockAt(53, c1Y + 1, -40).setType(Material.QUARTZ_PILLAR, false);
        world.getBlockAt(54, c1Y + 1, -40).setType(Material.QUARTZ_PILLAR, false);
        world.getBlockAt(55, c1Y + 1, -40).setType(Material.SANDSTONE_SLAB, false);

        // Fallen Roman Column #2 at (68, -40)
        int c2Y = getGroundY(world, 68, -40);
        world.getBlockAt(68, c2Y + 1, -40).setType(Material.CHISELED_SANDSTONE, false);
        world.getBlockAt(67, c2Y + 1, -40).setType(Material.QUARTZ_PILLAR, false);
        world.getBlockAt(66, c2Y + 1, -40).setType(Material.SANDSTONE_SLAB, false);

        // 4 Roman Soul Fire Braziers framing the arena walkway
        int[][] braziers = {{-4, -2}, {4, -2}, {-4, 2}, {4, 2}};
        for (int[] bz : braziers) {
            int bx = 60 + bz[0], bzCoord = -48 + bz[1];
            int gy = getGroundY(world, bx, bzCoord);
            world.getBlockAt(bx, gy + 1, bzCoord).setType(Material.CUT_SANDSTONE, false);
            world.getBlockAt(bx, gy + 2, bzCoord).setType(Material.SANDSTONE_WALL, false);
            world.getBlockAt(bx, gy + 3, bzCoord).setType(Material.SOUL_CAMPFIRE, false);
        }

        // 2 Mediterranean Pencil Cypress Trees
        int[][] cypresses = {{50, -56}, {70, -56}};
        for (int[] cp : cypresses) {
            int cx = cp[0], cz = cp[1];
            int gy = getGroundY(world, cx, cz);
            for (int y = 1; y <= 6; y++) world.getBlockAt(cx, gy + y, cz).setType(Material.OAK_LOG, false);
            for (int y = 2; y <= 6; y++) {
                world.getBlockAt(cx + 1, gy + y, cz).setType(Material.SPRUCE_LEAVES, false);
                world.getBlockAt(cx - 1, gy + y, cz).setType(Material.SPRUCE_LEAVES, false);
                world.getBlockAt(cx, gy + y, cz + 1).setType(Material.SPRUCE_LEAVES, false);
                world.getBlockAt(cx, gy + y, cz - 1).setType(Material.SPRUCE_LEAVES, false);
            }
            world.getBlockAt(cx, gy + 7, cz).setType(Material.SPRUCE_LEAVES, false);
        }
    }

    private void decorateAirshipHarbor(World world, int cy) {
        // Boardwalk wooden path from bridge (40, 48) to Pier & Observatory
        for (int x = 41; x <= 50; x++) {
            setPathBlock(world, x, 50, Material.SPRUCE_PLANKS);
            setPathBlock(world, x, 51, Material.SPRUCE_PLANKS);
        }
        for (int z = 52; z <= 64; z++) {
            setPathBlock(world, 50, z, Material.SPRUCE_PLANKS);
            setPathBlock(world, 51, z, Material.SPRUCE_PLANKS);
        }

        // 1. Heavy Timber Dockside Cargo Crane at (66, 62)
        int craneX = 66, craneZ = 62;
        int craneY = getGroundY(world, craneX, craneZ);
        for (int y = 1; y <= 6; y++) world.getBlockAt(craneX, craneY + y, craneZ).setType(Material.DARK_OAK_LOG, false);
        // Horizontal boom arm extending over the pier edge (towards x = 62)
        for (int bx = 62; bx <= 66; bx++) {
            world.getBlockAt(bx, craneY + 6, craneZ).setType(Material.DARK_OAK_LOG, false);
        }
        world.getBlockAt(62, craneY + 5, craneZ).setType(Material.IRON_CHAIN, false);
        world.getBlockAt(62, craneY + 4, craneZ).setType(Material.IRON_CHAIN, false);
        // Hanging cargo crate
        world.getBlockAt(62, craneY + 3, craneZ).setType(Material.CHEST, false);
        world.getBlockAt(62, craneY + 3, craneZ - 1).setType(Material.SPRUCE_TRAPDOOR, false);
        world.getBlockAt(62, craneY + 3, craneZ + 1).setType(Material.SPRUCE_TRAPDOOR, false);

        // 2. Cargo Storage Depot on the pier
        world.getBlockAt(54, cy + 1, 65).setType(Material.BARREL, false);
        world.getBlockAt(54, cy + 2, 65).setType(Material.BARREL, false);
        world.getBlockAt(55, cy + 1, 65).setType(Material.CHEST, false);
        world.getBlockAt(56, cy + 1, 65).setType(Material.BARREL, false);
        world.getBlockAt(56, cy + 1, 66).setType(Material.ANVIL, false); // anchor

        // 3. Stargazer Terrace outside Observatory (43, 56)
        int tX = 43, tZ = 56;
        int tY = getGroundY(world, tX, tZ);
        world.getBlockAt(tX, tY + 1, tZ).setType(Material.LECTERN, false);
        world.getBlockAt(tX, tY + 1, tZ - 1).setType(Material.CARTOGRAPHY_TABLE, false);
        world.getBlockAt(tX, tY + 1, tZ + 1).setType(Material.BOOKSHELF, false);
        world.getBlockAt(tX - 1, tY + 1, tZ).setType(Material.SPRUCE_FENCE, false);
        world.getBlockAt(tX - 1, tY + 2, tZ).setType(Material.LANTERN, false);
    }

    private void decorateToriiShrine(World world, int cy) {
        // Stepping stone path from bridge (-40, 48) to Torii (-55, 58) and Sumo (-55, 65)
        for (int step = 0; step <= 15; step++) {
            int px = -40 - step;
            int pz = 48 + step;
            if (px >= -55 && pz <= 65) {
                Material m = (step % 2 == 0) ? Material.POLISHED_ANDESITE : Material.SMOOTH_STONE;
                setPathBlock(world, px, pz, m);
            }
        }
        for (int pz = 58; pz <= 65; pz++) {
            setPathBlock(world, -55, pz, Material.POLISHED_ANDESITE);
        }

        // 1. Serene Sacred Koi Pond at (-46, 60)
        int pondX = -46, pondZ = 60;
        int pY = getGroundY(world, pondX, pondZ);
        // Sunken pond basin
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) + Math.abs(dz) <= 3) {
                    world.getBlockAt(pondX + dx, pY, pondZ + dz).setType(Material.WATER, false);
                    world.getBlockAt(pondX + dx, pY - 1, pondZ + dz).setType(Material.WATER, false);
                }
            }
        }
        // Glowing sea lantern at pond bottom
        world.getBlockAt(pondX, pY - 1, pondZ).setType(Material.SEA_LANTERN, false);
        // Lily pads and water plants
        world.getBlockAt(pondX - 1, pY + 1, pondZ).setType(Material.LILY_PAD, false);
        world.getBlockAt(pondX + 1, pY + 1, pondZ + 1).setType(Material.LILY_PAD, false);
        world.getBlockAt(pondX, pY, pondZ - 1).setType(Material.SEAGRASS, false);
        // Surrounding mossy stones
        world.getBlockAt(pondX - 2, pY + 1, pondZ - 1).setType(Material.MOSSY_COBBLESTONE_SLAB, false);
        world.getBlockAt(pondX + 2, pY + 1, pondZ + 1).setType(Material.MOSSY_COBBLESTONE_SLAB, false);

        // 2. Traditional Zen Rock Garden at (-63, 60)
        int zenX = -63, zenZ = 60;
        int zY = getGroundY(world, zenX, zenZ);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.getBlockAt(zenX + dx, zY, zenZ + dz).setType(Material.SAND, false);
            }
        }
        // Natural polished andesite boulders
        world.getBlockAt(zenX, zY + 1, zenZ).setType(Material.POLISHED_ANDESITE, false);
        world.getBlockAt(zenX - 1, zY + 1, zenZ + 1).setType(Material.SMOOTH_STONE, false);
        // Bamboo stalks
        world.getBlockAt(zenX + 1, zY + 1, zenZ - 1).setType(Material.BAMBOO, false);
        world.getBlockAt(zenX + 1, zY + 2, zenZ - 1).setType(Material.BAMBOO, false);
        world.getBlockAt(zenX - 1, zY + 1, zenZ - 1).setType(Material.MOSS_CARPET, false);

        // 3. Open-air Tea Pagoda / Gazebo at (-64, 68)
        int teaX = -64, teaZ = 68;
        int tY = getGroundY(world, teaX, teaZ);
        // 4 Bamboo fence pillars
        world.getBlockAt(teaX - 1, tY + 1, teaZ - 1).setType(Material.BAMBOO_FENCE, false);
        world.getBlockAt(teaX + 1, tY + 1, teaZ - 1).setType(Material.BAMBOO_FENCE, false);
        world.getBlockAt(teaX - 1, tY + 1, teaZ + 1).setType(Material.BAMBOO_FENCE, false);
        world.getBlockAt(teaX + 1, tY + 1, teaZ + 1).setType(Material.BAMBOO_FENCE, false);
        world.getBlockAt(teaX - 1, tY + 2, teaZ - 1).setType(Material.BAMBOO_FENCE, false);
        world.getBlockAt(teaX + 1, tY + 2, teaZ - 1).setType(Material.BAMBOO_FENCE, false);
        world.getBlockAt(teaX - 1, tY + 2, teaZ + 1).setType(Material.BAMBOO_FENCE, false);
        world.getBlockAt(teaX + 1, tY + 2, teaZ + 1).setType(Material.BAMBOO_FENCE, false);
        // Crimson slab roof
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.getBlockAt(teaX + dx, tY + 3, teaZ + dz).setType(Material.CRIMSON_SLAB, false);
            }
        }
        world.getBlockAt(teaX, tY + 4, teaZ).setType(Material.CRIMSON_SLAB, false);
        world.getBlockAt(teaX, tY + 1, teaZ).setType(Material.SPRUCE_SLAB, false); // tea table
        world.getBlockAt(teaX, tY + 2, teaZ).setType(Material.FLOWER_POT, false);

        // 4. Four Stone Toro Lanterns framing the Sumo Ring
        int[][] toroLocs = {{-51, 61}, {-59, 61}, {-51, 69}, {-59, 69}};
        for (int[] tl : toroLocs) {
            int tx = tl[0], tz = tl[1];
            int gy = getGroundY(world, tx, tz);
            world.getBlockAt(tx, gy + 1, tz).setType(Material.CHISELED_DEEPSLATE, false);
            world.getBlockAt(tx, gy + 2, tz).setType(Material.SOUL_LANTERN, false);
            world.getBlockAt(tx, gy + 3, tz).setType(Material.POLISHED_DEEPSLATE_SLAB, false);
        }

        // Pink petals under cherry trees
        int[][] cherryLocs = {{-60, 70}, {-48, 72}};
        for (int[] c : cherryLocs) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if ((dx + dz) % 2 == 0) setSurfaceBlock(world, c[0] + dx, c[1] + dz, Material.PINK_PETALS);
                }
            }
        }
    }

    private void decorateSkyWarsSanctuary(World world, int cy) {
        // Quartz and Purpur pathways from landing pad (52, 10) to Pavilion (72, 10)
        for (int x = 53; x <= 71; x++) {
            setPathBlock(world, x, 9, Material.QUARTZ_BRICKS);
            setPathBlock(world, x, 10, Material.SMOOTH_QUARTZ);
            setPathBlock(world, x, 11, Material.QUARTZ_BRICKS);
        }

        // Floating Amethyst Crystal Formations at (64, 18) and (64, 2)
        int[][] crystalLocs = {{64, 18}, {64, 2}};
        for (int[] cr : crystalLocs) {
            int cx = cr[0], cz = cr[1];
            int gy = getGroundY(world, cx, cz);
            // Floating 1 block above ground
            world.getBlockAt(cx, gy + 2, cz).setType(Material.AMETHYST_BLOCK, false);
            world.getBlockAt(cx, gy + 3, cz).setType(Material.BUDDING_AMETHYST, false);
            world.getBlockAt(cx + 1, gy + 2, cz).setType(Material.TINTED_GLASS, false);
            world.getBlockAt(cx - 1, gy + 2, cz).setType(Material.TINTED_GLASS, false);
            world.getBlockAt(cx, gy + 2, cz + 1).setType(Material.TINTED_GLASS, false);
            world.getBlockAt(cx, gy + 2, cz - 1).setType(Material.TINTED_GLASS, false);
            world.getBlockAt(cx, gy + 1, cz).setType(Material.SEA_LANTERN, false);
            world.getBlockAt(cx, gy + 4, cz).setType(Material.AMETHYST_CLUSTER, false);
        }

        // Celestial Arcane Library Nook at (78, 16)
        int libX = 78, libZ = 16;
        int lY = getGroundY(world, libX, libZ);
        world.getBlockAt(libX, lY + 1, libZ).setType(Material.CHISELED_QUARTZ_BLOCK, false);
        world.getBlockAt(libX, lY + 2, libZ).setType(Material.LECTERN, false);
        world.getBlockAt(libX + 1, lY + 1, libZ).setType(Material.BOOKSHELF, false);
        world.getBlockAt(libX + 1, lY + 2, libZ).setType(Material.BOOKSHELF, false);
        world.getBlockAt(libX - 1, lY + 1, libZ).setType(Material.BOOKSHELF, false);
        world.getBlockAt(libX, lY + 1, libZ + 1).setType(Material.QUARTZ_PILLAR, false);
        world.getBlockAt(libX, lY + 2, libZ + 1).setType(Material.END_ROD, false);

        // Stargazing Balcony over the void at (84, 10)
        int balX = 84, balZ = 10;
        int bY = getGroundY(world, balX, balZ);
        for (int dz = -2; dz <= 2; dz++) {
            world.getBlockAt(balX, bY + 1, balZ + dz).setType(Material.PURPUR_SLAB, false);
            world.getBlockAt(balX + 1, bY + 1, balZ + dz).setType(Material.PURPUR_PILLAR, false);
            world.getBlockAt(balX + 1, bY + 2, balZ + dz).setType(Material.END_ROD, false);
        }
    }

    private void decorateDuelsCitadel(World world, int cy) {
        // Paved tournament approach from bridge landing (0, 56) to Pavilion (0, 76)
        for (int z = 57; z <= 75; z++) {
            for (int x = -1; x <= 1; x++) {
                setPathBlock(world, x, z, Material.POLISHED_ANDESITE);
            }
        }

        // 1. Tournament Jousting Arena at (-8, 76) - 5x5 enclosed combat ring
        int ringX = -8, ringZ = 76;
        int rY = getGroundY(world, ringX, ringZ);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.getBlockAt(ringX + dx, rY, ringZ + dz).setType(Material.RED_SAND, false);
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    if (dx == 0 && dz == -2) continue; // entrance gate
                    world.getBlockAt(ringX + dx, rY + 1, ringZ + dz).setType(Material.DARK_OAK_FENCE, false);
                }
            }
        }

        // 2. Champions' 3-Tier Victory Podium at (8, 76)
        int podX = 8, podZ = 76;
        int pY = getGroundY(world, podX, podZ);
        // 1st Place (Gold, height 3)
        world.getBlockAt(podX, pY + 1, podZ).setType(Material.GOLD_BLOCK, false);
        world.getBlockAt(podX, pY + 2, podZ).setType(Material.GOLD_BLOCK, false);
        world.getBlockAt(podX, pY + 3, podZ).setType(Material.GOLD_BLOCK, false);
        // 2nd Place (Iron, height 2)
        world.getBlockAt(podX, pY + 1, podZ - 2).setType(Material.IRON_BLOCK, false);
        world.getBlockAt(podX, pY + 2, podZ - 2).setType(Material.IRON_BLOCK, false);
        // 3rd Place (Copper, height 1)
        world.getBlockAt(podX, pY + 1, podZ + 2).setType(Material.COPPER_BLOCK, false);

        // 3. Knight's Armory & Weapon Racks at (5, 80)
        int armX = 5, armZ = 80;
        int aY = getGroundY(world, armX, armZ);
        world.getBlockAt(armX, aY + 1, armZ).setType(Material.ANVIL, false);
        world.getBlockAt(armX + 1, aY + 1, armZ).setType(Material.GRINDSTONE, false);
        world.getBlockAt(armX, aY + 1, armZ + 1).setType(Material.SMITHING_TABLE, false);
        world.getBlockAt(armX - 1, aY + 1, armZ).setType(Material.BARREL, false);
    }

    private void decorateParkourRuins(World world, int cy) {
        // 1. Overgrown Ancient Stone Arch at (-50, -48)
        int archX = -50, archZ = -48;
        int aY = getGroundY(world, archX, archZ);
        world.getBlockAt(archX - 2, aY + 1, archZ).setType(Material.MOSSY_STONE_BRICKS, false);
        world.getBlockAt(archX - 2, aY + 2, archZ).setType(Material.MOSSY_STONE_BRICKS, false);
        world.getBlockAt(archX - 2, aY + 3, archZ).setType(Material.MOSSY_STONE_BRICK_WALL, false);
        world.getBlockAt(archX + 2, aY + 1, archZ).setType(Material.MOSSY_STONE_BRICKS, false);
        world.getBlockAt(archX + 2, aY + 2, archZ).setType(Material.MOSSY_STONE_BRICKS, false);
        world.getBlockAt(archX + 2, aY + 3, archZ).setType(Material.MOSSY_STONE_BRICK_WALL, false);
        for (int px = -2; px <= 2; px++) {
            world.getBlockAt(archX + px, aY + 4, archZ).setType(Material.MOSSY_STONE_BRICKS, false);
        }
        world.getBlockAt(archX, aY + 3, archZ).setType(Material.LANTERN, false);

        // 2. Parkour Stepping Pillars across the island meadow
        int[][] parkourPillars = {{-52, -58, 1}, {-54, -62, 2}, {-58, -64, 3}, {-62, -60, 2}, {-60, -56, 1}};
        for (int[] pp : parkourPillars) {
            int px = pp[0], pz = pp[1], ph = pp[2];
            int gy = getGroundY(world, px, pz);
            for (int y = 1; y <= ph; y++) {
                world.getBlockAt(px, gy + y, pz).setType(Material.CRACKED_STONE_BRICKS, false);
            }
            if (ph == 3) {
                world.getBlockAt(px, gy + ph + 1, pz).setType(Material.SLIME_BLOCK, false);
            } else if (ph == 2) {
                world.getBlockAt(px, gy + ph + 1, pz).setType(Material.HONEY_BLOCK, false);
            }
        }

        // 3. Rustic Windmill Wheat Field & Meadow around (-66, -62)
        int wX = -66, wZ = -62;
        int[][] wheatOffsets = {{-3, 3}, {-2, 4}, {-4, 4}, {-3, 5}, {-2, 5}};
        for (int[] wo : wheatOffsets) {
            int bx = wX + wo[0], bz = wZ + wo[1];
            int gy = getGroundY(world, bx, bz);
            world.getBlockAt(bx, gy, bz).setType(Material.FARMLAND, false);
            world.getBlockAt(bx, gy + 1, bz).setType(Material.WHEAT, false);
        }
        // Hay bales stack near windmill
        int gyW = getGroundY(world, wX - 3, wZ - 1);
        world.getBlockAt(wX - 3, gyW + 1, wZ - 1).setType(Material.HAY_BLOCK, false);
        world.getBlockAt(wX - 3, gyW + 1, wZ - 2).setType(Material.HAY_BLOCK, false);
        world.getBlockAt(wX - 3, gyW + 2, wZ - 1).setType(Material.HAY_BLOCK, false);
        world.getBlockAt(wX - 2, gyW + 1, wZ - 1).setType(Material.BARREL, false);

        // 4. Wildflower Meadow surrounding windmill
        int[][] meadowFlowers = {{-60, -66, 0}, {-64, -66, 1}, {-68, -58, 2}, {-70, -60, 3}};
        Material[] mFlowers = {Material.SUNFLOWER, Material.OXEYE_DAISY, Material.CORNFLOWER, Material.POPPY};
        for (int[] mf : meadowFlowers) {
            setSurfaceBlock(world, mf[0], mf[1], mFlowers[mf[2]]);
        }
    }


    // =========================================================================
    // 2. Global BedWars Maps (Min 3, Max 16)
    // =========================================================================

    private void setupGlobalBedWarsMaps(World defaultWorld) {
        String[] mapNames = {
                "Airshow", "Lighthouse", "Amazon", "Apollo", "Cascade",
                "Kingdom Castle", "Sandstone Citadel", "Cyber Spire", "Frozen Bastion", "Nether Fortress"
        };

        Material[][] bwPalettes = {
                // 0. Airshow: Aeronautical / Skyport
                {Material.SMOOTH_STONE, Material.WHITE_CONCRETE, Material.POLISHED_ANDESITE},
                // 1. Lighthouse: Marine / Ocean Coastal
                {Material.PRISMARINE_BRICKS, Material.DARK_PRISMARINE, Material.SMOOTH_SANDSTONE},
                // 2. Amazon: Jungle Temple / Rain Forest
                {Material.MOSS_BLOCK, Material.MUD_BRICKS, Material.CHISELED_STONE_BRICKS},
                // 3. Apollo: Lunar Observatory / Space Colony
                {Material.END_STONE_BRICKS, Material.PURPUR_BLOCK, Material.GRAY_CONCRETE},
                // 4. Cascade: Mountain Pine / Alpine Springs
                {Material.STONE_BRICKS, Material.SPRUCE_PLANKS, Material.COBBLESTONE},
                // 5. Kingdom Castle: Royal Medieval Keep
                {Material.STONE_BRICKS, Material.OAK_PLANKS, Material.CRACKED_STONE_BRICKS},
                // 6. Sandstone Citadel: Desert Palace / Bazaar
                {Material.CUT_SANDSTONE, Material.SMOOTH_SANDSTONE, Material.RED_SANDSTONE},
                // 7. Cyber Spire: Neon Grid / Cyberpunk
                {Material.POLISHED_BLACKSTONE_BRICKS, Material.CYAN_CONCRETE, Material.MAGENTA_CONCRETE},
                // 8. Frozen Bastion: Arctic Fortress / Glacier
                {Material.PACKED_ICE, Material.BLUE_ICE, Material.SNOW_BLOCK},
                // 9. Nether Fortress: Brimstone Citadel
                {Material.NETHER_BRICKS, Material.RED_NETHER_BRICKS, Material.POLISHED_BLACKSTONE}
        };

        for (int i = 0; i < mapNames.length; i++) {
            String name = mapNames[i];
            String id = "bw-" + (i + 1);
            World mapWorld = loadOrCreateWorld(name, defaultWorld);

            int cx = (mapWorld.equals(defaultWorld)) ? (i + 1) * 350 : 0;
            int cy = 100;
            int cz = (mapWorld.equals(defaultWorld)) ? 0 : 0;

            Location waitingLoc = new Location(mapWorld, cx + 0.5, cy + 55, cz + 0.5);

            Material[] pal = bwPalettes[i % bwPalettes.length];

            if (mapWorld.equals(defaultWorld)) {
                // Clear old arena blocks before building
                MapBuilder.fillBox(mapWorld, cx - 85, cy - 25, cz - 70, cx + 85, cy + 35, cz + 70, Material.AIR);

                // 1. Middle Island: Natural floating island with unique palette
                MapBuilder.buildFloatingIslandNatural(mapWorld, cx, cy, cz, 18, pal[0], pal[1], pal[2]);

                // Contested Elevated Emerald Altar in center (accessible via 4 stairs, elevated at Y + 3)
                MapBuilder.fillBox(mapWorld, cx - 3, cy + 1, cz - 3, cx + 3, cy + 2, cz + 3, pal[0]);
                MapBuilder.fillBox(mapWorld, cx - 2, cy + 3, cz - 2, cx + 2, cy + 3, cz + 2, pal[2]);
                // 4 Staircases leading up to the platform (North, South, East, West)
                for (int s = 1; s <= 3; s++) {
                    mapWorld.getBlockAt(cx, cy + s, cz - 4 + s).setType(Material.STONE_BRICK_STAIRS, false);
                    mapWorld.getBlockAt(cx, cy + s, cz + 4 - s).setType(Material.STONE_BRICK_STAIRS, false);
                    mapWorld.getBlockAt(cx - 4 + s, cy + s, cz).setType(Material.STONE_BRICK_STAIRS, false);
                    mapWorld.getBlockAt(cx + 4 - s, cy + s, cz).setType(Material.STONE_BRICK_STAIRS, false);
                }
                // Center Emerald generator on top
                mapWorld.getBlockAt(cx, cy + 3, cz).setType(Material.EMERALD_BLOCK, false);
                // 4 Corner pillars with lanterns
                int[][] corn = {{-2, -2}, {-2, 2}, {2, -2}, {2, 2}};
                for (int[] c : corn) {
                    mapWorld.getBlockAt(cx + c[0], cy + 4, cz + c[1]).setType(Material.STONE_BRICK_WALL, false);
                    mapWorld.getBlockAt(cx + c[0], cy + 5, cz + c[1]).setType(Material.LANTERN, false);
                }

                // 2. Red Island: (rx = cx - 48)
                int rx = cx - 48;
                MapBuilder.buildFloatingIslandNatural(mapWorld, rx, cy, cz, 15, pal[0], pal[1], pal[2]);

                // Open-air Bed placed directly on flat island ground (NO lanterns or pedestals so players can defend with blocks)
                MapBuilder.placeBed(mapWorld, rx - 7, cy + 1, cz, org.bukkit.block.BlockFace.EAST, Material.RED_BED);

                // Fortified Base Forge Structure (7x7) housing Spawners, Shopkeeper, and Upgrades NPC inside!
                MapBuilder.buildBedWarsBaseForge(mapWorld, rx + 2, cy, cz - 3, pal[0], pal[1], pal[2]);
                mapWorld.getBlockAt(rx + 5, cy, cz).setType(Material.IRON_BLOCK, false);
                mapWorld.getBlockAt(rx + 6, cy, cz).setType(Material.GOLD_BLOCK, false);

                // 3. Blue Island: (bx = cx + 48)
                int bx = cx + 48;
                MapBuilder.buildFloatingIslandNatural(mapWorld, bx, cy, cz, 15, pal[0], pal[1], pal[2]);

                // Open-air Bed placed directly on flat island ground (NO lanterns or pedestals so players can defend with blocks)
                MapBuilder.placeBed(mapWorld, bx + 7, cy + 1, cz, org.bukkit.block.BlockFace.EAST, Material.BLUE_BED);

                // Fortified Base Forge Structure (7x7) housing Spawners, Shopkeeper, and Upgrades NPC inside!
                MapBuilder.buildBedWarsBaseForge(mapWorld, bx - 8, cy, cz - 3, pal[0], pal[1], pal[2]);
                mapWorld.getBlockAt(bx - 5, cy, cz).setType(Material.IRON_BLOCK, false);
                mapWorld.getBlockAt(bx - 6, cy, cz).setType(Material.GOLD_BLOCK, false);

                // 4. Two Diamond Islands (North and South of Middle)
                MapBuilder.buildFloatingIslandNatural(mapWorld, cx, cy, cz - 40, 7, pal[0], pal[1], pal[2]);
                mapWorld.getBlockAt(cx, cy, cz - 40).setType(Material.DIAMOND_BLOCK, false);
                MapBuilder.buildPillar(mapWorld, cx - 2, cy + 1, cz - 40, 3, Material.STONE_BRICK_WALL);
                MapBuilder.buildPillar(mapWorld, cx + 2, cy + 1, cz - 40, 3, Material.STONE_BRICK_WALL);

                MapBuilder.buildFloatingIslandNatural(mapWorld, cx, cy, cz + 40, 7, pal[0], pal[1], pal[2]);
                mapWorld.getBlockAt(cx, cy, cz + 40).setType(Material.DIAMOND_BLOCK, false);
                MapBuilder.buildPillar(mapWorld, cx - 2, cy + 1, cz + 40, 3, Material.STONE_BRICK_WALL);
                MapBuilder.buildPillar(mapWorld, cx + 2, cy + 1, cz + 40, 3, Material.STONE_BRICK_WALL);
            }

            MapBuilder.buildWaitingLobby(mapWorld, waitingLoc, Material.STONE_BRICKS, Material.SMOOTH_STONE, Material.WHITE_STAINED_GLASS);

            BedWarsGame game = new BedWarsGame(plugin, id, "BedWars: " + name, 3, 16);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));

            int rx = cx - 48;
            int bx = cx + 48;

            BedWarsTeam red = game.getTeam("RED");
            if (red != null) {
                // Spawn inside base forge structure
                red.setSpawnLocation(new Location(mapWorld, rx + 5.5, cy + 1.2, cz + 0.5, -90f, 0f));
                red.setBedLocation(new Location(mapWorld, rx - 7, cy + 1, cz));
                red.setBedFacing(org.bukkit.block.BlockFace.EAST);
            }
            BedWarsTeam blue = game.getTeam("BLUE");
            if (blue != null) {
                // Spawn inside base forge structure
                blue.setSpawnLocation(new Location(mapWorld, bx - 4.5, cy + 1.2, cz + 0.5, 90f, 0f));
                blue.setBedLocation(new Location(mapWorld, bx + 7, cy + 1, cz));
                blue.setBedFacing(org.bukkit.block.BlockFace.EAST);
            }

            // Team Base Spawners (Inside base forge structure)
            game.addSpawner(new BedWarsSpawner(new Location(mapWorld, rx + 5.5, cy + 1.2, cz + 0.5), BedWarsSpawner.SpawnerType.IRON));
            game.addSpawner(new BedWarsSpawner(new Location(mapWorld, rx + 5.5, cy + 1.2, cz + 0.5), BedWarsSpawner.SpawnerType.GOLD));
            game.addSpawner(new BedWarsSpawner(new Location(mapWorld, bx - 4.5, cy + 1.2, cz + 0.5), BedWarsSpawner.SpawnerType.IRON));
            game.addSpawner(new BedWarsSpawner(new Location(mapWorld, bx - 4.5, cy + 1.2, cz + 0.5), BedWarsSpawner.SpawnerType.GOLD));

            // Middle Emerald Spawner (Elevated Altar platform, contested high ground)
            game.addSpawner(new BedWarsSpawner(new Location(mapWorld, cx + 0.5, cy + 4.2, cz + 0.5), BedWarsSpawner.SpawnerType.EMERALD));

            // Side Diamond Spawners
            game.addSpawner(new BedWarsSpawner(new Location(mapWorld, cx + 0.5, cy + 1.2, cz - 40 + 0.5), BedWarsSpawner.SpawnerType.DIAMOND));
            game.addSpawner(new BedWarsSpawner(new Location(mapWorld, cx + 0.5, cy + 1.2, cz + 40 + 0.5), BedWarsSpawner.SpawnerType.DIAMOND));

            plugin.getGameManager().registerArena(game);
        }
    }

    // =========================================================================
    // 3. Global SkyWars Maps (Min 3, Max 12)
    // =========================================================================

    private void setupGlobalSkyWarsMaps(World defaultWorld) {
        String[] names = {
                "Aegis Citadel", "Elven Canopy", "Dragon Roost", "Crystal Hollow", "Steampunk Air",
                "Celestial Peak", "Frostbite Crag", "Volcanic Ridge", "Oasis Haven", "Nether Siphon"
        };

        Material[][] swPalettes = {
                // Aegis Citadel (Medieval Castle)
                {Material.STONE_BRICKS, Material.MOSSY_STONE_BRICKS, Material.SMOOTH_STONE, Material.DARK_OAK_STAIRS},
                // Elven Canopy (Enchanted Forest)
                {Material.MOSS_BLOCK, Material.OAK_LOG, Material.MOSS_CARPET, Material.OAK_STAIRS},
                // Dragon Roost (Dark Dragon Crag)
                {Material.POLISHED_BLACKSTONE_BRICKS, Material.BLACKSTONE, Material.CRYING_OBSIDIAN, Material.NETHER_BRICK_STAIRS},
                // Crystal Hollow (Amethyst & Geode)
                {Material.AMETHYST_BLOCK, Material.CALCITE, Material.SMOOTH_BASALT, Material.PURPUR_STAIRS},
                // Steampunk Air (Industrial Foundry)
                {Material.COPPER_BLOCK, Material.CUT_COPPER, Material.DARK_OAK_PLANKS, Material.DARK_OAK_STAIRS},
                // Celestial Peak (Cloud Temple)
                {Material.SMOOTH_QUARTZ, Material.QUARTZ_PILLAR, Material.GOLD_BLOCK, Material.SMOOTH_QUARTZ_STAIRS},
                // Frostbite Crag (Glacial Peaks)
                {Material.PACKED_ICE, Material.BLUE_ICE, Material.SNOW_BLOCK, Material.DIORITE_STAIRS},
                // Volcanic Ridge (Lava Caldera)
                {Material.BASALT, Material.NETHERRACK, Material.RED_NETHER_BRICKS, Material.POLISHED_BLACKSTONE_BRICK_STAIRS},
                // Oasis Haven (Desert Sanctuary)
                {Material.SMOOTH_SANDSTONE, Material.CUT_SANDSTONE, Material.TERRACOTTA, Material.SANDSTONE_STAIRS},
                // Nether Siphon (Warped Realm)
                {Material.WARPED_NYLIUM, Material.WARPED_STEM, Material.WARPED_WART_BLOCK, Material.WARPED_STAIRS}
        };

        Material[] cageGlasses = {
                Material.YELLOW_STAINED_GLASS, Material.LIME_STAINED_GLASS, Material.RED_STAINED_GLASS,
                Material.PURPLE_STAINED_GLASS, Material.ORANGE_STAINED_GLASS, Material.WHITE_STAINED_GLASS,
                Material.LIGHT_BLUE_STAINED_GLASS, Material.RED_STAINED_GLASS, Material.YELLOW_STAINED_GLASS,
                Material.CYAN_STAINED_GLASS
        };

        for (int i = 0; i < names.length; i++) {
            int cx = (i + 1) * 350;
            int cy = 100;
            int cz = 350;
            String id = "sw-" + (i + 1);

            // Clear old blocks before building
            MapBuilder.fillBox(defaultWorld, cx - 75, cy - 25, cz - 75, cx + 75, cy + 45, cz + 75, Material.AIR);

            Location waitingLoc = new Location(defaultWorld, cx + 0.5, cy + 55, cz + 0.5);
            MapBuilder.buildWaitingLobby(defaultWorld, waitingLoc, Material.POLISHED_ANDESITE, Material.STONE_BRICKS, cageGlasses[i]);

            SkyWarsGame game = new SkyWarsGame(plugin, id, "SkyWars: " + names[i], 2, 12, cy - 40);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));

            Material[] pal = swPalettes[i];

            // Middle Island
            MapBuilder.buildFloatingIslandNatural(defaultWorld, cx, cy, cz, 16, pal[0], pal[1], pal[2]);
            MapBuilder.buildCastleTower(defaultWorld, cx, cy + 1, cz, 4, 14, pal[0], pal[1], pal[3]);

            int[][] midChests = {{3, 3}, {-3, 3}, {3, -3}, {-3, -3}};
            for (int[] mc : midChests) {
                Location cLoc = new Location(defaultWorld, cx + mc[0], cy + 2, cz + mc[1]);
                cLoc.getBlock().setType(Material.CHEST, false);
                game.addMidChest(cLoc);
            }

            double rad = 42.0;
            for (int p = 0; p < 8; p++) {
                double angle = p * (Math.PI / 4.0);
                int ix = (int) Math.round(cx + rad * Math.cos(angle));
                int iz = (int) Math.round(cz + rad * Math.sin(angle));

                MapBuilder.buildFloatingIslandNatural(defaultWorld, ix, cy, iz, 7, pal[0], pal[1], pal[2]);
                MapBuilder.buildMedievalHouse(defaultWorld, ix - 3, cy + 1, iz - 3, 6, 6, pal[1], pal[0], pal[3], Material.STONE_BRICK_SLAB);

                Location spawnLoc = new Location(defaultWorld, ix + 0.5, cy + 2, iz + 0.5);
                game.addIslandSpawn(spawnLoc);
                game.spawnCage(spawnLoc, cageGlasses[i]);

                Location ic1 = new Location(defaultWorld, ix - 2, cy + 2, iz + 1);
                Location ic2 = new Location(defaultWorld, ix + 2, cy + 2, iz + 1);
                ic1.getBlock().setType(Material.CHEST, false);
                ic2.getBlock().setType(Material.CHEST, false);
                game.addIslandChest(ic1);
                game.addIslandChest(ic2);
            }

            plugin.getGameManager().registerArena(game);
        }
    }

    // =========================================================================
    // 4. Global TNT Run Maps (Min 3, Max 16)
    // =========================================================================

    private void setupGlobalTNTRunMaps(World defaultWorld) {
        String[] names = {
                "The Grand Colosseum", "Cyber Grid", "Frozen Abyss", "Lava Chamber", "Jungle Ruins",
                "Space Station", "The Factory", "Cotton Fall", "Sahara Storm", "Void Shrine"
        };

        Material[] topMats = {
                Material.SAND, Material.CYAN_CONCRETE, Material.SNOW_BLOCK, Material.RED_SAND, Material.GRAVEL,
                Material.PURPLE_CONCRETE, Material.MUD, Material.PINK_CONCRETE, Material.SAND, Material.WHITE_CONCRETE
        };

        for (int i = 0; i < names.length; i++) {
            int cx = 0;
            int cy = 100;
            int cz = (i + 1) * 350;
            String id = "tnt-" + (i + 1);

            Location waitingLoc = new Location(defaultWorld, cx + 0.5, cy + 55, cz + 0.5);
            MapBuilder.buildWaitingLobby(defaultWorld, waitingLoc, Material.POLISHED_DEEPSLATE, Material.QUARTZ_PILLAR, Material.ORANGE_STAINED_GLASS);

            int[] floorYs = {cy, cy - 9, cy - 18, cy - 27};
            int[] radii = {18, 17, 16, 15};

            for (int f = 0; f < 4; f++) {
                int fy = floorYs[f];
                int r = radii[f];
                MapBuilder.fillCircle(defaultWorld, cx, fy, cz, r, topMats[i]);
                MapBuilder.fillCircle(defaultWorld, cx, fy - 1, cz, r, Material.TNT);
                // Vertical boundary wall at r + 1 so players cannot stand on any outer ledge
                for (int wy = 1; wy <= 3; wy++) {
                    MapBuilder.fillRing(defaultWorld, cx, fy + wy, cz, r + 1, r + 1, Material.TINTED_GLASS);
                }
            }

            int[] pd = {-20, 20};
            for (int px : pd) {
                for (int pz : pd) {
                    MapBuilder.buildCastleTower(defaultWorld, cx + px, cy - 30, cz + pz, 3, 45, Material.STONE_BRICKS, Material.SMOOTH_STONE, Material.DARK_OAK_STAIRS);
                }
            }

            TNTRunGame game = new TNTRunGame(plugin, id, "TNT Run: " + names[i], 2, 16, cy - 35);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));
            game.setArenaSpawn(new Location(defaultWorld, cx + 0.5, cy + 1, cz + 0.5));
            plugin.getGameManager().registerArena(game);
        }
    }

    // =========================================================================
    // 5. Global Parkour Maps (Min 2, Max 16)
    // =========================================================================

    private void setupGlobalParkourMaps(World defaultWorld) {
        String[] names = {
                "Castle Ramparts", "Jungle Canopy", "Neon Matrix", "Glacier Leap", "Nether Run",
                "Skyline Rooftops", "Desert Pillars", "Crystal Cavern", "Mushroom Hop", "Temple of Speed"
        };

        for (int i = 0; i < names.length; i++) {
            int cx = -(i + 1) * 350;
            int cy = 100;
            int cz = 350;
            String id = "pk-" + (i + 1);

            // Clear any old blocks/checkpoints from previous builds to prevent 2 maps in 1
            MapBuilder.fillBox(defaultWorld, cx - 40, cy - 15, cz - 10, cx + 40, cy + 30, cz + 180, Material.AIR);

            Location waitingLoc = new Location(defaultWorld, cx + 0.5, cy + 55, cz + 0.5);
            MapBuilder.buildWaitingLobby(defaultWorld, waitingLoc, Material.SMOOTH_STONE, Material.OAK_FENCE, Material.LIME_STAINED_GLASS);

            ParkourGame game = new ParkourGame(plugin, id, "Parkour: " + names[i], 2, 16, cy - 15);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));

            MapBuilder.fillCircle(defaultWorld, cx, cy, cz, 4, Material.POLISHED_ANDESITE);
            game.setStartLocation(new Location(defaultWorld, cx + 0.5, cy + 1, cz + 0.5, 0f, 0f));

            Material[][] mapPalettes = {
                    {Material.STONE_BRICKS, Material.MOSSY_STONE_BRICKS, Material.COBBLESTONE},
                    {Material.JUNGLE_LOG, Material.JUNGLE_PLANKS, Material.MOSS_BLOCK},
                    {Material.CYAN_CONCRETE, Material.MAGENTA_CONCRETE, Material.SEA_LANTERN},
                    {Material.PACKED_ICE, Material.BLUE_ICE, Material.SNOW_BLOCK},
                    {Material.NETHER_BRICKS, Material.CRIMSON_STEM, Material.RED_NETHER_BRICKS},
                    {Material.BRICKS, Material.TERRACOTTA, Material.POLISHED_GRANITE},
                    {Material.SMOOTH_SANDSTONE, Material.CUT_SANDSTONE, Material.GOLD_BLOCK},
                    {Material.PRISMARINE, Material.DARK_PRISMARINE, Material.SEA_LANTERN},
                    {Material.RED_MUSHROOM_BLOCK, Material.BROWN_MUSHROOM_BLOCK, Material.MUSHROOM_STEM},
                    {Material.POLISHED_BLACKSTONE_BRICKS, Material.CRYING_OBSIDIAN, Material.GILDED_BLACKSTONE}
            };
            Material[] palette = mapPalettes[i % mapPalettes.length];

            int curX = cx;
            int curY = cy + 1;
            int curZ = cz + 3;

            java.util.Random rnd = new java.util.Random((i + 1) * 314159L);

            for (int cp = 1; cp <= 4; cp++) {
                int jumpCount = 4 + rnd.nextInt(2); // 4 to 5 clean jumps per checkpoint
                for (int j = 0; j < jumpCount; j++) {
                    int dy = rnd.nextInt(3) - 1; // -1, 0, or +1 elevation
                    if (curY + dy < cy || curY + dy > cy + 6) dy = 0;

                    int dz;
                    int dx;

                    if (dy == 1) {
                        // Upwards jump (+1 Y): strictly 2 blocks forward to guarantee 100% possible jumps
                        dz = 2;
                        dx = (rnd.nextInt(100) < 25) ? (rnd.nextBoolean() ? 1 : -1) : 0;
                    } else if (dy == 0) {
                        // Flat jump (0 Y): 2 or 3 blocks forward (no sharp diagonal 4-block jumps)
                        boolean far = rnd.nextBoolean();
                        dz = far ? 3 : 2;
                        dx = far ? 0 : (rnd.nextInt(3) - 1);
                    } else {
                        // Downward jump (-1 Y): 2 to 3 blocks forward
                        dz = 2 + rnd.nextInt(2);
                        dx = rnd.nextInt(3) - 1;
                    }

                    // Keep path comfortably centered around cx
                    if (curX + dx > cx + 3) dx = -1;
                    if (curX + dx < cx - 3) dx = 1;

                    curX += dx;
                    curY += dy;
                    curZ += dz;

                    Material jumpMat = palette[rnd.nextInt(palette.length)];
                    // Task 8: Greatly reduce boosted slime jumps (at most 1 safe bounce on checkpoint 2 of every 3rd course)
                    if (cp == 2 && j == 1 && i % 3 == 1) {
                        jumpMat = Material.SLIME_BLOCK;
                    }
                    defaultWorld.getBlockAt(curX, curY, curZ).setType(jumpMat, false);
                }

                // Checkpoint Island
                curZ += 3;
                curY = Math.max(cy + 1, curY);
                MapBuilder.fillCircle(defaultWorld, curX, curY, curZ, 2, palette[0]);
                defaultWorld.getBlockAt(curX, curY + 1, curZ).setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, false);
                game.addCheckpoint(new Location(defaultWorld, curX + 0.5, curY + 1, curZ + 0.5));
            }

            // Final Challenge stretch
            for (int j = 0; j < 4; j++) {
                int dx = rnd.nextInt(3) - 1;
                curX += dx;
                curY += (j % 2 == 0 ? 1 : 0);
                curZ += 3;
                defaultWorld.getBlockAt(curX, curY, curZ).setType(palette[rnd.nextInt(palette.length)], false);
            }
            curZ += 4;

            // Grand Finish Temple
            MapBuilder.fillCircle(defaultWorld, curX, curY, curZ, 3, Material.QUARTZ_BLOCK);
            MapBuilder.buildPillar(defaultWorld, curX - 2, curY + 1, curZ, 4, Material.QUARTZ_PILLAR);
            MapBuilder.buildPillar(defaultWorld, curX + 2, curY + 1, curZ, 4, Material.QUARTZ_PILLAR);
            MapBuilder.fillBox(defaultWorld, curX - 2, curY + 5, curZ, curX + 2, curY + 5, curZ, Material.GOLD_BLOCK);
            defaultWorld.getBlockAt(curX, curY + 1, curZ).setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, false);
            game.setFinishLocation(new Location(defaultWorld, curX + 0.5, curY + 1, curZ + 0.5));

            plugin.getGameManager().registerArena(game);
        }
    }

    // =========================================================================
    // 6. Dedicated 1v1 Duel Arenas (minPlayers = 2, maxPlayers = 2)
    // =========================================================================

    private void setupAllDuelArenas(World defaultWorld) {
        // A. 1v1 SkyWars Duels (3 Arenas)
        for (int i = 1; i <= 3; i++) {
            int cx = -i * 250;
            int cy = 100;
            int cz = -350;
            String id = "duel-skywars-" + i;

            // Clear old blocks before building
            MapBuilder.fillBox(defaultWorld, cx - 30, cy - 20, cz - 15, cx + 30, cy + 25, cz + 15, Material.AIR);

            Location waitingLoc = new Location(defaultWorld, cx + 0.5, cy + 55, cz + 0.5);
            MapBuilder.buildWaitingLobby(defaultWorld, waitingLoc, Material.POLISHED_ANDESITE, Material.STONE_BRICKS, Material.YELLOW_STAINED_GLASS);

            SkyWarsGame game = new SkyWarsGame(plugin, id, "SkyWars 1v1 Arena #" + i, 2, 2, cy - 25);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));

            // Island 1 & Island 2
            MapBuilder.buildFloatingIslandNatural(defaultWorld, cx - 18, cy, cz, 6, Material.GRASS_BLOCK, Material.DIRT, Material.DEEPSLATE);
            MapBuilder.buildFloatingIslandNatural(defaultWorld, cx + 18, cy, cz, 6, Material.GRASS_BLOCK, Material.DIRT, Material.DEEPSLATE);
            MapBuilder.buildFloatingIslandNatural(defaultWorld, cx, cy, cz, 5, Material.STONE_BRICKS, Material.SMOOTH_STONE, Material.BEDROCK);

            Location sp1 = new Location(defaultWorld, cx - 18, cy + 1, cz);
            Location sp2 = new Location(defaultWorld, cx + 18, cy + 1, cz);
            game.addIslandSpawn(sp1);
            game.addIslandSpawn(sp2);
            game.spawnCage(sp1, Material.YELLOW_STAINED_GLASS);
            game.spawnCage(sp2, Material.YELLOW_STAINED_GLASS);

            Location c1 = new Location(defaultWorld, cx - 17, cy + 1, cz + 1);
            Location c2 = new Location(defaultWorld, cx + 17, cy + 1, cz + 1);
            Location mid = new Location(defaultWorld, cx, cy + 1, cz);
            c1.getBlock().setType(Material.CHEST, false);
            c2.getBlock().setType(Material.CHEST, false);
            mid.getBlock().setType(Material.CHEST, false);
            game.addIslandChest(c1);
            game.addIslandChest(c2);
            game.addMidChest(mid);

            plugin.getGameManager().registerArena(game);
        }

        // B. 1v1 BedWars Duels (3 Arenas)
        for (int i = 1; i <= 3; i++) {
            int cx = -i * 250;
            int cy = 100;
            int cz = -650;
            String id = "duel-bedwars-" + i;

            // Clear old blocks before building
            MapBuilder.fillBox(defaultWorld, cx - 60, cy - 20, cz - 45, cx + 60, cy + 30, cz + 45, Material.AIR);

            Location waitingLoc = new Location(defaultWorld, cx + 0.5, cy + 55, cz + 0.5);
            MapBuilder.buildWaitingLobby(defaultWorld, waitingLoc, Material.STONE_BRICKS, Material.SMOOTH_STONE, Material.RED_STAINED_GLASS);

            BedWarsGame game = new BedWarsGame(plugin, id, "BedWars 1v1 Arena #" + i, 2, 2);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));

            // Red & Blue 1v1 islands: Natural floating islands
            int rx = cx - 32;
            int bx = cx + 32;
            MapBuilder.buildFloatingIslandNatural(defaultWorld, rx, cy, cz, 12, Material.GRASS_BLOCK, Material.DIRT, Material.STONE);
            MapBuilder.buildFloatingIslandNatural(defaultWorld, bx, cy, cz, 12, Material.GRASS_BLOCK, Material.DIRT, Material.STONE);
            MapBuilder.buildFloatingIslandNatural(defaultWorld, cx, cy, cz, 9, Material.GRASS_BLOCK, Material.DIRT, Material.STONE);

            // Open-air beds directly on island floor (NO lanterns, pedestals, or clutter so players can defend)
            MapBuilder.placeBed(defaultWorld, rx - 6, cy + 1, cz, org.bukkit.block.BlockFace.EAST, Material.RED_BED);
            MapBuilder.placeBed(defaultWorld, bx + 6, cy + 1, cz, org.bukkit.block.BlockFace.EAST, Material.BLUE_BED);

            // Fortified Base Forge Shelters (7x7) housing Spawners, Shopkeeper, Upgrades NPC
            MapBuilder.buildBedWarsBaseForge(defaultWorld, rx + 1, cy, cz - 3, Material.STONE_BRICKS, Material.DEEPSLATE_BRICKS, Material.RED_TERRACOTTA);
            defaultWorld.getBlockAt(rx + 4, cy, cz).setType(Material.IRON_BLOCK, false);
            defaultWorld.getBlockAt(rx + 5, cy, cz).setType(Material.GOLD_BLOCK, false);

            MapBuilder.buildBedWarsBaseForge(defaultWorld, bx - 7, cy, cz - 3, Material.STONE_BRICKS, Material.DEEPSLATE_BRICKS, Material.BLUE_TERRACOTTA);
            defaultWorld.getBlockAt(bx - 4, cy, cz).setType(Material.IRON_BLOCK, false);
            defaultWorld.getBlockAt(bx - 5, cy, cz).setType(Material.GOLD_BLOCK, false);

            // Contested Elevated Emerald Altar at Middle
            MapBuilder.fillBox(defaultWorld, cx - 2, cy + 1, cz - 2, cx + 2, cy + 2, cz + 2, Material.STONE_BRICKS);
            for (int s = 1; s <= 2; s++) {
                defaultWorld.getBlockAt(cx, cy + s, cz - 3 + s).setType(Material.STONE_BRICK_STAIRS, false);
                defaultWorld.getBlockAt(cx, cy + s, cz + 3 - s).setType(Material.STONE_BRICK_STAIRS, false);
                defaultWorld.getBlockAt(cx - 3 + s, cy + s, cz).setType(Material.STONE_BRICK_STAIRS, false);
                defaultWorld.getBlockAt(cx + 3 - s, cy + s, cz).setType(Material.STONE_BRICK_STAIRS, false);
            }
            defaultWorld.getBlockAt(cx, cy + 2, cz).setType(Material.EMERALD_BLOCK, false);

            // Two Diamond Islands (North and South of Mid)
            MapBuilder.buildFloatingIslandNatural(defaultWorld, cx, cy, cz - 25, 5, Material.GRASS_BLOCK, Material.DIRT, Material.STONE);
            worldAt(defaultWorld, cx, cy, cz - 25, Material.DIAMOND_BLOCK);

            MapBuilder.buildFloatingIslandNatural(defaultWorld, cx, cy, cz + 25, 5, Material.GRASS_BLOCK, Material.DIRT, Material.STONE);
            worldAt(defaultWorld, cx, cy, cz + 25, Material.DIAMOND_BLOCK);

            BedWarsTeam red = game.getTeam("RED");
            if (red != null) {
                red.setSpawnLocation(new Location(defaultWorld, rx + 4.5, cy + 1.2, cz + 0.5, -90f, 0f));
                red.setBedLocation(new Location(defaultWorld, rx - 6, cy + 1, cz));
                red.setBedFacing(org.bukkit.block.BlockFace.EAST);
            }
            BedWarsTeam blue = game.getTeam("BLUE");
            if (blue != null) {
                blue.setSpawnLocation(new Location(defaultWorld, bx - 3.5, cy + 1.2, cz + 0.5, 90f, 0f));
                blue.setBedLocation(new Location(defaultWorld, bx + 6, cy + 1, cz));
                blue.setBedFacing(org.bukkit.block.BlockFace.EAST);
            }

            // Base Spawners (Inside shelters)
            game.addSpawner(new BedWarsSpawner(new Location(defaultWorld, rx + 4.5, cy + 1.2, cz + 0.5), BedWarsSpawner.SpawnerType.IRON));
            game.addSpawner(new BedWarsSpawner(new Location(defaultWorld, rx + 4.5, cy + 1.2, cz + 0.5), BedWarsSpawner.SpawnerType.GOLD));
            game.addSpawner(new BedWarsSpawner(new Location(defaultWorld, bx - 3.5, cy + 1.2, cz + 0.5), BedWarsSpawner.SpawnerType.IRON));
            game.addSpawner(new BedWarsSpawner(new Location(defaultWorld, bx - 3.5, cy + 1.2, cz + 0.5), BedWarsSpawner.SpawnerType.GOLD));
            // Middle Emerald Spawner (Elevated Altar)
            game.addSpawner(new BedWarsSpawner(new Location(defaultWorld, cx + 0.5, cy + 3.2, cz + 0.5), BedWarsSpawner.SpawnerType.EMERALD));
            // Side Diamond Spawners
            game.addSpawner(new BedWarsSpawner(new Location(defaultWorld, cx + 0.5, cy + 1.2, cz - 25 + 0.5), BedWarsSpawner.SpawnerType.DIAMOND));
            game.addSpawner(new BedWarsSpawner(new Location(defaultWorld, cx + 0.5, cy + 1.2, cz + 25 + 0.5), BedWarsSpawner.SpawnerType.DIAMOND));

            plugin.getGameManager().registerArena(game);
        }

        // C. 1v1 Classic/Nodebuff PvP Duels (3 Arenas)
        for (int i = 1; i <= 3; i++) {
            int cx = -i * 250;
            int cy = 100;
            int cz = -950;
            String id = "duel-pvp-" + i;

            Location waitingLoc = new Location(defaultWorld, cx + 0.5, cy + 55, cz + 0.5);
            MapBuilder.buildWaitingLobby(defaultWorld, waitingLoc, Material.POLISHED_BLACKSTONE, Material.OBSIDIAN, Material.RED_STAINED_GLASS);

            PvPGame game = new PvPGame(plugin, id, "PvP 1v1 Arena #" + i);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));

            // Colosseum flat floor
            MapBuilder.fillCircle(defaultWorld, cx, cy, cz, 16, Material.SMOOTH_STONE);
            MapBuilder.fillRing(defaultWorld, cx, cy, cz, 15, 16, Material.STONE_BRICKS);
            for (int y = 1; y <= 4; y++) {
                MapBuilder.fillRing(defaultWorld, cx, cy + y, cz, 16, 17, Material.STONE_BRICKS);
            }

            game.setSpawn1(new Location(defaultWorld, cx - 10 + 0.5, cy + 1, cz + 0.5, -90f, 0f));
            game.setSpawn2(new Location(defaultWorld, cx + 10 + 0.5, cy + 1, cz + 0.5, 90f, 0f));

            plugin.getGameManager().registerArena(game);
        }

        // D. 1v1 Parkour Duels (3 Arenas)
        for (int i = 1; i <= 3; i++) {
            int cx = -i * 250;
            int cy = 100;
            int cz = -1250;
            String id = "duel-parkour-" + i;

            // Clear any old duel parkour blocks
            MapBuilder.fillBox(defaultWorld, cx - 30, cy - 15, cz - 10, cx + 30, cy + 30, cz + 130, Material.AIR);

            Location waitingLoc = new Location(defaultWorld, cx + 0.5, cy + 55, cz + 0.5);
            MapBuilder.buildWaitingLobby(defaultWorld, waitingLoc, Material.SMOOTH_STONE, Material.OAK_FENCE, Material.LIME_STAINED_GLASS);

            ParkourGame game = new ParkourGame(plugin, id, "Parkour 1v1 Arena #" + i, 2, 2, cy - 15);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));

            MapBuilder.fillCircle(defaultWorld, cx, cy, cz, 3, Material.POLISHED_ANDESITE);
            game.setStartLocation(new Location(defaultWorld, cx + 0.5, cy + 1, cz + 0.5, 0f, 0f));

            Material[][] duelPalettes = {
                    {Material.SPRUCE_PLANKS, Material.DARK_OAK_PLANKS, Material.STRIPPED_SPRUCE_LOG},
                    {Material.PRISMARINE_BRICKS, Material.DARK_PRISMARINE, Material.SEA_LANTERN},
                    {Material.WARPED_PLANKS, Material.WARPED_STEM, Material.WARPED_WART_BLOCK}
            };
            Material[] palette = duelPalettes[(i - 1) % duelPalettes.length];
            java.util.Random rnd = new java.util.Random(i * 99991L);

            int curX = cx;
            int curY = cy + 1;
            int curZ = cz + 3;
            for (int cp = 1; cp <= 4; cp++) {
                int jumpCount = 4;
                for (int j = 0; j < jumpCount; j++) {
                    int dy = rnd.nextInt(3) - 1; // -1, 0, +1
                    if (curY + dy < cy || curY + dy > cy + 5) dy = 0;

                    int dz;
                    int dx;
                    if (dy == 1) {
                        dz = 2;
                        dx = 0; // Upward jump strictly forward
                    } else if (dy == 0) {
                        boolean far = rnd.nextBoolean();
                        dz = far ? 3 : 2;
                        dx = far ? 0 : (rnd.nextInt(3) - 1);
                    } else {
                        dz = 2 + rnd.nextInt(2);
                        dx = rnd.nextInt(3) - 1;
                    }

                    if (curX + dx > cx + 3) dx = -1;
                    if (curX + dx < cx - 3) dx = 1;

                    curX += dx;
                    curY += dy;
                    curZ += dz;
                    defaultWorld.getBlockAt(curX, curY, curZ).setType(palette[rnd.nextInt(palette.length)], false);
                }
                curZ += 3;
                curY = Math.max(cy + 1, curY);
                MapBuilder.fillCircle(defaultWorld, curX, curY, curZ, 2, Material.STONE_BRICKS);
                defaultWorld.getBlockAt(curX, curY + 1, curZ).setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, false);
                game.addCheckpoint(new Location(defaultWorld, curX + 0.5, curY + 1, curZ + 0.5));
            }

            for (int j = 0; j < 3; j++) {
                curZ += 3;
                defaultWorld.getBlockAt(curX, curY, curZ).setType(Material.QUARTZ_BLOCK, false);
            }
            curZ += 4;

            MapBuilder.fillCircle(defaultWorld, curX, curY, curZ, 3, Material.QUARTZ_BLOCK);
            MapBuilder.buildPillar(defaultWorld, curX - 2, curY + 1, curZ, 4, Material.QUARTZ_PILLAR);
            MapBuilder.buildPillar(defaultWorld, curX + 2, curY + 1, curZ, 4, Material.QUARTZ_PILLAR);
            MapBuilder.fillBox(defaultWorld, curX - 2, curY + 5, curZ, curX + 2, curY + 5, curZ, Material.GOLD_BLOCK);
            defaultWorld.getBlockAt(curX, curY + 1, curZ).setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, false);
            game.setFinishLocation(new Location(defaultWorld, curX + 0.5, curY + 1, curZ + 0.5));

            plugin.getGameManager().registerArena(game);
        }

        // E. 1v1 TNT Run Duels (3 Arenas)
        for (int i = 1; i <= 3; i++) {
            int cx = -i * 250;
            int cy = 100;
            int cz = -1550;
            String id = "duel-tnt-" + i;

            Location waitingLoc = new Location(defaultWorld, cx + 0.5, cy + 55, cz + 0.5);
            MapBuilder.buildWaitingLobby(defaultWorld, waitingLoc, Material.POLISHED_DEEPSLATE, Material.QUARTZ_PILLAR, Material.ORANGE_STAINED_GLASS);

            int[] floorYs = {cy, cy - 8, cy - 16};
            for (int fy : floorYs) {
                MapBuilder.fillCircle(defaultWorld, cx, fy, cz, 12, Material.SAND);
                MapBuilder.fillCircle(defaultWorld, cx, fy - 1, cz, 12, Material.TNT);
                // Glass barrier walls at radius 13 so players cannot stand on any outer rim
                for (int wy = 1; wy <= 3; wy++) {
                    MapBuilder.fillRing(defaultWorld, cx, fy + wy, cz, 13, 13, Material.TINTED_GLASS);
                }
            }

            TNTRunGame game = new TNTRunGame(plugin, id, "TNT Run 1v1 Arena #" + i, 2, 2, cy - 25);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));
            game.setArenaSpawn(new Location(defaultWorld, cx + 0.5, cy + 1, cz + 0.5));
            plugin.getGameManager().registerArena(game);
        }

        // F. 1v1 Sumo Duels (3 Arenas)
        for (int i = 1; i <= 3; i++) {
            int cx = -i * 250;
            int cy = 100;
            int cz = -1850;
            String id = "duel-sumo-" + i;

            Location waitingLoc = new Location(defaultWorld, cx + 0.5, cy + 55, cz + 0.5);
            MapBuilder.buildWaitingLobby(defaultWorld, waitingLoc, Material.SMOOTH_STONE, Material.DEEPSLATE_BRICK_WALL, Material.YELLOW_STAINED_GLASS);

            MapBuilder.buildFloatingIslandNatural(defaultWorld, cx, cy, cz, 7, Material.RED_WOOL, Material.DEEPSLATE, Material.BEDROCK);
            MapBuilder.fillRing(defaultWorld, cx, cy, cz, 6, 7, Material.GOLD_BLOCK);

            SumoGame game = new SumoGame(plugin, id, "Sumo 1v1 Arena #" + i, cy - 8);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));
            game.setSpawn1(new Location(defaultWorld, cx - 4 + 0.5, cy + 1, cz + 0.5, -90f, 0f));
            game.setSpawn2(new Location(defaultWorld, cx + 4 + 0.5, cy + 1, cz + 0.5, 90f, 0f));
            plugin.getGameManager().registerArena(game);
        }

        // G. 1v1 The Bridge Duels (Aquatica, Atlantis, Fortress)
        String[] bridgeMaps = {"Aquatica", "Atlantis", "Fortress"};
        for (int i = 0; i < bridgeMaps.length; i++) {
            String bName = bridgeMaps[i];
            String id = "duel-bridge-" + (i + 1);
            World bWorld = loadOrCreateWorld(bName, defaultWorld);

            int cx = (bWorld.equals(defaultWorld)) ? -(i + 1) * 250 : 0;
            int cy = 100;
            int cz = (bWorld.equals(defaultWorld)) ? -2150 : 0;

            if (bWorld.equals(defaultWorld)) {
                // Clear old arena blocks
                MapBuilder.fillBox(bWorld, cx - 65, cy - 25, cz - 20, cx + 65, cy + 40, cz + 20, Material.AIR);
            }

            Location waitingLoc = new Location(bWorld, cx + 0.5, cy + 55, cz + 0.5);
            MapBuilder.buildWaitingLobby(bWorld, waitingLoc, Material.QUARTZ_BLOCK, Material.POLISHED_BLACKSTONE, Material.RED_STAINED_GLASS);

            BridgeGame game = new BridgeGame(plugin, id, "The Bridge 1v1: " + bName, cy - 15);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));

            // Bridge runs from rx = cx - 16 to bx = cx + 16 (33 blocks long, 1 block wide)
            int rx = cx - 16;
            int bx = cx + 16;

            if (bWorld.equals(defaultWorld)) {

                // --- 1. Red Base (Spans 26x15: rx - 25 to rx, cz - 7 to cz + 7) ---
                // Island floor
                MapBuilder.fillBox(bWorld, rx - 25, cy, cz - 7, rx, cy, cz + 7, Material.SMOOTH_STONE);
                // Outer side railings
                MapBuilder.fillBox(bWorld, rx - 25, cy + 1, cz - 7, rx - 1, cy + 2, cz - 7, Material.STONE_BRICK_WALL);
                MapBuilder.fillBox(bWorld, rx - 25, cy + 1, cz + 7, rx - 1, cy + 2, cz + 7, Material.STONE_BRICK_WALL);
                // Back castle rampart wall
                MapBuilder.fillBox(bWorld, rx - 25, cy + 1, cz - 7, rx - 25, cy + 7, cz + 7, Material.STONE_BRICKS);
                MapBuilder.fillBox(bWorld, rx - 25, cy + 3, cz - 4, rx - 25, cy + 5, cz + 4, Material.RED_TERRACOTTA);

                // Elevated Red Player Cage Platform (5x5 platform at cy + 3)
                MapBuilder.fillBox(bWorld, rx - 22, cy + 3, cz - 2, rx - 18, cy + 3, cz + 2, Material.STONE_BRICKS);
                MapBuilder.fillBox(bWorld, rx - 22, cy + 1, cz - 2, rx - 18, cy + 2, cz + 2, Material.STONE_BRICKS);
                // Rampart steps leading down from cage platform to courtyard
                for (int s = 1; s <= 2; s++) {
                    bWorld.getBlockAt(rx - 18 + s, cy + 3 - s, cz).setType(Material.STONE_BRICK_STAIRS, false);
                    bWorld.getBlockAt(rx - 18 + s, cy + 3 - s, cz - 1).setType(Material.STONE_BRICK_STAIRS, false);
                    bWorld.getBlockAt(rx - 18 + s, cy + 3 - s, cz + 1).setType(Material.STONE_BRICK_STAIRS, false);
                }

                // 3x3 Red Goal Pit at (rx - 6, cy, cz)
                MapBuilder.fillBox(bWorld, rx - 7, cy, cz - 1, rx - 5, cy, cz + 1, Material.AIR);
                MapBuilder.fillBox(bWorld, rx - 8, cy, cz - 2, rx - 4, cy, cz + 2, Material.RED_GLAZED_TERRACOTTA);
                MapBuilder.fillBox(bWorld, rx - 7, cy - 2, cz - 1, rx - 5, cy - 1, cz + 1, Material.WATER);
                MapBuilder.fillBox(bWorld, rx - 7, cy - 3, cz - 1, rx - 5, cy - 3, cz + 1, Material.SEA_LANTERN);

                // --- 2. Blue Base (Spans 26x15: bx to bx + 25, cz - 7 to cz + 7) ---
                // Island floor
                MapBuilder.fillBox(bWorld, bx, cy, cz - 7, bx + 25, cy, cz + 7, Material.SMOOTH_STONE);
                // Outer side railings
                MapBuilder.fillBox(bWorld, bx + 1, cy + 1, cz - 7, bx + 25, cy + 2, cz - 7, Material.STONE_BRICK_WALL);
                MapBuilder.fillBox(bWorld, bx + 1, cy + 1, cz + 7, bx + 25, cy + 2, cz + 7, Material.STONE_BRICK_WALL);
                // Back castle rampart wall
                MapBuilder.fillBox(bWorld, bx + 25, cy + 1, cz - 7, bx + 25, cy + 7, cz + 7, Material.STONE_BRICKS);
                MapBuilder.fillBox(bWorld, bx + 25, cy + 3, cz - 4, bx + 25, cy + 5, cz + 4, Material.BLUE_TERRACOTTA);

                // Elevated Blue Player Cage Platform (5x5 platform at cy + 3)
                MapBuilder.fillBox(bWorld, bx + 18, cy + 3, cz - 2, bx + 22, cy + 3, cz + 2, Material.STONE_BRICKS);
                MapBuilder.fillBox(bWorld, bx + 18, cy + 1, cz - 2, bx + 22, cy + 2, cz + 2, Material.STONE_BRICKS);
                // Rampart steps leading down from cage platform to courtyard
                for (int s = 1; s <= 2; s++) {
                    bWorld.getBlockAt(bx + 18 - s, cy + 3 - s, cz).setType(Material.STONE_BRICK_STAIRS, false);
                    bWorld.getBlockAt(bx + 18 - s, cy + 3 - s, cz - 1).setType(Material.STONE_BRICK_STAIRS, false);
                    bWorld.getBlockAt(bx + 18 - s, cy + 3 - s, cz + 1).setType(Material.STONE_BRICK_STAIRS, false);
                }

                // 3x3 Blue Goal Pit at (bx + 6, cy, cz)
                MapBuilder.fillBox(bWorld, bx + 5, cy, cz - 1, bx + 7, cy, cz + 1, Material.AIR);
                MapBuilder.fillBox(bWorld, bx + 4, cy, cz - 2, bx + 8, cy, cz + 2, Material.BLUE_GLAZED_TERRACOTTA);
                MapBuilder.fillBox(bWorld, bx + 5, cy - 2, cz - 1, bx + 7, cy - 1, cz + 1, Material.WATER);
                MapBuilder.fillBox(bWorld, bx + 5, cy - 3, cz - 1, bx + 7, cy - 3, cz + 1, Material.SEA_LANTERN);

                // --- 3. Exact 1-block wide bridge connecting Red to Blue base ---
                for (int x = rx; x <= bx; x++) {
                    bWorld.getBlockAt(x, cy, cz).setType(Material.SMOOTH_STONE, false);
                }
            }

            game.setRedGoal(new Location(bWorld, rx - 6.0, cy, cz + 0.5));
            game.setBlueGoal(new Location(bWorld, bx + 6.0, cy, cz + 0.5));
            game.setRedSpawn(new Location(bWorld, rx - 14.0, cy + 1, cz + 0.5, -90f, 0f));
            game.setBlueSpawn(new Location(bWorld, bx + 14.0, cy + 1, cz + 0.5, 90f, 0f));
            game.setRedCage(new Location(bWorld, rx - 20.0, cy + 4, cz + 0.5, -90f, 0f));
            game.setBlueCage(new Location(bWorld, bx + 20.0, cy + 4, cz + 0.5, 90f, 0f));

            plugin.getGameManager().registerArena(game);
        }
    }

    // =========================================================================
    // 7. Murder Mystery Maps (Min 3, Max 4) - Sprawling, Multi-Tier, Richly Furnished
    // =========================================================================

    private void setupMurderMysteryMaps(World arenaWorld) {
        // Map 1: Haunted Manor (Expanded 76x76 Grand Estate with Multi-tier Hallways, Graveyard & Balconies)
        {
            int cx = 2500;
            int cy = 100;
            int cz = 3500;
            String id = "mm-1";

            // Clear previous map area
            MapBuilder.fillBox(arenaWorld, cx - 42, cy - 12, cz - 42, cx + 42, cy + 30, cz + 42, Material.AIR);

            // 1. Estate Grounds Base (76x76)
            MapBuilder.fillBox(arenaWorld, cx - 36, cy, cz - 36, cx + 36, cy, cz + 36, Material.DARK_OAK_PLANKS);
            MapBuilder.fillBox(arenaWorld, cx - 36, cy - 1, cz - 36, cx + 36, cy - 4, cz + 36, Material.DEEPSLATE_BRICKS);

            // Perimeter Wrought Iron Fence & Corner Guard Pillars
            for (int x = cx - 36; x <= cx + 36; x++) {
                worldAt(arenaWorld, x, cy + 1, cz - 36, Material.STONE_BRICK_WALL);
                worldAt(arenaWorld, x, cy + 2, cz - 36, Material.IRON_BARS);
                worldAt(arenaWorld, x, cy + 1, cz + 36, Material.STONE_BRICK_WALL);
                worldAt(arenaWorld, x, cy + 2, cz + 36, Material.IRON_BARS);
            }
            for (int z = cz - 36; z <= cz + 36; z++) {
                worldAt(arenaWorld, cx - 36, cy + 1, z, Material.STONE_BRICK_WALL);
                worldAt(arenaWorld, cx - 36, cy + 2, z, Material.IRON_BARS);
                worldAt(arenaWorld, cx + 36, cy + 1, z, Material.STONE_BRICK_WALL);
                worldAt(arenaWorld, cx + 36, cy + 2, z, Material.IRON_BARS);
            }
            int[][] cornerTowers = {{-36, -36}, {36, -36}, {-36, 36}, {36, 36}};
            for (int[] ct : cornerTowers) {
                MapBuilder.fillBox(arenaWorld, cx + ct[0] - 1, cy + 1, cz + ct[1] - 1, cx + ct[0] + 1, cy + 6, cz + ct[1] + 1, Material.STONE_BRICKS);
                worldAt(arenaWorld, cx + ct[0], cy + 7, cz + ct[1], Material.LANTERN);
            }

            // =====================================================================
            // 2. Outdoor Moonlit Graveyard & Crypts (North Grounds: cz - 35 to cz - 16)
            // =====================================================================
            MapBuilder.fillBox(arenaWorld, cx - 32, cy, cz - 34, cx + 32, cy, cz - 17, Material.COARSE_DIRT);
            // Stone Paths
            for (int z = cz - 34; z <= cz - 16; z++) {
                worldAt(arenaWorld, cx, cy, z, Material.MOSSY_COBBLESTONE);
                worldAt(arenaWorld, cx - 1, cy, z, Material.COBBLESTONE);
                worldAt(arenaWorld, cx + 1, cy, z, Material.COBBLESTONE);
            }
            // Sarcophagus Crypt in North-West
            MapBuilder.fillBox(arenaWorld, cx - 28, cy + 1, cz - 32, cx - 22, cy + 4, cz - 26, Material.STONE_BRICKS);
            MapBuilder.fillBox(arenaWorld, cx - 27, cy + 1, cz - 31, cx - 23, cy + 3, cz - 27, Material.AIR);
            worldAt(arenaWorld, cx - 25, cy + 1, cz - 29, Material.POLISHED_BASALT);
            worldAt(arenaWorld, cx - 25, cy + 2, cz - 29, Material.SOUL_TORCH);
            worldAt(arenaWorld, cx - 25, cy + 1, cz - 26, Material.AIR); // crypt entrance
            worldAt(arenaWorld, cx - 26, cy + 3, cz - 26, Material.SOUL_LANTERN);

            // Gravestones & Cobwebs scattered across the yard
            int[][] graves = {{-16, -30}, {-12, -26}, {-18, -22}, {12, -30}, {18, -26}, {14, -22}, {24, -28}, {22, -20}};
            for (int[] g : graves) {
                worldAt(arenaWorld, cx + g[0], cy + 1, cz + g[1], Material.CHISELED_STONE_BRICKS);
                worldAt(arenaWorld, cx + g[0], cy + 2, cz + g[1], Material.STONE_BRICK_WALL);
                worldAt(arenaWorld, cx + g[0] + 1, cy + 1, cz + g[1], Material.COBWEB);
            }
            // Dead twisted oak tree with hanging soul lanterns
            MapBuilder.buildPillar(arenaWorld, cx + 6, cy + 1, cz - 25, 6, Material.DARK_OAK_LOG);
            worldAt(arenaWorld, cx + 5, cy + 5, cz - 25, Material.DARK_OAK_WOOD);
            worldAt(arenaWorld, cx + 7, cy + 6, cz - 25, Material.DARK_OAK_WOOD);
            worldAt(arenaWorld, cx + 6, cy + 5, cz - 26, Material.DARK_OAK_WOOD);
            worldAt(arenaWorld, cx + 5, cy + 4, cz - 25, Material.IRON_BARS);
            worldAt(arenaWorld, cx + 5, cy + 3, cz - 25, Material.SOUL_LANTERN);

            // =====================================================================
            // 3. The Grand Manor Structure (x: cx - 30 to cx + 30, z: cz - 16 to cz + 34)
            // =====================================================================
            // Exterior Manor Walls (Height 14)
            MapBuilder.fillBox(arenaWorld, cx - 30, cy + 1, cz - 16, cx + 30, cy + 13, cz - 16, Material.POLISHED_DEEPSLATE);
            MapBuilder.fillBox(arenaWorld, cx - 30, cy + 1, cz + 34, cx + 30, cy + 13, cz + 34, Material.POLISHED_DEEPSLATE);
            MapBuilder.fillBox(arenaWorld, cx - 30, cy + 1, cz - 16, cx - 30, cy + 13, cz + 34, Material.POLISHED_DEEPSLATE);
            MapBuilder.fillBox(arenaWorld, cx + 30, cy + 1, cz - 16, cx + 30, cy + 13, cz + 34, Material.POLISHED_DEEPSLATE);

            // Roof with Dark Oak Planks & Slanted eaves
            MapBuilder.fillBox(arenaWorld, cx - 31, cy + 14, cz - 17, cx + 31, cy + 14, cz + 35, Material.DARK_OAK_PLANKS);

            // Windows with Tinted Glass
            for (int wx = cx - 24; wx <= cx + 24; wx += 8) {
                worldAt(arenaWorld, wx, cy + 3, cz - 16, Material.TINTED_GLASS);
                worldAt(arenaWorld, wx, cy + 4, cz - 16, Material.TINTED_GLASS);
                worldAt(arenaWorld, wx, cy + 9, cz - 16, Material.TINTED_GLASS);
                worldAt(arenaWorld, wx, cy + 10, cz - 16, Material.TINTED_GLASS);

                worldAt(arenaWorld, wx, cy + 3, cz + 34, Material.TINTED_GLASS);
                worldAt(arenaWorld, wx, cy + 4, cz + 34, Material.TINTED_GLASS);
                worldAt(arenaWorld, wx, cy + 9, cz + 34, Material.TINTED_GLASS);
                worldAt(arenaWorld, wx, cy + 10, cz + 34, Material.TINTED_GLASS);
            }

            // =====================================================================
            // 4. Ground Floor Rooms (cy + 1 to cy + 6)
            // =====================================================================

            // A. Central Grand Foyer & Twin Curved Staircases (x: cx - 10 to cx + 10, z: cz - 2 to cz + 16)
            // Checkerboard floor
            for (int x = cx - 9; x <= cx + 9; x++) {
                for (int z = cz - 1; z <= cz + 15; z++) {
                    Material fMat = ((x + z) % 2 == 0) ? Material.POLISHED_DEEPSLATE : Material.SMOOTH_QUARTZ;
                    worldAt(arenaWorld, x, cy, z, fMat);
                }
            }
            // 4-block wide Red Carpet runner down the middle
            MapBuilder.fillBox(arenaWorld, cx - 2, cy + 1, cz - 2, cx + 2, cy + 1, cz + 16, Material.RED_CARPET);

            // West Staircase to 2nd Floor (Stretching cx - 9 to cx - 5, cz + 2 to cz + 8)
            for (int s = 1; s <= 6; s++) {
                worldAt(arenaWorld, cx - 9, cy + s, cz + 1 + s, Material.DARK_OAK_STAIRS);
                worldAt(arenaWorld, cx - 8, cy + s, cz + 1 + s, Material.DARK_OAK_STAIRS);
            }
            // East Staircase to 2nd Floor (Stretching cx + 8 to cx + 9, cz + 2 to cz + 8)
            for (int s = 1; s <= 6; s++) {
                worldAt(arenaWorld, cx + 8, cy + s, cz + 1 + s, Material.DARK_OAK_STAIRS);
                worldAt(arenaWorld, cx + 9, cy + s, cz + 1 + s, Material.DARK_OAK_STAIRS);
            }

            // Grand Chandelier suspended from cy + 13
            MapBuilder.buildPillar(arenaWorld, cx, cy + 8, cz + 7, 5, Material.IRON_BARS);
            worldAt(arenaWorld, cx, cy + 7, cz + 7, Material.SEA_LANTERN);
            worldAt(arenaWorld, cx - 1, cy + 7, cz + 7, Material.SOUL_LANTERN);
            worldAt(arenaWorld, cx + 1, cy + 7, cz + 7, Material.SOUL_LANTERN);
            worldAt(arenaWorld, cx, cy + 7, cz + 6, Material.SOUL_LANTERN);
            worldAt(arenaWorld, cx, cy + 7, cz + 8, Material.SOUL_LANTERN);

            // Knights in Armor flanking Foyer
            worldAt(arenaWorld, cx - 4, cy + 1, cz + 2, Material.POLISHED_ANDESITE);
            worldAt(arenaWorld, cx + 4, cy + 1, cz + 2, Material.POLISHED_ANDESITE);
            worldAt(arenaWorld, cx - 4, cy + 2, cz + 2, Material.IRON_BLOCK);
            worldAt(arenaWorld, cx + 4, cy + 2, cz + 2, Material.IRON_BLOCK);

            // B. North Wing: Royal Ballroom & Grand Organ (x: cx - 18 to cx + 18, z: cz - 15 to cz - 3)
            // Polished Andesite dancing floor
            MapBuilder.fillBox(arenaWorld, cx - 18, cy, cz - 15, cx + 18, cy, cz - 3, Material.POLISHED_ANDESITE);
            MapBuilder.fillRing(arenaWorld, cx, cy, cz - 9, 4, 5, Material.SMOOTH_QUARTZ);
            MapBuilder.fillRing(arenaWorld, cx, cy, cz - 9, 2, 2, Material.GOLD_BLOCK);

            // Pipe Organ along North Wall
            MapBuilder.fillBox(arenaWorld, cx - 4, cy + 1, cz - 15, cx + 4, cy + 1, cz - 15, Material.DARK_OAK_WOOD);
            MapBuilder.fillBox(arenaWorld, cx - 4, cy + 2, cz - 15, cx + 4, cy + 2, cz - 15, Material.NOTE_BLOCK);
            MapBuilder.fillBox(arenaWorld, cx - 3, cy + 3, cz - 15, cx + 3, cy + 5, cz - 15, Material.QUARTZ_PILLAR);
            worldAt(arenaWorld, cx, cy + 6, cz - 15, Material.GOLD_BLOCK);
            worldAt(arenaWorld, cx - 1, cy + 1, cz - 14, Material.QUARTZ_STAIRS); // Organ bench

            // Double French Open Archways leading out to Graveyard
            MapBuilder.fillBox(arenaWorld, cx - 12, cy + 1, cz - 16, cx - 9, cy + 4, cz - 16, Material.AIR);
            MapBuilder.fillBox(arenaWorld, cx + 9, cy + 1, cz - 16, cx + 12, cy + 4, cz - 16, Material.AIR);

            // Wide Archway connecting Ballroom to Foyer
            MapBuilder.fillBox(arenaWorld, cx - 5, cy + 1, cz - 3, cx + 5, cy + 5, cz - 2, Material.AIR);

            // C. West Wing: Great Dining Hall & Fireplace (x: cx - 29 to cx - 11, z: cz - 10 to cz + 12)
            // Long Banquet Table (14 blocks long)
            MapBuilder.fillBox(arenaWorld, cx - 24, cy + 1, cz - 5, cx - 16, cy + 1, cz + 7, Material.AIR);
            MapBuilder.fillBox(arenaWorld, cx - 21, cy + 1, cz - 4, cx - 19, cy + 1, cz + 6, Material.SPRUCE_FENCE);
            MapBuilder.fillBox(arenaWorld, cx - 21, cy + 2, cz - 4, cx - 19, cy + 2, cz + 6, Material.OAK_PRESSURE_PLATE);
            // Dining Chairs
            for (int z = cz - 4; z <= cz + 6; z += 2) {
                worldAt(arenaWorld, cx - 22, cy + 1, z, Material.DARK_OAK_STAIRS);
                worldAt(arenaWorld, cx - 18, cy + 1, z, Material.DARK_OAK_STAIRS);
            }
            // Feast Platters
            worldAt(arenaWorld, cx - 20, cy + 3, cz - 3, Material.CAKE);
            worldAt(arenaWorld, cx - 20, cy + 3, cz, Material.LANTERN);
            worldAt(arenaWorld, cx - 20, cy + 3, cz + 3, Material.CAKE);
            worldAt(arenaWorld, cx - 20, cy + 3, cz + 5, Material.LANTERN);

            // Massive Roaring Fireplace on West Wall (cx - 30)
            MapBuilder.fillBox(arenaWorld, cx - 29, cy + 1, cz - 1, cx - 29, cy + 5, cz + 3, Material.BRICKS);
            MapBuilder.fillBox(arenaWorld, cx - 28, cy + 1, cz, cx - 28, cy + 3, cz + 2, Material.AIR);
            worldAt(arenaWorld, cx - 28, cy + 1, cz + 1, Material.CAMPFIRE);

            // Wide Archway between Dining Hall and Foyer
            MapBuilder.fillBox(arenaWorld, cx - 11, cy + 1, cz + 2, cx - 10, cy + 4, cz + 8, Material.AIR);
            // Wide Archway between Dining Hall and Ballroom
            MapBuilder.fillBox(arenaWorld, cx - 18, cy + 1, cz - 11, cx - 14, cy + 4, cz - 9, Material.AIR);

            // D. South-West: Manor Kitchen & Pantry (x: cx - 29 to cx - 11, z: cz + 14 to cz + 33)
            MapBuilder.fillBox(arenaWorld, cx - 29, cy + 1, cz + 14, cx - 25, cy + 2, cz + 14, Material.FURNACE);
            worldAt(arenaWorld, cx - 27, cy + 1, cz + 16, Material.SMOKER);
            worldAt(arenaWorld, cx - 26, cy + 1, cz + 16, Material.CAULDRON);
            // Pantry Crate Stacks
            worldAt(arenaWorld, cx - 28, cy + 1, cz + 25, Material.BARREL);
            worldAt(arenaWorld, cx - 28, cy + 2, cz + 25, Material.BARREL);
            worldAt(arenaWorld, cx - 27, cy + 1, cz + 25, Material.CHEST);
            worldAt(arenaWorld, cx - 28, cy + 1, cz + 26, Material.PUMPKIN);
            worldAt(arenaWorld, cx - 27, cy + 1, cz + 26, Material.MELON);

            // Wide Archway from Kitchen into Dining Hall
            MapBuilder.fillBox(arenaWorld, cx - 22, cy + 1, cz + 12, cx - 18, cy + 4, cz + 14, Material.AIR);
            // Wide Archway from Kitchen into Foyer
            MapBuilder.fillBox(arenaWorld, cx - 12, cy + 1, cz + 18, cx - 10, cy + 4, cz + 22, Material.AIR);

            // Spiral Staircase down to Catacombs & Wine Cellar (at cx - 22, cz + 30)
            MapBuilder.fillBox(arenaWorld, cx - 24, cy - 6, cz + 26, cx - 16, cy - 1, cz + 34, Material.AIR);
            MapBuilder.fillBox(arenaWorld, cx - 25, cy - 7, cz + 25, cx - 15, cy - 7, cz + 35, Material.MOSSY_COBBLESTONE);
            for (int s = 0; s <= 6; s++) {
                worldAt(arenaWorld, cx - 22, cy - s, cz + 28 + (s % 3), Material.STONE_BRICK_STAIRS);
            }
            // Catacombs features
            MapBuilder.fillBox(arenaWorld, cx - 19, cy - 6, cz + 28, cx - 17, cy - 4, cz + 32, Material.BARREL); // Wine casks
            MapBuilder.fillBox(arenaWorld, cx - 24, cy - 6, cz + 33, cx - 20, cy - 4, cz + 33, Material.IRON_BARS); // Dungeon cell
            worldAt(arenaWorld, cx - 22, cy - 6, cz + 31, Material.SOUL_LANTERN);

            // E. East Wing: 2-Story Grand Library (x: cx + 11 to cx + 29, z: cz - 10 to cz + 12)
            // Bookshelves towering from cy + 1 to cy + 11
            MapBuilder.fillBox(arenaWorld, cx + 14, cy + 1, cz - 9, cx + 29, cy + 11, cz - 9, Material.BOOKSHELF);
            MapBuilder.fillBox(arenaWorld, cx + 29, cy + 1, cz - 9, cx + 29, cy + 11, cz + 11, Material.BOOKSHELF);
            MapBuilder.fillBox(arenaWorld, cx + 18, cy + 1, cz - 4, cx + 25, cy + 1, cz - 4, Material.BOOKSHELF);
            MapBuilder.fillBox(arenaWorld, cx + 18, cy + 1, cz + 4, cx + 25, cy + 1, cz + 4, Material.BOOKSHELF);

            // Reading Desks & Enchanting Table Alcove
            worldAt(arenaWorld, cx + 23, cy + 1, cz, Material.ENCHANTING_TABLE);
            worldAt(arenaWorld, cx + 16, cy + 1, cz - 1, Material.LECTERN);
            worldAt(arenaWorld, cx + 16, cy + 1, cz + 1, Material.LECTERN);
            worldAt(arenaWorld, cx + 22, cy + 2, cz - 1, Material.COBWEB);
            worldAt(arenaWorld, cx + 24, cy + 2, cz + 1, Material.COBWEB);

            // Wide Archway between Library and Foyer
            MapBuilder.fillBox(arenaWorld, cx + 10, cy + 1, cz + 2, cx + 12, cy + 4, cz + 8, Material.AIR);
            // Wide Archway between Library and Ballroom
            MapBuilder.fillBox(arenaWorld, cx + 14, cy + 1, cz - 11, cx + 18, cy + 4, cz - 9, Material.AIR);

            // F. South-East: Botanical Conservatory & Glass Arboretum (x: cx + 11 to cx + 29, z: cz + 14 to cz + 33)
            // Mossy floor and central fountain
            MapBuilder.fillBox(arenaWorld, cx + 12, cy, cz + 15, cx + 28, cy, cz + 32, Material.MOSS_BLOCK);
            MapBuilder.fillCircle(arenaWorld, cx + 20, cy + 1, cz + 23, 3, Material.STONE_BRICKS);
            MapBuilder.fillCircle(arenaWorld, cx + 20, cy + 1, cz + 23, 2, Material.WATER);
            worldAt(arenaWorld, cx + 20, cy + 2, cz + 23, Material.SEA_LANTERN);

            // Lush Flora & Flowering Azaleas
            worldAt(arenaWorld, cx + 15, cy + 1, cz + 18, Material.FLOWERING_AZALEA);
            worldAt(arenaWorld, cx + 25, cy + 1, cz + 18, Material.FLOWERING_AZALEA);
            worldAt(arenaWorld, cx + 15, cy + 1, cz + 28, Material.FLOWERING_AZALEA);
            worldAt(arenaWorld, cx + 25, cy + 1, cz + 28, Material.FLOWERING_AZALEA);
            worldAt(arenaWorld, cx + 14, cy + 1, cz + 23, Material.SPORE_BLOSSOM);

            // Conservatory Glass Ceiling letting moonlight through
            MapBuilder.fillBox(arenaWorld, cx + 12, cy + 13, cz + 15, cx + 28, cy + 13, cz + 32, Material.GLASS);

            // Wide Archway from Conservatory into Foyer
            MapBuilder.fillBox(arenaWorld, cx + 10, cy + 1, cz + 18, cx + 12, cy + 4, cz + 22, Material.AIR);
            // Wide Archway from Conservatory into Library
            MapBuilder.fillBox(arenaWorld, cx + 18, cy + 1, cz + 12, cx + 22, cy + 4, cz + 14, Material.AIR);

            // =====================================================================
            // 5. Second Floor Gallery & Balconies (cy + 7 to cy + 13)
            // =====================================================================
            // Full 360-degree wraparound corridor overlooking Foyer!
            MapBuilder.fillBox(arenaWorld, cx - 11, cy + 6, cz - 3, cx + 11, cy + 6, cz + 17, Material.SPRUCE_PLANKS);
            MapBuilder.fillBox(arenaWorld, cx - 4, cy + 6, cz + 2, cx + 4, cy + 6, cz + 12, Material.AIR); // Center atrium opening

            // Iron Bar Balcony Railings around the central opening
            for (int x = cx - 4; x <= cx + 4; x++) {
                worldAt(arenaWorld, x, cy + 7, cz + 2, Material.IRON_BARS);
                worldAt(arenaWorld, x, cy + 7, cz + 12, Material.IRON_BARS);
            }
            for (int z = cz + 2; z <= cz + 12; z++) {
                worldAt(arenaWorld, cx - 4, cy + 7, z, Material.IRON_BARS);
                worldAt(arenaWorld, cx + 4, cy + 7, z, Material.IRON_BARS);
            }

            // 2nd Floor Corridors connecting all 4 Wings (4 blocks wide, open to sprint!)
            // North-West: Master Bedroom Suite
            MapBuilder.fillBox(arenaWorld, cx - 28, cy + 6, cz - 10, cx - 12, cy + 6, cz + 10, Material.SPRUCE_PLANKS);
            // 4-Poster King Bed
            worldAt(arenaWorld, cx - 26, cy + 7, cz, Material.RED_BED);
            MapBuilder.buildPillar(arenaWorld, cx - 27, cy + 7, cz - 1, 3, Material.SPRUCE_FENCE);
            MapBuilder.buildPillar(arenaWorld, cx - 25, cy + 7, cz - 1, 3, Material.SPRUCE_FENCE);
            MapBuilder.buildPillar(arenaWorld, cx - 27, cy + 7, cz + 1, 3, Material.SPRUCE_FENCE);
            MapBuilder.buildPillar(arenaWorld, cx - 25, cy + 7, cz + 1, 3, Material.SPRUCE_FENCE);
            worldAt(arenaWorld, cx - 26, cy + 7, cz + 5, Material.CHEST);
            worldAt(arenaWorld, cx - 26, cy + 7, cz - 5, Material.BOOKSHELF);

            // North-East: Upper Library Catwalk
            MapBuilder.fillBox(arenaWorld, cx + 12, cy + 6, cz - 10, cx + 28, cy + 6, cz + 10, Material.SPRUCE_PLANKS);
            MapBuilder.fillBox(arenaWorld, cx + 18, cy + 6, cz - 6, cx + 24, cy + 6, cz + 6, Material.AIR); // Library atrium drop
            for (int z = cz - 6; z <= cz + 6; z++) {
                worldAt(arenaWorld, cx + 18, cy + 7, z, Material.IRON_BARS);
                worldAt(arenaWorld, cx + 24, cy + 7, z, Material.IRON_BARS);
            }

            // South Gallery: Relics & Armory
            MapBuilder.fillBox(arenaWorld, cx - 14, cy + 6, cz + 18, cx + 14, cy + 6, cz + 30, Material.SPRUCE_PLANKS);
            worldAt(arenaWorld, cx - 6, cy + 7, cz + 25, Material.DIAMOND_BLOCK);
            worldAt(arenaWorld, cx - 6, cy + 8, cz + 25, Material.GLASS);
            worldAt(arenaWorld, cx + 6, cy + 7, cz + 25, Material.EMERALD_BLOCK);
            worldAt(arenaWorld, cx + 6, cy + 8, cz + 25, Material.GLASS);
            worldAt(arenaWorld, cx, cy + 7, cz + 28, Material.GOLD_BLOCK);
            worldAt(arenaWorld, cx, cy + 8, cz + 28, Material.GLASS);

            // =====================================================================
            // 6. Waiting Lobby high above (cy + 55)
            // =====================================================================
            Location waitingLoc = new Location(arenaWorld, cx + 0.5, cy + 55, cz + 0.5);
            MapBuilder.buildWaitingLobby(arenaWorld, waitingLoc, Material.POLISHED_DEEPSLATE, Material.DEEPSLATE_BRICK_WALL, Material.PURPLE_STAINED_GLASS);

            MurderMysteryGame game = new MurderMysteryGame(plugin, id, "Murder Mystery: Haunted Manor");
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));

            // 8 Scattered Player Spawns (Across all wings and floors!)
            game.addSpawnPoint(new Location(arenaWorld, cx + 0.5, cy + 1, cz + 4 + 0.5, 0f, 0f));      // Grand Foyer
            game.addSpawnPoint(new Location(arenaWorld, cx + 0.5, cy + 1, cz - 9 + 0.5, 180f, 0f));    // Ballroom
            game.addSpawnPoint(new Location(arenaWorld, cx - 20 + 0.5, cy + 1, cz + 1 + 0.5, -90f, 0f)); // Great Dining Hall
            game.addSpawnPoint(new Location(arenaWorld, cx + 20 + 0.5, cy + 1, cz + 1 + 0.5, 90f, 0f));  // Library Ground Floor
            game.addSpawnPoint(new Location(arenaWorld, cx + 20 + 0.5, cy + 1, cz + 23 + 0.5, 45f, 0f)); // Conservatory
            game.addSpawnPoint(new Location(arenaWorld, cx - 20 + 0.5, cy - 6, cz + 30 + 0.5, 0f, 0f)); // Catacombs Cellar
            game.addSpawnPoint(new Location(arenaWorld, cx + 0.5, cy + 1, cz - 26 + 0.5, 0f, 0f));     // Moonlit Graveyard
            game.addSpawnPoint(new Location(arenaWorld, cx - 20 + 0.5, cy + 7, cz + 1 + 0.5, -90f, 0f)); // 2nd Floor Master Suite

            // 24 Scattered Gold Spawns (Along running corridors, tables, balconies, graveyard)
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 0.5, cy + 1.2, cz + 1 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 0.5, cy + 1.2, cz + 12 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx - 7 + 0.5, cy + 1.2, cz + 5 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 7 + 0.5, cy + 1.2, cz + 5 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 0.5, cy + 1.2, cz - 5 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx - 12 + 0.5, cy + 1.2, cz - 9 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 12 + 0.5, cy + 1.2, cz - 9 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx - 20 + 0.5, cy + 3.2, cz + 1 + 0.5)); // Banquet table
            game.addGoldSpawnPoint(new Location(arenaWorld, cx - 25 + 0.5, cy + 1.2, cz - 5 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx - 25 + 0.5, cy + 1.2, cz + 8 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx - 20 + 0.5, cy + 1.2, cz + 20 + 0.5)); // Kitchen
            game.addGoldSpawnPoint(new Location(arenaWorld, cx - 20 + 0.5, cy - 5.8, cz + 28 + 0.5)); // Catacombs
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 20 + 0.5, cy + 1.2, cz - 5 + 0.5)); // Library
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 25 + 0.5, cy + 1.2, cz + 5 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 20 + 0.5, cy + 1.2, cz + 18 + 0.5)); // Conservatory
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 25 + 0.5, cy + 1.2, cz + 28 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 0.5, cy + 1.2, cz - 20 + 0.5)); // Graveyard Path
            game.addGoldSpawnPoint(new Location(arenaWorld, cx - 15 + 0.5, cy + 1.2, cz - 26 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 15 + 0.5, cy + 1.2, cz - 26 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx - 25 + 0.5, cy + 1.2, cz - 29 + 0.5)); // Sarcophagus
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 0.5, cy + 7.2, cz + 3 + 0.5)); // 2nd Floor Balcony
            game.addGoldSpawnPoint(new Location(arenaWorld, cx - 20 + 0.5, cy + 7.2, cz + 1 + 0.5)); // Master Suite
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 20 + 0.5, cy + 7.2, cz + 1 + 0.5)); // Upper Library
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 0.5, cy + 7.2, cz + 25 + 0.5)); // Armory Gallery

            plugin.getGameManager().registerArena(game);
        }

        // Map 2: Cyber Station (Expanded 70x70 Multi-Tier Space Station)
        {
            int cx = 2900;
            int cy = 100;
            int cz = 3500;
            String id = "mm-2";

            // Clear previous map area
            MapBuilder.fillBox(arenaWorld, cx - 40, cy - 12, cz - 40, cx + 40, cy + 30, cz + 40, Material.AIR);

            // 1. Station Foundation Floor (70x70)
            MapBuilder.fillBox(arenaWorld, cx - 35, cy, cz - 35, cx + 35, cy, cz + 35, Material.SMOOTH_QUARTZ);
            MapBuilder.fillBox(arenaWorld, cx - 35, cy - 1, cz - 35, cx + 35, cy - 4, cz + 35, Material.POLISHED_BLACKSTONE);

            // Exterior Hull Walls (Height 12)
            MapBuilder.fillBox(arenaWorld, cx - 35, cy + 1, cz - 35, cx + 35, cy + 12, cz - 35, Material.CYAN_CONCRETE);
            MapBuilder.fillBox(arenaWorld, cx - 35, cy + 1, cz + 35, cx + 35, cy + 12, cz + 35, Material.CYAN_CONCRETE);
            MapBuilder.fillBox(arenaWorld, cx - 35, cy + 1, cz - 35, cx - 35, cy + 12, cz + 35, Material.CYAN_CONCRETE);
            MapBuilder.fillBox(arenaWorld, cx + 35, cy + 1, cz - 35, cx + 35, cy + 12, cz + 35, Material.CYAN_CONCRETE);

            // Roof with Glass Skylights
            MapBuilder.fillBox(arenaWorld, cx - 35, cy + 13, cz - 35, cx + 35, cy + 13, cz + 35, Material.GRAY_CONCRETE);
            MapBuilder.fillCircle(arenaWorld, cx, cy + 13, cz, 12, Material.CYAN_STAINED_GLASS);

            // 2. Central Reactor Core (Radius 12, 3 Floors high)
            MapBuilder.fillCircle(arenaWorld, cx, cy, cz, 12, Material.POLISHED_BLACKSTONE);
            MapBuilder.fillRing(arenaWorld, cx, cy, cz, 10, 11, Material.SEA_LANTERN);
            worldAt(arenaWorld, cx, cy + 1, cz, Material.BEACON);
            worldAt(arenaWorld, cx, cy + 2, cz, Material.CYAN_STAINED_GLASS);

            // Suspended 2nd Floor Catwalk around the reactor at cy + 6
            MapBuilder.fillRing(arenaWorld, cx, cy + 6, cz, 8, 12, Material.SMOOTH_QUARTZ);
            MapBuilder.fillRing(arenaWorld, cx, cy + 7, cz, 8, 8, Material.IRON_BARS);
            // 4 Staircases leading up to 2nd Floor Catwalk
            for (int s = 1; s <= 6; s++) {
                worldAt(arenaWorld, cx + s, cy + s, cz + 13 - s, Material.QUARTZ_STAIRS);
                worldAt(arenaWorld, cx - s, cy + s, cz - 13 + s, Material.QUARTZ_STAIRS);
            }

            // 3. 4 Wide Running Corridors (4 blocks wide, perfectly open!)
            // North -> Command Bridge
            MapBuilder.fillBox(arenaWorld, cx - 2, cy + 1, cz - 34, cx + 2, cy + 6, cz - 12, Material.AIR);
            // South -> Crew Quarters & Mess Hall
            MapBuilder.fillBox(arenaWorld, cx - 2, cy + 1, cz + 12, cx + 2, cy + 6, cz + 34, Material.AIR);
            // West -> Research & Cryo Lab
            MapBuilder.fillBox(arenaWorld, cx - 34, cy + 1, cz - 2, cx - 12, cy + 6, cz + 2, Material.AIR);
            // East -> Hydroponics Arboretum
            MapBuilder.fillBox(arenaWorld, cx + 12, cy + 1, cz - 2, cx + 34, cy + 6, cz + 2, Material.AIR);

            // Neon Floor Strip Lighting in corridors
            for (int z = cz - 32; z <= cz + 32; z += 4) {
                worldAt(arenaWorld, cx, cy, z, Material.SEA_LANTERN);
            }
            for (int x = cx - 32; x <= cx + 32; x += 4) {
                worldAt(arenaWorld, x, cy, cz, Material.SEA_LANTERN);
            }

            // 4. Sector Alpha (North): Command Bridge & Navigation Deck (x: cx - 20 to cx + 20, z: cz - 34 to cz - 16)
            MapBuilder.fillBox(arenaWorld, cx - 16, cy + 1, cz - 34, cx + 16, cy + 6, cz - 34, Material.TINTED_GLASS); // Giant space observation viewport
            MapBuilder.fillBox(arenaWorld, cx - 6, cy + 1, cz - 26, cx + 6, cy + 1, cz - 24, Material.POLISHED_BLACKSTONE);
            worldAt(arenaWorld, cx, cy + 2, cz - 25, Material.BEACON);
            worldAt(arenaWorld, cx, cy + 3, cz - 25, Material.CYAN_STAINED_GLASS); // Hologram map
            worldAt(arenaWorld, cx - 2, cy + 1, cz - 28, Material.LEVER);
            worldAt(arenaWorld, cx + 2, cy + 1, cz - 28, Material.LEVER);

            // 5. Sector Beta (East): Hydroponics Biosphere (x: cx + 16 to cx + 34, z: cz - 16 to cz + 16)
            MapBuilder.fillBox(arenaWorld, cx + 18, cy, cz - 14, cx + 32, cy, cz + 14, Material.MOSS_BLOCK);
            MapBuilder.fillCircle(arenaWorld, cx + 25, cy + 1, cz, 4, Material.FLOWERING_AZALEA);
            worldAt(arenaWorld, cx + 25, cy + 1, cz, Material.MOSS_CARPET);
            worldAt(arenaWorld, cx + 25, cy + 2, cz, Material.SEA_LANTERN);
            worldAt(arenaWorld, cx + 20, cy + 1, cz - 8, Material.SPORE_BLOSSOM);
            worldAt(arenaWorld, cx + 30, cy + 1, cz + 8, Material.SPORE_BLOSSOM);

            // 6. Sector Gamma (West): Research & Cryo Labs (x: cx - 34 to cx - 16, z: cz - 16 to cz + 16)
            MapBuilder.fillBox(arenaWorld, cx - 32, cy + 1, cz - 12, cx - 20, cy + 1, cz - 12, Material.IRON_BLOCK);
            worldAt(arenaWorld, cx - 28, cy + 2, cz - 12, Material.BREWING_STAND);
            worldAt(arenaWorld, cx - 26, cy + 2, cz - 12, Material.CAULDRON);
            worldAt(arenaWorld, cx - 24, cy + 2, cz - 12, Material.ANVIL);
            // Cryo Pods
            for (int czp = cz - 4; czp <= cz + 8; czp += 4) {
                worldAt(arenaWorld, cx - 30, cy + 1, czp, Material.PACKED_ICE);
                worldAt(arenaWorld, cx - 30, cy + 2, czp, Material.CYAN_STAINED_GLASS);
                worldAt(arenaWorld, cx - 30, cy + 3, czp, Material.IRON_BLOCK);
            }

            // 7. Sector Delta (South): Mess Hall & Crew Quarters (x: cx - 20 to cx + 20, z: cz + 16 to cz + 34)
            // Long dining tables
            MapBuilder.fillBox(arenaWorld, cx - 10, cy + 1, cz + 24, cx - 2, cy + 1, cz + 24, Material.SMOOTH_QUARTZ);
            MapBuilder.fillBox(arenaWorld, cx + 2, cy + 1, cz + 24, cx + 10, cy + 1, cz + 24, Material.SMOOTH_QUARTZ);
            worldAt(arenaWorld, cx - 6, cy + 2, cz + 24, Material.CAKE);
            worldAt(arenaWorld, cx + 6, cy + 2, cz + 24, Material.CAKE);
            // Vending dispensers & Barrels
            worldAt(arenaWorld, cx - 16, cy + 1, cz + 30, Material.DISPENSER);
            worldAt(arenaWorld, cx - 15, cy + 1, cz + 30, Material.BARREL);
            worldAt(arenaWorld, cx + 15, cy + 1, cz + 30, Material.BARREL);

            // Sub-deck Maintenance Crawlways & Ladders at cy - 4 (Thick reinforced hull floor at cy - 5 to cy - 7)
            MapBuilder.fillBox(arenaWorld, cx - 6, cy - 7, cz - 28, cx + 6, cy - 5, cz + 28, Material.POLISHED_BLACKSTONE);
            // Side walls enclosing the crawlway
            MapBuilder.fillBox(arenaWorld, cx - 3, cy - 4, cz - 26, cx - 3, cy - 1, cz + 26, Material.POLISHED_BLACKSTONE);
            MapBuilder.fillBox(arenaWorld, cx + 3, cy - 4, cz - 26, cx + 3, cy - 1, cz + 26, Material.POLISHED_BLACKSTONE);
            MapBuilder.fillBox(arenaWorld, cx - 3, cy - 4, cz - 26, cx + 3, cy - 1, cz - 26, Material.POLISHED_BLACKSTONE);
            MapBuilder.fillBox(arenaWorld, cx - 3, cy - 4, cz + 26, cx + 3, cy - 1, cz + 26, Material.POLISHED_BLACKSTONE);
            // Crawlway walking space
            MapBuilder.fillBox(arenaWorld, cx - 2, cy - 4, cz - 25, cx + 2, cy - 2, cz + 25, Material.AIR);
            // Solid floor at cy - 5 with embedded strip lighting
            for (int z = cz - 20; z <= cz + 20; z += 4) {
                worldAt(arenaWorld, cx, cy - 5, z, Material.SEA_LANTERN);
            }
            // Emergency access ladders connecting sub-deck to main floor with solid blackstone backing
            for (int ly = 0; ly <= 4; ly++) {
                worldAt(arenaWorld, cx - 2, cy - ly, cz - 15, Material.POLISHED_BLACKSTONE);
                worldAt(arenaWorld, cx - 2, cy - ly, cz + 15, Material.POLISHED_BLACKSTONE);
                worldAt(arenaWorld, cx - 1, cy - ly, cz - 15, Material.LADDER);
                worldAt(arenaWorld, cx - 1, cy - ly, cz + 15, Material.LADDER);
            }
            // Solid floor at bottom of ladders
            worldAt(arenaWorld, cx - 1, cy - 5, cz - 15, Material.POLISHED_BLACKSTONE);
            worldAt(arenaWorld, cx - 1, cy - 5, cz + 15, Material.POLISHED_BLACKSTONE);

            // Waiting Lobby high above (cy + 55)
            Location waitingLoc = new Location(arenaWorld, cx + 0.5, cy + 55, cz + 0.5);
            MapBuilder.buildWaitingLobby(arenaWorld, waitingLoc, Material.SMOOTH_QUARTZ, Material.POLISHED_BLACKSTONE, Material.CYAN_STAINED_GLASS);

            MurderMysteryGame game = new MurderMysteryGame(plugin, id, "Murder Mystery: Cyber Station");
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));

            // 8 Scattered Player Spawns
            game.addSpawnPoint(new Location(arenaWorld, cx + 0.5, cy + 1, cz - 26 + 0.5, 0f, 0f));      // Command Deck
            game.addSpawnPoint(new Location(arenaWorld, cx + 22 + 0.5, cy + 1.2, cz + 0.5, 90f, 0f));     // Hydroponics Biosphere
            game.addSpawnPoint(new Location(arenaWorld, cx - 25 + 0.5, cy + 1, cz + 0.5, -90f, 0f));    // Cryo Lab
            game.addSpawnPoint(new Location(arenaWorld, cx + 0.5, cy + 1, cz + 26 + 0.5, 180f, 0f));    // Mess Hall
            game.addSpawnPoint(new Location(arenaWorld, cx + 0.5, cy + 6.2, cz + 10 + 0.5, 180f, 0f));  // 2nd Floor Catwalk
            game.addSpawnPoint(new Location(arenaWorld, cx + 0.5, cy + 6.2, cz - 10 + 0.5, 0f, 0f));    // 2nd Floor Catwalk
            game.addSpawnPoint(new Location(arenaWorld, cx + 0.5, cy - 4.0, cz + 0.5, 0f, 0f));        // Sub-deck Conduits
            game.addSpawnPoint(new Location(arenaWorld, cx + 0.5, cy + 1, cz + 0.5, 0f, 0f));           // Reactor Center

            // 24 Scattered Gold Spawns
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 0.5, cy + 1.2, cz + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 0.5, cy + 1.2, cz - 8 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 0.5, cy + 1.2, cz + 8 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx - 8 + 0.5, cy + 1.2, cz + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 8 + 0.5, cy + 1.2, cz + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 0.5, cy + 1.2, cz - 20 + 0.5)); // Command Hallway
            game.addGoldSpawnPoint(new Location(arenaWorld, cx - 6 + 0.5, cy + 1.2, cz - 28 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 6 + 0.5, cy + 1.2, cz - 28 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 20 + 0.5, cy + 1.2, cz + 0.5)); // Hydroponics
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 28 + 0.5, cy + 1.2, cz - 8 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 28 + 0.5, cy + 1.2, cz + 8 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx - 20 + 0.5, cy + 1.2, cz + 0.5)); // Cryo Lab
            game.addGoldSpawnPoint(new Location(arenaWorld, cx - 28 + 0.5, cy + 1.2, cz - 8 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx - 28 + 0.5, cy + 1.2, cz + 8 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 0.5, cy + 1.2, cz + 20 + 0.5)); // Mess Hall
            game.addGoldSpawnPoint(new Location(arenaWorld, cx - 6 + 0.5, cy + 1.2, cz + 26 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 6 + 0.5, cy + 1.2, cz + 26 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 0.5, cy + 6.2, cz - 10 + 0.5)); // 2nd Floor Catwalk
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 0.5, cy + 6.2, cz + 10 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx - 10 + 0.5, cy + 6.2, cz + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 10 + 0.5, cy + 6.2, cz + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 0.5, cy - 3.8, cz - 12 + 0.5)); // Sub-deck
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 0.5, cy - 3.8, cz + 12 + 0.5));
            game.addGoldSpawnPoint(new Location(arenaWorld, cx + 0.5, cy - 3.8, cz + 0.5));

            plugin.getGameManager().registerArena(game);
        }
    }

    // =========================================================================
    // 8. Prop Hunt Maps (Min 2, Max 4) - Zero Traps, Open Release, Rich Props
    // =========================================================================

    private void setupPropHuntMaps(World arenaWorld) {
        // Map 1: Warehouse & Workshop (Expanded 40x40 Open Industrial Facility)
        {
            int cx = -2500;
            int cy = 100;
            int cz = 3500;
            String id = "ph-1";

            // Clear previous map area
            MapBuilder.fillBox(arenaWorld, cx - 25, cy - 10, cz - 25, cx + 25, cy + 25, cz + 25, Material.AIR);

            // 1. Warehouse Floor (40x40)
            MapBuilder.fillBox(arenaWorld, cx - 20, cy, cz - 20, cx + 20, cy, cz + 20, Material.POLISHED_ANDESITE);
            MapBuilder.fillBox(arenaWorld, cx - 20, cy - 1, cz - 20, cx + 20, cy - 3, cz + 20, Material.SMOOTH_STONE);

            // 2. Brick Outer Walls (Height 10)
            MapBuilder.fillBox(arenaWorld, cx - 20, cy + 1, cz - 20, cx + 20, cy + 10, cz - 20, Material.BRICKS);
            MapBuilder.fillBox(arenaWorld, cx - 20, cy + 1, cz + 20, cx + 20, cy + 10, cz + 20, Material.BRICKS);
            MapBuilder.fillBox(arenaWorld, cx - 20, cy + 1, cz - 20, cx - 20, cy + 10, cz + 20, Material.BRICKS);
            MapBuilder.fillBox(arenaWorld, cx + 20, cy + 1, cz - 20, cx + 20, cy + 10, cz + 20, Material.BRICKS);

            // Warehouse Roof with Skylights
            MapBuilder.fillBox(arenaWorld, cx - 20, cy + 11, cz - 20, cx + 20, cy + 11, cz + 20, Material.SPRUCE_PLANKS);
            MapBuilder.fillBox(arenaWorld, cx - 12, cy + 11, cz - 12, cx + 12, cy + 11, cz + 12, Material.GLASS);

            // 3. Elevated Supervisor Balcony & Catwalk (at cy + 5 along North Wall) - OPEN AIR, NO CLOSED DOORS!
            MapBuilder.fillBox(arenaWorld, cx - 18, cy + 5, cz - 18, cx + 18, cy + 5, cz - 12, Material.SPRUCE_PLANKS);
            for (int x = cx - 18; x <= cx + 18; x++) {
                worldAt(arenaWorld, x, cy + 6, cz - 12, Material.IRON_BARS);
            }
            // 2 Wide Open Staircases descending to the main floor
            for (int s = 1; s <= 5; s++) {
                worldAt(arenaWorld, cx - 18, cy + s, cz - 12 + s, Material.SPRUCE_STAIRS);
                worldAt(arenaWorld, cx - 17, cy + s, cz - 12 + s, Material.SPRUCE_STAIRS);
                worldAt(arenaWorld, cx + 17, cy + s, cz - 12 + s, Material.SPRUCE_STAIRS);
                worldAt(arenaWorld, cx + 18, cy + s, cz - 12 + s, Material.SPRUCE_STAIRS);
            }

            // 4. Stacks of Natural Props across the Warehouse Floor!
            // Barrels & Shipping Pallets
            worldAt(arenaWorld, cx - 12, cy + 1, cz - 5, Material.BARREL);
            worldAt(arenaWorld, cx - 12, cy + 2, cz - 5, Material.BARREL);
            worldAt(arenaWorld, cx - 11, cy + 1, cz - 5, Material.BARREL);
            worldAt(arenaWorld, cx - 12, cy + 1, cz - 4, Material.CHEST);
            worldAt(arenaWorld, cx - 11, cy + 1, cz - 4, Material.CRAFTING_TABLE);

            // Workshop Bench Area
            worldAt(arenaWorld, cx + 8, cy + 1, cz - 4, Material.FURNACE);
            worldAt(arenaWorld, cx + 9, cy + 1, cz - 4, Material.ANVIL);
            worldAt(arenaWorld, cx + 10, cy + 1, cz - 4, Material.CAULDRON);
            worldAt(arenaWorld, cx + 11, cy + 1, cz - 4, Material.CRAFTING_TABLE);
            worldAt(arenaWorld, cx + 12, cy + 1, cz - 4, Material.FURNACE);

            // Harvest & Agriculture Stacks
            worldAt(arenaWorld, cx - 10, cy + 1, cz + 8, Material.HAY_BLOCK);
            worldAt(arenaWorld, cx - 10, cy + 2, cz + 8, Material.HAY_BLOCK);
            worldAt(arenaWorld, cx - 9, cy + 1, cz + 8, Material.HAY_BLOCK);
            worldAt(arenaWorld, cx - 10, cy + 1, cz + 9, Material.MELON);
            worldAt(arenaWorld, cx - 9, cy + 1, cz + 9, Material.MELON);

            // Explosives & Munitions Corner
            worldAt(arenaWorld, cx + 14, cy + 1, cz + 14, Material.TNT);
            worldAt(arenaWorld, cx + 14, cy + 2, cz + 14, Material.TNT);
            worldAt(arenaWorld, cx + 15, cy + 1, cz + 14, Material.BARREL);
            worldAt(arenaWorld, cx + 14, cy + 1, cz + 13, Material.CHEST);

            // Mezzanine Office Props
            worldAt(arenaWorld, cx - 14, cy + 6, cz - 16, Material.BOOKSHELF);
            worldAt(arenaWorld, cx - 13, cy + 6, cz - 16, Material.BOOKSHELF);
            worldAt(arenaWorld, cx - 12, cy + 6, cz - 16, Material.CHEST);
            worldAt(arenaWorld, cx - 11, cy + 6, cz - 16, Material.CRAFTING_TABLE);

            // Center Shipping Pallets
            worldAt(arenaWorld, cx, cy + 1, cz + 2, Material.BARREL);
            worldAt(arenaWorld, cx + 1, cy + 1, cz + 2, Material.CHEST);
            worldAt(arenaWorld, cx, cy + 1, cz + 3, Material.CRAFTING_TABLE);

            // Waiting Cage high above
            Location waitingLoc = new Location(arenaWorld, cx + 0.5, cy + 55, cz + 0.5);
            MapBuilder.buildWaitingLobby(arenaWorld, waitingLoc, Material.POLISHED_ANDESITE, Material.BRICK_WALL, Material.ORANGE_STAINED_GLASS);

            PropHuntGame game = new PropHuntGame(plugin, id, "Prop Hunt: Warehouse");
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));

            // Hunter Spawn on open supervisor balcony (NO closed doors!)
            game.setHunterSpawn(new Location(arenaWorld, cx + 0.5, cy + 6, cz - 15 + 0.5, 180f, 0f));
            // Hunter Release Location: straight down onto the open warehouse floor!
            game.setHunterReleaseLocation(new Location(arenaWorld, cx + 0.5, cy + 1, cz - 5 + 0.5, 180f, 0f));

            // 6 Scattered Prop Spawns (All in wide open areas!)
            game.addPropSpawn(new Location(arenaWorld, cx - 14 + 0.5, cy + 1, cz - 2 + 0.5, 0f, 0f));
            game.addPropSpawn(new Location(arenaWorld, cx + 14 + 0.5, cy + 1, cz - 2 + 0.5, 0f, 0f));
            game.addPropSpawn(new Location(arenaWorld, cx - 14 + 0.5, cy + 1, cz + 14 + 0.5, 90f, 0f));
            game.addPropSpawn(new Location(arenaWorld, cx + 14 + 0.5, cy + 1, cz + 14 + 0.5, -90f, 0f));
            game.addPropSpawn(new Location(arenaWorld, cx + 0.5, cy + 1, cz + 10 + 0.5, 180f, 0f));
            game.addPropSpawn(new Location(arenaWorld, cx - 8 + 0.5, cy + 6, cz - 15 + 0.5, 90f, 0f)); // Catwalk

            plugin.getGameManager().registerArena(game);
        }

        // Map 2: Grand Bazaar & Bakery (Expanded 40x40 Sunlit Courtyard - COMPLETELY OPEN, ZERO TRAP ROOMS)
        {
            int cx = -2900;
            int cy = 100;
            int cz = 3500;
            String id = "ph-2";

            // Clear previous map area
            MapBuilder.fillBox(arenaWorld, cx - 25, cy - 10, cz - 25, cx + 25, cy + 25, cz + 25, Material.AIR);

            // 1. Courtyard Floor (40x40)
            MapBuilder.fillBox(arenaWorld, cx - 20, cy, cz - 20, cx + 20, cy, cz + 20, Material.CUT_SANDSTONE);
            MapBuilder.fillBox(arenaWorld, cx - 20, cy - 1, cz - 20, cx + 20, cy - 3, cz + 20, Material.SMOOTH_SANDSTONE);

            // 2. Sandstone Perimeter Walls (Height 8)
            MapBuilder.fillBox(arenaWorld, cx - 20, cy + 1, cz - 20, cx + 20, cy + 8, cz - 20, Material.SMOOTH_SANDSTONE);
            MapBuilder.fillBox(arenaWorld, cx - 20, cy + 1, cz + 20, cx + 20, cy + 8, cz + 20, Material.SMOOTH_SANDSTONE);
            MapBuilder.fillBox(arenaWorld, cx - 20, cy + 1, cz - 20, cx - 20, cy + 8, cz + 20, Material.SMOOTH_SANDSTONE);
            MapBuilder.fillBox(arenaWorld, cx + 20, cy + 1, cz - 20, cx + 20, cy + 8, cz + 20, Material.SMOOTH_SANDSTONE);

            // Wall Battlements & Lanterns
            for (int x = cx - 20; x <= cx + 20; x += 4) {
                worldAt(arenaWorld, x, cy + 9, cz - 20, Material.SANDSTONE_WALL);
                worldAt(arenaWorld, x, cy + 10, cz - 20, Material.LANTERN);
                worldAt(arenaWorld, x, cy + 9, cz + 20, Material.SANDSTONE_WALL);
                worldAt(arenaWorld, x, cy + 10, cz + 20, Material.LANTERN);
            }

            // 3. Central Marble Bazaar Fountain
            MapBuilder.fillCircle(arenaWorld, cx, cy + 1, cz, 4, Material.SMOOTH_SANDSTONE);
            MapBuilder.fillCircle(arenaWorld, cx, cy + 1, cz, 3, Material.WATER);
            worldAt(arenaWorld, cx, cy + 2, cz, Material.CHISELED_SANDSTONE);
            worldAt(arenaWorld, cx, cy + 3, cz, Material.SEA_LANTERN);

            // 4. Elevated Open Hunter Pavilion (North: cz - 16 to cz - 10) - COMPLETELY OPEN AIR, ZERO DOORS!
            MapBuilder.fillBox(arenaWorld, cx - 4, cy + 3, cz - 17, cx + 4, cy + 3, cz - 11, Material.SMOOTH_SANDSTONE);
            // 4 Open Sandstone Corner Pillars
            MapBuilder.buildPillar(arenaWorld, cx - 4, cy + 4, cz - 17, 3, Material.SANDSTONE_WALL);
            MapBuilder.buildPillar(arenaWorld, cx + 4, cy + 4, cz - 17, 3, Material.SANDSTONE_WALL);
            MapBuilder.buildPillar(arenaWorld, cx - 4, cy + 4, cz - 11, 3, Material.SANDSTONE_WALL);
            MapBuilder.buildPillar(arenaWorld, cx + 4, cy + 4, cz - 11, 3, Material.SANDSTONE_WALL);
            // Open Canopy Roof
            MapBuilder.fillBox(arenaWorld, cx - 4, cy + 7, cz - 17, cx + 4, cy + 7, cz - 11, Material.RED_WOOL);
            // Wide Open Grand Stairs leading down into the courtyard!
            for (int s = 1; s <= 3; s++) {
                worldAt(arenaWorld, cx - 1, cy + 4 - s, cz - 11 + s, Material.SMOOTH_SANDSTONE_STAIRS);
                worldAt(arenaWorld, cx, cy + 4 - s, cz - 11 + s, Material.SMOOTH_SANDSTONE_STAIRS);
                worldAt(arenaWorld, cx + 1, cy + 4 - s, cz - 11 + s, Material.SMOOTH_SANDSTONE_STAIRS);
            }

            // 5. Six Colorful Open-Air Market Stalls! (Zero closed doors)
            // Stall 1 (West - Produce & Agriculture)
            MapBuilder.fillBox(arenaWorld, cx - 16, cy + 4, cz - 8, cx - 10, cy + 4, cz - 2, Material.YELLOW_WOOL);
            worldAt(arenaWorld, cx - 14, cy + 1, cz - 6, Material.MELON);
            worldAt(arenaWorld, cx - 14, cy + 2, cz - 6, Material.MELON);
            worldAt(arenaWorld, cx - 13, cy + 1, cz - 6, Material.HAY_BLOCK);
            worldAt(arenaWorld, cx - 13, cy + 2, cz - 6, Material.HAY_BLOCK);
            worldAt(arenaWorld, cx - 12, cy + 1, cz - 6, Material.BARREL);
            worldAt(arenaWorld, cx - 13, cy + 1, cz - 5, Material.CRAFTING_TABLE);

            // Stall 2 (East - Stone Bakery)
            MapBuilder.fillBox(arenaWorld, cx + 10, cy + 4, cz - 8, cx + 16, cy + 4, cz - 2, Material.ORANGE_WOOL);
            worldAt(arenaWorld, cx + 14, cy + 1, cz - 6, Material.FURNACE);
            worldAt(arenaWorld, cx + 14, cy + 2, cz - 6, Material.FURNACE);
            worldAt(arenaWorld, cx + 13, cy + 1, cz - 6, Material.FURNACE);
            worldAt(arenaWorld, cx + 13, cy + 1, cz - 5, Material.BARREL);
            worldAt(arenaWorld, cx + 14, cy + 1, cz - 4, Material.CHEST);
            worldAt(arenaWorld, cx + 13, cy + 1, cz - 4, Material.ANVIL);

            // Stall 3 (South-West - Textile & Carpets)
            MapBuilder.fillBox(arenaWorld, cx - 16, cy + 4, cz + 4, cx - 10, cy + 4, cz + 10, Material.LIME_WOOL);
            worldAt(arenaWorld, cx - 14, cy + 1, cz + 6, Material.BOOKSHELF);
            worldAt(arenaWorld, cx - 14, cy + 2, cz + 6, Material.BOOKSHELF);
            worldAt(arenaWorld, cx - 13, cy + 1, cz + 6, Material.CHEST);
            worldAt(arenaWorld, cx - 13, cy + 1, cz + 7, Material.CRAFTING_TABLE);
            worldAt(arenaWorld, cx - 14, cy + 1, cz + 7, Material.BARREL);

            // Stall 4 (South-East - Magic & Spices)
            MapBuilder.fillBox(arenaWorld, cx + 10, cy + 4, cz + 4, cx + 16, cy + 4, cz + 10, Material.CYAN_WOOL);
            worldAt(arenaWorld, cx + 14, cy + 1, cz + 6, Material.CAULDRON);
            worldAt(arenaWorld, cx + 13, cy + 1, cz + 6, Material.BOOKSHELF);
            worldAt(arenaWorld, cx + 13, cy + 2, cz + 6, Material.BOOKSHELF);
            worldAt(arenaWorld, cx + 14, cy + 1, cz + 7, Material.CHEST);
            worldAt(arenaWorld, cx + 13, cy + 1, cz + 7, Material.ANVIL);

            // Stall 5 (South - Blacksmith & Armour)
            MapBuilder.fillBox(arenaWorld, cx - 4, cy + 4, cz + 13, cx + 4, cy + 4, cz + 17, Material.RED_WOOL);
            worldAt(arenaWorld, cx - 2, cy + 1, cz + 15, Material.ANVIL);
            worldAt(arenaWorld, cx - 1, cy + 1, cz + 15, Material.FURNACE);
            worldAt(arenaWorld, cx, cy + 1, cz + 15, Material.CAULDRON);
            worldAt(arenaWorld, cx + 1, cy + 1, cz + 15, Material.BARREL);
            worldAt(arenaWorld, cx + 2, cy + 1, cz + 15, Material.CRAFTING_TABLE);

            // Waiting Cage
            Location waitingLoc = new Location(arenaWorld, cx + 0.5, cy + 55, cz + 0.5);
            MapBuilder.buildWaitingLobby(arenaWorld, waitingLoc, Material.CUT_SANDSTONE, Material.SANDSTONE_WALL, Material.YELLOW_STAINED_GLASS);

            PropHuntGame game = new PropHuntGame(plugin, id, "Prop Hunt: Grand Bazaar");
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));

            // Hunter Spawn on open pavilion (NO DOORS, COMPLETELY OPEN!)
            game.setHunterSpawn(new Location(arenaWorld, cx + 0.5, cy + 4, cz - 14 + 0.5, 180f, 0f));
            // Hunter Release Location: Right in the open central pathway facing the fountain!
            game.setHunterReleaseLocation(new Location(arenaWorld, cx + 0.5, cy + 1, cz - 6 + 0.5, 180f, 0f));

            // 6 Scattered Prop Spawns (Wide open unobstructed plaza paths!)
            game.addPropSpawn(new Location(arenaWorld, cx - 8 + 0.5, cy + 1, cz - 2 + 0.5, -90f, 0f));
            game.addPropSpawn(new Location(arenaWorld, cx + 8 + 0.5, cy + 1, cz - 2 + 0.5, 90f, 0f));
            game.addPropSpawn(new Location(arenaWorld, cx - 8 + 0.5, cy + 1, cz + 5 + 0.5, -90f, 0f));
            game.addPropSpawn(new Location(arenaWorld, cx + 8 + 0.5, cy + 1, cz + 5 + 0.5, 90f, 0f));
            game.addPropSpawn(new Location(arenaWorld, cx + 0.5, cy + 1, cz + 8 + 0.5, 180f, 0f));
            game.addPropSpawn(new Location(arenaWorld, cx + 0.5, cy + 1, cz - 2 + 0.5, 0f, 0f));

            plugin.getGameManager().registerArena(game);
        }
    }

    private void worldAt(World world, int x, int y, int z, Material mat) {
        world.getBlockAt(x, y, z).setType(mat, false);
    }
}
