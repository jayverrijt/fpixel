package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
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
                Player target = others.get(0);
                sendDuelChoices(player, target);
                return true;
            }

            ChatUtil.sendMessage(player, "§6Usage: §e/duel <player> [bridge|sumo|parkour|bedwars|skywars]");
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

        if (args.length == 1) {
            sendDuelChoices(player, target);
            return true;
        }

        String type = args[1].toUpperCase();
        if (!Arrays.asList("BRIDGE", "SUMO", "PARKOUR", "BEDWARS", "SKYWARS").contains(type)) {
            type = "BRIDGE";
        }

        plugin.getDuelManager().sendChallenge(player, target, type);
        return true;
    }

    private void sendDuelChoices(Player player, Player target) {
        ChatUtil.sendMessage(player, "§6§lDUELS §8» §eSelect a duel mode to challenge §a" + target.getName() + "§e:");

        Component choices = Component.text(" ")
                .append(createChoiceButton(target.getName(), "BRIDGE", "§6§l[THE BRIDGE]"))
                .append(Component.text(" "))
                .append(createChoiceButton(target.getName(), "SUMO", "§e§l[SUMO]"))
                .append(Component.text(" "))
                .append(createChoiceButton(target.getName(), "PARKOUR", "§a§l[PARKOUR]"))
                .append(Component.text(" "))
                .append(createChoiceButton(target.getName(), "SKYWARS", "§b§l[SKYWARS]"))
                .append(Component.text(" "))
                .append(createChoiceButton(target.getName(), "BEDWARS", "§c§l[BEDWARS]"));

        player.sendMessage(choices);
    }

    private Component createChoiceButton(String target, String mode, String text) {
        return Component.text(text)
                .clickEvent(ClickEvent.runCommand("/duel " + target + " " + mode.toLowerCase()))
                .hoverEvent(HoverEvent.showText(Component.text("§eClick to challenge in §a" + mode + "!")));
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
            return Arrays.asList("bridge", "sumo", "parkour", "bedwars", "skywars");
        }
        return new ArrayList<>();
    }
}
