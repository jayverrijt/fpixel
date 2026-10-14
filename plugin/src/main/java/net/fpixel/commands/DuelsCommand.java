package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.game.Arena;
import net.fpixel.game.GameState;
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
            // Find other online players
            List<Player> others = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(player)) others.add(p);
            }

            if (others.isEmpty()) {
                if (plugin.getTestingManager() != null && plugin.getTestingManager().isTestingMode()) {
                    sendDuelChoices(player, "bot");
                    return true;
                }
                ChatUtil.sendMessage(player, "§cNo other players online to duel! (Tip: Enable §e/testing on §cto duel bots)");
                return true;
            }

            if (others.size() == 1) {
                Player target = others.get(0);
                sendDuelChoices(player, target.getName());
                return true;
            }

            ChatUtil.sendMessage(player, "§6Usage: §e/duel <player|bot> [skywars|bedwars|pvp|parkour|tnt|sumo|bridge]");
            return true;
        }

        if (args[0].equalsIgnoreCase("bot")) {
            String rawType = args.length >= 2 ? args[1] : "sumo";
            String type = normalizeType(rawType);
            startBotDuel(player, type);
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
            sendDuelChoices(player, target.getName());
            return true;
        }

        String rawType = args[1].toUpperCase();
        String type = normalizeType(rawType);

        plugin.getDuelManager().sendChallenge(player, target, type);
        return true;
    }

    private String normalizeType(String raw) {
        return switch (raw.toUpperCase()) {
            case "SKYWARS", "SW" -> "SKYWARS";
            case "BEDWARS", "BEDWARD", "BW" -> "BEDWARS";
            case "PVP", "NODEBUFF", "CLASSIC", "SWORD" -> "PVP";
            case "PARKOUR", "PK" -> "PARKOUR";
            case "TNT", "TNTRUN", "TR" -> "TNTRUN";
            case "BRIDGE", "THEBRIDGE" -> "BRIDGE";
            default -> "SUMO";
        };
    }

    private void sendDuelChoices(Player player, String targetName) {
        String label = targetName.equalsIgnoreCase("bot") ? "a Simulated Bot" : targetName;
        ChatUtil.sendMessage(player, "§6§lDUELS §8» §eSelect a 1v1 mode to duel §a" + label + "§e:");

        Component choices = Component.text(" ")
                .append(createChoiceButton(targetName, "skywars", "§b§l[SKYWARS]"))
                .append(Component.text(" "))
                .append(createChoiceButton(targetName, "bedwars", "§c§l[BEDWARS]"))
                .append(Component.text(" "))
                .append(createChoiceButton(targetName, "pvp", "§4§l[PVP]"))
                .append(Component.text(" "))
                .append(createChoiceButton(targetName, "parkour", "§a§l[PARKOUR]"))
                .append(Component.text(" "))
                .append(createChoiceButton(targetName, "tnt", "§c§l[TNT RUN]"))
                .append(Component.text(" "))
                .append(createChoiceButton(targetName, "sumo", "§e§l[SUMO]"))
                .append(Component.text(" "))
                .append(createChoiceButton(targetName, "bridge", "§6§l[BRIDGE]"));

        player.sendMessage(choices);
    }

    private void startBotDuel(Player player, String type) {
        Arena duelArena = null;
        for (Arena arena : plugin.getGameManager().getArenas()) {
            if (arena.getMaxPlayers() == 2
                    && arena.getGameType().equalsIgnoreCase(type)
                    && arena.getState() == GameState.WAITING
                    && arena.getPlayerCount() == 0) {
                duelArena = arena;
                break;
            }
        }
        if (duelArena == null) {
            ChatUtil.sendMessage(player, "§cNo empty duel arena available for " + type + "!");
            return;
        }
        ChatUtil.sendMessage(player, "§aLaunching §6" + type + " 1v1 §aagainst a simulated bot...");
        duelArena.join(player);
        if (duelArena.getSimulatedPlayers().isEmpty()) {
            duelArena.addSimulatedPlayer(plugin.getTestingManager().createBot());
        }
        duelArena.setCountdown(3);
        duelArena.forceStart();
    }

    private Component createChoiceButton(String target, String mode, String text) {
        return Component.text(text)
                .clickEvent(ClickEvent.runCommand("/duel " + target + " " + mode))
                .hoverEvent(HoverEvent.showText(Component.text("§eClick to challenge in 1v1 §a" + mode.toUpperCase() + "!")));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add("bot");
            list.add("accept");
            list.add("decline");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getName().equalsIgnoreCase(sender.getName()) && p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(p.getName());
                }
            }
            return list;
        } else if (args.length == 2) {
            return Arrays.asList("skywars", "bedwars", "pvp", "parkour", "tnt", "sumo", "bridge");
        }
        return new ArrayList<>();
    }
}
