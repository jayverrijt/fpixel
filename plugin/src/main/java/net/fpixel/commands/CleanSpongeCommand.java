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
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CleanSpongeCommand implements CommandExecutor, TabCompleter {
    private final FPixelPlugin plugin;
    private final Map<UUID, List<BlockSnapshot>> undoHistory = new HashMap<>();

    public CleanSpongeCommand(FPixelPlugin plugin) {
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
            ChatUtil.sendMessage(player, "§a§lCLEAN §8» §aLaatste actie hersteld! §7(" + snapshots.size() + " blokken)");
            ChatUtil.playSound(player, Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
            return true;
        }

        int chunkRadius = 1; // Default: current chunk + 1 chunk around (3x3 chunks) to catch all lines of sponge
        if (args.length > 0) {
            try {
                chunkRadius = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }
        chunkRadius = Math.max(0, Math.min(5, chunkRadius));

        World world = player.getWorld();
        Chunk centerChunk = player.getLocation().getChunk();
        int centerCX = centerChunk.getX();
        int centerCZ = centerChunk.getZ();

        List<BlockSnapshot> snapshots = new ArrayList<>();
        int spongeCount = 0;
        int wetSpongeCount = 0;
        int glassCount = 0;

        int minY = Math.max(world.getMinHeight(), player.getLocation().getBlockY() - 35);
        int maxY = Math.min(world.getMaxHeight() - 1, player.getLocation().getBlockY() + 35);

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
                            Material mat = b.getType();

                            if (mat == Material.SPONGE) {
                                snapshots.add(new BlockSnapshot(b.getLocation(), b.getBlockData().clone()));
                                b.setType(Material.AIR, false);
                                spongeCount++;
                            } else if (mat == Material.WET_SPONGE) {
                                snapshots.add(new BlockSnapshot(b.getLocation(), b.getBlockData().clone()));
                                b.setType(Material.AIR, false);
                                wetSpongeCount++;
                            } else if (mat == Material.GLASS) {
                                snapshots.add(new BlockSnapshot(b.getLocation(), b.getBlockData().clone()));
                                b.setType(Material.AIR, false);
                                glassCount++;
                            }
                        }
                    }
                }
            }
        }

        undoHistory.put(player.getUniqueId(), snapshots);

        int total = spongeCount + wetSpongeCount + glassCount;
        ChatUtil.sendMessage(player, "§a§lCLEAN §8» §aOpgeruimd! §7(Totaal: §e" + total + " §7blokken)");
        if (spongeCount > 0) ChatUtil.sendMessage(player, "§7» §e" + spongeCount + " §7droge sponzen verwijderd");
        if (wetSpongeCount > 0) ChatUtil.sendMessage(player, "§7» §6" + wetSpongeCount + " §7natte sponzen verwijderd");
        if (glassCount > 0) ChatUtil.sendMessage(player, "§7» §b" + glassCount + " §7glasblokken verwijderd");
        ChatUtil.sendMessage(player, "§7» §eTyp §6/cleansponge undo §eom te herstellen.");
        ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("0"); // Current chunk only
            completions.add("1"); // 3x3 chunks
            completions.add("2"); // 5x5 chunks
            completions.add("undo");
        }
        return completions;
    }
}
