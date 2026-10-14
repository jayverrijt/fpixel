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

        // Dedicated pure void world for all minigames (Sumo, TNT Run, SkyWars, BedWars, Parkour, Bridge)
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
        plugin.getLogger().info("Rebuilding Grand Hub Archipelago: 5 Floating Islands, Marble Tiered Fountain, Blooming Cherry Blossoms, Royal Citadel, Roman Colosseum, Steampunk Airship Harbor, and Japanese Torii...");

        // 1. Purge all ground items and stray non-player entities in default world
        for (Entity e : world.getEntities()) {
            if (e instanceof Item || (e instanceof LivingEntity && !(e instanceof Player))) {
                e.remove();
            }
        }
        world.setGameRule(GameRule.DO_TILE_DROPS, false);
        world.setGameRule(GameRule.DO_ENTITY_DROPS, false);

        // 2. Clear all previous hub structures and vanilla generated terrain to open sky
        MapBuilder.clearVanillaTerrain(world, 0, 0, 140);
        for (int y = cy - 25; y <= cy + 50; y++) {
            MapBuilder.fillCircle(world, 0, y, 0, 85, Material.AIR);
        }

        // =========================================================================
        // 3. ISLAND 1: CENTRAL GRAND HARVEST PLAZA (Center: 0, cy, 0, Radius 16)
        // =========================================================================
        MapBuilder.buildFloatingIslandNatural(world, 0, cy, 0, 16, Material.POLISHED_DEEPSLATE, Material.STONE_BRICKS, Material.DEEPSLATE);

        // Concentric Plaza Rings
        MapBuilder.fillCircle(world, 0, cy, 0, 15, Material.POLISHED_DEEPSLATE);
        MapBuilder.fillRing(world, 0, cy, 0, 13, 14, Material.MUD_BRICKS);
        MapBuilder.fillRing(world, 0, cy, 0, 10, 12, Material.SPRUCE_PLANKS);
        MapBuilder.fillRing(world, 0, cy, 0, 7, 9, Material.SMOOTH_QUARTZ);
        MapBuilder.fillRing(world, 0, cy, 0, 5, 6, Material.POLISHED_ANDESITE);

        // Monumental Tiered Marble/Quartz Fountain with Skyward Beacon Spire
        MapBuilder.buildGrandFountain(world, 0, cy, 0, 5);
        world.getBlockAt(0, cy + 1, 0).setType(Material.BEACON, false);
        MapBuilder.buildCelestialRuneCrown(world, 0, cy, 0);

        // 4 Blooming Japanese Cherry Blossom Trees surrounding the fountain
        MapBuilder.buildCherryTree(world, -8, cy + 1, -8, 6);
        MapBuilder.buildCherryTree(world, 8, cy + 1, -8, 6);
        MapBuilder.buildCherryTree(world, -8, cy + 1, 8, 6);
        MapBuilder.buildCherryTree(world, 8, cy + 1, 8, 6);

        // Azalea bushes, flowering azaleas and lanterns around plaza edge
        for (int angle = 0; angle < 360; angle += 15) {
            double rad = Math.toRadians(angle);
            int bx = (int) Math.round(15 * Math.cos(rad));
            int bz = (int) Math.round(15 * Math.sin(rad));
            if (angle % 30 == 0) {
                world.getBlockAt(bx, cy + 1, bz).setType(Material.FLOWERING_AZALEA, false);
            } else {
                world.getBlockAt(bx, cy + 1, bz).setType(Material.STONE_BRICK_WALL, false);
                if (angle % 15 == 0 && (angle % 90 != 0)) {
                    world.getBlockAt(bx, cy + 2, bz).setType(Material.LANTERN, false);
                }
            }
        }

        // Directional Road Signposts around Plaza
        MapBuilder.placeDirectionalSign(world, 0, cy + 1, -13, "§a§l▲ NORTH ▲", "§2§lROYAL CITADEL", "§7Survival SMP Gateway");
        MapBuilder.placeDirectionalSign(world, -13, cy + 1, 0, "§c§l◀ WEST ◀", "§c§lMEDIEVAL VILLAGE", "§7BedWars & Bazaar");
        MapBuilder.placeDirectionalSign(world, 11, cy + 1, -8, "§6§l↗ NORTH-EAST", "§6§lROMAN COLOSSEUM", "§7TNT Run & Arena");
        MapBuilder.placeDirectionalSign(world, 11, cy + 1, 8, "§e§l↘ SOUTH-EAST", "§e§lAIRSHIP HARBOR", "§7Observatory & Zeppelins");
        MapBuilder.placeDirectionalSign(world, -11, cy + 1, 8, "§d§l↙ SOUTH-WEST", "§d§lTORII SHRINE", "§7Sumo 1v1 Dojo");
        MapBuilder.placeDirectionalSign(world, 0, cy + 1, 13, "§b§l▼ SOUTH ▼", "§b§lTOURNAMENT HALL", "§7Duels & SkyWars");

        // The 8 Super Launchpads Radiating from Central Plaza
        int[][] launchpadCoords = {
                {0, -11},   // North -> Royal Citadel
                {8, -8},    // North-East -> Roman Colosseum
                {11, 0},    // East -> SkyWars
                {8, 8},     // South-East -> Airship Harbor
                {0, 11},    // South -> Duels & PvP
                {-8, 8},    // South-West -> Sumo Torii
                {-11, 0},   // West -> Medieval Village
                {-8, -8}    // North-West -> Parkour
        };
        for (int[] lp : launchpadCoords) {
            int lx = lp[0], lz = lp[1];
            MapBuilder.fillRing(world, lx, cy, lz, 1, 2, Material.GOLD_BLOCK);
            world.getBlockAt(lx, cy, lz).setType(Material.EMERALD_BLOCK, false);
            world.getBlockAt(lx, cy + 1, lz).setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, false);
            world.getBlockAt(lx, cy - 1, lz).setType(Material.SEA_LANTERN, false);
        }

        // =========================================================================
        // 4. ISLAND 2: THE ROYAL CITADEL (North: 0, cy, -42, Radius 18)
        // =========================================================================
        // Grand Arched Stone Bridge connecting Central Plaza to Royal Citadel
        MapBuilder.buildArchedStoneBridge(world, 0, -15, 0, -25, cy, 5);

        // Floating Citadel Island Base
        MapBuilder.buildFloatingIslandNatural(world, 0, cy, -42, 18, Material.STONE_BRICKS, Material.COBBLESTONE, Material.DEEPSLATE);

        // Twin Soaring Citadel Bastion Towers
        MapBuilder.buildCastleTowerRound(world, -10, cy, -42, 4, 24, Material.STONE_BRICKS, Material.DARK_OAK_PLANKS);
        MapBuilder.buildCastleTowerRound(world, 10, cy, -42, 4, 24, Material.STONE_BRICKS, Material.DARK_OAK_PLANKS);

        // Monumental Vaulted Gatehouse Arch connecting the twin towers
        MapBuilder.fillBox(world, -6, cy + 1, -43, 6, cy + 12, -41, Material.STONE_BRICKS);
        MapBuilder.fillBox(world, -3, cy + 1, -43, 3, cy + 7, -41, Material.AIR); // arch tunnel
        MapBuilder.fillBox(world, -3, cy + 6, -42, 3, cy + 6, -42, Material.IRON_BARS); // portcullis top
        MapBuilder.fillBox(world, -6, cy + 13, -44, 6, cy + 13, -40, Material.POLISHED_ANDESITE); // battlements
        for (int gx = -6; gx <= 6; gx += 2) {
            world.getBlockAt(gx, cy + 14, -44).setType(Material.STONE_BRICK_WALL, false);
            world.getBlockAt(gx, cy + 14, -40).setType(Material.STONE_BRICK_WALL, false);
        }
        world.getBlockAt(-6, cy + 15, -44).setType(Material.LANTERN, false);
        world.getBlockAt(6, cy + 15, -44).setType(Material.LANTERN, false);

        // Radiant Skyward Cyan Beacon shooting from the citadel gateway
        MapBuilder.buildColoredBeacon(world, 0, cy, -42, Material.CYAN_STAINED_GLASS);

        // Survival SMP Portal Courtyard (Behind Gatehouse at 0, -48)
        int sgZ = -48;
        MapBuilder.buildPillar(world, -4, cy + 1, sgZ, 7, Material.STRIPPED_OAK_LOG);
        MapBuilder.buildPillar(world, 4, cy + 1, sgZ, 7, Material.STRIPPED_OAK_LOG);
        for (int px = -5; px <= 5; px++) {
            world.getBlockAt(px, cy + 8, sgZ).setType(Material.SPRUCE_STAIRS, false);
            world.getBlockAt(px, cy + 7, sgZ).setType(Material.DARK_OAK_PLANKS, false);
            world.getBlockAt(px, cy + 6, sgZ).setType(Material.STRIPPED_OAK_LOG, false);
        }
        world.getBlockAt(-4, cy + 5, sgZ).setType(Material.LANTERN, false);
        world.getBlockAt(4, cy + 5, sgZ).setType(Material.LANTERN, false);
        world.getBlockAt(0, cy + 1, sgZ).setType(Material.CAMPFIRE, false);
        world.getBlockAt(0, cy + 2, sgZ).setType(Material.COBBLESTONE_WALL, false);
        world.getBlockAt(-5, cy + 1, sgZ).setType(Material.BARREL, false);
        world.getBlockAt(5, cy + 1, sgZ).setType(Material.CHEST, false);
        MapBuilder.buildColoredBeacon(world, 0, cy, -54, Material.LIME_STAINED_GLASS);
        MapBuilder.buildGiantPickaxe(world, 0, cy + 15, -48);

        // =========================================================================
        // 5. ISLAND 3: MEDIEVAL VILLAGE & BAZAAR (West: -42, cy, 0, Radius 18)
        // =========================================================================
        // Arched Stone Bridge connecting Central Plaza to Medieval Village
        MapBuilder.buildArchedStoneBridge(world, -15, 0, -25, 0, cy, 5);

        // Floating Village Island Base
        MapBuilder.buildFloatingIslandNatural(world, -42, cy, 0, 18, Material.GRASS_BLOCK, Material.DIRT, Material.DEEPSLATE);

        // Half-Timbered Houses
        MapBuilder.buildAutumnTownHouse(world, -46, cy, -9, 6, 6, Material.MUD_BRICKS, Material.SPRUCE_STAIRS);
        MapBuilder.buildAutumnTownHouse(world, -46, cy, 7, 6, 6, Material.WHITE_TERRACOTTA, Material.DARK_OAK_STAIRS);
        MapBuilder.buildBedWarsBaseForge(world, -36, cy, -10, Material.STONE_BRICKS, Material.DEEPSLATE_BRICKS, Material.RED_TERRACOTTA);

        // Medieval Bazaar Market Stalls
        MapBuilder.buildMarketStall(world, -38, cy, -2, Material.RED_WOOL, Material.WHITE_WOOL);
        MapBuilder.buildMarketStall(world, -38, cy, 4, Material.YELLOW_WOOL, Material.ORANGE_WOOL);

        // BedWars Pavilion & Keep
        MapBuilder.buildGamePavilion(world, -40, cy, 0, 5, Material.STONE_BRICKS, Material.DEEPSLATE_BRICK_WALL, Material.RED_TERRACOTTA, Material.RED_GLAZED_TERRACOTTA);
        world.getBlockAt(-40, cy + 1, -3).setType(Material.RED_BED, false);
        world.getBlockAt(-40, cy + 1, 3).setType(Material.BLUE_BED, false);
        MapBuilder.buildColoredBeacon(world, -48, cy, 0, Material.RED_STAINED_GLASS);
        MapBuilder.buildGiantBed(world, -40, cy + 13, 0);

        // =========================================================================
        // 6. ISLAND 4: ROMAN COLOSSEUM & RUINS (North-East: 36, cy, -22, Radius 18)
        // =========================================================================
        // Arched Stone Bridge connecting Central Plaza to Roman Colosseum
        MapBuilder.buildArchedStoneBridge(world, 12, -10, 22, -15, cy, 4);

        // Floating Colosseum Island Base
        MapBuilder.buildFloatingIslandNatural(world, 36, cy, -22, 18, Material.SANDSTONE, Material.CUT_SANDSTONE, Material.DEEPSLATE);

        // 2-Tier Curved Roman Colosseum Arcade
        MapBuilder.buildRomanColosseumCurved(world, 36, cy + 1, -22, 13, -70, 110);

        // Colosseum Arena Interior
        MapBuilder.buildGamePavilion(world, 36, cy, -22, 5, Material.NETHER_BRICKS, Material.NETHER_BRICK_WALL, Material.RED_NETHER_BRICKS, Material.MAGMA_BLOCK);
        world.getBlockAt(39, cy + 1, -22).setType(Material.TNT, false);
        world.getBlockAt(36, cy + 1, -19).setType(Material.SOUL_CAMPFIRE, false);
        MapBuilder.buildColoredBeacon(world, 46, cy, -22, Material.ORANGE_STAINED_GLASS);
        MapBuilder.buildGiantTNT(world, 36, cy + 13, -22);

        // =========================================================================
        // 7. ISLAND 5: AIRSHIP HARBOR & OBSERVATORY (South-East: 32, cy, 28, Radius 18)
        // =========================================================================
        // Arched Wooden Bridge connecting Central Plaza to Airship Harbor
        MapBuilder.buildArchedWoodenBridge(world, 12, 11, 21, 18, cy, 4);

        // Floating Harbor Island Base
        MapBuilder.buildFloatingIslandNatural(world, 32, cy, 28, 18, Material.GRASS_BLOCK, Material.DIRT, Material.DEEPSLATE);

        // Celestial Glass-Domed Observatory
        MapBuilder.buildObservatory(world, 24, cy + 1, 20, 5);

        // Wooden Airship Docking Boardwalk Pier extending over the void
        for (int px = 28; px <= 44; px++) {
            for (int pz = 27; pz <= 30; pz++) {
                world.getBlockAt(px, cy, pz).setType(Material.SPRUCE_PLANKS, false);
                world.getBlockAt(px, cy - 1, pz).setType(Material.DARK_OAK_LOG, false);
            }
            world.getBlockAt(px, cy + 1, 27).setType(Material.SPRUCE_FENCE, false);
            if (px % 4 == 0) {
                world.getBlockAt(px, cy + 2, 27).setType(Material.LANTERN, false);
            }
        }

        // Moored Steampunk Zeppelin Airship moored alongside the pier
        MapBuilder.buildSteampunkAirship(world, 38, cy + 7, 34);

        // Gangway connecting pier to airship
        for (int z = 30; z <= 33; z++) {
            world.getBlockAt(36, cy + 1, z).setType(Material.SPRUCE_SLAB, false);
            world.getBlockAt(37, cy + 1, z).setType(Material.SPRUCE_SLAB, false);
        }

        // =========================================================================
        // 8. ISLAND 6: SACRED JAPANESE TORII SHRINE (South-West: -32, cy, 32, Radius 14)
        // =========================================================================
        // Arched Crimson Bridge connecting Central Plaza to Torii Island
        MapBuilder.buildArchedCrimsonBridge(world, -11, 11, -20, 20, cy, 4);

        // Floating Sacred Island Base
        MapBuilder.buildFloatingIslandNatural(world, -32, cy, 32, 14, Material.MOSS_BLOCK, Material.DIRT, Material.DEEPSLATE);

        // Crimson Torii Gate
        MapBuilder.buildToriiArch(world, -32, cy + 1, 26);

        // Blooming Cherry Blossom Trees with falling petals
        MapBuilder.buildCherryTree(world, -36, cy + 1, 35, 6);
        MapBuilder.buildCherryTree(world, -27, cy + 1, 36, 6);

        // Circular Golden Sumo Ring
        MapBuilder.fillCircle(world, -32, cy, 32, 5, Material.POLISHED_ANDESITE);
        MapBuilder.fillRing(world, -32, cy, 32, 4, 5, Material.GOLD_BLOCK);
        MapBuilder.buildColoredBeacon(world, -38, cy, 38, Material.WHITE_STAINED_GLASS);
        MapBuilder.buildGiantToriiEmblem(world, -32, cy + 13, 32);

        // =========================================================================
        // 9. ISLAND 7: SKYWARS CELESTIAL SANCTUARY (East: 48, cy, 0, Radius 12)
        // =========================================================================
        MapBuilder.buildFloatingIslandNatural(world, 48, cy, 0, 12, Material.SMOOTH_QUARTZ, Material.QUARTZ_BRICKS, Material.DEEPSLATE);
        // Arched crystal walkway connecting from Central Plaza
        for (int x = 16; x <= 36; x++) {
            world.getBlockAt(x, cy, 0).setType(Material.SMOOTH_QUARTZ, false);
            world.getBlockAt(x, cy - 1, 0).setType(Material.SEA_LANTERN, false);
            if (x % 4 == 0) {
                world.getBlockAt(x, cy + 1, -1).setType(Material.STONE_BRICK_WALL, false);
                world.getBlockAt(x, cy + 1, 1).setType(Material.STONE_BRICK_WALL, false);
                world.getBlockAt(x, cy + 2, -1).setType(Material.LANTERN, false);
                world.getBlockAt(x, cy + 2, 1).setType(Material.LANTERN, false);
            }
        }
        MapBuilder.buildGamePavilion(world, 48, cy, 0, 5, Material.SMOOTH_QUARTZ, Material.QUARTZ_PILLAR, Material.PURPUR_BLOCK, Material.AMETHYST_BLOCK);
        world.getBlockAt(48, cy + 1, 3).setType(Material.ENCHANTING_TABLE, false);
        world.getBlockAt(48, cy + 1, -3).setType(Material.ENDER_CHEST, false);
        MapBuilder.buildColoredBeacon(world, 54, cy, 0, Material.LIGHT_BLUE_STAINED_GLASS);
        MapBuilder.buildGiantEnderEye(world, 48, cy + 13, 0);

        // =========================================================================
        // 10. ISLAND 8: DUELS & PVP CITADEL (South: 0, cy, 38, Radius 14)
        // =========================================================================
        MapBuilder.buildArchedStoneBridge(world, 0, 15, 0, 25, cy, 5);
        MapBuilder.buildFloatingIslandNatural(world, 0, cy, 38, 14, Material.POLISHED_ANDESITE, Material.STONE_BRICKS, Material.DEEPSLATE);
        MapBuilder.buildGamePavilion(world, 0, cy, 38, 5, Material.POLISHED_ANDESITE, Material.STONE_BRICK_WALL, Material.DARK_OAK_PLANKS, Material.IRON_BLOCK);
        world.getBlockAt(-3, cy + 1, 38).setType(Material.HAY_BLOCK, false);
        world.getBlockAt(-3, cy + 2, 38).setType(Material.TARGET, false);
        world.getBlockAt(3, cy + 1, 38).setType(Material.SMITHING_TABLE, false);
        MapBuilder.buildColoredBeacon(world, 0, cy, 44, Material.MAGENTA_STAINED_GLASS);
        MapBuilder.buildGiantCrossedSwords(world, 0, cy + 13, 38);

        // =========================================================================
        // 11. ISLAND 9: PARKOUR AGILITY SPIRE (North-West: -28, cy, -28, Radius 12)
        // =========================================================================
        MapBuilder.buildArchedStoneBridge(world, -11, -11, -19, -19, cy, 4);
        MapBuilder.buildFloatingIslandNatural(world, -28, cy, -28, 12, Material.MOSS_BLOCK, Material.DIRT, Material.DEEPSLATE);
        MapBuilder.buildGamePavilion(world, -28, cy, -28, 5, Material.OAK_PLANKS, Material.OAK_LOG, Material.SPRUCE_STAIRS, Material.EMERALD_BLOCK);
        world.getBlockAt(-31, cy + 1, -28).setType(Material.SLIME_BLOCK, false);
        world.getBlockAt(-31, cy + 2, -28).setType(Material.GOLD_BLOCK, false);
        MapBuilder.buildColoredBeacon(world, -34, cy, -28, Material.GREEN_STAINED_GLASS);
        MapBuilder.buildGiantBoot(world, -28, cy + 13, -28);

        // 12. Spawn Point at Grand Central Plaza facing North (towards Fountain, Cherry Trees & Royal Citadel)
        Location hubSpawn = new Location(world, 0.5, cy + 1.5, 6.5, 0f, 0f);
        world.setSpawnLocation(0, cy + 2, 6);
        plugin.getLobbyManager().setSpawn(hubSpawn);

        // 13. Spawn All Interactive Gamemode & Lore NPCs with Floating Holograms!
        plugin.getHubNPCManager().spawnAllNPCs(world);

        // 14. Spawn All 7 Ancient Lore Relics & Secrets Displays!
        plugin.getHubLoreManager().spawnAllRelics(world);

        plugin.getLogger().info("Grand Hub Archipelago rebuild complete! 5+ Floating Islands, Marble Fountain, Cherry Blossoms, Royal Citadel, and Airship Harbor are live.");
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

    private void worldAt(World world, int x, int y, int z, Material mat) {
        world.getBlockAt(x, y, z).setType(mat, false);
    }
}
