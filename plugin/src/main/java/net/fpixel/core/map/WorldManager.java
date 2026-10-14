package net.fpixel.core.map;

import net.fpixel.FPixelPlugin;
import net.fpixel.games.bedwars.BedWarsGame;
import net.fpixel.games.bedwars.BedWarsSpawner;
import net.fpixel.games.bedwars.BedWarsTeam;
import net.fpixel.games.sumo.SumoGame;
import net.fpixel.games.tntrun.TNTRunGame;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class WorldManager {
    private final FPixelPlugin plugin;

    public WorldManager(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    public void generateAllMaps(World world) {
        plugin.getLogger().info("Generating grand hub and 30 custom minigame maps...");

        // 1. Build Grand Main Hub at (0, 100, 0)
        buildGrandHub(world);

        // 2. Build 10 BedWars Maps
        buildAllBedWarsMaps(world);

        // 3. Build 10 TNT Run Maps
        buildAllTNTRunMaps(world);

        // 4. Build 10 Sumo Maps
        buildAllSumoMaps(world);

        plugin.getLogger().info("Map generation complete! 30 unique minigame maps registered.");
    }

    private void buildGrandHub(World world) {
        int cx = 0, cy = 100, cz = 0;

        // Grand floating plaza (radius 22)
        MapBuilder.fillCircle(world, cx, cy, cz, 22, Material.QUARTZ_BLOCK);
        MapBuilder.fillRing(world, cx, cy, cz, 20, 22, Material.GOLD_BLOCK);
        MapBuilder.fillRing(world, cx, cy, cz, 12, 13, Material.SEA_LANTERN);
        MapBuilder.fillCircle(world, cx, cy, cz, 5, Material.CYAN_CONCRETE);
        MapBuilder.fillCircle(world, cx, cy, cz, 2, Material.GOLD_BLOCK);

        // Inverted island cone underneath
        MapBuilder.buildFloatingIsland(world, cx, cy - 1, cz, 22, Material.SMOOTH_STONE, Material.DEEPSLATE, Material.BEDROCK);

        // Four grand arched entrance portals / pillars
        int[][] dirs = {{16, 0}, {-16, 0}, {0, 16}, {0, -16}};
        for (int[] d : dirs) {
            MapBuilder.buildPillar(world, cx + d[0], cy + 1, cz + d[1], 8, Material.QUARTZ_PILLAR);
            world.getBlockAt(cx + d[0], cy + 9, cz + d[1]).setType(Material.SEA_LANTERN, false);

            // Launchpads on outer edges
            world.getBlockAt(cx + (int)(d[0] * 1.2), cy, cz + (int)(d[1] * 1.2)).setType(Material.EMERALD_BLOCK, false);
            world.getBlockAt(cx + (int)(d[0] * 1.2), cy + 1, cz + (int)(d[1] * 1.2)).setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, false);
        }

        // Set plugin lobby spawn at center
        Location hubSpawn = new Location(world, 0.5, 101.0, 0.5, 0f, 0f);
        plugin.getLobbyManager().setSpawn(hubSpawn);
    }

    private void buildAllBedWarsMaps(World world) {
        String[] names = {
                "Sky Castle", "Desert Dunes", "Neon Matrix", "Frost Peak", "Nether Citadel",
                "Fairy Hollow", "Clockwork Sky", "Sugar Rush", "Ancient Shrine", "Sunken Ocean"
        };

        Material[][] palettes = {
                {Material.STONE_BRICKS, Material.SMOOTH_STONE, Material.COBBLESTONE},
                {Material.CUT_SANDSTONE, Material.SANDSTONE, Material.TERRACOTTA},
                {Material.BLACK_CONCRETE, Material.CYAN_CONCRETE, Material.OBSIDIAN},
                {Material.PACKED_ICE, Material.SNOW_BLOCK, Material.SPRUCE_PLANKS},
                {Material.NETHER_BRICKS, Material.CRIMSON_PLANKS, Material.MAGMA_BLOCK},
                {Material.MYCELIUM, Material.RED_MUSHROOM_BLOCK, Material.BROWN_MUSHROOM_BLOCK},
                {Material.CUT_COPPER, Material.DARK_OAK_PLANKS, Material.MUD_BRICKS},
                {Material.PINK_CONCRETE, Material.WHITE_CONCRETE, Material.MAGENTA_CONCRETE},
                {Material.MOSSY_STONE_BRICKS, Material.JUNGLE_PLANKS, Material.MOSSY_COBBLESTONE},
                {Material.PRISMARINE_BRICKS, Material.DARK_PRISMARINE, Material.PRISMARINE}
        };

        for (int i = 0; i < 10; i++) {
            int cx = (i + 1) * 350;
            int cy = 100;
            int cz = 0;
            String id = "bw-" + (i + 1);
            String name = names[i];
            Material[] pal = palettes[i];

            // 1. Waiting room high above
            Location waitingLoc = new Location(world, cx + 0.5, cy + 35, cz + 0.5);
            MapBuilder.buildWaitingLobby(world, waitingLoc, pal[0], pal[1], Material.WHITE_STAINED_GLASS);

            // 2. Central Emerald Island
            MapBuilder.buildFloatingIsland(world, cx, cy, cz, 12, pal[0], pal[1], pal[2]);
            world.getBlockAt(cx, cy + 1, cz).setType(Material.EMERALD_BLOCK, false);

            // 3. Red Team Island
            int rx = cx - 40;
            MapBuilder.buildFloatingIsland(world, rx, cy, cz, 8, pal[0], pal[1], pal[2]);
            world.getBlockAt(rx - 2, cy + 1, cz).setType(Material.RED_BED, false);
            world.getBlockAt(rx + 2, cy + 1, cz).setType(Material.IRON_BLOCK, false); // Forge
            world.getBlockAt(rx, cy + 1, cz + 3).setType(Material.CHEST, false);

            // 4. Blue Team Island
            int bx = cx + 40;
            MapBuilder.buildFloatingIsland(world, bx, cy, cz, 8, pal[0], pal[1], pal[2]);
            world.getBlockAt(bx + 2, cy + 1, cz).setType(Material.BLUE_BED, false);
            world.getBlockAt(bx - 2, cy + 1, cz).setType(Material.IRON_BLOCK, false); // Forge
            world.getBlockAt(bx, cy + 1, cz + 3).setType(Material.CHEST, false);

            // 5. Diamond Islands (flanking)
            MapBuilder.buildFloatingIsland(world, cx, cy, cz - 35, 5, pal[0], pal[1], pal[2]);
            world.getBlockAt(cx, cy + 1, cz - 35).setType(Material.DIAMOND_BLOCK, false);

            MapBuilder.buildFloatingIsland(world, cx, cy, cz + 35, 5, pal[0], pal[1], pal[2]);
            world.getBlockAt(cx, cy + 1, cz + 35).setType(Material.DIAMOND_BLOCK, false);

            // Register BedWars Arena
            BedWarsGame game = new BedWarsGame(plugin, id, "BedWars: " + name, 2, 8);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));

            BedWarsTeam red = game.getTeam("RED");
            if (red != null) {
                red.setSpawnLocation(new Location(world, rx + 0.5, cy + 1, cz + 0.5));
                red.setBedLocation(new Location(world, rx - 2, cy + 1, cz));
            }

            BedWarsTeam blue = game.getTeam("BLUE");
            if (blue != null) {
                blue.setSpawnLocation(new Location(world, bx + 0.5, cy + 1, cz + 0.5));
                blue.setBedLocation(new Location(world, bx + 2, cy + 1, cz));
            }

            game.addSpawner(new BedWarsSpawner(new Location(world, rx + 2.5, cy + 1.5, cz + 0.5), BedWarsSpawner.SpawnerType.IRON));
            game.addSpawner(new BedWarsSpawner(new Location(world, rx + 2.5, cy + 1.5, cz + 0.5), BedWarsSpawner.SpawnerType.GOLD));
            game.addSpawner(new BedWarsSpawner(new Location(world, bx - 1.5, cy + 1.5, cz + 0.5), BedWarsSpawner.SpawnerType.IRON));
            game.addSpawner(new BedWarsSpawner(new Location(world, bx - 1.5, cy + 1.5, cz + 0.5), BedWarsSpawner.SpawnerType.GOLD));
            game.addSpawner(new BedWarsSpawner(new Location(world, cx + 0.5, cy + 1.5, cz + 0.5), BedWarsSpawner.SpawnerType.EMERALD));
            game.addSpawner(new BedWarsSpawner(new Location(world, cx + 0.5, cy + 1.5, cz - 34.5), BedWarsSpawner.SpawnerType.DIAMOND));
            game.addSpawner(new BedWarsSpawner(new Location(world, cx + 0.5, cy + 1.5, cz + 35.5), BedWarsSpawner.SpawnerType.DIAMOND));

            plugin.getGameManager().registerArena(game);
        }
    }

    private void buildAllTNTRunMaps(World world) {
        String[] names = {
                "The Colosseum", "Cyber Grid", "Frozen Abyss", "Lava Chamber", "Jungle Ruins",
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

            // 1. Waiting room high above
            Location waitingLoc = new Location(world, cx + 0.5, cy + 30, cz + 0.5);
            MapBuilder.buildWaitingLobby(world, waitingLoc, Material.POLISHED_DEEPSLATE, Material.QUARTZ_PILLAR, Material.ORANGE_STAINED_GLASS);

            // 2. Build 4 descending floors of TopMat + TNT
            int[] floorYs = {cy, cy - 9, cy - 18, cy - 27};
            int[] radii = {16, 15, 14, 13};

            for (int f = 0; f < 4; f++) {
                int fy = floorYs[f];
                int rad = radii[f];

                // Floor layer
                MapBuilder.fillCircle(world, cx, fy, cz, rad, topMat);
                // TNT layer directly beneath
                MapBuilder.fillCircle(world, cx, fy - 1, cz, rad, Material.TNT);
                // Border ring
                MapBuilder.fillRing(world, cx, fy + 1, cz, rad - 1, rad, Material.STONE_BRICK_SLAB);
            }

            // 3. Four corner pillars framing the arena
            int[] pd = {-18, 18};
            for (int px : pd) {
                for (int pz : pd) {
                    MapBuilder.buildPillar(world, cx + px, cy - 30, cz + pz, 40, Material.QUARTZ_PILLAR);
                    world.getBlockAt(cx + px, cy + 10, cz + pz).setType(Material.GLOWSTONE, false);
                }
            }

            // Register TNT Run Arena
            TNTRunGame game = new TNTRunGame(plugin, id, "TNT Run: " + name, 2, 16, cy - 35);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));
            plugin.getGameManager().registerArena(game);
        }
    }

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

            // 1. Waiting room elevated
            Location waitingLoc = new Location(world, cx + 0.5, cy + 25, cz + 0.5);
            MapBuilder.buildWaitingLobby(world, waitingLoc, Material.SMOOTH_STONE, Material.DEEPSLATE_BRICK_WALL, Material.YELLOW_STAINED_GLASS);

            // 2. Floating dueling circle (radius 6)
            MapBuilder.fillCircle(world, cx, cy, cz, 6, mat);
            MapBuilder.fillRing(world, cx, cy, cz, 5, 6, Material.GOLD_BLOCK);
            MapBuilder.fillCircle(world, cx, cy - 1, cz, 5, Material.DEEPSLATE);
            MapBuilder.fillCircle(world, cx, cy - 2, cz, 3, Material.BEDROCK);

            // 3. Two spawn pedestals facing each other
            world.getBlockAt(cx - 4, cy, cz).setType(Material.SEA_LANTERN, false);
            world.getBlockAt(cx + 4, cy, cz).setType(Material.SEA_LANTERN, false);

            // Register Sumo Arena
            SumoGame game = new SumoGame(plugin, id, "Sumo: " + name, cy - 8);
            game.setLobbySpawn(waitingLoc.clone().add(0, 1, 0));
            game.setSpectatorSpawn(waitingLoc.clone().add(0, 5, 0));
            game.setSpawn1(new Location(world, cx - 4 + 0.5, cy + 1, cz + 0.5, -90f, 0f));
            game.setSpawn2(new Location(world, cx + 4 + 0.5, cy + 1, cz + 0.5, 90f, 0f));
            plugin.getGameManager().registerArena(game);
        }
    }
}
