package net.fpixel.core.map;

import net.fpixel.FPixelPlugin;
import net.fpixel.games.bedwars.BedWarsGame;
import net.fpixel.games.bedwars.BedWarsSpawner;
import net.fpixel.games.bedwars.BedWarsTeam;
import net.fpixel.games.bridge.BridgeGame;
import net.fpixel.games.parkour.ParkourGame;
import net.fpixel.games.skywars.SkyWarsGame;
import net.fpixel.games.sumo.SumoGame;
import net.fpixel.games.tntrun.TNTRunGame;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class WorldManager {
    private final FPixelPlugin plugin;

    public WorldManager(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    public void generateAllMaps(World world) {
        plugin.getLogger().info("Generating grand medieval castle hub and complete Hypixel-style minigame arenas...");

        // 1. Build Grand Medieval Castle Hub at (0, 100, 0)
        buildGrandHub(world);

        // 2. Build 10 BedWars Maps with Fortresses & Cottages
        buildAllBedWarsMaps(world);

        // 3. Build 10 SkyWars Maps with Player Islands & Citadels
        buildAllSkyWarsMaps(world);

        // 4. Build 10 The Bridge Duels Maps with Castle Bases
        buildAllBridgeMaps(world);

        // 5. Build 10 Parkour Duels Maps with Themed Obstacle Courses
        buildAllParkourMaps(world);

        // 6. Build 10 TNT Run Maps with Roman Colosseums
        buildAllTNTRunMaps(world);

        // 7. Build 10 Sumo Maps with Japanese Dojos & Temples
        buildAllSumoMaps(world);

        plugin.getLogger().info("Map generation complete! All minigames and arenas registered.");
    }

    // =========================================================================
    // 1. Grand Medieval Castle Hub
    // =========================================================================

    private void buildGrandHub(World world) {
        int cx = 0, cy = 100, cz = 0;

        // 1. Grand floating kingdom landmass (Radius 60, deep natural stalactite underside)
        MapBuilder.buildFloatingIslandNatural(world, cx, cy, cz, 60, Material.GRASS_BLOCK, Material.DIRT, Material.DEEPSLATE);

        // Cobblestone / Stone Brick paved avenues with accents
        MapBuilder.fillCircle(world, cx, cy, cz, 18, Material.STONE_BRICKS);
        MapBuilder.fillRing(world, cx, cy, cz, 16, 18, Material.POLISHED_ANDESITE);
        MapBuilder.fillRing(world, cx, cy, cz, 12, 13, Material.SEA_LANTERN);

        // Outer fortress battlements & barrier perimeter (Radius 62, Y=101 to Y=135)
        for (int y = 1; y <= 5; y++) {
            MapBuilder.fillRing(world, cx, cy + y, cz, 60, 61, Material.STONE_BRICKS);
        }
        for (int y = 6; y <= 35; y++) {
            MapBuilder.fillRing(world, cx, cy + y, cz, 61, 62, Material.BARRIER);
        }

        // 2. Grand Central Fountain
        MapBuilder.buildGrandFountain(world, cx, cy, cz, 7);

        // 3. Grand Twin-Tower Castle Keep at the North (Z = -35)
        int castleZ = -38;
        // Central Gatehouse
        MapBuilder.fillBox(world, -12, cy + 1, castleZ - 8, 12, cy + 14, castleZ + 4, Material.STONE_BRICKS);
        MapBuilder.fillBox(world, -4, cy + 1, castleZ - 9, 4, cy + 8, castleZ + 5, Material.AIR); // Grand archway
        MapBuilder.fillBox(world, -4, cy + 6, castleZ, 4, cy + 8, castleZ, Material.IRON_BARS); // Portcullis

        // Stained glass Rose Window above gate
        MapBuilder.fillCircle(world, 0, cy + 11, castleZ + 4, 3, Material.PURPLE_STAINED_GLASS);
        world.getBlockAt(0, cy + 11, castleZ + 4).setType(Material.BEACON, false);

        // Twin Soaring Castle Towers flanking gatehouse (Height 26 blocks)
        MapBuilder.buildCastleTower(world, -15, cy + 1, castleZ, 4, 24, Material.STONE_BRICKS, Material.SMOOTH_STONE, Material.DARK_OAK_STAIRS);
        MapBuilder.buildCastleTower(world, 15, cy + 1, castleZ, 4, 24, Material.STONE_BRICKS, Material.SMOOTH_STONE, Material.DARK_OAK_STAIRS);

        // 4. Village Square with 4 Detailed Medieval Houses
        // West House 1 & 2
        MapBuilder.buildMedievalHouse(world, -36, cy + 1, -10, 8, 10, Material.CALCITE, Material.STRIPPED_OAK_LOG, Material.SPRUCE_STAIRS, Material.SPRUCE_SLAB);
        MapBuilder.buildMedievalHouse(world, -36, cy + 1, 10, 8, 10, Material.WHITE_TERRACOTTA, Material.STRIPPED_DARK_OAK_LOG, Material.DARK_OAK_STAIRS, Material.DARK_OAK_SLAB);

        // East House 1 & 2
        MapBuilder.buildMedievalHouse(world, 28, cy + 1, -10, 8, 10, Material.STRIPPED_BIRCH_WOOD, Material.OAK_LOG, Material.SPRUCE_STAIRS, Material.SPRUCE_SLAB);
        MapBuilder.buildMedievalHouse(world, 28, cy + 1, 10, 8, 10, Material.MUD_BRICKS, Material.SPRUCE_LOG, Material.DARK_OAK_STAIRS, Material.DARK_OAK_SLAB);

        // 5. Market Stalls in the Village Plaza
        MapBuilder.buildMarketStall(world, -16, cy, 14, Material.RED_WOOL, Material.WHITE_WOOL);
        MapBuilder.buildMarketStall(world, -16, cy, 22, Material.BLUE_WOOL, Material.YELLOW_WOOL);
        MapBuilder.buildMarketStall(world, 12, cy, 14, Material.GREEN_WOOL, Material.WHITE_WOOL);
        MapBuilder.buildMarketStall(world, 12, cy, 22, Material.ORANGE_WOOL, Material.CYAN_WOOL);

        // 6. Parkour Ruins Tower at South-East (X=40, Z=35)
        MapBuilder.buildCastleTower(world, 40, cy + 1, 35, 5, 18, Material.MOSSY_STONE_BRICKS, Material.COBBLESTONE, Material.SPRUCE_STAIRS);
        // Parkour steps winding around the exterior
        int[][] parkourOffsets = {
                {40, 30, 2}, {45, 30, 4}, {48, 35, 7}, {45, 40, 10},
                {40, 42, 13}, {35, 40, 16}, {33, 35, 19}, {40, 35, 22}
        };
        for (int[] p : parkourOffsets) {
            world.getBlockAt(p[0], cy + p[2], p[1]).setType(Material.GOLD_BLOCK, false);
        }
        world.getBlockAt(40, cy + 23, 35).setType(Material.EMERALD_BLOCK, false);

        // 7. Organic Fantasy Trees & Street Lamps
        MapBuilder.buildOrganicTree(world, -20, cy + 1, -20, Material.OAK_LOG, Material.OAK_LEAVES, 7);
        MapBuilder.buildOrganicTree(world, 20, cy + 1, -20, Material.CHERRY_LOG, Material.CHERRY_LEAVES, 7);
        MapBuilder.buildOrganicTree(world, 0, cy + 1, 35, Material.OAK_LOG, Material.AZALEA_LEAVES, 8);

        // Four cardinal Launchpads
        int[][] launchpads = {{0, 22}, {0, -22}, {22, 0}, {-22, 0}};
        for (int[] lp : launchpads) {
            world.getBlockAt(cx + lp[0], cy, cz + lp[1]).setType(Material.EMERALD_BLOCK, false);
            world.getBlockAt(cx + lp[0], cy + 1, cz + lp[1]).setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, false);
        }

        // Set Hub Spawn right in front of the fountain facing the castle
        Location hubSpawn = new Location(world, 0.5, cy + 1, 14.5, 180f, 0f);
        plugin.getLobbyManager().setSpawn(hubSpawn);
    }

    // =========================================================================
    // 2. BedWars Maps (10 Maps with Fortresses & Cottages)
    // =========================================================================

    private void buildAllBedWarsMaps(World world) {
        String[] names = {
                "Kingdom Castle", "Sandstone Citadel", "Cyber Spire", "Frozen Bastion", "Nether Fortress",
                "Mushroom Village", "Clockwork Keep", "Candy Palace", "Ancient Ruins", "Atlantis Shrine"
        };

        Material[][] palettes = {
                {Material.STONE_BRICKS, Material.SMOOTH_STONE, Material.COBBLESTONE, Material.SPRUCE_STAIRS},
                {Material.CUT_SANDSTONE, Material.SANDSTONE, Material.TERRACOTTA, Material.SANDSTONE_STAIRS},
                {Material.BLACK_CONCRETE, Material.CYAN_CONCRETE, Material.OBSIDIAN, Material.DARK_PRISMARINE_STAIRS},
                {Material.PACKED_ICE, Material.SNOW_BLOCK, Material.SPRUCE_PLANKS, Material.SPRUCE_STAIRS},
                {Material.NETHER_BRICKS, Material.CRIMSON_PLANKS, Material.MAGMA_BLOCK, Material.NETHER_BRICK_STAIRS},
                {Material.MUSHROOM_STEM, Material.RED_MUSHROOM_BLOCK, Material.BROWN_MUSHROOM_BLOCK, Material.SPRUCE_STAIRS},
                {Material.CUT_COPPER, Material.DARK_OAK_PLANKS, Material.MUD_BRICKS, Material.DARK_OAK_STAIRS},
                {Material.PINK_CONCRETE, Material.WHITE_CONCRETE, Material.MAGENTA_CONCRETE, Material.PURPUR_STAIRS},
                {Material.MOSSY_STONE_BRICKS, Material.JUNGLE_PLANKS, Material.MOSSY_COBBLESTONE, Material.MOSSY_STONE_BRICK_STAIRS},
                {Material.PRISMARINE_BRICKS, Material.DARK_PRISMARINE, Material.PRISMARINE, Material.PRISMARINE_BRICK_STAIRS}
        };

        for (int i = 0; i < 10; i++) {
            int cx = (i + 1) * 350;
            int cy = 100;
            int cz = 0;
            String id = "bw-" + (i + 1);
            String name = names[i];
            Material[] pal = palettes[i];

            // 1. Waiting Room Capsule
            Location waitingLoc = new Location(world, cx + 0.5, cy + 35, cz + 0.5);
            MapBuilder.buildWaitingLobby(world, waitingLoc, pal[0], pal[1], Material.WHITE_STAINED_GLASS);

            // 2. Central Emerald Island with Ruined Castle / Monument
            MapBuilder.buildFloatingIslandNatural(world, cx, cy, cz, 16, pal[0], pal[1], pal[2]);
            MapBuilder.buildCastleTower(world, cx, cy + 1, cz, 4, 12, pal[0], pal[1], pal[3]);
            world.getBlockAt(cx, cy + 1, cz).setType(Material.EMERALD_BLOCK, false);

            // 3. Red Team Island with Red Fortified Cottage & Forge
            int rx = cx - 45;
            MapBuilder.buildFloatingIslandNatural(world, rx, cy, cz, 12, pal[0], pal[1], pal[2]);
            MapBuilder.buildMedievalHouse(world, rx - 4, cy + 1, cz - 4, 8, 8, pal[0], Material.OAK_LOG, pal[3], Material.SPRUCE_SLAB);
            world.getBlockAt(rx - 2, cy + 2, cz).setType(Material.RED_BED, false);
            world.getBlockAt(rx + 2, cy + 2, cz).setType(Material.IRON_BLOCK, false); // Forge
            world.getBlockAt(rx, cy + 2, cz + 2).setType(Material.CHEST, false);

            // 4. Blue Team Island with Blue Fortified Cottage & Forge
            int bx = cx + 45;
            MapBuilder.buildFloatingIslandNatural(world, bx, cy, cz, 12, pal[0], pal[1], pal[2]);
            MapBuilder.buildMedievalHouse(world, bx - 4, cy + 1, cz - 4, 8, 8, pal[0], Material.OAK_LOG, pal[3], Material.SPRUCE_SLAB);
            world.getBlockAt(bx + 2, cy + 2, cz).setType(Material.BLUE_BED, false);
            world.getBlockAt(bx - 2, cy + 2, cz).setType(Material.IRON_BLOCK, false); // Forge
            world.getBlockAt(bx, cy + 2, cz + 2).setType(Material.CHEST, false);

            // 5. Diamond Islands with Shrines
            MapBuilder.buildFloatingIslandNatural(world, cx, cy, cz - 40, 7, pal[0], pal[1], pal[2]);
            MapBuilder.buildPillar(world, cx - 2, cy + 1, cz - 40, 4, Material.QUARTZ_PILLAR);
            MapBuilder.buildPillar(world, cx + 2, cy + 1, cz - 40, 4, Material.QUARTZ_PILLAR);
            world.getBlockAt(cx, cy + 1, cz - 40).setType(Material.DIAMOND_BLOCK, false);

            MapBuilder.buildFloatingIslandNatural(world, cx, cy, cz + 40, 7, pal[0], pal[1], pal[2]);
            MapBuilder.buildPillar(world, cx - 2, cy + 1, cz + 40, 4, Material.QUARTZ_PILLAR);
            MapBuilder.buildPillar(world, cx + 2, cy + 1, cz + 40, 4, Material.QUARTZ_PILLAR);
            world.getBlockAt(cx, cy + 1, cz + 40).setType(Material.DIAMOND_BLOCK, false);

            // Register BedWars Arena
            BedWarsGame game = new BedWarsGame(plugin, id, "BedWars: " + name, 3, 8);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));

            BedWarsTeam red = game.getTeam("RED");
            if (red != null) {
                red.setSpawnLocation(new Location(world, rx + 0.5, cy + 2, cz + 0.5));
                red.setBedLocation(new Location(world, rx - 2, cy + 2, cz));
            }

            BedWarsTeam blue = game.getTeam("BLUE");
            if (blue != null) {
                blue.setSpawnLocation(new Location(world, bx + 0.5, cy + 2, cz + 0.5));
                blue.setBedLocation(new Location(world, bx + 2, cy + 2, cz));
            }

            game.addSpawner(new BedWarsSpawner(new Location(world, rx + 2.5, cy + 2.5, cz + 0.5), BedWarsSpawner.SpawnerType.IRON));
            game.addSpawner(new BedWarsSpawner(new Location(world, rx + 2.5, cy + 2.5, cz + 0.5), BedWarsSpawner.SpawnerType.GOLD));
            game.addSpawner(new BedWarsSpawner(new Location(world, bx - 1.5, cy + 2.5, cz + 0.5), BedWarsSpawner.SpawnerType.IRON));
            game.addSpawner(new BedWarsSpawner(new Location(world, bx - 1.5, cy + 2.5, cz + 0.5), BedWarsSpawner.SpawnerType.GOLD));
            game.addSpawner(new BedWarsSpawner(new Location(world, cx + 0.5, cy + 1.5, cz + 0.5), BedWarsSpawner.SpawnerType.EMERALD));
            game.addSpawner(new BedWarsSpawner(new Location(world, cx + 0.5, cy + 1.5, cz - 39.5), BedWarsSpawner.SpawnerType.DIAMOND));
            game.addSpawner(new BedWarsSpawner(new Location(world, cx + 0.5, cy + 1.5, cz + 40.5), BedWarsSpawner.SpawnerType.DIAMOND));

            plugin.getGameManager().registerArena(game);
        }
    }

    // =========================================================================
    // 3. SkyWars Maps (10 Maps with 8 Player Islands & Central Citadels)
    // =========================================================================

    private void buildAllSkyWarsMaps(World world) {
        String[] names = {
                "Aegis Castle", "Elven Canopy", "Dragon Roost", "Crystal Hollow", "Steampunk Air",
                "Celestial Peak", "Frostbite Crag", "Volcanic Ridge", "Oasis Haven", "Nether Siphon"
        };

        for (int i = 0; i < 10; i++) {
            int cx = (i + 1) * 350;
            int cy = 100;
            int cz = 350;
            String id = "sw-" + (i + 1);
            String name = names[i];

            Location waitingLoc = new Location(world, cx + 0.5, cy + 35, cz + 0.5);
            MapBuilder.buildWaitingLobby(world, waitingLoc, Material.POLISHED_ANDESITE, Material.STONE_BRICKS, Material.CYAN_STAINED_GLASS);

            SkyWarsGame game = new SkyWarsGame(plugin, id, "SkyWars: " + name, 3, 8, cy - 40);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));

            // Central Giant Citadel Island
            MapBuilder.buildFloatingIslandNatural(world, cx, cy, cz, 16, Material.GRASS_BLOCK, Material.DIRT, Material.DEEPSLATE);
            MapBuilder.buildCastleTower(world, cx, cy + 1, cz, 4, 14, Material.STONE_BRICKS, Material.SMOOTH_STONE, Material.DARK_OAK_STAIRS);

            // 4 Middle Chests around the central tower
            int[][] midChestOffsets = {{3, 3}, {-3, 3}, {3, -3}, {-3, -3}};
            for (int[] mco : midChestOffsets) {
                Location cLoc = new Location(world, cx + mco[0], cy + 2, cz + mco[1]);
                cLoc.getBlock().setType(Material.CHEST, false);
                game.addMidChest(cLoc);
            }

            // 8 Radial Player Starter Islands
            double rad = 42.0;
            for (int p = 0; p < 8; p++) {
                double angle = p * (Math.PI / 4.0);
                int ix = (int) Math.round(cx + rad * Math.cos(angle));
                int iz = (int) Math.round(cz + rad * Math.sin(angle));

                MapBuilder.buildFloatingIslandNatural(world, ix, cy, iz, 7, Material.GRASS_BLOCK, Material.DIRT, Material.DEEPSLATE);
                // Mini cottage/watchtower on each island
                MapBuilder.buildMedievalHouse(world, ix - 3, cy + 1, iz - 3, 6, 6, Material.CALCITE, Material.OAK_LOG, Material.SPRUCE_STAIRS, Material.SPRUCE_SLAB);

                Location spawnLoc = new Location(world, ix, cy + 2, iz);
                game.addIslandSpawn(spawnLoc);

                // Spawn Glass Cage
                game.spawnCage(spawnLoc, Material.YELLOW_STAINED_GLASS);

                // Island starter chests (2 chests per island)
                Location ic1 = new Location(world, ix - 2, cy + 2, iz + 1);
                Location ic2 = new Location(world, ix + 2, cy + 2, iz + 1);
                ic1.getBlock().setType(Material.CHEST, false);
                ic2.getBlock().setType(Material.CHEST, false);
                game.addIslandChest(ic1);
                game.addIslandChest(ic2);
            }

            plugin.getGameManager().registerArena(game);
        }
    }

    // =========================================================================
    // 4. The Bridge Duels Maps (10 Maps with Castle Bases & Bridges)
    // =========================================================================

    private void buildAllBridgeMaps(World world) {
        String[] names = {
                "Castle Bridge", "Nether Chasm", "Cyber Tunnel", "Frost Spire", "Desert Span",
                "Jungle Viaduct", "Mushroom Crossing", "Royal Walkway", "Temple Pass", "Atlantis Trench"
        };

        for (int i = 0; i < 10; i++) {
            int cx = (i + 1) * 350;
            int cy = 100;
            int cz = 700;
            String id = "bridge-" + (i + 1);
            String name = names[i];

            Location waitingLoc = new Location(world, cx + 0.5, cy + 25, cz + 0.5);
            MapBuilder.buildWaitingLobby(world, waitingLoc, Material.QUARTZ_BLOCK, Material.POLISHED_BLACKSTONE, Material.RED_STAINED_GLASS);

            BridgeGame game = new BridgeGame(plugin, id, "Bridge: " + name, cy - 15);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));

            // Red Castle Base (X = cx - 35)
            int rx = cx - 35;
            MapBuilder.fillBox(world, rx - 8, cy, cz - 6, rx + 4, cy + 8, cz + 6, Material.STONE_BRICKS);
            MapBuilder.fillBox(world, rx - 7, cy + 1, cz - 5, rx + 3, cy + 7, cz + 5, Material.AIR);
            // Red Goal Pit (jump into water to score)
            MapBuilder.fillBox(world, rx - 6, cy, cz - 1, rx - 4, cy, cz + 1, Material.WATER);
            world.getBlockAt(rx - 5, cy - 1, cz).setType(Material.RED_GLAZED_TERRACOTTA, false);
            Location redGoalLoc = new Location(world, rx - 5, cy + 1, cz);
            game.setRedGoal(redGoalLoc);

            // Blue Castle Base (X = cx + 35)
            int bx = cx + 35;
            MapBuilder.fillBox(world, bx - 4, cy, cz - 6, bx + 8, cy + 8, cz + 6, Material.STONE_BRICKS);
            MapBuilder.fillBox(world, bx - 3, cy + 1, cz - 5, bx + 7, cy + 7, cz + 5, Material.AIR);
            // Blue Goal Pit
            MapBuilder.fillBox(world, bx + 4, cy, cz - 1, bx + 6, cy, cz + 1, Material.WATER);
            world.getBlockAt(bx + 5, cy - 1, cz).setType(Material.BLUE_GLAZED_TERRACOTTA, false);
            Location blueGoalLoc = new Location(world, bx + 5, cy + 1, cz);
            game.setBlueGoal(blueGoalLoc);

            // The Bridge Connecting Both Bases (1 block wide with 3-block center platform)
            for (int x = rx + 5; x < bx - 4; x++) {
                world.getBlockAt(x, cy, cz).setType(Material.SMOOTH_STONE, false);
            }
            // Central Diamond Dueling Platform
            MapBuilder.fillCircle(world, cx, cy, cz, 3, Material.POLISHED_ANDESITE);
            world.getBlockAt(cx, cy, cz).setType(Material.SEA_LANTERN, false);

            // Team Spawns & Cages (elevated)
            Location rSpawn = new Location(world, rx + 1.5, cy + 1, cz + 0.5, -90f, 0f);
            Location bSpawn = new Location(world, bx - 0.5, cy + 1, cz + 0.5, 90f, 0f);
            game.setRedSpawn(rSpawn);
            game.setBlueSpawn(bSpawn);

            Location rCage = new Location(world, rx + 1.5, cy + 4, cz + 0.5, -90f, 0f);
            Location bCage = new Location(world, bx - 0.5, cy + 4, cz + 0.5, 90f, 0f);
            game.setRedCage(rCage);
            game.setBlueCage(bCage);

            plugin.getGameManager().registerArena(game);
        }
    }

    // =========================================================================
    // 5. Parkour Duels Maps (10 Themed Race Tracks)
    // =========================================================================

    private void buildAllParkourMaps(World world) {
        String[] names = {
                "Castle Ramparts", "Jungle Canopy", "Neon Matrix", "Glacier Leap", "Nether Run",
                "Skyline Rooftops", "Desert Pillars", "Crystal Cavern", "Mushroom Hop", "Temple of Speed"
        };

        for (int i = 0; i < 10; i++) {
            int cx = -(i + 1) * 350;
            int cy = 100;
            int cz = 350;
            String id = "pk-" + (i + 1);
            String name = names[i];

            Location waitingLoc = new Location(world, cx + 0.5, cy + 25, cz + 0.5);
            MapBuilder.buildWaitingLobby(world, waitingLoc, Material.SMOOTH_STONE, Material.OAK_FENCE, Material.LIME_STAINED_GLASS);

            ParkourGame game = new ParkourGame(plugin, id, "Parkour: " + name, 2, 4, cy - 15);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));

            // Start line platform
            MapBuilder.fillCircle(world, cx, cy, cz, 4, Material.POLISHED_ANDESITE);
            game.setStartLocation(new Location(world, cx + 0.5, cy + 1, cz + 0.5, 0f, 0f));

            // 6 Checkpoint Platforms with parkour jumps in between
            int curX = cx;
            int curZ = cz + 8;
            for (int cp = 1; cp <= 5; cp++) {
                // Individual parkour stepping stones
                for (int step = 1; step <= 3; step++) {
                    int sx = curX + (step % 2 == 0 ? 2 : -2);
                    int sz = curZ + step * 4;
                    world.getBlockAt(sx, cy + (step % 2), sz).setType(Material.SPRUCE_PLANKS, false);
                }

                curZ += 18;
                // Checkpoint Gold Platform
                MapBuilder.fillCircle(world, curX, cy, curZ, 2, Material.STONE_BRICKS);
                world.getBlockAt(curX, cy + 1, curZ).setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, false);
                game.addCheckpoint(new Location(world, curX + 0.5, cy + 1, curZ + 0.5));
            }

            // Grand Finish Line Arch
            curZ += 16;
            MapBuilder.fillCircle(world, curX, cy, curZ, 4, Material.QUARTZ_BLOCK);
            MapBuilder.buildPillar(world, curX - 3, cy + 1, curZ, 6, Material.QUARTZ_PILLAR);
            MapBuilder.buildPillar(world, curX + 3, cy + 1, curZ, 6, Material.QUARTZ_PILLAR);
            MapBuilder.fillBox(world, curX - 3, cy + 7, curZ, curX + 3, cy + 7, curZ, Material.GOLD_BLOCK);
            world.getBlockAt(curX, cy + 1, curZ).setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, false);
            game.setFinishLocation(new Location(world, curX + 0.5, cy + 1, curZ + 0.5));

            plugin.getGameManager().registerArena(game);
        }
    }

    // =========================================================================
    // 6. TNT Run Maps (10 Colosseum Arenas)
    // =========================================================================

    private void buildAllTNTRunMaps(World world) {
        String[] names = {
                "The Grand Colosseum", "Cyber Grid", "Frozen Abyss", "Lava Chamber", "Jungle Ruins",
                "Space Station", "The Factory", "Cotton Fall", "Sahara Storm", "Void Shrine"
        };

        Material[] topMats = {
                Material.SAND, Material.CYAN_CONCRETE, Material.SNOW_BLOCK, Material.RED_SAND, Material.GRAVEL,
                Material.PURPLE_CONCRETE, Material.MUD, Material.PINK_CONCRETE, Material.SAND, Material.WHITE_CONCRETE
        };

        for (int i = 0; i < 10; i++) {
            int cx = 0;
            int cy = 100;
            int cz = (i + 1) * 350;
            String id = "tnt-" + (i + 1);
            String name = names[i];
            Material topMat = topMats[i];

            Location waitingLoc = new Location(world, cx + 0.5, cy + 30, cz + 0.5);
            MapBuilder.buildWaitingLobby(world, waitingLoc, Material.POLISHED_DEEPSLATE, Material.QUARTZ_PILLAR, Material.ORANGE_STAINED_GLASS);

            // 4 descending floors of TopMat + TNT
            int[] floorYs = {cy, cy - 9, cy - 18, cy - 27};
            int[] radii = {18, 17, 16, 15};

            for (int f = 0; f < 4; f++) {
                int fy = floorYs[f];
                int rad = radii[f];

                MapBuilder.fillCircle(world, cx, fy, cz, rad, topMat);
                MapBuilder.fillCircle(world, cx, fy - 1, cz, rad, Material.TNT);
                MapBuilder.fillRing(world, cx, fy + 1, cz, rad - 1, rad, Material.STONE_BRICK_SLAB);
            }

            // Colosseum Arcade Pillars framing the arena
            int[] pd = {-20, 20};
            for (int px : pd) {
                for (int pz : pd) {
                    MapBuilder.buildCastleTower(world, cx + px, cy - 30, cz + pz, 3, 45, Material.STONE_BRICKS, Material.SMOOTH_STONE, Material.DARK_OAK_STAIRS);
                }
            }

            TNTRunGame game = new TNTRunGame(plugin, id, "TNT Run: " + name, 3, 16, cy - 35);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));
            plugin.getGameManager().registerArena(game);
        }
    }

    // =========================================================================
    // 7. Sumo Maps (10 Japanese Dojos & Temples)
    // =========================================================================

    private void buildAllSumoMaps(World world) {
        String[] names = {
                "Japanese Dojo", "Neon Ring", "Roman Pit", "Slippery Peak", "Fire Caldera",
                "Sky Kingdom", "Mushroom Cap", "Plank Arena", "Amethyst Spire", "Oasis Ring"
        };

        Material[] mats = {
                Material.RED_WOOL, Material.BLACKSTONE, Material.POLISHED_GRANITE, Material.PACKED_ICE, Material.BASALT,
                Material.SMOOTH_QUARTZ, Material.RED_MUSHROOM_BLOCK, Material.SPRUCE_PLANKS, Material.AMETHYST_BLOCK, Material.CUT_SANDSTONE
        };

        for (int i = 0; i < 10; i++) {
            int cx = -(i + 1) * 350;
            int cy = 100;
            int cz = 0;
            String id = "sumo-" + (i + 1);
            String name = names[i];
            Material mat = mats[i];

            Location waitingLoc = new Location(world, cx + 0.5, cy + 25, cz + 0.5);
            MapBuilder.buildWaitingLobby(world, waitingLoc, Material.SMOOTH_STONE, Material.DEEPSLATE_BRICK_WALL, Material.YELLOW_STAINED_GLASS);

            // Floating dueling arena with natural 3D underside
            MapBuilder.buildFloatingIslandNatural(world, cx, cy, cz, 7, mat, Material.DEEPSLATE, Material.BEDROCK);
            MapBuilder.fillRing(world, cx, cy, cz, 6, 7, Material.GOLD_BLOCK);

            // Japanese Torii Gate / Arch framing the arena
            MapBuilder.buildPillar(world, cx - 6, cy + 1, cz, 5, Material.CRIMSON_STEM);
            MapBuilder.buildPillar(world, cx + 6, cy + 1, cz, 5, Material.CRIMSON_STEM);
            MapBuilder.fillBox(world, cx - 7, cy + 5, cz, cx + 7, cy + 5, cz, Material.CRIMSON_PLANKS);
            MapBuilder.fillBox(world, cx - 8, cy + 6, cz, cx + 8, cy + 6, cz, Material.CRIMSON_SLAB);

            // Two spawn pedestals facing each other
            world.getBlockAt(cx - 4, cy, cz).setType(Material.SEA_LANTERN, false);
            world.getBlockAt(cx + 4, cy, cz).setType(Material.SEA_LANTERN, false);

            SumoGame game = new SumoGame(plugin, id, "Sumo: " + name, cy - 8);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));
            game.setSpawn1(new Location(world, cx - 4 + 0.5, cy + 1, cz + 0.5, -90f, 0f));
            game.setSpawn2(new Location(world, cx + 4 + 0.5, cy + 1, cz + 0.5, 90f, 0f));
            plugin.getGameManager().registerArena(game);
        }
    }
}
