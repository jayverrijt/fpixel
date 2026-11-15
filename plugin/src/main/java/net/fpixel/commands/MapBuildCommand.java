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

public class MapBuildCommand implements CommandExecutor, TabCompleter {
    private final FPixelPlugin plugin;

    public MapBuildCommand(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDit commando kan alleen door spelers worden uitgevoerd.");
            return true;
        }

        if (plugin.getMapMakerManager() == null) {
            ChatUtil.sendMessage(player, "§cMap Maker is momenteel niet beschikbaar.");
            return true;
        }

        if (args.length < 1) {
            ChatUtil.sendMessage(player, "§cGebruik: /mapbuild <map-id>");
            return true;
        }

        plugin.getMapMakerManager().enterBuildMode(player, args[0]);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && sender instanceof Player player && plugin.getMapMakerManager() != null) {
            List<String> list = new ArrayList<>();
            for (var m : plugin.getMapMakerManager().getMapsByCreator(player.getName())) {
                if (m.getId().toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(m.getId());
                }
            }
            return list;
        }
        return List.of();
    }
}
