package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.user.FPixelUser;
import net.fpixel.core.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CoinsCommand implements CommandExecutor, TabCompleter {
    private final FPixelPlugin plugin;
    private final NumberFormat numFormat = NumberFormat.getInstance(Locale.US);

    public CoinsCommand(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (label.equalsIgnoreCase("addcoins")) {
            if (!sender.hasPermission("fpixel.admin")) {
                ChatUtil.sendMessage(sender, "§cYou do not have permission to add coins.");
                return true;
            }
            if (args.length < 2) {
                ChatUtil.sendMessage(sender, "§cUsage: /coins add <player> <amount>");
                return true;
            }
            String[] newArgs = new String[args.length + 1];
            newArgs[0] = "add";
            System.arraycopy(args, 0, newArgs, 1, args.length);
            args = newArgs;
        }

        if (args.length == 0) {
            if (sender instanceof Player player) {
                FPixelUser user = plugin.getUserManager().getUser(player);
                ChatUtil.sendMessage(player, "§eYour balance: §6" + numFormat.format(user.getCoins()) + " Coins§e.");
            } else {
                sendHelp(sender);
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "balance", "get" -> {
                if (args.length == 1) {
                    if (sender instanceof Player player) {
                        FPixelUser user = plugin.getUserManager().getUser(player);
                        ChatUtil.sendMessage(player, "§eYour balance: §6" + numFormat.format(user.getCoins()) + " Coins§e.");
                    } else {
                        sender.sendMessage(ChatUtil.component("§cUsage: /coins balance <player>"));
                    }
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    ChatUtil.sendMessage(sender, "§cPlayer '" + args[1] + "' is not online.");
                    return true;
                }
                FPixelUser targetUser = plugin.getUserManager().getUser(target);
                ChatUtil.sendMessage(sender, "§ePlayer §f" + target.getName() + " §ehas §6" + numFormat.format(targetUser.getCoins()) + " Coins§e.");
            }
            case "add", "give" -> {
                if (!sender.hasPermission("fpixel.admin")) {
                    ChatUtil.sendMessage(sender, "§cYou do not have permission to add coins.");
                    return true;
                }
                if (args.length < 3) {
                    ChatUtil.sendMessage(sender, "§cUsage: /coins add <player> <amount>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    ChatUtil.sendMessage(sender, "§cPlayer '" + args[1] + "' is not online.");
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[2]);
                    if (amount <= 0) {
                        ChatUtil.sendMessage(sender, "§cAmount must be greater than 0.");
                        return true;
                    }
                    FPixelUser user = plugin.getUserManager().getUser(target);
                    user.addCoins(amount);
                    plugin.getUserManager().saveUser(user);
                    ChatUtil.sendMessage(sender, "§aAdded §6" + numFormat.format(amount) + " Coins §ato §f" + target.getName() + " §a(New balance: §6" + numFormat.format(user.getCoins()) + "§a).");
                    ChatUtil.sendMessage(target, "§6+" + numFormat.format(amount) + " Coins §7added to your account! (Balance: §6" + numFormat.format(user.getCoins()) + " Coins§7)");
                } catch (NumberFormatException e) {
                    ChatUtil.sendMessage(sender, "§cInvalid amount: " + args[2]);
                }
            }
            case "set" -> {
                if (!sender.hasPermission("fpixel.admin")) {
                    ChatUtil.sendMessage(sender, "§cYou do not have permission to set coins.");
                    return true;
                }
                if (args.length < 3) {
                    ChatUtil.sendMessage(sender, "§cUsage: /coins set <player> <amount>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    ChatUtil.sendMessage(sender, "§cPlayer '" + args[1] + "' is not online.");
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[2]);
                    if (amount < 0) {
                        ChatUtil.sendMessage(sender, "§cAmount cannot be negative.");
                        return true;
                    }
                    FPixelUser user = plugin.getUserManager().getUser(target);
                    user.setCoins(amount);
                    plugin.getUserManager().saveUser(user);
                    ChatUtil.sendMessage(sender, "§aSet coins of §f" + target.getName() + " §ato §6" + numFormat.format(amount) + " Coins§a.");
                    ChatUtil.sendMessage(target, "§eYour coin balance has been set to §6" + numFormat.format(amount) + " Coins§e.");
                } catch (NumberFormatException e) {
                    ChatUtil.sendMessage(sender, "§cInvalid amount: " + args[2]);
                }
            }
            case "remove", "take" -> {
                if (!sender.hasPermission("fpixel.admin")) {
                    ChatUtil.sendMessage(sender, "§cYou do not have permission to remove coins.");
                    return true;
                }
                if (args.length < 3) {
                    ChatUtil.sendMessage(sender, "§cUsage: /coins remove <player> <amount>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    ChatUtil.sendMessage(sender, "§cPlayer '" + args[1] + "' is not online.");
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[2]);
                    if (amount <= 0) {
                        ChatUtil.sendMessage(sender, "§cAmount must be greater than 0.");
                        return true;
                    }
                    FPixelUser user = plugin.getUserManager().getUser(target);
                    user.removeCoins(amount);
                    plugin.getUserManager().saveUser(user);
                    ChatUtil.sendMessage(sender, "§aRemoved §6" + numFormat.format(amount) + " Coins §afrom §f" + target.getName() + " §a(New balance: §6" + numFormat.format(user.getCoins()) + "§a).");
                    ChatUtil.sendMessage(target, "§c-" + numFormat.format(amount) + " Coins §7deducted from your account! (Balance: §6" + numFormat.format(user.getCoins()) + " Coins§7)");
                } catch (NumberFormatException e) {
                    ChatUtil.sendMessage(sender, "§cInvalid amount: " + args[2]);
                }
            }
            case "pay", "send" -> {
                if (!(sender instanceof Player player)) {
                    ChatUtil.sendMessage(sender, "§cOnly players can send coins.");
                    return true;
                }
                if (args.length < 3) {
                    ChatUtil.sendMessage(player, "§cUsage: /coins pay <player> <amount>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    ChatUtil.sendMessage(player, "§cPlayer '" + args[1] + "' is not online.");
                    return true;
                }
                if (target.getUniqueId().equals(player.getUniqueId())) {
                    ChatUtil.sendMessage(player, "§cYou cannot pay yourself!");
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[2]);
                    if (amount <= 0) {
                        ChatUtil.sendMessage(player, "§cAmount must be greater than 0.");
                        return true;
                    }
                    FPixelUser senderUser = plugin.getUserManager().getUser(player);
                    if (senderUser.getCoins() < amount) {
                        ChatUtil.sendMessage(player, "§cYou do not have enough coins! (Current balance: §6" + numFormat.format(senderUser.getCoins()) + "§c)");
                        return true;
                    }
                    FPixelUser targetUser = plugin.getUserManager().getUser(target);

                    senderUser.removeCoins(amount);
                    targetUser.addCoins(amount);

                    plugin.getUserManager().saveUser(senderUser);
                    plugin.getUserManager().saveUser(targetUser);

                    ChatUtil.sendMessage(player, "§aSent §6" + numFormat.format(amount) + " Coins §ato §f" + target.getName() + "§a!");
                    ChatUtil.sendMessage(target, "§6+" + numFormat.format(amount) + " Coins §7received from §f" + player.getName() + "§7!");
                } catch (NumberFormatException e) {
                    ChatUtil.sendMessage(player, "§cInvalid amount: " + args[2]);
                }
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        ChatUtil.sendMessage(sender, "§6§lFPixel Coins Commands:");
        ChatUtil.sendMessage(sender, " §e/coins §7- Check your coin balance");
        ChatUtil.sendMessage(sender, " §e/coins balance [player] §7- View player's coins");
        ChatUtil.sendMessage(sender, " §e/coins pay <player> <amount> §7- Send coins to another player");
        if (sender.hasPermission("fpixel.admin")) {
            ChatUtil.sendMessage(sender, " §c/coins add <player> <amount> §7- Add coins (Admin)");
            ChatUtil.sendMessage(sender, " §c/coins set <player> <amount> §7- Set coins (Admin)");
            ChatUtil.sendMessage(sender, " §c/coins remove <player> <amount> §7- Take coins (Admin)");
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> subs = new ArrayList<>(List.of("balance", "pay", "help"));
            if (sender.hasPermission("fpixel.admin")) {
                subs.addAll(List.of("add", "set", "remove"));
            }
            for (String s : subs) {
                if (s.startsWith(input)) completions.add(s);
            }
        } else if (args.length == 2) {
            String input = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(input)) {
                    completions.add(p.getName());
                }
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (List.of("add", "set", "remove", "pay", "take", "give").contains(sub)) {
                completions.addAll(List.of("100", "500", "1000", "5000"));
            }
        }
        return completions;
    }
}
