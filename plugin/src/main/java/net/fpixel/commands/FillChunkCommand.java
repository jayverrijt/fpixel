package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.rank.Rank;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.game.rollback.BlockSnapshot;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
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

public class FillChunkCommand implements CommandExecutor, TabCompleter {
    private final FPixelPlugin plugin;
    private final Map<UUID, List<BlockSnapshot>> undoHistory = new HashMap<>();

    public FillChunkCommand(FPixelPlugin plugin) {
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
            ChatUtil.sendMessage(player, "§c§lCHUNK §8» §cJe hebt geen toestemming voor dit commando!");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        // Undo support
        if (args.length > 0 && args[0].equalsIgnoreCase("undo")) {
            List<BlockSnapshot> snapshots = undoHistory.remove(player.getUniqueId());
            if (snapshots == null || snapshots.isEmpty()) {
                ChatUtil.sendMessage(player, "§c§lCHUNK §8» §cEr is geen recente /fillchunk actie om ongedaan te maken!");
                ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return true;
            }

            for (BlockSnapshot snapshot : snapshots) {
                snapshot.restore();
            }
            ChatUtil.sendMessage(player, "§a§lCHUNK §8» §aLaatste chunk actie succesvol hersteld! §7(" + snapshots.size() + " blokken)");
            ChatUtil.playSound(player, Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
            return true;
        }

        int playerY = player.getLocation().getBlockY();
        int fromY = playerY;
        int toY = 81;
        boolean placeGlass = true;
        Material fillMaterial = Material.AIR;
        boolean isSafeMode = false;

        boolean isFloodFill = label.toLowerCase().contains("flood");

        boolean materialSpecified = false;

        List<String> remainingArgs = new ArrayList<>();
        for (String arg : args) {
            if (arg.equalsIgnoreCase("noglass")) {
                placeGlass = false;
                continue;
            }
            if (arg.equalsIgnoreCase("flood") || arg.equalsIgnoreCase("floodfill")
                    || arg.equalsIgnoreCase("connect") || arg.equalsIgnoreCase("connected")
                    || arg.equalsIgnoreCase("verbonden")) {
                isFloodFill = true;
                continue;
            }
            if (arg.equalsIgnoreCase("safe") || arg.equalsIgnoreCase("save")
                    || arg.equalsIgnoreCase("keep") || arg.equalsIgnoreCase("safemode")
                    || arg.equalsIgnoreCase("savemode")) {
                isSafeMode = true;
                continue;
            }
            Material m = Material.matchMaterial(arg);
            if (m != null && m.isBlock()) {
                fillMaterial = m;
                materialSpecified = true;
                continue;
            }
            remainingArgs.add(arg);
        }

        if (isFloodFill && !materialSpecified && fillMaterial == Material.AIR) {
            fillMaterial = Material.STONE;
        }

        if (remainingArgs.size() == 1) {
            try {
                toY = parseY(remainingArgs.get(0), playerY);
            } catch (NumberFormatException e) {
                ChatUtil.sendMessage(player, "§c§lCHUNK §8» §cOngeldige Y-waarde: §e" + remainingArgs.get(0));
                return true;
            }
        } else if (remainingArgs.size() >= 2) {
            try {
                fromY = parseY(remainingArgs.get(0), playerY);
                toY = parseY(remainingArgs.get(1), playerY);
            } catch (NumberFormatException e) {
                ChatUtil.sendMessage(player, "§c§lCHUNK §8» §cOngeldige Y-waarden! Gebruik: §e/fillchunk [vanY] [totY] [materiaal] [flood] [safe]");
                return true;
            }
        }

        World world = player.getWorld();
        int minY = Math.max(world.getMinHeight(), Math.min(fromY, toY));
        int maxY = Math.min(world.getMaxHeight() - 1, Math.max(fromY, toY));

        Chunk chunk;
        Block targetLook = player.getTargetBlockExact(64);
        if (targetLook != null && (isFloodFill || player.isSneaking())) {
            chunk = targetLook.getChunk();
        } else {
            chunk = player.getLocation().getChunk();
        }

        int minX = chunk.getX() << 4;
        int maxX = minX + 15;
        int minZ = chunk.getZ() << 4;
        int maxZ = minZ + 15;

        List<BlockSnapshot> snapshots = new ArrayList<>();
        int airCount = 0;
        int glassCount = 0;
        int waterPreserved = 0;
        int fillCount = 0;
        int skippedCount = 0;

        // --- FLOOD FILL MODE (Vullen tot aan de muur, stopt bij obstakels) ---
        if (isFloodFill) {
            int foundX = -1;
            int foundY = -1;
            int foundZ = -1;

            org.bukkit.util.RayTraceResult rayTrace = player.rayTraceBlocks(64);
            if (rayTrace != null && rayTrace.getHitBlock() != null && rayTrace.getHitBlockFace() != null) {
                Block hit = rayTrace.getHitBlock();
                Block adjacent = hit.getRelative(rayTrace.getHitBlockFace());

                if (hit.getX() >= minX && hit.getX() <= maxX && hit.getZ() >= minZ && hit.getZ() <= maxZ
                        && hit.getY() >= minY && hit.getY() <= maxY && isFillable(hit)) {
                    foundX = hit.getX();
                    foundY = hit.getY();
                    foundZ = hit.getZ();
                } else if (adjacent.getX() >= minX && adjacent.getX() <= maxX && adjacent.getZ() >= minZ && adjacent.getZ() <= maxZ) {
                    if (adjacent.getY() >= minY && adjacent.getY() <= maxY && isFillable(adjacent)) {
                        foundX = adjacent.getX();
                        foundY = adjacent.getY();
                        foundZ = adjacent.getZ();
                    } else {
                        for (int y = minY; y <= maxY; y++) {
                            if (isFillable(world.getBlockAt(adjacent.getX(), y, adjacent.getZ()))) {
                                foundX = adjacent.getX();
                                foundY = y;
                                foundZ = adjacent.getZ();
                                break;
                            }
                        }
                    }
                }
            }

            if (foundY == -1) {
                int px = player.getLocation().getBlockX();
                int pz = player.getLocation().getBlockZ();
                if (px >= minX && px <= maxX && pz >= minZ && pz <= maxZ) {
                    for (int dx = -2; dx <= 2 && foundY == -1; dx++) {
                        for (int dz = -2; dz <= 2 && foundY == -1; dz++) {
                            int cx = px + dx;
                            int cz = pz + dz;
                            if (cx < minX || cx > maxX || cz < minZ || cz > maxZ) continue;
                            for (int y = minY; y <= maxY; y++) {
                                if (isFillable(world.getBlockAt(cx, y, cz))) {
                                    foundX = cx;
                                    foundY = y;
                                    foundZ = cz;
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            if (foundY == -1) {
                ChatUtil.sendMessage(player, "§c§lCHUNK §8» §cGeen lege lucht (air) gevonden aan jouw kant tussen Y=§e" + minY + " §cen Y=§e" + maxY + "§c!");
                ChatUtil.sendMessage(player, "§7» §eJouw kant is op deze hoogte al massief gevuld. Er is niets meer te vullen zonder over de muur te gaan.");
                ChatUtil.sendMessage(player, "§7» §aTip: Gebruik §6//wand §eof §6//pos1 §e+ §6//pos2 §eom zelf het exacte rechthoekige gebied te vullen met §6//set " + fillMaterial.name().toLowerCase() + "§a!");
                return true;
            }

            Queue<BlockPos> queue = new ArrayDeque<>();
            Set<BlockPos> visited = new HashSet<>();

            BlockPos startPos = new BlockPos(foundX, foundY, foundZ);
            queue.add(startPos);
            visited.add(startPos);

            while (!queue.isEmpty()) {
                BlockPos p = queue.poll();
                Block block = world.getBlockAt(p.x, p.y, p.z);

                if (!isFillable(block)) {
                    continue;
                }

                if (block.getType() != fillMaterial) {
                    snapshots.add(new BlockSnapshot(block.getLocation(), block.getBlockData().clone()));
                    block.setType(fillMaterial, false);
                    fillCount++;
                }

                int[][] dirs = {
                    {1, 0, 0}, {-1, 0, 0},
                    {0, 0, 1}, {0, 0, -1},
                    {0, 1, 0}, {0, -1, 0}
                };

                for (int[] d : dirs) {
                    int nx = p.x + d[0];
                    int ny = p.y + d[1];
                    int nz = p.z + d[2];

                    if (nx < minX || nx > maxX || nz < minZ || nz > maxZ) continue;
                    if (ny < minY || ny > maxY) continue;

                    BlockPos np = new BlockPos(nx, ny, nz);
                    if (visited.add(np)) {
                        Block neighbor = world.getBlockAt(nx, ny, nz);
                        if (isFillable(neighbor)) {
                            queue.add(np);
                        }
                    }
                }
            }

            undoHistory.put(player.getUniqueId(), snapshots);

            int layerCount = maxY - minY + 1;
            ChatUtil.sendMessage(player, "§a§lCHUNK §8» §aFlood Fill voltooid! Gevuld met §e" + fillMaterial.name().toLowerCase() + "§a!");
            ChatUtil.sendMessage(player, "§7» §f" + fillCount + " §7blokken geplaatst (tussen Y=§e" + minY + " §7en Y=§e" + maxY + "§7, §e" + layerCount + " lagen§7)");
            ChatUtil.sendMessage(player, "§7» §7Doel-chunk: §e[X: " + chunk.getX() + ", Z: " + chunk.getZ() + "] §7(X: §f" + minX + ".." + maxX + "§7, Z: §f" + minZ + ".." + maxZ + "§7)");
            ChatUtil.sendMessage(player, "§7» §7Startpositie: §eX: " + foundX + ", Y: " + foundY + ", Z: " + foundZ);
            ChatUtil.sendMessage(player, "§7» §aDe andere kant van de muur is 100% onaangeroerd gebleven.");
            ChatUtil.sendMessage(player, "§7» §eTyp §6/" + label + " undo §eom dit ongedaan te maken.");
            ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            return true;
        }

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();

                    // Never modify bedrock
                    if (type == Material.BEDROCK) {
                        continue;
                    }

                    if (fillMaterial == Material.AIR) {
                        // Keep ocean water intact
                        if (isWater(block)) {
                            waterPreserved++;
                            continue;
                        }

                        // Check if block borders water
                        boolean touchesWater = placeGlass && touchesAnyWater(world, x, y, z);

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
                    } else {
                        // Filling with specific material (e.g. STONE)
                        if (isSafeMode && !type.isAir()) {
                            // Safe mode: do not overwrite existing blocks (e.g. dirt, stone, wood, etc.)
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
        }

        // Store for undo
        undoHistory.put(player.getUniqueId(), snapshots);

        // Prevent suffocating if player feet got turned into solid block
        if (player.getLocation().getBlock().getType().isSolid()) {
            player.teleport(player.getLocation().add(0, 1, 0));
        }

        if (fillMaterial == Material.AIR) {
            ChatUtil.sendMessage(player, "§a§lCHUNK §8» §aChunk leeggemaakt tussen Y=§e" + minY + " §aen Y=§e" + maxY + "§a!");
            ChatUtil.sendMessage(player, "§7» §f" + airCount + " §7blokken verwijderd (lucht)");
            if (glassCount > 0) {
                ChatUtil.sendMessage(player, "§7» §b" + glassCount + " §7glasblokken geplaatst (oceaan-bescherming)");
            }
            if (waterPreserved > 0) {
                ChatUtil.sendMessage(player, "§7» §9" + waterPreserved + " §7oceaan/waterblokken behouden");
            }
        } else {
            int layerCount = maxY - minY + 1;
            ChatUtil.sendMessage(player, "§a§lCHUNK §8» §aChunk succesvol gevuld met §e" + fillMaterial.name().toLowerCase() + " §atussen Y=§e" + minY + " §aen Y=§e" + maxY + "§a! §7(" + layerCount + " lagen)");
            ChatUtil.sendMessage(player, "§7» §f" + fillCount + " §7blokken geplaatst");
            if (isSafeMode) {
                ChatUtil.sendMessage(player, "§7» §a§lSAFE MODE: §f" + skippedCount + " §7bestaande blokken (zoals dirt/steen) beschermd en behouden!");
            }
        }
        ChatUtil.sendMessage(player, "§7» §eTyp §6/" + label + " undo §eom dit ongedaan te maken.");
        ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

        return true;
    }

    private int parseY(String arg, int playerY) {
        if (arg.equals("~")) {
            return playerY;
        }
        if (arg.startsWith("~")) {
            int offset = Integer.parseInt(arg.substring(1));
            return playerY + offset;
        }
        return Integer.parseInt(arg);
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

    private boolean isFillable(Block block) {
        if (block == null) return false;
        Material type = block.getType();
        if (type.isAir()) return true;
        if (isWater(block) || type == Material.LAVA || type == Material.BEDROCK) return false;
        return block.isReplaceable();
    }

    private boolean touchesAnyWater(World world, int x, int y, int z) {
        // 6 Cardinal directions
        if (y + 1 < world.getMaxHeight() && isWater(world.getBlockAt(x, y + 1, z))) return true;
        if (y - 1 >= world.getMinHeight() && isWater(world.getBlockAt(x, y - 1, z))) return true;
        if (isWater(world.getBlockAt(x + 1, y, z))) return true;
        if (isWater(world.getBlockAt(x - 1, y, z))) return true;
        if (isWater(world.getBlockAt(x, y, z + 1))) return true;
        if (isWater(world.getBlockAt(x, y, z - 1))) return true;

        // 4 Horizontal diagonals
        if (isWater(world.getBlockAt(x + 1, y, z + 1))) return true;
        if (isWater(world.getBlockAt(x + 1, y, z - 1))) return true;
        if (isWater(world.getBlockAt(x - 1, y, z + 1))) return true;
        if (isWater(world.getBlockAt(x - 1, y, z - 1))) return true;

        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("52");
            completions.add("81");
            completions.add("~");
            completions.add("undo");
            completions.add("stone");
        } else if (args.length == 2) {
            completions.add("54");
            completions.add("55");
            completions.add("81");
            completions.add("stone");
        } else if (args.length == 3) {
            completions.add("stone");
            completions.add("deepslate");
            completions.add("dirt");
            completions.add("glass");
            completions.add("air");
            completions.add("safe");
            completions.add("save");
            completions.add("flood");
            completions.add("noglass");
        } else if (args.length == 4) {
            completions.add("flood");
            completions.add("safe");
            completions.add("save");
            completions.add("keep");
            completions.add("noglass");
        } else if (args.length == 5) {
            completions.add("flood");
            completions.add("safe");
        }
        return completions;
    }

    private record BlockPos(int x, int y, int z) {}
}
