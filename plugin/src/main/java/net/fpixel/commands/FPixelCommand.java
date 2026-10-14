package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.user.FPixelUser;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.game.Arena;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FPixelCommand implements CommandExecutor, TabCompleter {
    private final FPixelPlugin plugin;

    public FPixelCommand(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fpixel.admin")) {
            sender.sendMessage(ChatUtil.colorize("§cYou do not have permission to execute this command."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("setlobby") && sender instanceof Player player) {
            plugin.getLobbyManager().setSpawn(player.getLocation());
            ChatUtil.sendMessage(player, "§aLobby spawn point updated to your current location!");
        } else if (sub.equals("reload")) {
            plugin.reloadConfig();
            plugin.getRankManager().loadRanks();
            plugin.getLobbyManager().loadSpawn();
            sender.sendMessage(ChatUtil.colorize("§aFPixel configuration and ranks reloaded successfully!"));
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

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatUtil.colorize("§6§lFPixel Admin Commands:"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel setlobby §7- Set hub spawn point"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel list §7- List all minigame arenas"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel addcoins <player> <amount> §7- Give coins"));
        sender.sendMessage(ChatUtil.colorize(" §e/fpixel reload §7- Reload configuration"));
        sender.sendMessage(ChatUtil.colorize(" §e/setrank <player> <rank> §7- Change player rank"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = Arrays.asList("setlobby", "list", "addcoins", "reload");
            List<String> filtered = new ArrayList<>();
            for (String s : list) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                    filtered.add(s);
                }
            }
            return filtered;
        }
        return new ArrayList<>();
    }
}
