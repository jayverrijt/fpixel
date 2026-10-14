package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
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

public class DuelsCommand implements CommandExecutor, TabCompleter {
    private final FPixelPlugin plugin;

    public DuelsCommand(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can challenge to duels.");
            return true;
        }

        if (args.length == 0) {
            // Find other players
            List<Player> others = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(player)) others.add(p);
            }

            if (others.isEmpty()) {
                ChatUtil.sendMessage(player, "§cNo other players online to duel!");
                return true;
            }

            if (others.size() == 1) {
                // Exactly 1 other player, duel them!
                plugin.getDuelManager().sendChallenge(player, others.get(0), "SUMO");
                return true;
            }

            ChatUtil.sendMessage(player, "§6Usage: §e/duel <player> [sumo|bedwars]");
            return true;
        }

        if (args[0].equalsIgnoreCase("accept") && args.length >= 2) {
            plugin.getDuelManager().acceptChallenge(player, args[1]);
            return true;
        }

        if (args[0].equalsIgnoreCase("decline") && args.length >= 2) {
            plugin.getDuelManager().declineChallenge(player, args[1]);
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            ChatUtil.sendMessage(player, "§cPlayer not found.");
            return true;
        }

        String type = args.length >= 2 ? args[1].toUpperCase() : "SUMO";
        if (!type.equals("SUMO") && !type.equals("BEDWARS")) {
            type = "SUMO";
        }

        plugin.getDuelManager().sendChallenge(player, target, type);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add("accept");
            list.add("decline");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getName().equalsIgnoreCase(sender.getName()) && p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(p.getName());
                }
            }
            return list;
        } else if (args.length == 2) {
            return Arrays.asList("sumo", "bedwars");
        }
        return new ArrayList<>();
    }
}
