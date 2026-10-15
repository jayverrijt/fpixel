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
        MapBuilder.buildGiantPickaxe(world, 0, cy + 15, -78);

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
        MapBuilder.buildGiantBed(world, -70, cy + 13, 0);

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
        MapBuilder.buildGiantTNT(world, 60, cy + 13, -48);

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
        MapBuilder.buildGiantToriiEmblem(world, -55, cy + 13, 65);

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
        MapBuilder.buildGiantEnderEye(world, 72, cy + 13, 10);

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
        MapBuilder.buildGiantCrossedSwords(world, 0, cy + 13, 76);

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
        MapBuilder.buildGiantBoot(world, -58, cy + 13, -54);

        // Rustic Windmill on Parkour Island (Slabs, zero stairs)
        MapBuilder.buildWindmill(world, -66, cy + 1, -62);

        // 12. Spawn Point at Grand Central Plaza facing North (towards Fountain, Cherry Trees & Royal Citadel)
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
                int jumpCount = 4 + rnd.nextInt(3); // 4 to 6 randomized jumps per checkpoint
                for (int j = 0; j < jumpCount; j++) {
                    int dx = rnd.nextInt(5) - 2; // -2 to +2
                    int dz = 3 + (rnd.nextInt(100) < 35 ? 1 : 0); // 3 or 4 blocks distance
                    int dy = rnd.nextInt(3) - 1; // -1, 0, or +1 elevation

                    // Keep Y within safe jumping bounds [cy - 2, cy + 8]
                    if (curY + dy < cy - 2 || curY + dy > cy + 8) dy = 0;

                    curX += dx;
                    curY += dy;
                    curZ += dz;

                    Material jumpMat = palette[rnd.nextInt(palette.length)];
                    // Occasional special obstacle
                    if (j == 2 && i % 2 == 1) {
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
                    int dx = rnd.nextInt(3) - 1; // -1, 0, +1
                    int dz = 3 + (rnd.nextBoolean() ? 1 : 0);
                    int dy = rnd.nextInt(3) - 1; // -1, 0, +1
                    if (curY + dy < cy - 1 || curY + dy > cy + 6) dy = 0;
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
