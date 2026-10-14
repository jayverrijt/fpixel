package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.rank.Rank;
import net.fpixel.core.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RankCommand implements CommandExecutor, TabCompleter {
    private final FPixelPlugin plugin;

    public RankCommand(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fpixel.admin")) {
            sender.sendMessage(ChatUtil.colorize("§cYou do not have permission to execute this command."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatUtil.colorize("§cUsage: /setrank <player> <OWNER|ADMIN|MOD|VIP_PLUS|VIP|DEFAULT>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatUtil.colorize("§cPlayer not found."));
            return true;
        }

        try {
            Rank rank = Rank.valueOf(args[1].toUpperCase());
            plugin.getRankManager().setRank(target, rank);
            sender.sendMessage(ChatUtil.colorize("§aSet " + target.getName() + "'s rank to " + rank.getColor() + rank.name() + "§a."));
            ChatUtil.sendMessage(target, "§aYour rank has been updated to " + rank.getColor() + rank.name() + "§a!");
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatUtil.colorize("§cInvalid rank! Valid ranks: " + Arrays.toString(Rank.values())));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(p.getName());
                }
            }
            return list;
        } else if (args.length == 2) {
            List<String> list = new ArrayList<>();
            for (Rank r : Rank.values()) {
                if (r.name().toLowerCase().startsWith(args[1].toLowerCase())) {
                    list.add(r.name());
                }
            }
            return list;
        }
        return new ArrayList<>();
    }
}
