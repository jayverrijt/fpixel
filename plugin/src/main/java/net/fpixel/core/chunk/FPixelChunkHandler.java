package net.fpixel.core.chunk;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.game.rollback.BlockSnapshot;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;

import java.util.*;

public class FPixelChunkHandler {
    private final FPixelPlugin plugin;
    private final Map<UUID, List<BlockSnapshot>> undoHistory = new HashMap<>();

    public FPixelChunkHandler(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean undo(Player player) {
        List<BlockSnapshot> snapshots = undoHistory.remove(player.getUniqueId());
        if (snapshots == null || snapshots.isEmpty()) {
            ChatUtil.sendMessage(player, "§c§lCHUNK §8» §cGeen eerdere actie gevonden om ongedaan te maken!");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        for (BlockSnapshot snapshot : snapshots) {
            snapshot.restore();
        }
        ChatUtil.sendMessage(player, "§a§lCHUNK §8» §aLaatste chunk actie succesvol hersteld! §7(" + snapshots.size() + " blokken)");
        ChatUtil.playSound(player, Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
        return true;
    }

    // ==========================================
    // -- FILL COMMANDS
    // ==========================================

    public void fillAll(Player player, Chunk chunk, int minY, int maxY, Material fillMaterial) {
        World world = player.getWorld();
        int minX = chunk.getX() << 4;
        int maxX = minX + 15;
        int minZ = chunk.getZ() << 4;
        int maxZ = minZ + 15;

        List<BlockSnapshot> snapshots = new ArrayList<>();
        int fillCount = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();

                    if (type == Material.BEDROCK) continue;

                    if (type != fillMaterial) {
                        snapshots.add(new BlockSnapshot(block.getLocation(), block.getBlockData().clone()));
                        block.setType(fillMaterial, false);
                        fillCount++;
                    }
                }
            }
        }

        undoHistory.put(player.getUniqueId(), snapshots);

        int layerCount = maxY - minY + 1;
        ChatUtil.sendMessage(player, "§a§lCHUNK §8» §aHele chunk gevuld met §e" + fillMaterial.name().toLowerCase() + "§a!");
        ChatUtil.sendMessage(player, "§7» §f" + fillCount + " §7blokken geplaatst (tussen Y=§e" + minY + " §7en Y=§e" + maxY + "§7, §e" + layerCount + " lagen§7)");
        ChatUtil.sendMessage(player, "§7» §7Chunk: §e[X: " + chunk.getX() + ", Z: " + chunk.getZ() + "] §7(X: §f" + minX + ".." + maxX + "§7, Z: §f" + minZ + ".." + maxZ + "§7)");
        ChatUtil.sendMessage(player, "§7» §eTyp §6/fpixel undo §eom dit ongedaan te maken.");
        ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
    }

