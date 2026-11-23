package net.fpixel.core.watchdog;

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

public class WatchdogCommand implements CommandExecutor, TabCompleter {

    private final FPixelPlugin plugin;
    private final WatchdogManager watchdog;

    public WatchdogCommand(FPixelPlugin plugin, WatchdogManager watchdog) {
        this.plugin = plugin;
        this.watchdog = watchdog;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean isStaff = !(sender instanceof Player) || sender.hasPermission("fpixel.mod") || sender.hasPermission("fpixel.admin") || (sender instanceof Player p && plugin.getRankManager().getRank(p).isStaff());
        if (!isStaff) {
            sender.sendMessage(ChatUtil.colorize("§cJe hebt minstens de [MODERATOR] rang nodig om Watchdog te beheren."));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            showWatchdogStatus(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("check") || sub.equals("inspect")) {
            if (args.length < 2) {
                sender.sendMessage(ChatUtil.colorize("§cGebruik: /watchdog check <speler>"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatUtil.colorize("§cSpeler niet online: " + args[1]));
                return true;
            }
            int score = watchdog.getTrustScore(target);
            String brand = watchdog.getClientBrand(target);
            String scoreColor = score >= 90 ? "§a" : (score >= 70 ? "§e" : "§c");

            sender.sendMessage(ChatUtil.colorize("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            sender.sendMessage(ChatUtil.colorize("§c§lWATCHDOG §8» §fClient Inspectie Rapport: §e" + target.getName()));
            sender.sendMessage(ChatUtil.colorize(" §7• Trust Score: " + scoreColor + score + "§7/§a100 §8(Minimaal 90 vereist)"));
            sender.sendMessage(ChatUtil.colorize(" §7• Client Brand: §b" + brand));
            sender.sendMessage(ChatUtil.colorize(" §7• Ping: §f" + target.getPing() + "ms"));
            sender.sendMessage(ChatUtil.colorize(" §7• Status: " + (score >= 90 ? "§aVeilig & Toegestaan" : "§cGEVAARLIJK (Onder 90)")));
            sender.sendMessage(ChatUtil.colorize("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            return true;
        }

        if (sub.equals("violations") || sub.equals("log") || sub.equals("history")) {
            var violations = watchdog.getRecentViolations();
            sender.sendMessage(ChatUtil.colorize("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            sender.sendMessage(ChatUtil.colorize("§c§lWATCHDOG §8» §fRecente Overtredingen & Kicks (" + violations.size() + ")"));
            if (violations.isEmpty()) {
                sender.sendMessage(ChatUtil.colorize("§aGeen recente beveiligingsovertredingen geregistreerd."));
            } else {
                for (int i = 0; i < Math.min(10, violations.size()); i++) {
                    var v = violations.get(i);
                    sender.sendMessage(ChatUtil.colorize(" §c[" + v.timestamp() + "] §f" + v.player() + " §7(Client: §c" + v.brand() + "§7) -> Score: §c" + v.score() + " §8| §e" + v.reason()));
                }
            }
            sender.sendMessage(ChatUtil.colorize("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
            return true;
        }

        if (sub.equals("setscore")) {
            boolean isOwner = !(sender instanceof Player) || sender.hasPermission("fpixel.owner") || (sender instanceof Player p && plugin.getRankManager().getRank(p).isOwner());
            if (!isOwner && !sender.hasPermission("fpixel.admin")) {
                sender.sendMessage(ChatUtil.colorize("§cAlleen de [OWNER] kan trust scores handmatig overschrijven."));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(ChatUtil.colorize("§cGebruik: /watchdog setscore <speler> <score 0-100>"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatUtil.colorize("§cSpeler niet online: " + args[1]));
                return true;
            }
            try {
                int score = Integer.parseInt(args[2]);
                watchdog.setTrustScore(target.getUniqueId(), score);
                sender.sendMessage(ChatUtil.colorize("§aTrust score van §e" + target.getName() + " §ais ingesteld op §f" + score + "/100§a."));
                if (score < WatchdogManager.MIN_REQUIRED_SCORE) {
                    watchdog.kickForViolation(target, score, "Handmatig verlaagd door beheerder " + sender.getName());
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatUtil.colorize("§cScore moet een heel getal tussen 0 en 100 zijn."));
            }
            return true;
        }

        sender.sendMessage(ChatUtil.colorize("§cOnbekend subcommando. Gebruik: /watchdog <status|check|violations|setscore>"));
        return true;
    }

    private void showWatchdogStatus(CommandSender sender) {
        int online = Bukkit.getOnlinePlayers().size();
        int safeCount = 0;
        int atRiskCount = 0;

        for (Player p : Bukkit.getOnlinePlayers()) {
            int score = watchdog.getTrustScore(p);
            if (score >= 90) safeCount++;
            else atRiskCount++;
        }

        sender.sendMessage(ChatUtil.colorize("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ChatUtil.colorize("§c§lFPixel Watchdog Anticheat & Client Integrity"));
        sender.sendMessage(ChatUtil.colorize(" §7• Minimale vereiste Trust Score: §a90§7/§a100"));
        sender.sendMessage(ChatUtil.colorize(" §7• Wurst / Hackclient detectie: §c0 punten (Instant Kick)"));
        sender.sendMessage(ChatUtil.colorize(" §7• Actief Gecontroleerde Spelers: §a" + online));
        sender.sendMessage(ChatUtil.colorize(" §7• Veilige Spelers (≥90): §a" + safeCount));
        sender.sendMessage(ChatUtil.colorize(" §7• Gevaarlijke Spelers (<90): §c" + atRiskCount));
        sender.sendMessage(ChatUtil.colorize(" §7• Beheer: §e/watchdog check <speler> §7| §e/watchdog violations"));
        sender.sendMessage(ChatUtil.colorize("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("status", "check", "violations", "setscore");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("check") || args[0].equalsIgnoreCase("setscore"))) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return names;
        }
        return List.of();
    }
}
