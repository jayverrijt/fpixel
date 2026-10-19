package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.rank.Rank;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.game.rollback.BlockSnapshot;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GlassWallCommand implements CommandExecutor, TabCompleter {
    private final FPixelPlugin plugin;
    private final Map<UUID, List<BlockSnapshot>> undoHistory = new HashMap<>();

    public GlassWallCommand(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Alleen spelers kunnen dit commando gebruiken.");
            return true;
        }

        Rank rank = plugin.getRankManager().getRank(player);
        if (!player.isOp() && !player.hasPermission("fpixel.admin") && (rank == null || rank.getWeight() < 90)) {
            ChatUtil.sendMessage(player, "§c§lGLASMUUR §8» §cJe hebt geen toestemming voor dit commando!");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        // Undo support
        if (args.length > 0 && args[0].equalsIgnoreCase("undo")) {
            List<BlockSnapshot> snapshots = undoHistory.remove(player.getUniqueId());
            if (snapshots == null || snapshots.isEmpty()) {
                ChatUtil.sendMessage(player, "§c§lGLASMUUR §8» §cEr is geen recente actie om ongedaan te maken!");
                ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return true;
            }

            for (BlockSnapshot snapshot : snapshots) {
                snapshot.restore();
            }
            ChatUtil.sendMessage(player, "§a§lGLASMUUR §8» §aLaatste actie succesvol hersteld! §7(" + snapshots.size() + " blokken)");
            ChatUtil.playSound(player, Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
            return true;
        }

        int radius = 5; // Default radius: 5 chunks around player (11x11 chunks)
        boolean doWall = true;
        boolean doDry = true;
        boolean doFixOcean = true;

        if (args.length > 0) {
            String sub = args[0].toLowerCase();
            if (sub.equals("dry")) {
                doWall = false;
                doFixOcean = false;
            } else if (sub.equals("wall")) {
                doDry = false;
                doFixOcean = false;
            } else if (sub.equals("ocean") || sub.equals("fixocean")) {
                doWall = false;
                doDry = false;
            } else {
                try {
                    radius = Integer.parseInt(args[0]);
                } catch (NumberFormatException ignored) {}
            }
        }

        if (args.length > 1) {
            try {
                radius = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {}
        }

        radius = Math.max(1, Math.min(10, radius));

        World world = player.getWorld();
        Chunk centerChunk = player.getLocation().getChunk();
        int centerCX = centerChunk.getX();
        int centerCZ = centerChunk.getZ();

        ChatUtil.sendMessage(player, "§e§lGLASMUUR §8» §7Bezig met scannen van gebied (§e" + ((radius * 2) + 1) + "x" + ((radius * 2) + 1) + " chunks§7)...");

        // 1. Classify all chunks in the radius as BUILD_AREA vs OUTSIDE
        Map<Long, Boolean> isBuildChunkMap = new HashMap<>();
        Set<Long> scannedChunks = new HashSet<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = centerCX + dx;
                int cz = centerCZ + dz;
                long key = chunkKey(cx, cz);
                scannedChunks.add(key);
                isBuildChunkMap.put(key, isBuildChunk(world, cx, cz));
            }
        }

        List<BlockSnapshot> snapshots = new ArrayList<>();
        int glassPlaced = 0;
        int driedWaterCount = 0;
        int repairedWaterCount = 0;

        // 2. Build Glass Walls where Build Chunks border Outside Water Chunks
        if (doWall) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int cx = centerCX + dx;
                    int cz = centerCZ + dz;
                    long key = chunkKey(cx, cz);

                    // Only check from build area chunks
                    if (!isBuildChunkMap.getOrDefault(key, false)) {
                        continue;
                    }

                    int minX = cx << 4;
                    int maxX = minX + 15;
                    int minZ = cz << 4;
                    int maxZ = minZ + 15;

                    // Check all 4 chunk borders
                    // West border (x = minX)
                    if (!isBuildChunkMap.getOrDefault(chunkKey(cx - 1, cz), false)) {
                        glassPlaced += placeWallAlongBorder(world, minX, minZ, maxZ, true, cx - 1, cz, snapshots);
                    }
                    // East border (x = maxX)
                    if (!isBuildChunkMap.getOrDefault(chunkKey(cx + 1, cz), false)) {
                        glassPlaced += placeWallAlongBorder(world, maxX, minZ, maxZ, true, cx + 1, cz, snapshots);
                    }
                    // North border (z = minZ)
                    if (!isBuildChunkMap.getOrDefault(chunkKey(cx, cz - 1), false)) {
                        glassPlaced += placeWallAlongBorder(world, minZ, minX, maxX, false, cx, cz - 1, snapshots);
                    }
                    // South border (z = maxZ)
                    if (!isBuildChunkMap.getOrDefault(chunkKey(cx, cz + 1), false)) {
                        glassPlaced += placeWallAlongBorder(world, maxZ, minX, maxX, false, cx, cz + 1, snapshots);
                    }
                }
            }
        }

        // 3. Dry out all loose water inside Build Chunks
        if (doDry) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int cx = centerCX + dx;
                    int cz = centerCZ + dz;
                    long key = chunkKey(cx, cz);

                    if (!isBuildChunkMap.getOrDefault(key, false)) {
                        continue;
                    }

                    int minX = cx << 4;
                    int maxX = minX + 15;
                    int minZ = cz << 4;
                    int maxZ = minZ + 15;

                    for (int x = minX; x <= maxX; x++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            for (int y = world.getMinHeight(); y <= 85; y++) {
                                Block b = world.getBlockAt(x, y, z);
                                if (isWater(b)) {
                                    snapshots.add(new BlockSnapshot(b.getLocation(), b.getBlockData().clone()));
                                    b.setType(Material.AIR, false);
                                    driedWaterCount++;
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Repair loose water pockets & broken glass in Outside Ocean Chunks
        if (doFixOcean) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int cx = centerCX + dx;
                    int cz = centerCZ + dz;
                    long key = chunkKey(cx, cz);

                    // Only process outside chunks
                    if (isBuildChunkMap.getOrDefault(key, false)) {
                        continue;
                    }

                    int minX = cx << 4;
                    int maxX = minX + 15;
                    int minZ = cz << 4;
                    int maxZ = minZ + 15;

                    for (int x = minX; x <= maxX; x++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            // Ocean surface is at Y=63
                            for (int y = 30; y <= 63; y++) {
                                Block b = world.getBlockAt(x, y, z);
                                Material mat = b.getType();

                                if (mat == Material.GLASS || mat.isAir()) {
                                    if (isOceanBlockPosition(world, x, y, z)) {
                                        snapshots.add(new BlockSnapshot(b.getLocation(), b.getBlockData().clone()));
                                        b.setType(Material.WATER, false);
                                        repairedWaterCount++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Save undo
        undoHistory.put(player.getUniqueId(), snapshots);

        // Safety check player position
        if (player.getLocation().getBlock().getType() == Material.GLASS) {
            player.teleport(player.getLocation().add(0, 1, 0));
        }

        ChatUtil.sendMessage(player, "§a§lGLASMUUR §8» §aGebied succesvol verwerkt! §7(Straal: §e" + radius + " chunks§7)");
        if (glassPlaced > 0) {
            ChatUtil.sendMessage(player, "§7» §b" + glassPlaced + " §7glazen wand blokken geplaatst");
        }
        if (driedWaterCount > 0) {
            ChatUtil.sendMessage(player, "§7» §f" + driedWaterCount + " §7losse waterblokken gedroogd in bouwgebied");
        }
        if (repairedWaterCount > 0) {
            ChatUtil.sendMessage(player, "§7» §9" + repairedWaterCount + " §7waterblokken hersteld in de oceaan");
        }
        ChatUtil.sendMessage(player, "§7» §eTyp §6/glasswall undo §eom dit ongedaan te maken.");
        ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

        return true;
    }

    private int placeWallAlongBorder(World world, int fixedCoord, int minV, int maxV, boolean isFixedX, int neighborCX, int neighborCZ, List<BlockSnapshot> snapshots) {
        int placed = 0;
        int seaLevel = 64; // Cover ocean water up to Y=64

        for (int v = minV; v <= maxV; v++) {
            int x = isFixedX ? fixedCoord : v;
            int z = isFixedX ? v : fixedCoord;

            int outsideX = isFixedX ? (fixedCoord == (neighborCX << 4) + 15 ? fixedCoord + 1 : fixedCoord - 1) : x;
            int outsideZ = isFixedX ? z : (fixedCoord == (neighborCZ << 4) + 15 ? fixedCoord + 1 : fixedCoord - 1);

            // Check if the outside column has water or borders ocean
            boolean outsideHasWater = false;
            for (int checkY = 30; checkY <= seaLevel; checkY++) {
                Block outsideBlock = world.getBlockAt(outsideX, checkY, outsideZ);
                if (isWater(outsideBlock) || outsideBlock.getType() == Material.GLASS) {
                    outsideHasWater = true;
                    break;
                }
            }

            if (!outsideHasWater) {
                // Also check if neighbor biome is ocean
                Biome b = world.getBiome(outsideX, 60, outsideZ);
                if (b.getKey().getKey().contains("ocean") || b.getKey().getKey().contains("river")) {
                    outsideHasWater = true;
                }
            }

            if (!outsideHasWater) {
                continue;
            }

            // Find lowest air block in the build column to ground the glass wall
            int lowestY = seaLevel;
            for (int y = seaLevel; y >= world.getMinHeight(); y--) {
                Block b = world.getBlockAt(x, y, z);
                if (b.getType().isAir() || isWater(b)) {
                    lowestY = y;
                } else if (lowestY < seaLevel) {
                    // Reached solid floor
                    break;
                }
            }

            // Place glass from the floor up to sea level (Y=64)
            for (int y = lowestY; y <= seaLevel; y++) {
                Block b = world.getBlockAt(x, y, z);
                if (b.getType() != Material.GLASS && b.getType() != Material.BEDROCK) {
                    // Only replace air, water, or surface foliage with glass wall
                    if (b.getType().isAir() || isWater(b) || b.getType() == Material.SEAGRASS || b.getType() == Material.KELP) {
                        snapshots.add(new BlockSnapshot(b.getLocation(), b.getBlockData().clone()));
                        b.setType(Material.GLASS, false);
                        placed++;
                    }
                }
            }
        }

        return placed;
    }

    private boolean isOceanBlockPosition(World world, int x, int y, int z) {
        Biome biome = world.getBiome(x, y, z);
        String name = biome.getKey().getKey();
        if (name.contains("ocean") || name.contains("river")) {
            return true;
        }

        // Also check if any horizontal neighbor is water
        if (isWater(world.getBlockAt(x + 1, y, z))) return true;
        if (isWater(world.getBlockAt(x - 1, y, z))) return true;
        if (isWater(world.getBlockAt(x, y, z + 1))) return true;
        if (isWater(world.getBlockAt(x, y, z - 1))) return true;
        if (y < 63 && isWater(world.getBlockAt(x, y + 1, z))) return true;

        return false;
    }

    private boolean isBuildChunk(World world, int cx, int cz) {
        int airCount = 0;
        int totalSampled = 0;

        int minX = cx << 4;
        int minZ = cz << 4;

        // Sample between Y=25 and Y=62 (38 layers = 9,728 blocks)
        for (int x = minX; x <= minX + 15; x += 2) {
            for (int z = minZ; z <= minZ + 15; z += 2) {
                for (int y = 25; y <= 62; y += 2) {
                    totalSampled++;
                    Block b = world.getBlockAt(x, y, z);
                    if (b.getType().isAir()) {
                        airCount++;
                    }
                }
            }
        }

        double airRatio = (double) airCount / totalSampled;
        // Natural terrain and ocean chunks have < 25% air between Y=25 and Y=62.
        // Excavated build chunks have > 45% air.
        return airRatio > 0.40;
    }

    private boolean isWater(Block block) {
        if (block == null) return false;
        Material type = block.getType();
        if (type == Material.WATER || type == Material.BUBBLE_COLUMN
                || type == Material.KELP || type == Material.KELP_PLANT
                || type == Material.SEAGRASS || type == Material.TALL_SEAGRASS) {
            return true;
        }
        if (block.getBlockData() instanceof Waterlogged wl && wl.isWaterlogged()) {
            return true;
        }
        return false;
    }

    private long chunkKey(int cx, int cz) {
        return (((long) cx) << 32) | (cz & 0xffffffffL);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("5");
            completions.add("3");
            completions.add("undo");
            completions.add("dry");
            completions.add("wall");
            completions.add("ocean");
        }
        return completions;
    }
}
