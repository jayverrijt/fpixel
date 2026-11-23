package net.fpixel.lobby;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

public class HubGenerator {
    private final FPixelPlugin plugin;

    public HubGenerator(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Builds the Grand Palatial Courtyard Hub centered around (0, 64, 0).
     */
    public void generateCourtyardHub(World world) {
        if (world == null) return;
        plugin.getLogger().info("Generating Grand Palatial Courtyard Hub at (0, 64, 0)...");

        int cy = 64;

        // 1. Clear bounding box around courtyard to open sky
        for (int x = -45; x <= 45; x++) {
            for (int z = -45; z <= 45; z++) {
                for (int y = cy - 5; y <= cy + 30; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }

        // 2. Foundation and Palatial Floor Plazas
        for (int x = -36; x <= 36; x++) {
            for (int z = -36; z <= 36; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist > 36) continue;

                // Bedrock base under floor
                world.getBlockAt(x, cy - 2, z).setType(Material.BEDROCK, false);
                world.getBlockAt(x, cy - 1, z).setType(Material.POLISHED_DEEPSLATE, false);

                // Patterned floor
                if (dist <= 7) {
                    world.getBlockAt(x, cy, z).setType(Material.SMOOTH_QUARTZ, false);
                } else if (dist <= 15) {
                    if ((Math.abs(x) + Math.abs(z)) % 2 == 0) {
                        world.getBlockAt(x, cy, z).setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
                    } else {
                        world.getBlockAt(x, cy, z).setType(Material.POLISHED_ANDESITE, false);
                    }
                } else if (dist <= 26) {
                    if (Math.abs(x) <= 2 || Math.abs(z) <= 2 || Math.abs(x) == Math.abs(z)) {
                        // Grand cross and diagonal pathways
                        world.getBlockAt(x, cy, z).setType(Material.SMOOTH_QUARTZ, false);
                    } else {
                        world.getBlockAt(x, cy, z).setType(Material.STONE_BRICKS, false);
                    }
                } else {
                    world.getBlockAt(x, cy, z).setType(Material.PRISMARINE_BRICKS, false);
                }
            }
        }

        // 3. Central Grand Marble & Stone Fountain with Beacon Spire
        buildCentralFountain(world, 0, cy, 0);

        // 4. Lush Courtyard Gardens (Azaleas, Cherry Blossom Trees, Flowers)
        buildGardens(world, cy);

        // 5. Palatial Colonnades with Red Roofs around the Courtyard
        buildColonnades(world, cy);

        // 6. Build the 8 Game Mode Portal Arches
        buildGamePortals(world, cy);

        // 7. Set Spawn Location
        Location spawnLoc = new Location(world, 0.5, cy + 1, 0.5, 0f, 0f);
        plugin.getLobbyManager().setSpawn(spawnLoc);
        world.setSpawnLocation(0, cy + 1, 0);

        plugin.getLogger().info("Grand Palatial Courtyard Hub generated successfully!");
    }

    private void buildCentralFountain(World world, int cx, int cy, int cz) {
        // Outer basin ring (radius 5)
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist > 5.2) continue;

                if (dist >= 4.0) {
                    world.getBlockAt(cx + x, cy + 1, cz + z).setType(Material.QUARTZ_BRICKS, false);
                    world.getBlockAt(cx + x, cy + 2, cz + z).setType(Material.SMOOTH_QUARTZ_SLAB, false);
                } else {
                    // Pool basin
                    world.getBlockAt(cx + x, cy, cz + z).setType(Material.SEA_LANTERN, false);
                    world.getBlockAt(cx + x, cy + 1, cz + z).setType(Material.WATER, false);
                }
            }
        }

        // Center Pillar and Tiered Water Spill
        for (int y = 1; y <= 5; y++) {
            world.getBlockAt(cx, cy + y, cz).setType(Material.PRISMARINE, false);
        }
        world.getBlockAt(cx, cy + 6, cz).setType(Material.BEACON, false);
        world.getBlockAt(cx, cy + 7, cz).setType(Material.WATER, false);

