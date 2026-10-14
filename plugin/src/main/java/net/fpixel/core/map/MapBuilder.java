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

    public static void placeBed(World world, int footX, int footY, int footZ, org.bukkit.block.BlockFace facing, Material bedMat) {
        Block foot = world.getBlockAt(footX, footY, footZ);
        Block head = foot.getRelative(facing);

        foot.setType(bedMat, false);
        head.setType(bedMat, false);

        try {
            org.bukkit.block.data.type.Bed footData = (org.bukkit.block.data.type.Bed) foot.getBlockData();
            footData.setPart(org.bukkit.block.data.type.Bed.Part.FOOT);
            footData.setFacing(facing);
            foot.setBlockData(footData, false);

            org.bukkit.block.data.type.Bed headData = (org.bukkit.block.data.type.Bed) head.getBlockData();
            headData.setPart(org.bukkit.block.data.type.Bed.Part.HEAD);
            headData.setFacing(facing);
            head.setBlockData(headData, false);
        } catch (Throwable ignored) {}
    }

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

    public static void setLeaves(World world, int x, int y, int z, Material leafMat) {
        Block b = world.getBlockAt(x, y, z);
        b.setType(leafMat, false);
        if (b.getBlockData() instanceof org.bukkit.block.data.type.Leaves leaves) {
            leaves.setPersistent(true);
            leaves.setDistance(1);
            b.setBlockData(leaves, false);
        }
    }

    public static void fillLeavesCircle(World world, int cx, int cy, int cz, int radius, Material leafMat) {
        int rSq = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= rSq) {
                    setLeaves(world, cx + x, cy, cz + z, leafMat);
                }
            }
        }
    }

    /**
     * Builds an organic decorative fantasy tree with curved trunk and cloud-like foliage (persistent leaves).
     */
    public static void buildOrganicTree(World world, int x, int y, int z, Material logMat, Material leafMat, int height) {
        // Curved trunk
        for (int i = 0; i < height; i++) {
            int ox = (i > height / 2) ? 1 : 0;
            world.getBlockAt(x + ox, y + i, z).setType(logMat, false);
        }

        int topY = y + height;
        // Canopy spheres with persistent leaves
        fillLeavesCircle(world, x + 1, topY, z, 3, leafMat);
        fillLeavesCircle(world, x + 1, topY + 1, z, 2, leafMat);
        fillLeavesCircle(world, x + 1, topY + 2, z, 1, leafMat);
        fillLeavesCircle(world, x + 1, topY - 1, z, 2, leafMat);

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
     * Guaranteed multi-layer solid floor, high unbroken glass walls, and barrier ceiling.
     */
    public static void buildWaitingLobby(World world, Location center, Material floorMat, Material pillarMat, Material glassMat) {
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        // 1. Triple-layer unbreakable solid floating base (Radius 8)
        fillCircle(world, cx, cy, cz, 8, floorMat);
        fillRing(world, cx, cy, cz, 7, 8, Material.GOLD_BLOCK);
        fillCircle(world, cx, cy - 1, cz, 8, Material.SMOOTH_STONE);
        fillCircle(world, cx, cy - 2, cz, 8, Material.BEDROCK);
        fillCircle(world, cx, cy - 3, cz, 6, Material.DEEPSLATE);

        // 2. Unbroken 5-block high glass barrier perimeter walls (Rings 7 & 8)
        for (int y = 1; y <= 5; y++) {
            fillRing(world, cx, cy + y, cz, 7, 8, glassMat);
        }

        // 3. Four fortified corner pillars with warm lighting
        int[] dx = {-5, 5, -5, 5};
        int[] dz = {-5, -5, 5, 5};
        for (int i = 0; i < 4; i++) {
            buildPillar(world, cx + dx[i], cy + 1, cz + dz[i], 5, pillarMat);
            world.getBlockAt(cx + dx[i], cy + 6, cz + dz[i]).setType(Material.SEA_LANTERN, false);
        }

        // 4. Center beacon marker
        world.getBlockAt(cx, cy, cz).setType(Material.BEACON, false);

        // 5. Barrier ceiling at cy + 6 to prevent jumping or flying out
        fillCircle(world, cx, cy + 6, cz, 8, Material.BARRIER);
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

        // Interior cozy decorations & Grand Chronicler's Library
        // Clear first floor interior
        fillBox(world, x + 1, y + 1, z + 1, x + width - 2, y + 3, z + length - 2, Material.AIR);
        fillBox(world, x + 1, y, z + 1, x + width - 2, y, z + length - 2, Material.DARK_OAK_PLANKS);

        // Red carpet runner from front door into the grand hall
        for (int pz = z + 1; pz < z + length - 2; pz++) {
            world.getBlockAt(midX, y + 1, pz).setType(Material.RED_CARPET, false);
        }

        // West Wing: Grand Library (Bookshelves, Lectern holding relic 1 at x = -44, y + 1, z = -44)
        for (int pz = z + 2; pz <= z + 6; pz++) {
            world.getBlockAt(x + 1, y + 1, pz).setType(Material.BOOKSHELF, false);
            world.getBlockAt(x + 1, y + 2, pz).setType(Material.BOOKSHELF, false);
            world.getBlockAt(x + 1, y + 3, pz).setType(Material.BOOKSHELF, false);
        }
        for (int px = x + 1; px <= x + 3; px++) {
            world.getBlockAt(px, y + 1, z + 6).setType(Material.BOOKSHELF, false);
            world.getBlockAt(px, y + 2, z + 6).setType(Material.BOOKSHELF, false);
        }
        // Lectern at (-44, y + 1, -44) which is x + 2, y + 1, z + 4
        world.getBlockAt(x + 2, y + 1, z + 4).setType(Material.LECTERN, false);
        world.getBlockAt(x + 2, y + 1, z + 2).setType(Material.DARK_OAK_STAIRS, false);
        world.getBlockAt(x + 3, y + 1, z + 2).setType(Material.DARK_OAK_SLAB, false);
        world.getBlockAt(x + 3, y + 2, z + 2).setType(Material.LANTERN, false);

        // East Wing: Cozy Fireplace & Living Quarters
        world.getBlockAt(x + width - 2, y + 1, z + 2).setType(Material.CRAFTING_TABLE, false);
        world.getBlockAt(x + width - 2, y + 1, z + 3).setType(Material.FURNACE, false);
        world.getBlockAt(x + width - 2, y + 1, z + 4).setType(Material.BARREL, false);
        world.getBlockAt(x + width - 2, y + 2, z + 4).setType(Material.LANTERN, false);
        world.getBlockAt(x + width - 3, y + 1, z + 3).setType(Material.RED_BED, false);

        // Chandelier hanging in the center
        world.getBlockAt(midX, y + 4, midZ).setType(Material.IRON_CHAIN, false);
        world.getBlockAt(midX, y + 3, midZ).setType(Material.LANTERN, false);

        // Spruce stairs to 2nd floor
        for (int st = 0; st < 3; st++) {
            world.getBlockAt(midX + 2, y + 1 + st, z + length - 3 - st).setType(Material.SPRUCE_STAIRS, false);
        }
        world.getBlockAt(midX + 2, y + 4, z + length - 5).setType(Material.AIR, false);
        world.getBlockAt(midX + 2, y + 4, z + length - 4).setType(Material.AIR, false);
    }

    /**
     * Builds a rustic wooden windmill with climbable hollow interior and turning sail blades.
     */
    public static void buildWindmill(World world, int cx, int y, int cz) {
        fillCylinder(world, cx, y, cz, 4, 3, Material.COBBLESTONE);
        fillCylinder(world, cx, y + 3, cz, 3, 6, Material.SPRUCE_PLANKS);
        fillCylinder(world, cx, y + 9, cz, 2, 4, Material.DARK_OAK_PLANKS);
        fillCylinder(world, cx, y + 13, cz, 1, 2, Material.DARK_OAK_STAIRS);

        // Hollow interior so players can walk in and climb
        fillCylinder(world, cx, y + 1, cz, 2, 8, Material.AIR);
        fillCylinder(world, cx, y + 9, cz, 1, 4, Material.AIR);

        // Ground entrance doorway on South
        world.getBlockAt(cx, y + 1, cz + 4).setType(Material.AIR, false);
        world.getBlockAt(cx, y + 2, cz + 4).setType(Material.AIR, false);
        world.getBlockAt(cx, y + 1, cz + 3).setType(Material.AIR, false);
        world.getBlockAt(cx, y + 2, cz + 3).setType(Material.AIR, false);
        world.getBlockAt(cx, y + 3, cz + 4).setType(Material.LANTERN, false);

        // Interior ladders leading up to the high loft
        for (int ly = y + 1; ly <= y + 13; ly++) {
            world.getBlockAt(cx, ly, cz + 1).setType(Material.LADDER, false);
        }

        // Loft floor at y + 13 & observation table for relic 4
        world.getBlockAt(cx, y + 13, cz).setType(Material.SPRUCE_TRAPDOOR, false);
        world.getBlockAt(cx - 1, y + 13, cz).setType(Material.DARK_OAK_PLANKS, false);
        world.getBlockAt(cx + 1, y + 13, cz).setType(Material.DARK_OAK_PLANKS, false);
        world.getBlockAt(cx, y + 13, cz - 1).setType(Material.DARK_OAK_PLANKS, false);
        world.getBlockAt(cx, y + 14, cz).setType(Material.AIR, false); // Relic 4 sits here!
        world.getBlockAt(cx, y + 14, cz - 1).setType(Material.LANTERN, false);

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
     * Builds the magnificent floating Celestial Rune Crown suspended above the central plaza beacon.
     */
    public static void buildCelestialRuneCrown(World world, int cx, int cy, int cz) {
        // Outer Rune Ring at cy + 9
        fillRing(world, cx, cy + 9, cz, 5, 7, Material.POLISHED_DEEPSLATE);
        for (int angle = 0; angle < 360; angle += 30) {
            double rad = Math.toRadians(angle);
            int rx = (int) Math.round(cx + 6 * Math.cos(rad));
            int rz = (int) Math.round(cz + 6 * Math.sin(rad));
            world.getBlockAt(rx, cy + 9, rz).setType(Material.SEA_LANTERN, false);
            if (angle % 60 == 0) {
                world.getBlockAt(rx, cy + 10, rz).setType(Material.GILDED_BLACKSTONE, false);
                world.getBlockAt(rx, cy + 8, rz).setType(Material.IRON_CHAIN, false);
                world.getBlockAt(rx, cy + 7, rz).setType(Material.SOUL_LANTERN, false);
            }
        }

        // Inner Rune Ring at cy + 11
        fillRing(world, cx, cy + 11, cz, 3, 4, Material.AMETHYST_BLOCK);
        for (int angle = 15; angle < 360; angle += 45) {
            double rad = Math.toRadians(angle);
            int rx = (int) Math.round(cx + 3.5 * Math.cos(rad));
            int rz = (int) Math.round(cz + 3.5 * Math.sin(rad));
            world.getBlockAt(rx, cy + 11, rz).setType(Material.CRYING_OBSIDIAN, false);
        }

        // Floating Center Celestial Core at cy + 12
        world.getBlockAt(cx, cy + 12, cz).setType(Material.BEACON, false);
        world.getBlockAt(cx, cy + 13, cz).setType(Material.CYAN_STAINED_GLASS, false);
        world.getBlockAt(cx, cy + 11, cz).setType(Material.SEA_LANTERN, false);

        // 4 Spires pointing upward
        int[][] spires = {{0, 6}, {0, -6}, {6, 0}, {-6, 0}};
        for (int[] sp : spires) {
            int sx = cx + sp[0], sz = cz + sp[1];
            world.getBlockAt(sx, cy + 10, sz).setType(Material.POLISHED_DEEPSLATE_WALL, false);
            world.getBlockAt(sx, cy + 11, sz).setType(Material.POLISHED_DEEPSLATE_WALL, false);
            world.getBlockAt(sx, cy + 12, sz).setType(Material.END_ROD, false);
        }
    }

    /**
     * Builds a cascading Mountain Waterfall and secret Hidden Crystal Grotto cavern.
     */
    public static void buildCrystalGrottoAndWaterfall(World world, int gx, int gz, int cy) {
        // 1. Crystal Grotto Cavern carved into the mountain (-66 to -58 in X, -16 to -8 in Z)
        fillBox(world, gx - 5, cy + 1, gz - 4, gx + 2, cy + 6, gz + 4, Material.AIR);

        // Cavern Floor
        fillBox(world, gx - 5, cy, gz - 4, gx + 2, cy, gz + 4, Material.POLISHED_BLACKSTONE);
        for (int x = gx - 5; x <= gx + 2; x++) {
            for (int z = gz - 4; z <= gz + 4; z++) {
                if ((x + z) % 3 == 0) {
                    world.getBlockAt(x, cy, z).setType(Material.MOSSY_STONE_BRICKS, false);
                }
            }
        }

        // Cavern Ceiling & Walls of Amethyst, Calcite & Weeping Vines
        for (int x = gx - 6; x <= gx + 3; x++) {
            for (int z = gz - 5; z <= gz + 5; z++) {
                world.getBlockAt(x, cy + 7, z).setType(Material.CALCITE, false);
                if ((x * 7 + z * 13) % 4 == 0) {
                    world.getBlockAt(x, cy + 6, z).setType(Material.WEEPING_VINES, false);
                }
            }
        }
        for (int y = cy + 1; y <= cy + 6; y++) {
            for (int z = gz - 4; z <= gz + 4; z++) {
                world.getBlockAt(gx - 6, y, z).setType(Material.AMETHYST_BLOCK, false);
            }
            for (int x = gx - 5; x <= gx + 2; x++) {
                world.getBlockAt(x, y, gz - 5).setType(Material.BUDDING_AMETHYST, false);
                world.getBlockAt(x, y, gz + 5).setType(Material.AMETHYST_BLOCK, false);
            }
        }

        // Ancient Altar in the center of Grotto
        world.getBlockAt(gx - 2, cy + 1, gz).setType(Material.ENCHANTING_TABLE, false);
        world.getBlockAt(gx - 2, cy + 1, gz - 1).setType(Material.BOOKSHELF, false);
        world.getBlockAt(gx - 2, cy + 1, gz + 1).setType(Material.BOOKSHELF, false);
        world.getBlockAt(gx - 2, cy + 2, gz - 1).setType(Material.SOUL_LANTERN, false);
        world.getBlockAt(gx - 2, cy + 2, gz + 1).setType(Material.SOUL_LANTERN, false);
        world.getBlockAt(gx - 4, cy + 1, gz).setType(Material.CHEST, false);

        // 2. Cascading Waterfall Curtains spilling across the cavern front (at gx + 3, z: gz - 2 to gz + 2)
        for (int y = cy + 16; y >= cy; y--) {
            for (int z = gz - 2; z <= gz + 2; z++) {
                world.getBlockAt(gx + 3, y, z).setType(Material.WATER, false);
            }
        }
        fillBox(world, gx + 2, cy + 17, gz - 3, gx + 4, cy + 18, gz + 3, Material.STONE);

        // 3. Rocky plunge pool at the foot of the waterfall
        for (int x = gx + 4; x <= gx + 8; x++) {
            for (int z = gz - 4; z <= gz + 4; z++) {
                world.getBlockAt(x, cy - 1, z).setType(Material.WATER, false);
                world.getBlockAt(x, cy - 2, z).setType(Material.SAND, false);
                if (x == gx + 8 || z == gz - 4 || z == gz + 4) {
                    world.getBlockAt(x, cy, z).setType(Material.MOSSY_COBBLESTONE, false);
                } else {
                    world.getBlockAt(x, cy, z).setType(Material.WATER, false);
                    if ((x + z) % 3 == 0) {
                        world.getBlockAt(x, cy + 1, z).setType(Material.LILY_PAD, false);
                    }
                }
            }
        }
        // Stepping stones through the waterfall curtain
        world.getBlockAt(gx + 4, cy, gz).setType(Material.SMOOTH_STONE_SLAB, false);
        world.getBlockAt(gx + 3, cy, gz).setType(Material.AIR, false);
        world.getBlockAt(gx + 3, cy + 1, gz).setType(Material.AIR, false);
        world.getBlockAt(gx + 3, cy + 2, gz).setType(Material.AIR, false);
    }

    /**
     * Builds a challenging parkour route on the Parkour Academy spire allowing rooftop ascent.
     */
    public static void buildParkourClimbRoute(World world, int cx, int y, int cz) {
        int colX = cx - 4; // -26
        int colZ = cz;     // -22

        world.getBlockAt(colX - 1, y + 1, colZ).setType(Material.SLIME_BLOCK, false);
        world.getBlockAt(colX - 1, y + 2, colZ - 1).setType(Material.OAK_FENCE, false);
        world.getBlockAt(colX, y + 3, colZ - 2).setType(Material.OAK_PLANKS, false);
        world.getBlockAt(colX + 1, y + 4, colZ - 2).setType(Material.SPRUCE_TRAPDOOR, false);
        world.getBlockAt(colX + 2, y + 5, colZ - 1).setType(Material.OAK_PLANKS, false);
        world.getBlockAt(colX + 1, y + 6, colZ).setType(Material.EMERALD_BLOCK, false); // Roof landing

        // Steps up the roof to the Spire peak
        for (int ry = y + 7; ry <= y + 12; ry++) {
            world.getBlockAt(cx - (y + 13 - ry), ry, cz).setType(Material.SPRUCE_STAIRS, false);
        }
        world.getBlockAt(cx, y + 13, cz).setType(Material.GOLD_BLOCK, false);
        world.getBlockAt(cx, y + 14, cz).setType(Material.AIR, false); // Relic 7 stands here!
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

    /**
     * Builds a real active colored beacon shooting a colored light beam into the sky.
     */
    public static void buildColoredBeacon(World world, int x, int y, int z, Material glassColor) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.getBlockAt(x + dx, y - 1, z + dz).setType(Material.IRON_BLOCK, false);
            }
        }
        world.getBlockAt(x, y, z).setType(Material.BEACON, false);
        world.getBlockAt(x, y + 1, z).setType(glassColor, false);
    }

    /**
     * Builds a continuous 360-degree colossal mountain amphitheater enclosing the entire hub valley.
     */
    public static void buildMountainRing(World world, int cx, int cz, int cy, int innerR, int peakR, int outerR) {
        for (int x = -outerR; x <= outerR; x++) {
            for (int z = -outerR; z <= outerR; z++) {
                double dist = Math.hypot(x, z);
                if (dist >= innerR && dist <= outerR) {
                    double angle = Math.atan2(z, x);
                    double noise = Math.sin(angle * 5) * 7.0 + Math.cos(angle * 9) * 5.0 + Math.sin(angle * 13) * 3.0;
                    double peakHeight = 138.0 + noise; // Varies naturally between 123 and 153

                    double targetH;
                    if (dist < peakR) {
                        double t = (dist - innerR) / (double) (peakR - innerR);
                        targetH = cy + (peakHeight - cy) * Math.sin(t * Math.PI / 2);
                    } else {
                        double t = (dist - peakR) / (double) (outerR - peakR);
                        targetH = peakHeight - (peakHeight - (cy - 10)) * Math.sin(t * Math.PI / 2);
                    }

                    int maxY = (int) Math.round(targetH);
                    int bx = cx + x;
                    int bz = cz + z;
                    int minY = Math.max(50, cy - 25);

                    for (int y = minY; y <= maxY; y++) {
                        Material mat;
                        if (y >= 132) {
                            mat = (y == maxY && ((bx + bz) % 3 != 0)) ? Material.SNOW_BLOCK : Material.CALCITE;
                        } else if (y >= 118) {
                            mat = ((bx * 7 + bz * 13) % 4 == 0) ? Material.COBBLESTONE : Material.STONE;
                        } else if (y >= 108) {
                            mat = ((bx + bz) % 5 == 0) ? Material.ANDESITE : Material.STONE;
                        } else {
                            mat = Material.DEEPSLATE;
                        }

                        if (y == maxY && y < 118) {
                            mat = Material.GRASS_BLOCK;
                        }

                        world.getBlockAt(bx, y, bz).setType(mat, false);
                    }

                    // Snow cap or alpine spruce pine tree on mountain ledges
                    if (maxY >= 132) {
                        world.getBlockAt(bx, maxY + 1, bz).setType(Material.SNOW, false);
                    } else if (maxY >= 108 && maxY < 125 && ((bx * 31 + bz * 17) % 73 == 0)) {
                        buildSprucePine(world, bx, maxY + 1, bz, 5);
                    }
                }
            }
        }

        // Waterfall cascades pouring down from high mountain ledges into the valley
        double[][] waterfallAngles = {{-0.6, 0.8}, {0.8, -0.6}, {-0.8, -0.6}, {0.6, 0.8}};
        for (double[] dir : waterfallAngles) {
            int wx = (int) Math.round(cx + dir[0] * (innerR + 1));
            int wz = (int) Math.round(cz + dir[1] * (innerR + 1));
            for (int wy = cy + 18; wy >= cy; wy--) {
                world.getBlockAt(wx, wy, wz).setType(Material.WATER, false);
            }
            world.getBlockAt(wx, cy - 1, wz).setType(Material.COBBLESTONE, false);
        }
    }

    public static void buildSprucePine(World world, int x, int y, int z, int height) {
        for (int i = 0; i < height; i++) {
            world.getBlockAt(x, y + i, z).setType(Material.SPRUCE_LOG, false);
        }
        for (int h = 1; h <= height; h++) {
            int radius = (height - h) / 2 + 1;
            fillLeavesCircle(world, x, y + h, z, radius, Material.SPRUCE_LEAVES);
        }
        setLeaves(world, x, y + height, z, Material.SPRUCE_LEAVES);
    }

    /**
     * Clears all default vanilla chunks and generated terrain outside the mountain amphitheater.
     */
    public static void clearSurroundingVanillaTerrain(World world, int cx, int cz, int outerR, int clearR) {
        int outerSq = outerR * outerR;
        int clearSq = clearR * clearR;
        for (int x = -clearR; x <= clearR; x++) {
            for (int z = -clearR; z <= clearR; z++) {
                int distSq = x * x + z * z;
                if (distSq > outerSq && distSq <= clearSq) {
                    int bx = cx + x;
                    int bz = cz + z;
                    int highestY = world.getHighestBlockYAt(bx, bz);
                    if (highestY >= 40) {
                        for (int y = Math.min(highestY, 180); y >= 40; y--) {
                            Block b = world.getBlockAt(bx, y, bz);
                            if (b.getType() != Material.AIR) {
                                b.setType(Material.AIR, false);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Clears all default vanilla terrain within a given radius.
     */
    public static void clearVanillaTerrain(World world, int cx, int cz, int radius) {
        int rSq = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= rSq) {
                    int bx = cx + x;
                    int bz = cz + z;
                    int highestY = world.getHighestBlockYAt(bx, bz);
                    if (highestY >= 40) {
                        for (int y = Math.min(highestY, 180); y >= 40; y--) {
                            Block b = world.getBlockAt(bx, y, bz);
                            if (b.getType() != Material.AIR) {
                                b.setType(Material.AIR, false);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Places a directional wooden signpost with glowing text.
     */
    public static void placeDirectionalSign(World world, int x, int y, int z, String line1, String line2, String line3) {
        world.getBlockAt(x, y, z).setType(Material.DARK_OAK_FENCE, false);
        Block signBlock = world.getBlockAt(x, y + 1, z);
        signBlock.setType(Material.OAK_SIGN, false);
        if (signBlock.getState() instanceof org.bukkit.block.Sign sign) {
            sign.setLine(0, line1);
            sign.setLine(1, line2);
            sign.setLine(2, line3);
            sign.setGlowingText(true);
            sign.update(true, false);
        }
    }

    // ==========================================
    // Giant Thematic 3D Sculptures
    // ==========================================

    public static void buildGiantBed(World world, int cx, int y, int cz) {
        // Legs
        world.getBlockAt(cx - 2, y, cz - 3).setType(Material.OAK_FENCE, false);
        world.getBlockAt(cx + 2, y, cz - 3).setType(Material.OAK_FENCE, false);
        world.getBlockAt(cx - 2, y, cz + 3).setType(Material.OAK_FENCE, false);
        world.getBlockAt(cx + 2, y, cz + 3).setType(Material.OAK_FENCE, false);

        // Frame
        fillBox(world, cx - 2, y + 1, cz - 3, cx + 2, y + 1, cz + 3, Material.OAK_PLANKS);
        // Pillow
        fillBox(world, cx - 2, y + 2, cz - 3, cx + 2, y + 2, cz - 2, Material.WHITE_WOOL);
        // Blanket
        fillBox(world, cx - 2, y + 2, cz - 1, cx + 2, y + 2, cz + 3, Material.RED_WOOL);
    }

    public static void buildGiantEnderEye(World world, int cx, int y, int cz) {
        fillSphere(world, cx, y, cz, 3, Material.GREEN_TERRACOTTA);
        fillSphere(world, cx, y, cz, 1, Material.BLACK_CONCRETE);
        world.getBlockAt(cx, y, cz).setType(Material.OBSIDIAN, false);

        // Sweeping quartz wings extending left and right
        for (int i = 1; i <= 4; i++) {
            world.getBlockAt(cx - 3 - i, y + i, cz).setType(Material.SMOOTH_QUARTZ, false);
            world.getBlockAt(cx + 3 + i, y + i, cz).setType(Material.SMOOTH_QUARTZ, false);
            world.getBlockAt(cx - 3 - i, y + i - 1, cz).setType(Material.QUARTZ_SLAB, false);
            world.getBlockAt(cx + 3 + i, y + i - 1, cz).setType(Material.QUARTZ_SLAB, false);
        }
    }

    public static void buildGiantPickaxe(World world, int cx, int y, int cz) {
        // Diagonal handle of oak wood
        for (int i = -3; i <= 3; i++) {
            world.getBlockAt(cx + i, y + i, cz).setType(Material.STRIPPED_OAK_LOG, false);
        }
        // Pickaxe head of diamond blocks
        int headX = cx + 3, headY = y + 3;
        for (int dx = -2; dx <= 2; dx++) {
            world.getBlockAt(headX - dx, headY + dx, cz).setType(Material.DIAMOND_BLOCK, false);
            world.getBlockAt(headX - dx, headY + dx + 1, cz).setType(Material.DIAMOND_BLOCK, false);
        }
    }

    public static void buildGiantTNT(World world, int cx, int y, int cz) {
        fillBox(world, cx - 1, y, cz - 1, cx + 1, y, cz + 1, Material.RED_CONCRETE);
        fillBox(world, cx - 1, y + 1, cz - 1, cx + 1, y + 1, cz + 1, Material.WHITE_CONCRETE);
        fillBox(world, cx - 1, y + 2, cz - 1, cx + 1, y + 2, cz + 1, Material.RED_CONCRETE);
        // Fuse on top
        world.getBlockAt(cx, y + 3, cz).setType(Material.DARK_OAK_FENCE, false);
        world.getBlockAt(cx, y + 4, cz).setType(Material.CAMPFIRE, false);
    }

    public static void buildGiantCrossedSwords(World world, int cx, int y, int cz) {
        // Sword 1 (\)
        for (int i = -3; i <= 3; i++) {
            world.getBlockAt(cx + i, y + i, cz).setType(Material.IRON_BLOCK, false);
        }
        // Sword 2 (/)
        for (int i = -3; i <= 3; i++) {
            world.getBlockAt(cx - i, y + i, cz).setType(Material.IRON_BLOCK, false);
        }
        // Pommels and tips
        world.getBlockAt(cx - 3, y - 3, cz).setType(Material.GOLD_BLOCK, false);
        world.getBlockAt(cx + 3, y - 3, cz).setType(Material.GOLD_BLOCK, false);
        world.getBlockAt(cx - 3, y + 3, cz).setType(Material.DIAMOND_BLOCK, false);
        world.getBlockAt(cx + 3, y + 3, cz).setType(Material.DIAMOND_BLOCK, false);
    }

    public static void buildGiantBridgeArch(World world, int cx, int y, int cz) {
        // Red pillar
        for (int dy = 0; dy <= 4; dy++) {
            world.getBlockAt(cx - 2, y + dy, cz).setType(Material.RED_CONCRETE, false);
            world.getBlockAt(cx + 2, y + dy, cz).setType(Material.BLUE_CONCRETE, false);
        }
        // Arch bridge on top
        for (int dx = -2; dx <= 2; dx++) {
            world.getBlockAt(cx + dx, y + 5, cz).setType(Material.GOLD_BLOCK, false);
        }
    }

    public static void buildGiantToriiEmblem(World world, int cx, int y, int cz) {
        // Torii crossbars
        for (int dx = -3; dx <= 3; dx++) {
            world.getBlockAt(cx + dx, y + 2, cz).setType(Material.RED_TERRACOTTA, false);
            world.getBlockAt(cx + dx, y + 4, cz).setType(Material.DARK_OAK_PLANKS, false);
        }
        // Golden sun in the middle
        fillCircle(world, cx, y + 3, cz, 1, Material.GOLD_BLOCK);
    }

    public static void buildGiantBoot(World world, int cx, int y, int cz) {
        // Leg shaft
        fillBox(world, cx - 1, y + 2, cz, cx + 1, y + 4, cz + 1, Material.DIAMOND_BLOCK);
        // Foot
        fillBox(world, cx - 1, y, cz - 2, cx + 1, y + 1, cz + 1, Material.DIAMOND_BLOCK);
        // White sneaker sole
        fillBox(world, cx - 1, y - 1, cz - 2, cx + 1, y - 1, cz + 1, Material.WHITE_CONCRETE);
        // Golden wing / feather on heel
        world.getBlockAt(cx + 2, y + 2, cz + 1).setType(Material.GOLD_BLOCK, false);
        world.getBlockAt(cx + 2, y + 3, cz + 2).setType(Material.GOLD_BLOCK, false);
    }

    public static void fillSphere(World world, int cx, int cy, int cz, int radius, Material material) {
        int rSq = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z <= rSq) {
                        world.getBlockAt(cx + x, cy + y, cz + z).setType(material, false);
                    }
                }
            }
        }
    }

    /**
     * Builds a vibrant, lush Autumn Tree with glowing warm canopy and fallen leaf carpets.
     */
    public static void buildAutumnTree(World world, int x, int y, int z, Material logMat, Material leafMat, Material accentMat, int height) {
        // Trunk
        for (int h = 0; h < height; h++) {
            world.getBlockAt(x, y + h, z).setType(logMat, false);
        }

        // Roots
        world.getBlockAt(x + 1, y, z).setType(logMat, false);
        world.getBlockAt(x - 1, y, z).setType(logMat, false);
        world.getBlockAt(x, y, z + 1).setType(logMat, false);
        world.getBlockAt(x, y, z - 1).setType(logMat, false);

        // Branches
        int branchY = y + height - 2;
        world.getBlockAt(x + 1, branchY, z).setType(logMat, false);
        world.getBlockAt(x - 1, branchY, z).setType(logMat, false);
        world.getBlockAt(x, branchY, z + 1).setType(logMat, false);
        world.getBlockAt(x, branchY, z - 1).setType(logMat, false);

        // Warm Glowing Core inside canopy
        world.getBlockAt(x, y + height, z).setType(Material.SHROOMLIGHT, false);

        // Multi-layer Autumn Foliage (Spherical naturally textured)
        int canopyCenterY = y + height;
        for (int ox = -3; ox <= 3; ox++) {
            for (int oy = -2; oy <= 3; oy++) {
                for (int oz = -3; oz <= 3; oz++) {
                    double dist = Math.sqrt(ox * ox + oy * oy * 1.3 + oz * oz);
                    if (dist <= 3.2 && !(ox == 0 && oy == 0 && oz == 0)) {
                        int px = x + ox;
                        int py = canopyCenterY + oy;
                        int pz = z + oz;
                        Material m = ((ox + oy + oz) % 3 == 0) ? accentMat : leafMat;
                        setLeaves(world, px, py, pz, m);
                    }
                }
            }
        }

        // Fallen Leaves on the ground around the tree
        Material carpet = (accentMat == Material.ORANGE_WOOL || accentMat == Material.ORANGE_TERRACOTTA) ? Material.ORANGE_CARPET : Material.YELLOW_CARPET;
        int[][] leafOffsets = {{1, 1}, {-1, 1}, {2, 0}, {-2, 0}, {0, 2}, {0, -2}, {1, -2}, {-1, -1}};
        for (int[] off : leafOffsets) {
            int lx = x + off[0];
            int lz = z + off[1];
            if (world.getBlockAt(lx, y, lz).getType().isSolid() && world.getBlockAt(lx, y + 1, lz).getType() == Material.AIR) {
                world.getBlockAt(lx, y + 1, lz).setType(carpet, false);
            }
        }
    }

    /**
     * Builds a magnificent Steampunk Autumn Airship suspended high above the courtyard (CubeCraft style).
     */
    public static void buildSteampunkAirship(World world, int cx, int cy, int cz) {
        // 1. Ellipsoid Balloon Envelope (Orange and White striped)
        for (int x = -7; x <= 7; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    double norm = (x * x) / 49.0 + (y * y) / 9.0 + (z * z) / 9.0;
                    if (norm <= 1.0) {
                        Material wool = (Math.abs(x) % 2 == 0) ? Material.ORANGE_WOOL : Material.WHITE_WOOL;
                        world.getBlockAt(cx + x, cy + y, cz + z).setType(wool, false);
                    }
                }
            }
        }
        // Gold / Copper nose cone
        world.getBlockAt(cx + 8, cy, cz).setType(Material.COPPER_BLOCK, false);
        world.getBlockAt(cx - 8, cy, cz).setType(Material.COPPER_BLOCK, false);

        // 2. Rigging / Suspension cables
        for (int x = -5; x <= 5; x += 5) {
            world.getBlockAt(cx + x, cy - 4, cz - 2).setType(Material.IRON_CHAIN, false);
            world.getBlockAt(cx + x, cy - 4, cz + 2).setType(Material.IRON_CHAIN, false);
            world.getBlockAt(cx + x, cy - 5, cz - 2).setType(Material.IRON_CHAIN, false);
            world.getBlockAt(cx + x, cy - 5, cz + 2).setType(Material.IRON_CHAIN, false);
        }

        // 3. Gondola Cabin (Spruce timber with observation windows)
        int gy = cy - 6;
        fillBox(world, cx - 4, gy, cz - 1, cx + 4, gy + 1, cz + 1, Material.SPRUCE_PLANKS);
        fillBox(world, cx - 3, gy + 1, cz - 1, cx + 3, gy + 1, cz - 1, Material.GLASS_PANE);
        fillBox(world, cx - 3, gy + 1, cz + 1, cx + 3, gy + 1, cz + 1, Material.GLASS_PANE);
        world.getBlockAt(cx - 4, gy, cz).setType(Material.DARK_OAK_DOOR, false);

        // Lanterns hanging below gondola
        world.getBlockAt(cx - 3, gy - 1, cz).setType(Material.LANTERN, false);
        world.getBlockAt(cx + 3, gy - 1, cz).setType(Material.LANTERN, false);

        // Rear Brass Propeller
        world.getBlockAt(cx - 5, gy + 1, cz).setType(Material.SPRUCE_FENCE, false);
        world.getBlockAt(cx - 6, gy + 1, cz).setType(Material.LIGHTNING_ROD, false);
        world.getBlockAt(cx - 6, gy + 2, cz).setType(Material.OAK_TRAPDOOR, false);
        world.getBlockAt(cx - 6, gy, cz).setType(Material.OAK_TRAPDOOR, false);
    }

    /**
     * Builds a cozy half-timbered medieval Autumn Town House (Hypixel style).
     */
    public static void buildAutumnTownHouse(World world, int x, int y, int z, int width, int length, Material wallMat, Material roofMat) {
        // Foundation of Mud Bricks & Cobblestone
        fillBox(world, x, y, z, x + width - 1, y, z + length - 1, Material.MUD_BRICKS);

        int height = 5;
        // Timber frame corners
        buildPillar(world, x, y + 1, z, height, Material.STRIPPED_DARK_OAK_LOG);
        buildPillar(world, x + width - 1, y + 1, z, height, Material.STRIPPED_DARK_OAK_LOG);
        buildPillar(world, x, y + 1, z + length - 1, height, Material.STRIPPED_DARK_OAK_LOG);
        buildPillar(world, x + width - 1, y + 1, z + length - 1, height, Material.STRIPPED_DARK_OAK_LOG);

        // Walls with windows
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

        // Windows & Doors
        int midX = x + width / 2;
        int midZ = z + length / 2;
        world.getBlockAt(midX, y + 1, z).setType(Material.AIR, false);
        world.getBlockAt(midX, y + 2, z).setType(Material.AIR, false);
        world.getBlockAt(midX, y + 3, z - 1).setType(Material.LANTERN, false);

        world.getBlockAt(x, y + 2, midZ).setType(Material.GLASS_PANE, false);
        world.getBlockAt(x + width - 1, y + 2, midZ).setType(Material.GLASS_PANE, false);

        // Steep Gable Roof
        int roofBaseY = y + height + 1;
        int steps = (width + 1) / 2;
        for (int step = 0; step < steps; step++) {
            int currentY = roofBaseY + step;
            int lx = x + step - 1;
            int rx = x + width - step;
            for (int pz = z - 1; pz <= z + length; pz++) {
                world.getBlockAt(lx, currentY, pz).setType(roofMat, false);
                world.getBlockAt(rx, currentY, pz).setType(roofMat, false);
            }
        }
        int ridgeY = roofBaseY + steps;
        for (int pz = z - 1; pz <= z + length; pz++) {
            world.getBlockAt(midX, ridgeY, pz).setType(Material.SPRUCE_SLAB, false);
        }

        // Smoking Chimney
        world.getBlockAt(x + 1, ridgeY + 1, z + length - 2).setType(Material.CAMPFIRE, false);
        world.getBlockAt(x + 1, ridgeY + 2, z + length - 2).setType(Material.COBBLESTONE_WALL, false);
    }

    /**
     * Builds a fortified BedWars team base forge & shelter (Houses Item Shop, Team Upgrades, and Spawner inside).
     */
    public static void buildBedWarsBaseForge(World world, int x, int y, int z, Material wallMat, Material roofMat, Material accentMat) {
        int width = 7;
        int length = 7;

        // Foundation
        fillBox(world, x, y, z, x + width - 1, y, z + length - 1, Material.STONE_BRICKS);

        // 4 Corner Pillars
        int height = 4;
        buildPillar(world, x, y + 1, z, height, Material.DEEPSLATE_BRICKS);
        buildPillar(world, x + width - 1, y + 1, z, height, Material.DEEPSLATE_BRICKS);
        buildPillar(world, x, y + 1, z + length - 1, height, Material.DEEPSLATE_BRICKS);
        buildPillar(world, x + width - 1, y + 1, z + length - 1, height, Material.DEEPSLATE_BRICKS);

        // Side Walls
        for (int py = y + 1; py <= y + height; py++) {
            for (int px = x + 1; px < x + width - 1; px++) {
                world.getBlockAt(px, py, z + length - 1).setType(wallMat, false);
            }
            for (int pz = z + 1; pz < z + length - 1; pz++) {
                world.getBlockAt(x, py, pz).setType(wallMat, false);
                world.getBlockAt(x + width - 1, py, pz).setType(wallMat, false);
            }
        }

        // Open Entrance Archway facing forward (z side)
        for (int px = x + 1; px < x + width - 1; px++) {
            world.getBlockAt(px, y + height, z).setType(wallMat, false);
        }
        world.getBlockAt(x + 1, y + height - 1, z).setType(Material.STONE_BRICK_STAIRS, false);
        world.getBlockAt(x + width - 2, y + height - 1, z).setType(Material.STONE_BRICK_STAIRS, false);

        // Fortified Roof with battlements & lanterns
        fillBox(world, x, y + height + 1, z, x + width - 1, y + height + 1, z + length - 1, roofMat);
        for (int ox = 0; ox < width; ox++) {
            for (int oz = 0; oz < length; oz++) {
                if (ox == 0 || ox == width - 1 || oz == 0 || oz == length - 1) {
                    if ((ox + oz) % 2 == 0) {
                        world.getBlockAt(x + ox, y + height + 2, z + oz).setType(Material.STONE_BRICK_WALL, false);
                    }
                }
            }
        }
        world.getBlockAt(x + 1, y + height + 3, z + 1).setType(Material.LANTERN, false);
        world.getBlockAt(x + width - 2, y + height + 3, z + 1).setType(Material.LANTERN, false);

        // Interior Forge Equipment (Furnace, Anvil, Chest, Barrels)
        world.getBlockAt(x + 1, y + 1, z + length - 2).setType(Material.BLAST_FURNACE, false);
        world.getBlockAt(x + 2, y + 1, z + length - 2).setType(Material.ANVIL, false);
        world.getBlockAt(x + width - 2, y + 1, z + length - 2).setType(Material.CHEST, false);
        world.getBlockAt(x + width - 3, y + 1, z + length - 2).setType(Material.BARREL, false);
        world.getBlockAt(x + 3, y + height, z + 3).setType(Material.LANTERN, false);
    }

    /**
     * Builds a grand circular fortified castle curtain wall with battlements and wall-walk ramparts.
     */
    public static void buildCastleWallRing(World world, int cx, int cz, int cy, int innerR, int outerR, int wallHeight) {
        for (int x = -outerR; x <= outerR; x++) {
            for (int z = -outerR; z <= outerR; z++) {
                double dist = Math.hypot(x, z);
                if (dist >= innerR && dist <= outerR) {
                    int bx = cx + x;
                    int bz = cz + z;
                    for (int y = cy - 2; y <= cy + wallHeight; y++) {
                        Material mat = (y == cy + wallHeight) ? Material.POLISHED_ANDESITE :
                                (((bx + bz + y) % 5 == 0) ? Material.MOSSY_STONE_BRICKS : Material.STONE_BRICKS);
                        world.getBlockAt(bx, y, bz).setType(mat, false);
                    }
                    if (dist >= outerR - 0.7) {
                        int angleDeg = (int) Math.toDegrees(Math.atan2(z, x)) + 180;
                        if ((angleDeg / 4) % 2 == 0) {
                            world.getBlockAt(bx, cy + wallHeight + 1, bz).setType(Material.STONE_BRICK_WALL, false);
                        }
                    }
                }
            }
        }
    }

    /**
     * Builds a fortified round medieval tower with battlements and roof.
     */
    public static void buildCastleTowerRound(World world, int tx, int ty, int tz, int radius, int height, Material wallMat, Material roofMat) {
        fillCircle(world, tx, ty - 2, tz, radius, Material.COBBLESTONE);
        for (int y = ty - 1; y <= ty + height; y++) {
            fillRing(world, tx, y, tz, radius - 1, radius, wallMat);
            world.getBlockAt(tx, y, tz).setType(Material.STONE_BRICKS, false);
        }
        fillCircle(world, tx, ty + height, tz, radius + 1, Material.STONE_BRICKS);
        fillRing(world, tx, ty + height + 1, tz, radius, radius + 1, Material.STONE_BRICK_WALL);

        int roofSteps = radius + 2;
        for (int s = 0; s < roofSteps; s++) {
            int r = Math.max(1, radius + 1 - s);
            fillCircle(world, tx, ty + height + 1 + s, tz, r, roofMat);
        }
        world.getBlockAt(tx, ty + height + roofSteps + 1, tz).setType(Material.LIGHTNING_ROD, false);
    }
}
