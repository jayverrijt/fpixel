package net.fpixel.core.bugreport;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BugReportCommand implements CommandExecutor, TabCompleter {

    private final FPixelPlugin plugin;
    private final BugReportManager reportManager;

    public BugReportCommand(FPixelPlugin plugin, BugReportManager reportManager) {
        this.plugin = plugin;
        this.reportManager = reportManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("list")) {
            List<BugReportManager.BugReport> list = reportManager.getAllReports();
            if (list.isEmpty()) {
                sender.sendMessage("§aEr zijn momenteel geen bug reports ingediend!");
                return true;
            }
            sender.sendMessage("§8§m----------------§r §6§lFPixel Bug Reports§r §8§m----------------");
            int count = 0;
            for (BugReportManager.BugReport r : list) {
                if (count++ >= 10) break;
                String statusColor = "OPEN".equalsIgnoreCase(r.getStatus()) ? "§c[OPEN]" : "§a[RESOLVED]";
                sender.sendMessage(String.format("§e#%d %s §f%s §7(%s in %s) §8- §e%s",
                        r.getId(), statusColor, r.getDescription(), r.getReporter(), r.getMinigame(), r.getTimestamp()));
            }
            sender.sendMessage("§7Gebruik §e/bugreport view <id> §7voor details of §e/bugreport resolve <id>§7.");
            return true;
        }

        if (sub.equals("view")) {
            if (args.length < 2) {
                sender.sendMessage("§cGebruik: /bugreport view <id>");
                return true;
            }
            try {
                int id = Integer.parseInt(args[1]);
                BugReportManager.BugReport r = reportManager.getReport(id);
                if (r == null) {
                    sender.sendMessage("§cBug report #" + id + " niet gevonden.");
                    return true;
                }
                sender.sendMessage("§8§m----------------§r §6§lBug Report #" + r.getId() + "§r §8§m----------------");
                sender.sendMessage("§7Status: " + ("OPEN".equalsIgnoreCase(r.getStatus()) ? "§c§lOPEN" : "§a§lRESOLVED"));
                if (r.getResolvedBy() != null) {
                    sender.sendMessage("§7Opgelost door: §b" + r.getResolvedBy());
                }
                sender.sendMessage("§7Inzender: §e" + r.getReporter() + " §8(" + r.getUuid() + ")");
                sender.sendMessage("§7Tijdstip: §f" + r.getTimestamp());
                sender.sendMessage("§7Wereld: §b" + r.getWorld() + " §7| Coördinaten: §f" + r.getCoordinates());
                sender.sendMessage("§7Gamemode: §f" + r.getGamemode() + " §7| Minigame: §a" + r.getMinigame());
                sender.sendMessage("§7Beschrijving: §f" + r.getDescription());
                sender.sendMessage("§8§m---------------------------------------------");
            } catch (NumberFormatException e) {
                sender.sendMessage("§cOngeldig rapport ID.");
            }
            return true;
        }

        if (sub.equals("resolve")) {
            if (!sender.hasPermission("fpixel.admin") && !sender.isOp()) {
                sender.sendMessage("§cJe hebt geen toestemming om bug reports als opgelost te markeren.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cGebruik: /bugreport resolve <id>");
                return true;
            }
            try {
                int id = Integer.parseInt(args[1]);
                boolean ok = reportManager.resolveReport(id, sender.getName());
                if (ok) {
                    sender.sendMessage("§aBug report #" + id + " is gemarkeerd als §a§lRESOLVED§a!");
                } else {
                    sender.sendMessage("§cBug report #" + id + " niet gevonden.");
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cOngeldig rapport ID.");
            }
            return true;
        }

        // Otherwise, submit a new bug report
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cAlleen spelers in het spel kunnen een live bugreport indienen.");
            return true;
        }

        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(arg);
        }
        String description = sb.toString();

        BugReportManager.BugReport report = reportManager.createReport(player, description);

        player.sendMessage("§8§m---------------------------------------------");
        player.sendMessage("§a✔ §lBedankt voor je bug report! §7(ID: §e#" + report.getId() + "§7)");
        player.sendMessage("§7Wereld: §f" + report.getWorld() + " §8| §7Locatie: §f" + report.getCoordinates());
        player.sendMessage("§7Gamemode/Minigame: §e" + report.getMinigame());
        player.sendMessage("§7Beschrijving: §f" + report.getDescription());
        player.sendMessage("§7Het FPixel team is genotificeerd.");
        player.sendMessage("§8§m---------------------------------------------");
        ChatUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);

        // Notify staff
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isOp() || p.hasPermission("fpixel.admin")) {
                p.sendMessage("§6[BugReport] §e" + player.getName() + " §7meldde: §f" + description + " §8(ID #" + report.getId() + " in " + report.getMinigame() + ")");
            }
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m----------------§r §6§lFPixel Bug Report§r §8§m----------------");
        sender.sendMessage("§e/bugreport <beschrijving> §7- Meld een bug op je huidige locatie/gamemode");
        sender.sendMessage("§e/bugreport list           §7- Bekijk recente bug reports");
        sender.sendMessage("§e/bugreport view <id>      §7- Bekijk details van een rapport");
        if (sender.isOp() || sender.hasPermission("fpixel.admin")) {
            sender.sendMessage("§e/bugreport resolve <id>   §7- Markeer een bug als opgelost");
        }
        sender.sendMessage("§8§m---------------------------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            String p = args[0].toLowerCase();
            if ("list".startsWith(p)) list.add("list");
            if ("view".startsWith(p)) list.add("view");
            if ("resolve".startsWith(p) && (sender.isOp() || sender.hasPermission("fpixel.admin"))) list.add("resolve");
        }
        return list;
    }
}
