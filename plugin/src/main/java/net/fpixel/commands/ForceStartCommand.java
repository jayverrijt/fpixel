package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
import net.fpixel.game.Arena;
import net.fpixel.game.GameState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ForceStartCommand implements CommandExecutor {
    private final FPixelPlugin plugin;

    public ForceStartCommand(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can force-start games.");
            return true;
        }

        Arena arena = plugin.getGameManager().getPlayerArena(player);
        if (arena == null) {
            ChatUtil.sendMessage(player, "§cYou must be inside an arena waiting room to force-start a game.");
            return true;
        }

        if (arena.getState() != GameState.WAITING && arena.getState() != GameState.STARTING) {
            ChatUtil.sendMessage(player, "§cThis game is already in progress!");
            return true;
        }

        if (arena.getPlayerCount() < 2) {
            ChatUtil.sendMessage(player, "§cYou need at least 2 players in the arena to start!");
            return true;
        }

        arena.forceStart();
        ChatUtil.sendMessage(player, "§aForce-started the countdown!");
        return true;
    }
}
