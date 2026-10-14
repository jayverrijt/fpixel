package net.fpixel.core.map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Campfire;

import java.util.Random;

public class MapBuilder {
    private static final Random RANDOM = new Random(42);

    // ==========================================
    // 1. Primitive Shapes & Volume Builders
    // ==========================================

    public static void fillBox(World world, int x1, int y1, int z1, int x2, int y2, int z2, Material material) {
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    world.getBlockAt(x, y, z).setType(material, false);
                }
            }
        }
    }

    public static void fillCylinder(World world, int centerX, int centerY, int centerZ, int radius, int height, Material material) {
        int rSquared = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= rSquared) {
                    for (int y = 0; y < height; y++) {
                        world.getBlockAt(centerX + x, centerY + y, centerZ + z).setType(material, false);
                    }
                }
            }
        }
    }

    public static void fillCircle(World world, int centerX, int centerY, int centerZ, int radius, Material material) {
        fillCylinder(world, centerX, centerY, centerZ, radius, 1, material);
    }

    public static void fillRing(World world, int centerX, int centerY, int centerZ, int innerRadius, int outerRadius, Material material) {
        int innerSq = innerRadius * innerRadius;
        int outerSq = outerRadius * outerRadius;
        for (int x = -outerRadius; x <= outerRadius; x++) {
            for (int z = -outerRadius; z <= outerRadius; z++) {
                int distSq = x * x + z * z;
                if (distSq >= innerSq && distSq <= outerSq) {
                    world.getBlockAt(centerX + x, centerY, centerZ + z).setType(material, false);
                }
            }
        }
    }

    public static void buildPillar(World world, int x, int baseY, int z, int height, Material material) {
        for (int y = 0; y < height; y++) {
            world.getBlockAt(x, baseY + y, z).setType(material, false);
        }
    }

    // ==========================================
    // 2. Realistic Floating Islands (Natural 3D)
    // ==========================================

    public static void buildFloatingIslandNatural(World world, int centerX, int surfaceY, int centerZ, int radius,
                                                  Material topMat, Material middleMat, Material bottomMat) {
        int rSq = radius * radius;

        // 1. Surface with natural subtle elevation curves
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int distSq = x * x + z * z;
                if (distSq <= rSq) {
                    int elev = 0;
                    if (distSq < (radius * 0.4) * (radius * 0.4)) elev = 1;
                    world.getBlockAt(centerX + x, surfaceY + elev, centerZ + z).setType(topMat, false);
                    if (elev > 0) {
                        world.getBlockAt(centerX + x, surfaceY, centerZ + z).setType(middleMat, false);
                    }
                }
            }
        }

        // 2. Sub-surface layer (dirt/stone)
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int distSq = x * x + z * z;
                if (distSq <= rSq) {
                    world.getBlockAt(centerX + x, surfaceY - 1, centerZ + z).setType(middleMat, false);
                    if (distSq <= (radius - 1) * (radius - 1)) {
                        world.getBlockAt(centerX + x, surfaceY - 2, centerZ + z).setType(middleMat, false);
                    }
                }
            }
        }

        // 3. Jagged underside cone with organic stalactites
        int maxDepth = Math.max(6, (int) (radius * 1.2));
        for (int d = 3; d <= maxDepth; d++) {
            int currentY = surfaceY - d;
            double progress = (double) d / maxDepth;
            int currentRadius = (int) (radius * (1.0 - Math.pow(progress, 0.75)));
            if (currentRadius <= 0) currentRadius = 1;

            for (int x = -currentRadius; x <= currentRadius; x++) {
                for (int z = -currentRadius; z <= currentRadius; z++) {
                    int distSq = x * x + z * z;
                    // Introduce pseudo-random organic ruggedness
                    int jitter = ((x * 7 + z * 13 + d * 17) % 5 == 0) ? 1 : 0;
                    if (distSq <= (currentRadius + jitter) * (currentRadius + jitter)) {
                        Material mat = d > maxDepth * 0.6 ? bottomMat : middleMat;
                        world.getBlockAt(centerX + x, currentY, centerZ + z).setType(mat, false);
                    }
                }
            }
        }

        // 4. Central trailing root / stalactite point
        for (int extra = 1; extra <= 3; extra++) {
            world.getBlockAt(centerX, surfaceY - maxDepth - extra, centerZ).setType(bottomMat, false);
        }
    }

    // ==========================================
    // 3. Architectural Medieval Buildings
    // ==========================================

    /**
     * Builds a detailed Hypixel-style medieval cottage with A-frame roof, chimney with smoke, windows and shutters.
     */
    public static void buildMedievalHouse(World world, int x, int y, int z, int width, int length,
                                          Material wallMat, Material logMat, Material roofStairs, Material roofSlab) {
        // 1. Foundation: Cobblestone / Stone Bricks
        fillBox(world, x, y, z, x + width - 1, y, z + length - 1, Material.COBBLESTONE);
        fillBox(world, x + 1, y, z + 1, x + width - 2, y, z + length - 2, Material.OAK_PLANKS);

        int height = 5;

        // 2. Corner Log Pillars
        buildPillar(world, x, y + 1, z, height, logMat);
        buildPillar(world, x + width - 1, y + 1, z, height, logMat);
        buildPillar(world, x, y + 1, z + length - 1, height, logMat);
        buildPillar(world, x + width - 1, y + 1, z + length - 1, height, logMat);

        // 3. Walls
        for (int py = y + 1; py <= y + height; py++) {
            for (int px = x + 1; px < x + width - 1; px++) {
                world.getBlockAt(px, py, z).setType(wallMat, false);
                world.getBlockAt(px, py, z + length - 1).setType(wallMat, false);
            }
            for (int pz = z + 1; pz < z + length - 1; pz++) {
                world.getBlockAt(x, py, pz).setType(wallMat, false);
                world.getBlockAt(x + width - 1, py, pz).setType(wallMat, false);
            }
        }

        // 4. Front Doorway & Windows
        int midX = x + width / 2;
        world.getBlockAt(midX, y + 1, z).setType(Material.AIR, false);
        world.getBlockAt(midX, y + 2, z).setType(Material.AIR, false);
        world.getBlockAt(midX, y + 3, z - 1).setType(Material.LANTERN, false);

        // Glass windows on sides
        int midZ = z + length / 2;
        world.getBlockAt(x, y + 2, midZ).setType(Material.GLASS_PANE, false);
        world.getBlockAt(x + width - 1, y + 2, midZ).setType(Material.GLASS_PANE, false);
        world.getBlockAt(midX, y + 2, z + length - 1).setType(Material.GLASS_PANE, false);

        // 5. Gable Triangular Walls
        int roofBaseY = y + height + 1;
        int gableSteps = (width + 1) / 2;
        for (int step = 0; step < gableSteps; step++) {
            int currentY = roofBaseY + step;
            int startX = x + step;
            int endX = x + width - 1 - step;
            for (int px = startX; px <= endX; px++) {
                world.getBlockAt(px, currentY, z).setType(wallMat, false);
                world.getBlockAt(px, currentY, z + length - 1).setType(wallMat, false);
            }
        }

        // 6. Sloped A-Frame Roof (overhanging by 1 block on each end)
        for (int step = 0; step < gableSteps; step++) {
            int currentY = roofBaseY + step;
            int leftX = x + step - 1;
            int rightX = x + width - step;

            for (int pz = z - 1; pz <= z + length; pz++) {
                world.getBlockAt(leftX, currentY, pz).setType(roofStairs, false);
                world.getBlockAt(rightX, currentY, pz).setType(roofStairs, false);
            }
        }

        // Roof ridge top slab
        int ridgeY = roofBaseY + gableSteps;
        for (int pz = z - 1; pz <= z + length; pz++) {
            world.getBlockAt(midX, ridgeY, pz).setType(roofSlab, false);
        }

        // 7. Stone Chimney with real smoking campfire
        int chimneyX = x + 1;
        int chimneyZ = z + length - 1;
        for (int cy = y + 1; cy <= ridgeY + 1; cy++) {
            world.getBlockAt(chimneyX, cy, chimneyZ).setType(Material.COBBLESTONE, false);
        }
        Block campBlock = world.getBlockAt(chimneyX, ridgeY + 2, chimneyZ);
        campBlock.setType(Material.CAMPFIRE, false);
        world.getBlockAt(chimneyX, ridgeY + 3, chimneyZ).setType(Material.COBBLESTONE_WALL, false);

        // 8. Interior cozy furniture
        world.getBlockAt(x + 1, y + 1, z + 1).setType(Material.CRAFTING_TABLE, false);
        world.getBlockAt(x + width - 2, y + 1, z + 1).setType(Material.FURNACE, false);
        world.getBlockAt(x + width - 2, y + 1, z + length - 2).setType(Material.BARREL, false);
    }

    /**
     * Builds a soaring Hypixel-style medieval tower with machicolations, battlements, and a needle spire.
     */
    public static void buildCastleTower(World world, int x, int baseY, int z, int radius, int height,
                                        Material wallMat, Material trimMat, Material roofMat) {
        // 1. Solid circular/octagonal shaft
        for (int y = 0; y < height; y++) {
            fillCircle(world, x, baseY + y, z, radius, wallMat);
        }

        // 2. Machicolations: Widened overhang at top
        int topY = baseY + height;
        fillCircle(world, x, topY, z, radius + 1, trimMat);

        // 3. Battlements / Crenellations
        for (int angle = 0; angle < 360; angle += 45) {
            double rad = Math.toRadians(angle);
            int bx = (int) Math.round(x + (radius + 1) * Math.cos(rad));
            int bz = (int) Math.round(z + (radius + 1) * Math.sin(rad));
            world.getBlockAt(bx, topY + 1, bz).setType(wallMat, false);
        }

        // 4. Peaked Conical Witch-Hat Roof / Spire
        int roofRadius = radius;
        int currentY = topY + 1;
        while (roofRadius > 0) {
            fillCircle(world, x, currentY, z, roofRadius, roofMat);
            currentY += 2;
            roofRadius--;
        }

        // Spire pinnacle with gold and lantern
        world.getBlockAt(x, currentY, z).setType(Material.GOLD_BLOCK, false);
        world.getBlockAt(x, currentY + 1, z).setType(Material.LIGHTNING_ROD, false);

        // 5. Arrow slits & Lighting
        world.getBlockAt(x + radius, baseY + height / 2, z).setType(Material.IRON_BARS, false);
        world.getBlockAt(x - radius, baseY + height / 2, z).setType(Material.IRON_BARS, false);
        world.getBlockAt(x, baseY + height / 2, z + radius).setType(Material.IRON_BARS, false);
        world.getBlockAt(x, baseY + height / 2, z - radius).setType(Material.IRON_BARS, false);
        world.getBlockAt(x, topY, z).setType(Material.SEA_LANTERN, false);
    }

    /**
     * Builds an authentic Market Stall with striped canopy, crates, and lanterns.
     */
    public static void buildMarketStall(World world, int x, int y, int z, Material wool1, Material wool2) {
        // 4 Posts
        world.getBlockAt(x, y + 1, z).setType(Material.SPRUCE_FENCE, false);
        world.getBlockAt(x, y + 2, z).setType(Material.SPRUCE_FENCE, false);
        world.getBlockAt(x + 3, y + 1, z).setType(Material.SPRUCE_FENCE, false);
        world.getBlockAt(x + 3, y + 2, z).setType(Material.SPRUCE_FENCE, false);
        world.getBlockAt(x, y + 1, z + 2).setType(Material.SPRUCE_FENCE, false);
        world.getBlockAt(x, y + 2, z + 2).setType(Material.SPRUCE_FENCE, false);
        world.getBlockAt(x + 3, y + 1, z + 2).setType(Material.SPRUCE_FENCE, false);
        world.getBlockAt(x + 3, y + 2, z + 2).setType(Material.SPRUCE_FENCE, false);

        // Striped wool canopy
        for (int pz = z; pz <= z + 2; pz++) {
            world.getBlockAt(x, y + 3, pz).setType(wool1, false);
            world.getBlockAt(x + 1, y + 3, pz).setType(wool2, false);
            world.getBlockAt(x + 2, y + 3, pz).setType(wool1, false);
            world.getBlockAt(x + 3, y + 3, pz).setType(wool2, false);
        }

        // Counter with barrels and chest
        world.getBlockAt(x + 1, y + 1, z).setType(Material.BARREL, false);
        world.getBlockAt(x + 2, y + 1, z).setType(Material.CHEST, false);
        world.getBlockAt(x + 1, y + 2, z).setType(Material.LANTERN, false);
        world.getBlockAt(x + 2, y + 1, z + 1).setType(Material.HAY_BLOCK, false);
    }

    /**
     * Builds an organic decorative fantasy tree with curved trunk and cloud-like foliage.
     */
    public static void buildOrganicTree(World world, int x, int y, int z, Material logMat, Material leafMat, int height) {
        // Curved trunk
        for (int i = 0; i < height; i++) {
            int ox = (i > height / 2) ? 1 : 0;
            world.getBlockAt(x + ox, y + i, z).setType(logMat, false);
        }

        int topY = y + height;
        // Canopy spheres
        fillCircle(world, x + 1, topY, z, 3, leafMat);
        fillCircle(world, x + 1, topY + 1, z, 2, leafMat);
        fillCircle(world, x + 1, topY + 2, z, 1, leafMat);
        fillCircle(world, x + 1, topY - 1, z, 2, leafMat);

        // Hanging lantern
        world.getBlockAt(x + 1, topY - 2, z + 1).setType(Material.LANTERN, false);
    }

    /**
     * Builds a Grand Classical Fountain with multi-tiered water cascades and sea lantern lighting.
     */
    public static void buildGrandFountain(World world, int cx, int cy, int cz, int outerRadius) {
        // Outer stone basin
        fillCircle(world, cx, cy, cz, outerRadius, Material.SMOOTH_STONE);
        fillRing(world, cx, cy + 1, cz, outerRadius - 1, outerRadius, Material.STONE_BRICKS);
        fillCircle(world, cx, cy + 1, cz, outerRadius - 2, Material.WATER);

        // Center tiered pedestal
        buildPillar(world, cx, cy + 1, cz, 4, Material.QUARTZ_PILLAR);
        fillCircle(world, cx, cy + 4, cz, 2, Material.SMOOTH_QUARTZ);
        fillRing(world, cx, cy + 5, cz, 1, 2, Material.QUARTZ_STAIRS);

        // Top spire emitting water
        world.getBlockAt(cx, cy + 5, cz).setType(Material.SEA_LANTERN, false);
        world.getBlockAt(cx, cy + 6, cz).setType(Material.WATER, false);
    }

    /**
     * Builds a Waiting Lobby capsule/pavilion for players waiting for games to start.
     */
    public static void buildWaitingLobby(World world, Location center, Material floorMat, Material pillarMat, Material glassMat) {
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        // 1. Floating round base with deep underside
        fillCircle(world, cx, cy, cz, 7, floorMat);
        fillRing(world, cx, cy, cz, 6, 7, Material.GOLD_BLOCK);
        fillCircle(world, cx, cy - 1, cz, 6, Material.SMOOTH_STONE);
        fillCircle(world, cx, cy - 2, cz, 4, Material.DEEPSLATE);
        fillCircle(world, cx, cy - 3, cz, 2, Material.BEDROCK);

        // 2. Glass barrier walls around waiting area
        for (int y = 1; y <= 4; y++) {
            fillRing(world, cx, cy + y, cz, 6, 7, glassMat);
        }

        // 3. Four corner pillars with lighting
        int[] dx = {-5, 5, -5, 5};
        int[] dz = {-5, -5, 5, 5};
        for (int i = 0; i < 4; i++) {
            buildPillar(world, cx + dx[i], cy + 1, cz + dz[i], 5, pillarMat);
            world.getBlockAt(cx + dx[i], cy + 6, cz + dz[i]).setType(Material.SEA_LANTERN, false);
        }

        // 4. Center beacon / glowstone marker
        world.getBlockAt(cx, cy, cz).setType(Material.BEACON, false);
    }

    /**
     * Builds an authentic arched wooden bridge inspired by the user's survival world.
     */
    public static void buildArchedWoodenBridge(World world, int x1, int z1, int x2, int z2, int y, int width) {
        int dx = x2 - x1;
        int dz = z2 - z1;
        int steps = (int) Math.hypot(dx, dz);
        if (steps <= 0) return;

        double xStep = (double) dx / steps;
        double zStep = (double) dz / steps;

        for (int i = 0; i <= steps; i++) {
            double curX = x1 + i * xStep;
            double curZ = z1 + i * zStep;
            double progress = (double) i / steps; // 0 to 1
            int archLift = (int) Math.round(Math.sin(progress * Math.PI) * 2.0);
            int curY = y + archLift;

            int bx = (int) Math.round(curX);
            int bz = (int) Math.round(curZ);

            int halfW = width / 2;
            for (int w = -halfW; w <= halfW; w++) {
                int px = (Math.abs(dx) < Math.abs(dz)) ? bx + w : bx;
                int pz = (Math.abs(dx) >= Math.abs(dz)) ? bz + w : bz;

                world.getBlockAt(px, curY, pz).setType(Material.SPRUCE_PLANKS, false);
                world.getBlockAt(px, curY - 1, pz).setType(Material.OAK_LOG, false);

                // Railings on edge
                if (w == -halfW || w == halfW) {
                    world.getBlockAt(px, curY + 1, pz).setType(Material.OAK_FENCE, false);
                    if (i == 0 || i == steps || i == steps / 2) {
                        world.getBlockAt(px, curY + 2, pz).setType(Material.LANTERN, false);
                    }
                }
            }
        }
    }

    /**
     * Builds the Grand Survival Estate Manor inspired directly by the user's survival world:
     * Cobblestone base, stripped oak frame, spruce gable roof, glass windows, rooftop viewing terrace, smoking chimney.
     */
    public static void buildSurvivalEstateManor(World world, int x, int y, int z, int width, int length) {
        // Foundation
        fillBox(world, x, y, z, x + width - 1, y, z + length - 1, Material.COBBLESTONE);
        fillBox(world, x + 1, y, z + 1, x + width - 2, y, z + length - 2, Material.DARK_OAK_PLANKS);

        int height = 8; // 2 stories

        // Corner Pillars
        buildPillar(world, x, y + 1, z, height, Material.STRIPPED_OAK_LOG);
        buildPillar(world, x + width - 1, y + 1, z, height, Material.STRIPPED_OAK_LOG);
        buildPillar(world, x, y + 1, z + length - 1, height, Material.STRIPPED_OAK_LOG);
        buildPillar(world, x + width - 1, y + 1, z + length - 1, height, Material.STRIPPED_OAK_LOG);

        // Mid-pillars
        int midX = x + width / 2;
        int midZ = z + length / 2;
        buildPillar(world, midX, y + 1, z, height, Material.STRIPPED_OAK_LOG);
        buildPillar(world, midX, y + 1, z + length - 1, height, Material.STRIPPED_OAK_LOG);

        // First floor walls: Cobblestone & Stone Bricks
        for (int py = y + 1; py <= y + 4; py++) {
            for (int px = x + 1; px < x + width - 1; px++) {
                if (px != midX) {
                    world.getBlockAt(px, py, z).setType(Material.STONE_BRICKS, false);
                    world.getBlockAt(px, py, z + length - 1).setType(Material.STONE_BRICKS, false);
                }
            }
            for (int pz = z + 1; pz < z + length - 1; pz++) {
                world.getBlockAt(x, py, pz).setType(Material.COBBLESTONE, false);
                world.getBlockAt(x + width - 1, py, pz).setType(Material.COBBLESTONE, false);
            }
        }

        // Second floor divider
        fillBox(world, x + 1, y + 4, z + 1, x + width - 2, y + 4, z + length - 2, Material.SPRUCE_PLANKS);

        // Second floor walls: Stripped oak wood & spruce
        for (int py = y + 5; py <= y + height; py++) {
            for (int px = x + 1; px < x + width - 1; px++) {
                if (px != midX) {
                    world.getBlockAt(px, py, z).setType(Material.SPRUCE_PLANKS, false);
                    world.getBlockAt(px, py, z + length - 1).setType(Material.SPRUCE_PLANKS, false);
                }
            }
            for (int pz = z + 1; pz < z + length - 1; pz++) {
                world.getBlockAt(x, py, pz).setType(Material.SPRUCE_PLANKS, false);
                world.getBlockAt(x + width - 1, py, pz).setType(Material.SPRUCE_PLANKS, false);
            }
        }

        // Glass windows
        world.getBlockAt(x + 2, y + 2, z).setType(Material.GLASS_PANE, false);
        world.getBlockAt(x + width - 3, y + 2, z).setType(Material.GLASS_PANE, false);
        world.getBlockAt(x + 2, y + 6, z).setType(Material.GLASS_PANE, false);
        world.getBlockAt(x + width - 3, y + 6, z).setType(Material.GLASS_PANE, false);
        world.getBlockAt(x, y + 2, midZ).setType(Material.GLASS_PANE, false);
        world.getBlockAt(x, y + 6, midZ).setType(Material.GLASS_PANE, false);
        world.getBlockAt(x + width - 1, y + 2, midZ).setType(Material.GLASS_PANE, false);
        world.getBlockAt(x + width - 1, y + 6, midZ).setType(Material.GLASS_PANE, false);

        // Front grand entrance door
        world.getBlockAt(midX, y + 1, z).setType(Material.AIR, false);
        world.getBlockAt(midX, y + 2, z).setType(Material.AIR, false);
        world.getBlockAt(midX, y + 3, z - 1).setType(Material.LANTERN, false);

        // Roof: Multi-tier steep A-frame with spruce stairs
        int roofBaseY = y + height + 1;
        int gableSteps = (width + 1) / 2;
        for (int step = 0; step < gableSteps; step++) {
            int currentY = roofBaseY + step;
            int leftX = x + step - 1;
            int rightX = x + width - step;

            for (int pz = z - 1; pz <= z + length; pz++) {
                world.getBlockAt(leftX, currentY, pz).setType(Material.SPRUCE_STAIRS, false);
                world.getBlockAt(rightX, currentY, pz).setType(Material.SPRUCE_STAIRS, false);
            }
            for (int px = x + step; px <= x + width - 1 - step; px++) {
                world.getBlockAt(px, currentY, z).setType(Material.STRIPPED_OAK_LOG, false);
                world.getBlockAt(px, currentY, z + length - 1).setType(Material.STRIPPED_OAK_LOG, false);
            }
        }
        int ridgeY = roofBaseY + gableSteps;
        for (int pz = z - 1; pz <= z + length; pz++) {
            world.getBlockAt(midX, ridgeY, pz).setType(Material.SPRUCE_SLAB, false);
        }

        // Rooftop viewing balcony with glass railings (as in survival screenshot)
        int balcX = x + width - 1;
        for (int bz = z; bz < z + 4; bz++) {
            world.getBlockAt(balcX + 1, y + 5, bz).setType(Material.DARK_OAK_PLANKS, false);
            world.getBlockAt(balcX + 2, y + 5, bz).setType(Material.DARK_OAK_PLANKS, false);
            world.getBlockAt(balcX + 2, y + 6, bz).setType(Material.GLASS_PANE, false);
        }
        world.getBlockAt(balcX + 1, y + 6, z).setType(Material.GLASS_PANE, false);
        world.getBlockAt(balcX + 1, y + 6, z + 3).setType(Material.GLASS_PANE, false);

        // Stone Chimney with real smoking campfire
        int chimneyX = x + 1;
        int chimneyZ = z + length - 2;
        for (int cy = y + 1; cy <= ridgeY + 2; cy++) {
            world.getBlockAt(chimneyX, cy, chimneyZ).setType(Material.COBBLESTONE, false);
        }
        world.getBlockAt(chimneyX, ridgeY + 3, chimneyZ).setType(Material.CAMPFIRE, false);
        world.getBlockAt(chimneyX, ridgeY + 4, chimneyZ).setType(Material.COBBLESTONE_WALL, false);

        // Interior cozy decorations
        world.getBlockAt(x + 1, y + 1, z + 1).setType(Material.CRAFTING_TABLE, false);
        world.getBlockAt(x + 1, y + 1, z + 2).setType(Material.FURNACE, false);
        world.getBlockAt(x + width - 2, y + 1, z + 1).setType(Material.BOOKSHELF, false);
        world.getBlockAt(x + width - 2, y + 2, z + 1).setType(Material.LANTERN, false);
        world.getBlockAt(x + width - 2, y + 1, z + length - 2).setType(Material.BARREL, false);
    }

    /**
     * Builds a rustic wooden windmill with turning sail blades.
     */
    public static void buildWindmill(World world, int cx, int y, int cz) {
        fillCylinder(world, cx, y, cz, 4, 3, Material.COBBLESTONE);
        fillCylinder(world, cx, y + 3, cz, 3, 6, Material.SPRUCE_PLANKS);
        fillCylinder(world, cx, y + 9, cz, 2, 4, Material.DARK_OAK_PLANKS);
        fillCylinder(world, cx, y + 13, cz, 1, 2, Material.DARK_OAK_STAIRS);

        // Balcony
        fillRing(world, cx, y + 9, cz, 3, 4, Material.OAK_SLAB);
        for (int angle = 0; angle < 360; angle += 45) {
            double rad = Math.toRadians(angle);
            int fx = (int) Math.round(cx + 4 * Math.cos(rad));
            int fz = (int) Math.round(cz + 4 * Math.sin(rad));
            world.getBlockAt(fx, y + 10, fz).setType(Material.OAK_FENCE, false);
        }

        // Center axle for blades
        int axleY = y + 11;
        world.getBlockAt(cx, axleY, cz - 3).setType(Material.OAK_LOG, false);

        // 4 Sail blades made of fences and white wool
        for (int i = 1; i <= 6; i++) {
            // Up
            world.getBlockAt(cx, axleY + i, cz - 3).setType(Material.OAK_FENCE, false);
            world.getBlockAt(cx + 1, axleY + i, cz - 3).setType(Material.WHITE_WOOL, false);
            // Down
            world.getBlockAt(cx, axleY - i, cz - 3).setType(Material.OAK_FENCE, false);
            world.getBlockAt(cx - 1, axleY - i, cz - 3).setType(Material.WHITE_WOOL, false);
            // Right
            world.getBlockAt(cx + i, axleY, cz - 3).setType(Material.OAK_FENCE, false);
            world.getBlockAt(cx + i, axleY - 1, cz - 3).setType(Material.WHITE_WOOL, false);
            // Left
            world.getBlockAt(cx - i, axleY, cz - 3).setType(Material.OAK_FENCE, false);
            world.getBlockAt(cx - i, axleY + 1, cz - 3).setType(Material.WHITE_WOOL, false);
        }
    }

    /**
     * Carves a natural pathway blending dirt path, cobblestone, stone bricks, mossy stone bricks, and gravel.
     */
    public static void buildScenicPath(World world, int x1, int z1, int x2, int z2, int y, int width) {
        int dx = x2 - x1;
        int dz = z2 - z1;
        int steps = (int) Math.hypot(dx, dz);
        if (steps <= 0) return;

        double xStep = (double) dx / steps;
        double zStep = (double) dz / steps;

        Material[] pathMats = {Material.DIRT_PATH, Material.COBBLESTONE, Material.STONE_BRICKS, Material.MOSSY_STONE_BRICKS, Material.GRAVEL};

        for (int i = 0; i <= steps; i++) {
            int bx = (int) Math.round(x1 + i * xStep);
            int bz = (int) Math.round(z1 + i * zStep);

            int halfW = width / 2;
            for (int ox = -halfW; ox <= halfW; ox++) {
                for (int oz = -halfW; oz <= halfW; oz++) {
                    if (ox * ox + oz * oz <= halfW * halfW + 1) {
                        int px = bx + ox;
                        int pz = bz + oz;
                        int hash = Math.abs((px * 31 + pz * 17) % pathMats.length);
                        world.getBlockAt(px, y, pz).setType(pathMats[hash], false);
                    }
                }
            }
        }
    }

    public static void buildLamppost(World world, int x, int y, int z) {
        world.getBlockAt(x, y + 1, z).setType(Material.COBBLESTONE_WALL, false);
        world.getBlockAt(x, y + 2, z).setType(Material.OAK_FENCE, false);
        world.getBlockAt(x, y + 3, z).setType(Material.OAK_FENCE, false);
        world.getBlockAt(x, y + 4, z).setType(Material.LANTERN, false);
    }

    public static void buildParkBench(World world, int x, int y, int z) {
        world.getBlockAt(x, y + 1, z).setType(Material.SPRUCE_STAIRS, false);
        world.getBlockAt(x + 1, y + 1, z).setType(Material.SPRUCE_STAIRS, false);
        world.getBlockAt(x - 1, y + 1, z).setType(Material.SPRUCE_TRAPDOOR, false);
        world.getBlockAt(x + 2, y + 1, z).setType(Material.SPRUCE_TRAPDOOR, false);
    }

    public static void buildToriiArch(World world, int x, int y, int z) {
        buildPillar(world, x - 3, y + 1, z, 6, Material.STRIPPED_DARK_OAK_LOG);
        buildPillar(world, x + 3, y + 1, z, 6, Material.STRIPPED_DARK_OAK_LOG);

        for (int px = x - 4; px <= x + 4; px++) {
            world.getBlockAt(px, y + 5, z).setType(Material.DARK_OAK_PLANKS, false);
        }
        for (int px = x - 5; px <= x + 5; px++) {
            world.getBlockAt(px, y + 7, z).setType(Material.RED_TERRACOTTA, false);
        }
        world.getBlockAt(x - 5, y + 8, z).setType(Material.DARK_OAK_STAIRS, false);
        world.getBlockAt(x + 5, y + 8, z).setType(Material.DARK_OAK_STAIRS, false);

        world.getBlockAt(x - 3, y + 4, z).setType(Material.LANTERN, false);
        world.getBlockAt(x + 3, y + 4, z).setType(Material.LANTERN, false);
    }

    /**
     * Builds an ornate open-air Game Pavilion with vaulted canopy, columns, and an NPC dais.
     */
    public static void buildGamePavilion(World world, int cx, int y, int cz, int radius,
                                         Material floorMat, Material columnMat, Material roofMat, Material accentMat) {
        fillCircle(world, cx, y, cz, radius, floorMat);
        fillRing(world, cx, y, cz, radius - 1, radius, accentMat);

        int colHeight = 5;
        for (int angle = 0; angle < 360; angle += 45) {
            double rad = Math.toRadians(angle);
            int px = (int) Math.round(cx + (radius - 1) * Math.cos(rad));
            int pz = (int) Math.round(cz + (radius - 1) * Math.sin(rad));
            buildPillar(world, px, y + 1, pz, colHeight, columnMat);
            world.getBlockAt(px, y + colHeight + 1, pz).setType(Material.SEA_LANTERN, false);
        }

        int roofBaseY = y + colHeight + 1;
        fillCircle(world, cx, roofBaseY, cz, radius, roofMat);
        fillCircle(world, cx, roofBaseY + 1, cz, Math.max(1, radius - 1), roofMat);
        fillCircle(world, cx, roofBaseY + 2, cz, Math.max(1, radius - 2), roofMat);
        world.getBlockAt(cx, roofBaseY + 3, cz).setType(Material.LIGHTNING_ROD, false);

        // Center dais for NPC
        world.getBlockAt(cx, y + 1, cz).setType(accentMat, false);
    }
}
