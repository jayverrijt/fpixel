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

public class CleanGlassCommand implements CommandExecutor, TabCompleter {
    private final FPixelPlugin plugin;
    private final Map<UUID, List<BlockSnapshot>> undoHistory = new HashMap<>();

    public CleanGlassCommand(FPixelPlugin plugin) {
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
            ChatUtil.sendMessage(player, "§c§lCLEAN §8» §cJe hebt geen toestemming voor dit commando!");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("undo")) {
            List<BlockSnapshot> snapshots = undoHistory.remove(player.getUniqueId());
            if (snapshots == null || snapshots.isEmpty()) {
                ChatUtil.sendMessage(player, "§c§lCLEAN §8» §cEr is geen recente actie om ongedaan te maken!");
                ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return true;
            }
            for (BlockSnapshot s : snapshots) {
                s.restore();
            }
            ChatUtil.sendMessage(player, "§a§lCLEAN §8» §aLaatste actie hersteld! §7(" + snapshots.size() + " glasblokken)");
            ChatUtil.playSound(player, Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
            return true;
        }

        // Mode:
        // "ocean" (default): only removes glass that actively touches/holds back water/ocean
        // "safe": only removes glass that does NOT touch water (keeps the barrier intact)
        // "all": removes all glass
        String mode = "ocean";
        int chunkRadius = 1; // Default: 3x3 chunks

        for (String arg : args) {
            String lower = arg.toLowerCase();
            if (lower.equals("safe")) {
                mode = "safe";
            } else if (lower.equals("all")) {
                mode = "all";
            } else if (lower.equals("ocean")) {
                mode = "ocean";
            } else {
                try {
                    chunkRadius = Integer.parseInt(arg);
                } catch (NumberFormatException ignored) {}
            }
        }

        chunkRadius = Math.max(0, Math.min(5, chunkRadius));

        World world = player.getWorld();
        Chunk centerChunk = player.getLocation().getChunk();
        int centerCX = centerChunk.getX();
        int centerCZ = centerChunk.getZ();

        List<BlockSnapshot> snapshots = new ArrayList<>();
        int removedCount = 0;
        int preservedCount = 0;

        int minY = Math.max(world.getMinHeight(), player.getLocation().getBlockY() - 40);
        int maxY = Math.min(world.getMaxHeight() - 1, player.getLocation().getBlockY() + 40);

        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                int cx = centerCX + dx;
                int cz = centerCZ + dz;

                int minX = cx << 4;
                int maxX = minX + 15;
                int minZ = cz << 4;
                int maxZ = minZ + 15;

                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        for (int y = minY; y <= maxY; y++) {
                            Block b = world.getBlockAt(x, y, z);
                            if (b.getType() != Material.GLASS) {
                                continue;
                            }

                            boolean touchesWater = touchesAnyWater(world, x, y, z);

                            boolean shouldRemove = false;
                            if (mode.equals("ocean")) {
                                // Only remove glass that actively borders water/ocean
                                shouldRemove = touchesWater;
                            } else if (mode.equals("safe")) {
                                // Only remove glass that does NOT border water (preserve ocean barrier)
                                shouldRemove = !touchesWater;
                            } else if (mode.equals("all")) {
                                shouldRemove = true;
                            }

                            if (shouldRemove) {
                                snapshots.add(new BlockSnapshot(b.getLocation(), b.getBlockData().clone()));
                                b.setType(Material.AIR, false);
                                removedCount++;
                            } else {
                                preservedCount++;
                            }
                        }
                    }
                }
            }
        }

        undoHistory.put(player.getUniqueId(), snapshots);

        ChatUtil.sendMessage(player, "§a§lCLEAN §8» §aGlas opgeruimd! §7(Modus: §e" + mode + "§7, Straal: §e" + chunkRadius + " chunks§7)");
        ChatUtil.sendMessage(player, "§7» §b" + removedCount + " §7glasblokken verwijderd (lucht)");
        if (preservedCount > 0) {
            ChatUtil.sendMessage(player, "§7» §a" + preservedCount + " §7overige glasblokken behouden");
        }
        ChatUtil.sendMessage(player, "§7» §eTyp §6/cleanglass undo §eom dit ongedaan te maken.");
        ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

        return true;
    }

    private boolean touchesAnyWater(World world, int x, int y, int z) {
        if (y + 1 < world.getMaxHeight() && isWater(world.getBlockAt(x, y + 1, z))) return true;
        if (y - 1 >= world.getMinHeight() && isWater(world.getBlockAt(x, y - 1, z))) return true;
        if (isWater(world.getBlockAt(x + 1, y, z))) return true;
        if (isWater(world.getBlockAt(x - 1, y, z))) return true;
        if (isWater(world.getBlockAt(x, y, z + 1))) return true;
        if (isWater(world.getBlockAt(x, y, z - 1))) return true;

        // Also check diagonal horizontal blocks
        if (isWater(world.getBlockAt(x + 1, y, z + 1))) return true;
        if (isWater(world.getBlockAt(x + 1, y, z - 1))) return true;
        if (isWater(world.getBlockAt(x - 1, y, z + 1))) return true;
        if (isWater(world.getBlockAt(x - 1, y, z - 1))) return true;

        // Also check if in ocean/river biome below sea level
        if (y <= 64) {
            Biome biome = world.getBiome(x, y, z);
            String name = biome.getKey().getKey();
            if (name.contains("ocean") || name.contains("river")) {
                return true;
            }
        }

        return false;
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

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("ocean");
            completions.add("safe");
            completions.add("all");
            completions.add("0");
            completions.add("1");
            completions.add("2");
            completions.add("undo");
        } else if (args.length == 2) {
            completions.add("0");
            completions.add("1");
            completions.add("2");
        }
        return completions;
    }
}
