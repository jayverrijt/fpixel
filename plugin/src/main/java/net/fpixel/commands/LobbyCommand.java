package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.game.Arena;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LobbyCommand implements CommandExecutor {
    private final FPixelPlugin plugin;

    public LobbyCommand(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can execute this command.");
            return true;
        }

        Arena arena = plugin.getGameManager().getPlayerArena(player);
        if (arena != null) {
            arena.leave(player);
        }

        plugin.getLobbyManager().teleportToLobby(player);
        return true;
    }
}