        // Tiered Quartz basin at Y+3 (Radius 2)
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) == 2 && Math.abs(z) == 2) continue;
                if (x == 0 && z == 0) continue;
                world.getBlockAt(cx + x, cy + 3, cz + z).setType(Material.SMOOTH_QUARTZ_SLAB, false);
            }
        }
    }

    private void buildGardens(World world, int cy) {
        // 4 Ornamental Cherry Blossom Trees in the corners of inner courtyard
        int[][] treeSpots = { {-9, -9}, {9, -9}, {-9, 9}, {9, 9} };
        for (int[] pos : treeSpots) {
            int tx = pos[0];
            int tz = pos[1];

            // Tree trunk
            for (int y = 1; y <= 4; y++) {
                world.getBlockAt(tx, cy + y, tz).setType(Material.CHERRY_LOG, false);
            }

            // Leaves canopy
            for (int lx = -2; lx <= 2; lx++) {
                for (int lz = -2; lz <= 2; lz++) {
                    for (int ly = 3; ly <= 5; ly++) {
                        if (Math.abs(lx) == 2 && Math.abs(lz) == 2 && ly == 5) continue;
                        Block b = world.getBlockAt(tx + lx, cy + ly, tz + lz);
                        if (b.getType() == Material.AIR) {
                            b.setType(Material.CHERRY_LEAVES, false);
                        }
                    }
                }
            }

            // Surrounding flower planter
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    world.getBlockAt(tx + dx, cy + 1, tz + dz).setType(Material.FLOWERING_AZALEA, false);
                }
            }
        }

        // Perimeter Lanterns & Floral Borders
        for (int angle = 0; angle < 360; angle += 15) {
            if (angle % 45 == 0) continue; // Keep portal paths open!
            double rad = Math.toRadians(angle);
            int gx = (int) Math.round(23 * Math.cos(rad));
            int gz = (int) Math.round(23 * Math.sin(rad));

            world.getBlockAt(gx, cy + 1, gz).setType(Material.STONE_BRICK_WALL, false);
            world.getBlockAt(gx, cy + 2, gz).setType(Material.LANTERN, false);
        }
    }

    private void buildColonnades(World world, int cy) {
        // Grand Palatial Colonnades with Red Terracotta/Nether Brick roofs at radius 30
        for (int angle = 0; angle < 360; angle += 45) {
            double rad = Math.toRadians(angle + 22.5);
            int px = (int) Math.round(30 * Math.cos(rad));
            int pz = (int) Math.round(30 * Math.sin(rad));

            // Pillar
            for (int y = 1; y <= 6; y++) {
                world.getBlockAt(px, cy + y, pz).setType(Material.SMOOTH_QUARTZ, false);
            }
            // Capital
            world.getBlockAt(px, cy + 7, pz).setType(Material.QUARTZ_PILLAR, false);

            // Red Palatial Pavilion Roof (Nether Bricks / Red Terracotta)
            for (int rx = -2; rx <= 2; rx++) {
                for (int rz = -2; rz <= 2; rz++) {
                    world.getBlockAt(px + rx, cy + 8, pz + rz).setType(Material.RED_TERRACOTTA, false);
                    if (Math.abs(rx) <= 1 && Math.abs(rz) <= 1) {
                        world.getBlockAt(px + rx, cy + 9, pz + rz).setType(Material.NETHER_BRICKS, false);
                    }
                }
            }
            world.getBlockAt(px, cy + 10, pz).setType(Material.RED_NETHER_BRICK_SLAB, false);
            world.getBlockAt(px, cy + 6, pz).setType(Material.SEA_LANTERN, false);
        }
    }

    private void buildGamePortals(World world, int cy) {
        // The 8 Portal Directions around courtyard (Radius 25)
        // 1: North (0, -25) -> Player Worlds (Survival)
        // 2: North-East (18, -18) -> Parkour Studio
        // 3: East (25, 0) -> BedWars
        // 4: South-East (18, 18) -> SkyWars
        // 5: South (0, 25) -> TNT Run
        // 6: South-West (-18, 18) -> Murder Mystery
        // 7: West (-25, 0) -> Prop Hunt
        // 8: North-West (-18, -18) -> The Bridge & Sumo 1v1

        int[][] portalOffsets = {
                {0, -26, 0},    // 1. Player Worlds (Survival)
                {18, -18, 45},  // 2. Parkour Studio
                {26, 0, 90},    // 3. BedWars
                {18, 18, 135},  // 4. SkyWars
                {0, 26, 180},   // 5. TNT Run
                {-18, 18, 225}, // 6. Murder Mystery
                {-26, 0, 270},  // 7. Prop Hunt
                {-18, -18, 315} // 8. The Bridge & Sumo
        };

        for (int i = 0; i < portalOffsets.length; i++) {
            int px = portalOffsets[i][0];
            int pz = portalOffsets[i][1];
            boolean isNorthSouth = Math.abs(pz) > Math.abs(px);

            // Grand Portal Frame (5 wide, 6 high)
            for (int w = -2; w <= 2; w++) {
                for (int h = 1; h <= 6; h++) {
                    int bx = isNorthSouth ? (px + w) : px;
                    int bz = isNorthSouth ? pz : (pz + w);

                    if (w == -2 || w == 2 || h == 6) {
                        // Frame border
                        world.getBlockAt(bx, cy + h, bz).setType(Material.POLISHED_BLACKSTONE_BRICKS, false);
                    } else if (h == 1) {
                        // Base threshold
                        world.getBlockAt(bx, cy + h, bz).setType(Material.SEA_LANTERN, false);
                    } else {
                        // Active Glowing Portal Interior
                        world.getBlockAt(bx, cy + h, bz).setType(Material.NETHER_PORTAL, false);
                    }
                }
            }

            // Portal Arch Crest & Decorative Glow
            int topX = isNorthSouth ? px : px;
            int topZ = isNorthSouth ? pz : pz;
            world.getBlockAt(topX, cy + 7, topZ).setType(Material.GLOWSTONE, false);
            world.getBlockAt(topX, cy + 8, topZ).setType(Material.RED_NETHER_BRICK_SLAB, false);
        }

        // Register Portals in HubPortalManager
        if (plugin.getHubPortalManager() != null) {
            plugin.getHubPortalManager().setupPortals(world, cy);
        }
    }
}
