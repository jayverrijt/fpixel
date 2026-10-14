package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.testing.SimulatedPlayer;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.game.Arena;
import net.fpixel.game.GameState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestingCommand implements CommandExecutor, TabCompleter {
    private final FPixelPlugin plugin;

    public TestingCommand(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fpixel.admin")) {
            sender.sendMessage(ChatUtil.colorize("§cYou do not have permission to execute this command."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "on", "enable" -> {
                plugin.getTestingManager().setTestingMode(true);
                sender.sendMessage(ChatUtil.colorize("§a§l[TESTING MODE] §aTesting mode is now §lON§a!"));
                sender.sendMessage(ChatUtil.colorize("§7Whenever you join any minigame, simulated players will auto-join and start the game in 3 seconds."));
            }
            case "off", "disable" -> {
                plugin.getTestingManager().setTestingMode(false);
                sender.sendMessage(ChatUtil.colorize("§c§l[TESTING MODE] §cTesting mode is now §lOFF§c."));
            }
            case "status" -> {
                boolean on = plugin.getTestingManager().isTestingMode();
                sender.sendMessage(ChatUtil.colorize("§6§l[TESTING MODE] §fStatus: " + (on ? "§a§lENABLED" : "§c§lDISABLED")));
                if (sender instanceof Player p) {
                    Arena arena = plugin.getGameManager().getPlayerArena(p);
                    if (arena != null) {
                        sender.sendMessage(ChatUtil.colorize("§7Current Arena: §e" + arena.getId() + " §7(" + arena.getState() + ")"));
                        sender.sendMessage(ChatUtil.colorize("§7Players: §b" + arena.getPlayerCount() + "§7/" + arena.getMaxPlayers() +
                                " (Real: " + arena.getBukkitPlayers().size() + ", Bots: " + arena.getSimulatedPlayers().size() + ")"));
                    }
                }
            }
            case "bot", "addbot" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can add bots to an arena.");
                    return true;
                }
                Arena arena = plugin.getGameManager().getPlayerArena(p);
                if (arena == null) {
                    ChatUtil.sendMessage(p, "§cYou must be inside an arena to add simulated players! Join a game first with /join or NPC.");
                    return true;
                }
                int count = 1;
                if (args.length >= 2) {
                    try {
                        count = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {}
                }
                for (int i = 0; i < count; i++) {
                    SimulatedPlayer bot = plugin.getTestingManager().createBot();
                    arena.addSimulatedPlayer(bot);
                }
                ChatUtil.sendMessage(p, "§aAdded §b" + count + " §asimulated player(s) to " + arena.getId() + "!");
            }
            case "start", "forcestart" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can force-start an arena.");
                    return true;
                }
                Arena arena = plugin.getGameManager().getPlayerArena(p);
                if (arena == null) {
                    ChatUtil.sendMessage(p, "§cYou must be inside an arena to start it.");
                    return true;
                }
                if (arena.getPlayerCount() < arena.getMinPlayers()) {
                    int needed = arena.getMinPlayers() - arena.getPlayerCount();
                    for (int i = 0; i < needed; i++) {
                        arena.addSimulatedPlayer(plugin.getTestingManager().createBot());
                    }
                }
                arena.setCountdown(3);
                arena.forceStart();
                ChatUtil.sendMessage(p, "§aArena countdown forced to 3 seconds!");
            }
            case "win" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can trigger victory.");
                    return true;
                }
                Arena arena = plugin.getGameManager().getPlayerArena(p);
                if (arena == null || arena.getState() != GameState.IN_GAME) {
                    ChatUtil.sendMessage(p, "§cYou must be in an active IN_GAME match to trigger a win.");
                    return true;
                }
                // Clear all bots to simulate full victory
                for (SimulatedPlayer bot : new ArrayList<>(arena.getSimulatedPlayers().values())) {
                    arena.eliminateSimulatedPlayer(bot);
                }
                arena.endGame(p);
                ChatUtil.sendMessage(p, "§a§l[TESTING] §eTriggered match victory for testing!");
            }
            case "end", "reset" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can reset an arena.");
                    return true;
                }
                Arena arena = plugin.getGameManager().getPlayerArena(p);
                if (arena == null) {
                    ChatUtil.sendMessage(p, "§cYou must be inside an arena to reset it.");
                    return true;
                }
                arena.resetArena();
                ChatUtil.sendMessage(p, "§aArena §e" + arena.getId() + " §ahas been reset and players returned to lobby.");
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatUtil.colorize("§6§lFPixel Testing Commands:"));
        sender.sendMessage(ChatUtil.colorize(" §e/testing on §7- Enable auto-simulated players on game join"));
        sender.sendMessage(ChatUtil.colorize(" §e/testing off §7- Disable testing mode"));
        sender.sendMessage(ChatUtil.colorize(" §e/testing status §7- View testing mode status & arena stats"));
        sender.sendMessage(ChatUtil.colorize(" §e/testing bot [count] §7- Spawn simulated players in current arena"));
        sender.sendMessage(ChatUtil.colorize(" §e/testing start §7- Fast-start countdown (3 seconds)"));
        sender.sendMessage(ChatUtil.colorize(" §e/testing win §7- Instantly trigger victory in current game"));
        sender.sendMessage(ChatUtil.colorize(" §e/testing end §7- Reset current arena back to lobby"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = Arrays.asList("on", "off", "status", "bot", "start", "win", "end");
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
