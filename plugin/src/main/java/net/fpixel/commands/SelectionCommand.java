package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.rank.Rank;
import net.fpixel.core.selection.SelectionManager;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.game.rollback.BlockSnapshot;
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

public class SelectionCommand implements CommandExecutor, TabCompleter {
    private final FPixelPlugin plugin;
    private final SelectionManager selectionManager;

    public SelectionCommand(FPixelPlugin plugin, SelectionManager selectionManager) {
        this.plugin = plugin;
        this.selectionManager = selectionManager;
    }

    private boolean checkPermission(Player player) {
        if (player.hasPermission("fpixel.admin") || player.isOp()) return true;
        Rank rank = plugin.getRankManager().getRank(player);
        return rank != null && rank.getWeight() >= 90;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Alleen spelers kunnen dit commando gebruiken.");
            return true;
        }

        if (!checkPermission(player)) {
            ChatUtil.sendMessage(player, "§c§lWE §8» §cGeen permissie om dit commando uit te voeren!");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        String cmd = label.toLowerCase();
        if (cmd.startsWith("/")) cmd = cmd.substring(1);

        if (cmd.equals("wand") || cmd.equals("selectwand")) {
            player.getInventory().addItem(selectionManager.createWandItem());
            ChatUtil.sendMessage(player, "§6§lWE §8» §aSelectie Bijl ontvangen!");
            ChatUtil.sendMessage(player, "§7» §eLinksklik §7op een blok voor §ePositie 1");
            ChatUtil.sendMessage(player, "§7» §eRechtsklik §7op een blok voor §ePositie 2");
            ChatUtil.sendMessage(player, "§7» §aTyp daarna §6//set <blok> [safe] §aom precies die bak te vullen!");
            ChatUtil.playSound(player, Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1.0f, 1.2f);
            return true;
        }

        if (cmd.equals("pos1") || cmd.equals("hpos1")) {
            Location loc;
            if (args.length >= 3) {
                try {
                    int x = Integer.parseInt(args[0]);
                    int y = Integer.parseInt(args[1]);
                    int z = Integer.parseInt(args[2]);
                    loc = new Location(player.getWorld(), x, y, z);
                } catch (NumberFormatException e) {
                    ChatUtil.sendMessage(player, "§c§lWE §8» §cOngeldige coördinaten. Gebruik: §e//pos1 [x y z]");
                    return true;
                }
            } else {
                Block target = player.getTargetBlockExact(64);
                loc = (target != null && cmd.equals("hpos1")) ? target.getLocation() : player.getLocation().getBlock().getLocation();
                if (target != null && !cmd.equals("hpos1") && player.isSneaking()) {
                    loc = target.getLocation();
                }
            }

            selectionManager.setPos1(player.getUniqueId(), loc);
            int count = selectionManager.getSelectionBlockCount(player.getUniqueId());
            ChatUtil.sendMessage(player, "§6§lWE §8» §aEerste positie (§ePos 1§a) ingesteld op §e" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
                    + (count > 0 ? " §7(§f" + count + " §7blokken)" : ""));
            ChatUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            return true;
        }

        if (cmd.equals("pos2") || cmd.equals("hpos2")) {
            Location loc;
            if (args.length >= 3) {
                try {
                    int x = Integer.parseInt(args[0]);
                    int y = Integer.parseInt(args[1]);
                    int z = Integer.parseInt(args[2]);
                    loc = new Location(player.getWorld(), x, y, z);
                } catch (NumberFormatException e) {
                    ChatUtil.sendMessage(player, "§c§lWE §8» §cOngeldige coördinaten. Gebruik: §e//pos2 [x y z]");
                    return true;
                }
            } else {
                Block target = player.getTargetBlockExact(64);
                loc = (target != null && cmd.equals("hpos2")) ? target.getLocation() : player.getLocation().getBlock().getLocation();
                if (target != null && !cmd.equals("hpos2") && player.isSneaking()) {
                    loc = target.getLocation();
                }
            }

            selectionManager.setPos2(player.getUniqueId(), loc);
            int count = selectionManager.getSelectionBlockCount(player.getUniqueId());
            ChatUtil.sendMessage(player, "§6§lWE §8» §aTweede positie (§ePos 2§a) ingesteld op §e" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
                    + (count > 0 ? " §7(§f" + count + " §7blokken)" : ""));
            ChatUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            return true;
        }

        if (cmd.equals("undo") || cmd.equals("weundo")) {
            List<BlockSnapshot> snapshots = selectionManager.getUndo(player.getUniqueId());
            if (snapshots == null || snapshots.isEmpty()) {
                ChatUtil.sendMessage(player, "§c§lWE §8» §cGeen actie gevonden om ongedaan te maken!");
                ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return true;
            }

            for (BlockSnapshot snapshot : snapshots) {
                snapshot.restore();
            }
            ChatUtil.sendMessage(player, "§a§lWE §8» §aLaatste actie hersteld! §7(" + snapshots.size() + " blokken)");
            ChatUtil.playSound(player, Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
            return true;
        }

        if (cmd.equals("set") || cmd.equals("fillregion") || cmd.equals("regionfill")) {
            if (!selectionManager.hasSelection(player.getUniqueId())) {
                ChatUtil.sendMessage(player, "§c§lWE §8» §cJe hebt nog geen selectie gemaakt!");
                ChatUtil.sendMessage(player, "§7» §eGebruik §6//wand §eof stel §6//pos1 §een §6//pos2 §ein.");
                ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return true;
            }

            if (args.length == 0) {
                ChatUtil.sendMessage(player, "§c§lWE §8» §cGebruik: §e//set <materiaal> [safe]");
                return true;
            }

            boolean isSafe = false;
            Material fillMaterial = null;

            for (String arg : args) {
                if (arg.equalsIgnoreCase("safe") || arg.equalsIgnoreCase("save")
                        || arg.equalsIgnoreCase("keep") || arg.equalsIgnoreCase("safemode")) {
                    isSafe = true;
                    continue;
                }
                Material m = Material.matchMaterial(arg);
                if (m != null && m.isBlock()) {
                    fillMaterial = m;
                }
            }

            if (fillMaterial == null) {
                ChatUtil.sendMessage(player, "§c§lWE §8» §cOngeldig blok type: §e" + args[0]);
                return true;
            }

            Location p1 = selectionManager.getPos1(player.getUniqueId());
            Location p2 = selectionManager.getPos2(player.getUniqueId());
            World world = p1.getWorld();

            int minX = Math.min(p1.getBlockX(), p2.getBlockX());
            int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
            int minY = Math.max(world.getMinHeight(), Math.min(p1.getBlockY(), p2.getBlockY()));
            int maxY = Math.min(world.getMaxHeight() - 1, Math.max(p1.getBlockY(), p2.getBlockY()));
            int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
            int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

            List<BlockSnapshot> snapshots = new ArrayList<>();
            int count = 0;
            int skipped = 0;

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        Block block = world.getBlockAt(x, y, z);
                        Material type = block.getType();

                        if (type == Material.BEDROCK) continue;

                        if (isSafe && !isReplaceable(block)) {
                            skipped++;
                            continue;
                        }

                        if (type != fillMaterial) {
                            snapshots.add(new BlockSnapshot(block.getLocation(), block.getBlockData().clone()));
                            block.setType(fillMaterial, false);
                            count++;
                        }
                    }
                }
            }

