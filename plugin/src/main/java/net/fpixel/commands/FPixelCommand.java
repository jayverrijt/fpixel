package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.chunk.FPixelChunkHandler;
import net.fpixel.core.rank.Rank;
import net.fpixel.core.user.FPixelUser;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.game.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FPixelCommand implements CommandExecutor, TabCompleter {
    private final FPixelPlugin plugin;
    private final FPixelChunkHandler chunkHandler;

    public FPixelCommand(FPixelPlugin plugin, FPixelChunkHandler chunkHandler) {
        this.plugin = plugin;
        this.chunkHandler = chunkHandler;
    }

    private boolean checkPermission(CommandSender sender) {
        if (sender.hasPermission("fpixel.admin") || sender.isOp()) return true;
        if (sender instanceof Player player) {
            Rank rank = plugin.getRankManager().getRank(player);
            return rank != null && rank.getWeight() >= 90;
        }
        return false;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] rawArgs) {
        if (!checkPermission(sender)) {
            sender.sendMessage(ChatUtil.colorize("§cYou do not have permission to execute this command."));
            return true;
        }

        String cmdLabel = label.toLowerCase();
        if (cmdLabel.startsWith("/")) cmdLabel = cmdLabel.substring(1);

        String[] args;
        if (cmdLabel.contains("fill")) {
            args = new String[rawArgs.length + 1];
            args[0] = "fill";
            System.arraycopy(rawArgs, 0, args, 1, rawArgs.length);
        } else if (cmdLabel.contains("empty")) {
            args = new String[rawArgs.length + 1];
            args[0] = "empty";
            System.arraycopy(rawArgs, 0, args, 1, rawArgs.length);
        } else if (cmdLabel.equals("undo")) {
            args = new String[]{"undo"};
        } else {
            args = rawArgs;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        // ------------------------------------------
        // UNIFIED /fpixel fill
        // ------------------------------------------
        if (sub.equals("fill")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Alleen spelers kunnen /fpixel fill gebruiken.");
                return true;
            }

            if (args.length < 3) {
                ChatUtil.sendMessage(player, "§c§lCHUNK §8» §cGebruik: §e/fpixel fill <startY> <endY> [blok] [all|safe|oneside]");
                return true;
            }

            int playerY = player.getLocation().getBlockY();
            int fromY, toY;
            try {
                fromY = parseY(args[1], playerY);
                toY = parseY(args[2], playerY);
            } catch (NumberFormatException e) {
                ChatUtil.sendMessage(player, "§c§lCHUNK §8» §cOngeldige Y-waarden! Gebruik getallen of ~ (bijv. 52 54)");
                return true;
            }

            int minY = Math.max(player.getWorld().getMinHeight(), Math.min(fromY, toY));
            int maxY = Math.min(player.getWorld().getMaxHeight() - 1, Math.max(fromY, toY));

            Material fillMaterial = Material.STONE;
            String mode = "all";

            for (int i = 3; i < args.length; i++) {
                String a = args[i].toLowerCase();
                if (a.equals("all") || a.equals("safe") || a.equals("oneside")) {
                    mode = a;
                    continue;
                }
                Material m = Material.matchMaterial(args[i]);
                if (m != null && m.isBlock()) {
                    fillMaterial = m;
                }
            }

            Chunk chunk;
            Block targetLook = player.getTargetBlockExact(64);
            if (targetLook != null && player.isSneaking()) {
                chunk = targetLook.getChunk();
            } else {
                chunk = player.getLocation().getChunk();
            }

            if (mode.equals("safe")) {
                chunkHandler.fillSafe(player, chunk, minY, maxY, fillMaterial);
            } else if (mode.equals("oneside")) {
                chunkHandler.fillOneSide(player, chunk, minY, maxY, fillMaterial);
            } else {
                chunkHandler.fillAll(player, chunk, minY, maxY, fillMaterial);
            }
            return true;
        }

        // ------------------------------------------
        // UNIFIED /fpixel empty
        // ------------------------------------------
        if (sub.equals("empty")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Alleen spelers kunnen /fpixel empty gebruiken.");
                return true;
            }

            Chunk chunk;
            Block targetLook = player.getTargetBlockExact(64);
            if (targetLook != null && player.isSneaking()) {
                chunk = targetLook.getChunk();
            } else {
                chunk = player.getLocation().getChunk();
            }

            if (args.length == 1) {
                chunkHandler.emptyAll(player, chunk);
                return true;
            }

            String option = args[1].toLowerCase();
            if (option.equals("safe")) {
                chunkHandler.emptySafe(player, chunk);
            } else if (option.equals("ignore-water") || option.equals("ignorewater") || option.equals("keepwater")) {
                chunkHandler.emptyIgnoreWater(player, chunk);
            } else if (option.equals("ignore-water-and-box") || option.equals("ignorewaterandbox") || option.equals("boxwater")) {
                chunkHandler.emptyIgnoreWaterAndBox(player, chunk);
            } else if (option.equals("glass")) {
                chunkHandler.emptyGlass(player, chunk);
            } else if (option.equals("sponge") || option.equals("sponges")) {
                chunkHandler.emptySponge(player, chunk);
            } else {
                ChatUtil.sendMessage(player, "§c§lCHUNK §8» §cOnbekende optie: §e" + args[1]);
                ChatUtil.sendMessage(player, "§7» §eOpties: §fsafe, ignore-water, ignore-water-and-box, glass, sponge");
            }
            return true;
        }

        // ------------------------------------------
        // UNIFIED /fpixel undo
        // ------------------------------------------
        if (sub.equals("undo")) {
            if (sender instanceof Player player) {
                chunkHandler.undo(player);
            } else {
                sender.sendMessage("Alleen spelers kunnen undo gebruiken.");
            }
            return true;
        }

        // ------------------------------------------
        // Network Admin Commands
        // ------------------------------------------
        if (sub.equals("setlobby") && sender instanceof Player player) {
            plugin.getLobbyManager().setSpawn(player.getLocation());
            ChatUtil.sendMessage(player, "§aLobby spawn point updated to your current location!");
        } else if (sub.equals("reload")) {
            plugin.reloadConfig();
            plugin.getRankManager().loadRanks();
            plugin.getLobbyManager().loadSpawn();
            sender.sendMessage(ChatUtil.colorize("§aFPixel configuration and ranks reloaded successfully!"));
        } else if (sub.equals("rebuildhub") || sub.equals("hub")) {
            sender.sendMessage(ChatUtil.colorize("§eRebuilding Grand Hub with NPCs, launchpads, and survival-inspired architecture..."));
            plugin.getWorldManager().rebuildGrandHub(Bukkit.getWorlds().get(0));
            sender.sendMessage(ChatUtil.colorize("§aGrand Hub rebuild complete! All 8 gamemode pavilions, launchpads, and NPCs are live."));
        } else if (sub.equals("buildmaps")) {
            sender.sendMessage(ChatUtil.colorize("§eBuilding Grand Hub and 30 custom minigame maps..."));
            plugin.getWorldManager().generateAllMaps(Bukkit.getWorlds().get(0));
            sender.sendMessage(ChatUtil.colorize("§aAll 30 maps generated and registered successfully!"));
        } else if (sub.equals("list")) {
            sender.sendMessage(ChatUtil.colorize("§6§lActive Arenas:"));
            for (Arena arena : plugin.getGameManager().getArenas()) {
                sender.sendMessage(ChatUtil.colorize(" §e• §f" + arena.getId() + " §7(" + arena.getGameType() + "): "
                        + arena.getState().getDisplay() + " §7[" + arena.getPlayerCount() + "/" + arena.getMaxPlayers() + "]"));
            }
        } else if (sub.equals("addcoins") && args.length >= 3) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatUtil.colorize("§cPlayer not found."));
                return true;
            }
            try {
                int amount = Integer.parseInt(args[2]);
                FPixelUser user = plugin.getUserManager().getUser(target);
                user.addCoins(amount);
                sender.sendMessage(ChatUtil.colorize("§aGave " + amount + " coins to " + target.getName() + "."));
                ChatUtil.sendMessage(target, "§6+" + amount + " Coins §7added to your balance!");
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatUtil.colorize("§cInvalid amount."));
            }
        } else {
            sendHelp(sender);
        }

        return true;
    }

    private int parseY(String arg, int playerY) {
        if (arg.equals("~")) return playerY;
        if (arg.startsWith("~")) return playerY + Integer.parseInt(arg.substring(1));
        return Integer.parseInt(arg);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatUtil.colorize("§6§lFPixel Unified Commands:"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel fill <vanY> <totY> [blok] [all|safe|oneside] §7- Vul chunk"));
        sender.sendMessage(ChatUtil.colorize("   §f• all: §7vult hele chunk tussen Y hoogtes"));
        sender.sendMessage(ChatUtil.colorize("   §f• safe: §7behoudt geplaatste blokken"));
        sender.sendMessage(ChatUtil.colorize("   §f• oneside: §7stopt per Y-laag bij afwijkend blok/muur"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel empty [safe|ignore-water|ignore-water-and-box|glass|sponge] §7- Maak chunk leeg"));
        sender.sendMessage(ChatUtil.colorize("   §f• (leeg): §7maakt hele chunk leeg"));
        sender.sendMessage(ChatUtil.colorize("   §f• safe: §7verwijdert alles boven de speler Y"));
        sender.sendMessage(ChatUtil.colorize("   §f• ignore-water: §7behoudt water"));
        sender.sendMessage(ChatUtil.colorize("   §f• ignore-water-and-box: §7behoudt water en plaatst glazen wand"));
        sender.sendMessage(ChatUtil.colorize("   §f• glass: §7verwijdert alle glas in chunk"));
        sender.sendMessage(ChatUtil.colorize("   §f• sponge: §7verwijdert alle sponzen in chunk"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel undo §7- Herstel laatste actie"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel rebuildhub §7- Rebuild Hub"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel setlobby §7- Set hub spawn point"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] rawArgs) {
        String cmdLabel = alias.toLowerCase();
        if (cmdLabel.startsWith("/")) cmdLabel = cmdLabel.substring(1);

        String[] args;
        if (cmdLabel.contains("fill")) {
            args = new String[rawArgs.length + 1];
            args[0] = "fill";
            System.arraycopy(rawArgs, 0, args, 1, rawArgs.length);
        } else if (cmdLabel.contains("empty")) {
            args = new String[rawArgs.length + 1];
            args[0] = "empty";
            System.arraycopy(rawArgs, 0, args, 1, rawArgs.length);
        } else {
            args = rawArgs;
        }

        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.addAll(Arrays.asList("fill", "empty", "undo", "rebuildhub", "setlobby", "buildmaps", "list", "addcoins", "reload"));
        } else if (args[0].equalsIgnoreCase("fill")) {
            if (args.length == 2) {
                list.add("52");
                list.add("~");
            } else if (args.length == 3) {
                list.add("54");
                list.add("55");
                list.add("81");
            } else if (args.length == 4) {
                list.add("stone");
                list.add("deepslate");
                list.add("dirt");
                list.add("glass");
                list.add("all");
                list.add("safe");
                list.add("oneside");
            } else if (args.length == 5) {
                list.add("all");
                list.add("safe");
                list.add("oneside");
            }
        } else if (args[0].equalsIgnoreCase("empty")) {
            if (args.length == 2) {
                list.add("safe");
                list.add("ignore-water");
                list.add("ignore-water-and-box");
                list.add("glass");
                list.add("sponge");
            }
        }

        List<String> filtered = new ArrayList<>();
        String current = args[args.length - 1].toLowerCase();
        for (String s : list) {
            if (s.toLowerCase().startsWith(current)) {
                filtered.add(s);
            }
        }
        return filtered;
    }
}
