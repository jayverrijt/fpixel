package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.party.Party;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class PartyCommand implements CommandExecutor, TabCompleter {
    private final FPixelPlugin plugin;

    public PartyCommand(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use party commands.");
            return true;
        }

        // Handle shorthand /p <message> for party chat
        if (label.equalsIgnoreCase("p") && args.length > 0 && !isPartySubcommand(args[0])) {
            Party party = plugin.getPartyManager().getParty(player);
            if (party == null) {
                ChatUtil.sendMessage(player, "§cYou are not in a party. Create one using /party invite <player>.");
                return true;
            }
            party.chat(player, String.join(" ", args));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendPartyHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("invite") && args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                ChatUtil.sendMessage(player, "§cPlayer not found.");
                return true;
            }
            plugin.getPartyManager().invite(player, target);
        } else if (sub.equals("accept") && args.length >= 2) {
            plugin.getPartyManager().accept(player, args[1]);
        } else if (sub.equals("decline") && args.length >= 2) {
            plugin.getPartyManager().decline(player, args[1]);
        } else if (sub.equals("leave")) {
            plugin.getPartyManager().leave(player);
        } else if (sub.equals("kick") && args.length >= 2) {
            plugin.getPartyManager().kick(player, args[1]);
        } else if (sub.equals("disband")) {
            plugin.getPartyManager().disband(player);
        } else if (sub.equals("list")) {
            Party party = plugin.getPartyManager().getParty(player);
            if (party == null) {
                ChatUtil.sendMessage(player, "§cYou are not in a party.");
                return true;
            }
            Player leader = Bukkit.getPlayer(party.getLeader());
            String leaderName = leader != null ? leader.getName() : "Unknown";
            ChatUtil.sendRawMessage(player, "§9§m----------------------------------------");
            ChatUtil.sendRawMessage(player, "§9§lParty Members §7(" + party.getMembers().size() + "):");
            ChatUtil.sendRawMessage(player, " §e• Leader: §a" + leaderName);
            for (UUID uid : party.getMembers()) {
                if (!uid.equals(party.getLeader())) {
                    Player m = Bukkit.getPlayer(uid);
                    if (m != null) {
                        ChatUtil.sendRawMessage(player, " §e• Member: §f" + m.getName() + (m.isOnline() ? " §a(Online)" : " §7(Offline)"));
                    }
                }
            }
            ChatUtil.sendRawMessage(player, "§9§m----------------------------------------");
        } else if (sub.equals("warp")) {
            plugin.getPartyManager().warpParty(player);
        } else if (sub.equals("chat") && args.length >= 2) {
            Party party = plugin.getPartyManager().getParty(player);
            if (party == null) {
                ChatUtil.sendMessage(player, "§cYou are not in a party.");
                return true;
            }
            String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            party.chat(player, msg);
        } else {
            sendPartyHelp(player);
        }

        return true;
    }

    private boolean isPartySubcommand(String arg) {
        List<String> subs = Arrays.asList("invite", "accept", "decline", "leave", "kick", "disband", "list", "chat", "warp", "help");
        return subs.contains(arg.toLowerCase());
    }

    private void sendPartyHelp(Player player) {
        ChatUtil.sendRawMessage(player, "§9§lParty Commands:");
        ChatUtil.sendRawMessage(player, " §e/party invite <player> §7- Invite a player to your party");
        ChatUtil.sendRawMessage(player, " §e/party accept <player> §7- Accept a party invite");
        ChatUtil.sendRawMessage(player, " §e/party warp §7- Warp all party members to your location");
        ChatUtil.sendRawMessage(player, " §e/party leave §7- Leave your current party");
        ChatUtil.sendRawMessage(player, " §e/party kick <player> §7- Kick a member from party");
        ChatUtil.sendRawMessage(player, " §e/party disband §7- Disband your party");
        ChatUtil.sendRawMessage(player, " §e/party list §7- View party members");
        ChatUtil.sendRawMessage(player, " §e/p <message> §7- Send a message to party chat");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = Arrays.asList("invite", "accept", "decline", "leave", "kick", "disband", "list", "chat", "warp");
            List<String> filtered = new ArrayList<>();
            for (String s : list) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                    filtered.add(s);
                }
            }
            return filtered;
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick"))) {
            List<String> list = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getName().equalsIgnoreCase(sender.getName()) && p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    list.add(p.getName());
                }
            }
            return list;
        }
        return new ArrayList<>();
    }
}