            selectionManager.saveUndo(player.getUniqueId(), snapshots);
            ChatUtil.sendMessage(player, "§a§lWE §8» §aSuccesvol §f" + count + " §ablokken gevuld met §e" + fillMaterial.name().toLowerCase() + "§a!"
                    + (isSafe ? " §7(§e" + skipped + " §7bestaande blokken beschermd)" : ""));
            ChatUtil.sendMessage(player, "§7» §eTyp §6//undo §eom dit ongedaan te maken.");
            ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            return true;
        }

        if (cmd.equals("replace") || cmd.equals("replaceregion")) {
            if (!selectionManager.hasSelection(player.getUniqueId())) {
                ChatUtil.sendMessage(player, "§c§lWE §8» §cJe hebt nog geen selectie gemaakt!");
                ChatUtil.sendMessage(player, "§7» §eGebruik §6//wand §eof stel §6//pos1 §een §6//pos2 §ein.");
                return true;
            }

            if (args.length < 2) {
                ChatUtil.sendMessage(player, "§c§lWE §8» §cGebruik: §e//replace <vanBlok> <naarBlok>");
                return true;
            }

            Material fromMat = Material.matchMaterial(args[0]);
            Material toMat = Material.matchMaterial(args[1]);

            if (fromMat == null || !fromMat.isBlock()) {
                ChatUtil.sendMessage(player, "§c§lWE §8» §cOngeldig bron blok: §e" + args[0]);
                return true;
            }
            if (toMat == null || !toMat.isBlock()) {
                ChatUtil.sendMessage(player, "§c§lWE §8» §cOngeldig doel blok: §e" + args[1]);
                return true;
            }

            Location p1 = selectionManager.getPos1(player.getUniqueId());
            Location p2 = selectionManager.getPos2(player.getUniqueId());
            World world = p1.getWorld();

            int minX = Math.min(p1.getBlockX(), p2.getBlockX());
            int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
            int minY = Math.max(world.getMinHeight(), Math.min(p1.getBlockY(), p2.getBlockY()));
            int maxY = Math.min(world.getMaxHeight() - 1, Math.max(p1.getBlockY(), p2.getBlockY()));
            int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
            int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

            List<BlockSnapshot> snapshots = new ArrayList<>();
            int count = 0;

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        Block block = world.getBlockAt(x, y, z);
                        if (block.getType() == fromMat) {
                            snapshots.add(new BlockSnapshot(block.getLocation(), block.getBlockData().clone()));
                            block.setType(toMat, false);
                            count++;
                        }
                    }
                }
            }

            selectionManager.saveUndo(player.getUniqueId(), snapshots);
            ChatUtil.sendMessage(player, "§a§lWE §8» §aSuccesvol §f" + count + " §ablokken van §e" + fromMat.name().toLowerCase() + " §anaar §e" + toMat.name().toLowerCase() + " §avervangen!");
            ChatUtil.sendMessage(player, "§7» §eTyp §6//undo §eom dit ongedaan te maken.");
            ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            return true;
        }

        return false;
    }

    private boolean isReplaceable(Block block) {
        if (block == null) return false;
        Material type = block.getType();
        if (type.isAir()) return true;
        if (type == Material.WATER || type == Material.BUBBLE_COLUMN || type == Material.LAVA) return false;
        if (block.getBlockData() instanceof Waterlogged wl && wl.isWaterlogged()) return false;
        return block.isReplaceable();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        String cmd = label.toLowerCase();
        if (cmd.startsWith("/")) cmd = cmd.substring(1);

        if (cmd.equals("set")) {
            if (args.length == 1) {
                list.add("stone");
                list.add("deepslate");
                list.add("dirt");
                list.add("air");
                list.add("glass");
                list.add("smooth_stone");
            } else if (args.length == 2) {
                list.add("safe");
            }
        } else if (cmd.equals("replace")) {
            if (args.length == 1 || args.length == 2) {
                list.add("air");
                list.add("stone");
                list.add("dirt");
                list.add("water");
                list.add("sponge");
                list.add("glass");
            }
        }
        return list;
    }
}