    public void fillSafe(Player player, Chunk chunk, int minY, int maxY, Material fillMaterial) {
        World world = player.getWorld();
        int minX = chunk.getX() << 4;
        int maxX = minX + 15;
        int minZ = chunk.getZ() << 4;
        int maxZ = minZ + 15;

        List<BlockSnapshot> snapshots = new ArrayList<>();
        int fillCount = 0;
        int skippedCount = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();

                    if (type == Material.BEDROCK) continue;

                    // Safe: ignores placed/solid blocks, only fills air or replaceable
                    if (!type.isAir() && !block.isReplaceable()) {
                        skippedCount++;
                        continue;
                    }

                    if (type != fillMaterial) {
                        snapshots.add(new BlockSnapshot(block.getLocation(), block.getBlockData().clone()));
                        block.setType(fillMaterial, false);
                        fillCount++;
                    }
                }
            }
        }

        undoHistory.put(player.getUniqueId(), snapshots);

        int layerCount = maxY - minY + 1;
        ChatUtil.sendMessage(player, "§a§lCHUNK §8» §aSafe Fill voltooid met §e" + fillMaterial.name().toLowerCase() + "§a!");
        ChatUtil.sendMessage(player, "§7» §f" + fillCount + " §7blokken geplaatst (tussen Y=§e" + minY + " §7en Y=§e" + maxY + "§7, §e" + layerCount + " lagen§7)");
        ChatUtil.sendMessage(player, "§7» §e" + skippedCount + " §7bestaande blokken genegeerd en behouden.");
        ChatUtil.sendMessage(player, "§7» §7Chunk: §e[X: " + chunk.getX() + ", Z: " + chunk.getZ() + "] §7(X: §f" + minX + ".." + maxX + "§7, Z: §f" + minZ + ".." + maxZ + "§7)");
        ChatUtil.sendMessage(player, "§7» §eTyp §6/fpixel undo §eom dit ongedaan te maken.");
        ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
    }

    public void fillOneSide(Player player, Chunk chunk, int minY, int maxY, Material fillMaterial) {
        World world = player.getWorld();
        int minX = chunk.getX() << 4;
        int maxX = minX + 15;
        int minZ = chunk.getZ() << 4;
        int maxZ = minZ + 15;

        // Determine player seed X, Z
        int seedX = player.getLocation().getBlockX();
        int seedZ = player.getLocation().getBlockZ();

        Block targetBlock = player.getTargetBlockExact(64);
        if (targetBlock != null && targetBlock.getX() >= minX && targetBlock.getX() <= maxX
                && targetBlock.getZ() >= minZ && targetBlock.getZ() <= maxZ) {
            seedX = targetBlock.getX();
            seedZ = targetBlock.getZ();
        } else {
            seedX = Math.max(minX, Math.min(maxX, seedX));
            seedZ = Math.max(minZ, Math.min(maxZ, seedZ));
        }

        List<BlockSnapshot> snapshots = new ArrayList<>();
        int fillCount = 0;

        // Process each Y layer independently!
        for (int y = minY; y <= maxY; y++) {
            int layerStartX = seedX;
            int layerStartZ = seedZ;

            Block startBlock = world.getBlockAt(layerStartX, y, layerStartZ);
            if (!isFillableTarget(startBlock)) {
                boolean foundNearby = false;
                for (int r = 1; r <= 3 && !foundNearby; r++) {
                    for (int dx = -r; dx <= r && !foundNearby; dx++) {
                        for (int dz = -r; dz <= r && !foundNearby; dz++) {
                            int cx = seedX + dx;
                            int cz = seedZ + dz;
                            if (cx < minX || cx > maxX || cz < minZ || cz > maxZ) continue;
                            Block b = world.getBlockAt(cx, y, cz);
                            if (isFillableTarget(b)) {
                                layerStartX = cx;
                                layerStartZ = cz;
                                foundNearby = true;
                            }
                        }
                    }
                }
                if (!foundNearby) {
                    // On this Y layer, the player's side is already solid (wall/floor), skip layer
                    continue;
                }
            }

            // 2D BFS flood on this Y layer only
            Queue<Point2D> queue = new ArrayDeque<>();
            Set<Point2D> visited = new HashSet<>();

            Point2D startPoint = new Point2D(layerStartX, layerStartZ);
            queue.add(startPoint);
            visited.add(startPoint);

            while (!queue.isEmpty()) {
                Point2D p = queue.poll();
                Block block = world.getBlockAt(p.x, y, p.z);

                if (!isFillableTarget(block)) {
                    continue;
                }

                if (block.getType() != fillMaterial) {
                    snapshots.add(new BlockSnapshot(block.getLocation(), block.getBlockData().clone()));
                    block.setType(fillMaterial, false);
                    fillCount++;
                }

                int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (int[] d : dirs) {
                    int nx = p.x + d[0];
                    int nz = p.z + d[1];

                    if (nx < minX || nx > maxX || nz < minZ || nz > maxZ) continue;

                    Point2D np = new Point2D(nx, nz);
                    if (visited.add(np)) {
                        Block neighbor = world.getBlockAt(nx, y, nz);
                        // Stopt zodra het een blok tegenkomt dat geen target block (lucht) is!
                        if (isFillableTarget(neighbor)) {
                            queue.add(np);
                        }
                    }
                }
            }
        }

        undoHistory.put(player.getUniqueId(), snapshots);

        int layerCount = maxY - minY + 1;
        ChatUtil.sendMessage(player, "§a§lCHUNK §8» §aOneside Fill voltooid met §e" + fillMaterial.name().toLowerCase() + "§a!");
        ChatUtil.sendMessage(player, "§7» §f" + fillCount + " §7blokken geplaatst (tussen Y=§e" + minY + " §7en Y=§e" + maxY + "§7, §e" + layerCount + " lagen§7)");
        ChatUtil.sendMessage(player, "§7» §7Chunk: §e[X: " + chunk.getX() + ", Z: " + chunk.getZ() + "] §7(X: §f" + minX + ".." + maxX + "§7, Z: §f" + minZ + ".." + maxZ + "§7)");
        ChatUtil.sendMessage(player, "§7» §aElke laag is gestopt bij het ontmoeten van een muur/afwijkend blok.");
        ChatUtil.sendMessage(player, "§7» §eTyp §6/fpixel undo §eom dit ongedaan te maken.");
        ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
    }

    // ==========================================
    // -- EMPTY COMMANDS
    // ==========================================

    public void emptyAll(Player player, Chunk chunk) {
        World world = player.getWorld();
        int minX = chunk.getX() << 4;
        int maxX = minX + 15;
        int minZ = chunk.getZ() << 4;
        int maxZ = minZ + 15;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;

        List<BlockSnapshot> snapshots = new ArrayList<>();
        int count = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();

                    if (type == Material.BEDROCK || type.isAir()) continue;

                    snapshots.add(new BlockSnapshot(block.getLocation(), block.getBlockData().clone()));
                    block.setType(Material.AIR, false);
                    count++;
                }
            }
        }

        undoHistory.put(player.getUniqueId(), snapshots);
        ChatUtil.sendMessage(player, "§a§lCHUNK §8» §aChunk leeggemaakt! §f" + count + " §ablokken verwijderd.");
        ChatUtil.sendMessage(player, "§7» §eTyp §6/fpixel undo §eom dit ongedaan te maken.");
        ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
    }

    public void emptySafe(Player player, Chunk chunk) {
        World world = player.getWorld();
        int minX = chunk.getX() << 4;
        int maxX = minX + 15;
        int minZ = chunk.getZ() << 4;
        int maxZ = minZ + 15;
        int playerY = player.getLocation().getBlockY();
        int maxY = world.getMaxHeight() - 1;

        List<BlockSnapshot> snapshots = new ArrayList<>();
        int count = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = playerY + 1; y <= maxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();

                    if (type == Material.BEDROCK || type.isAir()) continue;

                    snapshots.add(new BlockSnapshot(block.getLocation(), block.getBlockData().clone()));
                    block.setType(Material.AIR, false);
                    count++;
                }
            }
        }

        undoHistory.put(player.getUniqueId(), snapshots);
        ChatUtil.sendMessage(player, "§a§lCHUNK §8» §aEmpty Safe voltooid! Alle blokken boven Y=§e" + playerY + " §averwijderd (§f" + count + " §ablokken).");
        ChatUtil.sendMessage(player, "§7» §eTyp §6/fpixel undo §eom dit ongedaan te maken.");
        ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
    }

    public void emptyIgnoreWater(Player player, Chunk chunk) {
        World world = player.getWorld();
        int minX = chunk.getX() << 4;
        int maxX = minX + 15;
        int minZ = chunk.getZ() << 4;
        int maxZ = minZ + 15;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;

        List<BlockSnapshot> snapshots = new ArrayList<>();
        int count = 0;
        int waterPreserved = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();

                    if (type == Material.BEDROCK || type.isAir()) continue;

                    if (isWater(block)) {
                        waterPreserved++;
                        continue;
                    }

                    snapshots.add(new BlockSnapshot(block.getLocation(), block.getBlockData().clone()));
                    block.setType(Material.AIR, false);
                    count++;
                }
            }
        }

        undoHistory.put(player.getUniqueId(), snapshots);
        ChatUtil.sendMessage(player, "§a§lCHUNK §8» §aEmpty Ignore-Water voltooid! §f" + count + " §ablokken verwijderd, §e" + waterPreserved + " §awaterblokken behouden.");
        ChatUtil.sendMessage(player, "§7» §eTyp §6/fpixel undo §eom dit ongedaan te maken.");
        ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
    }

    public void emptyIgnoreWaterAndBox(Player player, Chunk chunk) {
        World world = player.getWorld();
        int minX = chunk.getX() << 4;
        int maxX = minX + 15;
        int minZ = chunk.getZ() << 4;
        int maxZ = minZ + 15;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;

        List<BlockSnapshot> snapshots = new ArrayList<>();
        int airCount = 0;
        int glassCount = 0;
        int waterPreserved = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();

                    if (type == Material.BEDROCK) continue;

                    if (isWater(block)) {
                        waterPreserved++;
                        continue;
                    }

                    boolean touchesWater = touchesAnyWater(world, x, y, z);
                    if (touchesWater) {
                        if (type != Material.GLASS) {
                            snapshots.add(new BlockSnapshot(block.getLocation(), block.getBlockData().clone()));
                            block.setType(Material.GLASS, false);
                            glassCount++;
                        }
                    } else {
                        if (!type.isAir()) {
                            snapshots.add(new BlockSnapshot(block.getLocation(), block.getBlockData().clone()));
                            block.setType(Material.AIR, false);
                            airCount++;
                        }
                    }
                }
            }
        }

        undoHistory.put(player.getUniqueId(), snapshots);
        ChatUtil.sendMessage(player, "§a§lCHUNK §8» §aEmpty Ignore-Water-And-Box voltooid!");
        ChatUtil.sendMessage(player, "§7» §f" + airCount + " §7blokken leeggemaakt");
        ChatUtil.sendMessage(player, "§7» §b" + glassCount + " §7glazen wand blokken geplaatst om water in te sluiten");
        ChatUtil.sendMessage(player, "§7» §9" + waterPreserved + " §7waterblokken behouden");
        ChatUtil.sendMessage(player, "§7» §eTyp §6/fpixel undo §eom dit ongedaan te maken.");
        ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
    }

    public void emptyGlass(Player player, Chunk chunk) {
        World world = player.getWorld();
        int minX = chunk.getX() << 4;
        int maxX = minX + 15;
        int minZ = chunk.getZ() << 4;
        int maxZ = minZ + 15;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;

        List<BlockSnapshot> snapshots = new ArrayList<>();
        int count = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.GLASS) {
                        snapshots.add(new BlockSnapshot(block.getLocation(), block.getBlockData().clone()));
                        block.setType(Material.AIR, false);
                        count++;
                    }
                }
            }
        }

        undoHistory.put(player.getUniqueId(), snapshots);
        ChatUtil.sendMessage(player, "§a§lCHUNK §8» §aEmpty Glass voltooid! §f" + count + " §aglasblokken verwijderd in deze chunk.");
        ChatUtil.sendMessage(player, "§7» §eTyp §6/fpixel undo §eom dit ongedaan te maken.");
        ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
    }

    public void emptySponge(Player player, Chunk chunk) {
        World world = player.getWorld();
        int minX = chunk.getX() << 4;
        int maxX = minX + 15;
        int minZ = chunk.getZ() << 4;
        int maxZ = minZ + 15;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;

        List<BlockSnapshot> snapshots = new ArrayList<>();
        int count = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();
                    if (type == Material.SPONGE || type == Material.WET_SPONGE) {
                        snapshots.add(new BlockSnapshot(block.getLocation(), block.getBlockData().clone()));
                        block.setType(Material.AIR, false);
                        count++;
                    }
                }
            }
        }

        undoHistory.put(player.getUniqueId(), snapshots);
        ChatUtil.sendMessage(player, "§a§lCHUNK §8» §aEmpty Sponge voltooid! §f" + count + " §asponzen verwijderd in deze chunk.");
        ChatUtil.sendMessage(player, "§7» §eTyp §6/fpixel undo §eom dit ongedaan te maken.");
        ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
    }

    // ==========================================
    // -- HELPERS
    // ==========================================

    private boolean isFillableTarget(Block block) {
        if (block == null) return false;
        Material type = block.getType();
        if (type.isAir()) return true;
        if (isWater(block) || type == Material.LAVA || type == Material.BEDROCK) return false;
        return block.isReplaceable();
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

    private boolean touchesAnyWater(World world, int x, int y, int z) {
        if (y + 1 < world.getMaxHeight() && isWater(world.getBlockAt(x, y + 1, z))) return true;
        if (y - 1 >= world.getMinHeight() && isWater(world.getBlockAt(x, y - 1, z))) return true;
        if (isWater(world.getBlockAt(x + 1, y, z))) return true;
        if (isWater(world.getBlockAt(x - 1, y, z))) return true;
        if (isWater(world.getBlockAt(x, y, z + 1))) return true;
        if (isWater(world.getBlockAt(x, y, z - 1))) return true;

        if (isWater(world.getBlockAt(x + 1, y, z + 1))) return true;
        if (isWater(world.getBlockAt(x + 1, y, z - 1))) return true;
        if (isWater(world.getBlockAt(x - 1, y, z + 1))) return true;
        if (isWater(world.getBlockAt(x - 1, y, z - 1))) return true;

        return false;
    }

    private record Point2D(int x, int z) {}
}
