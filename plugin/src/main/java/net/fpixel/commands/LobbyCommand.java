package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.util.ChatUtil;
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

        if (plugin.getPartyManager() != null) {
            var party = plugin.getPartyManager().getParty(player);
            if (party != null && party.isLeader(player) && arena != null && (arena.getState() == net.fpixel.game.GameState.WAITING || arena.getState() == net.fpixel.game.GameState.STARTING)) {
                for (Player member : party.getOnlineMembers()) {
                    if (!member.equals(player)) {
                        Arena mArena = plugin.getGameManager().getPlayerArena(member);
                        if (mArena != null) mArena.leave(member);
                        plugin.getLobbyManager().teleportToLobby(member);
                        ChatUtil.sendMessage(member, "§cYour party leader returned to the lobby.");
                    }
                }
            }
        }

        plugin.getLobbyManager().teleportToLobby(player);
        return true;
    }
}
