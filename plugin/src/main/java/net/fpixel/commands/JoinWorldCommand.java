package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class JoinWorldCommand implements CommandExecutor, TabCompleter {
    private final FPixelPlugin plugin;

    public JoinWorldCommand(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDit commando kan alleen door een speler worden uitgevoerd.");
            return true;
        }

        if (plugin.getPlayerWorldManager() == null) {
            ChatUtil.sendMessage(player, "§cPlayer Worlds is momenteel niet geladen.");
            return true;
        }

        if (args.length == 0) {
            plugin.getPlayerWorldManager().openPlayerWorldsMenu(player);
            return true;
        }

        String uid = args[0].toUpperCase();
        plugin.getPlayerWorldManager().teleportPlayerToWorld(player, uid);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && sender instanceof Player player && plugin.getPlayerWorldManager() != null) {
            List<String> list = new ArrayList<>();
            for (var pw : plugin.getPlayerWorldManager().getAccessibleWorlds(player)) {
                if (pw.getUid().toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(pw.getUid());
                }
            }
            return list;
        }
        return List.of();
    }
}
