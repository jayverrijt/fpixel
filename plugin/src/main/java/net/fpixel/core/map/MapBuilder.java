package net.fpixel.core.map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class MapBuilder {

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

    public static void buildFloatingIsland(World world, int centerX, int surfaceY, int centerZ, int radius, Material topMat, Material middleMat, Material bottomMat) {
        // Flat circular surface
        fillCircle(world, centerX, surfaceY, centerZ, radius, topMat);

        // Underside inverted cone / natural island tapering
        int currentRadius = radius;
        int depth = 0;
        while (currentRadius > 0 && depth < 8) {
            depth++;
            int y = surfaceY - depth;
            currentRadius = Math.max(0, radius - (int)(depth * 1.2));
            Material mat = depth <= 2 ? middleMat : bottomMat;
            fillCircle(world, centerX, y, centerZ, currentRadius, mat);
        }
    }

    public static void buildWaitingLobby(World world, Location center, Material floorMat, Material pillarMat, Material glassMat) {
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        // 1. Floating round base
        fillCircle(world, cx, cy, cz, 7, floorMat);
        fillRing(world, cx, cy, cz, 6, 7, Material.GOLD_BLOCK);
        fillCircle(world, cx, cy - 1, cz, 6, Material.SMOOTH_STONE);
        fillCircle(world, cx, cy - 2, cz, 4, Material.DEEPSLATE);
        fillCircle(world, cx, cy - 3, cz, 2, Material.BEDROCK);

        // 2. Glass dome / walls around waiting area so players don't fall off before game starts
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
        world.getBlockAt(cx, cy, cz).setType(Material.SEA_LANTERN, false);
    }
}
