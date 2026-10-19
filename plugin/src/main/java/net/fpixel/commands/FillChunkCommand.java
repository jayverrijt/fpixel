package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.rank.Rank;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.game.rollback.BlockSnapshot;
import org.bukkit.Chunk;
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

        List<String> remainingArgs = new ArrayList<>();
        for (String arg : args) {
            if (arg.equalsIgnoreCase("noglass")) {
                placeGlass = false;
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
                continue;
            }
            remainingArgs.add(arg);
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
                ChatUtil.sendMessage(player, "§c§lCHUNK §8» §cOngeldige Y-waarden! Gebruik: §e/fillchunk [vanY] [totY] [materiaal] [safe]");
                return true;
            }
        }

        World world = player.getWorld();
        int minY = Math.max(world.getMinHeight(), Math.min(fromY, toY));
        int maxY = Math.min(world.getMaxHeight() - 1, Math.max(fromY, toY));

        Chunk chunk = player.getLocation().getChunk();
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
            completions.add("noglass");
        } else if (args.length == 4) {
            completions.add("safe");
            completions.add("save");
            completions.add("keep");
            completions.add("noglass");
        }
        return completions;
    }
}
