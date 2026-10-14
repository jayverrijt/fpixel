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
    // 1. Grand Castle Hub
    // =========================================================================

    private void buildGrandHub(World world) {
        int cx = 0, cy = 100, cz = 0;

        MapBuilder.buildFloatingIslandNatural(world, cx, cy, cz, 60, Material.GRASS_BLOCK, Material.DIRT, Material.DEEPSLATE);

        MapBuilder.fillCircle(world, cx, cy, cz, 18, Material.STONE_BRICKS);
        MapBuilder.fillRing(world, cx, cy, cz, 16, 18, Material.POLISHED_ANDESITE);
        MapBuilder.fillRing(world, cx, cy, cz, 12, 13, Material.SEA_LANTERN);

        for (int y = 1; y <= 5; y++) {
            MapBuilder.fillRing(world, cx, cy + y, cz, 60, 61, Material.STONE_BRICKS);
        }
        for (int y = 6; y <= 35; y++) {
            MapBuilder.fillRing(world, cx, cy + y, cz, 61, 62, Material.BARRIER);
        }

        MapBuilder.buildGrandFountain(world, cx, cy, cz, 7);

        int castleZ = -38;
        MapBuilder.fillBox(world, -12, cy + 1, castleZ - 8, 12, cy + 14, castleZ + 4, Material.STONE_BRICKS);
        MapBuilder.fillBox(world, -4, cy + 1, castleZ - 9, 4, cy + 8, castleZ + 5, Material.AIR);
        MapBuilder.fillBox(world, -4, cy + 6, castleZ, 4, cy + 8, castleZ, Material.IRON_BARS);
        MapBuilder.fillCircle(world, 0, cy + 11, castleZ + 4, 3, Material.PURPLE_STAINED_GLASS);
        world.getBlockAt(0, cy + 11, castleZ + 4).setType(Material.BEACON, false);

        MapBuilder.buildCastleTower(world, -15, cy + 1, castleZ, 4, 24, Material.STONE_BRICKS, Material.SMOOTH_STONE, Material.DARK_OAK_STAIRS);
        MapBuilder.buildCastleTower(world, 15, cy + 1, castleZ, 4, 24, Material.STONE_BRICKS, Material.SMOOTH_STONE, Material.DARK_OAK_STAIRS);

        MapBuilder.buildMedievalHouse(world, -36, cy + 1, -10, 8, 10, Material.CALCITE, Material.STRIPPED_OAK_LOG, Material.SPRUCE_STAIRS, Material.SPRUCE_SLAB);
        MapBuilder.buildMedievalHouse(world, -36, cy + 1, 10, 8, 10, Material.WHITE_TERRACOTTA, Material.STRIPPED_DARK_OAK_LOG, Material.DARK_OAK_STAIRS, Material.DARK_OAK_SLAB);
        MapBuilder.buildMedievalHouse(world, 28, cy + 1, -10, 8, 10, Material.STRIPPED_BIRCH_WOOD, Material.OAK_LOG, Material.SPRUCE_STAIRS, Material.SPRUCE_SLAB);
        MapBuilder.buildMedievalHouse(world, 28, cy + 1, 10, 8, 10, Material.MUD_BRICKS, Material.SPRUCE_LOG, Material.DARK_OAK_STAIRS, Material.DARK_OAK_SLAB);

        MapBuilder.buildMarketStall(world, -16, cy, 14, Material.RED_WOOL, Material.WHITE_WOOL);
        MapBuilder.buildMarketStall(world, -16, cy, 22, Material.BLUE_WOOL, Material.YELLOW_WOOL);
        MapBuilder.buildMarketStall(world, 12, cy, 14, Material.GREEN_WOOL, Material.WHITE_WOOL);
        MapBuilder.buildMarketStall(world, 12, cy, 22, Material.ORANGE_WOOL, Material.CYAN_WOOL);

        MapBuilder.buildCastleTower(world, 40, cy + 1, 35, 5, 18, Material.MOSSY_STONE_BRICKS, Material.COBBLESTONE, Material.SPRUCE_STAIRS);
        int[][] parkourOffsets = {
                {40, 30, 2}, {45, 30, 4}, {48, 35, 7}, {45, 40, 10},
                {40, 42, 13}, {35, 40, 16}, {33, 35, 19}, {40, 35, 22}
        };
        for (int[] p : parkourOffsets) {
            world.getBlockAt(p[0], cy + p[2], p[1]).setType(Material.GOLD_BLOCK, false);
        }
        world.getBlockAt(40, cy + 23, 35).setType(Material.EMERALD_BLOCK, false);

        MapBuilder.buildOrganicTree(world, -20, cy + 1, -20, Material.OAK_LOG, Material.OAK_LEAVES, 7);
        MapBuilder.buildOrganicTree(world, 20, cy + 1, -20, Material.CHERRY_LOG, Material.CHERRY_LEAVES, 7);
        MapBuilder.buildOrganicTree(world, 0, cy + 1, 35, Material.OAK_LOG, Material.AZALEA_LEAVES, 8);

        int[][] launchpads = {{0, 22}, {0, -22}, {22, 0}, {-22, 0}};
        for (int[] lp : launchpads) {
            world.getBlockAt(cx + lp[0], cy, cz + lp[1]).setType(Material.EMERALD_BLOCK, false);
            world.getBlockAt(cx + lp[0], cy + 1, cz + lp[1]).setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, false);
        }

        Location hubSpawn = new Location(world, 0.5, cy + 1, 14.5, 180f, 0f);
        plugin.getLobbyManager().setSpawn(hubSpawn);
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
