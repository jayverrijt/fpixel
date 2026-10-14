package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JoinCommand implements CommandExecutor, TabCompleter {
    private final FPixelPlugin plugin;

    public JoinCommand(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can join minigames.");
            return true;
        }

        if (args.length < 1) {
            ChatUtil.sendMessage(player, "§cUsage: /join <bedwars|skywars|tntrun|bridge|sumo|parkour>");
            return true;
        }

        String gameType = args[0].toUpperCase();
        plugin.getGameManager().joinQueue(player, gameType);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = Arrays.asList("bedwars", "skywars", "tntrun", "bridge", "sumo", "parkour");
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
