package net.fpixel.core.map;

import net.fpixel.FPixelPlugin;
import net.fpixel.games.bedwars.BedWarsGame;
import net.fpixel.games.bedwars.BedWarsSpawner;
import net.fpixel.games.bedwars.BedWarsTeam;
import net.fpixel.games.bridge.BridgeGame;
import net.fpixel.games.parkour.ParkourGame;
import net.fpixel.games.pvp.PvPGame;
import net.fpixel.games.skywars.SkyWarsGame;
import net.fpixel.games.sumo.SumoGame;
import net.fpixel.games.tntrun.TNTRunGame;
import org.bukkit.*;

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

        // 2. Global Multiplayer BedWars (Min 3, Max 8-16) - Loads downloaded worlds if available
        setupGlobalBedWarsMaps(defaultWorld);

        // 3. Global Multiplayer SkyWars (Min 3, Max 12)
        setupGlobalSkyWarsMaps(defaultWorld);

        // 4. Global Multiplayer TNT Run (Min 3, Max 16)
        setupGlobalTNTRunMaps(defaultWorld);

        // 5. Global Multiplayer Parkour (Min 3, Max 16)
        setupGlobalParkourMaps(defaultWorld);

        // 6. Dedicated 1v1 Duel Arenas (Min 2, Max 2) for:
        //    SkyWars, BedWars, PvP, Parkour, TNT, Sumo, Bridge
        setupAllDuelArenas(defaultWorld);

        plugin.getLogger().info("Map setup complete! All global lobbies and 1v1 duel arenas are live.");
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
    }

    private void buildGrandHub(World world) {
        int cy = 100;
        plugin.getLogger().info("Rebuilding Grand Hub with survival-inspired architecture, launchpads, and gamemode NPCs...");

        // 1. Clear upper airspace
        MapBuilder.fillBox(world, -75, cy + 1, -75, 75, cy + 50, 75, Material.AIR);

        // 2. Vast Natural Floating Island (Radius 64 blocks)
        MapBuilder.buildFloatingIslandNatural(world, 0, cy, 0, 64, Material.GRASS_BLOCK, Material.DIRT, Material.DEEPSLATE);

        // 3. Central Grand Launch Plaza (Radius 18 blocks)
        MapBuilder.fillCircle(world, 0, cy, 0, 18, Material.STONE_BRICKS);
        MapBuilder.fillRing(world, 0, cy, 0, 16, 18, Material.POLISHED_ANDESITE);
        MapBuilder.fillRing(world, 0, cy, 0, 13, 14, Material.SEA_LANTERN);
        MapBuilder.fillRing(world, 0, cy, 0, 10, 12, Material.DEEPSLATE_TILES);
        MapBuilder.fillRing(world, 0, cy, 0, 7, 9, Material.POLISHED_DIORITE);

        // Grand Central Tiered Fountain & Beacon Spire
        MapBuilder.buildGrandFountain(world, 0, cy, 0, 5);
        world.getBlockAt(0, cy + 1, 0).setType(Material.BEACON, false);

        // Low stone balustrade around plaza perimeter with lanterns
        for (int angle = 0; angle < 360; angle += 15) {
            double rad = Math.toRadians(angle);
            int bx = (int) Math.round(18 * Math.cos(rad));
            int bz = (int) Math.round(18 * Math.sin(rad));
            world.getBlockAt(bx, cy + 1, bz).setType(Material.STONE_BRICK_WALL, false);
            if (angle % 30 == 0) {
                world.getBlockAt(bx, cy + 2, bz).setType(Material.LANTERN, false);
            }
        }

        // 4. The 8 Super Launchpads (Launch Spot) Radiating from Central Plaza
        int[][] launchpadCoords = {
                {0, -13},   // North -> Survival SMP
                {9, -9},    // North-East -> BedWars
                {13, 0},    // East -> SkyWars
                {9, 9},     // South-East -> TNT Run
                {0, 13},    // South -> Duels & PvP
                {-9, 9},    // South-West -> The Bridge
                {-13, 0},   // West -> Sumo
                {-9, -9}    // North-West -> Parkour
        };
        for (int[] lp : launchpadCoords) {
            int lx = lp[0], lz = lp[1];
            MapBuilder.fillRing(world, lx, cy, lz, 1, 2, Material.GOLD_BLOCK);
            world.getBlockAt(lx, cy, lz).setType(Material.EMERALD_BLOCK, false);
            world.getBlockAt(lx, cy + 1, lz).setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, false);
            world.getBlockAt(lx, cy - 1, lz).setType(Material.SEA_LANTERN, false);
        }

        // 5. Connecting Scenic Pathways (Dirt Path, Cobblestone, Stone Bricks)
        MapBuilder.buildScenicPath(world, 0, -14, 0, -28, cy, 3);      // North
        MapBuilder.buildScenicPath(world, 10, -10, 22, -22, cy, 3);    // North-East
        MapBuilder.buildScenicPath(world, 14, 0, 28, 0, cy, 3);        // East
        MapBuilder.buildScenicPath(world, 10, 10, 22, 22, cy, 3);      // South-East
        MapBuilder.buildScenicPath(world, 0, 14, 0, 28, cy, 3);        // South
        MapBuilder.buildScenicPath(world, -10, 10, -22, 22, cy, 3);    // South-West
        MapBuilder.buildScenicPath(world, -14, 0, -28, 0, cy, 3);      // West
        MapBuilder.buildScenicPath(world, -10, -10, -22, -22, cy, 3);  // North-West
        // Secondary trails
        MapBuilder.buildScenicPath(world, -22, -22, -36, -36, cy, 2);  // Trail to Manor
        MapBuilder.buildScenicPath(world, 22, -22, 34, -34, cy, 2);    // Trail to Windmill

        // 6. Natural Curving River & Freshwater Canal
        for (int rz = -38; rz <= -24; rz++) {
            for (int rx = -16; rx <= 16; rx++) {
                int distFromCenter = Math.abs(rz - (-31));
                if (Math.abs(rx) <= 8 && distFromCenter <= 2) {
                    world.getBlockAt(rx, cy, rz).setType(Material.WATER, false);
                    world.getBlockAt(rx, cy - 1, rz).setType(Material.SAND, false);
                    if ((rx + rz) % 3 == 0) {
                        world.getBlockAt(rx, cy + 1, rz).setType(Material.LILY_PAD, false);
                    }
                }
            }
        }

        // Authentic arched wooden bridge spanning the river right in front of the Survival Gate!
        MapBuilder.buildArchedWoodenBridge(world, 0, -35, 0, -26, cy, 3);

        // -----------------------------------------------------------------
        // 7. The 8 Dedicated Gamemode Pavilions & Buildings
        // -----------------------------------------------------------------

        // 1. Survival SMP Grand Gate & Lodge (North: 0, cy, -32)
        int sgZ = -34;
        MapBuilder.buildPillar(world, -4, cy + 1, sgZ, 7, Material.STRIPPED_OAK_LOG);
        MapBuilder.buildPillar(world, 4, cy + 1, sgZ, 7, Material.STRIPPED_OAK_LOG);
        for (int px = -5; px <= 5; px++) {
            world.getBlockAt(px, cy + 8, sgZ).setType(Material.SPRUCE_STAIRS, false);
            world.getBlockAt(px, cy + 7, sgZ).setType(Material.DARK_OAK_PLANKS, false);
            world.getBlockAt(px, cy + 6, sgZ).setType(Material.STRIPPED_OAK_LOG, false);
        }
        world.getBlockAt(-4, cy + 5, sgZ).setType(Material.LANTERN, false);
        world.getBlockAt(4, cy + 5, sgZ).setType(Material.LANTERN, false);
        world.getBlockAt(0, cy + 5, sgZ).setType(Material.CAMPFIRE, false);
        world.getBlockAt(0, cy + 6, sgZ).setType(Material.COBBLESTONE_WALL, false);
        world.getBlockAt(-5, cy + 1, sgZ).setType(Material.BARREL, false);
        world.getBlockAt(5, cy + 1, sgZ).setType(Material.CHEST, false);
        world.getBlockAt(-5, cy + 2, sgZ).setType(Material.LANTERN, false);
        world.getBlockAt(0, cy, -30).setType(Material.DIAMOND_BLOCK, false);
        world.getBlockAt(0, cy + 1, -30).setType(Material.AIR, false);

        // 2. BedWars Fortress & Forge (North-East: 22, cy, -22)
        MapBuilder.buildGamePavilion(world, 22, cy, -22, 5, Material.STONE_BRICKS, Material.DEEPSLATE_BRICK_WALL, Material.RED_TERRACOTTA, Material.RED_GLAZED_TERRACOTTA);
        world.getBlockAt(25, cy + 1, -22).setType(Material.ANVIL, false);
        world.getBlockAt(25, cy + 1, -23).setType(Material.CAULDRON, false);
        world.getBlockAt(25, cy + 1, -21).setType(Material.BARREL, false);
        world.getBlockAt(22, cy + 1, -25).setType(Material.RED_BED, false);

        // 3. SkyWars Mystical Shrine (East: 30, cy, 0)
        MapBuilder.buildGamePavilion(world, 30, cy, 0, 5, Material.SMOOTH_QUARTZ, Material.QUARTZ_PILLAR, Material.PURPUR_BLOCK, Material.AMETHYST_BLOCK);
        world.getBlockAt(30, cy + 1, 3).setType(Material.ENCHANTING_TABLE, false);
        world.getBlockAt(30, cy + 1, -3).setType(Material.ENDER_CHEST, false);
        world.getBlockAt(30, cy + 2, 3).setType(Material.SOUL_LANTERN, false);
        world.getBlockAt(30, cy + 2, -3).setType(Material.SOUL_LANTERN, false);

        // 4. TNT Run Volcanic Colosseum (South-East: 22, cy, 22)
        MapBuilder.buildGamePavilion(world, 22, cy, 22, 5, Material.NETHER_BRICKS, Material.NETHER_BRICK_WALL, Material.RED_NETHER_BRICKS, Material.MAGMA_BLOCK);
        world.getBlockAt(25, cy + 1, 22).setType(Material.TNT, false);
        world.getBlockAt(25, cy + 2, 22).setType(Material.TNT, false);
        world.getBlockAt(22, cy + 1, 25).setType(Material.CAMPFIRE, false);

        // 5. Duels & PvP Knight's Armory (South: 0, cy, 30)
        MapBuilder.buildGamePavilion(world, 0, cy, 30, 5, Material.POLISHED_ANDESITE, Material.STONE_BRICK_WALL, Material.DARK_OAK_PLANKS, Material.IRON_BLOCK);
        world.getBlockAt(-3, cy + 1, 30).setType(Material.HAY_BLOCK, false);
        world.getBlockAt(-3, cy + 2, 30).setType(Material.TARGET, false);
        world.getBlockAt(3, cy + 1, 30).setType(Material.SMITHING_TABLE, false);
        world.getBlockAt(3, cy + 2, 30).setType(Material.LANTERN, false);

        // 6. The Bridge 1v1 Arena Pavilion (South-West: -22, cy, 22)
        MapBuilder.buildGamePavilion(world, -22, cy, 22, 5, Material.SPRUCE_PLANKS, Material.OAK_FENCE, Material.DARK_OAK_STAIRS, Material.LAPIS_BLOCK);
        world.getBlockAt(-25, cy + 1, 22).setType(Material.RED_TERRACOTTA, false);
        world.getBlockAt(-25, cy + 2, 22).setType(Material.RED_GLAZED_TERRACOTTA, false);
        world.getBlockAt(-19, cy + 1, 22).setType(Material.BLUE_TERRACOTTA, false);
        world.getBlockAt(-19, cy + 2, 22).setType(Material.BLUE_GLAZED_TERRACOTTA, false);

        // 7. Sumo Dojo & Sacred Ring (West: -30, cy, 0)
        MapBuilder.fillCircle(world, -30, cy, 0, 5, Material.POLISHED_ANDESITE);
        MapBuilder.fillRing(world, -30, cy, 0, 4, 5, Material.GOLD_BLOCK);
        MapBuilder.buildToriiArch(world, -30, cy, -6);
        world.getBlockAt(-30, cy, 0).setType(Material.YELLOW_TERRACOTTA, false);

        // 8. Parkour Duels Tower (North-West: -22, cy, -22)
        MapBuilder.buildGamePavilion(world, -22, cy, -22, 5, Material.OAK_PLANKS, Material.OAK_LOG, Material.SPRUCE_STAIRS, Material.EMERALD_BLOCK);
        world.getBlockAt(-25, cy + 1, -22).setType(Material.SLIME_BLOCK, false);
        world.getBlockAt(-25, cy + 2, -22).setType(Material.GOLD_BLOCK, false);
        world.getBlockAt(-25, cy + 3, -22).setType(Material.EMERALD_BLOCK, false);

        // -----------------------------------------------------------------
        // 8. Survival-World-Inspired Surroundings & Masterpiece Buildings
        // -----------------------------------------------------------------

        // Grand Survival Estate Manor (-46, cy, -48)
        MapBuilder.buildSurvivalEstateManor(world, -46, cy, -48, 11, 13);

        // Rustic Wooden Windmill & Golden Farmland (North-East: 38, cy, -38)
        MapBuilder.buildWindmill(world, 42, cy, -42);
        MapBuilder.fillBox(world, 32, cy, -46, 48, cy, -34, Material.FARMLAND);
        world.getBlockAt(36, cy, -40).setType(Material.WATER, false);
        world.getBlockAt(44, cy, -40).setType(Material.WATER, false);
        for (int fx = 33; fx <= 47; fx++) {
            for (int fz = -45; fz <= -35; fz++) {
                if (world.getBlockAt(fx, cy, fz).getType() == Material.FARMLAND) {
                    world.getBlockAt(fx, cy + 1, fz).setType(Material.WHEAT, false);
                }
            }
        }
        world.getBlockAt(33, cy + 1, -34).setType(Material.PUMPKIN, false);
        world.getBlockAt(34, cy + 1, -34).setType(Material.HAY_BLOCK, false);
        world.getBlockAt(35, cy + 1, -34).setType(Material.HAY_BLOCK, false);

        // Lampposts & Cozy Park Benches
        MapBuilder.buildLamppost(world, -12, cy, -12);
        MapBuilder.buildLamppost(world, 12, cy, -12);
        MapBuilder.buildLamppost(world, -12, cy, 12);
        MapBuilder.buildLamppost(world, 12, cy, 12);
        MapBuilder.buildLamppost(world, 0, cy, -20);
        MapBuilder.buildLamppost(world, 0, cy, 20);
        MapBuilder.buildLamppost(world, -20, cy, 0);
        MapBuilder.buildLamppost(world, 20, cy, 0);

        MapBuilder.buildParkBench(world, -6, cy, -15);
        MapBuilder.buildParkBench(world, 6, cy, -15);
        MapBuilder.buildParkBench(world, -6, cy, 15);
        MapBuilder.buildParkBench(world, 6, cy, 15);

        // Lush Trees & Cherry Blossoms
        MapBuilder.buildOrganicTree(world, -18, cy + 1, -14, Material.CHERRY_LOG, Material.CHERRY_LEAVES, 7);
        MapBuilder.buildOrganicTree(world, -36, cy + 1, 6, Material.CHERRY_LOG, Material.CHERRY_LEAVES, 7);
        MapBuilder.buildOrganicTree(world, 18, cy + 1, -14, Material.OAK_LOG, Material.AZALEA_LEAVES, 7);
        MapBuilder.buildOrganicTree(world, 18, cy + 1, 14, Material.CHERRY_LOG, Material.CHERRY_LEAVES, 7);
        MapBuilder.buildOrganicTree(world, -18, cy + 1, 14, Material.OAK_LOG, Material.OAK_LEAVES, 7);
        MapBuilder.buildOrganicTree(world, 0, cy + 1, -44, Material.OAK_LOG, Material.AZALEA_LEAVES, 8);
        MapBuilder.buildOrganicTree(world, 0, cy + 1, 44, Material.OAK_LOG, Material.OAK_LEAVES, 8);

        // Flowerbeds around Central Plaza
        for (int angle = 7; angle < 360; angle += 15) {
            double rad = Math.toRadians(angle);
            int fx = (int) Math.round(17 * Math.cos(rad));
            int fz = (int) Math.round(17 * Math.sin(rad));
            Material flower = (angle % 2 == 0) ? Material.POPPY : Material.CORNFLOWER;
            world.getBlockAt(fx, cy + 1, fz).setType(flower, false);
        }

        // 9. Spawn Point at Grand Central Plaza facing North (towards Survival Gateway & Fountain)
        Location hubSpawn = new Location(world, 0.5, cy + 1, 6.5, 0f, 0f);
        plugin.getLobbyManager().setSpawn(hubSpawn);

        // 10. Spawn All Interactive Gamemode NPCs with Floating Holograms!
        plugin.getHubNPCManager().spawnAllNPCs(world);

        plugin.getLogger().info("Grand Hub rebuild complete! All pavilions, launchpads, and NPCs are live.");
    }


    // =========================================================================
    // 2. Global BedWars Maps (Min 3, Max 16)
    // =========================================================================

    private void setupGlobalBedWarsMaps(World defaultWorld) {
        String[] mapNames = {
                "Airshow", "Lighthouse", "Amazon", "Apollo", "Cascade",
                "Kingdom Castle", "Sandstone Citadel", "Cyber Spire", "Frozen Bastion", "Nether Fortress"
        };

        for (int i = 0; i < mapNames.length; i++) {
            String name = mapNames[i];
            String id = "bw-" + (i + 1);
            World mapWorld = loadOrCreateWorld(name, defaultWorld);

            int cx = (mapWorld.equals(defaultWorld)) ? (i + 1) * 350 : 0;
            int cy = 100;
            int cz = (mapWorld.equals(defaultWorld)) ? 0 : 0;

            Location waitingLoc = new Location(mapWorld, cx + 0.5, cy + 35, cz + 0.5);
            MapBuilder.buildWaitingLobby(mapWorld, waitingLoc, Material.STONE_BRICKS, Material.SMOOTH_STONE, Material.WHITE_STAINED_GLASS);

            if (mapWorld.equals(defaultWorld)) {
                // Procedural generation if world was not downloaded
                MapBuilder.buildFloatingIslandNatural(mapWorld, cx, cy, cz, 16, Material.STONE_BRICKS, Material.SMOOTH_STONE, Material.COBBLESTONE);
                MapBuilder.buildCastleTower(mapWorld, cx, cy + 1, cz, 4, 12, Material.STONE_BRICKS, Material.SMOOTH_STONE, Material.DARK_OAK_STAIRS);
                worldAt(mapWorld, cx, cy + 1, cz, Material.EMERALD_BLOCK);

                int rx = cx - 45;
                MapBuilder.buildFloatingIslandNatural(mapWorld, rx, cy, cz, 12, Material.STONE_BRICKS, Material.SMOOTH_STONE, Material.COBBLESTONE);
                MapBuilder.buildMedievalHouse(mapWorld, rx - 4, cy + 1, cz - 4, 8, 8, Material.STONE_BRICKS, Material.OAK_LOG, Material.DARK_OAK_STAIRS, Material.SPRUCE_SLAB);
                worldAt(mapWorld, rx - 2, cy + 2, cz, Material.RED_BED);
                worldAt(mapWorld, rx + 2, cy + 2, cz, Material.IRON_BLOCK);

                int bx = cx + 45;
                MapBuilder.buildFloatingIslandNatural(mapWorld, bx, cy, cz, 12, Material.STONE_BRICKS, Material.SMOOTH_STONE, Material.COBBLESTONE);
                MapBuilder.buildMedievalHouse(mapWorld, bx - 4, cy + 1, cz - 4, 8, 8, Material.STONE_BRICKS, Material.OAK_LOG, Material.DARK_OAK_STAIRS, Material.SPRUCE_SLAB);
                worldAt(mapWorld, bx + 2, cy + 2, cz, Material.BLUE_BED);
                worldAt(mapWorld, bx - 2, cy + 2, cz, Material.IRON_BLOCK);
            }

            BedWarsGame game = new BedWarsGame(plugin, id, "BedWars: " + name, 3, 16);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));

            BedWarsTeam red = game.getTeam("RED");
            if (red != null) {
                red.setSpawnLocation(new Location(mapWorld, cx - 45 + 0.5, cy + 2, cz + 0.5));
                red.setBedLocation(new Location(mapWorld, cx - 47, cy + 2, cz));
            }
            BedWarsTeam blue = game.getTeam("BLUE");
            if (blue != null) {
                blue.setSpawnLocation(new Location(mapWorld, cx + 45 + 0.5, cy + 2, cz + 0.5));
                blue.setBedLocation(new Location(mapWorld, cx + 47, cy + 2, cz));
            }

            game.addSpawner(new BedWarsSpawner(new Location(mapWorld, cx - 42.5, cy + 2.5, cz + 0.5), BedWarsSpawner.SpawnerType.IRON));
            game.addSpawner(new BedWarsSpawner(new Location(mapWorld, cx - 42.5, cy + 2.5, cz + 0.5), BedWarsSpawner.SpawnerType.GOLD));
            game.addSpawner(new BedWarsSpawner(new Location(mapWorld, cx + 42.5, cy + 2.5, cz + 0.5), BedWarsSpawner.SpawnerType.IRON));
            game.addSpawner(new BedWarsSpawner(new Location(mapWorld, cx + 42.5, cy + 2.5, cz + 0.5), BedWarsSpawner.SpawnerType.GOLD));
            game.addSpawner(new BedWarsSpawner(new Location(mapWorld, cx + 0.5, cy + 2.5, cz + 0.5), BedWarsSpawner.SpawnerType.EMERALD));

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

        for (int i = 0; i < names.length; i++) {
            int cx = (i + 1) * 350;
            int cy = 100;
            int cz = 350;
            String id = "sw-" + (i + 1);

            Location waitingLoc = new Location(defaultWorld, cx + 0.5, cy + 35, cz + 0.5);
            MapBuilder.buildWaitingLobby(defaultWorld, waitingLoc, Material.POLISHED_ANDESITE, Material.STONE_BRICKS, Material.CYAN_STAINED_GLASS);

            SkyWarsGame game = new SkyWarsGame(plugin, id, "SkyWars: " + names[i], 3, 12, cy - 40);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));

            MapBuilder.buildFloatingIslandNatural(defaultWorld, cx, cy, cz, 16, Material.GRASS_BLOCK, Material.DIRT, Material.DEEPSLATE);
            MapBuilder.buildCastleTower(defaultWorld, cx, cy + 1, cz, 4, 14, Material.STONE_BRICKS, Material.SMOOTH_STONE, Material.DARK_OAK_STAIRS);

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

                MapBuilder.buildFloatingIslandNatural(defaultWorld, ix, cy, iz, 7, Material.GRASS_BLOCK, Material.DIRT, Material.DEEPSLATE);
                MapBuilder.buildMedievalHouse(defaultWorld, ix - 3, cy + 1, iz - 3, 6, 6, Material.CALCITE, Material.OAK_LOG, Material.SPRUCE_STAIRS, Material.SPRUCE_SLAB);

                Location spawnLoc = new Location(defaultWorld, ix, cy + 2, iz);
                game.addIslandSpawn(spawnLoc);
                game.spawnCage(spawnLoc, Material.YELLOW_STAINED_GLASS);

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

            Location waitingLoc = new Location(defaultWorld, cx + 0.5, cy + 30, cz + 0.5);
            MapBuilder.buildWaitingLobby(defaultWorld, waitingLoc, Material.POLISHED_DEEPSLATE, Material.QUARTZ_PILLAR, Material.ORANGE_STAINED_GLASS);

            int[] floorYs = {cy, cy - 9, cy - 18, cy - 27};
            int[] radii = {18, 17, 16, 15};

            for (int f = 0; f < 4; f++) {
                int fy = floorYs[f];
                int r = radii[f];
                MapBuilder.fillCircle(defaultWorld, cx, fy, cz, r, topMats[i]);
                MapBuilder.fillCircle(defaultWorld, cx, fy - 1, cz, r, Material.TNT);
                MapBuilder.fillRing(defaultWorld, cx, fy + 1, cz, r - 1, r, Material.STONE_BRICK_SLAB);
            }

            int[] pd = {-20, 20};
            for (int px : pd) {
                for (int pz : pd) {
                    MapBuilder.buildCastleTower(defaultWorld, cx + px, cy - 30, cz + pz, 3, 45, Material.STONE_BRICKS, Material.SMOOTH_STONE, Material.DARK_OAK_STAIRS);
                }
            }

            TNTRunGame game = new TNTRunGame(plugin, id, "TNT Run: " + names[i], 3, 16, cy - 35);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));
            plugin.getGameManager().registerArena(game);
        }
    }

    // =========================================================================
    // 5. Global Parkour Maps (Min 3, Max 16)
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

            Location waitingLoc = new Location(defaultWorld, cx + 0.5, cy + 25, cz + 0.5);
            MapBuilder.buildWaitingLobby(defaultWorld, waitingLoc, Material.SMOOTH_STONE, Material.OAK_FENCE, Material.LIME_STAINED_GLASS);

            ParkourGame game = new ParkourGame(plugin, id, "Parkour: " + names[i], 3, 16, cy - 15);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));

            MapBuilder.fillCircle(defaultWorld, cx, cy, cz, 4, Material.POLISHED_ANDESITE);
            game.setStartLocation(new Location(defaultWorld, cx + 0.5, cy + 1, cz + 0.5, 0f, 0f));

            int curX = cx;
            int curZ = cz + 8;
            for (int cp = 1; cp <= 5; cp++) {
                for (int step = 1; step <= 3; step++) {
                    int sx = curX + (step % 2 == 0 ? 2 : -2);
                    int sz = curZ + step * 4;
                    defaultWorld.getBlockAt(sx, cy + (step % 2), sz).setType(Material.SPRUCE_PLANKS, false);
                }
                curZ += 18;
                MapBuilder.fillCircle(defaultWorld, curX, cy, curZ, 2, Material.STONE_BRICKS);
                defaultWorld.getBlockAt(curX, cy + 1, curZ).setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, false);
                game.addCheckpoint(new Location(defaultWorld, curX + 0.5, cy + 1, curZ + 0.5));
            }

            curZ += 16;
            MapBuilder.fillCircle(defaultWorld, curX, cy, curZ, 4, Material.QUARTZ_BLOCK);
            MapBuilder.buildPillar(defaultWorld, curX - 3, cy + 1, curZ, 6, Material.QUARTZ_PILLAR);
            MapBuilder.buildPillar(defaultWorld, curX + 3, cy + 1, curZ, 6, Material.QUARTZ_PILLAR);
            MapBuilder.fillBox(defaultWorld, curX - 3, cy + 7, curZ, curX + 3, cy + 7, curZ, Material.GOLD_BLOCK);
            defaultWorld.getBlockAt(curX, cy + 1, curZ).setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, false);
            game.setFinishLocation(new Location(defaultWorld, curX + 0.5, cy + 1, curZ + 0.5));

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

            Location waitingLoc = new Location(defaultWorld, cx + 0.5, cy + 25, cz + 0.5);
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

            Location waitingLoc = new Location(defaultWorld, cx + 0.5, cy + 25, cz + 0.5);
            MapBuilder.buildWaitingLobby(defaultWorld, waitingLoc, Material.STONE_BRICKS, Material.SMOOTH_STONE, Material.RED_STAINED_GLASS);

            BedWarsGame game = new BedWarsGame(plugin, id, "BedWars 1v1 Arena #" + i, 2, 2);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));

            // Red & Blue 1v1 islands
            int rx = cx - 25;
            int bx = cx + 25;
            MapBuilder.buildFloatingIslandNatural(defaultWorld, rx, cy, cz, 8, Material.STONE_BRICKS, Material.SMOOTH_STONE, Material.COBBLESTONE);
            MapBuilder.buildFloatingIslandNatural(defaultWorld, bx, cy, cz, 8, Material.STONE_BRICKS, Material.SMOOTH_STONE, Material.COBBLESTONE);
            MapBuilder.buildFloatingIslandNatural(defaultWorld, cx, cy, cz, 6, Material.STONE_BRICKS, Material.SMOOTH_STONE, Material.BEDROCK);

            worldAt(defaultWorld, rx - 2, cy + 1, cz, Material.RED_BED);
            worldAt(defaultWorld, rx + 2, cy + 1, cz, Material.IRON_BLOCK);
            worldAt(defaultWorld, bx + 2, cy + 1, cz, Material.BLUE_BED);
            worldAt(defaultWorld, bx - 2, cy + 1, cz, Material.IRON_BLOCK);
            worldAt(defaultWorld, cx, cy + 1, cz, Material.EMERALD_BLOCK);

            BedWarsTeam red = game.getTeam("RED");
            if (red != null) {
                red.setSpawnLocation(new Location(defaultWorld, rx + 0.5, cy + 1, cz + 0.5));
                red.setBedLocation(new Location(defaultWorld, rx - 2, cy + 1, cz));
            }
            BedWarsTeam blue = game.getTeam("BLUE");
            if (blue != null) {
                blue.setSpawnLocation(new Location(defaultWorld, bx + 0.5, cy + 1, cz + 0.5));
                blue.setBedLocation(new Location(defaultWorld, bx + 2, cy + 1, cz));
            }

            game.addSpawner(new BedWarsSpawner(new Location(defaultWorld, rx + 2.5, cy + 1.5, cz + 0.5), BedWarsSpawner.SpawnerType.IRON));
            game.addSpawner(new BedWarsSpawner(new Location(defaultWorld, bx - 1.5, cy + 1.5, cz + 0.5), BedWarsSpawner.SpawnerType.IRON));
            game.addSpawner(new BedWarsSpawner(new Location(defaultWorld, cx + 0.5, cy + 1.5, cz + 0.5), BedWarsSpawner.SpawnerType.EMERALD));

            plugin.getGameManager().registerArena(game);
        }

        // C. 1v1 Classic/Nodebuff PvP Duels (3 Arenas)
        for (int i = 1; i <= 3; i++) {
            int cx = -i * 250;
            int cy = 100;
            int cz = -950;
            String id = "duel-pvp-" + i;

            Location waitingLoc = new Location(defaultWorld, cx + 0.5, cy + 25, cz + 0.5);
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

            Location waitingLoc = new Location(defaultWorld, cx + 0.5, cy + 25, cz + 0.5);
            MapBuilder.buildWaitingLobby(defaultWorld, waitingLoc, Material.SMOOTH_STONE, Material.OAK_FENCE, Material.LIME_STAINED_GLASS);

            ParkourGame game = new ParkourGame(plugin, id, "Parkour 1v1 Arena #" + i, 2, 2, cy - 15);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));

            MapBuilder.fillCircle(defaultWorld, cx, cy, cz, 3, Material.POLISHED_ANDESITE);
            game.setStartLocation(new Location(defaultWorld, cx + 0.5, cy + 1, cz + 0.5, 0f, 0f));

            int curX = cx;
            int curZ = cz + 6;
            for (int cp = 1; cp <= 4; cp++) {
                for (int step = 1; step <= 3; step++) {
                    int sx = curX + (step % 2 == 0 ? 2 : -2);
                    int sz = curZ + step * 4;
                    defaultWorld.getBlockAt(sx, cy + (step % 2), sz).setType(Material.SPRUCE_PLANKS, false);
                }
                curZ += 16;
                MapBuilder.fillCircle(defaultWorld, curX, cy, curZ, 2, Material.STONE_BRICKS);
                defaultWorld.getBlockAt(curX, cy + 1, curZ).setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, false);
                game.addCheckpoint(new Location(defaultWorld, curX + 0.5, cy + 1, curZ + 0.5));
            }

            curZ += 14;
            MapBuilder.fillCircle(defaultWorld, curX, cy, curZ, 3, Material.QUARTZ_BLOCK);
            defaultWorld.getBlockAt(curX, cy + 1, curZ).setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, false);
            game.setFinishLocation(new Location(defaultWorld, curX + 0.5, cy + 1, curZ + 0.5));

            plugin.getGameManager().registerArena(game);
        }

        // E. 1v1 TNT Run Duels (3 Arenas)
        for (int i = 1; i <= 3; i++) {
            int cx = -i * 250;
            int cy = 100;
            int cz = -1550;
            String id = "duel-tnt-" + i;

            Location waitingLoc = new Location(defaultWorld, cx + 0.5, cy + 25, cz + 0.5);
            MapBuilder.buildWaitingLobby(defaultWorld, waitingLoc, Material.POLISHED_DEEPSLATE, Material.QUARTZ_PILLAR, Material.ORANGE_STAINED_GLASS);

            int[] floorYs = {cy, cy - 8, cy - 16};
            for (int fy : floorYs) {
                MapBuilder.fillCircle(defaultWorld, cx, fy, cz, 12, Material.SAND);
                MapBuilder.fillCircle(defaultWorld, cx, fy - 1, cz, 12, Material.TNT);
                MapBuilder.fillRing(defaultWorld, cx, fy + 1, cz, 11, 12, Material.STONE_BRICK_SLAB);
            }

            TNTRunGame game = new TNTRunGame(plugin, id, "TNT Run 1v1 Arena #" + i, 2, 2, cy - 25);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));
            plugin.getGameManager().registerArena(game);
        }

        // F. 1v1 Sumo Duels (3 Arenas)
        for (int i = 1; i <= 3; i++) {
            int cx = -i * 250;
            int cy = 100;
            int cz = -1850;
            String id = "duel-sumo-" + i;

            Location waitingLoc = new Location(defaultWorld, cx + 0.5, cy + 20, cz + 0.5);
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

        // G. 1v1 The Bridge Duels (Loads downloaded Bridge maps: Aquatica, Atlantis, Fortress)
        String[] bridgeMaps = {"Aquatica", "Atlantis", "Fortress"};
        for (int i = 0; i < bridgeMaps.length; i++) {
            String bName = bridgeMaps[i];
            String id = "duel-bridge-" + (i + 1);
            World bWorld = loadOrCreateWorld(bName, defaultWorld);

            int cx = (bWorld.equals(defaultWorld)) ? -(i + 1) * 250 : 0;
            int cy = 100;
            int cz = (bWorld.equals(defaultWorld)) ? -2150 : 0;

            Location waitingLoc = new Location(bWorld, cx + 0.5, cy + 20, cz + 0.5);
            MapBuilder.buildWaitingLobby(bWorld, waitingLoc, Material.QUARTZ_BLOCK, Material.POLISHED_BLACKSTONE, Material.RED_STAINED_GLASS);

            BridgeGame game = new BridgeGame(plugin, id, "The Bridge 1v1: " + bName, cy - 15);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));

            int rx = cx - 30;
            int bx = cx + 30;

            if (bWorld.equals(defaultWorld)) {
                MapBuilder.fillBox(bWorld, rx - 6, cy, cz - 4, rx + 3, cy + 6, cz + 4, Material.STONE_BRICKS);
                MapBuilder.fillBox(bWorld, rx - 5, cy + 1, cz - 3, rx + 2, cy + 5, cz + 3, Material.AIR);
                worldAt(bWorld, rx - 4, cy, cz, Material.WATER);
                worldAt(bWorld, rx - 4, cy - 1, cz, Material.RED_GLAZED_TERRACOTTA);

                MapBuilder.fillBox(bWorld, bx - 3, cy, cz - 4, bx + 6, cy + 6, cz + 4, Material.STONE_BRICKS);
                MapBuilder.fillBox(bWorld, bx - 2, cy + 1, cz - 3, bx + 5, cy + 5, cz + 3, Material.AIR);
                worldAt(bWorld, bx + 4, cy, cz, Material.WATER);
                worldAt(bWorld, bx + 4, cy - 1, cz, Material.BLUE_GLAZED_TERRACOTTA);

                for (int x = rx + 4; x < bx - 3; x++) {
                    bWorld.getBlockAt(x, cy, cz).setType(Material.SMOOTH_STONE, false);
                }
            }

            game.setRedGoal(new Location(bWorld, rx - 4, cy + 1, cz));
            game.setBlueGoal(new Location(bWorld, bx + 4, cy + 1, cz));
            game.setRedSpawn(new Location(bWorld, rx + 1.5, cy + 1, cz + 0.5, -90f, 0f));
            game.setBlueSpawn(new Location(bWorld, bx - 0.5, cy + 1, cz + 0.5, 90f, 0f));
            game.setRedCage(new Location(bWorld, rx + 1.5, cy + 4, cz + 0.5, -90f, 0f));
            game.setBlueCage(new Location(bWorld, bx - 0.5, cy + 4, cz + 0.5, 90f, 0f));

            plugin.getGameManager().registerArena(game);
        }
    }

    private void worldAt(World world, int x, int y, int z, Material mat) {
        world.getBlockAt(x, y, z).setType(mat, false);
    }
}
