package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.lobby.menus.MapSelectMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapsCommand implements CommandExecutor, TabCompleter {
    private final FPixelPlugin plugin;

    public MapsCommand(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can execute this command.");
            return true;
        }

        if (args.length < 1) {
            ChatUtil.sendMessage(player, "§cUsage: /maps <bedwars|tntrun|sumo>");
            return true;
        }

        String gameType = args[0].toUpperCase();
        if (!gameType.equals("BEDWARS") && !gameType.equals("TNTRUN") && !gameType.equals("SUMO")) {
            ChatUtil.sendMessage(player, "§cInvalid minigame! Choose from: bedwars, tntrun, sumo");
            return true;
        }

        MapSelectMenu.open(plugin, player, gameType);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = Arrays.asList("bedwars", "tntrun", "sumo");
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
