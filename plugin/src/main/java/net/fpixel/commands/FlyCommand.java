package net.fpixel.commands;

import net.fpixel.FPixelPlugin;
import net.fpixel.core.rank.Rank;
import net.fpixel.core.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlyCommand implements CommandExecutor {
    private final FPixelPlugin plugin;

    public FlyCommand(FPixelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /fly.");
            return true;
        }

        // Only allowed in the Lobby world
        if (plugin.getGameManager().getPlayerArena(player) != null
                || !player.getWorld().equals(plugin.getLobbyManager().getSpawnLocation().getWorld())) {
            ChatUtil.sendMessage(player, "§cYou can only toggle flight in the Lobby!");
            return true;
        }

        Rank rank = plugin.getRankManager().getRank(player);
        if (rank == Rank.MEMBER && !player.hasPermission("fpixel.fly")) {
            ChatUtil.sendMessage(player, "§c§lFLY §8» §cYou need §a[VIP] §crank or higher to fly in the Lobby!");
            ChatUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        if (player.isFlying()) {
            player.setFlying(false);
            ChatUtil.sendMessage(player, "§c§lFLY §8» §cFlight mode disabled.");
            ChatUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
        } else {
            player.setAllowFlight(true);
            player.setFlying(true);
            ChatUtil.sendMessage(player, "§a§lFLY §8» §aFlight mode enabled! Enjoy flying around the Grand Hub.");
            ChatUtil.playSound(player, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.4f);
        }

        return true;
    }
}
